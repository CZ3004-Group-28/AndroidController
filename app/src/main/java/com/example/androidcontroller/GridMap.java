package com.example.androidcontroller;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.valueOf;

public class GridMap extends View {

    public GridMap(Context c) {
        super(c);
        initMap();
    }

    SharedPreferences sharedPreferences;

    private Paint blackPaint = new Paint();
    private Paint obstacleColor = new Paint();
    private Paint robotColor = new Paint();
    private Paint endColor = new Paint();
    private Paint startColor = new Paint();
    private Paint waypointColor = new Paint();
    private Paint unexploredColor = new Paint();
    private Paint exploredColor = new Paint();
    private Paint arrowColor = new Paint();
    //private Paint fastestPathColor = new Paint();
    private Paint imageLine = new Paint();
    private Paint imageLineConfirm = new Paint();

    private static JSONObject receivedJsonObject = new JSONObject();
    private static JSONObject mapInformation;
    private static JSONObject backupMapInformation;
    private static String robotDirection = "None";
    private static int[] startCoord = new int[]{-1, -1};
    private static int[] curCoord = new int[]{-1, -1};
    private static int[] oldCoord = new int[]{-1, -1};
    private static int[] waypointCoord = new int[]{-1, -1};
    private static ArrayList<String[]> arrowCoord = new ArrayList<>();
    private static ArrayList<int[]> obstacleCoord = new ArrayList<>();
    private static boolean autoUpdate = false;
    private static boolean canDrawRobot = false;
    private static boolean setWaypointStatus = false;
    private static boolean startCoordStatus = false;
    private static boolean setObstacleStatus = false;
    private static boolean unSetCellStatus = false;
    private static boolean setExploredStatus = false;
    private static boolean validPosition = false;
    private static boolean waypointNew=false;
    private boolean newEndCoord=false;
    private Bitmap arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_error);
    private static boolean setObstacleDirection = false;

    private static final String TAG = "GridMap";
    private static final int COL = 20;
    private static final int ROW = 20;
    private static float cellSize;
    private static Cell[][] cells;
    private Canvas can;

    private boolean mapDrawn = false;
    public static String publicMDFExploration;
    public static String publicMDFObstacle;

    private static int[] selectedObsCoord = new int[3];
    private static boolean obsSelected = false;
    private static ArrayList<Cell> oCellArr = new ArrayList<Cell>();
    //private static ArrayList<Integer> oCellArrDirection = new ArrayList<Integer>();
    //private static ArrayList<ObstacleCellArray> oCellArr = new ArrayList<ObstacleCellArray>();

    int newDirection = -1; // 0:None 1: Up, 2: Down, 3: Left, 4:Right
    int switchDirection = -1; // 0:None 1: Up, 2: Down, 3: Left, 4:Right
    String[] directionList = new String[] {"NONE","UP", "DOWN", "LEFT", "RIGHT"};
    private static String obsSelectedFacing = null; // <-- newly added
    private static String obsTargetImageID = null; // <-- newly added
    private static int[] obstacleNoArray = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};

    //For RPI message
    String rpiRobot = "";
    String rpiObstacle;

    public GridMap(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initMap();
        blackPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        obstacleColor.setColor(Color.BLACK);
        robotColor.setColor(Color.CYAN);
        //endColor.setColor(Color.TRANSPARENT);
        endColor.setColor(Color.GREEN);
        startColor.setColor(Color.CYAN);
        waypointColor.setColor(Color.parseColor("#fefdca"));
        unexploredColor.setColor(Color.LTGRAY);
        exploredColor.setColor(Color.WHITE);
        arrowColor.setColor(Color.BLACK);
        //fastestPathColor.setColor(Color.MAGENTA);

        imageLine.setStyle(Paint.Style.STROKE);
        imageLine.setColor(Color.YELLOW);
        imageLine.setStrokeWidth(2); // <-- line thickness

        imageLineConfirm.setStyle(Paint.Style.STROKE);
        imageLineConfirm.setColor(Color.YELLOW);
        imageLineConfirm.setStrokeWidth(5); // <-- line thickness

        // get shared preferences
        sharedPreferences = getContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
    }

    private void initMap() {
        setWillNotDraw(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        showLog("Entering onDraw");
        super.onDraw(canvas);
        showLog("Redrawing map");

        //CREATE CELL COORDINATES
        Log.d(TAG,"Creating Cell");

        if (!mapDrawn) {
            String[] dummyArrowCoord = new String[3];
            dummyArrowCoord[0] = "1";
            dummyArrowCoord[1] = "1";
            dummyArrowCoord[2] = "dummy";
            arrowCoord.add(dummyArrowCoord);
            this.createCell();
            // TODO: Remove end coordinate
            //this.setEndCoord(COL-1, ROW-1);
            newEndCoord=true;
            showLog("Map drawn");
        }

        drawIndividualCell(canvas);
        drawHorizontalLines(canvas);
        drawVerticalLines(canvas);
        drawGridNumber(canvas);

        if(newEndCoord==true){
            int endcol = COL-1;
            int endrow = ROW-1;
            endrow = this.convertRow(endrow);
            RectF endrect = new RectF(endcol * cellSize, endrow * cellSize, (endcol + 1) * cellSize, (endrow + 1) * cellSize);
            //Bitmap goal = BitmapFactory.decodeResource(getResources(), R.drawable.goal);
            //canvas.drawBitmap(goal, null, endrect, null);
            mapDrawn = true;
            newEndCoord=false;
        }

        if (getCanDrawRobot())
            drawRobot(canvas, curCoord);
        //drawArrow(canvas, arrowCoord);

        showLog("Exiting onDraw");
    }
    /*private void drawWaypoint(Canvas canvas){
        int col=waypointCoord[0];
        int row=waypointCoord[1];
        row = this.convertRow(row);
        cells[col][row].setType("waypoint");
        RectF rect = new RectF(col * cellSize, row * cellSize, (col + 1) * cellSize, (row + 1) * cellSize);
        Bitmap dog = BitmapFactory.decodeResource(getResources(), R.drawable.dog);
        canvas.drawBitmap(dog, null, rect, null);
    }*/
    public static String returnObstacleFacing(int x, int y){
        return cells[x][y].obstacleFacing;
    }

    private void drawIndividualCell(Canvas canvas) {
        showLog("Entering drawIndividualCell");

        for (int x = 1; x <= COL; x++)
            for (int y = 0; y < ROW; y++)
                for (int i = 0; i < this.getArrowCoord().size(); i++)
                    if (!cells[x][y].type.equals("image") && cells[x][y].getId() == -1) {
                        canvas.drawRect(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY, cells[x][y].paint);

                        // this is for drawing numbers in obstacle and using oCellArr arraylist for drag and drop
                        if (cells[x][y].type.equals("obstacle")) {
                            boolean written = false;
                            for (int a = 0; a < oCellArr.size(); a++) {
                                if (cells[x][y] == oCellArr.get(a)) {

                                    // TODO: NEW Target ID
                                    if(cells[x][y].targetID == null) {
                                        //canvas.drawText(Integer.toString(a + 1), cells[x][y].startX + (cellSize / 3.2f), cells[x][y].startY + (cellSize / 1.5f), exploredColor);
                                        canvas.drawText(Integer.toString(cells[x][y].obstacleNo), cells[x][y].startX + (cellSize / 3.2f), cells[x][y].startY + (cellSize / 1.5f), exploredColor);
                                    } else{
                                        Paint textPaint2 = new Paint();
                                        textPaint2.setTextSize(20);
                                        textPaint2.setColor(Color.WHITE);
                                        textPaint2.setTextAlign(Paint.Align.CENTER);
                                        canvas.drawRect(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY, blackPaint);
                                        canvas.drawText(cells[x][y].targetID, (cells[x][y].startX+cells[x][y].endX)/2, cells[x][y].endY + (cells[x][y].startY-cells[x][y].endY)/4, textPaint2);
                                    }
                                    written = true;
                                    break;

//                                    canvas.drawText(Integer.toString(a + 1), cells[x][y].startX + (cellSize / 3f), cells[x][y].startY + (cellSize / 1.5f), exploredColor);
//
//                                    //TODO: new direction
//                                    switch (oCellArrDirection.get(a)) {
//                                        case 1:
//                                            // Up
//                                            canvas.drawRect(cells[x][y].startX + 2, cells[x][y].startY + 1, cells[x][y].endX, cells[x][y].endY - (cellSize / 1.1f), imageLine);
//                                            break;
//                                        case 2:
//                                            // Down
//                                            canvas.drawRect(cells[x][y].startX + 2, cells[x][y].startY + (cellSize / 1f), cells[x][y].endX, cells[x][y].endY, imageLine);
//                                            break;
//                                        case 3:
//                                            // Left
//                                            canvas.drawRect(cells[x][y].startX + 1, cells[x][y].startY + 2, cells[x][y].endX - (cellSize / 1.1f), cells[x][y].endY, imageLine);
//                                            break;
//                                        case 4:
//                                            // Right
//                                            canvas.drawRect(cells[x][y].startX + (cellSize / 1f), cells[x][y].startY, cells[x][y].endX, cells[x][y].endY, imageLine);
//                                            break;
//
//                                    }
//
//                                    written = true;
//                                    break;
                                }
                            }

                            if (written == false) {

                                // TODO: NEW Obstacle ID
                                oCellArr.add(cells[x][y]);
                                //cells[x][y].obstacleNo = (oCellArr.size()); // assign number to id
                                if(cells[x][y].targetID == null){
                                    //canvas.drawText(Integer.toString(oCellArr.size()), cells[x][y].startX + (cellSize / 3.2f), cells[x][y].startY + (cellSize / 1.5f), exploredColor);
                                    canvas.drawText(Integer.toString(cells[x][y].obstacleNo), cells[x][y].startX + (cellSize / 3.2f), cells[x][y].startY + (cellSize / 1.5f), exploredColor);
                                } else {
                                    Paint textPaint2 = new Paint();
                                    textPaint2.setTextSize(20);
                                    textPaint2.setColor(Color.WHITE);
                                    textPaint2.setTextAlign(Paint.Align.CENTER);
                                    canvas.drawRect(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY, blackPaint);
                                    canvas.drawText(cells[x][y].targetID, (cells[x][y].startX+cells[x][y].endX)/2, cells[x][y].endY + (cells[x][y].startY-cells[x][y].endY)/4, textPaint2);
                                }

//                                // add a numbered obstacle cell
//                                oCellArr.add(cells[x][y]);
//                                canvas.drawText(Integer.toString(oCellArr.size()), cells[x][y].startX + (cellSize / 3f), cells[x][y].startY + (cellSize / 1.5f), exploredColor);
//
//                                //TODO: new direction
//                                oCellArrDirection.add(newDirection);
//                                switch (newDirection) {
//                                    case 1:
//                                        // Up
//                                        canvas.drawRect(cells[x][y].startX + 2 , cells[x][y].startY + 1, cells[x][y].endX, cells[x][y].endY - (cellSize / 1.1f), imageLine);
//                                        break;
//                                    case 2:
//                                        // Down
//                                        canvas.drawRect(cells[x][y].startX + 2, cells[x][y].startY + (cellSize / 1f), cells[x][y].endX, cells[x][y].endY, imageLine);
//                                        break;
//                                    case 3:
//                                        // Left
//                                        canvas.drawRect(cells[x][y].startX + 1, cells[x][y].startY + 2, cells[x][y].endX - (cellSize / 1.1f), cells[x][y].endY, imageLine);
//                                        break;
//                                    case 4:
//                                        // Right
//                                        canvas.drawRect(cells[x][y].startX + (cellSize / 1f), cells[x][y].startY, cells[x][y].endX, cells[x][y].endY, imageLine);
//                                        break;
//
//                                }
                            }
                            /*switch (iDirection) {
                                case 0:
                                    // Up
                                    canvas.drawRect(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY - (cellSize / 1f), imageLine);
                                    break;
                                case 1:
                                    // Down
                                    canvas.drawRect(cells[x][y].startX, cells[x][y].startY + (cellSize / 1f), cells[x][y].endX, cells[x][y].endY, imageLine);
                                    break;
                                case 2:
                                    // Left
                                    canvas.drawRect(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX - (cellSize / 1f), cells[x][y].endY, imageLine);
                                    break;
                                case 3:
                                    // Right
                                    canvas.drawRect(cells[x][y].startX + (cellSize / 1f), cells[x][y].startY, cells[x][y].endX, cells[x][y].endY, imageLine);
                                    break;

                            }*/
                        }
                    }
                    else {
                        // this part draw the numbers out on the map
                        Paint textPaint = new Paint();
                        textPaint.setTextSize(20);
                        textPaint.setColor(Color.WHITE);
                        textPaint.setTextAlign(Paint.Align.CENTER);
                        canvas.drawRect(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY, cells[x][y].paint);
                        canvas.drawText(String.valueOf(cells[x][y].getId()),(cells[x][y].startX+cells[x][y].endX)/2, cells[x][y].endY + (cells[x][y].startY-cells[x][y].endY)/4, textPaint);
                    }

        // Obstacle Face --> Assign Drawable to the cells @ GridMap.Java (Start)
        for (int x = 1; x <= COL; x++)
            for (int y = 0; y < ROW; y++)
                for (int i = 0; i < this.getArrowCoord().size(); i++) {
                    // Update Target Image ID
                    /*
                    if(cells[x][y].obstacleNo != -1 && cells[x][y].targetID != null){
                        Paint textPaint2 = new Paint();
                        textPaint2.setTextSize(20);
                        textPaint2.setColor(Color.WHITE);
                        textPaint2.setTextAlign(Paint.Align.CENTER);
                        canvas.drawRect(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY, blackPaint);
                        canvas.drawText(cells[x][y].targetID, (cells[x][y].startX+cells[x][y].endX)/2, cells[x][y].endY + (cells[x][y].startY-cells[x][y].endY)/4, textPaint2);
                    } */

                    // TODO: NEW Obstacle Facing
                    if (cells[x][y].obstacleFacing != null) {
                        if (cells[x][y].obstacleFacing == "UP" && cells[x][y].isDirection == false ) {

                            canvas.drawRect(cells[x][y].startX + 2 , cells[x][y].startY + 1, cells[x][y].endX, cells[x][y].endY - (cellSize / 1.1f), imageLine);

                            //RectF rect = new RectF(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY);
//                            Drawable face = getResources().getDrawable(R.drawable.border_top);
//                            canvas.drawBitmap(drawableToBitmap(face), null, rect, null);
                            //Toast.makeText((Activity) this.getContext(), "Update", Toast.LENGTH_SHORT).show();
                        }
                        if (cells[x][y].obstacleFacing == "DOWN" && cells[x][y].isDirection == false) {

                            canvas.drawRect(cells[x][y].startX + 2, cells[x][y].startY + (cellSize / 1f) - 2, cells[x][y].endX, cells[x][y].endY - 1, imageLine);
//                            RectF rect = new RectF(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY);
//                            Drawable face = getResources().getDrawable(R.drawable.border_bottom);
//                            canvas.drawBitmap(drawableToBitmap(face), null, rect, null);
                        }
                        if (cells[x][y].obstacleFacing == "LEFT" && cells[x][y].isDirection == false) {
                            canvas.drawRect(cells[x][y].startX + 1, cells[x][y].startY + 2, cells[x][y].endX - (cellSize / 1.1f), cells[x][y].endY, imageLine);

//                            RectF rect = new RectF(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY);
//                            Drawable face = getResources().getDrawable(R.drawable.border_left);
//                            canvas.drawBitmap(drawableToBitmap(face), null, rect, null);
                        }
                        if (cells[x][y].obstacleFacing == "RIGHT" && cells[x][y].isDirection == false) {

                            canvas.drawRect(cells[x][y].startX + (cellSize / 1f) -2, cells[x][y].startY, cells[x][y].endX -1, cells[x][y].endY, imageLine);

//                            RectF rect = new RectF(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY);
//                            Drawable face = getResources().getDrawable(R.drawable.border_right);
//                            canvas.drawBitmap(drawableToBitmap(face), null, rect, null);
                        }

                        // for confirm direction display
                        if (cells[x][y].obstacleFacing == "UP" && cells[x][y].isDirection == true ) {


                            canvas.drawRect(cells[x][y].startX + 2 , cells[x][y].startY + 1, cells[x][y].endX, cells[x][y].endY - (cellSize / 1.1f), imageLineConfirm);

                            //RectF rect = new RectF(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY);
//                            Drawable face = getResources().getDrawable(R.drawable.border_top);
//                            canvas.drawBitmap(drawableToBitmap(face), null, rect, null);
                            //Toast.makeText((Activity) this.getContext(), "Update", Toast.LENGTH_SHORT).show();
                        }
                        if (cells[x][y].obstacleFacing == "DOWN" && cells[x][y].isDirection == true) {

                            canvas.drawRect(cells[x][y].startX + 2, cells[x][y].startY + (cellSize / 1f) - 2, cells[x][y].endX, cells[x][y].endY - 1, imageLineConfirm);
//                            RectF rect = new RectF(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY);
//                            Drawable face = getResources().getDrawable(R.drawable.border_bottom);
//                            canvas.drawBitmap(drawableToBitmap(face), null, rect, null);
                        }
                        if (cells[x][y].obstacleFacing == "LEFT" && cells[x][y].isDirection == true) {


                            canvas.drawRect(cells[x][y].startX + 1, cells[x][y].startY + 2, cells[x][y].endX - (cellSize / 1.1f), cells[x][y].endY, imageLineConfirm);

//                            RectF rect = new RectF(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY);
//                            Drawable face = getResources().getDrawable(R.drawable.border_left);
//                            canvas.drawBitmap(drawableToBitmap(face), null, rect, null);
                        }
                        if (cells[x][y].obstacleFacing == "RIGHT" && cells[x][y].isDirection == true) {


                            canvas.drawRect(cells[x][y].startX + (cellSize / 1f) -2, cells[x][y].startY, cells[x][y].endX -1, cells[x][y].endY, imageLineConfirm);

//                            RectF rect = new RectF(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY);
//                            Drawable face = getResources().getDrawable(R.drawable.border_right);
//                            canvas.drawBitmap(drawableToBitmap(face), null, rect, null);
                        }
                    } // End
                }

        showLog("Exiting drawIndividualCell");
    }
    // Obstacle Face - Function to convert drawable to bitmap  @ GridMap.java (start)
    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : 1;
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : 1;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    } // end

    public void drawImageNumberCell(int id,int x, int y) {
        cells[x+1][19-y].setType("image");
        cells[x+1][19-y].setId(id);
        this.invalidate();
    }

    public void setCellType(int x, int y, String type){
        cells[x+1][19-y].setType(type);
        cells[x+1][19-y].setId(-1);
        this.invalidate();
    }

    public void updateImageNumberCell(int obstacleNo, String targetID){
        // find the obstacle no which has the same id
        for (int x = 1; x <= COL; x++)
            for (int y = 0; y < ROW; y++)
                for (int i = 0; i < this.getArrowCoord().size(); i++)
                    if (cells[x][y].obstacleNo == obstacleNo) {
                        cells[x][y].targetID = targetID;
                        //cells[x][y].isDirection = true;
                    }
        this.invalidate();
    }

    public void drawRobotIfEmpty(){

    }

    public void updateImageNumberCell(int obstacleNo, String targetID, String obstacleFacing){
        // find the obstacle no which has the same id
        for (int x = 1; x <= COL; x++)
            for (int y = 0; y < ROW; y++)
                for (int i = 0; i < this.getArrowCoord().size(); i++)
                    if (cells[x][y].obstacleNo == obstacleNo && cells[x][y].type == "obstacle") {
                        cells[x][y].targetID = targetID;
                        cells[x][y].isDirection = true;
                        if(obstacleFacing.contains("UP")) {
                            cells[x][y].setobstacleFacing("UP");
                        }
                        if(obstacleFacing.contains("DOWN")) {
                            cells[x][y].setobstacleFacing("DOWN");
                        }
                        if(obstacleFacing.contains("RIGHT")) {
                            cells[x][y].setobstacleFacing("RIGHT");
                        }
                        if(obstacleFacing.contains("LEFT")) {
                            cells[x][y].setobstacleFacing("LEFT");

                        }
                        break;
                    }
        this.invalidate();
    }

    // For RPI
    public void updateImageNumberCellRPI(int x, int y, String targetID, String obstacleFacing)
    {
        cells[x+1][19-y].targetID = targetID;
        cells[x+1][19-y].isDirection = true;
        if(obstacleFacing.contains("N")) {
            cells[x+1][19-y].setobstacleFacing("UP");
        }
        if(obstacleFacing.contains("S")) {
            cells[x+1][19-y].setobstacleFacing("DOWN");
        }
        if(obstacleFacing.contains("E")) {
            cells[x+1][19-y].setobstacleFacing("RIGHT");
        }
        if(obstacleFacing.contains("W")) {
            cells[x+1][19-y].setobstacleFacing("LEFT");

        }
        this.invalidate();
    }
    public void updateImageNumberCellRPI(int x, int y, String targetID)
    {
        cells[x+1][19-y].targetID = targetID;
        this.invalidate();
    }

    private void drawHorizontalLines(Canvas canvas) {
        for (int y = 0; y <= ROW; y++)
            canvas.drawLine(cells[1][y].startX, cells[1][y].startY - (cellSize / 30), cells[20][y].endX, cells[20][y].startY - (cellSize / 30), blackPaint);
    }

    private void drawVerticalLines(Canvas canvas) {
        for (int x = 0; x <= COL; x++)
            canvas.drawLine(cells[x][0].startX - (cellSize / 30) + cellSize, cells[x][0].startY - (cellSize / 30), cells[x][0].startX - (cellSize / 30) + cellSize, cells[x][19].endY + (cellSize / 30), blackPaint);
    }

    private void drawGridNumber(Canvas canvas) {
        showLog("Entering drawGridNumber");
        for (int x = 1; x <= COL; x++) {
            if (x > 9)
                canvas.drawText(Integer.toString(x-1), cells[x][20].startX + (cellSize / 5), cells[x][20].startY + (cellSize / 3), blackPaint);
            else
                canvas.drawText(Integer.toString(x-1), cells[x][20].startX + (cellSize / 3), cells[x][20].startY + (cellSize / 3), blackPaint);
        }
        for (int y = 0; y < ROW; y++) {
            if ((20 - y) > 9)
                canvas.drawText(Integer.toString(19 - y), cells[0][y].startX + (cellSize / 2), cells[0][y].startY + (cellSize / 1.5f), blackPaint);
            else
                canvas.drawText(Integer.toString(19 - y), cells[0][y].startX + (cellSize / 1.5f), cells[0][y].startY + (cellSize / 1.5f), blackPaint);
        }
        showLog("Exiting drawGridNumber");
    }

    private void drawRobot(Canvas canvas, int[] curCoord) {
        showLog("Entering drawRobot");
        int androidRowCoord = this.convertRow(curCoord[1]);
        if(newEndCoord){
            int endcol = 19;
            int endrow = 19;
            endrow = this.convertRow(endrow);
            RectF endrect = new RectF(endcol * cellSize, endrow * cellSize, (endcol + 1) * cellSize, (endrow + 1) * cellSize);
            //Bitmap goal = BitmapFactory.decodeResource(getResources(), R.drawable.goal);
            //canvas.drawBitmap(goal, null, endrect, null);
            newEndCoord=false;
        }
        for (int y = androidRowCoord; y <= androidRowCoord + 1; y++){
            canvas.drawLine(cells[curCoord[0] - 1][y].startX, cells[curCoord[0] - 1][y].startY - (cellSize / 30), cells[curCoord[0] + 1][y].endX, cells[curCoord[0] + 1][y].startY - (cellSize / 30), robotColor );
        }
        //canvas.drawBitmap(bitmap, null, mRedPaddleRect, mPaint);
        for (int x = curCoord[0] - 1; x < curCoord[0] + 1; x++){
            canvas.drawLine(cells[x][androidRowCoord - 1].startX - (cellSize / 30) + cellSize, cells[x][androidRowCoord - 1].startY, cells[x][androidRowCoord + 1].startX - (cellSize / 30) + cellSize, cells[x][androidRowCoord + 1].endY, robotColor);
        }
        int col = curCoord[0];
        int row = androidRowCoord;
        RectF rect = new RectF(col * cellSize, row * cellSize, (col + 1) * cellSize, (row + 1) * cellSize);
        //Bitmap robot = BitmapFactory.decodeResource(getResources(), R.drawable.robot);
        //canvas.drawBitmap(robot, null, rect, null);
        //canvas.drawRect(cells[curCoord[0] - 1][androidRowCoord - 1].startX,cells[curCoord[0] - 1][androidRowCoord - 1].startY,cells[curCoord[0] - 1][androidRowCoord + 1].startX,cells[curCoord[0] - 1][androidRowCoord + 1].endY,waypointColor);

        switch (this.getRobotDirection()) {
            case "up":

                //left drawn line
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord + 1].startX, cells[curCoord[0] - 1][androidRowCoord + 1].endY, (cells[curCoord[0]][androidRowCoord - 1].startX + cells[curCoord[0]][androidRowCoord - 1].endX) / 2, cells[curCoord[0]][androidRowCoord - 1].startY, blackPaint);

                //right drawn line
                canvas.drawLine((cells[curCoord[0]][androidRowCoord - 1].startX + cells[curCoord[0]][androidRowCoord - 1].endX) / 2, cells[curCoord[0]][androidRowCoord - 1].startY, cells[curCoord[0] + 1][androidRowCoord + 1].endX, cells[curCoord[0] + 1][androidRowCoord + 1].endY, blackPaint);

                //NE
                //left drawn line
                //canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord].startX, cells[curCoord[0] - 1][androidRowCoord].endY, (cells[curCoord[0] + 1][androidRowCoord - 1].startX + cells[curCoord[0] + 2][androidRowCoord - 1 ].endX) / 2, cells[curCoord[0] + 2][androidRowCoord - 1].startY, blackPaint);
                //canvas.drawLine((cells[curCoord[0] + 1][androidRowCoord - 1].startX + cells[curCoord[0] + 2][androidRowCoord - 1].endX) / 2, cells[curCoord[0] + 1][androidRowCoord - 1].startY, cells[curCoord[0] - 1][androidRowCoord].endX, cells[curCoord[0] - 1][androidRowCoord + 1].endY, blackPaint);

                //NW
                //left drawn line
                //canvas.drawLine(cells[curCoord[0] + 1][androidRowCoord + 1].startX, cells[curCoord[0] + 1][androidRowCoord + 1].endY, (cells[curCoord[0] - 2][androidRowCoord].startX + cells[curCoord[0] - 1][androidRowCoord].endX) / 2, cells[curCoord[0] - 1][androidRowCoord - 1].startY, blackPaint);
                //canvas.drawLine((cells[curCoord[0] - 2][androidRowCoord].startX + cells[curCoord[0] - 1][androidRowCoord].endX) / 2, cells[curCoord[0]][androidRowCoord - 1].startY, cells[curCoord[0] + 1][androidRowCoord].endX, cells[curCoord[0] + 1][androidRowCoord].endY, blackPaint);

                // SE
                //canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord].startX, cells[curCoord[0] - 1][androidRowCoord].startY, (cells[curCoord[0] + 1][androidRowCoord + 1].startX + cells[curCoord[0] + 2][androidRowCoord + 1].endX) / 2, cells[curCoord[0] + 1][androidRowCoord + 1].endY, blackPaint);
                //canvas.drawLine((cells[curCoord[0] + 1][androidRowCoord + 1].startX + cells[curCoord[0] + 2][androidRowCoord + 1].endX) / 2, cells[curCoord[0]][androidRowCoord + 1].endY, cells[curCoord[0] - 1][androidRowCoord - 1].endX, cells[curCoord[0] - 1][androidRowCoord - 1].startY, blackPaint);


                // SW
                //canvas.drawLine(cells[curCoord[0] + 1][androidRowCoord - 1].startX, cells[curCoord[0] + 1][androidRowCoord - 1].startY, (cells[curCoord[0] - 2][androidRowCoord + 1].startX + cells[curCoord[0] - 1][androidRowCoord + 1].endX) / 2, cells[curCoord[0] - 1][androidRowCoord + 1].endY, blackPaint);
                //canvas.drawLine((cells[curCoord[0] - 2][androidRowCoord + 1].startX + cells[curCoord[0] - 1][androidRowCoord + 1].endX) / 2, cells[curCoord[0]][androidRowCoord + 1].endY, cells[curCoord[0] + 1][androidRowCoord].endX, cells[curCoord[0] + 1][androidRowCoord].startY, blackPaint);


                //showLog( " curCoord[0]: " +Integer.toString(curCoord[0]) + " androidRowCoord: " +Integer.toString(20 - (androidRowCoord)) + " curCoord[1]: " + Integer.toString(curCoord[1]));


                break;
            case "down":
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord - 1].startX, cells[curCoord[0] - 1][androidRowCoord - 1].startY, (cells[curCoord[0]][androidRowCoord + 1].startX + cells[curCoord[0]][androidRowCoord + 1].endX) / 2, cells[curCoord[0]][androidRowCoord + 1].endY, blackPaint);
                canvas.drawLine((cells[curCoord[0]][androidRowCoord + 1].startX + cells[curCoord[0]][androidRowCoord + 1].endX) / 2, cells[curCoord[0]][androidRowCoord + 1].endY, cells[curCoord[0] + 1][androidRowCoord - 1].endX, cells[curCoord[0] + 1][androidRowCoord - 1].startY, blackPaint);
                break;
            case "right":
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord - 1].startX, cells[curCoord[0] - 1][androidRowCoord - 1].startY, cells[curCoord[0] + 1][androidRowCoord].endX, cells[curCoord[0] + 1][androidRowCoord - 1].endY + (cells[curCoord[0] + 1][androidRowCoord].endY - cells[curCoord[0] + 1][androidRowCoord - 1].endY) / 2, blackPaint);
                canvas.drawLine(cells[curCoord[0] + 1][androidRowCoord].endX, cells[curCoord[0] + 1][androidRowCoord - 1].endY + (cells[curCoord[0] + 1][androidRowCoord].endY - cells[curCoord[0] + 1][androidRowCoord - 1].endY) / 2, cells[curCoord[0] - 1][androidRowCoord + 1].startX, cells[curCoord[0] - 1][androidRowCoord + 1].endY, blackPaint);
                break;
            case "left":
                canvas.drawLine(cells[curCoord[0] + 1][androidRowCoord - 1].endX, cells[curCoord[0] + 1][androidRowCoord - 1].startY, cells[curCoord[0] - 1][androidRowCoord].startX, cells[curCoord[0] - 1][androidRowCoord - 1].endY + (cells[curCoord[0] - 1][androidRowCoord].endY - cells[curCoord[0] - 1][androidRowCoord - 1].endY) / 2, blackPaint);
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord].startX, cells[curCoord[0] - 1][androidRowCoord - 1].endY + (cells[curCoord[0] - 1][androidRowCoord].endY - cells[curCoord[0] - 1][androidRowCoord - 1].endY) / 2, cells[curCoord[0] + 1][androidRowCoord + 1].endX, cells[curCoord[0] + 1][androidRowCoord + 1].endY, blackPaint);
                break;
            case "upright": // NE
                //NE
                //left drawn line
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord].startX, cells[curCoord[0] - 1][androidRowCoord].endY, (cells[curCoord[0] + 1][androidRowCoord - 1].startX + cells[curCoord[0] + 2][androidRowCoord - 1 ].endX) / 2, cells[curCoord[0] + 2][androidRowCoord - 1].startY, blackPaint);
                canvas.drawLine((cells[curCoord[0] + 1][androidRowCoord - 1].startX + cells[curCoord[0] + 2][androidRowCoord - 1].endX) / 2, cells[curCoord[0] + 1][androidRowCoord - 1].startY, cells[curCoord[0] - 1][androidRowCoord].endX, cells[curCoord[0] - 1][androidRowCoord + 1].endY, blackPaint);

                break;
            case "upleft": // NW
                //NW
                //left drawn line
                canvas.drawLine(cells[curCoord[0] + 1][androidRowCoord + 1].startX, cells[curCoord[0] + 1][androidRowCoord + 1].endY, (cells[curCoord[0] - 2][androidRowCoord].startX + cells[curCoord[0] - 1][androidRowCoord].endX) / 2, cells[curCoord[0] - 1][androidRowCoord - 1].startY, blackPaint);
                canvas.drawLine((cells[curCoord[0] - 2][androidRowCoord].startX + cells[curCoord[0] - 1][androidRowCoord].endX) / 2, cells[curCoord[0]][androidRowCoord - 1].startY, cells[curCoord[0] + 1][androidRowCoord].endX, cells[curCoord[0] + 1][androidRowCoord].endY, blackPaint);

                break;
            case "downright": // SE
                // SE
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord].startX, cells[curCoord[0] - 1][androidRowCoord].startY, (cells[curCoord[0] + 1][androidRowCoord + 1].startX + cells[curCoord[0] + 2][androidRowCoord + 1].endX) / 2, cells[curCoord[0] + 1][androidRowCoord + 1].endY, blackPaint);
                canvas.drawLine((cells[curCoord[0] + 1][androidRowCoord + 1].startX + cells[curCoord[0] + 2][androidRowCoord + 1].endX) / 2, cells[curCoord[0]][androidRowCoord + 1].endY, cells[curCoord[0] - 1][androidRowCoord - 1].endX, cells[curCoord[0] - 1][androidRowCoord - 1].startY, blackPaint);

                break;

            case "downleft": // SW
                // SW
                canvas.drawLine(cells[curCoord[0] + 1][androidRowCoord - 1].startX, cells[curCoord[0] + 1][androidRowCoord - 1].startY, (cells[curCoord[0] - 2][androidRowCoord + 1].startX + cells[curCoord[0] - 1][androidRowCoord + 1].endX) / 2, cells[curCoord[0] - 1][androidRowCoord + 1].endY, blackPaint);
                canvas.drawLine((cells[curCoord[0] - 2][androidRowCoord + 1].startX + cells[curCoord[0] - 1][androidRowCoord + 1].endX) / 2, cells[curCoord[0]][androidRowCoord + 1].endY, cells[curCoord[0] + 1][androidRowCoord].endX, cells[curCoord[0] + 1][androidRowCoord].startY, blackPaint);

                break;
            default:
                Toast.makeText(this.getContext(), "Error with drawing robot (unknown direction)", Toast.LENGTH_LONG).show();
                break;
        }
        showLog("Exiting drawRobot");
    }

    private ArrayList<String[]> getArrowCoord() {
        return arrowCoord;
    }

    public String getRobotDirection() {
        return robotDirection;
    }

    public void setNewDirection(int newDirection)
    {
        this.newDirection = newDirection;
    }

    public void setAutoUpdate(boolean autoUpdate) throws JSONException {
        showLog(String.valueOf(backupMapInformation));
        if (!autoUpdate)
            backupMapInformation = this.getReceivedJsonObject();
        else {
            setReceivedJsonObject(backupMapInformation);
            backupMapInformation = null;
            this.updateMapInformation();
        }
        GridMap.autoUpdate = autoUpdate;
    }

    public JSONObject getReceivedJsonObject() {
        return receivedJsonObject;
    }

    public void setReceivedJsonObject(JSONObject receivedJsonObject) {
        showLog("Entered setReceivedJsonObject");
        GridMap.receivedJsonObject = receivedJsonObject;
        backupMapInformation = receivedJsonObject;
    }

    /*public static void findMDF(){
        String obstacleString="";
        String exploredString="";
        String hexStringExplored="";
        BigInteger hexBigIntegerExplored, hexBigIntegerObstacle;
        for (int y=ROW-1; y>=0; y--)
            for (int x=1; x<=COL; x++)
                if (cells[x][y].type.equals("explored") || cells[x][y].type.equals("robot") || cells[x][y].type.equals("obstacle") || cells[x][y].type.equals("arrow"))
                    exploredString = exploredString + "1";
                else
                    exploredString = exploredString + "0";
        exploredString = exploredString + "11";

        hexBigIntegerExplored = new BigInteger(exploredString, 2);
        hexStringExplored = hexBigIntegerExplored.toString(16);
        for (int y=ROW-1; y>=0; y--)
            for (int x=1; x<=COL; x++)
                if (cells[x][y].type.equals("explored") || cells[x][y].type.equals("robot"))
                    obstacleString = obstacleString + "0";
                else if (cells[x][y].type.equals("obstacle") || cells[x][y].type.equals("arrow"))
                    obstacleString = obstacleString + "1";

        while ((obstacleString.length() % 8) != 0) {
            obstacleString = obstacleString + "0";
        }

        //publicMDFObstacle = obstacleString;
        //publicMDFExploration = exploredString;
        publicMDFExploration = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";

        publicMDFObstacle = binaryToHex(obstacleString);
        //publicMDFExploration = binaryToHex(exploredString);
    }*/
    public static String binaryToHex(String bin) {
        String hex;
        BigInteger b = new BigInteger(bin, 2);
        hex=b.toString(16);
        return hex;
    }
    public boolean getAutoUpdate() {
        return autoUpdate;
    }

    public boolean getMapDrawn() {
        return mapDrawn;
    }

    private void setValidPosition(boolean status) {
        validPosition = status;
    }

    public boolean getValidPosition() {
        return validPosition;
    }

    public void setUnSetCellStatus(boolean status) {
        unSetCellStatus = status;
    }

    public boolean getUnSetCellStatus() {
        return unSetCellStatus;
    }

    public void setSetObstacleDirection(boolean status)
    {
        setObstacleDirection = status;
    }

    public boolean getSetObstacleDirection()
    {
        return setObstacleDirection;
    }

    public void setSetObstacleStatus(boolean status) {
        setObstacleStatus = status;
    }

    public boolean getSetObstacleStatus() {
        return setObstacleStatus;
    }

    public void setExploredStatus(boolean status) {
        setExploredStatus = status;
    }

    public boolean getExploredStatus() {
        return setExploredStatus;
    }

    public void setStartCoordStatus(boolean status) {
        startCoordStatus = status;
    }

    private boolean getStartCoordStatus() {
        return startCoordStatus;
    }

    public void setWaypointStatus(boolean status) {
        setWaypointStatus = status;
    }

    public boolean getCanDrawRobot() {
        return canDrawRobot;
    }

    private void createCell() {
        showLog("Entering cellCreate");
        cells = new Cell[COL + 1][ROW + 1];
        this.calculateDimension();
        cellSize = this.getCellSize();

        for (int x = 0; x <= COL; x++)
            for (int y = 0; y <= ROW; y++)
                cells[x][y] = new Cell(x * cellSize + (cellSize / 30), y * cellSize + (cellSize / 30), (x + 1) * cellSize, (y + 1) * cellSize, unexploredColor, "unexplored");
        showLog("Exiting createCell");
    }

    public void setEndCoord(int col, int row) {
        showLog("Entering setEndCoord");
        row = this.convertRow(row);
        for (int x = col - 1; x <= col + 1; x++)
            for (int y = row - 1; y <= row + 1; y++)
                cells[x][y].setType("end");

        showLog("Exiting setEndCoord");
    }

    public void setStartCoord(int col, int row){
        showLog("Entering setStartCoord");
        startCoord[0] = col;
        startCoord[1] = row;
        String direction = getRobotDirection();
        if(direction.equals("None")) {
            direction = "up";
        }
        if (this.getStartCoordStatus())
            this.setCurCoord(col, row, direction);
        showLog("Exiting setStartCoord");
    }

    private int[] getStartCoord() {
        return startCoord;
    }

    public void setCurCoord(int col, int row, String direction) {
        showLog("Entering setCurCoord");
        curCoord[0] = col;
        curCoord[1] = row;
        this.setRobotDirection(direction);
        this.updateRobotAxis(col, row, direction);

        row = this.convertRow(row);
        for (int x = col - 1; x <= col + 1; x++)
            for (int y = row - 1; y <= row + 1; y++)
                cells[x][y].setType("robot");
        showLog("Exiting setCurCoord");
    }

    public int[] getCurCoord() {
        return curCoord;
    }

    private void calculateDimension() {
        this.setCellSize(getWidth()/(COL+1));
    }

    private int convertRow(int row) {
        return (20 - row);
    }

    private void setCellSize(float cellSize) {
        GridMap.cellSize = cellSize;
    }

    private float getCellSize() {
        return cellSize;
    }

    private void setOldRobotCoord(int oldCol, int oldRow) {
        showLog("Entering setOldRobotCoord");
        oldCoord[0] = oldCol;
        oldCoord[1] = oldRow;
        oldRow = this.convertRow(oldRow);
        for (int x = oldCol - 1; x <= oldCol + 1; x++)
            for (int y = oldRow - 1; y <= oldRow + 1; y++)
                cells[x][y].setType("explored");
        showLog("Exiting setOldRobotCoord");
    }

    public void unsetOldRobotCoord(int oldCol, int oldRow) {
        showLog("Entering setOldRobotCoord");
        oldCoord[0] = oldCol;
        oldCoord[1] = oldRow;
        oldRow = this.convertRow(oldRow);
        for (int x = oldCol - 1; x <= oldCol + 1; x++)
            for (int y = oldRow - 1; y <= oldRow + 1; y++)
                cells[x][y].setType("unexplored");
        showLog("Exiting setOldRobotCoord");
    }

    private int[] getOldRobotCoord() {
        return oldCoord;
    }

    private void setArrowCoordinate(int col, int row, String arrowDirection) {
        showLog("Entering setArrowCoordinate");
        int[] obstacleCoord = new int[]{col, row};
        this.getObstacleCoord().add(obstacleCoord);
        String[] arrowCoord = new String[3];
        arrowCoord[0] = String.valueOf(col);
        arrowCoord[1] = String.valueOf(row);
        arrowCoord[2] = arrowDirection;
        this.getArrowCoord().add(arrowCoord);

        row = convertRow(row);
        cells[col][row].setType("arrow");
        showLog("Exiting setArrowCoordinate");
    }

    public void setRobotDirection(String direction) {
        sharedPreferences = getContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        robotDirection = direction;
        editor.putString("direction", direction);
        editor.commit();
        this.invalidate();
    }

    private void updateRobotAxis(int col, int row, String direction) {
//        TextView xAxisTextView =  ((Activity)this.getContext()).findViewById(R.id.xAxisTextView);
//        TextView yAxisTextView =  ((Activity)this.getContext()).findViewById(R.id.yAxisTextView);
//        TextView directionAxisTextView =  ((Activity)this.getContext()).findViewById(R.id.directionAxisTextView);
//
//        xAxisTextView.setText(String.valueOf(col-1));
//        yAxisTextView.setText(String.valueOf(row-1));
//        //directionAxisTextView.setText(direction);
//        if(direction.equals("up"))
//        {
//            //directionAxisTextView.setText(direction + " (N)");
//            directionAxisTextView.setText("N");
//        }
//        else if (direction.equals("down"))
//        {
//            //directionAxisTextView.setText(direction + " (S) ");
//            directionAxisTextView.setText("S");
//        }
//        else if (direction.equals("right"))
//        {
//            //.setText(direction + " (E) ");
//            directionAxisTextView.setText("E");
//        }
//        else if (direction.equals("left"))
//        {
//            //directionAxisTextView.setText(direction + " (W) ");
//            directionAxisTextView.setText("W");
//        }
//        else if (direction.equals("upleft"))
//        {
//            //directionAxisTextView.setText(direction + " (W) ");
//            directionAxisTextView.setText("NW");
//        }
//        else if (direction.equals("upright"))
//        {
//            //directionAxisTextView.setText(direction + " (W) ");
//            directionAxisTextView.setText("NE");
//        }
//        else if (direction.equals("downleft"))
//        {
//            //directionAxisTextView.setText(direction + " (W) ");
//            directionAxisTextView.setText("SW");
//        }
//        else if (direction.equals("downright"))
//        {
//            //directionAxisTextView.setText(direction + " (W) ");
//            directionAxisTextView.setText("SE");
//        }
//        else
//        {
//            directionAxisTextView.setText("None");
//        }
    }

    public void setWaypointCoord(int col, int row) throws JSONException {
        showLog("Entering setWaypointCoord");
        waypointCoord[0] = col;
        waypointCoord[1] = row;
        waypointNew=true;
        //Canvas canvas = null;

        row = this.convertRow(row);
        cells[col][row].setType("waypoint");
        /*RectF rect = new RectF(col * cellSize, row * cellSize, (col + 1) * cellSize, (row + 1) * cellSize);
        Bitmap dog = BitmapFactory.decodeResource(getResources(), R.drawable.dog);
        canvas.drawBitmap(dog, null, rect, null);*/
        this.invalidate();

        // TODO:uncommand for bluetooth to work
        //BluetoothFragment.printMessage("waypoint", waypointCoord[1]-1,waypointCoord[0]-1);
        showLog("Exiting setWaypointCoord");
    }

    private int[] getWaypointCoord() {
        return waypointCoord;
    }

    private void setObstacleCoord(int col, int row) {
        showLog("Entering setObstacleCoord");
        int[] obstacleCoord = new int[]{col, row};
        GridMap.obstacleCoord.add(obstacleCoord);
        row = this.convertRow(row);
        cells[col][row].setType("obstacle");
        // set obstacle No
        for(int i = 0; i<obstacleNoArray.length; i++){
            if(obstacleNoArray[i] != -1){
                if(cells[col][row].obstacleNo == -1){
                    cells[col][row].obstacleNo = obstacleNoArray[i]; // assign obstacle no
                    obstacleNoArray[i] = -1; // set index to marked as used
                    break;
                }
            }
        }
        showLog("Exiting setObstacleCoord");
        // TODO: uncommand for bluetooth
//        ArenaFragment.updateArenaBTMessage( "Obstacle No: " + cells[col][row].obstacleNo + "\t\tX: "+ (col-1) + "\t\tY: " + (19-row) +  "\t\tDirection: " + cells[col][row].getobstacleFacing());

        // Obstacle, <x,y>, <obstacleID>, <obstacleDirection>
        //BluetoothFragment.printMessage("Obstacle, "+ "<"+ (col-1) + ">, <" + (19-row) + ">, " + "<" + cells[col][row].obstacleNo+ ">, " + "<" + cells[col][row].getobstacleFacing() +">");
//        try {
//
//            BluetoothFragment.printMessage("Obstacle", (col-1), (19-row), getObstacleDirectionText(newDirection));
//            BluetoothFragment.printMessage("Obstacle"+ " Column: "+ (col-1) + " Row: "+ (19-row) + " Direction: " + getObstacleDirectionText(newDirection));
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
    }

    private ArrayList<int[]> getObstacleCoord() {
        return obstacleCoord;
    }

    private void showLog(String message) {
        Log.d(TAG, message);
    }

    private String getObstacleDirectionText(int inDirection)
    {
        String direction = "";
        switch (inDirection)
        {
            case 0:
                direction = "NONE";
                break;
            case 1:
                direction = "UP";
                break;
            case 2:
                direction = "DOWN";
                break;
            case 3:
                direction = "LEFT";
                break;
            case 4:
                direction = "RIGHT";
                break;
        }

        return direction;
    }

    /*private void drawArrow(Canvas canvas, ArrayList<String[]> arrowCoord) {
        showLog("Entering drawArrow");
        RectF rect;

        for (int i = 0; i < arrowCoord.size(); i++) {
            if (!arrowCoord.get(i)[2].equals("dummy")) {
                int col = Integer.parseInt(arrowCoord.get(i)[0]);
                int row = convertRow(Integer.parseInt(arrowCoord.get(i)[1]));
                rect = new RectF(col * cellSize, row * cellSize, (col + 1) * cellSize, (row + 1) * cellSize);
                switch (arrowCoord.get(i)[2]) {
                    case "up":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_up);
                        break;
                    case "right":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_right);
                        break;
                    case "down":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_down);
                        break;
                    case "left":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_left);
                        break;
                    default:
                        break;
                }
                canvas.drawBitmap(arrowBitmap, null, rect, null);
            }
            showLog("Exiting drawArrow");
        }
    }*/


    private class Cell {
        float startX, startY, endX, endY;
        Paint paint;
        String type;
        int id = -1;
        // Obstacle Face @ GridMap.Java -> class Cell
        String obstacleFacing = "null";

        String targetID = null;
        int obstacleNo = -1;

        boolean isDirection = false;

        private Cell(float startX, float startY, float endX, float endY, Paint paint, String type) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.paint = paint;
            this.type = type;
        }

        public void setType(String type) {
            this.type = type;
            switch (type) {
                case "obstacle":
                    this.paint = obstacleColor;
                    break;
                case "robot":
                    this.paint = robotColor;
                    break;
                case "end":
                    this.paint = endColor;
                    break;
                case "start":
                    this.paint = startColor;
                    break;
                case "waypoint":
                    this.paint = waypointColor;
                    break;
                case "unexplored":
                    this.paint = unexploredColor;
                    break;
                case "explored":
                    this.paint = exploredColor;
                    break;
                case "arrow":
                    this.paint = arrowColor;
                    break;
                /*case "fastestPath":
                    this.paint = fastestPathColor;
                    break;
                case "image":
                    this.paint = obstacleColor;
                    break;*/
                case "id":
                    this.paint = obstacleColor;
                    break;
                default:
                    showLog("setTtype default: " + type);
                    break;
            }
        }

        //TODO: set the IDs and obstacleFacing

        // Obstacle Face @ GridMap.Java -> class Cell
        public void setobstacleFacing(String obstacleFacing) {
            this.obstacleFacing = obstacleFacing;
        }
        public String getobstacleFacing() {
            return this.obstacleFacing;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getId() {
            return this.id;
        }
    }

    // For RPI
    private String rpiConvertDirection(String direction)
    {
        String direction_NSEW = "";
        switch (direction)
        {
            case "UP":
                direction_NSEW = "N";
                break;
            case "DOWN":
                direction_NSEW = "S";
                break;
            case "LEFT":
                direction_NSEW = "W";
                break;
            case "RIGHT":
                direction_NSEW = "E";
                break;
            default:
                direction_NSEW = "NONE";
        }
        return direction_NSEW;
    }

    //For RPI
    public void sendRPIMessage()
    {
        if(rpiRobot.equals("") == false) {
            String message = rpiRobot;

            for (int x = 1; x <= COL; x++) {
                for (int y = 0; y < ROW; y++) {
                    for (int i = 0; i < this.getArrowCoord().size(); i++) {

                        if (cells[x][y].type.equals("obstacle")) {

                            message = message + Integer.toString(x - 1) + "," + Integer.toString(19 - y) + "," + rpiConvertDirection(cells[x][y].getobstacleFacing()) + ",";
                        }

                    }
                }
            }

            // remove the last coma
            StringBuffer messageBuffer = new StringBuffer(message);
            messageBuffer.deleteCharAt(messageBuffer.length() - 1);

//            BluetoothFragment.printMessage(messageBuffer.toString());
        }
        else
        {
            Toast.makeText(this.getContext(), "Please set the Starting Position of the robot", Toast.LENGTH_LONG).show();
        }


    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        showLog("Entering onTouchEvent");
        int column = (int) (event.getX() / cellSize);
        int row = this.convertRow((int) (event.getY() / cellSize));
//        ToggleButton setStartPointToggleBtn = ((Activity)this.getContext()).findViewById(R.id.setStartPointToggleBtn);
//        ToggleButton setWaypointToggleBtn = ((Activity)this. getContext()).findViewById(R.id.setWaypointToggleBtn);
        // new FAB
//        FloatingActionButton setStartPointFAB = ((Activity)this.getContext()).findViewById(R.id.startingPointFAB);

        // && column<=20 && row<=20 && column>=1 && row>=1 is the validation when placing in the map
        if (event.getAction() == MotionEvent.ACTION_DOWN && this.getAutoUpdate() == false && column<=20 && row<=20 && column>=1 && row>=1) {

            if (startCoordStatus) {
                if (canDrawRobot) {
                    int[] startCoord = this.getStartCoord();
                    if (startCoord[0] >= 2 && startCoord[1] >= 2) {
                        startCoord[1] = this.convertRow(startCoord[1]);
                        for (int x = startCoord[0] - 1; x <= startCoord[0] + 1; x++)
                            for (int y = startCoord[1] - 1; y <= startCoord[1] + 1; y++)
                                cells[x][y].setType("unexplored");
                    }
                }
                else
                    canDrawRobot = true;
                this.setStartCoord(column, row);
                startCoordStatus = false;
                String direction = getRobotDirection();
                if(direction.equals("None")) {
                    direction = "up";
                }
                try {
                    int directionInt = 0;
                    if(direction.equals("up")){
                        directionInt = 0;
                    } else if(direction.equals("left")) {
                        directionInt = 3;
                    } else if(direction.equals("right")) {
                        directionInt = 1;
                    } else if(direction.equals("down")) {
                        directionInt = 2;
                    }
                    // TODO: uncoomand for Bluetooth
                    //BluetoothFragment.printMessage("Starting, " + "<" + (row - 1) + ">, <" + (column - 1) + ">, <" + direction.toUpperCase() + ">");
                    // For RPI
                    rpiRobot = "Al," + (row - 1) + "," + (column - 1) + "," + rpiConvertDirection(direction.toUpperCase()) + ",";
                    showLog("rpiRobot: " + rpiRobot);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                updateRobotAxis(column, row, direction);
//                if (setStartPointToggleBtn.isChecked())
//                    setStartPointToggleBtn.toggle();
//                // reset setStartPointFAB icon
//                if (ArenaFragment.isSetStartingPiont)
//                {
//                    ArenaFragment.isSetStartingPiont = false;
//                    setStartPointFAB.setImageDrawable(getContext().getDrawable(R.drawable.triangle));
//                }

                this.invalidate();
                return true;
            }
            if (setWaypointStatus) {
                int[] waypointCoord = this.getWaypointCoord();
                if (waypointCoord[0] >= 1 && waypointCoord[1] >= 1)
                    cells[waypointCoord[0]][this.convertRow(waypointCoord[1])].setType("unexplored");
                setWaypointStatus = false;
                try {
                    this.setWaypointCoord(column, row);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
//                if (setWaypointToggleBtn.isChecked())
//                    setWaypointToggleBtn.toggle();
                this.invalidate();
                return true;
            }
            if (setObstacleStatus) { // setting the position of the obstacle


                // TODO: New direction
                cells[column][20-row].setobstacleFacing(getObstacleDirectionText(newDirection));

                this.setObstacleCoord(column, row);

                this.invalidate();
                return true;

            }
            if (setExploredStatus) {
                cells[column][20-row].setType("explored");
                this.invalidate();
                return true;
            }
            if (unSetCellStatus) { // TODO: remove obstacle not yet use (ontouch on the map to remove)
                ArrayList<int[]> obstacleCoord = this.getObstacleCoord();
                cells[column][20-row].setType("unexplored");
                for (int i=0; i<obstacleCoord.size(); i++) {
//                    if (obstacleCoord.get(i)[0] == column && obstacleCoord.get(i)[1] == row)
//                        obstacleCoord.remove(i);
                    if (obstacleCoord.get(i)[0] == column && obstacleCoord.get(i)[1] == row){
                        // TODO: uncoomand for bluetooth
                        //BluetoothFragment.printMessage("RemovedObstacle, " + "<" + valueOf(obstacleCoord.get(i)[0] -1) + "," + valueOf(Math.abs(obstacleCoord.get(i)[1]) - 1) + ">");
                        //BluetoothFragment.printMessage("RemovedObstacle, " + "<" + valueOf(selectedObsCoord[0] - 1) + ">, <" + valueOf(Math.abs(selectedObsCoord[1]) - 1) + ">, <" + cells[selectedObsCoord[0]][20 - selectedObsCoord[1]].obstacleNo + ">");
                        obstacleNoArray[cells[column][20-row].obstacleNo - 1] = cells[column][20-row].obstacleNo; // unset obstacle no by assigning number back to array
                        cells[column][20-row].obstacleNo = -1;
                        obstacleCoord.remove(i);
                        if(oCellArr.get(oCellArr.size()-1) == cells[column][20-row]){
                            oCellArr.remove(oCellArr.size()-1);
                            //oCellArrDirection.remove(oCellArrDirection.size()-1);
                        }
                        //TODO: new oCellArr
//                        if(cells[(oCellArr.get(oCellArr.size()-1).getX())][oCellArr.get(oCellArr.size()-1).getY()] == cells[column][20-row]){
//                            oCellArr.remove(oCellArr.size()-1);
//                        }
                    }
                }
                this.invalidate();
                return true;
            }
            if(setObstacleDirection)
            {
                showLog("Enter set obstacle direction");


                AlertDialog.Builder mBuilder = new AlertDialog.Builder(getContext());
                mBuilder.setTitle("Select Obstacle Direction");
                mBuilder.setSingleChoiceItems(directionList, switchDirection, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        switchDirection = i;
                    }
                });
                mBuilder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Create the new cell
                        //oCellArr.set(selectedObsDirectionCor[2], cells[column][20 - row]);
                        //oCellArrDirection.set(selectedObsDirectionCor[2], switchDirection);

                        switch (switchDirection)
                        {
                            case 0:
                                cells[column][20 - row].setobstacleFacing("NONE");
                                break;
                            case 1:
                                cells[column][20 - row].setobstacleFacing("UP");
                                break;
                            case 2:
                                cells[column][20 - row].setobstacleFacing("DOWN");
                                break;
                            case 3:
                                cells[column][20 - row].setobstacleFacing("LEFT");
                                break;
                            case 4:
                                cells[column][20 - row].setobstacleFacing("RIGHT");
                                break;
                        }
//                        ArenaFragment.updateArenaBTMessage( "Obstacle No: " + cells[column][20 - row].obstacleNo + "\t\tX: "+ (column-1) + "\t\tY: " + (row-1) +  "\t\tDirection: " + cells[column][20 - row].getobstacleFacing());


                        invalidate();

                        // obstacle direction <x,y>, <obstacleID>, <obstacleDirection>
                        //BluetoothFragment.printMessage("ObstacleDirection, " + "<" + String.valueOf(column-1) + ">, <" + String.valueOf(row-1) + ">, <" + cells[column][20 - row].obstacleNo + ">, " + "<" + cells[column][20 - row].getobstacleFacing() + ">");
                        //TODO: Bluetooth
//                        try {
//                            BluetoothFragment.printMessage("Obstacle direction change",column,(20 - row), getObstacleDirectionText(switchDirection));
//                        }
//                        catch (JSONException e) {
//                            e.printStackTrace();
//                        }

                        dialogInterface.dismiss();
                    }
                });

                // check if the cell selected is obstacle or not
                if(cells[column][20 - row].type.equals("obstacle")) {

                    AlertDialog dialog = mBuilder.create();
                    dialog.show();
                }

                this.invalidate();
                showLog("Exit set obstacle direction");
                return true;
            }
            // selection of obstacle in the map
            if (obsSelected == false){
                ArrayList<int[]> obstacleCoord = this.getObstacleCoord();
                for (int i=0; i<obstacleCoord.size(); i++)
                    if (obstacleCoord.get(i)[0] == column && obstacleCoord.get(i)[1] == row){
                        selectedObsCoord[0] = column;
                        selectedObsCoord[1] = row;
                        for (int x = 0; x < oCellArr.size(); x++) {
                            if (oCellArr.get(x) == cells[column][20-row]) {
                                selectedObsCoord[2] = x;
                            }
                            //TODO: new oCellArr
//                            if (cells[oCellArr.get(x).getX()][oCellArr.get(x).getY()] == cells[column][20-row]) {
//                                selectedObsCoord[2] = x;
//                            }
                        }
                        obsSelected = true;
                        return true;
                    }
            }
        }
        // when touch event is release from the map
        else if (event.getAction() == MotionEvent.ACTION_UP && this.getAutoUpdate() == false) {
            if(obsSelected) {
                obsSelected = false;
                Log.d("obsSelected", Boolean.toString(obsSelected));
                return true;
            }
        }
        // moving obstacle around or out of the map
        else if (event.getAction() == MotionEvent.ACTION_MOVE && this.getAutoUpdate() == false) {
            if (obsSelected) {
                boolean occupied = false;
                ArrayList<int[]> obstacleCoord = this.getObstacleCoord();
                for (int i = 0; i < obstacleCoord.size(); i++) {
                    if (obstacleCoord.get(i)[0] == column && obstacleCoord.get(i)[1] == row) {
                        occupied = true;
                    }
                }
                if (occupied == false) {

                    // TODO: NEW obstacle
                    //BluetoothFragment.printMessage("RemovedObstacle, " + "<" + valueOf(selectedObsCoord[0] - 1) + ">, <" + valueOf(Math.abs(selectedObsCoord[1]) - 1) + ">, <" + cells[selectedObsCoord[0]][20 - selectedObsCoord[1]].obstacleNo + ">");
                    obstacleNoArray[cells[selectedObsCoord[0]][20 - selectedObsCoord[1]].obstacleNo - 1] = cells[selectedObsCoord[0]][20 - selectedObsCoord[1]].obstacleNo; // unset obstacle no by assigning number back to array
                    cells[selectedObsCoord[0]][20 - selectedObsCoord[1]].obstacleNo = -1;
                    cells[selectedObsCoord[0]][20 - selectedObsCoord[1]].setType("unexplored");
                    // Remove obstacle facing direction
                    obsSelectedFacing = cells[selectedObsCoord[0]][20 - selectedObsCoord[1]].getobstacleFacing(); // <-- newly added
                    cells[selectedObsCoord[0]][20 - selectedObsCoord[1]].setobstacleFacing(null); // <-- newly added
                    // Remove target ID
                    obsTargetImageID = cells[selectedObsCoord[0]][20 - selectedObsCoord[1]].targetID; // <-- newly added
                    cells[selectedObsCoord[0]][20 - selectedObsCoord[1]].targetID = null; // <-- newly added

                    //Remove old Cell
                    // TODO: uncommand for bluetooth to work
//                    BluetoothFragment.printMessage("Removed obstacle " + "(" + valueOf(selectedObsCoord[0] - 1) + "," + valueOf(Math.abs(selectedObsCoord[1]) - 1) + ")");
//                    cells[selectedObsCoord[0]][20 - selectedObsCoord[1]].setType("unexplored");
                    //Remove from obstacles coord arraylist
                    for (int i = 0; i < obstacleCoord.size(); i++) {
                        if (obstacleCoord.get(i)[0] == selectedObsCoord[0] && obstacleCoord.get(i)[1] == selectedObsCoord[1]) {
                            obstacleCoord.remove(i);
                        }
                    }
                    //If selection is within the grid
                    if (column <= 20 && row <= 20 && column >= 1 && row >= 1) {
                        //Create the new cell
                        oCellArr.set(selectedObsCoord[2], cells[column][20 - row]);
//                        this.setObstacleCoord(column, row);
                        //oCellArrDirection.set(selectedObsCoord[2],-1);
                        //TODO: new oCellArr
//                        ObstacleCellArray obstacleCellArray = new ObstacleCellArray(column,(20 - row));
//                        oCellArr.set(selectedObsCoord[2],obstacleCellArray);
//                        oCellArr.get(selectedObsCoord[2]).setX(column);
//                        oCellArr.get(selectedObsCoord[2]).setY((20 - row));

                        selectedObsCoord[0] = column;
                        selectedObsCoord[1] = row;
                        // TODO: new Obstacle
                        // Add obstacle facing direction
                        cells[column][20-row].setobstacleFacing(obsSelectedFacing); // <-- newly added
                        // Add target ID
                        cells[column][20-row].targetID = obsTargetImageID;  // <-- newly added

                        this.setObstacleCoord(column, row);
                    }
                    //If selection is outside the grid
                    else if (column < 1 || row < 1 || column > 20 || row > 20) {
                        obsSelected = false;
                        //Remove from oCellArr
                        if (oCellArr.get(oCellArr.size() - 1) == cells[selectedObsCoord[0]][20 - selectedObsCoord[1]]) {
                            oCellArr.remove(oCellArr.size() - 1);
                            //oCellArrDirection.remove(oCellArrDirection.size() - 1);

                            // TODO: new Obstacle
                            // Remove obstacle facing direction
                            cells[selectedObsCoord[0]][20-selectedObsCoord[1]].setobstacleFacing(null); //<-- newly added
                            // Remove target ID
                            cells[selectedObsCoord[0]][20 - selectedObsCoord[1]].targetID = null; // <-- newly added
                        }
                        //TODO: new oCellArr
//                        if (cells[oCellArr.get(oCellArr.size() - 1).getX()][oCellArr.get(oCellArr.size() - 1).getY()] == cells[selectedObsCoord[0]][20 - selectedObsCoord[1]]) {
//                            oCellArr.remove(oCellArr.size() - 1);
//                        }
                    }
                    this.invalidate();
                    return true;
                }

            }
        }
        showLog("Exiting onTouchEvent");
        return false;
    }

    /*public void toggleCheckedBtn(String buttonName) {
        ToggleButton setStartPointToggleBtn = ((Activity)this.getContext()).findViewById(R.id.setStartPointToggleBtn);
        ToggleButton setWaypointToggleBtn = ((Activity)this.getContext()).findViewById(R.id.setWaypointToggleBtn);
        ImageButton obstacleImageBtn = ((Activity)this.getContext()).findViewById(R.id.obstacleImageBtn);
        ImageButton exploredImageBtn = ((Activity)this.getContext()).findViewById(R.id.exploredImageBtn);
        ImageButton clearImageBtn = ((Activity)this.getContext()).findViewById(R.id.clearImageBtn);

        if (!buttonName.equals("setStartPointToggleBtn"))
            if (setStartPointToggleBtn.isChecked()) {
                this.setStartCoordStatus(false);
                setStartPointToggleBtn.toggle();
            }
        if (!buttonName.equals("setWaypointToggleBtn"))
            if (setWaypointToggleBtn.isChecked()) {
                this.setWaypointStatus(false);
                setWaypointToggleBtn.toggle();
            }
        if (!buttonName.equals("exploredImageBtn"))
            if (exploredImageBtn.isEnabled())
                this.setExploredStatus(false);
        if (!buttonName.equals("obstacleImageBtn"))
            if (obstacleImageBtn.isEnabled())
                this.setSetObstacleStatus(false);
        if (!buttonName.equals("clearImageBtn"))
            if (clearImageBtn.isEnabled())
                this.setUnSetCellStatus(false);
    }*/

    public void toggleCheckedBtn(String buttonName) {
//        ToggleButton setStartPointToggleBtn = ((Activity)this.getContext()).findViewById(R.id.setStartPointToggleBtn);
//        ToggleButton setWaypointToggleBtn = ((Activity)this.getContext()).findViewById(R.id.setWaypointToggleBtn);
//        //ImageButton obstacleImageBtn = ((Activity)this.getContext()).findViewById(R.id.obstacleImageBtn);
//        ToggleButton setObstacleBtn = ((Activity)this.getContext()).findViewById(R.id.setObstacleToggleBtn);
        //ImageButton exploredImageBtn = ((Activity)this.getContext()).findViewById(R.id.exploredImageBtn);
        //ImageButton clearImageBtn = ((Activity)this.getContext()).findViewById(R.id.clearImageBtn);

//        if (!buttonName.equals("setStartPointToggleBtn"))
//            if (setStartPointToggleBtn.isChecked()) {
//                this.setStartCoordStatus(false);
//                setStartPointToggleBtn.toggle();
//            }
//        if (!buttonName.equals("setWaypointToggleBtn"))
//            if (setWaypointToggleBtn.isChecked()) {
//                this.setWaypointStatus(false);
//                setWaypointToggleBtn.toggle();
//            }
//        if (!buttonName.equals("setObstacleToggleBtn"))
//            if (setObstacleBtn.isChecked()) {
//                this.setSetObstacleStatus(false);
//                setObstacleBtn.toggle();
//            }

//        if (!buttonName.equals("exploredImageBtn"))
//            if (exploredImageBtn.isEnabled())
//                this.setExploredStatus(false);
//        if (!buttonName.equals("obstacleImageBtn"))
//            if (obstacleImageBtn.isEnabled())
//                this.setSetObstacleStatus(false);
//        if (!buttonName.equals("clearImageBtn"))
//            if (clearImageBtn.isEnabled())
//                this.setUnSetCellStatus(false);
    }

    public void setStartingPointManual() throws JSONException {
        //TextView robotStatusTextView = ((Activity)this.getContext()).findViewById(R.id.robotStatusTextView);
        //robotStatusTextView.setText("yesss");
        if (canDrawRobot)
            this.setOldRobotCoord(curCoord[0], curCoord[1]);
        Integer[] infoJsonArray = new Integer[]{0,17,90};
//                    infoJsonObject = infoJsonArray.getJSONObject(0);

        for (int row = ROW - 1; row >= 0; row--)
            for (int col = 1; col <= COL; col++)
                cells[col][row].setType("unexplored");

        String direction;
        if (infoJsonArray[2] == 90) {
            direction = "right";
        } else if (infoJsonArray[2] == 180) {
            direction = "down";
        } else if (infoJsonArray[2] == 270) {
            direction = "left";
        } else {
            direction = "up";
        }
        this.setStartCoord(infoJsonArray[0], infoJsonArray[1]);
        this.setCurCoord(infoJsonArray[0]+2, convertRow(infoJsonArray[1])-1, direction);
        canDrawRobot = true;
        setEndCoord(COL-1, ROW-1);
        newEndCoord=true;
    }

    public void resetMap() {
        showLog("Entering resetMap");
        TextView robotStatusTextView =  ((Activity)this.getContext()).findViewById(R.id.robotStatusTextView);
//        Switch manualAutoToggleBtn = ((Activity)this.getContext()).findViewById(R.id.manualAutoToggleBtn);
//        Switch phoneTiltSwitch = ((Activity)this.getContext()).findViewById(R.id.phoneTiltSwitch);
        updateRobotAxis(1, 1, "None");
        robotStatusTextView.setText("Not Available");
        SharedPreferences.Editor editor = sharedPreferences.edit();


//        if (manualAutoToggleBtn.isChecked()) {
//            manualAutoToggleBtn.toggle();
//            manualAutoToggleBtn.setText("MANUAL");
//        }
//        this.toggleCheckedBtn("None");
//
//        if (phoneTiltSwitch.isChecked()) {
//            phoneTiltSwitch.toggle();
//            phoneTiltSwitch.setText("TILT OFF");
//        }

        receivedJsonObject = null;
        backupMapInformation = null;
        startCoord = new int[]{-1, -1};
        curCoord = new int[]{-1, -1};
        oldCoord = new int[]{-1, -1};
        robotDirection = "None";
        autoUpdate = false;
        arrowCoord = new ArrayList<>();
        obstacleCoord = new ArrayList<>();
        waypointCoord = new int[]{-1, -1};
        mapDrawn = false;
        canDrawRobot = false;
        validPosition = false;
        oCellArr = new ArrayList<>();
        rpiObstacle = "";
        rpiRobot = "";
        //oCellArrDirection = new ArrayList<>();

        // newly added
        obstacleNoArray = new int[]{1,2,3,4,5,6,7,8,9,10,11,12,13,14,15}; //reset obstacle no array
        Bitmap arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_error);

        showLog("Exiting resetMap");
        this.invalidate();
    }

    public void updateMapInformation() throws JSONException {
        showLog("Entering updateMapInformation");
        JSONObject mapInformation = this.getReceivedJsonObject();
        showLog("updateMapInformation --- mapInformation: " + mapInformation);
        JSONArray infoJsonArray;
        JSONObject infoJsonObject;
        String hexStringExplored, hexStringObstacle, exploredString, obstacleString;
        BigInteger hexBigIntegerExplored, hexBigIntegerObstacle;
        String message;

        if (mapInformation == null)
            return;

        for(int i=0; i<mapInformation.names().length(); i++) {
            message = "updateMapInformation Default message";
            switch (mapInformation.names().getString(i)) {
                case "map":
                    infoJsonArray = mapInformation.getJSONArray("map");
                    infoJsonObject = infoJsonArray.getJSONObject(0);

                    hexStringExplored = infoJsonObject.getString("explored");
                    hexBigIntegerExplored = new BigInteger(hexStringExplored, 16);
                    exploredString = hexBigIntegerExplored.toString(2);
                    showLog("updateMapInformation.exploredString: " + exploredString);

                    int x, y;
                    for (int j = 0; j < exploredString.length() - 4; j++) {
                        y = 19 - (j / 15);
                        x = 1 + j - ((19 - y) * 15);
                        if ((String.valueOf(exploredString.charAt(j + 2))).equals("1") && !cells[x][y].type.equals("robot"))
                            cells[x][y].setType("explored");
                        else if ((String.valueOf(exploredString.charAt(j + 2))).equals("0") && !cells[x][y].type.equals("robot"))
                            cells[x][y].setType("unexplored");
                    }

                    int length = infoJsonObject.getInt("length");

                    hexStringObstacle = infoJsonObject.getString("obstacle");
                    showLog("updateMapInformation hexStringObstacle: " + hexStringObstacle);
                    hexBigIntegerObstacle = new BigInteger(hexStringObstacle, 16);
                    showLog("updateMapInformation hexBigIntegerObstacle: " + hexBigIntegerObstacle);
                    obstacleString = hexBigIntegerObstacle.toString(2);
                    while (obstacleString.length() < length) {
                        obstacleString = "0" + obstacleString;
                    }
                    showLog("updateMapInformation obstacleString: " + obstacleString);
                    setPublicMDFExploration(hexStringExplored);
                    setPublicMDFObstacle(hexStringObstacle);

                    int k = 0;
                    for (int row = ROW - 1; row >= 0; row--)
                        for (int col = 1; col <= COL; col++)
                            if ((cells[col][row].type.equals("explored") || (cells[col][row].type.equals("robot"))) && k < obstacleString.length()) {
                                if ((String.valueOf(obstacleString.charAt(k))).equals("1"))
                                    this.setObstacleCoord(col, 20 - row);
                                k++;
                            }

                    int[] waypointCoord = this.getWaypointCoord();
                    if (waypointCoord[0] >= 1 && waypointCoord[1] >= 1)
                        cells[waypointCoord[0]][20 - waypointCoord[1]].setType("waypoint");
                    break;
                case "robotPosition":
                    infoJsonArray = mapInformation.getJSONArray("robotPosition");
                    if (infoJsonArray.getInt(0)<=13 && infoJsonArray.getInt(0)>=0 && infoJsonArray.getInt(1)<=18 && infoJsonArray.getInt(1)>=0){
                        //TextView robotStatusTextView = ((Activity)this.getContext()).findViewById(R.id.robotStatusTextView);
                        //robotStatusTextView.setText("yesss");
                        if (canDrawRobot)
                            this.setOldRobotCoord(curCoord[0], curCoord[1]);

//                    infoJsonObject = infoJsonArray.getJSONObject(0);

                    /*for (int row = ROW - 1; row >= 0; row--)
                        for (int col = 1; col <= COL; col++)
                            cells[col][row].setType("unexplored");*/

                        String direction;
                        if (infoJsonArray.getInt(2) == 90) {
                            direction = "right";
                        } else if (infoJsonArray.getInt(2) == 180) {
                            direction = "down";
                        } else if (infoJsonArray.getInt(2) == 270) {
                            direction = "left";
                        } else {
                            direction = "up";
                        }
                        this.setStartCoord(infoJsonArray.getInt(0), infoJsonArray.getInt(1));
                        this.setCurCoord(infoJsonArray.getInt(0)+1, convertRow(19-infoJsonArray.getInt(1)), direction);
                        //this.setCurCoord(infoJsonArray.getInt(0)+2, convertRow(infoJsonArray.getInt(1)), direction);
                        canDrawRobot = true;
                    }
                    break;
                case "waypoint":
                    infoJsonArray = mapInformation.getJSONArray("waypoint");
                    infoJsonObject = infoJsonArray.getJSONObject(0);
                    this.setWaypointCoord(infoJsonObject.getInt("x"), infoJsonObject.getInt("y"));
                    setWaypointStatus = true;
                    break;
                case "obstacle":
                    infoJsonArray = mapInformation.getJSONArray("obstacle");
                    for (int j = 0; j < infoJsonArray.length(); j++) {
                        infoJsonObject = infoJsonArray.getJSONObject(j);
                        this.setObstacleCoord(infoJsonObject.getInt("x")+1, infoJsonObject.getInt("y")+1);
                    }
                    message = "No. of Obstacle: " + String.valueOf(infoJsonArray.length());
                    break;
                case "arrow":
                    infoJsonArray = mapInformation.getJSONArray("arrow");
                    for (int j = 0; j < infoJsonArray.length(); j++) {
                        infoJsonObject = infoJsonArray.getJSONObject(j);
                        if (!infoJsonObject.getString("face").equals("dummy")) {
                            this.setArrowCoordinate(infoJsonObject.getInt("x"), infoJsonObject.getInt("y"), infoJsonObject.getString("face"));
                            message = "Arrow:  (" + String.valueOf(infoJsonObject.getInt("x")) + "," + String.valueOf(infoJsonObject.getInt("y")) + "), face: " + infoJsonObject.getString("face");
                        }
                    }
                    break;
                case "move":
                    infoJsonArray = mapInformation.getJSONArray("move");
                    infoJsonObject = infoJsonArray.getJSONObject(0);
                    if (canDrawRobot)
                        moveRobot(infoJsonObject.getString("direction"));
                    message = "moveDirection: " + infoJsonObject.getString("direction");
                    //message = "length of the map info: " + mapInformation.names().length();
                    break;
                case "status":
//                    infoJsonArray = mapInformation.getJSONArray("status");
//                    infoJsonObject = infoJsonArray.getJSONObject(0);
//                    printRobotStatus(infoJsonObject.getString("status"));
//                    message = "status: " + infoJsonObject.getString("status");
                    String msg = mapInformation.getString("status");
                    printRobotStatus(msg);
                    message = "status: " + msg;
                    break;
                case "id":
                    break;
                default:
                    message = "Unintended default for JSONObject";
                    break;
            }
//            if (!message.equals("updateMapInformation Default message"))
//                BluetoothFragment.receiveMessage(message);
        }
        showLog("Exiting updateMapInformation");
        this.invalidate();
    }

    // TODO: need find a way to make the turn like a car using angle (clock)
    public void moveRobot(String direction) {
        showLog("Entering moveRobot");
        setValidPosition(false);
        int[] curCoord = this.getCurCoord();
        ArrayList<int[]> obstacleCoord = this.getObstacleCoord();
        this.setOldRobotCoord(curCoord[0], curCoord[1]);
        int[] oldCoord = this.getOldRobotCoord();
        String robotDirection = getRobotDirection();
        String backupDirection = robotDirection;

        switch (robotDirection) {
            case "up":
                switch (direction) {
                    case "forward":
                        if (curCoord[1] != 19) {
                            curCoord[1] += 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "right";
                        break;
                    case "back":
                        if (curCoord[1] != 2) {
                            curCoord[1] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "left";
                        break;
                    default:
                        robotDirection = "error up";
                        break;
                }
                break;
            case "right":
                switch (direction) {
                    case "forward":
                        if (curCoord[0] != COL-1) {
                            curCoord[0] += 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "down";
                        break;
                    case "back":
                        if (curCoord[0] != 2) {
                            curCoord[0] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "up";
                        break;
                    default:
                        robotDirection = "error right";
                }
                break;
            case "down":
                switch (direction) {
                    case "forward":
                        if (curCoord[1] != 2) {
                            curCoord[1] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "left";
                        break;
                    case "back":
                        if (curCoord[1] != COL-1) {
                            curCoord[1] += 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "right";
                        break;
                    default:
                        robotDirection = "error down";
                }
                break;
            case "left":
                switch (direction) {
                    case "forward":
                        if (curCoord[0] != 2) {
                            curCoord[0] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "up";
                        break;
                    case "back":
                        if (curCoord[0] != COL-1) {
                            curCoord[0] += 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "down";
                        break;
                    default:
                        robotDirection = "error left";
                }
                break;
            default:
                robotDirection = "error moveCurCoord";
                break;
        }
        if (getValidPosition())
            for (int x = curCoord[0] - 1; x <= curCoord[0] + 1; x++) {
                for (int y = curCoord[1] - 1; y <= curCoord[1] + 1; y++) {
                    for (int i = 0; i < obstacleCoord.size(); i++) {
                        if (obstacleCoord.get(i)[0] != x || obstacleCoord.get(i)[1] != y)
                            setValidPosition(true);
                        else {
                            setValidPosition(false);
                            break;
                        }
                    }
                    if (!getValidPosition())
                        break;
                }
                if (!getValidPosition())
                    break;
            }
        if (getValidPosition())
            this.setCurCoord(curCoord[0], curCoord[1], robotDirection);
        else {
            if (direction.equals("forward") || direction.equals("back"))
                robotDirection = backupDirection;
            this.setCurCoord(oldCoord[0], oldCoord[1], robotDirection);
        }
        this.invalidate();
        showLog("Exiting moveRobot");
    }

    public JSONObject getCreateJsonObject() {
        showLog("Entering getCreateJsonObject");
        String exploredString = "11";
        String obstacleString = "";
        String hexStringObstacle = "";
        String hexStringExplored = "";
        BigInteger hexBigIntegerObstacle, hexBigIntegerExplored;
        int[] waypointCoord = this.getWaypointCoord();
        int[] curCoord = this.getCurCoord();
        String robotDirection = this.getRobotDirection();
        List<int[]> obstacleCoord = new ArrayList<>(this.getObstacleCoord());
        List<String[]> arrowCoord = new ArrayList<>(this.getArrowCoord());

        TextView robotStatusTextView =  ((Activity)this.getContext()).findViewById(R.id.robotStatusTextView);

        JSONObject map = new JSONObject();
        for (int y=ROW-1; y>=0; y--)
            for (int x=1; x<=COL; x++)
                if (cells[x][y].type.equals("explored") || cells[x][y].type.equals("robot") || cells[x][y].type.equals("obstacle") || cells[x][y].type.equals("arrow"))
                    exploredString = exploredString + "1";
                else
                    exploredString = exploredString + "0";
        exploredString = exploredString + "11";
        showLog("exploredString: " + exploredString);

        hexBigIntegerExplored = new BigInteger(exploredString, 2);
        showLog("hexBigIntegerExplored: " + hexBigIntegerExplored);
        hexStringExplored = hexBigIntegerExplored.toString(16);
        showLog("hexStringExplored: " + hexStringExplored);

        for (int y=ROW-1; y>=0; y--)
            for (int x=1; x<=COL; x++)
                if (cells[x][y].type.equals("explored") || cells[x][y].type.equals("robot"))
                    obstacleString = obstacleString + "0";
                else if (cells[x][y].type.equals("obstacle") || cells[x][y].type.equals("arrow"))
                    obstacleString = obstacleString + "1";
        showLog("Before loop: obstacleString: " + obstacleString + ", length: " + obstacleString.length());


        while ((obstacleString.length() % 8) != 0) {
            obstacleString = obstacleString + "0";
        }

        showLog("After loop: obstacleString: " + obstacleString + ", length: " + obstacleString.length());


//        publicMDFObstacle = obstacleString;
//        publicMDFExploration = exploredString;

        if (!obstacleString.equals("")) {
            hexBigIntegerObstacle = new BigInteger(obstacleString, 2);
            showLog("hexBigIntegerObstacle: " + hexBigIntegerObstacle);
            hexStringObstacle = hexBigIntegerObstacle.toString(16);
            if (hexStringObstacle.length() % 2 != 0)
                hexStringObstacle = "0" + hexStringObstacle;
            showLog("hexStringObstacle: " + hexStringObstacle);
        }
        try {
            map.put("explored", hexStringExplored);
            map.put("length", obstacleString.length());
            if (!obstacleString.equals(""))
                map.put("obstacle", hexStringObstacle);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONArray jsonMap = new JSONArray();
        jsonMap.put(map);

        JSONArray jsonRobot = new JSONArray();
        if (curCoord[0] >= 2 && curCoord[1] >= 2)
            try {
                JSONObject robot = new JSONObject();
                robot.put("x", curCoord[0]);
                robot.put("y", curCoord[1]);
                robot.put("direction", robotDirection);
                jsonRobot.put(robot);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        JSONArray jsonWaypoint = new JSONArray();
        if (waypointCoord[0] >= 1 && waypointCoord[1] >= 1)
            try {
                JSONObject waypoint = new JSONObject();
                waypoint.put("x", waypointCoord[0]);
                waypoint.put("y", waypointCoord[1]);
                setWaypointStatus = true;
                jsonWaypoint.put(waypoint);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        JSONArray jsonObstacle = new JSONArray();
        for (int i=0; i<obstacleCoord.size(); i++)
            try {
                JSONObject obstacle = new JSONObject();
                obstacle.put("x", obstacleCoord.get(i)[0]);
                obstacle.put("y", obstacleCoord.get(i)[1]);
                jsonObstacle.put(obstacle);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        JSONArray jsonArrow = new JSONArray();
        for (int i=0; i<arrowCoord.size(); i++) {
            try {
                JSONObject arrow = new JSONObject();
                arrow.put("x", Integer.parseInt(arrowCoord.get(i)[0]));
                arrow.put("y", Integer.parseInt(arrowCoord.get(i)[1]));
                arrow.put("face", arrowCoord.get(i)[2]);
                jsonArrow.put(arrow);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        JSONArray jsonStatus = new JSONArray();
        try {
            JSONObject status = new JSONObject();
            status.put("status", robotStatusTextView.getText().toString());
            jsonStatus.put(status);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mapInformation = new JSONObject();
        try {
            mapInformation.put("map", jsonMap);
            mapInformation.put("robot", jsonRobot);
            if (setWaypointStatus) {
                mapInformation.put("waypoint", jsonWaypoint);
                setWaypointStatus = false;
            }
            mapInformation.put("obstacle", jsonObstacle);
            mapInformation.put("arrow", jsonArrow);
            mapInformation.put("status", jsonStatus);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        showLog("Exiting getCreateJsonObject");
        return mapInformation;
    }

    public void printRobotStatus(String message) {
        TextView robotStatusTextView = ((Activity)this.getContext()).findViewById(R.id.robotStatusTextView);
        robotStatusTextView.setText(message);
    }

    public static void setPublicMDFExploration(String msg) {
        publicMDFExploration = msg;
    }

    public static void setPublicMDFObstacle(String msg) {
        publicMDFObstacle = msg;
    }

    public static String getPublicMDFExploration() {
        return publicMDFExploration;
    }

    public static String getPublicMDFObstacle() {
        return publicMDFObstacle;
    }


}
