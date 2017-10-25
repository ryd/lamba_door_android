package de.nons.lambadoor;

import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {
    public static TextView text = null;
    private ProgressBar bar = null;
    private Button button = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        text = (TextView) findViewById(R.id.textView2);
        bar = (ProgressBar) findViewById(R.id.progressBar2);
        button = (Button) findViewById(R.id.button);

        // update status
        this.onClick(null);

    }

    public void onClick(View v) {
        button.setEnabled(false);
        bar.setVisibility(View.VISIBLE);
        text.setText("Status: Fetching LAMBA status.");
        bar.setProgress(10);

        new Thread(new Runnable() {
            public void setStatus(final int status) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView text = (TextView) findViewById(R.id.textView2);
                        if (text == null)
                            return;
                        ImageView view = (ImageView) findViewById(R.id.imageView);
                        if (view == null)
                            return;

                        if (status == 1) {
                            text.setText("Status: LAMBA is open.");
                            view.setImageResource(R.drawable.logo_green);
                        } else {
                            text.setText("Status: Closed at the moment. Check again later.");
                            view.setImageResource(R.drawable.logo_red);
                        }
                    }
                });
            }

            public void enableButton() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Button button = (Button) findViewById(R.id.button);
                        if (button == null)
                            return;
                        button.setEnabled(true);

                        ProgressBar bar = (ProgressBar) findViewById(R.id.progressBar2);
                        if (bar == null)
                            return;
                        bar.setVisibility(View.INVISIBLE);
                    }
                });
            }

            public void run() {
                try {
                    StringBuffer buffer = new StringBuffer();
                    URL url = new URL("https://thingspeak.com/channels/251901/feed.json");
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setReadTimeout(3000);
                    urlConnection.setConnectTimeout(3000);
                    bar.setProgress(20);
                    urlConnection.connect();
                    bar.setProgress(30);
                    int responseCode = urlConnection.getResponseCode();
                    if (responseCode != HttpsURLConnection.HTTP_OK) {
                        throw new IOException("HTTP error code: " + responseCode);
                    }

                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                        bar.setProgress(40);
                        String line;
                        while ((line = reader.readLine()) != null) {
                            buffer.append(line);
                        }
                        reader.close();
                        bar.setProgress(60);
                    } finally {
                        urlConnection.disconnect();
                    }

                    JSONObject json = new JSONObject(buffer.toString());
                    bar.setProgress(70);
                    JSONArray status = json.getJSONArray("feeds");
                    if (status == null) {
                        throw new Exception("Missing feeds information in JSON String.");
                    }
                    int len = status.length();
                    JSONObject door = status.getJSONObject(len - 1);

                    setStatus(door.getInt("field1"));
                    bar.setProgress(100);

                } catch (Exception e)
                {
                    text.setText("Status: Error getting the status.");
                    bar.setProgress(100);
                }

                enableButton();
            }
        }).start();
    }
}
