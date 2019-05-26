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

//public class ExampleService extends Service implements SensorEventListener {
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

    /*private Double orientBle = -1.0;
    int mAzimuth,A;
    private SensorManager mSensorManager;
    private Sensor mRotationV, mAccelerometer, mMagnetometer;
    boolean haveSensor = false, haveSensor2 = false;
    float[] rMat = new float[9];
    float[] orientation = new float[3];
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;*/

    @Override
    public void onCreate() {
        super.onCreate();
       /* mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) == null) {
            if ((mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) || (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null)) {
                noSensorsAlert();
            }
            else {
                mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
                haveSensor = mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
                haveSensor2 = mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI);
            }
        }
        else{
            mRotationV = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            haveSensor = mSensorManager.registerListener(this, mRotationV, SensorManager.SENSOR_DELAY_UI);
        }*/
    }

    Handler handler = new Handler();
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
                            noti_text = bleDevice.getRssi() + " " + String.format("%.2f", distance) + " " + noti_text + noti_num_1 + " Device : Distance " + NOTI_1 + "\n";
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

                           /* BleManager.getInstance().notify(
                                    bleDevice,
                                    UUID_SERVICE,
                                    UUID_NOTIFY,
                                    new BleNotifyCallback() {
                                        @Override
                                        public void onNotifySuccess() {

                                        }

                                        @Override
                                        public void onNotifyFailure(BleException exception) {
                                            //point.setText(exception.toString());
                                        }

                                        @RequiresApi(api = Build.VERSION_CODES.O)
                                        @Override
                                        public void onCharacteristicChanged(byte[] data) {

                                            if(data.length > 1){
                                                String orientBle_ = (new String(data)).replaceAll("([A-Za-z\\t\\r\\n\\v\\f])", "");
                                                orientBle =  Double.parseDouble(orientBle_);
                                            }
                                        }
                                    });*/
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

                           /* BleManager.getInstance().notify(
                                    bleDevice,
                                    UUID_SERVICE,
                                    UUID_NOTIFY,
                                    new BleNotifyCallback() {
                                        @Override
                                        public void onNotifySuccess() {

                                        }

                                        @Override
                                        public void onNotifyFailure(BleException exception) {
                                            //point.setText(exception.toString());
                                        }

                                        @RequiresApi(api = Build.VERSION_CODES.O)
                                        @Override
                                        public void onCharacteristicChanged(byte[] data) {

                                            if(data.length > 1){
                                                String orientBle_ = (new String(data)).replaceAll("([A-Za-z\\t\\r\\n\\v\\f])", "");
                                                orientBle =  Double.parseDouble(orientBle_);
                                            }
                                        }
                                    });*/
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
                            Intent notificationIntent = new Intent(ExampleService.this, MainActivity.class);
                            notificationIntent.putExtra("menuFragment", "notiFragment");
                            PendingIntent pendingIntent = PendingIntent.getActivity(ExampleService.this,
                                    0, notificationIntent, 0);


                            /*BleManager.getInstance().notify(
                                    bleDevice,
                                    UUID_SERVICE,
                                    UUID_NOTIFY,
                                    new BleNotifyCallback() {
                                        @Override
                                        public void onNotifySuccess() {

                                        }

                                        @Override
                                        public void onNotifyFailure(BleException exception) {
                                            //point.setText(exception.toString());
                                        }

                                        @RequiresApi(api = Build.VERSION_CODES.O)
                                        @Override
                                        public void onCharacteristicChanged(byte[] data) {

                                            if(data.length > 1){
                                                String orientBle_ = (new String(data)).replaceAll("([A-Za-z\\t\\r\\n\\v\\f])", "");
                                                orientBle =  Double.parseDouble(orientBle_);
                                            }
                                        }
                                    });*/
                        }

                    }
                } else {
                    noti_text = "No Device is Connected";
                }
            } else {
                noti_text = "No Device is Connected";
            }


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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                stopForeground(true);
            } else {
                stopSelf();
            }

            startForeground(1, notification);

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
        /*if(haveSensor && haveSensor2){
            mSensorManager.unregisterListener(this,mAccelerometer);
            mSensorManager.unregisterListener(this,mMagnetometer);
        }
        else{
            if(haveSensor)
                mSensorManager.unregisterListener(this,mRotationV);
        }*/
        //BleManager.getInstance().stopNotify(bleDevice,UUID_SERVICE, UUID_NOTIFY);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

   /* @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rMat, event.values);
            mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 360) % 360;
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(rMat, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(rMat, orientation);
            mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 360) % 360;
        }

        mAzimuth = Math.round(mAzimuth);
        A=mAzimuth-90; //setให้เด็กหันหน้าไปทิศ90เสมอ
        if(A<0){
            A+=360;
        }
        //direc.setRotation(-A);

        if(orientBle != -1){
            int orientationDiff = calculate_orientation(mAzimuth, (int) Math.round(orientBle));
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }*/

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
}