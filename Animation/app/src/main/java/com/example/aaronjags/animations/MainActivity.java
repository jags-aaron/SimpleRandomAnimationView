package com.example.aaronjags.animations;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private FigureAnimationView mAnimationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAnimationView = findViewById(R.id.animated_view);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAnimationView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAnimationView.pause();
    }
}


