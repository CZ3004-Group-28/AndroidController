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
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class BluetoothFragment extends Fragment {
    private static final String TAG = "BluetoothFragment";

    private BluetoothAdapter bluetoothAdapter;
    private boolean bluetoothOn;

    //ui
    private Button btnToggleBluetooth;
    private Button btnSearchBluetooth;
    private BluetoothDiscoveredListViewAdapter discoveredDevicesAdapter;
    private List<String> discoveredDevicesAdapterData;
    private BluetoothPairedListViewAdapter pairedDevicesAdapter;
    private List<String> pairedDevicesAdapterData;

    //Data
    private HashMap<String, BluetoothDevice> pairedDevices;
    private HashMap<String, BluetoothDevice> discoveredDevices;

    public BluetoothFragment() {
        bluetoothOn = false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        discoveredDevices = new HashMap<String, BluetoothDevice>();
        discoveredDevicesAdapterData = new ArrayList<>();
        pairedDevices = new HashMap<String, BluetoothDevice>();
        pairedDevicesAdapterData = new ArrayList<>();

        IntentFilter btPairingFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        getActivity().registerReceiver(btPairingReceiver, btPairingFilter);
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

        ListView discoveredDevicesListView = (ListView) rootView.findViewById(R.id.bluetooth_device_list);
        discoveredDevicesAdapter = new BluetoothDiscoveredListViewAdapter(getContext(), R.layout.bt_device_list_layout, discoveredDevicesAdapterData);
        discoveredDevicesListView.setAdapter(discoveredDevicesAdapter);

        ListView pairedDevicesListView = (ListView) rootView.findViewById(R.id.paired_device_list);
        pairedDevicesAdapter = new BluetoothPairedListViewAdapter(getContext(), R.layout.bt_paired_device_list_layout, pairedDevicesAdapterData);
        pairedDevicesListView.setAdapter(pairedDevicesAdapter);

        initializeBluetooth();
        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(btDiscoveryReceiver);
        getActivity().unregisterReceiver(btPairingReceiver);
    }

    private void initializeBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            showShortToast("Bluetooth not supported");
            return;
        }

        btnToggleBluetooth.setEnabled(true);
        if (bluetoothAdapter.isEnabled()) {
            bluetoothOn = true;
        }

        Set<BluetoothDevice> paired = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : paired) {
            if (!pairedDevices.containsKey(device.getAddress())) {
                pairedDevices.put(device.getAddress(), device);
                pairedDevicesAdapterData.add(device.getAddress());
                pairedDevicesAdapter.updateList(pairedDevicesAdapterData);
            }
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

            discoveredDevices.clear();
            discoveredDevicesAdapter.clear();

            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }

            checkLocationPermission();
            bluetoothAdapter.startDiscovery();

            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            getContext().registerReceiver(btDiscoveryReceiver, filter);
        } else {
            showShortToast("Enable bluetooth first");
            Log.d(TAG, "Tried to discover wtihout bluetooth enabled");
        }
    }

    private final BroadcastReceiver btDiscoveryReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                //discovery starts, we can show progress dialog or perform other tasks
                showShortToast("Discovering Devices");

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //discovery finishes, dismis progress dialog
                showShortToast("Discovery Ended");

            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //bluetooth device found
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    String deviceName = device.getName();
                    String deviceAddress = device.getAddress();

                    //Do not add if device previously discovered
                    if (discoveredDevices.containsKey(deviceAddress)) return;
                    //Do not add if device paired
                    if(pairedDevices.containsKey(deviceAddress)) return;

                    discoveredDevices.put(deviceAddress, device);
                    discoveredDevicesAdapterData.add(deviceAddress);
                    discoveredDevicesAdapter.updateList(discoveredDevicesAdapterData);
                    Log.d(TAG, "Found device: " + device.getName() + ", " + device.getAddress());
                }
            }
        }
    };

    private final BroadcastReceiver btPairingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    setPaired(mDevice);
                }
                //case2: creating a bone
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }
            }
        }
    };


    private void updateBluetoothControlButtons() {
        if (bluetoothOn) {
            btnToggleBluetooth.setText("Bluetooth: ON");
            btnSearchBluetooth.setEnabled(true);
        } else {
            btnToggleBluetooth.setText("Bluetooth: OFF");
            btnSearchBluetooth.setEnabled(false);
        }
    }


    public void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        showShortToast("Please grant locations permissions first!");

        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    private void connectBluetooth(String macAddress) {
        //TODO: Logic to connect bluetooth
        showShortToast("Connect to: " + macAddress);
    }

    private void pairBluetooth(String macAddress) {
        try {
//            String macAddress = btDeviceLVItem.getAddress();
            if (pairedDevices.containsKey(macAddress)) {
                Log.d(TAG, "Pair bluetooth: Device " + macAddress + " is already paired");
                return;
            }
            BluetoothDevice device = discoveredDevices.get(macAddress);
            if (device == null) {
                Log.d(TAG, "Pair bluetooth: Device " + macAddress + " is not found");
                return;
            }

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                Log.d(TAG, "Trying to pair with " + macAddress);
                boolean bonded = device.createBond();
                if (!bonded) {
                    Log.e(TAG, "An error occured while trying to pair with device " + macAddress);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    showShortToast("Location permissions granted");
                } else {
                }
            });

    private class BluetoothDiscoveredListViewAdapter extends ArrayAdapter<String> {
        private List<String> items;

        public BluetoothDiscoveredListViewAdapter(@NonNull Context context, int resource, @NonNull List<String> objects) {
            super(context, resource, objects);
            items = objects;
        }

        public void updateList(List<String> list) {
            items = list;
            this.notifyDataSetChanged();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.bt_device_list_layout, parent, false);
            }
            BluetoothDevice btDevice = discoveredDevices.get(items.get(position));

            String deviceName = btDevice.getName();
            String deviceMAC = btDevice.getAddress();

            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = "Unnamed Device";
            }
            if (deviceMAC == null || deviceMAC.isEmpty()) {
                deviceMAC = "No address found";
            }

            TextView btDeviceTitleTxt = (TextView) convertView.findViewById(R.id.bt_list_title);
            TextView btDeviceMACTxt = (TextView) convertView.findViewById(R.id.bt_list_macaddr);
            Button btnConnect = (Button) convertView.findViewById(R.id.bluetooth_pair_btn);

            btDeviceTitleTxt.setText(deviceName);
            btDeviceMACTxt.setText(deviceMAC);
            btnConnect.setOnClickListener(v -> {
                pairBluetooth(items.get(position));
            });
            return convertView;
        }
    }

    private class BluetoothPairedListViewAdapter extends ArrayAdapter<String> {
        private List<String> items;

        public BluetoothPairedListViewAdapter(@NonNull Context context, int resource, @NonNull List<String> objects) {
            super(context, resource, objects);
            items = objects;
        }

        public void updateList(List<String> list) {
            items = list;
            this.notifyDataSetChanged();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.bt_paired_device_list_layout, parent, false);
            }
            BluetoothDevice btDevice = pairedDevices.get(items.get(position));

            String deviceName = btDevice.getName();
            String deviceMAC = btDevice.getAddress();

            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = "Unnamed Device";
            }
            if (deviceMAC == null || deviceMAC.isEmpty()) {
                deviceMAC = "No address found";
            }

            TextView btDeviceTitleTxt = (TextView) convertView.findViewById(R.id.bt_list_paired_title);
            TextView btDeviceMACTxt = (TextView) convertView.findViewById(R.id.bt_list_paired_macaddr);
            Button btnConnect = (Button) convertView.findViewById(R.id.bluetooth_connect_btn);

            btDeviceTitleTxt.setText(deviceName);
            btDeviceMACTxt.setText(deviceMAC);
            btnConnect.setOnClickListener(v -> {
                connectBluetooth(items.get(position));
            });
            return convertView;
        }
    }

    private void setPaired(BluetoothDevice pairedDevice){
        String pairedAddress = pairedDevice.getAddress();
        pairedDevices.put(pairedAddress,pairedDevice);
        pairedDevicesAdapterData.add(pairedAddress);

        discoveredDevices.remove(pairedAddress);
        discoveredDevicesAdapterData.remove(pairedAddress);

        discoveredDevicesAdapter.updateList(discoveredDevicesAdapterData);
        pairedDevicesAdapter.updateList(pairedDevicesAdapterData);
    }

    private void showShortToast(String msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
    }

    private void showLongToast(String msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
    }
}