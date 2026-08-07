package lk.kcd.quickbite.helper;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import lk.kcd.quickbite.R;
import lk.kcd.quickbite.activity.MainActivity;

public class NotificationHelper {


    private static final String CHANNEL_GENERAL  = "quickbite_general";
    private static final String CHANNEL_ORDERS   = "quickbite_orders";
    private static final String CHANNEL_PROMOS   = "quickbite_promos";


    public static final int NOTIF_WELCOME         = 1001;
    public static final int NOTIF_PAYMENT_SUCCESS = 1002;
    public static final int NOTIF_ORDER_QUEUED    = 1003;


    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm =
                    context.getSystemService(NotificationManager.class);


            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL_GENERAL,
                    "General",
                    NotificationManager.IMPORTANCE_DEFAULT));


            NotificationChannel ordersChannel = new NotificationChannel(
                    CHANNEL_ORDERS,
                    "Order Updates",
                    NotificationManager.IMPORTANCE_HIGH);
            ordersChannel.setDescription("Notifications about your orders");
            ordersChannel.enableVibration(true);
            nm.createNotificationChannel(ordersChannel);


            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL_PROMOS,
                    "Promotions",
                    NotificationManager.IMPORTANCE_LOW));
        }
    }


    public static void showWelcome(Context context, String userName) {
        PendingIntent pi = buildPendingIntent(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, CHANNEL_GENERAL)
                .setSmallIcon(R.drawable.home_24)
                .setContentTitle("Welcome to QuickBite! 🍔")
                .setContentText("Hi " + userName + "! Hungry? Let's find you something delicious.")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Hi " + userName + "! 👋\n"
                                + "Welcome to QuickBite — your favourite food, delivered fast.\n"
                                + "Browse our menu and place your order today!"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pi)
                .setAutoCancel(true);

        notify(context, NOTIF_WELCOME, builder);
    }


    public static void showPaymentSuccess(Context context, String orderId, double amount) {
        PendingIntent pi = buildPendingIntent(context);

        String amountStr = String.format(java.util.Locale.US, "LKR %,.2f", amount);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, CHANNEL_ORDERS)
                .setSmallIcon(R.drawable.shopping_cart_24)
                .setContentTitle("Payment Successful ✅")
                .setContentText("Your payment of " + amountStr + " was confirmed!")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("✅ Payment confirmed!\n"
                                + "Order: #" + orderId + "\n"
                                + "Amount: " + amountStr + "\n"
                                + "Your order is now being prepared. 🍳"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 300, 200, 300});

        notify(context, NOTIF_PAYMENT_SUCCESS, builder);
    }


    public static void showOrderInQueue(Context context, String orderId) {
        PendingIntent pi = buildPendingIntent(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, CHANNEL_ORDERS)
                .setSmallIcon(R.drawable.order_list_24)
                .setContentTitle("Order Confirmed! 🎉")
                .setContentText("Order #" + orderId + " is in the queue.")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("🎉 Your order is confirmed!\n"
                                + "Order #" + orderId + " has been placed and is in the queue.\n"
                                + "We'll notify you when it's on its way! 🛵"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 400, 200, 400});

        notify(context, NOTIF_ORDER_QUEUED, builder);
    }




    private static PendingIntent buildPendingIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }


    private static void notify(Context context, int id,
                               NotificationCompat.Builder builder) {
        try {
            NotificationManagerCompat.from(context).notify(id, builder.build());
        } catch (SecurityException e) {

        }
    }
}