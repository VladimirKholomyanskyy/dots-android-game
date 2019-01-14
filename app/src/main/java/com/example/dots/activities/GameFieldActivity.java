package com.example.dots.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import com.example.dots.view.GameSurfaceView;

public class GameFieldActivity extends AppCompatActivity{
    private GameSurfaceView gameSurfaceView;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(savedInstanceState == null){
            Bundle extras = getIntent().getExtras();
            gameSurfaceView = new GameSurfaceView(this, extras);
        } else {

        }

        setContentView(gameSurfaceView);

    }

    @Override
    protected  void onResume(){
        super.onResume();
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        gameSurfaceView.onResume();

    }

    @Override
    protected void onPause(){
        super.onPause();
        gameSurfaceView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        gameSurfaceView.onStop();
    }
}
