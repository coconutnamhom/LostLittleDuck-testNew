package com.example.lostlittleduck.service;


import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

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

public class AwesomeJobIntentService extends JobIntentService {
    final Handler mHandler = new Handler();

    Handler handler = new Handler(); String input;
    final String UUID_SERVICE = "0000ffe0-0000-1000-8000-00805f9b34fb";
    final String UUID_WRITE = "0000ffe1-0000-1000-8000-00805f9b34fb";
    final String UUID_READ = "0000ffe1-0000-1000-8000-00805f9b34fb";
    final String UUID_NOTIFY = "0000ffe1-0000-1000-8000-00805f9b34fb";

    final String NOTI_1 = "< 3 meters";
    final String NOTI_2 = "3 - 5 meters";
    final String NOTI_3 = "> 5 meters";
    ArrayList<Integer> rssilist = new ArrayList<Integer>();
    int count = 0, avgrssi, total;

    private static final String TAG = "MyJobIntentService";
    /**
     * Unique job ID for this service.
     */
    private static final int JOB_ID = 2;

    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, AwesomeJobIntentService.class, JOB_ID, intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        showToast("Job Execution Started");
        handler.post(periodicUpdate);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        showToast("Job Execution Finished");
    }


    // Helper for showing tests
    void showToast(final CharSequence text) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(AwesomeJobIntentService.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Runnable periodicUpdate = new Runnable() {
        @Override
        public void run() {
            handler.postDelayed(periodicUpdate, 10 * 1000 - SystemClock.elapsedRealtime() % 1000);
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
                                        if (count >= 15) {
                                            total = 0;
                                            for (int i = 0; i < 15; i++) {
                                                total += rssilist.get(i);
                                                avgrssi = total / 15;

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

                        }

                    }
                } else {
//                     noti_text = "No Device is Connected";
                }
            } else {
//                noti_text = "No Device is Connected";
            }
            if(noti_text!=""){
                startNotiService(noti_text);
            }

        }
    };

    private Double calculateDistance(BleDevice bleDevice) {

//        int MeasurePower = -69; //hard coded power value. Usually ranges between -59 to -65
        int rssi = bleDevice.getRssi();
        if(rssi > -65){
            double distance = 0.00;
            return distance;
        } else if (rssi > -70 && rssi <= -65) {
            double distance = 1.00;
            return distance;
        } else if (rssi > -74 && rssi <= -70) {
            double distance = 2.00;
            return distance;
        }else if (rssi > -77 && rssi <= -74) {
            double distance = 3.00;
            return distance;
        }else if (rssi > -80 && rssi <= -77) {
            double distance = 4.00;
            return distance;
        }else if (rssi > -83 && rssi <= -80) {
            double distance = 5.00;
            return distance;
        }else if (rssi > -87 && rssi <= 83) {
            double distance = 6.00;
            return distance;
        }else {
            double distance = 7.00;
            return distance;
        }
    }

    public void startNotiService(String noti_text) {

        Intent serviceIntent = new Intent(this, NotiService.class);
        serviceIntent.putExtra("noti_text", noti_text);

        ContextCompat.startForegroundService(this, serviceIntent);
    }


}