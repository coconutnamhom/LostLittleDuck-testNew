package com.example.lostlittleduck.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothGatt;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.example.lostlittleduck.DeviceDetail;
import com.example.lostlittleduck.MainActivity;
import com.example.lostlittleduck.R;
import com.example.lostlittleduck.adapter.ConnectedDeviceAdapter;
import com.example.lostlittleduck.comm.ObserverManager;

import java.util.List;

public class ConnectedBLEFragment extends Fragment {
    private ConnectedDeviceAdapter mDeviceAdapter;
    private ProgressDialog progressDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.i("Check", "OnCreateView");

        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.connected_ble_fragment, container, false);

        initView(rootView);

        return rootView;
    }
    @Override
    public void onResume() {
        super.onResume();
        showConnectedDevice();
    }

    private void initView(View rootView) {

        mDeviceAdapter = new ConnectedDeviceAdapter(getActivity());
        mDeviceAdapter.setOnDeviceClickListener(new ConnectedDeviceAdapter.OnDeviceClickListener() {

            @Override
            public void onDetail(BleDevice bleDevice) {
                if (BleManager.getInstance().isConnected(bleDevice)) {
                    Intent intent = new Intent(((MainActivity)getActivity()), DeviceDetail.class);
                    intent.putExtra(DeviceDetail.KEY_DATA, bleDevice);
                    ((MainActivity)getActivity()).startActivity(intent);
                }
            }
        });
        ListView listView_device = (ListView) rootView.findViewById(R.id.list_connected);
        listView_device.setAdapter(mDeviceAdapter);
    }

    private void showConnectedDevice() {
        List<BleDevice> deviceList = BleManager.getInstance().getAllConnectedDevice();
        mDeviceAdapter.clearConnectedDevice();

        if(deviceList != null){
            for (BleDevice bleDevice : deviceList) {
                mDeviceAdapter.addDevice(bleDevice);
            }
        }

        mDeviceAdapter.notifyDataSetChanged();
    }
    private void connect(final BleDevice bleDevice) {
        BleManager.getInstance().connect(bleDevice, new BleGattCallback() {
            @Override
            public void onStartConnect() {
                progressDialog.show();
            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
//                img_loading.clearAnimation();
//                img_loading.setVisibility(View.INVISIBLE);
//                btn_scan.setText(getString(R.string.start_scan));
                progressDialog.dismiss();
                Toast.makeText(getActivity(), getString(R.string.connect_fail), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                progressDialog.dismiss();
                mDeviceAdapter.addDevice(bleDevice);
                mDeviceAdapter.notifyDataSetChanged();
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {
                progressDialog.dismiss();

                mDeviceAdapter.removeDevice(bleDevice);
                mDeviceAdapter.notifyDataSetChanged();

                if (isActiveDisConnected) {
//                    Toast.makeText(getActivity(), getString(R.string.active_disconnected), Toast.LENGTH_LONG).show();
                } else {
//                    Toast.makeText(getActivity(), getString(R.string.disconnected), Toast.LENGTH_LONG).show();
                    ObserverManager.getInstance().notifyObserver(bleDevice);
                }

            }
        });
    }

}