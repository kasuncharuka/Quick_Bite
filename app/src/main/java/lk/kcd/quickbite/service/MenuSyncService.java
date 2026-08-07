package lk.kcd.quickbite.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import lk.kcd.quickbite.R;
import lk.kcd.quickbite.helper.NotificationHelper;



public class MenuSyncService extends Service {

    private static final String TAG              = "MenuSyncService";
    private static final String CHANNEL_SYNC     = "quickbite_sync";
    private static final int    NOTIF_SYNC_ID    = 3001;
    private static final long   SYNC_INTERVAL_S  = 60; // sync every 60 s


    public interface SyncListener {
        void onSyncComplete(int productCount);
    }
    private static SyncListener syncListener;
    public static void setSyncListener(SyncListener l)  { syncListener = l; }
    public static void removeSyncListener()             { syncListener = null; }


    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?>       syncTask;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());


    @Override
    public void onCreate() {
        super.onCreate();
        createSyncChannel();
        startForeground(NOTIF_SYNC_ID, buildForegroundNotification("Syncing menu…"));
        Log.d(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand → starting scheduler");
        scheduler = Executors.newSingleThreadScheduledExecutor();
        syncTask  = scheduler.scheduleAtFixedRate(
                this::syncMenu, 0, SYNC_INTERVAL_S, TimeUnit.SECONDS);
        // Restart if killed by system
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (syncTask != null)  syncTask.cancel(true);
        if (scheduler != null) scheduler.shutdownNow();
        Log.d(TAG, "Service destroyed");
    }

    @Nullable @Override
    public IBinder onBind(Intent intent) { return null; } // Not a bound service


    private void syncMenu() {
        Log.d(TAG, "Syncing menu from Firestore…");
        updateNotification("Syncing menu…");

        FirebaseFirestore.getInstance()
                .collection("products")
                .get()
                .addOnSuccessListener(qds -> {
                    int count = qds.size();
                    Log.d(TAG, "Sync complete — " + count + " products");
                    updateNotification("Menu up to date ✅  (" + count + " items)");

                    // Notify listener on main thread
                    if (syncListener != null) {
                        mainHandler.post(() -> syncListener.onSyncComplete(count));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Sync failed: " + e.getMessage());
                    updateNotification("Sync failed — will retry");
                });
    }


    private void createSyncChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_SYNC, "Menu Sync", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Background menu synchronisation");
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(ch);
        }
    }

    private Notification buildForegroundNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_SYNC)
                .setSmallIcon(R.drawable.order_list_24)
                .setContentTitle("QuickBite")
                .setContentText(text)
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification(String text) {
        Notification n = buildForegroundNotification(text);
        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIF_SYNC_ID, n);
    }
}