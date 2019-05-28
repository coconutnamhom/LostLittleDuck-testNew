package com.example.lostlittleduck;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleRssiCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.utils.HexUtil;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Timer;
import java.util.TimerTask;

public class DeviceDetail extends AppCompatActivity implements SensorEventListener {

    public static final String KEY_DATA = "key_data";
    private BleDevice bleDevice;

    private TextView id_device;
    //    private TextView point;
    private TextView distance;
    private ImageView direc;

    int mAzimuth, Q;
    private SensorManager mSensorManager;
    private Sensor mRotationV, mAccelerometer, mMagnetometer;
    boolean haveSensor = false, haveSensor2 = false;
    float[] rMat = new float[9];
    float[] orientation = new float[3];
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;

    private Timer timer;

    /*public String UUID_SERVICE="6E400001-B5A3-F393-E0A9-E50E24DCCA9E";
    public String UUID_WRITE= "6E400002-B5A3-F393-E0A9-E50E24DCCA9E";
    public String UUID_READ=   "6E400003-B5A3-F393-E0A9-E50E24DCCA9E";*/

    final String UUID_SERVICE = "0000ffe0-0000-1000-8000-00805f9b34fb";
    final String UUID_WRITE = "0000ffe1-0000-1000-8000-00805f9b34fb";
    final String UUID_READ = "0000ffe1-0000-1000-8000-00805f9b34fb";
    final String UUID_NOTIFY = "0000ffe1-0000-1000-8000-00805f9b34fb";

    private Double orientBle = -1.0;
    private Double distanceApp = 0.0;

    final String SHARED_PREFERENCES_FILE_NAME = "orientation";
    final int SAFEZONE_DISTANCE = 3;

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    ArrayList<Integer> rssilist = new ArrayList<Integer>();
    int count = 0, avgrssi, total;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        direc = (ImageView) findViewById(R.id.direction);

        initData();
        start();

        timer = new Timer();
        setTimer();

        sharedPreferences = this.getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, MODE_PRIVATE);
        editor = sharedPreferences.edit();

        editor.putBoolean("isFirstTime", true);
        editor.putInt("zeta_appOld", 0);
        editor.putInt("orientationMobileOld", 0);
        editor.putLong("distanceOld", Double.doubleToRawLongBits(0.0));

        editor.apply();
    }

    private void initData() {
        bleDevice = getIntent().getParcelableExtra(KEY_DATA);
        if (bleDevice == null)
            finish();

        id_device = (TextView) findViewById(R.id.id_device);
        id_device.setText(bleDevice.getName());

//        point = (TextView) findViewById(R.id.points);
//        int point_ = calculate_point(bleDevice);

        distance = (TextView) findViewById(R.id.distance);


    }

    private void setTimer() {
        //Set the schedule function
        timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {

                DeviceDetail.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(DeviceDetail.this,"2 second",Toast.LENGTH_SHORT).show();
                    }
                });

                BleManager.getInstance().readRssi(
                        bleDevice,
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
                                Double distance_ = calculate_distance(bleDevice);
                                distance.setText(String.format("%.2f", distance_) + " meters , rssi =" + bleDevice.getRssi());
                                distanceApp = distance_;
                            }
                        });

                BleManager.getInstance().notify(
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
                                /*point.setText(new String(HexUtil.formatHexString(data, true)));
                                if(data.length > 1){
                                    String o = new String(data);
                                    point.setText(o);
                                    String orientBle_ = o.replaceAll("([A-Za-z\\t\\r\\n\\v\\f])", "");
                                    String empty_ = new String("");
                                    if(empty_.equals(orientBle_)){
                                        point.setText(orientBle_);
                                        orientBle =  Double.parseDouble(orientBle_);
                                    }

                                }*/

                                if (data.length > 2) {
//                                    point.setText(new String(data));
                                    String orientBle_ = (new String(data)).replaceAll("([A-Za-z\\t\\r\\n\\v\\f])", "");
                                    orientBle = Double.parseDouble(orientBle_);
                                }
                            }
                        });

                if (orientBle != -1) {
                    DeviceDetail.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int zeta_app = calculate_orientation(mAzimuth, (int) Math.round(orientBle), distanceApp);
                            zeta_app = zeta_app;

//                            A = orientationDiff;
//                            if(A<0){
//                                A+=360;
//                            }


                            if (zeta_app >= 337.5 && zeta_app < 360 || zeta_app >= 0 && zeta_app < 22.5) {
                                Q = 0;
                            } else if (zeta_app >= 22.5 && zeta_app < 67.5) {
                                Q = 45;
                            } else if (zeta_app >= 67.5 && zeta_app < 112.5) {
                                Q = 90;
                            } else if (zeta_app >= 112.5 && zeta_app < 157.5) {
                                Q = 135;
                            } else if (zeta_app >= 157.5 && zeta_app < 202.5) {
                                Q = 180;
                            } else if (zeta_app >= 202.5 && zeta_app < 247.5) {
                                Q = 225;
                            } else if (zeta_app >= 247.5 && zeta_app < 292.5) {
                                Q = 270;
                            } else {
                                Q = 315;
                            }

                            direc.setRotation(Q);
                        }
                    });
                }

            }
        }, 0, 1000);   // 1000 Millisecond  = 1 second
    }
//
//   private int calculate_point(BleDevice bleDevice){
//        return bleDevice.getRssi();
//
//   }

    private Double calculate_distance(BleDevice bleDevice) {
        int MeasurePower = -60; //hard coded power value. Usually ranges between -59 to -65
        int rssi = bleDevice.getRssi();

        if (rssi == 0) {
            return -1.0;
        }
        Double ratio = (MeasurePower - rssi) / 20.00;
        Double distance = Math.pow(10, ratio);
        return distance;

//        int rssi = bleDevice.getRssi();
//        if (rssi > -53) {
//            double distance = 0.00;
//            return distance;
//        } else if (rssi > -71 && rssi <= -53) {
//            double distance = 3.00;
//            return distance;
//        } else {
//            double distance = 5.00;
//            return distance;
//        }
    }

// else if (rssi > -77 && rssi <= -73) {
//            double distance = 3.00;
//                return distance;
//        }else if (rssi > -80 && rssi <= -77) {
//            double distance = 4.00;
//                return distance;
//        }else if (rssi > -83 && rssi <= -80) {
//            double distance = 5.00;
//                return distance;
//        }else if (rssi > -87 && rssi <= 83) {
//            double distance = 6.00;
//                return distance;
//        }else {
//            double distance = 7.00;
//                return distance;
//        }


//    private int calculate_orientation(int orientationMobile,int orientationBLE, Double distance){
//
//        int zeta_app = 0;
//
//        Boolean isSafeZone = sharedPreferences.getBoolean("isSafeZone",true);
//
////        distance = 7.0;
////        Double distanceOld = 5.0;
////
////        orientationMobile = 0;
////        orientationBLE = 120;
//
//        if(distance > SAFEZONE_DISTANCE){
//
//            if(isSafeZone){
//                int zetaDiffOld = orientationBLE - orientationMobile;
//                Double distanceOld = distance;
//
//                editor.putInt("zetaDiffOld",zetaDiffOld);
//                editor.putInt("zetaDiffOld",90);
//                //editor.putLong("distanceOld", Double.doubleToRawLongBits(distanceOld));
//                editor.putLong("distanceOld", Double.doubleToRawLongBits(distance));
//                editor.putInt("orientationBleOld",orientationBLE);
////                editor.apply();
//            }
//            else{
//                int orientationBleOld = sharedPreferences.getInt("orientationBleOld",-1);
////                int orientationBleOld = 90;
//                int zetaDiffOld = sharedPreferences.getInt("zetaDiffOld",-1);
//                Double distanceOld = Double.longBitsToDouble(sharedPreferences.getLong("distanceOld", Double.doubleToLongBits(0)));
//
//                if(orientationBLE > orientationBleOld){
//                    if(distanceOld < distance){
//                        // zetaDiifBle = (int) Math.acos(distanceOld/distance);
//                        int zetaDiifBle = (int) Math.toDegrees(Math.acos(distanceOld/distance));
//
//                        zetaDiifBle = zetaDiifBle;
//                        zeta_app = zetaDiffOld - zetaDiifBle;
//                        editor.putInt("zetaDiffOld",zeta_app);
//                        //editor.apply();
//                    }
//                    else{
//
//                    }
//                }
//                else if(orientationBLE < orientationBleOld){
//                    if(distanceOld < distance){
//                        int zetaDiifBle = (int) Math.acos(distanceOld/distance);
//                        zeta_app = zetaDiffOld + zetaDiifBle;
//                        editor.putInt("zetaDiffOld",zeta_app);
//                        //editor.apply();
//                    }
//                    else{
//
//                    }
//                }
//                else if(zetaDiffOld == orientationBLE){
//                    zeta_app = zetaDiffOld;
//                }
//
//
//            }
//
//            editor.putBoolean("isSafeZone",false);
//            //editor.apply();
//        }
//        else{
//            editor.putBoolean("isSafeZone",true);
//            //editor.apply();
//        }
//
//        editor.apply();
//
//        return zeta_app;
//    }


    private int calculate_orientation(int orientationMobile, int orientationBLE, Double distance) {

        int zeta_app = 0;
        int zeta_appOld = sharedPreferences.getInt("zeta_appOld", 0);
        Double distanceOld;
        Boolean isFirstTime = sharedPreferences.getBoolean("isFirstTime", true); //ควรไว้ตรงไหน?
        if (distance >= SAFEZONE_DISTANCE) {
            int orientationMobileOld = sharedPreferences.getInt("orientationMobileOld", 0);

            distanceOld = distance;

            if (orientationMobile > orientationBLE) {
                int zetaOld = Math.abs(orientationBLE - orientationMobile);
                zetaOld = 360 - zetaOld;
                editor.putInt("zetaOld", zetaOld);
                editor.apply();
            } else {
                int zetaOld = Math.abs(orientationBLE - orientationMobile);
                editor.putInt("zetaOld", zetaOld);
                editor.apply();
            }


            if (distance >= distanceOld) {
                if (isFirstTime) {
                    int zetaOld = sharedPreferences.getInt("zetaOld", -1);
                    int A = orientationMobileOld - 180, B = orientationBLE - 23, C = orientationBLE + 23;
//                    if (B < orientationMobile || orientationMobile < C) {
//                        int D = 1;
//                    }
                    if ((orientationMobile < A) && (B > orientationMobile || orientationMobile > C)) {
                        int orientationMobile_2 = orientationMobile - 180;
                        int zetaNew = Math.abs(orientationBLE - orientationMobile_2);
                        int zetaAvg = Math.abs((zetaOld + zetaNew) / 2);
                        zeta_app = Math.abs(zetaAvg - (orientationMobile - orientationMobile_2));
//                        if (orientationMobile > zeta_app) {
////                            int zeta_app2 = 360 - zeta_app;
////                            zeta_app = zeta_app2;
////                        }

                        editor.putInt("zetaAvg", zetaAvg);
                        editor.putLong("distanceOld", Double.doubleToRawLongBits(distance));
                        editor.putInt("orientationMobileOld", orientationMobile);
                        editor.putInt("zeta_appOld", zeta_app);
                        editor.putBoolean("isFirstTime", false);
                        editor.putInt("orientationMobileOld", orientationMobile);
                        editor.putLong("distanceOld", Double.doubleToRawLongBits(distanceOld));
                        editor.apply();
                        return zeta_app;

                    } else if ((B > orientationMobile || orientationMobile > C)) {
                        int zetaNew = Math.abs(orientationBLE - orientationMobile);
                        int zetaAvg = Math.abs((zetaOld + zetaNew) / 2);
                        zeta_app = Math.abs(zetaAvg - (orientationMobile - orientationMobile));
                        if (orientationMobile > zeta_app) {
                            int zeta_app2 = 360 - zeta_app;
                            zeta_app = zeta_app2;
                        }
                        editor.putInt("zetaAvg", zetaAvg);
                        editor.putLong("distanceOld", Double.doubleToRawLongBits(distance));
                        editor.putInt("orientationMobileOld", orientationMobile);
                        editor.putInt("zeta_appOld", zeta_app);
                        editor.putBoolean("isFirstTime", false);
                        editor.putInt("orientationMobileOld", orientationMobile);
                        editor.putLong("distanceOld", Double.doubleToRawLongBits(distanceOld));
                        editor.apply();
                        return zeta_app;

                    } else {
                        zeta_app = 0;
                        editor.putInt("zetaAvg", 0);
                        editor.putInt("orientationMobileOld", orientationMobile);
                        editor.putLong("distanceOld", Double.doubleToRawLongBits(distance));
                        editor.putInt("zeta_appOld", zeta_app);
                        editor.putBoolean("isFirstTime", false);
                        editor.putInt("orientationMobileOld", orientationMobile);
                        editor.putLong("distanceOld", Double.doubleToRawLongBits(distanceOld));
                        editor.apply();
                        return zeta_app;
                    }
//                    }
                } else {
                    int zetaAvg = sharedPreferences.getInt("zetaAvg", 0);
                    int A = orientationMobileOld - 180, B = zetaAvg - 23, C = zetaAvg + 23;
                    if ((orientationMobile < A) && (B > orientationMobile || orientationMobile > C)) {
                        int orientationMobileOld_2 = orientationMobileOld - 180;
                        int zetaOld = zeta_appOld;
                        int zetaNew = Math.abs(orientationBLE - orientationMobileOld_2);
                        zetaAvg = Math.abs((zetaOld + zetaNew) / 2);
                        zeta_app = Math.abs(zetaAvg - (orientationMobile - orientationMobileOld_2));
                        if (orientationMobile > zeta_app) {
                            int zeta_app2 = 360 - zeta_app;
                            zeta_app = zeta_app2;
                        }

                        editor.putInt("zetaAvg", zetaAvg);
                        editor.putLong("distanceOld", Double.doubleToRawLongBits(distance));
                        editor.putInt("zeta_appOld", zeta_app);
                        editor.putInt("orientationMobileOld", orientationMobile);
                        editor.apply();
                        return zeta_app;
                    } else if ((B > orientationMobile || orientationMobile > C)) {
                        int zetaOld = zeta_appOld;
                        int zetaNew = Math.abs(orientationBLE - orientationMobileOld);
                        zetaAvg = Math.abs((zetaOld + zetaNew) / 2);
                        zeta_app = Math.abs(zetaAvg - (orientationMobile - orientationMobileOld));
                        if (orientationMobile > zeta_app) {
                            int zeta_app2 = 360 - zeta_app;
                            zeta_app = zeta_app2;
                        }
                            editor.putInt("zetaAvg", zetaAvg);
                            editor.putLong("distanceOld", Double.doubleToRawLongBits(distance));
                            editor.putInt("orientationMobileOld", orientationMobile);
                            editor.putInt("zeta_appOld", zeta_app);
                            editor.putBoolean("isFirstTime", false);
                        editor.putInt("orientationMobileOld", orientationMobile);
                            editor.apply();
                            return zeta_app;
                        }

                     else {
                        zeta_app = 0;
                        editor.putInt("zetaAvg", zetaAvg);
                        editor.putLong("distanceOld", Double.doubleToRawLongBits(distance));
                        editor.putInt("orientationMobileOld", orientationMobile);
                        editor.putInt("zeta_appOld", zeta_app);
                        editor.putBoolean("isFirstTime", false);
                        editor.putInt("orientationMobileOld", orientationMobile);
                        editor.apply();
                        return zeta_app;
                    }


                }
            } else {
                int zetaAvg = sharedPreferences.getInt("zetaAvg", 0);
                if (distance < SAFEZONE_DISTANCE) {
                    editor.putBoolean("isFirstTime", true);
                } else {
                    zeta_app = Math.abs(zetaAvg - (orientationMobile - orientationMobileOld));
                    editor.putInt("zetaAvg", zetaAvg);
                    editor.putInt("orientationMobileOld", orientationMobile);
                    editor.putLong("distanceOld", Double.doubleToRawLongBits(distance));
                    editor.putInt("zeta_appOld", zeta_app);
                    editor.putBoolean("isFirstTime", false);
                    editor.putInt("orientationMobileOld", orientationMobile);
                    editor.apply();
                    return zeta_app;
                }
            }

        }
//        editor.apply();

            return zeta_app;

        }

    @Override
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
//        int zeta_app=30; //ยังไม่ได้ดึงค่า
//        if (zeta_app >= 337.5 && zeta_app < 360 || zeta_app >= 0 && zeta_app < 22.5) {
////            A = 0;
//        } else if (zeta_app >= 22.5 && zeta_app < 67.5) {
//            A = 45;
//        } else if (zeta_app >= 67.5 && zeta_app < 112.5) {
//            A = 90;
//        } else if (zeta_app >= 112.5 && zeta_app < 157.5) {
//            A = 135;
//        } else if (zeta_app >= 157.5 && zeta_app < 202.5) {
//            A = 180;
//        }else if(zeta_app >= 202.5 && zeta_app < 247.5 ) {
//            A = 225;
//        }else if(zeta_app >= 247.5 && zeta_app < 292.5 ) {
//            A = 270;
//        }else  {
//            A = 315;
//        }
//        direc.setRotation(-A);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void start() {
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
        }
    }
    public void noSensorsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage("Your device doesn't support the Compass.")
                .setCancelable(false)
                .setNegativeButton("Close",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        alertDialog.show();
    }
    public void stop() {
        if(haveSensor && haveSensor2){
            mSensorManager.unregisterListener(this,mAccelerometer);
            mSensorManager.unregisterListener(this,mMagnetometer);
        }
        else{
            if(haveSensor)
                mSensorManager.unregisterListener(this,mRotationV);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        setTimer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stop();
        timer.cancel();
        BleManager.getInstance().stopNotify(bleDevice,UUID_SERVICE, UUID_NOTIFY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        start();
        initData();
        setTimer();
    }

    @Override
    protected void onStop() {
        super.onStop();
        timer.cancel();
        BleManager.getInstance().stopNotify(bleDevice,UUID_SERVICE, UUID_NOTIFY);
    }
}
