package shehryar.paighaam;

import android.app.NotificationChannel;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class NotificationManager {

    final static int NOTIFICATION_ID = 0;
    final static String NOTIFICATION_CHANNEL = "Channel Name";

    private NotificationCompat.Builder builder;

    Context context;
    String total;
    android.app.NotificationManager notificationManager;

    public NotificationManager(Context context, String total, android.app.NotificationManager notificationManager) {
        this.context = context;
        this.total = total;
        this.notificationManager = notificationManager;
    }

    public void createNewNotification() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = android.app.NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL, "App Name", importance);
            channel.setDescription("Description");
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(channel);
        }

        builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)
                .setContentText("0/".concat(total))
                .setContentTitle("Sending Messages")
                .setSmallIcon(R.drawable.ic_message_black_24dp)
                .setAutoCancel(false)
                .setOngoing(true)
                .setOnlyAlertOnce(true);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public void updateText(String content) {
        builder.setContentText(content);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public void removeNotification() {
        notificationManager.cancelAll();
    }

}
