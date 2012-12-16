package com.floern.rhabarber;

import java.io.IOException;
import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

import com.floern.rhabarber.graphic.GameGLSurfaceView;
import com.floern.rhabarber.graphic.primitives.SkeletonKeyframe;
import com.floern.rhabarber.logic.elements.GameWorld;
import com.floern.rhabarber.logic.elements.Player;
import com.floern.rhabarber.network2.ClientNetworkingLogic;
import com.floern.rhabarber.network2.ClientStateAccumulator;
import com.floern.rhabarber.network2.GameNetworkingProtocolConnection;
import com.floern.rhabarber.util.FXMath;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import at.emini.physics2D.Event;
import at.emini.physics2D.PhysicsEventListener;
import at.emini.physics2D.util.FXVector;

/* contains the game itself, starts open gl (which calls the physics and logic on every frame)
 * 
 */
public class GameActivity extends Activity implements SensorEventListener {

	// connection with server
	public static ClientNetworkingLogic __clientNetworkingLogic = null; // global variable, as we can't pass sockets via Intent
	private       ClientNetworkingLogic   clientNetworkingLogic = null;
	
	private GameGLSurfaceView surfaceView;

	private SensorManager sensorManager;
	private boolean deviceIsLandscapeDefault;

	GameWorld game;

	private float[] acceleration = new float[3];
	private int playerIdx;
	private boolean isserver = false;
	boolean walk_left = false, walk_right = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		clientNetworkingLogic = __clientNetworkingLogic;
		__clientNetworkingLogic = null;
		
		
		
		// avoid screen turning off
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// fullscreen
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		// set View
		surfaceView = new GameGLSurfaceView(this);
		surfaceView.setRendererCallback(this);
		setContentView(surfaceView);

		// setup sensor manager
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		// check default device orientation
		Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		int orientation = (display.getWidth() <= display.getHeight()) ?
				  Configuration.ORIENTATION_PORTRAIT
				: Configuration.ORIENTATION_LANDSCAPE;
		
		// sensor vector is rotated on landscape-default devices (some tablets)
		int rotation = display.getRotation();
		deviceIsLandscapeDefault = (orientation == Configuration.ORIENTATION_LANDSCAPE && (rotation == Surface.ROTATION_0  || rotation == Surface.ROTATION_180))
				                || (orientation == Configuration.ORIENTATION_PORTRAIT  && (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270));

		// setup up the actual game
		try {
			isserver   = getIntent().getExtras().getBoolean("isserver");
			playerIdx  = getIntent().getExtras().getInt("playerIdx");
			game = new GameWorld(this.getAssets().open(	"level/"+getIntent().getExtras().getString("level")), this.getResources(), isserver, playerIdx);
			Log.d("foo", "starting a game");
			Log.d("foo", "isserver = "+isserver);
			Log.d("foo", "playerIdx = "+playerIdx);
			clientNetworkingLogic.game = game;
			Log.d("foo", "linked world to network stuff");
			
			
			//playerIdx = game.addPlayer();
			
			surfaceView.renderer.readLevelSize(game);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void onDraw(GL10 gl) {
		// TODO: fix for singleplayer (the whole project is as of now only working for multiplayer)
		game.tick();
		
		/*game.setAccel(acceleration);
		if (walk_left != walk_right) {
			
			if (walk_left) {
				game.walk(ClientStateAccumulator.UserInputWalk.LEFT);
			} else
				game.walk(ClientStateAccumulator.UserInputWalk.RIGHT);
		}*/
		game.draw(gl);
		
		if(game.isFinished()) {
			this.onGameFinished(game.getWinner());
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (ev != null) {
			walk_right = false;
			walk_left  = false;

			if ((MotionEvent.ACTION_MASK & ev.getAction()) != MotionEvent.ACTION_UP) {

				for (int p = 0; p < ev.getPointerCount(); p++) {
					if (ev.getX(p) > this.getWindow().getDecorView().getWidth() / 2) {
						walk_right = true;
					} else {
						walk_left = true;
					}
				}
			}
			
			if     (!walk_right &&  walk_left)
				sendUserInputToServer(ClientStateAccumulator.UserInputWalk.LEFT);
			else if( walk_right && !walk_left)
				sendUserInputToServer(ClientStateAccumulator.UserInputWalk.RIGHT);
			else
				sendUserInputToServer(ClientStateAccumulator.UserInputWalk.NONE);

			return true;
		}

		return super.onTouchEvent(ev);
	}

	/**
	 * Called when sensor values have changed.
	 * 
	 * @param event
	 *            SensorEvent
	 */
	public void onSensorChanged(SensorEvent event) {
		// some devices always report UNRELIABLE, making it unusable with this
		// code:
		/*
		 * if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
		 * return; // sensor data unreliable }
		 */

		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			// update acceleration values
			// TODO: fix this 
			if (true || deviceIsLandscapeDefault) {
				// rotate X and Y
				acceleration[0] =  event.values[1];
				acceleration[1] = event.values[0]; // this was minus, every device seems to behave a bit differently
				acceleration[2] =  event.values[2];
			} else {
				System.arraycopy(event.values, 0, acceleration, 0, 3);
			}
			
			sendAccelerationToServer();
		}
	}
	
	public void sendAccelerationToServer() {
		clientNetworkingLogic.serverConnection.sendAccelerationData(playerIdx, new ClientStateAccumulator.Acceleration(acceleration));
	}
	
	public void sendUserInputToServer(ClientStateAccumulator.UserInputWalk i) {
		clientNetworkingLogic.serverConnection.sendUserInputData(playerIdx, i);
	}

	/**
	 * Register sensor listener
	 */
	public void sensorEnable() {
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_GAME);
	}

	/**
	 * Unregister sensor listener
	 */
	public void sensorDisable() {
		sensorManager.unregisterListener(this);
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	protected void onResume() {
		super.onResume();
		surfaceView.onResume();
		sensorEnable();
	}

	@Override
	protected void onPause() {
		sensorDisable();
		surfaceView.onPause();
		super.onPause();
		System.gc();
	}

	@Override
	protected void onDestroy() {
		sensorDisable();
		super.onDestroy();
	}
	
	public void onGameFinished(final int winIdx)
	{
		runOnUiThread(new Runnable() {
			public void run() {
				Resources res = getResources();
				AlertDialog builder = new AlertDialog.Builder(GameActivity.this).create();
			    builder.setTitle("Game finished!");
			    builder.setCanceledOnTouchOutside(false);
			    builder.setCancelable(false);
			    if(winIdx == playerIdx)
			    {
			    	builder.setMessage(res.getString(R.string.winNotification));
			    }
			    else if(winIdx < 0)
			    {
			    	builder.setMessage(res.getString(R.string.canceledNotification));
			    }
			    else
			    {
			    	builder.setMessage(res.getString(R.string.loseNotification));
			    }
			    builder.setButton(Dialog.BUTTON_POSITIVE, res.getString(R.string.ok), new OnClickListener() {
					
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				});
			    builder.show();
			}
		});
	}
	
	
	/*
	 * How the heck do events work???
	 * I have no idea how to get info about the colliding bodies :-/
	 */
	/*public void eventTriggered(Event e, Object triggerBody) {
		if (e == null) {
			Log.d("bla", "null event");
			//never observed
		} else {
			if (triggerBody.equals(e.getTargetObject())) {
				//never observed
				Log.d("bla", "target equals trigger");
			}
			for (Player p : this.game.getPlayers()) {
				Log.d("bla", "check for player collision");
				if (triggerBody.equals(p)) {
					Log.d("bla", "1st check: player " + p.getIdx()
							+ " has collected a treasure");
					//never observed
				} else if (e.getTargetObject() != null) {
					if (p.equals((e.getTargetObject()))) {
						Log.d("bla", "2nd check: player " + p.getIdx()
								+ " has collected a treasure");
						//never observed
					}
				} else {
					Log.d("bla", "null targetObject");
					//this
				}

			}
		}
	}*/
}
