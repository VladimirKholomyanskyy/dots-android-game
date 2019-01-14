package com.example.dots.model;

import android.graphics.Canvas;
import android.graphics.PointF;

import com.example.dots.interfaces.Drawable;

/**
 * Dot is a circle.
 */

public abstract class Dot extends PointF implements Drawable{
    protected static float scaledRadius;// radius in pixels
    public static final float RADIUS = 0.1f;//default radius in cm, without scaling

    public Dot(float x, float y, float scaledRadius){
        super(x,y);
        Dot.scaledRadius = scaledRadius;
    }



    @Override
    public String toString() {
        return "Dot{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }

    /**
     * Compare two dots
     * @param o - dot to compare
     * @return true if two dots are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Dot dot = (Dot) o;

        if (Float.compare(dot.x, x) != 0) return false;
        return Float.compare(dot.y, y) == 0;
    }


    @Override
    public int hashCode() {
        int result = (x != +0.0f ? Float.floatToIntBits(x) : 0);
        result = 31 * result + (y != +0.0f ? Float.floatToIntBits(y) : 0);
        return result;
    }


    @Override
    public abstract void draw(Canvas canvas);


}
