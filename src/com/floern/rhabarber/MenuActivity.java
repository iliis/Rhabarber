package com.floern.rhabarber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.floern.rhabarber.network.*;

public class MenuActivity extends Activity{
	public static final String TAG = "Menu";

	private static final int TOAST_DURATION = Toast.LENGTH_SHORT;

	private NetworkController networkController = null;

	private MyArrayAdapter listViewAdapter;
	ArrayList<GameDescription> gameDescriptions = new ArrayList<GameDescription>();

	private GameDescription hostGameDescription = new GameDescription();

	private EditText editTextGameName;
	private ToggleButton toggleButtonHost;
	private Button buttonStartGame;
	private TextView textViewPlayerCount;
	private ListView listViewGameDescriptions;
	private Spinner spinnerMap;
	
	// Dialogs
	private Dialog joinDialog = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_menu);

		editTextGameName = (EditText) findViewById(R.id.editTextGameName);
		editTextGameName.setText("");

		toggleButtonHost = (ToggleButton) findViewById(R.id.toggleButtonAdvertise);
		toggleButtonHost.setText("Idle");

		listViewGameDescriptions = (ListView) findViewById(R.id.listViewGameDescriptions);
		listViewGameDescriptions
				.setOnItemClickListener(new ListView.OnItemClickListener() {
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						GameDescription selected = (GameDescription) parent
								.getItemAtPosition(position);
						
						// TODO join channel
						
						joinDialog = DialogBuilder.createJoinDialog(MenuActivity.this, networkController);
						joinDialog.show();
					}
				});

		spinnerMap = (Spinner) findViewById(R.id.spinnerMap);
		spinnerMap.setOnItemSelectedListener(new OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				String selected = parent.getItemAtPosition(pos).toString();
				hostGameDescription.setMapName(selected);
			}

			public void onNothingSelected(AdapterView parent) {
				// Do nothing.
			}
		});

		// list all levels in asset/level/
		String[] levels = new String[] { "no levels found" };
		try {
			levels = this.getAssets().list("level");
		} catch (IOException e) {
			e.printStackTrace();
			Log.e("Rhabarber", "Couldn't load levels from asset/level/");
			Toast.makeText(this, "Couldn't find any levels.", TOAST_DURATION)
					.show();
		}

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, levels);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerMap.setAdapter(adapter);

		textViewPlayerCount = (TextView) findViewById(R.id.textViewPlayerCount);

		buttonStartGame = (Button) findViewById(R.id.buttonStartGame);
		buttonStartGame.setEnabled(false);

		ListView listView = (ListView) findViewById(R.id.listViewGameDescriptions);
		// Assign adapter to ListView
		listViewAdapter = new MyArrayAdapter(this, gameDescriptions);
		listView.setAdapter(listViewAdapter);

		/*
		 * Keep a pointer to the Android Appliation class around. We use this as
		 * the Model for our MVC-based application. Whenever we are started we
		 * need to "check in" with the application so it can ensure that our
		 * required services are running.
		 */
		networkController = (NetworkController) getApplication();
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
			// TODO
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

				// update game description object
				this.hostGameDescription.setGameName(name);

				// TODO start hosting
			}

		} else {
			Log.i(TAG, "onClickHostGame() - Stop hosting game");

			// TODO stop hosting
		}
	}

	public void onDestroy() {
		Log.i(TAG, "onDestroy()");
		networkController = (NetworkController) getApplication();
		super.onDestroy();
	}



	  
}
