package com.example.androidcontroller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class HomeFragment extends Fragment{
    public static String TAG = "HomeFragment";

    private boolean initializedIntentListeners = false;
    private TextView txtRoboStatus;

    private View rootview;

    //For Arena
    boolean placingRobot, settingObstacle, settingDir;

    private Handler handler = new Handler();

    //GridMap
    private static GridMap gridMap;

    //For robot
    private boolean isManual = false;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ArenaFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        if(!initializedIntentListeners){
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(roboStatusUpdateReceiver, new IntentFilter("updateRobocarStatus"));
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(imageRecResultReceiver, new IntentFilter("imageResult"));
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(robotLocationUpdateReceiver, new IntentFilter("updateRobocarLocation"));

            initializedIntentListeners = true;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootview = inflater.inflate(R.layout.fragment_home, container, false);

        if(gridMap == null){
            gridMap = new GridMap(getContext());
            gridMap = rootview.findViewById(R.id.mapView);
        }

        //Initialize Flags
        placingRobot = false;

        // For updating of robot status
        this.txtRoboStatus = (TextView) rootview.findViewById(R.id.robotStatusText);

        //CONTROL BUTTON DECLARATIONS
        ImageButton controlBtnUp = rootview.findViewById(R.id.upArrowBtn);
        ImageButton controlBtnDown = rootview.findViewById(R.id.downArrowBtn);
        ImageButton controlBtnLeft = rootview.findViewById(R.id.leftArrowBtn);
        ImageButton controlBtnRight = rootview.findViewById(R.id.rightArrowBtn);

        //CONTROL BUTTON: Forward
//        controlBtnUp.setOnClickListener(v -> {
//            try{
//                sendDirectionCmdIntent("FW01");
//            }catch (Exception e){
//                Log.e(TAG, "onCreateView: An error occured while making message intent");
//            }
//        });

        controlBtnUp.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    sendDirectionCmdIntent("FW--");

                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    sendDirectionCmdIntent("STOP");
                }

                return true;
            }
        });

        //CONTROL BUTTON: Reverse
        controlBtnDown.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    sendDirectionCmdIntent("BW--");

                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    sendDirectionCmdIntent("STOP");
                }

                return true;
            }
        });

        //CONTROL BUTTON: Left
        controlBtnLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    sendDirectionCmdIntent("TL--");

                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    sendDirectionCmdIntent("STOP");
                }

                return true;
            }
        });

        //CONTROL BUTTON: Right
        controlBtnRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    sendDirectionCmdIntent("TR--");

                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    sendDirectionCmdIntent("STOP");
                }

                return true;
            }
        });

        //ROBOT RELATED
        Button btnToggleRobotMode = rootview.findViewById(R.id.btnToggleRobotMode);
        Button btnSendArenaInfo = rootview.findViewById(R.id.btnSendInfo);
        Button btnSendStartImageRec = rootview.findViewById(R.id.btnStartImageRec);
        Button btnSendStartFastestCar = rootview.findViewById(R.id.btnStartFastestCar);

        btnToggleRobotMode.setOnClickListener(v -> {
            isManual = !isManual;
            if(isManual){
                btnToggleRobotMode.setText("Mode: Manual");
                sendModeCmdIntent("manual");
            }else{
                btnToggleRobotMode.setText("Mode: Path");
                sendModeCmdIntent("path");
            }
        });

        btnSendArenaInfo.setOnClickListener(v->{
            gridMap.sendUpdatedObstacleInformation();
        });

        btnSendStartImageRec.setOnClickListener(v->{

            sendControlCmdIntent("start");

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    sendControlCmdIntent("stop");
                }
            }, 360000);


        });


        //ARENA RELATED
        Button btnResetArena = rootview.findViewById(R.id.btnResetArena);
        Button btnSetObstacle = rootview.findViewById(R.id.btnSetObstacle);
        Button btnSetFacing = rootview.findViewById(R.id.btnDirectionFacing);
        Button btnPlaceRobot = rootview.findViewById(R.id.btnPlaceRobot);

        btnResetArena.setOnClickListener(v->{
            try{
                gridMap.resetMap();
            }catch (Exception e){
                Log.e(TAG, "onCreateView: An error occured while resetting map");
                e.printStackTrace();
            }
        });

        btnPlaceRobot.setOnClickListener(v -> {
            try{
                //New status
                placingRobot = !placingRobot;
                if(placingRobot){
                    gridMap.setStartCoordStatus(placingRobot);
                    btnPlaceRobot.setText("Cancel");
                }else{
                    gridMap.setStartCoordStatus(placingRobot);
                    btnPlaceRobot.setText("Place Robot");
                }
            }catch (Exception e){
                Log.e(TAG, "onCreateView: An error occured while placing robot");
                e.printStackTrace();
            }
        });

        btnSetObstacle.setOnClickListener(v->{
            try{
                settingObstacle = !settingObstacle;
                if(settingObstacle){
                    gridMap.setSetObstacleStatus(settingObstacle);
                    btnSetObstacle.setText("Stop Add Obstacle");
                }else{
                    gridMap.setSetObstacleStatus(settingObstacle);
                    btnSetObstacle.setText("Add Obstacle");
                }
            }catch (Exception e){
                Log.e(TAG, "onCreateView: An error occurred while setting obstacle");
                e.printStackTrace();
            }
        });

        btnSetFacing.setOnClickListener(v -> {

            try{
                settingDir = !settingDir;
                if(settingDir){
                    gridMap.setSetObstacleDirection(settingDir);
                    btnSetFacing.setText("Stop Set Facing");
                }else{
                    gridMap.setSetObstacleDirection(settingDir);
                    btnSetFacing.setText("Set Facing");
                }
            }catch (Exception e){
                Log.e(TAG, "onCreateView: An error occurred while setting obstacle direction");
                e.printStackTrace();
            }

        });

        //Adding obstacles using buttons
        Button addObsManual = rootview.findViewById(R.id.add_obs_btn);
        EditText addObs_x = rootview.findViewById(R.id.add_obs_x_value);
        EditText addObs_y = rootview.findViewById(R.id.add_obs_y_value);

        addObsManual.setOnClickListener(v -> {

            try{

                String x_value = addObs_x.getText().toString();
                String y_value = addObs_y.getText().toString();

                try
                {
                    int x_value_int = Integer.parseInt(x_value);
                    int y_value_int = Integer.parseInt(y_value);

                    if( x_value_int < 20 && x_value_int >=0 && y_value_int < 20 && y_value_int >=0){
                        gridMap.setObstacleCoord(x_value_int+1, y_value_int+1);
                        showShortToast("obs successfully added!");
                    }else{
                        showShortToast("wrong values entered!!");
                    }

                }catch (Exception e){
                    showShortToast("Incorrect values!");
                    //incorrect format, reset EditText fields
                    addObs_x.setText("");
                    addObs_y.setText("");
                }


            }catch (Exception e){
                Log.e(TAG, "onCreateView: An error occurred while adding obstacle manually");
                e.printStackTrace();
            }

        });

        // DEBUGGING BUTTONS
        Button btnFW10 = rootview.findViewById(R.id.temp_btnFW10);
        btnFW10.setOnClickListener(v -> {sendDirectionCmdIntent("FW10");});
        Button btnBT10 = rootview.findViewById(R.id.temp_btnBW10);
        btnBT10.setOnClickListener(v -> {sendControlCmdIntent("BW10");});
        Button btnFL00 = rootview.findViewById(R.id.temp_btnFL00);
        btnFL00.setOnClickListener(v -> {sendControlCmdIntent("FL00");});
        Button btnFR00 = rootview.findViewById(R.id.temp_btnFR00);
        btnFR00.setOnClickListener(v -> {sendControlCmdIntent("FR00");});
        Button btnBL00 = rootview.findViewById(R.id.temp_btnBL00);
        btnBL00.setOnClickListener(v -> {sendControlCmdIntent("BL00");});
        Button btnBR00 = rootview.findViewById(R.id.temp_btnBR00);
        btnBR00.setOnClickListener(v->{sendControlCmdIntent("BR00");});

        // Inflate the layout for this fragment
        return rootview;
    }

    private BroadcastReceiver roboStatusUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try{
                String msgInfo = intent.getStringExtra("msg");
                txtRoboStatus.setText(msgInfo);
            }catch (Exception e){
                txtRoboStatus.setText("UNKNOWN");
                showShortToast("Error updating robocar status");
                Log.e(TAG, "onReceive: An error occured while updating the robocar status");
                e.printStackTrace();
            }
        }
    };

    private BroadcastReceiver robotLocationUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try{
                JSONObject msgJSON = new JSONObject(intent.getStringExtra("msg"));
                int xCoord = msgJSON.getInt("x");
                int yCoord = msgJSON.getInt("y");
                int dirInt = msgJSON.getInt("d");
                String direction = "up";
                switch(dirInt){
                    case 0: //NORTH
                        direction = "up";
                        break;
                    case 2: //EAST
                        direction = "right";
                        break;
                    case 4: //SOUTH
                        direction = "down";
                        break;
                    case 6: //WEST
                        direction = "left";
                        break;
                }

                if(xCoord < 0 || yCoord < 0 || xCoord > 20 || yCoord > 20){
                    showShortToast("Error: Robot move out of area (x: "+xCoord+", y: "+yCoord+")");
                    Log.e(TAG, "onReceive: Robot is out of the arena area");
                    return;
                }

                int[] curCoord = gridMap.getCurCoord(); // robot current coordinate this.setOldRobotCoord(curCoord[0], curCoord[1]);

                if (curCoord[0] != -1 && curCoord[1] != -1) {
                    gridMap.unsetOldRobotCoord(curCoord[0], curCoord[1]);
                    gridMap.setCurCoord(xCoord, yCoord, direction);
                } else {
                    showShortToast("Error: Robot has no start point");
                }
            }catch (Exception e){
                showShortToast("Error updating robot location");
                Log.e(TAG, "onReceive: An error occured while updating robot location");
                e.printStackTrace();
            }
        }
    };

    private BroadcastReceiver imageRecResultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try{
                JSONObject msgJSON = new JSONObject(intent.getStringExtra("msg"));
                int obstacleID = Integer.parseInt(msgJSON.getString("obstacle_id"));
                String targetID = msgJSON.getString("image_id");
                gridMap.updateImageNumberCell(obstacleID, targetID);
            }catch (Exception e){
                showShortToast("Error updating image rec result");
                Log.e(TAG, "onReceive: An error occured while upating the image rec result");
                e.printStackTrace();
            }
        }
    };

    private void showShortToast(String msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
    }

    private void showLongToast(String msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
    }

    private void sendDirectionCmdIntent(String direction){

        try{
            JSONObject directionJSONObj = new JSONObject();
            directionJSONObj.put("cat","manual");
            directionJSONObj.put("value",direction);

            broadcastSendBTIntent(directionJSONObj.toString());
        }catch (Exception e){
            Log.e(TAG, "sendDirectionCmdIntent: An error occured while sending direction command intent");
            e.printStackTrace();
        }
    }

    private void sendModeCmdIntent(String mode){
        try{
            if(!mode.equals("path") && !mode.equals("manual")){
                Log.i(TAG, "sendModeIntent: Invalid mode to send: "+mode);
                return;
            }
            JSONObject modeJSONObj = new JSONObject();
            modeJSONObj.put("cat","mode");
            modeJSONObj.put("value",mode);

            broadcastSendBTIntent(modeJSONObj.toString());
        }catch (Exception e){
            Log.e(TAG, "sendModeIntent: An error occured while sending mode command intent");
            e.printStackTrace();
        }
    }

    private void sendControlCmdIntent(String control){
        try{
            JSONObject ctrlJSONObj = new JSONObject();
            ctrlJSONObj.put("cat","control");
            ctrlJSONObj.put("value",control);

            broadcastSendBTIntent(ctrlJSONObj.toString());
        }catch (Exception e){
            Log.e(TAG, "sendControlCmdIntent: An error occured while sending control command intent");
            e.printStackTrace();
        }
    }

    private void broadcastSendBTIntent(String msg){
        Intent sendBTIntent = new Intent("sendBTMessage");
        sendBTIntent.putExtra("msg",msg);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(sendBTIntent);
    }
}