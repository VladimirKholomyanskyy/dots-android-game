package com.example.dots.model;

import android.graphics.Point;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Enclosed area is list of points that form a polygon. Distance between two close points is ONE cell.
 */

public class EnclosedArea {
    private List<Point> polygon;
    private Set<Point> pointsInsideEnclosedArea;//All the points that can be placed inside area
    private static final float DISTANCE_BETWEEN_DOTS = 0.5f;// length of edge of the cell


    public EnclosedArea(List<Point> cycle, Set<Point> pointsInsideEnclosedArea) {
        this.pointsInsideEnclosedArea = new HashSet<>(pointsInsideEnclosedArea);
        this.polygon = new ArrayList<>(cycle);

    }

    /**
     * Checks if point inside enclosed area
     * @param dot - point to check
     * @return true - if point is inside the area, false otherwise
     */
    public boolean isDotInside(Point dot){
       if(pointsInsideEnclosedArea.contains(dot))
           return true;
       return false;
    }

    /**
     * Builds lines from list of points to form a polygon
     * @return set of lines
     */
    public Set<Line> getLines(){
        Set<Line> result = new HashSet<>();

        Iterator<Point> pointIterator = polygon.iterator();
        Point start = pointIterator.next();
        Point stop;
        while (pointIterator.hasNext()){
            stop = pointIterator.next();
            Line line = new Line(start.x,start.y,stop.x,stop.y);
            line.scale(DISTANCE_BETWEEN_DOTS);
            result.add(line);
            start = stop;
        }

        return result;
    }


    public Set<Point> getPointsInsideEnclosedArea() {
        return pointsInsideEnclosedArea;
    }


    @Override
    public String toString() {
        return "EnclosedArea{" +
                "polygon=" + polygon +
                ", pointsInsideEnclosedArea=" + pointsInsideEnclosedArea +
                '}';
    }
}
