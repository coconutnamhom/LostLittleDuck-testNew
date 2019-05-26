package com.example.lostlittleduck.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleRssiCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.utils.HexUtil;
import com.example.lostlittleduck.MainActivity;
import com.example.lostlittleduck.R;

import java.util.ArrayList;
import java.util.List;

import static com.example.lostlittleduck.App.CHANNEL_ID;

public class NotiService extends Service {

    String input;
    final String UUID_SERVICE = "0000ffe0-0000-1000-8000-00805f9b34fb";
    final String UUID_WRITE = "0000ffe1-0000-1000-8000-00805f9b34fb";
    final String UUID_READ = "0000ffe1-0000-1000-8000-00805f9b34fb";
    final String UUID_NOTIFY = "0000ffe1-0000-1000-8000-00805f9b34fb";

    final String NOTI_1 = "< 3 meters";
    final String NOTI_2 = "3 - 5 meters";
    final String NOTI_3 = "> 5 meters";
    ArrayList<Integer> rssilist = new ArrayList<Integer>();
    int count = 0, avgrssi, total;



    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        input = intent.getStringExtra("noti(String noti_text)");

        //stopSelf();
//        handler.post(periodicUpdate);
        noti(input);

        return START_NOT_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void noti(String noti_text){

        Intent notificationIntent = new Intent(NotiService.this, MainActivity.class);
        notificationIntent.putExtra("menuFragment", "notiFragment");
        PendingIntent pendingIntent = PendingIntent.getActivity(NotiService.this,
                0, notificationIntent, 0);


        Notification notification = new NotificationCompat.Builder(NotiService.this, CHANNEL_ID)
                .setContentTitle("Notification")
                .setContentText(noti_text)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        } else {
            stopSelf();
        }


            startForeground(1, notification);

    }
}