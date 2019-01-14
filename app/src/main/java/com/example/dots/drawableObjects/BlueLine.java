package com.example.dots.drawableObjects;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import com.example.dots.interfaces.Drawable;
import com.example.dots.model.Line;

public class BlueLine extends Line implements Drawable {
    private static float scaledThickness;
    private final static Paint BLUE = new Paint();
    private static final float THICKNESS = 0.1f;//cm





    public BlueLine(Line line){
        super(line.startX,line.startY,line.stopX,line.stopY);
    }

    /**
     * Sets stroke width for all blue lines
     * @param scaleFactor
     */
    public static void setScaledThickness(float scaleFactor){
        BlueLine.scaledThickness = scaleFactor*THICKNESS;
        BLUE.setStrokeWidth(scaledThickness);
        BLUE.setColor(Color.BLUE);
    }



    @Override
    public void draw(Canvas canvas) {
        canvas.drawLine(startX,startY,stopX,stopY,BLUE);
    }
}
