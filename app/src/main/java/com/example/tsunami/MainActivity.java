package com.example.tsunami;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;

public class MainActivity extends AppCompatActivity {
    /** Tag for the log messages */
    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final String USGS_REQUEST_URL =
            "https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&starttime=2019-01-01&endtime=2020-05-20&minmagnitude=6";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Kick off an {@link AsyncTask} to perform the network request
        TsunamiAsyncTask task = new TsunamiAsyncTask(this);
        task.execute();
    }

    //UPDATE DATA ON ACTIVITY
    private void updateUi(Event earthquake) {
        // Display the earthquake title in the UI
        TextView titleTextView = (TextView) findViewById(R.id.title);
        titleTextView.setText(earthquake.title);

        // Display the earthquake date in the UI
        TextView dateTextView = (TextView) findViewById(R.id.date);
        dateTextView.setText(getDateString(earthquake.time));

        // Display whether or not there was a tsunami alert in the UI
        TextView tsunamiTextView = (TextView) findViewById(R.id.tsunami_alert);
        tsunamiTextView.setText(getTsunamiAlertString(earthquake.tsunamiAlert));
    }

    //OBTENER LA FECHA
    private String getDateString(long timeInMilliseconds) {
        SimpleDateFormat formatter =    new SimpleDateFormat("EEE, d MMM yyyy 'at' HH:mm:ss z");
        return formatter.format(timeInMilliseconds);
    }

    //OBTENER SI HAY UN MENSAJE DE ALERTA
    private String getTsunamiAlertString(int tsunamiAlert) {
        switch (tsunamiAlert) {
            case 0:
                return getString(R.string.alert_no);
            case 1:
                return getString(R.string.alert_yes);
            default:
                return getString(R.string.alert_not_available);
        }
    }

    /**
     * {@link AsyncTask} to perform the network request on a background thread, and then
     * update the UI with the first earthquake in the response.
     */
    private class TsunamiAsyncTask extends AsyncTask<URL, Void, Event> {
        Context context;

        public TsunamiAsyncTask(Context context) {
            this.context = context;
        }

        @Override
        protected Event doInBackground(URL... urls) {
            // Create URL object
            URL url = createUrl(USGS_REQUEST_URL);

            if (url != null) {
                String jsonResponse = "";
                try {
                    jsonResponse = makeHttpRequest(url);
                } catch (IOException e) {
                    // TODO: Handle the IOException
                }

                // Extract relevant fields from the JSON response and create an {@link Event} object
                Event earthquake = extractFeatureFromJson(jsonResponse);

                // Return the {@link Event} object as the result fo the {@link TsunamiAsyncTask}
                return earthquake;
            }else{
                message(context, "URL no valida");
            }
            return null;
        }

        //MAANDAR DAATOS A LA ACTIVITY SI EL EVENT NO ES NULL
        @Override
        protected void onPostExecute(Event earthquake) {
            if (earthquake == null) {
                return;
            }
            updateUi(earthquake);
        }

        //CREAR OBJETO URL DESDE UN STRING
        private URL createUrl(String stringUrl) {
            URL url = null;
            if (validateURL(stringUrl)){
                try {
                    url = new URL(stringUrl);
                } catch (MalformedURLException exception) {
                    Log.e(LOG_TAG, "Error with creating URL", exception);
                    return null;
                }
            }else{
                return null;
            }
            return url;
        }

        //PEDIR DATA AL SERVIDOR Y OBTENER EL JSON
        private String makeHttpRequest(URL url) throws IOException {

            String jsonResponse = "";
            HttpURLConnection urlConnection = null;
            InputStream inputStream = null;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000 /* milliseconds */);
                urlConnection.setConnectTimeout(15000 /* milliseconds */);
                urlConnection.connect();
                if (urlConnection.getResponseCode() >= 200 && urlConnection.getResponseCode() <= 299){
                    inputStream = urlConnection.getInputStream();
                    jsonResponse = readFromStream(inputStream);
                    Log.d("MESSAGE", urlConnection.getResponseCode()+"");
                }else{
                    message(context, "ERROR:" + urlConnection.getResponseCode());
                    Log.e("MESSAGE", urlConnection.getResponseCode()+"");
                }

            } catch (IOException e) {
                // TODO: Handle the exception
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (inputStream != null) {
                    // function must handle java.io.IOException here
                    inputStream.close();
                }
            }
            return jsonResponse;
        }

        //Convierte la respuesta del servidor (JSON) en un String
        private String readFromStream(InputStream inputStream) throws IOException {
            StringBuilder output = new StringBuilder();
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String line = reader.readLine();
                while (line != null) {
                    output.append(line);
                    line = reader.readLine();
                }
            }
            return output.toString();
        }

        //EXTRAER LOS DATOS DEL PRIMER TERREMOTO EN EL JSON
        private Event extractFeatureFromJson(String earthquakeJSON) {
            try {
                JSONObject baseJsonResponse = new JSONObject(earthquakeJSON);
                JSONArray featureArray = baseJsonResponse.getJSONArray("features");

                // If there are results in the features array
                if (featureArray.length() > 0) {
                    // Extract out the first feature (which is an earthquake)
                    JSONObject firstFeature = featureArray.getJSONObject(0);
                    JSONObject properties = firstFeature.getJSONObject("properties");

                    // Extract out the title, time, and tsunami values
                    String title = properties.getString("title");
                    long time = properties.getLong("time");
                    int tsunamiAlert = properties.getInt("tsunami");

                    // Create a new {@link Event} object
                    return new Event(title, time, tsunamiAlert);
                }
                return null;
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Problem parsing the earthquake JSON results", e);
                return null;
            }
        }
    }

    public boolean validateURL(String url){
        return (url != null && !url.isEmpty()) ? true : false;
    }

    public void message(Context context, String message){
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
