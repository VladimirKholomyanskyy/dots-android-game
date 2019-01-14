package com.example.dots.model;

import android.graphics.Point;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * represents game data that can be sent to server
 */
public class GameData {
    private Point turn;


    public GameData(int x, int y){
        this.turn = new Point(x,y);
    }

    /**
     * Converts to json object
     * @return json object
     */
    public JSONObject toJSON(){
        JSONObject gameDataJSON = new JSONObject();
        JSONObject turnJSON = new JSONObject();
        try {
            turnJSON.put("x",turn.x);
            turnJSON.put("y", turn.y);
            gameDataJSON.put("turn",turnJSON);

        } catch (JSONException e) {
            e.printStackTrace();

        }
        return  gameDataJSON;
    }

}
