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

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!initializedIntentListeners){
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(roboStatusUpdateReceiver, new IntentFilter("updateRobocarStatus"));
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
                    btnSetFacing.setText("Stop set direction");
                }else{
                    gridMap.setSetObstacleDirection(settingDir);
                    btnSetFacing.setText("Set direction");
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

    private void showShortToast(String msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
    }

    private void showLongToast(String msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
    }
}