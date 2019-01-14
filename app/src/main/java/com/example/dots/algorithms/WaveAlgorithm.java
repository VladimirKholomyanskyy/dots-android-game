package com.example.dots.algorithms;

import android.graphics.Point;
import com.example.dots.model.EnclosedArea;
import com.example.dots.model.Node;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Wave algorithm or Lee algorithm
 */

public class WaveAlgorithm {

    /**
     * Calls method getEnclosed area from 4 points that close to source (North, South,West, East)
     * @param dots - active dots
     * @param startX - x coordinate of source
     * @param startY - y coordinate of source
     * @param gameFieldWidth
     * @param gameFieldHeight
     * @return list of enclosed areas if the exist, or empty list if they don't
     */
    public static List<EnclosedArea> getDotsOfEnclosedAreas(Set<Point> dots, int startX, int startY,
                                                            int gameFieldWidth, int gameFieldHeight) {

        List<EnclosedArea> result = new LinkedList<>();
        int dx[] = {1, 0, -1, 0};
        int dy[] = {0, 1, 0, -1};
        //Call getEnclosedArea from 4 places
        for (int v = 0; v < dx.length; v++) {
            int w = startX + dx[v];
            int h = startY + dy[v];
            EnclosedArea area = getEnclosedArea(dots, w, h, gameFieldWidth, gameFieldHeight);
            if (area != null)
                result.add(area);


        }
        return result;
    }

    /**
     * Searches enclosed areas using wave algorithm. Wave starts in (startX,startY)
     * Wave can go in 4 directions North, South, East, West
     * @param dots - active dots
     * @param startX - x coordinate of source
     * @param startY - y coordinate of source
     * @param gameFieldWidth
     * @param gameFieldHeight
     * @return enclosed area if exist, or null if don't
     */
    public static EnclosedArea getEnclosedArea(Set<Point> dots, int startX, int startY,
                                        int gameFieldWidth, int gameFieldHeight){

        int stopX = gameFieldWidth - 1;
        int stopY = gameFieldHeight - 1;
        int dx[] = {1, 0, -1, 0};
        int dy[] = {0, 1, 0, -1};
        short d;
        boolean stop;
        Set<Point> cycle = new HashSet<>();
        Node field[][] = WaveAlgorithm.buildArray(dots,gameFieldWidth, gameFieldHeight);
        EnclosedArea result = null;

        if(startX>=0 && startX<gameFieldWidth && startY>=0 && startY<gameFieldHeight && field[startX][startY].distance!=Node.IMPASSABLE){
            field[startX][startY].distance = 0;
            d = 0;
            do {
                stop = true;
                for (int y = 0; y < gameFieldHeight; y++) {
                    for (int x = 0; x < gameFieldWidth; x++) {
                        if(field[x][y].distance == d){
                            for (int k = 0; k < dx.length; k++) {
                                int ix = x+dx[k];
                                int iy = y+dy[k];

                                if(ix>=0 && ix<gameFieldWidth && iy>=0 && iy<gameFieldHeight){
                                    if(field[ix][iy].distance == Node.IMPASSABLE){
                                        Point next = new Point(ix,iy);
                                        cycle.add(next);

                                    }else if(field[ix][iy].distance == Node.PASSABLE){
                                        stop = false;
                                        field[ix][iy].distance = (short)(d+1);
                                    }
                                }
                            }
                        }
                    }
                }
                d++;
            }while(!stop && field[stopX][stopY].distance == Node.PASSABLE);
            //Wave haven't reached edge of the game field. This means there is enclosed area
            if(field[stopX][stopY].distance == Node.PASSABLE){
                List<Point> points = processCycle(cycle);
                Set<Point> capturedPoint = getCapturedPoint(field);
                result = new EnclosedArea(points,capturedPoint);

            }
        }
        return result;
    }

    /**
     * Builds field that can be used by Wave algorithm
     * @param dots
     * @param width
     * @param height
     * @return
     */
    private static Node[][] buildArray(Set<Point> dots, int width, int height){
        Node nodes[][] = new Node[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                nodes[i][j] = new Node();
            }
        }
        for (Point dot :
                dots) {
            nodes[dot.x][dot.y].distance = Node.IMPASSABLE;
        }
        return nodes;
    }

    /**
     * Searches for cells that are inside an enclosed area
     * @param field
     * @return set of captured points
     */
    private static Set<Point> getCapturedPoint(Node[][] field){
        Set<Point> result = new HashSet<>();
        for (int i = 0; i < field.length; i++) {
            for (int j = 0; j < field[0].length; j++) {
                if(field[i][j].distance < Node.PASSABLE){
                    result.add(new Point(i,j));
                }
            }
        }
        return result;
    }


    /**
     * Builds ordered list of points of enclosed area. Distance between to points in indexes i and i+1 is 1 cell
     * @param cycle - enclosed area, points are not ordered
     * @return
     */
    private static List<Point> processCycle(Set<Point> cycle){
        LinkedList<Point> processedCycle = new LinkedList<>();

        Point points[] = new Point[cycle.size()];
        cycle.toArray(points);
        Point nextPoint = new Point(points[0].x, points[0].y);
        processedCycle.add(nextPoint);
        cycle.remove(nextPoint);
        boolean process = true;
        while (process){
            process = false;
            nextPoint = processedCycle.getLast();
            for (Point point : cycle) {
                if(isClose(point,nextPoint)){
                    processedCycle.add(point);
                    cycle.remove(point);
                    process = true;
                    break;
                }
            }

        }
        processedCycle.add(processedCycle.getFirst());
        return processedCycle;
    }


    /**
     * Checks if distance between two points is 1 cell
     * @param p1 - first point
     * @param p2 - second points
     * @return true - if distance is one cel, false otherwise
     */
    private static boolean isClose(Point p1, Point p2){
        if(p1.equals(p2))
            return false;

        double distance = Math.sqrt((p1.x-p2.x)*(p1.x-p2.x) + (p1.y-p2.y)*(p1.y-p2.y));
        if(distance<2) return true;
        return false;
    }


}
