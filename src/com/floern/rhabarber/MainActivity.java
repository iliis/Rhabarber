package com.floern.rhabarber;

import com.floern.rhabarber.physicstest.PhysicsTestActivity;
import com.floern.rhabarber.sensorgltest.SensorTestOpenGLActivity;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    
    
    /**
     * onClick event for starting test activity
     * @param v pressed Button
     */
    public void startSensorTestOpenGLActivity(View v) {
    	startActivity(new Intent(this, SensorTestOpenGLActivity.class));
    }
    
    /**
     * onClick event for starting physics test activity
     * @param v pressed Button
     */
    public void onPhysicsTestActivityButtonPressed(View v) {
    	startActivity(new Intent(this, PhysicsTestActivity.class));
    }
    

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
}
