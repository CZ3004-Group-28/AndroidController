package com.example.androidcontroller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

public class HomeFragment extends Fragment {
    public static String TAG = "HomeFragment";

    private boolean initializedIntentListeners = false;
    private TextView txtRoboStatus;

    private View rootview;

    private PopupWindow arena_popup;

    //For Arena
    boolean placingRobot, settingObstacle, settingDir;

    //GridMap
    private static GridMap gridMap;

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

        Button arena_options_btn = rootview.findViewById(R.id.arenaSetupBtn);
        arena_options_btn.setOnClickListener(v -> {
            arenaSetOptions();
        });

        //CONTROL BUTTON DECLARATIONS
        ImageButton controlBtnUp = rootview.findViewById(R.id.upArrowBtn);
        ImageButton controlBtnDown = rootview.findViewById(R.id.downArrowBtn);
        ImageButton controlBtnLeft = rootview.findViewById(R.id.leftArrowBtn);
        ImageButton controlBtnRight = rootview.findViewById(R.id.rightArrowBtn);

        //CONTROL BUTTON: Forward
        controlBtnUp.setOnClickListener(v -> {
            try{
                Intent upDirectionIntent = new Intent("sendBTMessage");
                upDirectionIntent.putExtra("msg","f");
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(upDirectionIntent);
            }catch (Exception e){
                Log.e(TAG, "onCreateView: An error occured while making message intent");
            }
        });

        //CONTROL BUTTON: Reverse
        controlBtnDown.setOnClickListener(v -> {
            try{
                Intent downDirectionIntent = new Intent("sendBTMessage");
                downDirectionIntent.putExtra("msg","r");
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(downDirectionIntent);
            }catch (Exception e){
                Log.e(TAG, "onCreateView: An error occured while making message intent");
            }
        });

        //CONTROL BUTTON: Left
        controlBtnLeft.setOnClickListener(v -> {
            try{
                Intent leftDirectionIntent = new Intent("sendBTMessage");
                leftDirectionIntent.putExtra("msg","tl");
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(leftDirectionIntent);
            }catch (Exception e){
                Log.e(TAG, "onCreateView: An error occured while making message intent");
            }
        });

        //CONTROL BUTTON: Right
        controlBtnRight.setOnClickListener(v -> {
            try{
                Intent rightDirectionIntent = new Intent("sendBTMessage");
                rightDirectionIntent.putExtra("msg","tr");
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(rightDirectionIntent);
            }catch (Exception e){
                Log.e(TAG, "onCreateView: An error occured while making message intent");
            }
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

        // Inflate the layout for this fragment
        return rootview;
    }

    private void arenaSetOptions(){
//        View arenaPopUpView = getLayoutInflater().inflate(R.layout.arena_setup, null);
//        arena_popup = new PopupWindow(arenaPopUpView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
//        arena_popup.setBackgroundDrawable(new BitmapDrawable());
//        arena_popup.showAtLocation(rootview, Gravity.CENTER, 0, 0);

        View popupView = getLayoutInflater().inflate(R.layout.arena_setup, null);
        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true; // lets taps outside the popup also dismiss it
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window tolken
        popupWindow.showAtLocation(rootview, Gravity.CENTER, 0, 0);
    }

    private BroadcastReceiver roboStatusUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try{
                JSONObject msgJSON = new JSONObject(intent.getStringExtra("msg"));
                if(msgJSON.has("status")){
                    txtRoboStatus.setText(msgJSON.getString("status"));
                }else{
                    txtRoboStatus.setText("UNKNOWN");
                }
            }catch (Exception e){
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
}