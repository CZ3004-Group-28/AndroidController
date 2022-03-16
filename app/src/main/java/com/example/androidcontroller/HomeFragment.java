package com.example.androidcontroller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class HomeFragment extends Fragment{
    public static String TAG = "HomeFragment";

    private boolean initializedIntentListeners = false;
    private TextView txtRoboStatus;

    private Switch manualModeSwitch;
    private Switch outdoorArenaSwitch;
    private Switch turningModeSwitch;

    private View rootview;

    //For Arena
    boolean placingRobot, settingObstacle, settingDir;

    private Handler handler = new Handler();

    //GridMap
    private static GridMap gridMap;

    //For robot
    private boolean isManual = false;

    //For Obstalce listview
    private ObstaclesListViewAdapter obstaclesListViewAdapter;
    private List<ObstacleListItem> obstacleListItemList;

    //Auxiliary
    private long timeStarted;
    private long timeEnded;
    private long timeTakenInNanoSeconds;

    //Android widgets for UI
    //ROBOT RELATED
    Button btnSendArenaInfo;
    Button btnSendStartImageRec;
    Button btnSendStartFastestCar;

    //ARENA RELATED
    Button btnResetArena;
    Button btnSetObstacle;
    Button btnSetFacing;
    Button btnPlaceRobot;

    //Adding obstacles using buttons
    Button btnAddObsManual;
    EditText addObs_x;
    EditText addObs_y;

    //Bot Status
    TextView txtTimeTaken;

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

        obstacleListItemList = new ArrayList<>();

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        if(!initializedIntentListeners){
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(roboStatusUpdateReceiver, new IntentFilter("updateRobocarStatus"));
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(roboStateReceiver, new IntentFilter("updateRoboCarState"));
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(roboModeUpdateReceiver, new IntentFilter("updateRobocarMode"));
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(updateObstalceListReceiver, new IntentFilter("newObstacleList"));
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

        //For obstacle list view
        ListView obstacleListView = (ListView)  rootview.findViewById(R.id.home_obstacles_listview);
        obstaclesListViewAdapter = new ObstaclesListViewAdapter(getContext(), R.layout.home_obstacle_list_layout, obstacleListItemList);
        obstacleListView.setAdapter(obstaclesListViewAdapter);

        //Switches
        manualModeSwitch = (Switch) rootview.findViewById(R.id.switch_manualMode);
        outdoorArenaSwitch = (Switch) rootview.findViewById(R.id.switch_outdoor);
        turningModeSwitch = (Switch) rootview.findViewById(R.id.switch_turnmode);

        manualModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    sendModeCmdIntent("manual");
                }else{
                    sendModeCmdIntent("path");
                }
            }
        });

        outdoorArenaSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                gridMap.setIsOutdoorArena(isChecked);
            }
        });

        //Initialize Flags
        placingRobot = false;

        // For updating of robot status
        this.txtRoboStatus = (TextView) rootview.findViewById(R.id.robotStatusText);

        //CONTROL BUTTON DECLARATIONS
        ImageButton controlBtnUp = rootview.findViewById(R.id.upArrowBtn);
        ImageButton controlBtnDown = rootview.findViewById(R.id.downArrowBtn);
        ImageButton controlBtnLeft = rootview.findViewById(R.id.leftArrowBtn);
        ImageButton controlBtnRight = rootview.findViewById(R.id.rightArrowBtn);

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

        //TIME TAKEN TEXTVIEW
        txtTimeTaken = rootview.findViewById(R.id.txt_timeTaken);

        //ROBOT RELATED
        btnSendArenaInfo = rootview.findViewById(R.id.btnSendInfo);
        btnSendStartImageRec = rootview.findViewById(R.id.btnStartImageRec);
        btnSendStartFastestCar = rootview.findViewById(R.id.btnStartFastestCar);

        //ARENA RELATED
        btnResetArena = rootview.findViewById(R.id.btnResetArena);
        btnSetObstacle = rootview.findViewById(R.id.btnSetObstacle);
        btnSetFacing = rootview.findViewById(R.id.btnDirectionFacing);
        btnPlaceRobot = rootview.findViewById(R.id.btnPlaceRobot);

        //Adding obstacles using buttons
        btnAddObsManual = rootview.findViewById(R.id.add_obs_btn);
        addObs_x = rootview.findViewById(R.id.add_obs_x_value);
        addObs_y = rootview.findViewById(R.id.add_obs_y_value);

        // OnClickListeners for sending arena info to RPI
        btnSendArenaInfo.setOnClickListener(v->{
            gridMap.sendUpdatedObstacleInformation();
        });

        btnSendStartImageRec.setOnClickListener(v->{
            gridMap.removeAllTargetIDs();
            txtTimeTaken.setVisibility(View.INVISIBLE);
            sendControlCmdIntent("start");
            timeStarted = System.nanoTime();
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    sendControlCmdIntent("stop");
                }
            }, 360000);
        });

        btnSendStartFastestCar.setOnClickListener(v->{
            txtTimeTaken.setVisibility(View.INVISIBLE);
            timeStarted = System.nanoTime();
            if(turningModeSwitch.isChecked()){
                //Big turn
                sendTurningModeCmdIntent("WN02");
            }else{
                sendTurningModeCmdIntent("WN01");
            }
        });

        btnResetArena.setOnClickListener(v->{
            try{
                gridMap.resetMap();
            }catch (Exception e){
                Log.e(TAG, "onCreateView: An error occured while resetting map");
                e.printStackTrace();
            }
        });

        // OnClickListeners for the arena related buttons
        btnPlaceRobot.setOnClickListener(v -> {
            try{
                //New status
                placingRobot = !placingRobot;
                if(placingRobot){
                    gridMap.setStartCoordStatus(placingRobot);
                    btnPlaceRobot.setText("Stop Set Robot");

                    //Disable other buttons
                    btnSetObstacle.setEnabled(false);
                    btnSetFacing.setEnabled(false);
                    btnResetArena.setEnabled(false);
                    btnSendStartFastestCar.setEnabled(false);
                    btnSendStartImageRec.setEnabled(false);
                }else{
                    gridMap.setStartCoordStatus(placingRobot);
                    btnSetObstacle.setEnabled(true);
                    btnSetFacing.setEnabled(true);
                    btnResetArena.setEnabled(true);
                    btnSendStartFastestCar.setEnabled(true);
                    btnSendStartImageRec.setEnabled(true);
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
                    btnSetObstacle.setText("Stop Set Obstacle");

                    //Disable other buttons
                    btnSetFacing.setEnabled(false);
                    btnPlaceRobot.setEnabled(false);
                    btnResetArena.setEnabled(false);
                    btnSendStartFastestCar.setEnabled(false);
                    btnSendStartImageRec.setEnabled(false);
                }else{
                    gridMap.setSetObstacleStatus(settingObstacle);
                    btnSetObstacle.setText("Set Obstacle");

                    //Re-enable other buttons
                    btnSetFacing.setEnabled(true);
                    btnPlaceRobot.setEnabled(true);
                    btnResetArena.setEnabled(true);
                    btnSendStartFastestCar.setEnabled(true);
                    btnSendStartImageRec.setEnabled(true);
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

                    //Disable Other Buttons
                    btnSetObstacle.setEnabled(false);
                    btnPlaceRobot.setEnabled(false);
                    btnResetArena.setEnabled(false);
                    btnSendStartFastestCar.setEnabled(false);
                    btnSendStartImageRec.setEnabled(false);
                }else{
                    gridMap.setSetObstacleDirection(settingDir);
                    btnSetFacing.setText("Set Facing");

                    //Reenable other buttons
                    btnSetObstacle.setEnabled(true);
                    btnPlaceRobot.setEnabled(true);
                    btnResetArena.setEnabled(true);
                    btnSendStartFastestCar.setEnabled(true);
                    btnSendStartImageRec.setEnabled(true);
                }
            }catch (Exception e){
                Log.e(TAG, "onCreateView: An error occurred while setting obstacle direction");
                e.printStackTrace();
            }
        });

        btnAddObsManual.setOnClickListener(v -> {
            try{
                String x_value = addObs_x.getText().toString();
                String y_value = addObs_y.getText().toString();
                try
                {
                    int x_value_int = Integer.parseInt(x_value);
                    int y_value_int = Integer.parseInt(y_value);

                    if( x_value_int < 20 && x_value_int >=0 && y_value_int < 20 && y_value_int >=0){
                        gridMap.setObstacleCoord(x_value_int, y_value_int);
                        showShortToast("Added obstacle");
                        addObs_x.setText("");
                        addObs_y.setText("");
                    }else{
                        showShortToast("Invalid Coordinates");
                    }
                }catch (Exception e){
                    showShortToast("Incorrect values!");
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
        btnBT10.setOnClickListener(v -> {sendDirectionCmdIntent("BW10");});
        Button btnFL00 = rootview.findViewById(R.id.temp_btnFL00);
        btnFL00.setOnClickListener(v -> {sendDirectionCmdIntent("FL00");});
        Button btnFR00 = rootview.findViewById(R.id.temp_btnFR00);
        btnFR00.setOnClickListener(v -> {sendDirectionCmdIntent("FR00");});
        Button btnBL00 = rootview.findViewById(R.id.temp_btnBL00);
        btnBL00.setOnClickListener(v -> {sendDirectionCmdIntent("BL00");});
        Button btnBR00 = rootview.findViewById(R.id.temp_btnBR00);
        btnBR00.setOnClickListener(v->{sendDirectionCmdIntent("BR00");});

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

    private BroadcastReceiver roboStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try{
                String state = intent.getStringExtra("msg");
                switch(state.toUpperCase()){
                    case "FINISHED":
                        timeEnded = System.nanoTime();
                        timeTakenInNanoSeconds = timeEnded - timeStarted;

                        double timeTakenInSeconds = (double) timeTakenInNanoSeconds/1000000000;
                        int timeTakenMin = (int) timeTakenInSeconds/60;
                        int timeTakenSec = (int) timeTakenInSeconds%60;
                        DecimalFormat df = new DecimalFormat("0.00");

                        txtTimeTaken.setText("Run completed in: "+Integer.toString(timeTakenMin)+"min "+df.format(timeTakenSec)+"secs");
                        txtTimeTaken.setVisibility(View.VISIBLE);

                        btnSetObstacle.setEnabled(true);
                        btnPlaceRobot.setEnabled(true);
                        btnResetArena.setEnabled(true);
                        btnSetFacing.setEnabled(true);
                        btnSendStartFastestCar.setEnabled(true);
                        btnSendStartImageRec.setEnabled(true);
                        btnSendArenaInfo.setEnabled(true);
                        btnAddObsManual.setEnabled(true);
                        break;
                    case "RUNNING":
                        btnSetObstacle.setEnabled(false);
                        btnPlaceRobot.setEnabled(false);
                        btnResetArena.setEnabled(false);
                        btnSetFacing.setEnabled(false);
                        btnSendStartFastestCar.setEnabled(false);
                        btnSendStartImageRec.setEnabled(false);
                        btnSendArenaInfo.setEnabled(false);
                        btnAddObsManual.setEnabled(false);
                        break;
                }
            }catch (Exception ex){
                Log.e(TAG, "onReceive: Error receiving robot completion status");
            }
        }
    };

    private BroadcastReceiver roboModeUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try{
                String mode = intent.getStringExtra("msg");
                switch (mode.toUpperCase()){
                    case "PATH":
                        manualModeSwitch.setChecked(false);
                        break;
                    case "MANUAL":
                        manualModeSwitch.setChecked(true);
                        break;
                }
            }catch (Exception ex){
                Log.e(TAG, "onReceive: An error occured on receiving robocar mode");
                ex.printStackTrace();
            }
        }
    };

    private BroadcastReceiver updateObstalceListReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            obstacleListItemList.clear();
            try{
                JSONArray msgInfo = new JSONArray(intent.getStringExtra("msg"));
                for(int i=0; i<msgInfo.length();i++){
                    JSONObject obj = msgInfo.getJSONObject(i);
                    obstacleListItemList.add(new ObstacleListItem(obj.getInt("no"), obj.getInt("x"),obj.getInt("y"),obj.getString("facing")));
                }
                obstaclesListViewAdapter.updateList(obstacleListItemList);
            }catch (Exception ex){
                Log.e(TAG, "onReceive: An error occured while updating obstacle list view");
                ex.printStackTrace();
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
                GridMap.Direction direction = GridMap.Direction.UP;
                switch(dirInt){
                    case 0: //NORTH
                        direction = GridMap.Direction.UP;
                        break;
                    case 2: //EAST
                        direction = GridMap.Direction.RIGHT;
                        break;
                    case 4: //SOUTH
                        direction = GridMap.Direction.DOWN;
                        break;
                    case 6: //WEST
                        direction = GridMap.Direction.LEFT;
                        break;
                }

                if(xCoord < 0 || yCoord < 0 || xCoord > 20 || yCoord > 20){
                    showShortToast("Error: Robot move out of area (x: "+xCoord+", y: "+yCoord+")");
                    Log.e(TAG, "onReceive: Robot is out of the arena area");
                    return;
                }

                gridMap.updateCurCoord(xCoord, yCoord, direction);
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

    private void sendTurningModeCmdIntent(String mode){
        try{
            JSONObject modeJSONObj = new JSONObject();
            modeJSONObj.put("cat","manual");
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

    private class ObstaclesListViewAdapter extends ArrayAdapter<ObstacleListItem>{
        private List<ObstacleListItem> items;

        public ObstaclesListViewAdapter(@NonNull Context context, int resource, @NonNull List<ObstacleListItem> objects) {
            super(context, resource, objects);
            items=objects;
        }

        public void updateList(List<ObstacleListItem> list) {
            this.items = list;
            this.notifyDataSetChanged();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.home_obstacle_list_layout, parent, false);
            }
            ObstacleListItem item = items.get(position);
            TextView obsNoTxt = (TextView) convertView.findViewById(R.id.txtObsListItem_obsNo);
            TextView xPosTxt = (TextView) convertView.findViewById(R.id.txtObsListItem_x);
            TextView yPosTxt = (TextView) convertView.findViewById(R.id.txtObsListItem_y);
            TextView facingTxt = (TextView) convertView.findViewById(R.id.txtObsListItem_dir);

            obsNoTxt.setText("#"+item.obsNo);
            xPosTxt.setText(Integer.toString(item.x));
            yPosTxt.setText(Integer.toString(item.y));
            facingTxt.setText(item.facing);

            return convertView;
        }
    }

    private class ObstacleListItem {
        int obsNo;
        int x;
        int y;
        String facing;

        public ObstacleListItem(int obsNo,int x, int y, String facing){
            this.obsNo = obsNo;
            this.x=x;
            this.y=y;
            this.facing=facing;
        }
    }
}