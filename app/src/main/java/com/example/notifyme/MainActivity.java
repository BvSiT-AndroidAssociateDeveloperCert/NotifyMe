package com.example.notifyme;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String PRIMARY_CHANNEL_ID = "primary_notification_channel";
    private static final int NOTIFICATION_ID = 0;
    private Button button_notify;
    private Button button_cancel;
    private Button button_update;
    private NotificationManager mNotifyManager;

    private NotificationReceiver mReceiver = new NotificationReceiver();
    private static String ACTION_NOTIFICATION_UPDATE="com.example.notifyme.ACTION_NOTIFICATION_UPDATE";
    private static String ACTION_SEND_NOTIFICATION_REMOVED="com.example.notifyme.ACTION_NOTIFICATION_REMOVED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createNotificationChannel(); //without this app crashes!
        button_notify = findViewById(R.id.notify);
        button_notify.setOnClickListener(v->sendNotification());
        button_cancel = findViewById(R.id.cancel);
        button_cancel.setOnClickListener(v->cancelNotification());
        button_update = findViewById(R.id.update);
        button_update.setOnClickListener(v->updateNotification());
        setNotificationButtonState(true,false,false);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_NOTIFICATION_UPDATE);
        intentFilter.addAction(ACTION_SEND_NOTIFICATION_REMOVED);
        registerReceiver(mReceiver,intentFilter);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mReceiver); //Sic. Must be before calling super?
        super.onDestroy();
    }

    private NotificationCompat.Builder getNotificationBuilder(){

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this,NOTIFICATION_ID,notificationIntent,PendingIntent.FLAG_UPDATE_CURRENT); //!!

        Intent updateIntent = new Intent(ACTION_NOTIFICATION_UPDATE);
        PendingIntent updatePendingIntent= PendingIntent.getBroadcast(this,NOTIFICATION_ID,updateIntent,PendingIntent.FLAG_ONE_SHOT);

        Intent sendRemovedIntent = new Intent(ACTION_SEND_NOTIFICATION_REMOVED);
        PendingIntent sendRemovedPendingIntent= PendingIntent.getBroadcast(this,NOTIFICATION_ID,sendRemovedIntent,PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder notifyBuilder =
                new NotificationCompat.Builder(this, PRIMARY_CHANNEL_ID)
            .setContentTitle("You've been notified!")
            .setContentText("This is your notification text.")
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setUsesChronometer(true)
            //backward compat <=7.1
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

            //In org. version action is added in sendNotification
            .addAction(R.drawable.ic_update,"Update Notification",updatePendingIntent)
            .setDeleteIntent(sendRemovedPendingIntent);

                ;
        return  notifyBuilder;
    }

    private void createNotificationChannel(){
        mNotifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT>=26){
            NotificationChannel notificationChannel =
                    new NotificationChannel(PRIMARY_CHANNEL_ID,"Mascot Notification",NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            notificationChannel.setDescription("Notification from Mascot");
            mNotifyManager.createNotificationChannel(notificationChannel);
        }
    }

    private void sendNotification(){
        Notification notification = getNotificationBuilder().build();
        mNotifyManager.notify(NOTIFICATION_ID,notification);
        setNotificationButtonState(false,true,true);
    }

    private void updateNotification(){
        Bitmap androidImage = BitmapFactory
                .decodeResource(getResources(),R.drawable.mascot_1);

        // BvS I made an error which was difficult to debug:
        //NotificationCompat.Builder notifyBuilder =
        //        new NotificationCompat.Builder(this, PRIMARY_CHANNEL_ID)
        //This is wrong because we need to add at least .setSmallIcon
        //Instead use the helper method

        // Build the notification with all of the parameters using helper
        // method.
        NotificationCompat.Builder notifyBuilder = getNotificationBuilder();

        notifyBuilder.setStyle(
                new NotificationCompat.BigPictureStyle()
                    .bigPicture(androidImage)
                    .setBigContentTitle("Notification Updated!")
        );
        mNotifyManager.notify(NOTIFICATION_ID,notifyBuilder.build());
        setNotificationButtonState(false,true,true);
    }

    private void cancelNotification(){
        mNotifyManager.cancel(NOTIFICATION_ID);
        setNotificationButtonState(true,false,false);
    }

    /**
     * Helper method to enable/disable the buttons.
     *
     * @param isNotifyEnabled, boolean: true if notify button enabled
     * @param isUpdateEnabled, boolean: true if update button enabled
     * @param isCancelEnabled, boolean: true if cancel button enabled
     */
    void setNotificationButtonState(Boolean isNotifyEnabled, Boolean
            isUpdateEnabled, Boolean isCancelEnabled) {
        button_notify.setEnabled(isNotifyEnabled);
        button_update.setEnabled(isUpdateEnabled);
        button_cancel.setEnabled(isCancelEnabled);
    }

    class NotificationReceiver extends BroadcastReceiver{
        NotificationReceiver(){}
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals(ACTION_NOTIFICATION_UPDATE)){
                updateNotification();
            } else if (intent.getAction().equals(ACTION_SEND_NOTIFICATION_REMOVED)) {
                Toast.makeText(context,"Notfication removed",Toast.LENGTH_SHORT).show();
                setNotificationButtonState(true,false,false);
            }
        }
    }
}
