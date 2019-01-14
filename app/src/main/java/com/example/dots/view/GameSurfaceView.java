package com.example.dots.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.dots.R;
import com.example.dots.drawableObjects.TimerPanel;
import com.example.dots.interfaces.Drawable;
import com.example.dots.model.Game;
import com.example.dots.model.GameData;
import com.example.dots.model.Player;
import com.example.dots.urls.MyURLs;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Class that draws game field on the screen, "talks" with game server, listen to user touch events
 * Runs on his own thread
 */

public class GameSurfaceView extends SurfaceView implements Runnable {
    private Thread thread = null;
    private SurfaceHolder surfaceHolder;
    private volatile boolean running = false;
    private float scaleFactor;
    private PointF previous;//previous position of touch
    private PointF moveTo; // started point for visible part of game field
    private PointF max; // maximum point for started point for visible part of th game field
    private int screenWidth;
    private int screenHeight;
    private int statusCode; // Http status code of response
    private float densityDpCM;// number of pixels in cm
    private static final float INCH = 2.54f;//cm
    private static final int INVALID_POINTER_ID = -1;
    private int mActivePointerId = INVALID_POINTER_ID;
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    private Game game;
    private Player player;
    private  String encodedCredentials;
    private int timeLeftFromTurn;// time that player haven't used for turn
    private boolean delayedPutRequestIsSent = false;
    private RequestQueue requestQueue;
    private RequestQueue requestQueue2;// second queue is used than player want to end game, while he awaits opp turn
    private boolean isChanged = true;// if there is need to redraw game field
    private TimerPanel turnTimerPanel;
    private TimerPanel surrenderTimerPanel;
    private Timer turnTimer;
    private Timer reDrawPanelsTimer;
    private Timer finishGameTimer;
    private Timer delayedPutRequestTimer;
    private static final int NOT_YOUR_TURN = 1;
    private static final int NO_RECEIVER = 2;
    private static final int SUCCESS = 3;
    private static final int OPPONENT_HAS_GONE = 4;



    public GameSurfaceView(Context context, Bundle extras) {
        super(context);
        init(context,extras);
    }




    public void onResume(){
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public void onPause(){
        running = false;
        boolean retry = true;
        while(retry){
            try{
                thread.join();
                retry = false;
            }catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void onStop(){
        requestQueue2.add(createEndGameRequest());
    }

    @Override
    public void run() {
        while(running){
            if(surfaceHolder.getSurface().isValid() && isChanged){
                Paint pint = new Paint();
                pint.setColor(Color.BLACK);
                //calculate what part of the game field to draw
                RectF rectF  = new RectF((moveTo.x/densityDpCM), (moveTo.y/densityDpCM),
                        ((screenWidth/scaleFactor+ moveTo.x)/densityDpCM),
                        ((screenHeight/scaleFactor+ moveTo.y)/densityDpCM));
                List<Drawable> objectsToDraw = game.getDrawable(rectF,scaleFactor,
                    densityDpCM);
                Canvas canvas = surfaceHolder.lockCanvas();
                canvas.drawColor(Color.WHITE);

                for (Drawable toDraw : objectsToDraw) {
                    toDraw.draw(canvas);
                }

                game.getScoreTable().draw(canvas);
                if(turnTimerPanel.isVisible()) turnTimerPanel.draw(canvas);
                if (surrenderTimerPanel.isVisible()) surrenderTimerPanel.draw(canvas);

                surfaceHolder.unlockCanvasAndPost(canvas);
                isChanged = false;

            }

        }
    }



    @Override
    public boolean onTouchEvent(MotionEvent event) {
        isChanged = true;
        scaleGestureDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        int action = event.getAction();
        float x,y;
        switch (action & MotionEvent.ACTION_MASK){
            case MotionEvent.ACTION_DOWN:
                x = event.getX();
                y = event.getY();

                previous.set(x,y);

                mActivePointerId  = event.getPointerId(0);
                break;
            case MotionEvent.ACTION_MOVE:{
                int pointerIndex = event.findPointerIndex(mActivePointerId);
                x = event.getX(pointerIndex);
                y = event.getY(pointerIndex);


                float dx = x-previous.x;
                float dy = y-previous.y;
                moveTo.offset(-dx,-dy);
                moveTo.x = Math.max(0,Math.min(moveTo.x,max.x));
                moveTo.y = Math.max(0,Math.min(moveTo.y,max.y));
                previous.set(x,y);
                break;}
            case MotionEvent.ACTION_UP:
                mActivePointerId = INVALID_POINTER_ID;
                break;
            case MotionEvent.ACTION_CANCEL:
                mActivePointerId = INVALID_POINTER_ID;
                break;

            case MotionEvent.ACTION_POINTER_UP: {
                // Extract the index of the pointer that left the touch sensor
                final int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK)
                        >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int pointerId = event.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    previous.set(event.getX(newPointerIndex),event.getY(newPointerIndex));
                    mActivePointerId = event.getPointerId(newPointerIndex);
                }
                break;
            }

        }
        return true;
    }


    /**
     * Calculates scale factor from scale gesture
     */
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener{
        @Override
        public boolean onScale(ScaleGestureDetector detector){
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 5.0f));
            max.set(Game.GAME_FIELD_WIDTH*densityDpCM - screenWidth/scaleFactor,
                    Game.GAME_FIELD_HEIGHT*densityDpCM - screenHeight/scaleFactor);

            return true;

        }
    }

    /**
     * Translate player single tap gesture to position on game field
     */
    private class GestureTap extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            //apply scale factor
            float x = e.getX()/scaleFactor;
            float y = e.getY()/scaleFactor;
            //half of edge length of cell
            float mid = 0.5f*densityDpCM/2;
            //find hom many full cells fit into coordinates
            int cellCountX = (int)((x+ moveTo.x)/(0.5f*densityDpCM));
            int cellCountY = (int)((y+ moveTo.y)/(0.5f*densityDpCM));
            //find reminder
            float remainderX = (x+moveTo.x)%(0.5f*densityDpCM);
            float remainderY = (y+moveTo.y)%(0.5f*densityDpCM);
            //check to what edge touch is closed to
            int addToX = (remainderX>=mid)? 1:0;
            int addToY = (remainderY>=mid)? 1:0;
            int cellX = cellCountX + addToX;
            int cellY = cellCountY + addToY;

            if(game.isTurnCanBeMade(player,cellX,cellY)){
                game.makeTurn(cellX,cellY);
                if(game.getGameState() == Game.GAME_IS_STOPPED){

                    if(game.isNoMoreFreeSpace()){
                        finishGameTimer.cancel();
                        reDrawPanelsTimer.cancel();
                        endGameAfterOppHasLeft();
                    }
                }else{
                    turnTimer.cancel();
                    reDrawPanelsTimer.cancel();
                    timeLeftFromTurn = turnTimerPanel.getTime();
                    turnTimerPanel.reset();
                    turnTimerPanel.hide();
                    requestQueue.add(createPutRequest(cellX,cellY));
                    if(game.isNoMoreFreeSpace())
                        endGame();
                }
            }
            isChanged = true;
            return true;
        }
    }

    private String createCredentials(String userName, String password){
        String credentials = userName + ":" + password;
        return Base64.encodeToString(credentials.getBytes(),Base64.NO_WRAP);

    }

    /**
     * Create put request for player turn
     * @param x
     * @param y
     * @return JsonObjectRequest
     */
    private JsonObjectRequest createPutRequest(final int x, final int y){

        final GameData gameData = new GameData(x,y);

        JsonObjectRequest putRequest = new JsonObjectRequest(Request.Method.PUT, game.getGameLink(),
                gameData.toJSON(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject  response) {

                        if(statusCode == HttpURLConnection.HTTP_OK){
                            try {
                                int result = response.getInt("result");
                                switch(result){
                                    //Opponent received the turn
                                    case SUCCESS:
                                        if(!game.isNoMoreFreeSpace()){
                                            Toast.makeText(GameSurfaceView.this.getContext(),
                                                    R.string.waitingForOpponentTurn,Toast.LENGTH_SHORT).show();
                                            requestQueue.add(createGetRequest());
                                        }
                                        break;
                                        //player is not waiting for our turn
                                    case NO_RECEIVER:
                                        //try one time more, maybe made your turn to quick
                                        if(!delayedPutRequestIsSent){
                                            delayedPutRequestTimer = new Timer();
                                            delayedPutRequestTimer.schedule(new DelayedPutRequest(x,y),timeLeftFromTurn);
                                            delayedPutRequestIsSent = true;
                                        }else{
                                            //Opponent left
                                            game.setGameState(Game.GAME_IS_STOPPED);
                                            game.switchTurn();
                                            if(!game.isNoMoreFreeSpace()) {
                                                Toast.makeText(GameSurfaceView.this.getContext(),
                                                        R.string.oppLeftMessage, Toast.LENGTH_SHORT).show();
                                                finishGameTimer = new Timer();
                                                finishGameTimer.schedule(new FinishGameTimerTask(),Game.FINISH_GAME_TIME);
                                                reDrawPanelsTimer = new Timer();
                                                reDrawPanelsTimer.scheduleAtFixedRate(new DecrementTimerTask(),
                                                        1000,1000);
                                                surrenderTimerPanel.show();
                                            }
                                        }

                                        break;
                                    case NOT_YOUR_TURN:
                                        requestQueue.add(createGetRequest());
                                        break;
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }


                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                NetworkResponse networkResponse = error.networkResponse;
                //Opponent requested from the game server to delete the game. He left the game
                if(networkResponse != null && networkResponse.statusCode == HttpURLConnection.HTTP_NOT_FOUND){
                    game.setGameState(Game.GAME_IS_STOPPED);
                    game.switchTurn();
                    if(!game.isNoMoreFreeSpace()){
                        invokeOppSurrenderMode();
                    }
                }else{
                    Toast.makeText(GameSurfaceView.this.getContext(),R.string.appError,Toast.LENGTH_SHORT).show();
                    requestQueue.add(createEndGameRequest());
                }
            }
        }){
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response){
                statusCode = response.statusCode;
                try {

                    String jsonString = new String(response.data,
                            HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
                    JSONObject result = null;

                    if (jsonString != null && jsonString.length() > 0)
                        result = new JSONObject(jsonString);

                    return Response.success(result,
                            HttpHeaderParser.parseCacheHeaders(response));
                } catch (UnsupportedEncodingException | JSONException e) {
                    return Response.error(new ParseError(e));
                }


            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public Map<String,String> getHeaders() throws AuthFailureError {
                HashMap<String,String> headers = new HashMap<>();
                headers.put("Authorization","Basic "+encodedCredentials);
               // headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }
        };
        return putRequest;
    }

    /**
     * create get request to get opponent turn
     * @return JsonObjectRequest
     */
    private JsonObjectRequest createGetRequest(){

        JsonObjectRequest getRequest = new JsonObjectRequest(Request.Method.GET, game.getGameLink(),null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject  response) {

                        try {
                            int result = response.getInt("result");
                            if(result == SUCCESS) {
                                JSONObject gameData = response.getJSONObject("gameData");
                                JSONObject turn = gameData.getJSONObject("turn");
                                int x = Integer.parseInt(turn.getString("x"));
                                int y = Integer.parseInt(turn.getString("y"));
                                game.makeTurn(x, y);
                                if (game.isNoMoreFreeSpace()) {
                                    endGame();
                                } else {
                                    //Start turn timer
                                    turnTimer = new Timer();
                                    turnTimer.schedule(new TurnTimerTask(), Game.TURN_TIME);
                                    reDrawPanelsTimer = new Timer();
                                    reDrawPanelsTimer.scheduleAtFixedRate(new DecrementTimerTask(),
                                            1000, 1000);
                                    turnTimerPanel.show();
                                }
                            }else if(result == OPPONENT_HAS_GONE){
                                game.setGameState(Game.GAME_IS_STOPPED);
                                //switch turn to yours
                                game.switchTurn();
                                invokeOppSurrenderMode();
                                }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }){
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response){
                statusCode = response.statusCode;
                return super.parseNetworkResponse(response);
            }

            @Override
            public Map<String,String> getHeaders() throws AuthFailureError {
                HashMap<String,String> headers = new HashMap<>();
                headers.put("Authorization","Basic "+encodedCredentials);
                headers.put("timeout",""+(Game.TURN_TIME/1000 + 2));
                return headers;
            }
        };
        //Fixes bug when volley sends two request instead one
        getRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        return getRequest;
    }

    /**
     * Creates request for updating player stats
     * @param playerName
     * @param gameResult - win, loss, tie
     * @param playerScore
     * @param oppScore
     * @return StringRequest
     */
    private StringRequest createPutStatsRequest(final String playerName,final String gameResult,
                                                    final String playerScore, final String oppScore){


        StringRequest PutStatsRequest = new StringRequest(Request.Method.PUT, MyURLs.PLAYER_STATS_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String  response) {
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }){
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response){
                statusCode = response.statusCode;
                return super.parseNetworkResponse(response);
            }

            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("playerName",playerName);
                params.put("gameResult",gameResult);
                params.put("playerScore",playerScore);
                params.put("oppScore",oppScore);
                return params;
            }

            @Override
            public Map<String,String> getHeaders() throws AuthFailureError {
                HashMap<String,String> headers = new HashMap<>();
                headers.put("Authorization","Basic "+encodedCredentials);

                return headers;
            }
        };
        return PutStatsRequest;
    }

    /**
     * Creates request from deleting running game from the game server
     * @return StringRequest
     */
    private StringRequest createEndGameRequest(){
        if(turnTimer!=null)
            turnTimer.cancel();
        if(finishGameTimer!=null)
            finishGameTimer.cancel();
        if(reDrawPanelsTimer!=null)
            reDrawPanelsTimer.cancel();
        if(delayedPutRequestTimer!=null)
            delayedPutRequestTimer.cancel();


        String url = game.getGameLink().replace("/turns","");
        StringRequest endGameRequest = new StringRequest(Request.Method.DELETE, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Activity activity = (Activity)GameSurfaceView.this.getContext();
                        activity.finish();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Activity activity = (Activity)GameSurfaceView.this.getContext();
                        activity.finish();
                    }

        }){
            @Override
            public Map<String,String> getHeaders() throws AuthFailureError {
                HashMap<String,String> headers = new HashMap<>();
                headers.put("Authorization","Basic "+encodedCredentials);
                return headers;
            }
        };


        return  endGameRequest;
    }

    /**
     * Starts timer for Stopped game state (Opponent has left)
     */
    private void invokeOppSurrenderMode(){
        Toast.makeText(GameSurfaceView.this.getContext(),
                R.string.oppLeftMessage,Toast.LENGTH_SHORT).show();
        finishGameTimer = new Timer();
        finishGameTimer.schedule(new FinishGameTimerTask(),Game.FINISH_GAME_TIME);
        reDrawPanelsTimer = new Timer();
        reDrawPanelsTimer.scheduleAtFixedRate(new DecrementTimerTask(),
                1000,1000);
        surrenderTimerPanel.show();
    }

    /**
     * Updates player stats on server and requests to delete game from the server
     */
    private void endGame(){
        Map<String, List<String>> results = game.getResults();
        List<String> bluePlayer = results.get("bluePlayer");
        List<String> redPlayer = results.get("redPlayer");
        List<String> myResults;
        if(bluePlayer.get(0).equals(player.getName()))
            myResults = bluePlayer;
        else
            myResults = redPlayer;

        requestQueue.add(createPutStatsRequest(myResults.get(0),myResults.get(1),myResults.get(2),
                myResults.get(3)));

        switch (myResults.get(1)){
            case "WIN":
                Toast.makeText(GameSurfaceView.this.getContext(),R.string.winMessage,Toast.LENGTH_SHORT).show();
                break;
            case "LOSS":
                Toast.makeText(GameSurfaceView.this.getContext(),R.string.lossMessage,Toast.LENGTH_SHORT).show();
                break;
            case "TIE":
                Toast.makeText(GameSurfaceView.this.getContext(),R.string.drawMessage,Toast.LENGTH_SHORT).show();
                break;
        }
        requestQueue.add(createEndGameRequest());

    }

    /**
     * Updates player and opponent stats and request to delete game from the server
     */
    private void endGameAfterOppHasLeft(){
        Map<String, List<String>> results = game.getResults();
        List<String> bluePlayer = results.get("bluePlayer");
        List<String> redPlayer = results.get("redPlayer");
        List<String> myResults;


        if(bluePlayer.get(0).equals(player.getName()))
            myResults = bluePlayer;
        else
            myResults = redPlayer;
        switch (myResults.get(1)){
            case "WIN":
                Toast.makeText(GameSurfaceView.this.getContext(),R.string.winMessage,Toast.LENGTH_SHORT).show();
                break;
            case "LOSS":
                Toast.makeText(GameSurfaceView.this.getContext(),R.string.lossMessage,Toast.LENGTH_SHORT).show();
                break;
            case "TIE":
                Toast.makeText(GameSurfaceView.this.getContext(),R.string.drawMessage,Toast.LENGTH_SHORT).show();
                break;
        }
        requestQueue.add(createPutStatsRequest(bluePlayer.get(0),bluePlayer.get(1),
                bluePlayer.get(2),bluePlayer.get(3)));
        requestQueue.add(createPutStatsRequest(redPlayer.get(0),redPlayer.get(1),
                redPlayer.get(2),redPlayer.get(3)));
        requestQueue.add(createEndGameRequest());

    }



    private class TurnTimerTask extends TimerTask{

        @Override
        public void run() {
            reDrawPanelsTimer.cancel();
            Point turn = game.makeRandomTurn();
            requestQueue.add(createPutRequest(turn.x,turn.y));
            turnTimerPanel.hide();
            turnTimerPanel.reset();
            isChanged = true;


        }
    }

    private class DecrementTimerTask extends TimerTask{

        @Override
        public void run() {
            isChanged = true;
            if(turnTimerPanel.isVisible()) turnTimerPanel.decrement();
            if(surrenderTimerPanel.isVisible()) surrenderTimerPanel.decrement();
        }
    }


    private class FinishGameTimerTask extends TimerTask{

        @Override
        public void run() {
            isChanged = true;
            surrenderTimerPanel.hide();
            surrenderTimerPanel.reset();
            reDrawPanelsTimer.cancel();
            endGameAfterOppHasLeft();

        }
    }

    private class DelayedPutRequest extends TimerTask{
        private int x;
        private int y;

        DelayedPutRequest(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public void run() {
            requestQueue.add(createPutRequest(x,y));
        }
    }

    private void init(Context context, Bundle extras){
        String nextTurn,bluePlayer,redPlayer,gameLink,userName,password;
        requestQueue = Volley.newRequestQueue(context);
        requestQueue2 = Volley.newRequestQueue(context);

        nextTurn = extras.getString("nextTurn");
        bluePlayer = extras.getString("bluePlayer");
        redPlayer = extras.getString("redPlayer");
        gameLink = extras.getString("gameLink");
        userName = extras.getString("userName");
        password = extras.getString("password");
        player = new Player(userName);
        encodedCredentials = createCredentials(userName,password);
        game = new Game(new Player(bluePlayer),new Player(redPlayer),new Player(nextTurn),gameLink);

        surfaceHolder = getHolder();
        scaleGestureDetector = new ScaleGestureDetector(context,new ScaleListener());
        gestureDetector = new GestureDetector(context,new GestureTap());

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        android.graphics.Point size = new android.graphics.Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int densityDpi = displayMetrics.densityDpi;
        densityDpCM = densityDpi/INCH;
        scaleFactor = 0.5f;

        game.getScoreTable().setTextSize(getResources().getDimensionPixelSize(R.dimen.myFontSize));

        previous = new PointF(0,0);
        moveTo = new PointF(0,0);
        max = new PointF(Game.GAME_FIELD_WIDTH*densityDpCM - screenWidth,
                Game.GAME_FIELD_HEIGHT*densityDpCM - screenHeight);


        turnTimerPanel = new TimerPanel(new PointF(screenWidth/2,densityDpCM),15,
                getResources().getDimensionPixelSize(R.dimen.myFontSize));
        surrenderTimerPanel = new TimerPanel(new PointF(screenWidth/2,densityDpCM),120,
                getResources().getDimensionPixelSize(R.dimen.myFontSize));

        if(!game.isPlayerTurn(player)) {
            requestQueue.add(createGetRequest());
            Toast.makeText(context,R.string.waitingForOpponentTurn,Toast.LENGTH_SHORT).show();
        }else {
            turnTimer = new Timer();
            turnTimer.schedule(new TurnTimerTask(), Game.TURN_TIME);
            reDrawPanelsTimer = new Timer();
            reDrawPanelsTimer.scheduleAtFixedRate(new DecrementTimerTask(),
                    1000,1000);
            turnTimerPanel.show();
            Toast.makeText(context,R.string.makeATurnMessage,Toast.LENGTH_SHORT).show();
        }

        setFocusableInTouchMode(true);
        requestFocus();
        setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                    AlertDialog alertbox = new AlertDialog.Builder(v.getContext())
                            .setMessage(R.string.exitGameMessage)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                                // do something when the button is clicked
                                public void onClick(DialogInterface arg0, int arg1) {

                                    if(game.getGameState()==Game.GAME_IS_STOPPED)
                                        endGameAfterOppHasLeft();
                                    else
                                        requestQueue2.add(createEndGameRequest());


                                }
                            })
                            .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                                // do something when the button is clicked
                                public void onClick(DialogInterface arg0, int arg1) {

                                }
                            })
                            .show();

                }
                return true;
            }
        });
    }
}
