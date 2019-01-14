package com.example.dots.model;

import android.graphics.RectF;

/**
 * Represents line between two points
 */

public class Line {
    public float startX;
    public float startY;
    public float stopX;
    public float stopY;


    public Line(float startX, float startY, float stopX, float stopY) {
        this.startX = startX;
        this.startY = startY;
        this.stopX = stopX;
        this.stopY = stopY;
    }


    /**
     * Calculates intersection between line and rectangle. Uses Liang–Barsky algorithm.
     * @param rectF - rectangle
     * @return Line if there is intersection, null if line is outside the rectangle
     */
    public Line intersection (RectF rectF){
        float u1,u2,startXClipped,startYClipped,stopXClipped,stopYClipped;
        u1 = -Float.MAX_VALUE;
        u2 = Float.MAX_VALUE;
        float p1 = -(stopX - startX);
        float p2 = -p1;
        float p3 = -(stopY - startY);
        float p4 = -p3;

        float q1 = startX - rectF.left;
        float q2 = rectF.right - startX;
        float q3 = startY - rectF.top;
        float q4 = rectF.bottom - startY;
        float p[] = {p1, p2, p3, p4};
        float q[] = {q1, q2, q3, q4};


        for(int i=0;i<4;i++){
            if(p[i] == 0){
                if(q[i]<0) return null;
            }else{
                float t = q[i]/p[i];
                if(p[i]<0 && u1<t)
                    u1 = t;
                else if(p[i]>0 && u2>t)
                    u2 = t;
            }
        }

        // Line is inside the rectangle
        if(u1<=0 && u2>1) return new Line(startX,startY,stopX,stopY);
        //Line intersect the rectangle in two points
        if(u1>0 && u2>u1 && u2<1){
            startXClipped = startX + u1*p2;
            startYClipped = startY + u1*p4;
            stopXClipped = startX + u2*p2;
            stopYClipped = startY + u2*p4;
            return new Line(startXClipped,startYClipped,stopXClipped,stopYClipped);
        }
        //Line starts outside the rectangle and end inside
        if(u1>0 && u1<1){
            startXClipped = startX + u1*p2;
            startYClipped = startY + u1*p4;
            stopXClipped = stopX;
            stopYClipped = stopY;
            return new Line(startXClipped,startYClipped,stopXClipped,stopYClipped);
        }
        //Line starts inside the rectangle and ends outside
        if(u2>0 && u2<1){
            startXClipped = startX;
            startYClipped = startY;
            stopXClipped = startX + u2*p2;
            stopYClipped = startY + u2*p4;
            return new Line(startXClipped,startYClipped,stopXClipped,stopYClipped);
        }
        //Line is outside th rectangle
        if (u1 > u2 || u1 > 1 || u1 < 0) {
            return null;
        }
        return null;
    }

    /**
     * Scales Line
     * @param scaleFactor - scale line by scaleFactor times
     * @return
     */
    public Line scale (float scaleFactor) {
        startX*=scaleFactor;
        startY*=scaleFactor;
        stopX*=scaleFactor;
        stopY*=scaleFactor;
        return this;
    }

    /**
     * Moves line.
     * @param dx - offset for x axis
     * @param dy - offset for y axis
     * @return
     */
    public Line offset (float dx, float dy){
        startX+=dx;
        startY+=dy;
        stopX+=dx;
        stopY+=dy;
        return this;
    }

    @Override
    public String toString() {
        return "Line{" +
                "startX=" + startX +
                ", startY=" + startY +
                ", stopX=" + stopX +
                ", stopY=" + stopY +
                '}';
    }
}
