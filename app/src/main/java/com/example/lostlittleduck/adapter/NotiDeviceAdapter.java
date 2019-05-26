package com.example.lostlittleduck.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.clj.fastble.BleManager;
import com.clj.fastble.data.BleDevice;
import com.example.lostlittleduck.R;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class NotiDeviceAdapter extends BaseAdapter {


    private Context context;
    private List<BleDevice> bleDeviceList;

    public NotiDeviceAdapter(Context context) {
        this.context = context;
        bleDeviceList = new ArrayList<>();
    }

    public void addDevice(BleDevice bleDevice) {
        removeDevice(bleDevice);
        bleDeviceList.add(bleDevice);
    }

    public void removeDevice(BleDevice bleDevice) {
        for (int i = 0; i < bleDeviceList.size(); i++) {
            BleDevice device = bleDeviceList.get(i);
            if (bleDevice.getKey().equals(device.getKey())) {
                bleDeviceList.remove(i);
            }
        }
    }

    public void clearConnectedDevice() {
        for (int i = 0; i < bleDeviceList.size(); i++) {
            BleDevice device = bleDeviceList.get(i);
            if (BleManager.getInstance().isConnected(device)) {
                bleDeviceList.remove(i);
            }
        }
    }

    public void clearScanDevice() {
        for (int i = 0; i < bleDeviceList.size(); i++) {
            BleDevice device = bleDeviceList.get(i);
            if (!BleManager.getInstance().isConnected(device)) {
                bleDeviceList.remove(i);
            }
        }
    }

    public void clear() {
        clearConnectedDevice();
        clearScanDevice();
    }

    @Override
    public int getCount() {
        return bleDeviceList.size();
    }

    @Override
    public BleDevice getItem(int position) {
        if (position > bleDeviceList.size())
            return null;
        return bleDeviceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView != null) {
            holder = (ViewHolder) convertView.getTag();
        } else {
            convertView = View.inflate(context, R.layout.adapter_noti_device, null);
            holder = new ViewHolder();
            convertView.setTag(holder);
            holder.img_blue = (ImageView) convertView.findViewById(R.id.img_con_blue);
            holder.txt_name = (TextView) convertView.findViewById(R.id.txt_con_name);
            holder.txt_mac = (TextView) convertView.findViewById(R.id.txt_con_mac);
//            holder.txt_rssi = (TextView) convertView.findViewById(R.id.txt_con_rssi);
            holder.layout_idle = (LinearLayout) convertView.findViewById(R.id.layout_con_idle);
            holder.layout_connected = (LinearLayout) convertView.findViewById(R.id.layout_con_connected);
            holder.btn_detail = (Button) convertView.findViewById(R.id.btn_noti_detail);
        }

        final BleDevice bleDevice = getItem(position);
        if (bleDevice != null) {
            boolean isConnected = BleManager.getInstance().isConnected(bleDevice);
            String name = bleDevice.getName();
            String mac = bleDevice.getMac();
            int rssi = bleDevice.getRssi();
            holder.txt_name.setText(name);
            holder.txt_mac.setText(mac);
//            holder.txt_rssi.setText(String.valueOf(rssi));

            double distance = calculateDistance(bleDevice);
            if (isConnected) {
//                holder.txt_name.setText(name + " ( distance : " + String.format("%.2f", distance) + " meters )");
                holder.txt_name.setText(name);
                holder.img_blue.setImageResource(R.mipmap.ic_blue_connected);
                holder.layout_idle.setVisibility(View.GONE);
                holder.layout_connected.setVisibility(View.VISIBLE);
                if(distance >= 0 && distance < 5){
                    holder.txt_name.setTextColor(Color.parseColor("#6df200"));
                    holder.txt_mac.setTextColor(Color.parseColor("#6df200"));
                }
                else if(distance >= 5 && distance < 8){
                    holder.txt_name.setTextColor(Color.parseColor("#FF8F00"));
                    holder.txt_mac.setTextColor(Color.parseColor("#FF8F00"));
                }
                else if(distance >= 8){
                    holder.txt_name.setTextColor(Color.RED);
                    holder.txt_mac.setTextColor(Color.RED);
                }

            } else {
                holder.img_blue.setImageResource(R.mipmap.ic_blue_remote);
                holder.txt_name.setTextColor(0xFF000000);
                holder.txt_mac.setTextColor(0xFF000000);
                holder.layout_idle.setVisibility(View.VISIBLE);
                holder.layout_connected.setVisibility(View.GONE);
            }
        }

        holder.btn_detail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.onDetail(bleDevice);
                }
            }
        });

        return convertView;
    }

    class ViewHolder {
        ImageView img_blue;
        TextView txt_name;
        TextView txt_mac;
        TextView txt_rssi;
        LinearLayout layout_idle;
        LinearLayout layout_connected;
        Button btn_detail;
    }

    public interface OnDeviceClickListener {

        void onDetail(BleDevice bleDevice);
    }

    private OnDeviceClickListener mListener;

    public void setOnDeviceClickListener(OnDeviceClickListener listener) {
        this.mListener = listener;
    }

    private Double calculateDistance(BleDevice bleDevice){
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

//        int txPower = -59; //hard coded power value. Usually ranges between -59 to -65
//        int rssi = bleDevice.getRssi();
//
//        if (rssi == 0) {
//            return -1.0;
//        }
//
//        Double ratio = rssi*1.0/txPower;
//        if (ratio < 1.0) {
//            return Math.pow(ratio,10);
//        }
//        else {
//            Double distance =  (0.89976)*Math.pow(ratio,7.7095) + 0.111;
//            return distance;
//        }
    }

}
