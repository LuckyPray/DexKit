package org.luckypray.dexkit.demo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.luckypray.dexkit.demo.annotations.Router;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

@Router(path = "/play")
public class PlayActivity extends AppCompatActivity {

    private final String TAG = "PlayActivity";

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

                if (msg.what == 0) {
                    runOnUiThread(() -> rollDice());
                }
            }
        };

        resultText = findViewById(R.id.resultText);
        Button rollButton = findViewById(R.id.rollButton);
        rollButton.setOnClickListener(v -> {
            Log.d(TAG, "onClick: rollButton");
            handler.sendEmptyMessage(0);
        });
    }

    public void rollDice() {
        int diceValue = RandomUtil.getRandomDice();
        String result = "You rolled a " + diceValue;
        resultText.setText(result);
        Log.d(TAG, "rollDice: " + result);
    }
}
