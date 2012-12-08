package com.floern.rhabarber;

import java.util.ArrayList;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.Dialog;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.floern.rhabarber.network.*;

public class MenuActivity extends Activity implements Observer {
	public static final String TAG = "Menu";

	private static final int TOAST_DURATION = Toast.LENGTH_SHORT;

	private ChatApplication networkController = null;

	private MyArrayAdapter adapter;
	ArrayList<GameDescription> gameDescriptions = new ArrayList<GameDescription>();

	private GameDescription hostGameDescription = new GameDescription();

	private EditText editTextGameName;
	private ToggleButton toggleButtonHost;
	private Button buttonStartGame;
	private TextView textViewPlayerCount;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_menu);

		editTextGameName = (EditText) findViewById(R.id.editTextGameName);
		editTextGameName.setText("");

		toggleButtonHost = (ToggleButton) findViewById(R.id.toggleButtonAdvertise);
		toggleButtonHost.setText("Idle");

		textViewPlayerCount = (TextView) findViewById(R.id.textViewPlayerCount);

		buttonStartGame = (Button) findViewById(R.id.buttonStartGame);
		buttonStartGame.setEnabled(false);

		// TODO delete test
		this.gameDescriptions.add(new GameDescription("Test name", "Test map",
				0));
		ListView listView = (ListView) findViewById(R.id.listViewGameDescriptions);

		// Assign adapter to ListView
		adapter = new MyArrayAdapter(this, gameDescriptions);
		listView.setAdapter(adapter);

		/*
		 * Keep a pointer to the Android Appliation class around. We use this as
		 * the Model for our MVC-based application. Whenever we are started we
		 * need to "check in" with the application so it can ensure that our
		 * required services are running.
		 */
		networkController = (ChatApplication) getApplication();
		networkController.checkin();

		/*
		 * Call down into the model to get its current state. Since the model
		 * outlives its Activities, this may actually be a lot of state and not
		 * just empty.
		 */
		updateChannelState();

		/*
		 * Now that we're all ready to go, we are ready to accept notifications
		 * from other components.
		 */
		networkController.addObserver(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_settings:
			// TODO
			return true;
		case R.id.menu_quit:
			networkController.quit();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void onClickHostGame(View v) {
		if (toggleButtonHost.isChecked()) {

			// update name information if not empty
			String name = editTextGameName.getText().toString();
			if (name.equals("")) {
				Toast.makeText(this, R.string.name_invalid, TOAST_DURATION)
						.show();
			} else {
				Log.i(TAG, "onClickHostGame() - Start hosting game");

				// update name in application object
				networkController.hostSetChannelName(name);
				networkController.hostInitChannel();
				networkController.hostStartChannel();

				// TODO also set map
			}

		} else {
			Log.i(TAG, "onClickHostGame() - Stop hosting game");

			networkController.hostStopChannel();
		}
		updateChannelState();
	}

	public void onClickStartGame(View v) {
		Log.i(TAG, "onClickStartGame()");

		updateChannelState();
	}

	public void onDestroy() {
		Log.i(TAG, "onDestroy()");
		networkController = (ChatApplication) getApplication();
		networkController.deleteObserver(this);
		super.onDestroy();
	}

	static final int DIALOG_JOIN_ID = 1;
	public static final int DIALOG_ALLJOYN_ERROR_ID = 3;

	protected Dialog onCreateDialog(int id) {
		Log.i(TAG, "onCreateDialog()");
		Dialog result = null;
		switch (id) {
		case DIALOG_JOIN_ID: {
			DialogBuilder builder = new DialogBuilder();
			result = builder.createJoinDialog(this, networkController);
		}
			break;
		case DIALOG_ALLJOYN_ERROR_ID: {
			DialogBuilder builder = new DialogBuilder();
			result = builder.createAllJoynErrorDialog(this, networkController);
		}
			break;
		}
		return result;
	}

	public synchronized void update(Observable o, Object arg) {
		Log.i(TAG, "update(" + arg + ")");
		String qualifier = (String) arg;

		if (qualifier.equals(ChatApplication.APPLICATION_QUIT_EVENT)) {
			Message message = mHandler
					.obtainMessage(HANDLE_APPLICATION_QUIT_EVENT);
			mHandler.sendMessage(message);
		}

		if (qualifier.equals(ChatApplication.HOST_CHANNEL_STATE_CHANGED_EVENT)) {
			Message message = mHandler
					.obtainMessage(HANDLE_CHANNEL_STATE_CHANGED_EVENT);
			mHandler.sendMessage(message);
		}

		if (qualifier.equals(ChatApplication.ALLJOYN_ERROR_EVENT)) {
			Message message = mHandler
					.obtainMessage(HANDLE_ALLJOYN_ERROR_EVENT);
			mHandler.sendMessage(message);
		}
	}

	private void updateChannelState() {
		AllJoynService.HostChannelState channelState = networkController
				.hostGetChannelState();
		Log.i(TAG,"updateChannelState - channelState = "+channelState);

		switch (channelState) {
		case IDLE:
			toggleButtonHost.setText("Idle");
			break;
		case NAMED:
			toggleButtonHost.setText("Named");
			break;
		case BOUND:
			toggleButtonHost.setText("Bound");
			break;
		case ADVERTISED:
			toggleButtonHost.setText("Advertised");
			break;
		case CONNECTED:
			toggleButtonHost.setText("Connected");
			textViewPlayerCount.setText(this.hostGameDescription
					.getPlayerCount()
					+ getResources().getString(R.string.player_count));
			break;
		default:
			toggleButtonHost.setText("Unknown");
			break;
		}
		
		if (channelState == AllJoynService.HostChannelState.IDLE) {
			EditText editTextGameName = (EditText) findViewById(R.id.editTextGameName);
			if (editTextGameName.getTag() != null)
				editTextGameName.setKeyListener((KeyListener) editTextGameName
						.getTag());

			toggleButtonHost.setChecked(false);

			((Spinner) findViewById(R.id.spinnerMap)).setClickable(true);
			buttonStartGame.setEnabled(false);
		} else {
			EditText editTextGameName = (EditText) findViewById(R.id.editTextGameName);
			if (editTextGameName.getKeyListener() != null)
				editTextGameName.setTag(editTextGameName.getKeyListener());
			editTextGameName.setKeyListener(null);

			toggleButtonHost.setChecked(true);

			((Spinner) findViewById(R.id.spinnerMap)).setClickable(false);
			buttonStartGame.setEnabled(true);
		}
	}

	private void alljoynError() {
		if (networkController.getErrorModule() == ChatApplication.Module.GENERAL
				|| networkController.getErrorModule() == ChatApplication.Module.USE) {
			showDialog(DIALOG_ALLJOYN_ERROR_ID);
		}
	}

	private static final int HANDLE_APPLICATION_QUIT_EVENT = 0;
	private static final int HANDLE_CHANNEL_STATE_CHANGED_EVENT = 1;
	private static final int HANDLE_ALLJOYN_ERROR_EVENT = 2;

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HANDLE_APPLICATION_QUIT_EVENT: {
				Log.i(TAG,
						"mHandler.handleMessage(): HANDLE_APPLICATION_QUIT_EVENT");
				finish();
			}
				break;
			case HANDLE_CHANNEL_STATE_CHANGED_EVENT: {
				Log.i(TAG,
						"mHandler.handleMessage(): HANDLE_CHANNEL_STATE_CHANGED_EVENT");
				updateChannelState();
			}
				break;
			case HANDLE_ALLJOYN_ERROR_EVENT: {
				Log.i(TAG,
						"mHandler.handleMessage(): HANDLE_ALLJOYN_ERROR_EVENT");
				alljoynError();
			}
				break;
			default:
				break;
			}
		}
	};
}
