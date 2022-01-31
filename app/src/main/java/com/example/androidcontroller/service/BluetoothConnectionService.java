package com.example.androidcontroller.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class BluetoothConnectionService {
    public static final String TAG = "BtConnectionSvc";

    public static volatile BluetoothConnectionService INSTANCE;

    private static final String appName = "MAP-GRP28-CONTROLLER";
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("49930a2c-04f6-4fe6-beb7-688360fc5995");

    private final BluetoothAdapter bluetoothAdapter;
    Context context;

    private AcceptThread insecureAcceptThread;

    private ConnectThread connectThread;
    private BluetoothDevice bluetoothDevice;
    private UUID deviceUUID;

    private ConnectedThread connectedThread;

    public static boolean isConnected = false;

    public BluetoothConnectionService(Context context){
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        start();
    }

    /**
     * AcceptThread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread{
        //Local Server Socket
        private final BluetoothServerSocket serverSocket;
        public AcceptThread(){
            BluetoothServerSocket temp = null;

            try{
                temp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID_INSECURE);
                Log.d(TAG,"Setting up server using "+MY_UUID_INSECURE);
            }catch (IOException e){
                Log.e(TAG, "AcceptThread: IO Exception "+e.getMessage());
            }
            serverSocket = temp;
        }

        public void run(){
            Log.d(TAG, "AcceptThread: run");
            BluetoothSocket socket = null;
            try{
                Log.d(TAG, "run: RFCOM server socket start.....");
                // Blocking call, returns on a successful connection or an exception only
                socket = serverSocket.accept();
                Log.d(TAG, "run: RFCOM server socket accepted connection.");
            }catch (IOException e){
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage() );
            }

            if(socket != null){
                connected(socket,bluetoothDevice);
            }
            Log.i(TAG, "END acceptThread ");
        }
        //To close the serversocket
        public void cancel() {
            Log.d(TAG, "cancel: Canceling AcceptThread.");
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: Close of AcceptThread ServerSocket failed. " + e.getMessage() );
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private BluetoothSocket bluetoothSocket;

        public ConnectThread(BluetoothDevice device) {
            Log.d(TAG, "ConnectThread: start");
            bluetoothDevice = device;
        }

        public void run(){
            BluetoothSocket tmp = null;
            Log.i(TAG, "ConnectThread: run");

            for(ParcelUuid uuid:bluetoothDevice.getUuids()){
                deviceUUID = UUID.fromString(uuid.toString());
                // Get a BluetoothSocket for a connection with the
                // given BluetoothDevice
                try {
                    Log.d(TAG, "ConnectThread: Trying to create InsecureRfcommSocket using UUID: "
                            +MY_UUID_INSECURE );
                    tmp = bluetoothDevice.createRfcommSocketToServiceRecord(deviceUUID);
                } catch (IOException e) {
                    Log.e(TAG, "ConnectThread: Could not create InsecureRfcommSocket " + e.getMessage());
                }

                bluetoothSocket = tmp;

                // Always cancel discovery because it will slow down a connection
                bluetoothAdapter.cancelDiscovery();

                // Make a connection to the BluetoothSocket
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    bluetoothSocket.connect();
                    Log.d(TAG, "run: ConnectThread connected.");
                    break;
                } catch (IOException e) {
                    // Close the socket
                    try {
                        bluetoothSocket.close();
                        Log.d(TAG, "run: Closed Socket.");
                    } catch (IOException e1) {
                        Log.e(TAG, "mConnectThread: run: Unable to close connection in socket " + e1.getMessage());
                    }
                    Log.d(TAG, "run: ConnectThread: Could not connect to UUID: " + MY_UUID_INSECURE );
                }
            }

            connected(bluetoothSocket,bluetoothDevice);
        }
        public void cancel() {
            try {
                Log.d(TAG, "cancel: Closing Client Socket.");
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: close() of mmSocket in Connectthread failed. " + e.getMessage());
            }
        }
    }

    /**
     * Start the service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (insecureAcceptThread == null) {
            insecureAcceptThread = new AcceptThread();
            insecureAcceptThread.start();
        }
        //Accept Thread starts and waits for a connection
    }

    public void startClient(BluetoothDevice device){
        Log.d(TAG, "startClient: Started.");

        connectThread = new ConnectThread(device);
        connectThread.start();
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream btInStream;
        private final OutputStream btOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread: Starting.");

            bluetoothSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = bluetoothSocket.getInputStream();
                tmpOut = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            isConnected = true;

            btInStream = tmpIn;
            btOutStream = tmpOut;
        }

        public void run(){
            byte[] buffer = new byte[1024];  // buffer store for the stream

            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                // Read from the InputStream
                try {
                    bytes = btInStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream: " + incomingMessage);

                    Intent incomingMessageIntent = new Intent("incomingBTMessage");
                    incomingMessageIntent.putExtra("msg",incomingMessage);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(incomingMessageIntent);
                } catch (IOException e) {
                    isConnected = false;
                    Log.e(TAG, "write: Error reading Input Stream. " + e.getMessage() );
                    break;
                }
            }
        }

        //Call this from the main activity to send data to the remote device
        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to outputstream: " + text);
            try {
                btOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "write: Error writing to output stream. " + e.getMessage() );
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) { }
        }
    }

    private void connected(BluetoothSocket socket, BluetoothDevice device) {
        Log.d(TAG, "connected: Starting.");

        // Start the thread to manage the connection and perform transmissions
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
    }


    /**
     * Write to connected thread in unsynchronized manner
     * @param out
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;

        // Synchronize a copy of the ConnectedThread
        Log.d(TAG, "write: Write Called.");
        //perform the write
        connectedThread.write(out);
    }
}
