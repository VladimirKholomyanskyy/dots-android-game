package com.example.dots.drawableObjects;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import com.example.dots.model.Dot;


public class BlueDot extends Dot {
    private static final Paint BLUE = new Paint();

    public BlueDot(float x, float y, float radius) {
        super(x, y, radius);
        BLUE.setColor(Color.BLUE);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawCircle(x,y, scaledRadius,BLUE);
    }
}
