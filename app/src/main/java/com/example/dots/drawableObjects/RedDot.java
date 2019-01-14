package com.example.dots.drawableObjects;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import com.example.dots.model.Dot;

public class RedDot extends Dot {
    private static final Paint RED = new Paint();


    public RedDot(float x, float y, float radius){
        super(x,y,radius);
        RED.setColor(Color.RED);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawCircle(x,y, scaledRadius,RED);
    }
}
