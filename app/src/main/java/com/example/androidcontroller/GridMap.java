package com.example.androidcontroller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
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

    private static String robotDirection = "None";
    private static int[] startCoord = new int[]{-1, -1};
    private static int[] curCoord = new int[]{-1, -1};
    private static int[] oldCoord = new int[]{-1, -1};
    private static int[] waypointCoord = new int[]{-1, -1};
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

    private static int[] selectedObsCoord = new int[3];
    private static boolean obsSelected = false;
    private static ArrayList<Cell> oCellArr = new ArrayList<Cell>();

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
        endColor.setColor(Color.GREEN);
        startColor.setColor(Color.CYAN);
        waypointColor.setColor(Color.parseColor("#fefdca"));
        unexploredColor.setColor(Color.LTGRAY);
        exploredColor.setColor(Color.WHITE);
        arrowColor.setColor(Color.BLACK);

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
            this.createCell();
            mapDrawn = true;
        }

        drawIndividualCell(canvas);
        drawGridLines(canvas);
        drawGridNumber(canvas);

        if (getCanDrawRobot())
            drawRobot(canvas, curCoord);

        showLog("Exiting onDraw");
    }

    private void drawIndividualCell(Canvas canvas) {
        showLog("Entering drawIndividualCell");

        for(int x=0;x<COL+2;x++){
            for(int y=0;y<ROW+2;y++){
                Cell cell = cells[x][y];
                canvas.drawRect(cell.startX, cell.startY, cell.endX, cell.endY, cell.paint);

                if(cell.type == CellType.OBSTACLE){
                    //Draw the number for the obstacle
                    if(cell.targetID == null){
                        canvas.drawText(Integer.toString(cell.obstacleNo), cell.startX + (cellSize / 3.2f), cell.startY + (cellSize / 1.5f), exploredColor);
                    }else{
                        Paint targetPaint = new Paint();
                        targetPaint.setTextSize(20);
                        targetPaint.setColor(Color.GREEN);
                        targetPaint.setTextAlign(Paint.Align.CENTER);
                        canvas.drawText(cell.targetID, (cell.startX+cell.endX)/2, cell.endY + (cell.startY-cell.endY)/4, targetPaint);
                    }

                    //Draw the obstacle facing
                    if(cell.obstacleFacing != null || cell.obstacleFacing.equalsIgnoreCase("NONE")){
                        switch(cell.obstacleFacing){
                            case "UP":
                                canvas.drawRect(cell.startX + 2 , cell.startY + 1, cell.endX, cell.endY - (cellSize / 1.1f), imageLine);
                                break;
                            case "DOWN":
                                canvas.drawRect(cell.startX + 2, cell.startY + (cellSize / 1f) - 2, cell.endX, cell.endY - 1, imageLine);
                                break;
                            case "LEFT":
                                canvas.drawRect(cell.startX + 1, cell.startY + 2, cell.endX - (cellSize / 1.1f), cell.endY, imageLine);
                                break;
                            case "RIGHT":
                                canvas.drawRect(cell.startX + (cellSize / 1f) -2, cell.startY, cell.endX -1, cell.endY, imageLine);
                                break;
                        }
                    }
                }
            }
        }
        showLog("Exiting drawIndividualCell");
    }

    private Cell getCellAtMapCoords(int x, int y){
        return cells[x+1][COL-y];
    }

    public void updateImageNumberCell(int obstacleNo, String targetID){
        // find the obstacle no which has the same id
        for (int x = 1; x <= COL; x++)
            for (int y = 1; y <= ROW; y++)
                    if (cells[x][y].obstacleNo == obstacleNo) {
                        cells[x][y].targetID = targetID;
                    }
        this.invalidate();
    }

    private void drawGridLines(Canvas canvas){
        //HORIZONTAL LINES
        for(int y=0;y<=COL; y++){
            Cell start = cells[1][y];
            Cell end = cells[COL][y];
            canvas.drawLine(start.startX, start.endY,end.endX,end.endY, blackPaint );
        }

        //VERTICAL LINES
        for(int x=1;x<=COL+1;x++){
            Cell start = cells[x][1];
            Cell end = cells[x][ROW];
            canvas.drawLine(start.startX, start.startY,end.startX,end.endY, blackPaint );
        }
    }

    private void drawGridNumber(Canvas canvas) {
        showLog("Entering drawGridNumber");
        //X-AXIS numbers
        for(int x=1; x<=COL;x++){
            Cell cell = cells[x][COL+1];
            String num = ""+(x-1);
            if(x>9)
                canvas.drawText(num, cell.startX + (cellSize/5), cell.startY+(cellSize/3),blackPaint);
            else
                canvas.drawText(num, cell.startX + (cellSize/3), cell.startY+(cellSize/3),blackPaint);
        }
        //Y-AXIS numbers
        for(int y=1;y<=ROW;y++){
            Cell cell = cells[0][y];
            int adjustedY = ROW-y;
            String num = ""+adjustedY;
            if(adjustedY > 9)
                canvas.drawText(num, cell.startX + (cellSize / 2), cell.startY + (cellSize / 1.5f), blackPaint);
            else
                canvas.drawText(num, cell.startX + (cellSize / 2), cell.startY + (cellSize / 1.5f), blackPaint);
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
            newEndCoord=false;
        }
        for (int y = androidRowCoord; y <= androidRowCoord + 1; y++){
            canvas.drawLine(cells[curCoord[0] - 1][y].startX, cells[curCoord[0] - 1][y].startY - (cellSize / 30), cells[curCoord[0] + 1][y].endX, cells[curCoord[0] + 1][y].startY - (cellSize / 30), robotColor );
        }
        for (int x = curCoord[0] - 1; x < curCoord[0] + 1; x++){
            canvas.drawLine(cells[x][androidRowCoord - 1].startX - (cellSize / 30) + cellSize, cells[x][androidRowCoord - 1].startY, cells[x][androidRowCoord + 1].startX - (cellSize / 30) + cellSize, cells[x][androidRowCoord + 1].endY, robotColor);
        }
        int col = curCoord[0];
        int row = androidRowCoord;
        RectF rect = new RectF(col * cellSize, row * cellSize, (col + 1) * cellSize, (row + 1) * cellSize);

        switch (this.getRobotDirection()) {
            case "up":
                //left drawn line
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord + 1].startX, cells[curCoord[0] - 1][androidRowCoord + 1].endY, (cells[curCoord[0]][androidRowCoord - 1].startX + cells[curCoord[0]][androidRowCoord - 1].endX) / 2, cells[curCoord[0]][androidRowCoord - 1].startY, blackPaint);
                //right drawn line
                canvas.drawLine((cells[curCoord[0]][androidRowCoord - 1].startX + cells[curCoord[0]][androidRowCoord - 1].endX) / 2, cells[curCoord[0]][androidRowCoord - 1].startY, cells[curCoord[0] + 1][androidRowCoord + 1].endX, cells[curCoord[0] + 1][androidRowCoord + 1].endY, blackPaint);
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
        turnOffRobotPlacementButton();
        showLog("Exiting drawRobot");
    }

    public String getRobotDirection() {
        return robotDirection;
    }

    public boolean getAutoUpdate() {
        return autoUpdate;
    }

    public void setSetObstacleDirection(boolean status)
    {
        setObstacleDirection = status;
    }

    public void setSetObstacleStatus(boolean status) {
        setObstacleStatus = status;
    }

    public void setStartCoordStatus(boolean status) {
        startCoordStatus = status;
    }

    private boolean getStartCoordStatus() {
        return startCoordStatus;
    }

    public boolean getCanDrawRobot() {
        return canDrawRobot;
    }

    private void createCell() {
        showLog("Entering cellCreate");
        cells = new Cell[COL + 2][ROW + 2];
        cellSize = getWidth()/(COL+2);

        for (int x = 0; x < COL+2; x++){
            for (int y = 0; y < ROW+2; y++){
                float startX = x*cellSize;
                float startY = y*cellSize;
                cells[x][y] = new Cell(startX, startY, startX+cellSize, startY+cellSize, CellType.UNEXPLORED);
            }
        }

        //Set the borders
        for(int x=0;x<COL+2;x++){
            cells[x][0].setType(CellType.BORDER);
            cells[x][ROW+1].setType(CellType.BORDER);
        }
        for(int y=0;y<ROW+2;y++){
            cells[0][y].setType(CellType.BORDER);
            cells[COL+1][y].setType(CellType.BORDER);
        }

        showLog("Exiting createCell");
    }

    public void setEndCoord(int col, int row) {
        showLog("Entering setEndCoord");
        row = this.convertRow(row);
        for (int x = col - 1; x <= col + 1; x++)
            for (int y = row - 1; y <= row + 1; y++)
                cells[x][y].setType(CellType.BORDER);

        showLog("Exiting setEndCoord");
    }

    public void setStartCoord(int col, int row){
        showLog("Entering setStartCoord");
        Toast.makeText(getContext(), "Entering setStartCoord", Toast.LENGTH_SHORT).show();
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
        Toast.makeText(getContext(), "Entering setCurCoord", Toast.LENGTH_SHORT).show();

        curCoord[0] = col;
        curCoord[1] = row;
        this.setRobotDirection(direction);
        this.updateRobotAxis(col, row, direction);

        row = this.convertRow(row);
        for (int x = col - 1; x <= col + 1; x++){
            for (int y = row - 1; y <= row + 1; y++){
                if(cells[x][y].type != CellType.OBSTACLE){
                    cells[x][y].setType(CellType.ROBOT);
                }
            }
        }

        showLog("Exiting setCurCoord");
    }

    public int[] getCurCoord() {
        return curCoord;
    }

    private int convertRow(int row) {
        return (20 - row);
    }

    public void unsetOldRobotCoord(int oldCol, int oldRow) {
        showLog("Entering setOldRobotCoord");
        oldCoord[0] = oldCol;
        oldCoord[1] = oldRow;
        oldRow = this.convertRow(oldRow);
        for (int x = oldCol - 1; x <= oldCol + 1; x++){
            for (int y = oldRow - 1; y <= oldRow + 1; y++){
                if(cells[x][y].type != CellType.OBSTACLE){
                    cells[x][y].setType(CellType.UNEXPLORED);
                }
            }
        }
        showLog("Exiting setOldRobotCoord");
    }

    private int[] getOldRobotCoord() {
        return oldCoord;
    }

    public void setRobotDirection(String direction) {
        Toast.makeText(getContext(), "SET robotDirection", Toast.LENGTH_SHORT).show();

        sharedPreferences = getContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        robotDirection = direction;
        editor.putString("direction", direction);
        editor.commit();
        this.invalidate();
    }

    private void updateRobotAxis(int col, int row, String direction) {
        TextView xAxisTextView =  ((Activity)this.getContext()).findViewById(R.id.robot_x_value);
        TextView yAxisTextView =  ((Activity)this.getContext()).findViewById(R.id.robot_y_value);
        TextView directionAxisTextView =  ((Activity)this.getContext()).findViewById(R.id.robotDirText);

        String newDirText_x = "X: " + String.valueOf(col-1);

        String newDirText_y = "Y: " + String.valueOf(row-1);

        xAxisTextView.setText(newDirText_x);
        yAxisTextView.setText(newDirText_y);

        directionAxisTextView.setText(direction);
        if(direction.equals("up"))
        {
            //directionAxisTextView.setText(direction + " (N)");
            directionAxisTextView.setText("N");
        }
        else if (direction.equals("down"))
        {
            //directionAxisTextView.setText(direction + " (S) ");
            directionAxisTextView.setText("S");
        }
        else if (direction.equals("right"))
        {
            //.setText(direction + " (E) ");
            directionAxisTextView.setText("E");
        }
        else if (direction.equals("left"))
        {
            //directionAxisTextView.setText(direction + " (W) ");
            directionAxisTextView.setText("W");
        }
        else if (direction.equals("upleft"))
        {
            //directionAxisTextView.setText(direction + " (W) ");
            directionAxisTextView.setText("NW");
        }
        else if (direction.equals("upright"))
        {
            //directionAxisTextView.setText(direction + " (W) ");
            directionAxisTextView.setText("NE");
        }
        else if (direction.equals("downleft"))
        {
            //directionAxisTextView.setText(direction + " (W) ");
            directionAxisTextView.setText("SW");
        }
        else if (direction.equals("downright"))
        {
            //directionAxisTextView.setText(direction + " (W) ");
            directionAxisTextView.setText("SE");
        }
        else
        {
            directionAxisTextView.setText("None");
        }
    }

    protected void setObstacleCoord(int mapX, int mapY) {
        showLog("Entering setObstacleCoord");
        //Check if obstacle has been previously set there
        if(getCellAtMapCoords(mapX,mapY).type == CellType.OBSTACLE){
            return;
        }
        int[] obstacleCoord = new int[]{mapX, mapY};
        GridMap.obstacleCoord.add(obstacleCoord);

        Cell newObsCell = getCellAtMapCoords(mapX,mapY);
        newObsCell.setType(CellType.OBSTACLE);
        // Assign obstacle no
        for(int i = 0; i<obstacleNoArray.length; i++){
            if(obstacleNoArray[i] != -1){
                if(newObsCell.obstacleNo == -1){
                    newObsCell.obstacleNo = obstacleNoArray[i]; // assign obstacle no
                    obstacleNoArray[i] = -1; // set index to marked as used
                    break;
                }
            }
        }
        this.invalidate();
        showLog("Exiting setObstacleCoord");
//         UNCOMMENT LINE BELOW FOR C6/7
//        sendUpdatedObstacleInformation();
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

    private class Cell {
        float startX, startY, endX, endY;
        Paint paint;
        CellType type;
        int id = -1;
        // Obstacle Face @ GridMap.Java -> class Cell
        String obstacleFacing = "null";

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

    private enum CellType{
        UNEXPLORED,
        OBSTACLE,
        ROBOT,
        BORDER
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        showLog("Entering onTouchEvent");
        int mapX = (int) (event.getX() / cellSize)-1;
        int mapY = this.convertRow((int) (event.getY() / cellSize));

        Toast.makeText((Activity) this.getContext(), "Tapped on Map: X:"+mapX+" Y: "+mapY, Toast.LENGTH_SHORT).show();
        
        if(!(mapX >= 0  && mapY >= 0 && mapX <= COL-1 && mapY <= ROW-1)){
            Log.i(TAG, "onTouchEvent: Tapped out of arena bounds");
            return true;
        }

        Cell selectedCell = getCellAtMapCoord(mapX, mapY);
        
        if(event.getAction() == MotionEvent.ACTION_DOWN){
            if(startCoordStatus){
                //TODO: Draw Robot
                //Move to a new function setRobotPosition(int x, int y)
                //Current code base violates DRY
            }
            if(setObstacleStatus){
                Log.i(TAG, "onTouchEvent: Adding Obstacle at X: "+mapX+" Y: "+mapY);
                this.setObstacleCoord(mapX, mapY);
                return true;
            }
            if (setObstacleDirection) {
                boolean isSetRobot;
                isSetRobot = (selectedCell.type == CellType.ROBOT);

            }
        }
        //=============OLD CODE======================
//        if (event.getAction() == MotionEvent.ACTION_DOWN && this.getAutoUpdate() == false && mapX<=20 && mapY<=20 && mapX>=1 && mapY>=1) {
//
//            if (startCoordStatus) {
//                if (canDrawRobot) {
//                    int[] startCoord = this.getStartCoord();
//                    if (startCoord[0] >= 2 && startCoord[1] >= 2) {
//                        startCoord[1] = this.convertRow(startCoord[1]);
//                        for (int x = startCoord[0] - 1; x <= startCoord[0] + 1; x++) {
//                            for (int y = startCoord[1] - 1; y <= startCoord[1] + 1; y++) {
//                                if (!cells[x][y].type.equals("obstacle")) {
//                                    cells[x][y].setType(CellType.UNEXPLORED);
//                                }
//                            }
//                        }
//                    }
//                }
//                else
//                    canDrawRobot = true;
//                this.setStartCoord(mapX, mapY);
//                startCoordStatus = false;
//                String direction = getRobotDirection();
//                if(direction.equals("None")) {
//                    direction = "up";
//                }
//                try {
//                    int directionInt = 0;
//                    if(direction.equals("up")){
//                        directionInt = 0;
//                    } else if(direction.equals("left")) {
//                        directionInt = 3;
//                    } else if(direction.equals("right")) {
//                        directionInt = 1;
//                    } else if(direction.equals("down")) {
//                        directionInt = 2;
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//
//                //update robot axis
//                updateRobotAxis(mapX, mapY, direction);
//
//                this.invalidate();
//                return true;
//            }
//            if (setObstacleStatus) { // setting the position of the obstacle
//
//                // TODO: New direction
//                cells[mapX][20-mapY].setobstacleFacing(getObstacleDirectionText(newDirection));
//
//                this.setObstacleCoord(mapX, mapY);
//
//                this.invalidate();
//                return true;
//
//            }
//            if (unSetCellStatus) { // TODO: remove obstacle not yet use (ontouch on the map to remove)
//                ArrayList<int[]> obstacleCoord = this.getObstacleCoord();
//                cells[mapX][20-mapY].setType(CellType.UNEXPLORED);
//                for (int i=0; i<obstacleCoord.size(); i++) {
//                    if (obstacleCoord.get(i)[0] == mapX && obstacleCoord.get(i)[1] == mapY){
//                        obstacleNoArray[cells[mapX][20-mapY].obstacleNo - 1] = cells[mapX][20-mapY].obstacleNo; // unset obstacle no by assigning number back to array
//                        cells[mapX][20-mapY].obstacleNo = -1;
//                        obstacleCoord.remove(i);
//                        if(oCellArr.get(oCellArr.size()-1) == cells[mapX][20-mapY]){
//                            oCellArr.remove(oCellArr.size()-1);
//                            //oCellArrDirection.remove(oCellArrDirection.size()-1);
//                        }
//                    }
//                }
//                this.invalidate();
//                return true;
//            }
//
//            if(setObstacleDirection)
//            {
//                boolean isSetRobot;
//                isSetRobot = cells[mapX][20 - mapY].type.equals("robot");
//
//                if((setObstacleDirection && isSetRobot)){
//                    Toast.makeText((Activity) this.getContext(), "SETTING ROBOT DIR", Toast.LENGTH_SHORT).show();
//                }
//                showLog("Enter set obstacle direction");
//
//                AlertDialog.Builder mBuilder = new AlertDialog.Builder(getContext());
//                mBuilder.setTitle("Select Obstacle Direction");
//                mBuilder.setSingleChoiceItems(directionList, switchDirection, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        switchDirection = i;
//                    }
//                });
//                mBuilder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//
//                        switch (switchDirection)
//                        {
//                            case 0:
//                                if(isSetRobot){
//                                    setRobotDirection("up");
//                                }
//                                else {
//                                    cells[mapX][20 - mapY].setobstacleFacing("NONE");
//                                }
//                                break;
//                            case 1:
//                                if(isSetRobot){
//                                    setRobotDirection("up");
//                                }
//                                else{
//                                    cells[mapX][20 - mapY].setobstacleFacing("UP");
//                                }
//                                break;
//                            case 2:
//                                if(isSetRobot){
//                                    setRobotDirection("down");
//                                }
//                                else{
//                                    cells[mapX][20 - mapY].setobstacleFacing("DOWN");
//                                }
//                                break;
//                            case 3:
//                                if(isSetRobot){
//                                    setRobotDirection("left");
//                                }
//                                else{
//                                    cells[mapX][20 - mapY].setobstacleFacing("LEFT");
//                                }
//                                break;
//                            case 4:
//                                if(isSetRobot){
//                                    setRobotDirection("right");
//                                }
//                                else{
//                                cells[mapX][20 - mapY].setobstacleFacing("RIGHT");
//                                }
//                                break;
//                        }
//                        // UNCOMMENT BELOW FOR C6/7
////                        if(!isSetRobot){
////                            sendUpdatedObstacleInformation();
////                        }
//                        invalidate();
//
//                        dialogInterface.dismiss();
//                    }
//                });
//
//                // check if the cell selected is obstacle or not
//                if(cells[mapX][20 - mapY].type.equals("obstacle") || isSetRobot) {
//
//                    AlertDialog dialog = mBuilder.create();
//                    dialog.show();
//                }
//
//                this.invalidate();
//                showLog("Exit set obstacle direction");
//                return true;
//            }
//            // selection of obstacle in the map
//            if (obsSelected == false){
//                ArrayList<int[]> obstacleCoord = this.getObstacleCoord();
//                for (int i=0; i<obstacleCoord.size(); i++)
//                    if (obstacleCoord.get(i)[0] == mapX && obstacleCoord.get(i)[1] == mapY){
//                        selectedObsCoord[0] = mapX;
//                        selectedObsCoord[1] = mapY;
//                        for (int x = 0; x < oCellArr.size(); x++) {
//                            if (oCellArr.get(x) == cells[mapX][20-mapY]) {
//                                selectedObsCoord[2] = x;
//                            }
//                        }
//                        obsSelected = true;
//                        return true;
//                    }
//            }
//        }
//        // when touch event is release from the map
//        else if (event.getAction() == MotionEvent.ACTION_UP && this.getAutoUpdate() == false) {
//            if(obsSelected) {
//                obsSelected = false;
//                Log.d("obsSelected", Boolean.toString(obsSelected));
//                return true;
//            }
//        }
//        // moving obstacle around or out of the map
//        else if (event.getAction() == MotionEvent.ACTION_MOVE && this.getAutoUpdate() == false) {
//            if (obsSelected) {
//                boolean occupied = false;
//                ArrayList<int[]> obstacleCoord = this.getObstacleCoord();
//                for (int i = 0; i < obstacleCoord.size(); i++) {
//                    if (obstacleCoord.get(i)[0] == mapX && obstacleCoord.get(i)[1] == mapY) {
//                        occupied = true;
//                    }
//                }
//                if (occupied == false) {
//
//                    // TODO: NEW obstacle
//                    //BluetoothFragment.printMessage("RemovedObstacle, " + "<" + valueOf(selectedObsCoord[0] - 1) + ">, <" + valueOf(Math.abs(selectedObsCoord[1]) - 1) + ">, <" + cells[selectedObsCoord[0]][20 - selectedObsCoord[1]].obstacleNo + ">");
//                    obstacleNoArray[cells[selectedObsCoord[0]][20 - selectedObsCoord[1]].obstacleNo - 1] = cells[selectedObsCoord[0]][20 - selectedObsCoord[1]].obstacleNo; // unset obstacle no by assigning number back to array
//                    cells[selectedObsCoord[0]][20 - selectedObsCoord[1]].obstacleNo = -1;
//                    cells[selectedObsCoord[0]][20 - selectedObsCoord[1]].setType(CellType.UNEXPLORED);
//                    // Remove obstacle facing direction
//                    obsSelectedFacing = cells[selectedObsCoord[0]][20 - selectedObsCoord[1]].getobstacleFacing(); // <-- newly added
//                    cells[selectedObsCoord[0]][20 - selectedObsCoord[1]].setobstacleFacing(null); // <-- newly added
//                    // Remove target ID
//                    obsTargetImageID = cells[selectedObsCoord[0]][20 - selectedObsCoord[1]].targetID; // <-- newly added
//                    cells[selectedObsCoord[0]][20 - selectedObsCoord[1]].targetID = null; // <-- newly added
//
//                    //Remove from obstacles coord arraylist
//                    for (int i = 0; i < obstacleCoord.size(); i++) {
//                        if (obstacleCoord.get(i)[0] == selectedObsCoord[0] && obstacleCoord.get(i)[1] == selectedObsCoord[1]) {
//                            obstacleCoord.remove(i);
//                            //UNCOMMENT BELOW FOR C6/7
////                            sendUpdatedObstacleInformation();
//                        }
//                    }
//                    //If selection is within the grid
//                    if (mapX <= 20 && mapY <= 20 && mapX >= 1 && mapY >= 1) {
//                        //Create the new cell
//                        oCellArr.set(selectedObsCoord[2], cells[mapX][20 - mapY]);
//
//                        selectedObsCoord[0] = mapX;
//                        selectedObsCoord[1] = mapY;
//                        // Add obstacle facing direction
//                        cells[mapX][20-mapY].setobstacleFacing(obsSelectedFacing); // <-- newly added
//                        // Add target ID
//                        cells[mapX][20-mapY].targetID = obsTargetImageID;  // <-- newly added
//
//                        this.setObstacleCoord(mapX, mapY);
//                    }
//                    //If selection is outside the grid
//                    else if (mapX < 1 || mapY < 1 || mapX > 20 || mapY > 20) {
//                        obsSelected = false;
//                        //Remove from oCellArr
//                        if (oCellArr.get(oCellArr.size() - 1) == cells[selectedObsCoord[0]][20 - selectedObsCoord[1]]) {
//                            oCellArr.remove(oCellArr.size() - 1);
//                            //oCellArrDirection.remove(oCellArrDirection.size() - 1);
//
//                            // Remove obstacle facing direction
//                            cells[selectedObsCoord[0]][20-selectedObsCoord[1]].setobstacleFacing(null); //<-- newly added
//                            // Remove target ID
//                            cells[selectedObsCoord[0]][20 - selectedObsCoord[1]].targetID = null; // <-- newly added
//                        }
//                    }
//                    this.invalidate();
//                    return true;
//                }
//
//            }
//        }
        showLog("Exiting onTouchEvent");
        return false;
    }

    private void startFacingSelection(){

    }

    public void turnOffRobotPlacementButton(){
        Button placeRobotBtn = ((Activity)this.getContext()).findViewById(R.id.btnPlaceRobot);
        setStartCoordStatus(false);
        placeRobotBtn.setText("Place Robot");
    }

    public void resetMap() {
        showLog("Entering resetMap");
        TextView robotStatusTextView =  ((Activity)this.getContext()).findViewById(R.id.robotStatusText);
        updateRobotAxis(1, 1, "None");
        robotStatusTextView.setText("Not Available");
        SharedPreferences.Editor editor = sharedPreferences.edit();

        startCoord = new int[]{-1, -1};
        curCoord = new int[]{-1, -1};
        oldCoord = new int[]{-1, -1};
        robotDirection = "None";
        autoUpdate = false;
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

    private int facingStringToInt(String direction){
        if(direction == null || direction.isEmpty()){
            return -1;
        }

        switch(direction.toUpperCase()){
            case "N":
            case "NORTH":
            case "UP":
            case "U":
                return 0;
            case "E":
            case "EAST":
            case "RIGHT":
            case "R":

                return 2;
            case "S":
            case "SOUTH":
            case "DOWN":
            case "D":
                return 4;
            case "W":
            case "WEST":
            case "LEFT":
            case "L":
                return 6;
            default:
                return -1;
        }
    }
    
    public void sendUpdatedObstacleInformation(){
        try{
            JSONArray obstaclesList = new JSONArray();

            for(int i = 0; i<obstacleCoord.size();i++){
                JSONObject obstacle = new JSONObject();
                int obstacleX = obstacleCoord.get(i)[0];
                int obstacleY = obstacleCoord.get(i)[1];
                Cell obstacleCell = cells[obstacleX][20-obstacleY];
                obstacle.put("x",obstacleX-1);
                obstacle.put("y",obstacleY-1);
                obstacle.put("id",obstacleCell.obstacleNo);
                obstacle.put("d",facingStringToInt(obstacleCell.obstacleFacing));

                obstaclesList.put(obstacle);
            }
            JSONObject valueObj = new JSONObject();
            valueObj.put("obstacles",obstaclesList);

            JSONObject msgJSON = new JSONObject();
            msgJSON.put("cat","obstacles");
            msgJSON.put("value",valueObj);

            Intent upDirectionIntent = new Intent("sendBTMessage");
            upDirectionIntent.putExtra("msg",msgJSON.toString());
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(upDirectionIntent);
        }catch (Exception ex){
            Log.e(TAG, "sendUpdatedObstacleInformation: An error occured while sending obstacle information to device");
            ex.printStackTrace();
        }


    }

    private Cell getCellAtMapCoord(int x, int y){
        return cells[x+1][ROW-y];
    }


}
