package lk.kcd.quickbite.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;


public class NetworkChangeReceiver extends BroadcastReceiver {

    private static final String TAG = "NetworkChangeReceiver";


    public interface NetworkListener {

        void onNetworkChanged(boolean isConnected, String networkType);
    }


    private static NetworkListener listener;

    public static void setListener(NetworkListener l) {
        listener = l;
    }

    public static void removeListener() {
        listener = null;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) return;

        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) return;

        NetworkInfo activeInfo = cm.getActiveNetworkInfo();
        boolean connected = (activeInfo != null && activeInfo.isConnected());
        String type = "None";

        if (connected) {
            switch (activeInfo.getType()) {
                case ConnectivityManager.TYPE_WIFI:
                    type = "WiFi";
                    break;
                case ConnectivityManager.TYPE_MOBILE:
                    type = "Mobile";
                    break;
                default:
                    type = "Other";
            }
        }

        Log.d(TAG, "Network changed → connected=" + connected + ", type=" + type);

        if (listener != null) {
            listener.onNetworkChanged(connected, type);
        }
    }


    public static boolean isConnected(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }
}