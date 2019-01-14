package com.example.dots.drawableObjects;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.example.dots.interfaces.Drawable;

/**
 * Represents score of two players
 */

public class ScoreTable implements Drawable{

    private int blueScore;
    private int redScore;
    private float textSize;



    public ScoreTable() {
        this.blueScore = 0;
        this.redScore = 0;
    }


    public void setTextSize(float textSize) {
        this.textSize = textSize;
    }

    /**
     * Calculates position of text and draw on canvas
     * @param canvas - canvas to draw on
     */
    @Override
    public void draw(Canvas canvas) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(textSize);
        float firstWidth  = paint.measureText(""+blueScore);
        float secondWidth = paint.measureText(":");
        paint.setColor(Color.BLUE);
        canvas.drawText(""+blueScore,0,textSize,paint);
        paint.setColor(Color.BLACK);
        canvas.drawText(":",firstWidth,textSize,paint);
        paint.setColor(Color.RED);
        canvas.drawText(""+redScore,firstWidth+secondWidth,textSize,paint);
    }

    public void setBlueScore(int blueScore) {
        this.blueScore = blueScore;
    }

    public void setRedScore(int redScore) {
        this.redScore = redScore;
    }


    public int getBlueScore() {
        return blueScore;
    }

    public int getRedScore() {
        return redScore;
    }
}
