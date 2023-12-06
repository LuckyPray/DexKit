package org.luckypray.dexkit.demo;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.luckypray.dexkit.demo.annotations.Router;

import java.util.Random;

@Router(path = "/play")
public class PlayActivity extends AppCompatActivity {

    private static final String TAG = "PlayActivity";

    private TextView resultText;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        Log.d(TAG, "onCreate");

        HandlerThread handlerThread = new HandlerThread("PlayActivity");
        handlerThread.start();

        handler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case -1:
                        System.exit(0);
                        break;
                    case 0:
                        runOnUiThread(() -> rollDice(false));
                        break;
                    case 114514:
                        runOnUiThread(() -> rollDice(true));
                        break;
                }
            }
        };

        resultText = findViewById(R.id.resultText);
        Button rollButton = findViewById(R.id.rollButton);
        rollButton.setOnClickListener(v -> {
            Log.d(TAG, "onClick: rollButton");
            float r = new Random().nextFloat();
            if (r < 0.01) {
                handler.sendEmptyMessage(-1);
            } else if(r < 0.987f) {
                handler.sendEmptyMessage(0);
            } else {
                handler.sendEmptyMessage(114514);
            }
        });
    }

    public void rollDice(boolean jackpot) {
        int diceValue;
        if (!jackpot) {
            diceValue = RandomUtil.getRandomDice();
        } else {
            diceValue = 6;
        }
        String result = "You rolled a " + diceValue;
        resultText.setText(result);
        Log.d(TAG, "rollDice: " + result);
    }
}
