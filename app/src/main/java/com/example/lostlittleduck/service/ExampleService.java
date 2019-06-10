package com.example.lostlittleduck.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.clj.fastble.callback.BleNotifyCallback;
import com.example.lostlittleduck.R;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleRssiCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.utils.HexUtil;
import com.example.lostlittleduck.MainActivity;

import java.util.ArrayList;
import java.util.List;

import static com.example.lostlittleduck.App.CHANNEL_ID;

public class ExampleService extends Service {

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

    Handler handler = new Handler();
    private Runnable periodicUpdate = new Runnable() {
        @Override
        public void run() {
            handler.postDelayed(periodicUpdate, 1 * 1000 - SystemClock.elapsedRealtime() % 1000);
            // whatever you want to do below
            //Toast. makeText(ExampleService.this,"10 second" , Toast.LENGTH_SHORT).show();

            List<BleDevice> deviceConnectedList = BleManager.getInstance().getAllConnectedDevice();
            String noti_text = "";

            if (deviceConnectedList != null) {
                if (deviceConnectedList.size() > 0) {

                    int noti_num_1 = 0;
                    int noti_num_2 = 0;
                    int noti_num_3 = 0;

                    for (BleDevice device : deviceConnectedList) {

                        final BleDevice bleDevice = device;

                        BleManager.getInstance().readRssi(
                                device,
                                new BleRssiCallback() {

                                    @Override
                                    public void onRssiFailure(BleException exception) {

                                    }

                                    @Override
                                    public void onRssiSuccess(int rssi) {
                                        bleDevice.setRssi(rssi);
                                        rssilist.add(rssi);
                                        count += 1;
                                        if (count >= 12) {
                                            total = 0;
                                            for (int i = 0; i < 12; i++) {
                                                total += rssilist.get(i);
                                                avgrssi = total / 12;

                                            }
                                            bleDevice.setRssi(avgrssi);
                                            rssilist.remove(0);
                                        }

                                    }
                                });

                        Double distance = calculateDistance(bleDevice);

                        if (distance >= 0 && distance < 3) {
                            noti_num_1++;
//                            noti_text = bleDevice.getRssi() + " " + String.format("%.2f", distance) + " " + noti_text + noti_num_1 + " Device : Distance " + NOTI_1 + "\n";
                            //write
                            BleManager.getInstance().write(
                                    device,
                                    UUID_SERVICE,
                                    UUID_WRITE,
                                    HexUtil.hexStringToBytes("31"),
                                    new BleWriteCallback() {
                                        @Override
                                        public void onWriteSuccess(int current, int total, byte[] justWrite) {
                                        /*Toast.makeText(ExampleService.this,"write success, current: " + current
                                                + " total: " + total
                                                + " justWrite: " + HexUtil.formatHexString(justWrite, true) , Toast.LENGTH_SHORT).show();*/
                                        }

                                        @Override
                                        public void onWriteFailure(BleException exception) {
                                            /*Toast.makeText(ExampleService.this,exception.toString(), Toast.LENGTH_SHORT).show();*/
                                        }
                                    });

                        } else if (distance >= 3 && distance < 5) {

                            noti_num_2++;
                            noti_text = bleDevice.getRssi() + " " + String.format("%.2f", distance) + " " + noti_text + noti_num_2 + " Device : Distance " + NOTI_2 + "\n";
                            BleManager.getInstance().write(
                                    device,
                                    UUID_SERVICE,
                                    UUID_WRITE,
                                    HexUtil.hexStringToBytes("32"),
                                    new BleWriteCallback() {
                                        @Override
                                        public void onWriteSuccess(int current, int total, byte[] justWrite) {
                                        /*Toast.makeText(ExampleService.this,"write success, current: " + current
                                                + " total: " + total
                                                + " justWrite: " + HexUtil.formatHexString(justWrite, true) , Toast.LENGTH_SHORT).show();*/
                                        }

                                        @Override
                                        public void onWriteFailure(BleException exception) {
                                            /*Toast.makeText(ExampleService.this,exception.toString(), Toast.LENGTH_SHORT).show();*/
                                        }
                                    });


//                            noti(noti_text);

                        } else if (distance > 5) {
                            noti_num_3++;
                            noti_text = bleDevice.getRssi() + " " + String.format("%.2f", distance) + noti_text + noti_num_3 + " Device : Distance " + NOTI_3 + "\n";
                            BleManager.getInstance().write(
                                    device,
                                    UUID_SERVICE,
                                    UUID_WRITE,
                                    HexUtil.hexStringToBytes("33"),
                                    new BleWriteCallback() {
                                        @Override
                                        public void onWriteSuccess(int current, int total, byte[] justWrite) {
                                        /*Toast.makeText(ExampleService.this,"write success, current: " + current
                                                + " total: " + total
                                                + " justWrite: " + HexUtil.formatHexString(justWrite, true) , Toast.LENGTH_SHORT).show();*/
                                        }

                                        @Override
                                        public void onWriteFailure(BleException exception) {
                                            /*Toast.makeText(ExampleService.this,exception.toString(), Toast.LENGTH_SHORT).show();*/
                                        }
                                    });
//                            noti(noti_text);


                        }

                    }
                } else {
//                     noti_text = "No Device is Connected";
                }
            } else {
//                noti_text = "No Device is Connected";
            }
//
//<<<<<<< Updated upstream
            noti(noti_text);
//=======
            noti(noti_text);
//>>>>>>> Stashed changes
        }

    };

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        input = intent.getStringExtra("inputExtra");

        //stopSelf();
        handler.post(periodicUpdate);

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


    private Double calculateDistance(BleDevice bleDevice) {


        int MeasurePower = -60; //hard coded power value. Usually ranges between -59 to -65
        int rssi = bleDevice.getRssi();

        if (rssi == 0) {
            return -1.0;
        }
        Double ratio = (MeasurePower - rssi) / 20.00;
        Double distance = Math.pow(10, ratio);
        return distance;
    }

//        int rssi = bleDevice.getRssi();
//        if (rssi > -65) {
//            double distance = 0.00;
//            return distance;
//        } else if (rssi > -70 && rssi <= -65) {
//            double distance = 1.00;
//            return distance;
//        } else if (rssi > -73 && rssi <= -72) {
//            double distance = 2.00;
//            return distance;
//        } else if (rssi > -77 && rssi <= -73) {
//            double distance = 3.00;
//            return distance;
//        } else if (rssi > -80 && rssi <= -77) {
//            double distance = 4.00;
//            return distance;
//        } else if (rssi > -83 && rssi <= -80) {
//            double distance = 5.00;
//            return distance;
//        } else if (rssi > -87 && rssi <= 83) {
//            double distance = 6.00;
//            return distance;
//        } else {
//            double distance = 7.00;
//            return distance;
//        }
//    }


    private void noti(String noti_text){

        Intent notificationIntent = new Intent(ExampleService.this, MainActivity.class);
        notificationIntent.putExtra("menuFragment", "notiFragment");
        PendingIntent pendingIntent = PendingIntent.getActivity(ExampleService.this,
                0, notificationIntent, 0);


        Notification notification = new NotificationCompat.Builder(ExampleService.this, CHANNEL_ID)
                .setContentTitle("Notification")
                .setContentText(noti_text)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();

//<<<<<<< Updated upstream
//        notification.priority = Notification.PRIORITY_MIN;
//=======
        notification.priority = Notification.PRIORITY_MIN;
//>>>>>>> Stashed changes

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        } else {
            stopSelf();
        }

        if(noti_text!="") {
            startForeground(1, notification);
        }else {
            startForeground(15, notification);
        }

    }
}