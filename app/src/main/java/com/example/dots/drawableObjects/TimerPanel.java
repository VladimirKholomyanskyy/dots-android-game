package com.example.dots.drawableObjects;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import com.example.dots.interfaces.Drawable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents time that are left for turn or to finish a game than one player has left
 */
public class TimerPanel implements Drawable {
    private AtomicInteger time;
    private PointF centerF;
    private int startTime;
    private float textSize;
    private boolean visible = false;

    public TimerPanel(PointF centerF, int startTime, float textSize){
        this.centerF = centerF;
        this.startTime = startTime;
        this.textSize = textSize;
        time = new AtomicInteger(startTime);
    }

    public int decrement(){
        return time.getAndDecrement();
    }

    public void reset(){
        time = new AtomicInteger(startTime);
    }

    public void show() {
        this.visible = true;
    }

    public void hide(){
        this.visible = false;
    }

    public int getTime(){
        return time.get();
    }

    public boolean isVisible(){
        return visible;
    }

    /**
     * Calculates position of text end draws it
     * @param canvas
     */
    @Override
    public void draw(Canvas canvas) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(textSize);
        paint.setColor(Color.BLACK);
        float textWidth = paint.measureText("" + time.get());
        canvas.drawText(""+time.get(),centerF.x-textWidth/2,centerF.y,paint);
    }
}
