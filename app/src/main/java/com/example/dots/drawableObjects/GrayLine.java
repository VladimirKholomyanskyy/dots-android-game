package com.example.dots.drawableObjects;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import com.example.dots.interfaces.Drawable;
import com.example.dots.model.Line;

public class GrayLine extends Line implements Drawable {
    private static float scaledThickness;
    private final static Paint GRAY = new Paint();
    private static final float THICKNESS = 0.025f;//cm



    public GrayLine(Line line){
        super(line.startX,line.startY,line.stopX,line.stopY);
        GRAY.setColor(Color.LTGRAY);
    }

    /**
     * Sets stroke width for all gray lines
     * @param scaleFactor
     */
    public static void setScaledThickness(float scaleFactor){
        GrayLine.scaledThickness = scaleFactor*THICKNESS;
        GRAY.setStrokeWidth(scaledThickness);
    }


    @Override
    public void draw(Canvas canvas) {
        canvas.drawLine(startX,startY,stopX,stopY,GRAY);
    }
}
