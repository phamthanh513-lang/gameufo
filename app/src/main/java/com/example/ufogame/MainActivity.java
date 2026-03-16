package com.example.ufogame;

import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gameView = findViewById(R.id.gameView);

        Button start = findViewById(R.id.btnStart);
        Button pause = findViewById(R.id.btnPause);
        Button restart = findViewById(R.id.btnRestart);

        start.setOnClickListener(v -> gameView.startGame());

        pause.setOnClickListener(v -> gameView.pauseGame());

        restart.setOnClickListener(v -> gameView.restartGame());
    }
}