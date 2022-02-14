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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.androidcontroller.service.BluetoothConnectionService;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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

    //Bluetooth Connection
    private BluetoothConnectionService bluetoothConnectionService;
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("49930a2c-04f6-4fe6-beb7-688360fc5995");
    private boolean retryConnection = false;
    private String curDeviceAddress;
    private Handler reconnectionHandler = new Handler();
    private Button curConnectionBtn;

    //Auxiliary Functions
    private boolean initializedBCastReceivers = false;

    //TEMPORARY FOR USE TO TEST BT CONNECTION
    Button sendMsgBtn;
    TextView receivedTextView;
    EditText txtMsgToSend;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment BluetoothFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static BluetoothFragment newInstance(String param1, String param2) {
        BluetoothFragment fragment = new BluetoothFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

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

        //TO FIX: This is being called multiple times everytime we click onto this view
        //It is breaking the bt connection

        //Intent Filter for pairing devices
        IntentFilter btPairingFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        getActivity().registerReceiver(btPairingReceiver, btPairingFilter);

        if(bluetoothConnectionService == null){
            bluetoothConnectionService = new BluetoothConnectionService(getContext());
        }

        if(!initializedBCastReceivers){
            //Intent Filter for received messages
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(bluetoothMsgReceiver, new IntentFilter("incomingBTMessage"));
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(sendBluetoothReceiver, new IntentFilter("sendBTMessage"));
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(btConnectionUpdateReceiver, new IntentFilter("connectionBTStatus"));
            initializedBCastReceivers = true;
        }

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

        //TEMPORARY FOR BT TEXTING
        sendMsgBtn = (Button) rootView.findViewById(R.id.temp_btnsend);
        receivedTextView = (TextView) rootView.findViewById(R.id.temp_btreceivedmsgs);
        txtMsgToSend = (EditText) rootView.findViewById(R.id.temp_msginput);

        sendMsgBtn.setOnClickListener(v -> {
            byte[] bytes = txtMsgToSend.getText().toString().getBytes(Charset.defaultCharset());
            bluetoothConnectionService.write(bytes);
            txtMsgToSend.setText("");
        });
        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        try{
//            getContext().unregisterReceiver(btDiscoveryReceiver);
//            getContext().unregisterReceiver(btPairingReceiver);
        }catch (Exception e){
            Log.e(TAG, "onPause: An error occured while deregistering receivers");
            e.printStackTrace();
        }
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

            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);

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

    private void disconnectBluetooth(){
        bluetoothConnectionService.disconnect();
    }

    private boolean connectBluetooth(String macAddress) {
        //TODO: Logic to connect bluetooth
        showShortToast("Connect to: " + macAddress);
        BluetoothDevice btDevice = pairedDevices.get(macAddress);
        if(btDevice == null){
            showShortToast("Bluetooth device not paired");
            return false;
        }
        try{
            bluetoothConnectionService.startClient(btDevice);
            curDeviceAddress = macAddress;
            return true;
        }catch(Exception e){
            showShortToast("An error occured while attempting to start connection");
            e.printStackTrace();
            return false;
        }
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
                if(btnConnect.getText().equals("Disconnect")){
                    retryConnection = false;
                    disconnectBluetooth();
                    return;
                }
                boolean connectSuccess = connectBluetooth(items.get(position));
                if(connectSuccess){
                    btnConnect.setText("Wait..");
                    retryConnection = true;
                    curConnectionBtn = btnConnect;
                }
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

    private BroadcastReceiver bluetoothMsgReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("msg");
            if(receivedTextView.getText() == null){
                receivedTextView.setText(text);
            }else{
                receivedTextView.setText(receivedTextView.getText() + "\n"+text);
            }
        }
    };

    private BroadcastReceiver sendBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String msg = intent.getStringExtra("msg");
            try{
                byte[] msgInBytes = msg.getBytes(Charset.defaultCharset());
                bluetoothConnectionService.write(msgInBytes);
            }catch(Exception e){
                Log.e(TAG,"An error occured while sending bluetooth message");
                e.printStackTrace();
            }
        }
    };

    private BroadcastReceiver btConnectionUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try{
                String status = intent.getStringExtra("msg");
                switch(status.toUpperCase()){
                    case "CONNECTED":
                        curConnectionBtn.setText("Disconnect");
                        break;
                    case "DISCONNECTED":
                        showShortToast("Bluetooth Connection disconnected");
                        if(retryConnection){
                            reconnectionHandler.postDelayed(reconnectRunnable, 5000);
                        }else {
                            curConnectionBtn.setText("Connect");
                        }
                        break;
                }
            }catch (Exception e){
                Log.e(TAG, "onReceive: An error occured while trying to auto reconnect bluetooth");
                e.printStackTrace();
            }
        }
    };

    private Runnable reconnectRunnable =new Runnable() {
        @Override
        public void run() {
            try {
                if(!BluetoothConnectionService.isConnected && retryConnection){
                    connectBluetooth(curDeviceAddress);
                }
                reconnectionHandler.removeCallbacks(reconnectRunnable);
            }catch (Exception e){
                Log.e(TAG, "run: An error occured while running reconnectRunnable");
                showShortToast("Error reconnecting, retrying in 5s");
                e.printStackTrace();
            }
        }
    };

    private void showShortToast(String msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
    }

    private void showLongToast(String msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
    }
}