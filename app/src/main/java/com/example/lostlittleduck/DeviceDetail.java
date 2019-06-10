package com.example.lostlittleduck;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
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
                                if(distance_ >= 0 && distance_ < 3){
                                    distance.setText("SAFE ZONE");
                                    distance.setTextColor(Color.parseColor("#6df200"));

                                }else if(distance_ >= 3 && distance_ < 5){
                                    distance.setText("WARNING ZONE");
                                    distance.setTextColor(Color.parseColor("#ffae00"));

                                } else if(distance_ > 5){
                                    distance.setText("DANGER ZONE");
                                    distance.setTextColor(Color.RED);

                                }


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


    private Double calculate_distance(BleDevice bleDevice) {
        int MeasurePower = -60; //hard coded power value. Usually ranges between -59 to -65
        int rssi = bleDevice.getRssi();

        if (rssi == 0) {
            return -1.0;
        }
        Double ratio = (MeasurePower - rssi) / 20.00;
        Double distance = Math.pow(10, ratio);
        return distance;
    }




    private int calculate_orientation(int orientationMobile, int orientationBLE, Double distance) {

        int zeta_app = 0;
        int zeta_appOld = sharedPreferences.getInt("zeta_appOld", 0);
        Double distanceOld;
        Boolean isFirstTime = sharedPreferences.getBoolean("isFirstTime", true);
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
                    int A = orientationMobileOld - 180, B = orientationBLE - 15, C = orientationBLE + 15;
                    if ((orientationMobile > orientationBLE)&&(B > orientationMobile || orientationMobile > C)) {
                        int zetaNew = Math.abs(orientationBLE - orientationMobile);
                        zetaNew = 360 - zetaNew;
                        int zetaAvg = Math.abs((zetaOld + zetaNew) / 2);
                        zeta_app = Math.abs(zetaAvg - (orientationMobile - orientationMobile));
                        editor.putInt("zetaAvg", zetaAvg);
                        editor.putLong("distanceOld", Double.doubleToRawLongBits(distance));
                        editor.putInt("orientationMobileOld", orientationMobile);
                        editor.putInt("zeta_appOld", zeta_app);
                        editor.putBoolean("isFirstTime", false);
                        editor.putInt("orientationMobileOld", orientationMobile);
                        editor.apply();
                        return zeta_app;
                    } else if ((B > orientationMobile || orientationMobile > C)) {
                        int zetaNew = Math.abs(orientationBLE - orientationMobile);
                        int zetaAvg = Math.abs((zetaOld + zetaNew) / 2);
                        zeta_app = Math.abs(zetaAvg - (orientationMobile - orientationMobile));

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
                } else {
                    int zetaAvg = sharedPreferences.getInt("zetaAvg", 0);
                    int A = orientationMobileOld - 180, B = zetaAvg - 15, C = zetaAvg + 15;
                    if ((orientationMobileOld > orientationBLE)&&(B > orientationMobile || orientationMobile > C)) {
                        int zetaOld = zeta_appOld;
                        int zetaNew = Math.abs(orientationBLE - orientationMobileOld);
                        zetaNew = 360 - zetaNew;
                        zetaAvg = Math.abs((zetaOld + zetaNew) / 2);
                        zeta_app = Math.abs(zetaAvg - (orientationMobile - orientationMobileOld));

                        editor.putInt("zetaAvg", zetaAvg);
                        editor.putLong("distanceOld", Double.doubleToRawLongBits(distance));
                        editor.putInt("orientationMobileOld", orientationMobile);
                        editor.putInt("zeta_appOld", zeta_app);
                        editor.putBoolean("isFirstTime", false);
                        editor.putInt("orientationMobileOld", orientationMobile);
                        editor.apply();
                        return zeta_app;
                    } else if ((B > orientationMobile || orientationMobile > C)) {
                        int zetaOld = zeta_appOld;
                        int zetaNew = Math.abs(orientationBLE - orientationMobileOld);
                        zetaAvg = Math.abs((zetaOld + zetaNew) / 2);
                        zeta_app = Math.abs(zetaAvg - (orientationMobile - orientationMobileOld));
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
