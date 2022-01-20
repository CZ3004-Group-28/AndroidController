package com.example.androidcontroller;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BluetoothFragment extends Fragment {
    private static final String TAG = "BluetoothFragment";

    private BluetoothAdapter bluetoothAdapter;
    private boolean bluetoothOn;

    //ui
    private Button btnToggleBluetooth;
    private Button btnSearchBluetooth;
    private BluetoothDeviceListViewAdapter discoveredDevicesAdapter;
    private BluetoothDeviceListViewAdapter pairedDevicesAdapter;

    //Data
    private ArrayList<BluetoothDevice> pairedDevices;
    private ArrayList<BluetoothDevice> discoveredDevices;

    public BluetoothFragment() {
        bluetoothOn = false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        discoveredDevices = new ArrayList<>();
        pairedDevices = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_bluetooth, container, false);

        btnToggleBluetooth = rootView.findViewById(R.id.bluetooth_btn_toggle);
        btnToggleBluetooth.setOnClickListener(v -> {
            toggleBluetooth();
        });
        btnSearchBluetooth = rootView.findViewById(R.id.bluetooth_btn_search);
        btnSearchBluetooth.setOnClickListener(v -> {
            searchBluetooth();
        });
        initializeBluetooth();

        ListView discoveredDevicesListView = (ListView) rootView.findViewById(R.id.bluetooth_device_list);
        discoveredDevicesAdapter = new BluetoothDeviceListViewAdapter(getContext(), R.layout.bt_device_list_layout, discoveredDevices);
        discoveredDevicesListView.setAdapter(discoveredDevicesAdapter);

        // Inflate the layout for this fragment
        return rootView;
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    private void initializeBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getActivity(), "Bluetooth not supported", Toast.LENGTH_LONG).show();
            return;
        }

        btnToggleBluetooth.setEnabled(true);
        if (bluetoothAdapter.isEnabled()) {
            bluetoothOn = true;
        }
        updateBluetoothControlButtons();
    }

    private void toggleBluetooth() {
        //Toggle the status
        bluetoothOn = !bluetoothOn;
        if (bluetoothOn) {
            bluetoothAdapter.enable();
        } else {
            bluetoothAdapter.disable();
        }
        updateBluetoothControlButtons();
    }

    private void searchBluetooth() {
        if (bluetoothOn) {
//            Intent discoverableIntent =
//                    new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
//            startActivity(discoverableIntent);

            discoveredDevices.clear();
            discoveredDevicesAdapter.clear();

            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            checkBluetoothPermission();
            bluetoothAdapter.startDiscovery();

            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            getContext().registerReceiver(mReceiver, filter);
        } else {
            Toast.makeText(getActivity(), "Enable bluetooth first", Toast.LENGTH_LONG).show();
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                //discovery starts, we can show progress dialog or perform other tasks
                Toast.makeText(getActivity(), "Discovering Devices..", Toast.LENGTH_SHORT).show();

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //discovery finishes, dismis progress dialog
                Toast.makeText(getActivity(), "Discovery ended", Toast.LENGTH_SHORT).show();

            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //bluetooth device found
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                discoveredDevices.add(device);
                discoveredDevicesAdapter.add(device);
                discoveredDevicesAdapter.notifyDataSetChanged();
                Log.d(TAG, "Found device: " + device.getName() + ", " + device.getAddress());
                Toast.makeText(getActivity(), "Found Device " + device.getName(), Toast.LENGTH_SHORT).show();
            }
        }
    };


    private void updateBluetoothControlButtons() {
        if (bluetoothOn) {
            btnToggleBluetooth.setText("Bluetooth: ON");
            btnSearchBluetooth.setEnabled(true);
        } else {
            btnToggleBluetooth.setText("Bluetooth: OFF");
        }
    }

    private class BluetoothDeviceListViewAdapter extends ArrayAdapter<BluetoothDevice> {

        public BluetoothDeviceListViewAdapter(@NonNull Context context, int resource, @NonNull List<BluetoothDevice> objects) {
            super(context, resource, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.bt_device_list_layout, parent, false);

            String deviceName = getItem(position).getName();
            String deviceMAC = getItem(position).getAddress();

            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = "Unnamed Device";
            }
            if (deviceMAC == null || deviceMAC.isEmpty()) {
                deviceMAC = "No address found";
            }

            TextView btDeviceTitleTxt = (TextView) convertView.findViewById(R.id.bt_list_title);
            TextView btDeviceMACTxt = (TextView) convertView.findViewById(R.id.bt_list_macaddr);

            btDeviceTitleTxt.setText(deviceName);
            btDeviceMACTxt.setText(deviceMAC);

            return convertView;
        }
    }

    private ActivityResultLauncher<String[]> requestMultiplePermissions = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {
        for (String key : isGranted.keySet()) {
            Log.d(TAG, "Permission: " + key + " Granted: " + isGranted.get(key));
        }
    });

    public void checkBluetoothPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int permissionCheck = ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION) + ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION) + ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) + ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),
                    Manifest.permission.BLUETOOTH_ADMIN) + ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),
                    Manifest.permission.BLUETOOTH_CONNECT) + ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),
                    Manifest.permission.BLUETOOTH_SCAN) + ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),
                    Manifest.permission.BLUETOOTH);

            if (permissionCheck == PackageManager.PERMISSION_GRANTED) return;

            requestMultiplePermissions.launch(new String[]{
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH
            });

        }
    }
}