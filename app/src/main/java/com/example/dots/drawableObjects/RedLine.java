package com.example.dots.drawableObjects;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.example.dots.interfaces.Drawable;
import com.example.dots.model.Line;

public class RedLine extends Line implements Drawable {
    private static float scaledThickness;
    private final static Paint RED = new Paint();
    private static final float THICKNESS = 0.1f;//cm



    public RedLine(Line line){
        super(line.startX,line.startY,line.stopX,line.stopY);
    }

    /**
     * Sets stroke width for all red lines
     * @param scaleFactor
     */
    public static void setScaledThickness(float scaleFactor){
        RedLine.scaledThickness = scaleFactor*THICKNESS;
        RED.setColor(Color.RED);
        RED.setStrokeWidth(scaledThickness);
    }


    @Override
    public void draw(Canvas canvas) {
        canvas.drawLine(startX,startY,stopX,stopY,RED);
    }
}
