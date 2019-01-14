package com.example.dots.model;

import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;

import com.example.dots.algorithms.WaveAlgorithm;
import com.example.dots.drawableObjects.BlueDot;
import com.example.dots.drawableObjects.BlueLine;
import com.example.dots.drawableObjects.GrayLine;
import com.example.dots.drawableObjects.RedDot;
import com.example.dots.drawableObjects.RedLine;
import com.example.dots.interfaces.Drawable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Class represents a squared list of paper and a collection of all dots and lines.
 * cell is 0.5cm x 0.5cm
 */

public class GameField {
    private Set<Point> activeBlueDots;//blue dots that can be used for surrounding
    private Set<Point> activeRedDots;// red dots that can be used for surrounding
    private Set<Point> capturedBlueDots;// captured blue dots by red player, cannot be used for surrounding
    private Set<Point> capturedRedDots;// captured red dots by blue player, cannot be used for surrounding
    private Set<Point> releasedBlueDots;//blue dots that are inside opponent enclosed area that is inside blue player enclosed area, doesn't count in score
    private Set<Point> releasedRedDots;//red dots that are inside opponent enclosed area that is inside red player enclosed area, doesn't count in score
    private Set<Line> blueLines;
    private Set<Line> redLines;
    private List<Line> defaultLines;// gray lines, vertical and horizontal. represent squared list of paper
    private boolean emptyCells[][]; // free cells, dot can be placed there
    private static final int SET_SIZE = 32;
    private static final boolean OCCUPIED = true;


    public GameField(int widthCM, int heightCM){
        int width = widthCM*2 + 1;
        int height = heightCM*2 + 1;
        emptyCells = new boolean[width][height];
        activeBlueDots = new HashSet<>(SET_SIZE);
        activeRedDots = new HashSet<>(SET_SIZE);
        capturedBlueDots = new HashSet<>(SET_SIZE);
        capturedRedDots = new HashSet<>(SET_SIZE);
        blueLines = new HashSet<>(SET_SIZE);
        redLines = new HashSet<>(SET_SIZE);
        defaultLines = new LinkedList<>();
        releasedBlueDots = new HashSet<>(SET_SIZE);
        releasedRedDots = new HashSet<>(SET_SIZE);
        for(float i = 0;i<height+1; i++){//horizontal
            float startX = 0;
            float startY = i*0.5f;
            float stopX = width*0.5f;
            float stopY = i*0.5f;
            defaultLines.add(new Line(startX,startY,stopX,stopY));
        }
        for(float i = 0; i<width+1; i++){//vertical
            float startX = i*0.5f;
            float startY = 0;
            float stopX = i*0.5f;
            float stopY = height*0.5f;
            defaultLines.add(new Line(startX,startY,stopX,stopY));
        }
        createStartedDots();
    }

    /**
     * Set dot if can, and calculates all enclosed areas if they exist
     * @param dot - dot to be placed
     * @param color of dot, blue ore red
     * @return true - if dot can be placed, false otherwise
     */
    public boolean setDot(Point dot, int color){
        Set<Point> playerActiveDots, playerCapturedDots, playerReleasedDots,
                opponentsActiveDots, opponentCapturedDots;
        Set<Line> playerLines, opponentLines;
        List<EnclosedArea> areas;
        if(emptyCells[dot.x][dot.y] == OCCUPIED)
            return false;

        switch (color){
            case Color.BLUE:
                playerActiveDots = activeBlueDots;
                playerCapturedDots = capturedBlueDots;
                playerReleasedDots = releasedBlueDots;
                opponentsActiveDots = activeRedDots;
                opponentCapturedDots = capturedRedDots;
                playerLines = blueLines;
                opponentLines = redLines;
                break;
            case Color.RED:
                playerActiveDots = activeRedDots;
                playerCapturedDots = capturedRedDots;
                playerReleasedDots = releasedRedDots;
                opponentsActiveDots = activeBlueDots;
                opponentCapturedDots = capturedBlueDots;
                playerLines = redLines;
                opponentLines = blueLines;
                break;
            default:
                return false;
        }

        playerActiveDots.add(new Point(dot));
        //find enclosed areas
        areas = WaveAlgorithm.getDotsOfEnclosedAreas(playerActiveDots,dot.x,dot.y, emptyCells.length,
                emptyCells[0].length);
        emptyCells[dot.x][dot.y] = OCCUPIED;
        //there are enclosed areas
        if(!areas.isEmpty()){
            //check all of them
            for (EnclosedArea area : areas) {
                //find all opponent active dots that are inside area
                Set<Point> opponentActiveDotsInsideArea = getOpponentDotsInsideArea(opponentsActiveDots, area);
                //there are some dots inside the area
                if(!opponentActiveDotsInsideArea.isEmpty()){
                    //add new lines
                    playerLines.addAll(area.getLines());
                    //add captured dots
                    opponentCapturedDots.addAll(opponentActiveDotsInsideArea);
                    //remove captured dots from set of active dots
                    opponentsActiveDots.removeAll(opponentActiveDotsInsideArea);
                    //find if there are players dots that can be released
                    Set<Point> playerCapturedDotsInsideArea = getOpponentDotsInsideArea(playerCapturedDots,area);
                    // remove them from players dots that captured by opponent
                    playerCapturedDots.removeAll(playerCapturedDotsInsideArea);
                    // add them to set of released dots
                    playerReleasedDots.addAll(playerCapturedDotsInsideArea);
                    //mark empty cells inside the area as occupied
                    fillEmptyCells(area.getPointsInsideEnclosedArea());
                }
            }

        }else{
            //check if player dot is surrounded by opponents active dots
            EnclosedArea enclosedArea = WaveAlgorithm.getEnclosedArea(opponentsActiveDots, dot.x, dot.y,
                    emptyCells.length, emptyCells[0].length);
            //player dot is surrounded by opp dots, so make same process like before
            if(enclosedArea!=null){
                opponentLines.addAll(enclosedArea.getLines());
                playerActiveDots.remove(dot);
                playerCapturedDots.add(dot);
                fillEmptyCells(enclosedArea.getPointsInsideEnclosedArea());
            }

        }
        return true;
    }

    /**
     * Calls method for calculating drawable object for all dots and lines that are inside the rectangle
     * @param rectF - rectangle
     * @param scaleFactor - scale coordinates of all objects by factor scaleFactor
     * @param densityDpCM - number of pixels in one cm
     * @return list of Drawable objects
     */
    public List<Drawable> getDrawable(RectF rectF, float scaleFactor, float densityDpCM){
        List<Drawable> result = new LinkedList<>();
        result.addAll(getLinesInsideRectangle(rectF,scaleFactor,densityDpCM,Color.LTGRAY));
        result.addAll(getDotsInsideRectangle(rectF,scaleFactor,densityDpCM,Color.BLUE));
        result.addAll(getDotsInsideRectangle(rectF,scaleFactor,densityDpCM,Color.RED));
        result.addAll(getLinesInsideRectangle(rectF,scaleFactor,densityDpCM,Color.RED));
        result.addAll(getLinesInsideRectangle(rectF,scaleFactor,densityDpCM,Color.BLUE));

        return result;
    }

    public int getAmountOfCapturedBlueDots(){
        return capturedBlueDots.size();
    }


    public int getAmountOfCapturedRedDots(){
        return capturedRedDots.size();
    }

    /**
     * Checks if cell is occupied, cells that are on edge of game field are occupied by default
     * @param x - coordinate in cell units
     * @param y - coordinate in cell units
     * @return true - if cell is occupied, false otherwise
     */
    public boolean isOccupied(int x, int y){
        if(x <= 0 || y <= 0 || x >= emptyCells.length-1 || y >= emptyCells[0].length-1)
            return true;
        return emptyCells[x][y];
    }


    /**
     * Calculates number of free cells
     * @return number of free cells
     */
    public int freeSpace(){
        int result = 0;
        for (int i = 1; i < emptyCells.length - 1; i++) {
            for (int j = 1; j < emptyCells[0].length-1; j++) {
                if(emptyCells[i][j] != OCCUPIED) result++;
            }
        }
        return result;
    }

    /**
     * Finds all free cells
     * @return list of free cells
     */
    public List<Point> getFreeSpacePoints(){
        List<Point> result = new LinkedList<>();
        for (int i = 1; i < emptyCells.length - 1; i++) {
            for (int j = 1; j < emptyCells[0].length-1; j++) {
                if(emptyCells[i][j] != OCCUPIED) result.add(new Point(i,j));
            }
        }
        return result;
    }

    /**
     * Creates 8 dots, 4 blue and 4 red in the middle of the game field
     */
    private void createStartedDots(){
        int midX = emptyCells.length/2;
        int midY = emptyCells[0].length/2;
        activeBlueDots.add(new Point(midX,midY));
        activeBlueDots.add(new Point(midX+1,midY));
        activeBlueDots.add(new Point(midX+2,midY+1));
        activeBlueDots.add(new Point(midX-1,midY+1));
        activeRedDots.add(new Point(midX,midY+1));
        activeRedDots.add(new Point(midX+1,midY+1));
        activeRedDots.add(new Point(midX+2,midY));
        activeRedDots.add(new Point(midX-1,midY));
        fillEmptyCells(activeBlueDots);
        fillEmptyCells(activeRedDots);
    }

    /**
     * Finds dots that are inside enclosed area
     * @param opponentDots - dots to check
     * @param area
     * @return set of dots that are inside the area
     */
    private Set<Point> getOpponentDotsInsideArea(Set<Point> opponentDots, EnclosedArea area){
        Set<Point> result = new HashSet<>();
        for (Point point : opponentDots) {
            if(area.isDotInside(point))
                result.add(point);
        }
        return  result;
    }

    /**
     * Search intersection of lines this rectangle
     * @param rectF - rectangle
     * @param scaleFactor
     * @param densityDpCM
     * @param color - blue, light gray or red
     * @return list of drawable lines, blue lines, gray(default) lines and red lines if they exist
     */
    private List<Drawable> getLinesInsideRectangle(RectF rectF, float scaleFactor, float densityDpCM, int color){
        List<Drawable> result = new LinkedList<>();

        switch (color){
            case Color.BLUE:
                BlueLine.setScaledThickness(scaleFactor*densityDpCM);
                for (Line line :
                        blueLines) {
                    Line intersection = line.intersection(rectF);

                    if(intersection!=null){

                        result.add(new BlueLine(intersection.offset(-rectF.left,-rectF.top).scale(scaleFactor*densityDpCM)));
                    }
                }
                break;

            case Color.LTGRAY:
                GrayLine.setScaledThickness(scaleFactor*densityDpCM);

                for (Line line :
                        defaultLines) {

                    Line intersection = line.intersection(rectF);
                    if(intersection!=null){

                        result.add(new GrayLine(intersection.offset(-rectF.left,-rectF.top).scale(scaleFactor*densityDpCM)));
                    }

                }
                break;

            case Color.RED:
                RedLine.setScaledThickness(scaleFactor*densityDpCM);
                for (Line line :
                        redLines) {
                    Line intersection = line.intersection(rectF);
                    if(intersection!=null){
                        result.add(new RedLine(intersection.offset(-rectF.left,-rectF.top).scale(scaleFactor*densityDpCM)));
                    }
                }
                break;
        }
        return result;
    }


    /**
     * Search dots inside rectangle
     * @param rectF - rectangle
     * @param scaleFactor - factor for scaling coordinates
     * @param densityDpCM
     * @param color - blue or red
     * @return
     */
    private List<Drawable> getDotsInsideRectangle(RectF  rectF, float scaleFactor, float densityDpCM, int color){
        List<Drawable> result = new LinkedList<>();
        float scaledX, scaledY,scaledRadius;
        Rect rect = translate(rectF);
        List<Point> list = new LinkedList<>();
        if(color == Color.RED){
            list.addAll(activeRedDots);
            list.addAll(capturedRedDots);
            list.addAll(releasedRedDots);
            for (Point point : list) {
                if(insideRectangle(rect,point)){
                    //transform coordinates to coordinate system of the screen
                    scaledX = (0.5f*point.x - rectF.left)*scaleFactor*densityDpCM;
                    scaledY = (0.5f*point.y - rectF.top)*scaleFactor*densityDpCM;
                    scaledRadius = Dot.RADIUS*scaleFactor*densityDpCM;
                    result.add(new RedDot(scaledX,scaledY,scaledRadius));
                }
            }
        }else if(color == Color.BLUE){
            list.addAll(activeBlueDots);
            list.addAll(capturedBlueDots);
            list.addAll(releasedBlueDots);
            for (Point point : list) {
                if(insideRectangle(rect,point)){
                    //transform coordinates to coordinate system of the screen
                    scaledX = (0.5f*point.x - rectF.left)*scaleFactor*densityDpCM;
                    scaledY = (0.5f*point.y - rectF.top)*scaleFactor*densityDpCM;
                    scaledRadius = Dot.RADIUS*scaleFactor*densityDpCM;
                    result.add(new BlueDot(scaledX,scaledY,scaledRadius));
                }
            }
        }
        return result;
    }

    /**
     * Translate rectangle from cell units to cm units
     * @param rectF
     * @return
     */
    private Rect translate(RectF rectF){
        int left = (int)(rectF.left/0.5);
        int right =(int)(rectF.right/0.5);
        int bottom = (int)(rectF.bottom/0.5);
        int top = (int)(rectF.top/0.5);
        return new Rect(left,top,right,bottom);
    }


    private boolean insideRectangle(Rect rect, Point point){
        return (point.x>=rect.left && point.x<=rect.right && point.y>=rect.top && point.y<=rect.bottom);
    }

   

    private void fillEmptyCells(Set<Point> capturedCells){
        for (Point point :
                capturedCells) {
            emptyCells[point.x][point.y] = OCCUPIED;
        }
    }
}
