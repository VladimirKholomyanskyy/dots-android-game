package com.example.dots.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.dots.R;
import com.example.dots.urls.MyURLs;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private Button searchCancelButton;
    private String userName;
    private String password;
    private int statusCode;
    private int statusCodeStats;
    private String gameLink;
    private RequestQueue queue;
    private RequestQueue queueForDelete;
    private int buttonMode;
    private EditText userNameEditText;
    private EditText amountOfGamesEditText;
    private EditText winRateEditText;
    private EditText lastTimePlayedEditText;
    private EditText resultOfLastGameEditText;
    private EditText highestScoreEditText;
    private final static int CANCEL = 1;
    private final static int SEARCH = 2;
    private Timer refreshTimer;
    private final static int DELAY = 130000;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        searchCancelButton = findViewById(R.id.searchCancelButton);
        queue = Volley.newRequestQueue(this);
        queueForDelete = Volley.newRequestQueue(this);
        buttonMode = SEARCH;
        userNameEditText = findViewById(R.id.userNameEditText);
        amountOfGamesEditText = findViewById(R.id.amountOfGamesEditText);
        winRateEditText = findViewById(R.id.winRateEditText);
        lastTimePlayedEditText = findViewById(R.id.lastTimePlayedEditText);
        resultOfLastGameEditText = findViewById(R.id.resultOfLastGameEditText);
        highestScoreEditText = findViewById(R.id.highestScoreEditText);

        if(savedInstanceState == null){
            Bundle extras = getIntent().getExtras();
            if(extras != null) {
                userName = extras.getString("UserName");
                password = extras.getString("Password");
            }
        } else {
            userName = savedInstanceState.getString("userName");
            password = savedInstanceState.getString("password");
        }



        searchCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(buttonMode == SEARCH){
                    buttonMode = CANCEL;
                    queue.add(createGetRequest());
                    searchCancelButton.setText(R.string.cancel);


                }
                else if(buttonMode == CANCEL){
                    searchCancelButton.setText(R.string.search);
                    buttonMode = SEARCH;

                    queueForDelete.add(createDeleteRequest());

                }
            }
        });




    }


    @Override
    protected void onStart() {
        super.onStart();
        queue.add(createGetRequestForStatsResource());
        refreshTimer = new Timer();
        refreshTimer.schedule(new RefreshTimerTask(),DELAY);
    }


    @Override
    protected void onStop() {
        super.onStop();
        if(refreshTimer!=null)
            refreshTimer.cancel();
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("userName",userName);
        outState.putString("password",password);
        super.onSaveInstanceState(outState);
    }


    private JsonObjectRequest createGetRequestForStatsResource(){
        String playerStatsURL = MyURLs.PLAYER_STATS_URL+"/"+userName;

        JsonObjectRequest getRequestForStats = new JsonObjectRequest(Request.Method.GET, playerStatsURL,null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject  response) {
                        if(statusCodeStats == HttpURLConnection.HTTP_OK) {
                            try {
                                String playerName = response.getString("playerName");
                                int gamesAmount = response.getInt("gamesAmount");
                                int wins = response.getInt("wins");
                                int highestScore = response.getInt("highestScore");

                                String lastGameResultText, lastTimePlayedText, resultOfLastGameText, winRateText,highestScoreText;
                                int lastGamePlayerScore = response.getInt("lastGamePlayerScore");
                                int lastGameOppScore = response.getInt("lastGameOppScore");


                                if (gamesAmount == 0) {
                                    lastTimePlayedText = "";
                                    resultOfLastGameText = "";
                                    winRateText = "";
                                    highestScoreText = "";

                                } else {
                                    lastGameResultText = response.getString("lastGameResult");
                                    lastTimePlayedText = response.getString("lastTimePlayed");
                                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH);
                                    SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMM dd, yyyy HH:mm:ss",Locale.ENGLISH);
                                    try {
                                        Date date = formatter.parse(lastTimePlayedText);
                                        lastTimePlayedText = dateFormat.format(date);
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                    }
                                    double winRate = (wins * 100.0) / gamesAmount;
                                    winRateText = String.format("%.1f%%", winRate);
                                    resultOfLastGameText = String.format("%s: %d-%d",
                                            lastGameResultText, lastGamePlayerScore, lastGameOppScore);
                                    highestScoreText = ""+highestScore;

                                }
                                highestScoreEditText.setText(highestScoreText);
                                winRateEditText.setText(winRateText);
                                resultOfLastGameEditText.setText(resultOfLastGameText);
                                userNameEditText.setText(playerName);
                                amountOfGamesEditText.setText("" + gamesAmount);
                                lastTimePlayedEditText.setText(lastTimePlayedText);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this,R.string.serverError, Toast.LENGTH_SHORT).show();
                MainActivity.this.finish();
            }
        }){
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response){
                statusCodeStats = response.statusCode;
                return super.parseNetworkResponse(response);
            }

            @Override
            public Map<String,String> getHeaders() throws AuthFailureError {
                HashMap<String,String> headers = new HashMap<>();
                String credentials = userName + ":" + password;
                String encodedCredentials = Base64.encodeToString(credentials.getBytes(),Base64.NO_WRAP);
                headers.put("Authorization","Basic "+encodedCredentials);
                return headers;
            }
        };
        return getRequestForStats;
    }


    private StringRequest createDeleteRequest(){
        StringRequest deleteRequest = new StringRequest(Request.Method.DELETE, MyURLs.GAME_QUEUE_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String  response) {
                        if(statusCode == HttpURLConnection.HTTP_OK){
                           searchCancelButton.setText(R.string.search);
                           buttonMode = SEARCH;
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                searchCancelButton.setText(R.string.search);
                buttonMode = SEARCH;
                Toast.makeText(MainActivity.this,R.string.serverError, Toast.LENGTH_SHORT).show();

            }
        }){
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response){
                statusCode = response.statusCode;
                return super.parseNetworkResponse(response);
            }

            @Override
            public Map<String,String> getHeaders() throws AuthFailureError {
                HashMap<String,String> headers = new HashMap<>();
                String credentials = userName + ":" + password;
                String encodedCredentials = Base64.encodeToString(credentials.getBytes(),Base64.NO_WRAP);
                headers.put("Authorization","Basic "+encodedCredentials);
                return headers;
            }
        };
        return deleteRequest;
    }



    private JsonObjectRequest createPostRequest(){
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, MyURLs.GAME_QUEUE_URL,null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject  response) {
                        if(statusCode == HttpURLConnection.HTTP_OK){
                            try {
                                startGameFieldActivity(response);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                NetworkResponse networkResponse = error.networkResponse;
                if(networkResponse != null && networkResponse.statusCode!=HttpURLConnection.HTTP_UNAVAILABLE){
                    buttonMode = SEARCH;
                    searchCancelButton.setText(R.string.search);
                }

            }
        }){
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response){
                statusCode = response.statusCode;
                gameLink = response.headers.get("link");
                return super.parseNetworkResponse(response);
            }

            @Override
            public Map<String,String> getHeaders() throws AuthFailureError {
                HashMap<String,String> headers = new HashMap<>();
                String credentials = userName + ":" + password;
                String encodedCredentials = Base64.encodeToString(credentials.getBytes(),Base64.NO_WRAP);
                headers.put("Authorization","Basic "+encodedCredentials);
                return headers;
            }
        };

        postRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        return postRequest;
    }


    private JsonObjectRequest createGetRequest(){
        JsonObjectRequest getRequest = new JsonObjectRequest(Request.Method.GET, MyURLs.GAME_QUEUE_URL,null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject  response) {
                        if(statusCode == HttpURLConnection.HTTP_OK){
                            try {
                                startGameFieldActivity(response);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                NetworkResponse networkResponse = error.networkResponse;
                if(networkResponse != null && networkResponse.statusCode == HttpURLConnection.HTTP_NOT_FOUND){
                    queue.add(createPostRequest());
                }else {
                    searchCancelButton.setText(R.string.search);
                    buttonMode = SEARCH;
                    Toast.makeText(MainActivity.this,R.string.serverError,Toast.LENGTH_SHORT).show();
                }
            }
        }){
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response){
                statusCode = response.statusCode;
                gameLink = response.headers.get("link");
                return super.parseNetworkResponse(response);
            }

            @Override
            public Map<String,String> getHeaders() throws AuthFailureError {
                HashMap<String,String> headers = new HashMap<>();
                String credentials = userName + ":" + password;
                String encodedCredentials = Base64.encodeToString(credentials.getBytes(),Base64.NO_WRAP);
                headers.put("Authorization","Basic "+encodedCredentials);
                return headers;
            }
        };

        return getRequest;
    }


    private void startGameFieldActivity(JSONObject jsonObject) throws JSONException {
        searchCancelButton.setText(R.string.search);
        buttonMode = SEARCH;
        JSONObject json = jsonObject.getJSONObject("firstPlayer");
        String firstPlayer = json.getString("name");

        json = jsonObject.getJSONObject("secondPlayer");
        String secondPlayer = json.getString("name");

        json = jsonObject.getJSONObject("nextTurn");
        String nextTurn = json.getString("name");
        Bundle extras = createExtras(firstPlayer,secondPlayer,nextTurn);
        Intent intent = new Intent(MainActivity.this, GameFieldActivity.class);
        intent.putExtras(extras);
        startActivity(intent);
    }


    private class RefreshTimerTask extends TimerTask{

        @Override
        public void run() {
            queue.add(createGetRequestForStatsResource());
        }
    }


    private Bundle createExtras(String firstPlayer, String secondPlayer, String nextTurn){
        String bluePlayer, redPlayer;
        Bundle extras = new Bundle();
        bluePlayer = nextTurn;
        if(nextTurn.equals(firstPlayer))
            redPlayer = secondPlayer;
        else
            redPlayer = firstPlayer;


        extras.putString("bluePlayer",bluePlayer);
        extras.putString("redPlayer",redPlayer);
        extras.putString("nextTurn", nextTurn);
        extras.putString("gameLink", gameLink);
        extras.putString("userName",userName);
        extras.putString("password",password);
        return extras;
    }
}
