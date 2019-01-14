package com.example.dots.model;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;

import com.example.dots.drawableObjects.ScoreTable;
import com.example.dots.interfaces.Drawable;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Game between two players. Game has two states: Running - normal state, Stopped - when one player
 * has left the game and the second has 2 minutes two finish the game, he must capture maximum opponents
 * dots before time ends.
 */

public class Game {
    private Player bluePlayer;
    private Player redPlayer;
    private Player nextTurn;
    private ScoreTable scoreTable;
    private GameField gameField;
    private String gameLink;
    private int gameState;
    public static final int GAME_FIELD_WIDTH = 15;//cm
    public static final int GAME_FIELD_HEIGHT = 12;//cm
    public static final int GAME_IS_RUNNING = 1;
    public static final int GAME_IS_STOPPED = 2;
    public static final int TURN_TIME = 15000; // time to make a turn
    public static final int FINISH_GAME_TIME = 120000;// 2 min, time to finish game one player has left
    private static final Object lock = new Object();
    private Point lastTurn;
    private Paint lastTurnPaint;// depends who had the last turn




    public Game(Player bluePlayer, Player redPlayer,Player nextTurn, String gameLink) {
        this.bluePlayer = bluePlayer;
        this.redPlayer = redPlayer;
        this.nextTurn = nextTurn;
        gameState = GAME_IS_RUNNING;
        int start = gameLink.indexOf('<');
        int end = gameLink.indexOf('>');
        //extracts game url from server link
        this.gameLink = gameLink.substring(start+1,end).concat("/turns");
        scoreTable = new ScoreTable();
        gameField = new GameField(GAME_FIELD_WIDTH,GAME_FIELD_HEIGHT);
        scoreTable.setRedScore(gameField.getAmountOfCapturedBlueDots());
        scoreTable.setBlueScore(gameField.getAmountOfCapturedRedDots());
        lastTurnPaint = new Paint();
        lastTurnPaint.setStyle(Paint.Style.STROKE );


    }

    /**
     * Make a turn and change next turn to the second player
     * @param x - coordinate in cell units
     * @param y - coordinate in cell units
     * @return - true - if succeed, false otherwise
     */
    public boolean makeTurn(int x, int y){
        //Locks to apply changes to game field
        synchronized (lock) {
            lastTurn = new Point(x, y);
            boolean result;
            if (nextTurn.equals(bluePlayer)) {
                result = gameField.setDot(lastTurn, Color.BLUE);
                lastTurnPaint.setColor(Color.BLUE);
                if (gameState==GAME_IS_RUNNING)
                    nextTurn = redPlayer;
            } else {
                result = gameField.setDot(lastTurn, Color.RED);
                lastTurnPaint.setColor(Color.RED);
                if (gameState==GAME_IS_RUNNING)
                    nextTurn = bluePlayer;
            }
            scoreTable.setBlueScore(gameField.getAmountOfCapturedRedDots());
            scoreTable.setRedScore(gameField.getAmountOfCapturedBlueDots());
            return result;
        }

    }


    /**
     * Makes Random turn - sets dot in random places
     * @return
     */
    public Point makeRandomTurn(){
        //gets all free positions
        List<Point> freeSpacePoints = gameField.getFreeSpacePoints();
        Random random = new Random();
        int index = random.nextInt(freeSpacePoints.size());
        Point turn = freeSpacePoints.get(index);
        makeTurn(turn.x,turn.y);
        return turn;
    }

    /**
     * Checks if players turn
     * @param player - player to check
     * @return true - if next turn is player, false otherwise
     */
    public boolean isPlayerTurn(Player player){
        return nextTurn.equals(player);
    }


    public void switchTurn(){
        if(nextTurn.equals(redPlayer))
            nextTurn = bluePlayer;
        else
            nextTurn = redPlayer;
    }

    /**
     * Checks if there is no more free space
     * @return true - if there is no more space, false otherwise
     */
    public boolean isNoMoreFreeSpace(){
        if(gameField.freeSpace() == 0) return true;
        return false;
    }

    /**
     * Gets all drawable objects that are inside the rectangle by calling getDrawable from GameField class,
     * and also adds circle around dot that was placed last.
     * @param rectF - rectangle
     * @param scaleFactor - by how many times objects must be scaled
     * @param densityDpCM - number of pixels in 1 cm
     * @return - list of drawable objects
     */
    public List<Drawable> getDrawable(final RectF rectF, final float scaleFactor, final float densityDpCM){
        synchronized (lock) {
            List<Drawable> result = gameField.getDrawable(rectF, scaleFactor, densityDpCM);
            if (lastTurn != null) {
                //translate cell coordinates to coordinates in cm
                final PointF point = new PointF(lastTurn.x * 0.5f, lastTurn.y * 0.5f);
                //check if last dot is inside the rectangle
                if (point.x >= rectF.left && point.x <= rectF.right && point.y >= rectF.top && point.y <= rectF.bottom) {
                    Drawable lastTurnCircle = new Drawable() {
                        @Override
                        public void draw(Canvas canvas) {
                            //transform actual coordinates of point to coordinates on screen of the device
                            point.x = (point.x - rectF.left) * scaleFactor * densityDpCM;
                            point.y = (point.y - rectF.top) * scaleFactor * densityDpCM;
                            float radius = (Dot.RADIUS + 0.03f) * scaleFactor * densityDpCM;
                            lastTurnPaint.setStrokeWidth(scaleFactor * densityDpCM * 0.01f);
                            canvas.drawCircle(point.x, point.y, radius, lastTurnPaint);
                        }
                    };
                    result.add(lastTurnCircle);
                }
            }
            return result;
        }
    }


    public String getGameLink() {
        return gameLink;
    }

    public ScoreTable getScoreTable(){
        return scoreTable;
    }



    public boolean isTurnCanBeMade(Player player, int xCell, int yCell){
        return isPlayerTurn(player) && !gameField.isOccupied(xCell, yCell);
    }

    public int getGameState() {
        return gameState;
    }


    public void setGameState(int gameState) {
        this.gameState = gameState;
    }

    /**
     * Builds results of two players
     * @return results of two players, list[0] - player name, list[1] - game result,
     * list[2] - player score, list[3] - opp score
     */
    public Map<String,List<String>> getResults(){
        synchronized (lock) {
            Map<String, List<String>> results = new HashMap<>();
            List<String> bluePlayerResult = new LinkedList<>();
            List<String> redPlayerResult = new LinkedList<>();
            bluePlayerResult.add(bluePlayer.getName());
            redPlayerResult.add(redPlayer.getName());
            int redPlayerScore = scoreTable.getRedScore();
            int bluePlayerScore = scoreTable.getBlueScore();
            if (redPlayerScore > bluePlayerScore) {
                bluePlayerResult.add("LOSS");
                redPlayerResult.add("WIN");
            } else if (bluePlayerScore == redPlayerScore) {
                bluePlayerResult.add("TIE");
                redPlayerResult.add("TIE");
            } else {
                bluePlayerResult.add("WIN");
                redPlayerResult.add("LOSS");
            }
            bluePlayerResult.add("" + bluePlayerScore);
            bluePlayerResult.add("" + redPlayerScore);
            redPlayerResult.add("" + redPlayerScore);
            redPlayerResult.add("" + bluePlayerScore);
            results.put("bluePlayer", bluePlayerResult);
            results.put("redPlayer", redPlayerResult);
            return results;
        }
    }


}
