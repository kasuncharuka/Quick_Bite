package lk.kcd.quickbite.helper;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class NetworkHelper {

    private static final String TAG = "NetworkHelper";


    public static final String PROMO_URL =
            "https://run.mocky.io/v3/your-mock-id-here";


    public interface Callback {
        void onSuccess(String responseBody);
        void onFailure(String errorMessage);
    }


    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler mainHandler   = new Handler(Looper.getMainLooper());


    public static void get(String urlString, Callback callback) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(urlString);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8_000);   // 8 s connect timeout
                conn.setReadTimeout(8_000);      // 8 s read timeout
                conn.setRequestProperty("Accept", "application/json");

                int code = conn.getResponseCode();
                Log.d(TAG, "GET " + urlString + " → HTTP " + code);

                if (code == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();
                    final String body = sb.toString();
                    mainHandler.post(() -> callback.onSuccess(body));
                } else {
                    final String err = "HTTP error: " + code;
                    mainHandler.post(() -> callback.onFailure(err));
                }

            } catch (IOException e) {
                Log.e(TAG, "Network error: " + e.getMessage());
                final String msg = e.getMessage();
                mainHandler.post(() -> callback.onFailure(msg));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }


    public static PromoData parsePromo(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            PromoData p = new PromoData();
            p.title    = obj.optString("title",    "Special Deal!");
            p.subtitle = obj.optString("subtitle", "Order now and save big.");
            p.code     = obj.optString("code",     "QUICKBITE");
            p.btnLabel = obj.optString("btnLabel", "Order Now →");
            return p;
        } catch (Exception e) {
            Log.e(TAG, "parsePromo: " + e.getMessage());
            return null;
        }
    }


    public static class PromoData {
        public String title;
        public String subtitle;
        public String code;
        public String btnLabel;
    }
}