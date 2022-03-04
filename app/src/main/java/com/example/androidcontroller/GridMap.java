package com.example.androidcontroller;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebHistoryItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import static java.lang.String.valueOf;

/***
 * IMPORTANT NOTES:
 * cells[][] contain Cell Objects, to access the object at coordinates x & y, you will need to access it via cells[x][20-y]
 */

public class GridMap extends View {

    public GridMap(Context c) {
        super(c);
        initMap();
    }

    private Paint blackPaint = new Paint();
    private Paint whitePaint = new Paint();
    private Paint gridNoText =  new Paint();
    private Paint obstacleColor = new Paint();
    private Paint robotColor = new Paint();
    private Paint endColor = new Paint();
    private Paint startColor = new Paint();
    private Paint unexploredColor = new Paint();
    private Paint imageLine = new Paint();
    private Paint imageLineConfirm = new Paint();

    private static Direction robotDirection = Direction.NONE;
    private static int[] curCoord = new int[]{-1, -1};
    private static ArrayList<int[]> obstacleCoords = new ArrayList<>();
    private static boolean autoUpdate = false;
    private static boolean canDrawRobot = false;
    private static boolean startCoordStatus = false;
    private static boolean setObstacleStatus = false;
    private static boolean setObstacleDirection = false;

    private static final String TAG = "GridMap";
    private static final int COL = 20;
    private static final int ROW = 20;
    private static float cellSize;
    private static Cell[][] cells;

    private static boolean isOutdoorArena = false;

    private boolean mapDrawn = false;

    private static int[] selectedObsCoord = new int[3];
    private static boolean obsSelected = false;
    private static ArrayList<Cell> oCellArr = new ArrayList<Cell>();

    int switchDirection = -1; // 0:None 1: Up, 2: Down, 3: Left, 4:Right
    String[] directionList = new String[]{"NONE", "UP", "DOWN", "LEFT", "RIGHT"};
    private static int[] obstacleNoArray = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};

    public GridMap(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initMap();
        blackPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        whitePaint.setColor(Color.WHITE);
        obstacleColor.setColor(Color.BLACK);
        robotColor.setColor(Color.CYAN);
        endColor.setColor(Color.GRAY);
        startColor.setColor(Color.CYAN);
        unexploredColor.setColor(Color.LTGRAY);
        gridNoText.setColor(Color.WHITE);
        gridNoText.setTextSize(15);
        gridNoText.setFakeBoldText(true);

        imageLine.setStyle(Paint.Style.STROKE);
        imageLine.setColor(Color.YELLOW);
        imageLine.setStrokeWidth(2);

        imageLineConfirm.setStyle(Paint.Style.STROKE);
        imageLineConfirm.setColor(Color.YELLOW);
        imageLineConfirm.setStrokeWidth(5);
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
        Log.d(TAG, "Creating Cell");

        if (!mapDrawn) {
            createCell();
            resetMap();
            mapDrawn = true;
        }

        drawIndividualCell(canvas);
        drawGridLines(canvas);
        drawGridNumber(canvas);

        if (canDrawRobot)
            drawRobot(canvas);

        showLog("Exiting onDraw");
    }

    private void drawIndividualCell(Canvas canvas) {
        showLog("Entering drawIndividualCell");

        for (int x = 0; x < COL + 2; x++) {
            for (int y = 0; y < ROW + 2; y++) {
                Cell cell = cells[x][y];

                //DRAWING: CELL ITSELF
                canvas.drawRect(cell.startX, cell.startY, cell.endX, cell.endY, cell.paint);

                //DRAWING: OBSTACLE NUMBER & DIRECTION
                if (cell.type == CellType.OBSTACLE) {
                    //Draw the number for the obstacle
                    if (cell.targetID == null) {
                        canvas.drawText(Integer.toString(cell.obstacleNo), cell.startX + (cellSize / 3.2f), cell.startY + (cellSize / 1.5f), whitePaint);
                    } else {
                        Paint targetPaint = new Paint();
                        targetPaint.setTextSize(20);
                        targetPaint.setColor(Color.GREEN);
                        targetPaint.setTextAlign(Paint.Align.CENTER);
                        canvas.drawText(cell.targetID, (cell.startX + cell.endX) / 2, cell.endY + (cell.startY - cell.endY) / 4, targetPaint);
                    }

                    //Draw the obstacle facing
                    if (cell.obstacleFacing != null || cell.obstacleFacing == Direction.NONE) {
                        switch (cell.obstacleFacing) {
                            case UP:
                                canvas.drawRect(cell.startX + 2, cell.startY + 1, cell.endX, cell.endY - (cellSize / 1.1f), imageLine);
                                break;
                            case DOWN:
                                canvas.drawRect(cell.startX + 2, cell.startY + (cellSize / 1f) - 2, cell.endX, cell.endY - 1, imageLine);
                                break;
                            case LEFT:
                                canvas.drawRect(cell.startX + 1, cell.startY + 2, cell.endX - (cellSize / 1.1f), cell.endY, imageLine);
                                break;
                            case RIGHT:
                                canvas.drawRect(cell.startX + (cellSize / 1f) - 2, cell.startY, cell.endX - 1, cell.endY, imageLine);
                                break;
                        }
                    }
                }
            }
        }
        showLog("Exiting drawIndividualCell");
    }

    private Cell getCellAtMapCoords(int x, int y) {
        return cells[x + 1][COL - y];
    }

    public void updateImageNumberCell(int obstacleNo, String targetID) {
        // find the obstacle no which has the same id
        for (int x = 1; x <= COL; x++)
            for (int y = 1; y <= ROW; y++)
                if (cells[x][y].obstacleNo == obstacleNo) {
                    cells[x][y].targetID = targetID;
                }
        this.invalidate();
    }

    private void drawGridLines(Canvas canvas) {
        //HORIZONTAL LINES
        for (int y = 0; y <= COL; y++) {
            Cell start = cells[1][y];
            Cell end = cells[COL][y];
            canvas.drawLine(start.startX, start.endY, end.endX, end.endY, blackPaint);
        }

        //VERTICAL LINES
        for (int x = 1; x <= COL + 1; x++) {
            Cell start = cells[x][1];
            Cell end = cells[x][ROW];
            canvas.drawLine(start.startX, start.startY, end.startX, end.endY, blackPaint);
        }
    }

    private void drawGridNumber(Canvas canvas) {
        showLog("Entering drawGridNumber");
        //X-AXIS numbers
        for (int x = 1; x <= COL; x++) {
            Cell cell = cells[x][COL + 1];
            String num = "" + (x - 1);
            if (x > 9)
                canvas.drawText(num, cell.startX + (cellSize / 5), cell.startY + (cellSize / 2), gridNoText);
            else
                canvas.drawText(num, cell.startX + (cellSize / 3), cell.startY + (cellSize / 2), gridNoText);
        }
        //Y-AXIS numbers
        for (int y = 1; y <= ROW; y++) {
            Cell cell = cells[0][y];
            int adjustedY = ROW - y;
            String num = "" + adjustedY;
            if (adjustedY > 9)
                canvas.drawText(num, cell.startX + (cellSize / 3), cell.startY + (cellSize / 1.5f), gridNoText);
            else
                canvas.drawText(num, cell.startX + (cellSize / 2), cell.startY + (cellSize / 1.5f), gridNoText);
        }
        showLog("Exiting drawGridNumber");
    }

    private void drawRobot(Canvas canvas) {
        showLog("Entering drawRobot");
        if(curCoord[0] == -1 || curCoord[1] == -1){
            //No robot to draw
            return;
        }

        int[] cellIndexes = convertMapCoordToCellsIndexes(curCoord[0],curCoord[1]);
        int indexX = cellIndexes[0];
        int indexY = cellIndexes[1];

        switch (robotDirection) {
            case UP:
                //left drawn line
                canvas.drawLine(cells[indexX - 1][indexY + 1].startX, cells[indexX - 1][indexY + 1].endY, (cells[indexX][indexY - 1].startX + cells[indexX][indexY - 1].endX) / 2, cells[indexX][indexY - 1].startY, blackPaint);
                //right drawn line
                canvas.drawLine((cells[indexX][indexY - 1].startX + cells[indexX][indexY - 1].endX) / 2, cells[indexX][indexY - 1].startY, cells[indexX + 1][indexY + 1].endX, cells[indexX + 1][indexY + 1].endY, blackPaint);
                break;
            case DOWN:
                canvas.drawLine(cells[indexX - 1][indexY - 1].startX, cells[indexX - 1][indexY - 1].startY, (cells[indexX][indexY + 1].startX + cells[indexX][indexY + 1].endX) / 2, cells[indexX][indexY + 1].endY, blackPaint);
                canvas.drawLine((cells[indexX][indexY + 1].startX + cells[indexX][indexY + 1].endX) / 2, cells[indexX][indexY + 1].endY, cells[indexX + 1][indexY - 1].endX, cells[indexX + 1][indexY - 1].startY, blackPaint);
                break;
            case RIGHT:
                canvas.drawLine(cells[indexX - 1][indexY - 1].startX, cells[indexX - 1][indexY - 1].startY, cells[indexX + 1][indexY].endX, cells[indexX + 1][indexY - 1].endY + (cells[indexX + 1][indexY].endY - cells[indexX + 1][indexY - 1].endY) / 2, blackPaint);
                canvas.drawLine(cells[indexX + 1][indexY].endX, cells[indexX + 1][indexY - 1].endY + (cells[indexX + 1][indexY].endY - cells[indexX + 1][indexY - 1].endY) / 2, cells[indexX - 1][indexY + 1].startX, cells[indexX - 1][indexY + 1].endY, blackPaint);
                break;
            case LEFT:
                canvas.drawLine(cells[indexX + 1][indexY - 1].endX, cells[indexX + 1][indexY - 1].startY, cells[indexX - 1][indexY].startX, cells[indexX - 1][indexY - 1].endY + (cells[indexX - 1][indexY].endY - cells[indexX - 1][indexY - 1].endY) / 2, blackPaint);
                canvas.drawLine(cells[indexX - 1][indexY].startX, cells[indexX - 1][indexY - 1].endY + (cells[indexX - 1][indexY].endY - cells[indexX - 1][indexY - 1].endY) / 2, cells[indexX + 1][indexY + 1].endX, cells[indexX + 1][indexY + 1].endY, blackPaint);
                break;
            default:
                Toast.makeText(this.getContext(), "Error with drawing robot (unknown direction)", Toast.LENGTH_LONG).show();
                break;
        }
        showLog("Exiting drawRobot");
    }

    public Direction getRobotDirection() {
        return robotDirection;
    }

    public boolean getAutoUpdate() {
        return autoUpdate;
    }

    public boolean getIsOutdoorArena(){
        return isOutdoorArena;
    }

    public void setIsOutdoorArena(boolean isOutdoor){
        isOutdoorArena=isOutdoor;
    }

    public void setSetObstacleDirection(boolean status) {
        setObstacleDirection = status;
    }

    public void setSetObstacleStatus(boolean status) {
        setObstacleStatus = status;
    }

    public void setStartCoordStatus(boolean status) {
        startCoordStatus = status;
    }

    private void createCell() {
        showLog("Entering cellCreate");
        cells = new Cell[COL + 2][ROW + 2];
        cellSize = getWidth() / (COL + 2);

        for (int x = 0; x < COL + 2; x++) {
            for (int y = 0; y < ROW + 2; y++) {
                float startX = x * cellSize;
                float startY = y * cellSize;
                cells[x][y] = new Cell(startX, startY, startX + cellSize, startY + cellSize, CellType.UNEXPLORED);
            }
        }

        //Set the borders
        for (int x = 0; x < COL + 2; x++) {
            cells[x][0].setType(CellType.BORDER);
            cells[x][ROW + 1].setType(CellType.BORDER);
        }
        for (int y = 0; y < ROW + 2; y++) {
            cells[0][y].setType(CellType.BORDER);
            cells[COL + 1][y].setType(CellType.BORDER);
        }

        showLog("Exiting createCell");
    }

    public void updateCurCoord(int mapX, int mapY, Direction direction){
        Log.i(TAG, "updateCurCoord: CURRENT COORDS:"+curCoord[0]+","+curCoord[1]);
        if(curCoord[0] != -1 && curCoord[1] != -1){
            Log.i(TAG, "updateCurCoord: UNSETTING ROBOT");
            //Unset old robot position
            int[] oldCoordIndexes = convertMapCoordToCellsIndexes(curCoord[0],curCoord[1]);
            int oldCoordXindex = oldCoordIndexes[0];
            int oldCoordYindex = oldCoordIndexes[1];

            for (int x = oldCoordXindex - 1; x <= oldCoordXindex + 1; x++) {
                for (int y = oldCoordYindex - 1; y <= oldCoordYindex + 1; y++) {
                    if (cells[x][y].type == CellType.ROBOT) {
                        Log.i(TAG, "updateCurCoord: set ["+x+"]"+"["+y+"] to unexplored");
                        cells[x][y].setType(CellType.UNEXPLORED);
                    }
                }
            }
        }

        //Update new location
        robotDirection = direction;
        curCoord[0] = mapX;
        curCoord[1] = mapY;
        int[] newCoordIndexes = convertMapCoordToCellsIndexes(mapX,mapY);
        int newCoordXIndex = newCoordIndexes[0];
        int newCoordYIndex = newCoordIndexes[1];
        for (int x = newCoordXIndex - 1; x <= newCoordXIndex + 1; x++) {
            for (int y = newCoordYIndex - 1; y <= newCoordYIndex + 1; y++) {
                if (cells[x][y].type != CellType.OBSTACLE && cells[x][y].type != CellType.BORDER) {
                    cells[x][y].setType(CellType.ROBOT);
                }
            }
        }
        invalidate();
    }

    public void setRobotDirection(Direction direction) {
        robotDirection = direction;
        this.invalidate();
    }

    private void updateRobotStatusTextView(int col, int row, Direction direction) {
        TextView xAxisTextView = ((Activity) this.getContext()).findViewById(R.id.robot_x_value);
        TextView yAxisTextView = ((Activity) this.getContext()).findViewById(R.id.robot_y_value);
        TextView directionAxisTextView = ((Activity) this.getContext()).findViewById(R.id.robotDirText);

        String newDirText_x = "X: " + String.valueOf(col - 1);

        String newDirText_y = "Y: " + String.valueOf(row - 1);

        xAxisTextView.setText(newDirText_x);
        yAxisTextView.setText(newDirText_y);

        directionAxisTextView.setText(direction.toString());
    }

    protected void setObstacleCoord(int mapX, int mapY) {
        showLog("Entering setObstacleCoord");
        //Check if obstacle has been previously set there
        if (getCellAtMapCoords(mapX, mapY).type == CellType.OBSTACLE) {
            return;
        }
        int[] obstacleCoord = new int[]{mapX, mapY};
        GridMap.obstacleCoords.add(obstacleCoord);

        Cell newObsCell = getCellAtMapCoords(mapX, mapY);
        newObsCell.setType(CellType.OBSTACLE);
        // Assign obstacle no
        for (int i = 0; i < obstacleNoArray.length; i++) {
            if (obstacleNoArray[i] != -1) {
                if (newObsCell.obstacleNo == -1) {
                    newObsCell.obstacleNo = obstacleNoArray[i]; // assign obstacle no
                    obstacleNoArray[i] = -1; // set index to marked as used
                    break;
                }
            }
        }
        this.invalidate();
        showLog("Exiting setObstacleCoord");
        updateHomeObstacleListView();
//         UNCOMMENT LINE BELOW FOR C6/7
//        sendUpdatedObstacleInformation();
    }

    protected void removeObstacleCoord(int mapX, int mapY){
        showLog("Entering removeObstacleCoord");
        Cell removeObstacleCell = getCellAtMapCoord(mapX, mapY);
        if(removeObstacleCell.type != CellType.OBSTACLE){
            Log.i(TAG, "removeObstacleCoord: Tried to remove obstacle that is not an obstacle at X:"+mapX+"Y: "+mapY);
            return;
        }
        //Return available obstacle no.
        int oldObstacleNo = removeObstacleCell.obstacleNo;
        obstacleNoArray[oldObstacleNo-1] = oldObstacleNo;
        //Reset the obstacle cell
        removeObstacleCell.obstacleNo=-1;
        removeObstacleCell.targetID=null;
        removeObstacleCell.obstacleFacing = Direction.NONE;
        removeObstacleCell.setType(CellType.UNEXPLORED);
        //Remove from arraylist
        for(int i = 0; i<obstacleCoords.size(); i++){
            int[] coord = obstacleCoords.get(i);
            if(coord[0] == mapX && coord[1]==mapY){
                obstacleCoords.remove(i);
                break;
            }
        }
        this.invalidate();
        updateHomeObstacleListView();
        showLog("Exiting removeObstacleCoord");
    }

    private ArrayList<int[]> getObstacleCoord() {
        return obstacleCoords;
    }

    private void showLog(String message) {
        Log.d(TAG, message);
    }

    private String getObstacleDirectionText(int inDirection) {
        String direction = "";
        switch (inDirection) {
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

    private class Cell {
        float startX, startY, endX, endY;
        Paint paint;
        CellType type;
        int id = -1;
        // Obstacle Face @ GridMap.Java -> class Cell
        Direction obstacleFacing = Direction.NONE;

        String targetID = null;
        int obstacleNo = -1;

        boolean isDirection = false;

        private Cell(float startX, float startY, float endX, float endY, CellType type) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            setType(type);
        }

        public void setType(CellType type) {
            this.type = type;
            switch (type) {
                case OBSTACLE:
                    this.paint = obstacleColor;
                    break;
                case ROBOT:
                    this.paint = robotColor;
                    break;
                case BORDER:
                    this.paint = endColor;
                    break;
                case UNEXPLORED:
                    this.paint = unexploredColor;
                    break;
                default:
                    showLog("setTtype default: " + type);
                    break;
            }
        }

        // Obstacle Face @ GridMap.Java -> class Cell
        public void setobstacleFacing(Direction obstacleFacing) {
            this.obstacleFacing = obstacleFacing;
        }

        public Direction getobstacleFacing() {
            return this.obstacleFacing;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getId() {
            return this.id;
        }
    }

    private enum CellType {
        UNEXPLORED,
        OBSTACLE,
        ROBOT,
        BORDER
    }

    public enum Direction{
        NONE,
        UP,
        DOWN,
        LEFT,
        RIGHT
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        showLog("Entering onTouchEvent");
        int mapX = (int) (event.getX() / cellSize) - 1;
        int mapY = ROW - ((int) (event.getY() / cellSize));

        Cell selectedCell = null;
        if ((mapX >= 0 && mapY >= 0 && mapX <= COL - 1 && mapY <= ROW - 1)) {
            selectedCell = getCellAtMapCoord(mapX, mapY);
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (startCoordStatus && selectedCell != null) {
                //TODO: Draw Robot
                //Move to a new function setRobotPosition(int x, int y)
                //Current code base violates DRY
                updateCurCoord(mapX,mapY,Direction.UP);
                canDrawRobot = true;
                invalidate();
                turnOffRobotPlacementButton();
                return true;
            }
            if (setObstacleStatus && selectedCell != null) {
                Log.i(TAG, "onTouchEvent: Adding Obstacle at X: " + mapX + " Y: " + mapY);
                this.setObstacleCoord(mapX, mapY);
                return true;
            }
            if (setObstacleDirection && selectedCell != null) {
                startFacingSelection(selectedCell);
            }
            if (obsSelected == false && selectedCell != null) {
                for (int i = 0; i < obstacleCoords.size(); i++)
                    if (obstacleCoords.get(i)[0] == mapX && obstacleCoords.get(i)[1] == mapY) {
                        selectedObsCoord[0] = mapX;
                        selectedObsCoord[1] = mapY;
                        obsSelected = true;
                        return true;
                    }
            }
        }else if(event.getAction() == MotionEvent.ACTION_UP){
            //Reset obs selected when finger is lifted from map
            if (obsSelected) {
                obsSelected = false;
                Log.d("obsSelected", Boolean.toString(obsSelected));
                return true;
            }
        }else if(event.getAction() == MotionEvent.ACTION_MOVE){
            if (obsSelected) {
                boolean occupied = false;
                for (int i = 0; i < obstacleCoords.size(); i++) {
                    if (obstacleCoords.get(i)[0] == mapX && obstacleCoords.get(i)[1] == mapY) {
                        occupied = true;
                    }
                }
                if (occupied == false) {
                    Cell oldObstacleCell = getCellAtMapCoord(selectedObsCoord[0], selectedObsCoord[1]);
                    //Cache old obstacle direction
                    Direction oldObstacleDir = oldObstacleCell.obstacleFacing;
                    String oldTargetID = oldObstacleCell.targetID;
                    removeObstacleCoord(selectedObsCoord[0], selectedObsCoord[1]);

                    //If selection is within the grid; move to new position
                    if (mapX < 20 && mapY < 20 && mapX > 0 && mapY > 0) {
                        //Update selectedObsCoord;
                        selectedObsCoord[0] = mapX;
                        selectedObsCoord[1] = mapY;

                        setObstacleCoord(mapX,mapY);
                        selectedCell.obstacleFacing = oldObstacleDir;
                        selectedCell.targetID = oldTargetID;
                    }
                    this.invalidate();
                    return true;
                }

            }
        }
        showLog("Exiting onTouchEvent");
        return false;
    }

    private void startFacingSelection(Cell selectedCell) {
        boolean isSetRobot = (selectedCell.type == CellType.ROBOT);

        AlertDialog.Builder mBuilder = new AlertDialog.Builder(getContext());
        mBuilder.setTitle("Select Direction");
        mBuilder.setSingleChoiceItems(directionList, switchDirection, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switchDirection = i;
            }
        });
        mBuilder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Direction selectedDirection = Direction.NONE;
                switch (switchDirection) {
                    case 0:
                        selectedDirection = Direction.NONE;
                        break;
                    case 1:
                        selectedDirection = Direction.UP;
                        break;
                    case 2:
                        selectedDirection = Direction.DOWN;
                        break;
                    case 3:
                        selectedDirection = Direction.LEFT;
                        break;
                    case 4:
                        selectedDirection = Direction.RIGHT;
                        break;
                }

                if(isSetRobot && selectedDirection == Direction.NONE){
                    setRobotDirection(Direction.UP);
                }else if(isSetRobot){
                    setRobotDirection(selectedDirection);
                }else{
                    selectedCell.setobstacleFacing(selectedDirection);
                    updateHomeObstacleListView();
                }
                // UNCOMMENT BELOW FOR C6/7
//                        if(!isSetRobot){
//                            sendUpdatedObstacleInformation();
//                        }
                invalidate();
                dialogInterface.dismiss();
            }
        });

        if(selectedCell.type == CellType.OBSTACLE || selectedCell.type == CellType.ROBOT){
            AlertDialog dialog = mBuilder.create();
            dialog.show();
        }
    }

    public void turnOffRobotPlacementButton() {
        Button placeRobotBtn = ((Activity) this.getContext()).findViewById(R.id.btnPlaceRobot);
        setStartCoordStatus(false);
        placeRobotBtn.setText("Place Robot");
    }

    public void resetMap() {
        showLog("Entering resetMap");
        TextView robotStatusTextView = ((Activity) this.getContext()).findViewById(R.id.robotStatusText);
        updateRobotStatusTextView(1, 1, Direction.NONE);
        robotStatusTextView.setText("Not Available");

        curCoord = new int[]{-1, -1};
        robotDirection = Direction.NONE;
        obstacleCoords = new ArrayList<>();
        mapDrawn = false;
        canDrawRobot = false;
        oCellArr = new ArrayList<>();

        // newly added
        obstacleNoArray = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};

        updateHomeObstacleListView();
        showLog("Exiting resetMap");
        this.invalidate();
    }

    private int directionEnumToInt(Direction direction) {
        switch (direction) {
            case UP:
                return 0;
            case RIGHT:
                return 2;
            case DOWN:
                return 4;
            case LEFT:
                return 6;
            default:
                return -1;
        }
    }

    public void sendUpdatedObstacleInformation() {
        try {
            JSONArray obstaclesList = new JSONArray();

            for (int i = 0; i < obstacleCoords.size(); i++) {
                JSONObject obstacle = new JSONObject();
                int obstacleX = obstacleCoords.get(i)[0];
                int obstacleY = obstacleCoords.get(i)[1];
                Cell obstacleCell = getCellAtMapCoord(obstacleX, obstacleY);
                obstacle.put("x", obstacleX);
                obstacle.put("y", obstacleY);
                obstacle.put("id", obstacleCell.obstacleNo);
                obstacle.put("d", directionEnumToInt(obstacleCell.obstacleFacing));

                obstaclesList.put(obstacle);
            }
            JSONObject valueObj = new JSONObject();
            valueObj.put("obstacles", obstaclesList);
            if(isOutdoorArena){
                valueObj.put("mode","1");
            }else{
                valueObj.put("mode","0");
            }

            JSONObject msgJSON = new JSONObject();
            msgJSON.put("cat", "obstacles");
            msgJSON.put("value", valueObj);

            Intent upDirectionIntent = new Intent("sendBTMessage");
            upDirectionIntent.putExtra("msg", msgJSON.toString());
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(upDirectionIntent);
        } catch (Exception ex) {
            Log.e(TAG, "sendUpdatedObstacleInformation: An error occured while sending obstacle information to device");
            ex.printStackTrace();
        }


    }

    private Cell getCellAtMapCoord(int x, int y) {
        return cells[x + 1][ROW - y];
    }

    private int[] convertMapCoordToCellsIndexes(int mapX, int mapY){
        int[] convertedCoords = {mapX+1,ROW-mapY};
        return convertedCoords;
    }

    private void updateHomeObstacleListView(){
        try{
            JSONArray obstacleInfo = new JSONArray();
            for(int[] obsCoord : obstacleCoords){
                JSONObject obstalceObj = new JSONObject();
                Cell cell = getCellAtMapCoords(obsCoord[0],obsCoord[1]);
                obstalceObj.put("no",cell.obstacleNo);
                obstalceObj.put("x",obsCoord[0]);
                obstalceObj.put("y",obsCoord[1]);
                obstalceObj.put("facing",cell.obstacleFacing.toString());

                obstacleInfo.put(obstalceObj);
            }

            Intent obstacleListIntent = new Intent("newObstacleList");
            obstacleListIntent.putExtra("msg", obstacleInfo.toString());
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(obstacleListIntent);
        }catch (Exception e){
            Log.e(TAG, "updateFrontEndListView: Error adding obstalce to JSON");
        }
    }


}
