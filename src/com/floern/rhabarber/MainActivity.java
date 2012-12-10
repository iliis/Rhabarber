package com.floern.rhabarber;

import java.io.IOException;

import com.floern.rhabarber.sensorgltest.SensorTestOpenGLActivity;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

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
    

    public void startGameActivity(View v) {
    	// list all levels in asset/level/
		try {
			final String[] levels = this.getAssets().list("level");
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
		    builder.setTitle("choose a map");
		    builder.setItems(levels, new DialogInterface.OnClickListener() {
		               public void onClick(DialogInterface dialog, int which) {
		               // The 'which' argument contains the index position
		               // of the selected item
		            	   Intent i = new Intent(MainActivity.this, GameActivity.class);
		            	   i.putExtra("level", levels[which]);
		            	   startActivity(i);
		           }
		    });
		    builder.create().show();
		    
		} catch (IOException e) {
			e.printStackTrace();
			Log.e("Rhabarber", "Couldn't load levels from asset/level/");
			Toast.makeText(this, "Couldn't find any levels.", Toast.LENGTH_SHORT).show();
		}
    	
    }
    
    

    
    public void onClickJoinServer(View v){
    	Intent i = new Intent(this, ServerJoinActivity.class);
    	startActivity(i);
    }
    
    public void onClickSetupServer(View v){
    	Intent i = new Intent(this, ServerSetupActivity.class);
    	startActivity(i);
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
}
