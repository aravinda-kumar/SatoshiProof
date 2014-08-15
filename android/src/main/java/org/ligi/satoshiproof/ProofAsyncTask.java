package org.ligi.satoshiproof;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.params.MainNetParams;

import org.ligi.axt.AXT;
import org.ligi.axt.listeners.DialogDiscardingOnClickListener;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import de.schildbach.wallet.integration.android.BitcoinIntegration;

class ProofAsyncTask extends AsyncTask<Void, String, String> {

    private ProgressDialog progressDialog;
    private Address address;
    private final byte[] data;
    private final Context context;

    public ProofAsyncTask(Context context, byte[] data) {
        this.data = data;
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("Checking this text");
        progressDialog.show();
    }

    @Override
    protected void onProgressUpdate(String... values) {
        progressDialog.setMessage(values[0]);
        super.onProgressUpdate(values);
    }

    @Override
    protected String doInBackground(Void... voids) {
        address = new Address(MainNetParams.get(), Utils.sha256hash160(data));
        publishProgress("searching for Address: " + address.toString());

        try {
            final URL url = new URL("http://blockexplorer.com/q/addressfirstseen/" + address.toString());
            return AXT.at(url).downloadToString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String firstSeenDateString) {
        progressDialog.dismiss();
        final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
        alertBuilder.setPositiveButton("OK", new DialogDiscardingOnClickListener());
        if (firstSeenDateString.toLowerCase(Locale.getDefault()).startsWith("never seen")) {
            alertBuilder.setMessage("The existence of this is not proven yet.");
            alertBuilder.setNeutralButton("Add Proof", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    BitcoinIntegration.request(context, address.toString(), 1);
                }
            });

        } else {
            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            Date date = null;
            try {
                dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                date = dateFormat.parse(firstSeenDateString);
            } catch (ParseException e) {
            }

            String dateString = firstSeenDateString + " UTC";
            if (date != null) {
                dateString = date.toString();
            }

            alertBuilder.setMessage("The existence of this was proven on:" + dateString);
        }
        alertBuilder.show();
        super.onPostExecute(firstSeenDateString);
    }
}