package com.floern.rhabarber;

import java.util.ArrayList;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;

import com.floern.rhabarber.network.*;

public class MenuActivity extends Activity {

	private MyArrayAdapter adapter;
	ArrayList<GameDescription> gameDescriptions = new ArrayList<GameDescription>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_menu);
		
		this.gameDescriptions.add(new GameDescription("Test name","Test map",0)); // TODO delete test
		ListView listView = (ListView) findViewById(R.id.listViewGameDescriptions);

		// Assign adapter to ListView
		adapter = new MyArrayAdapter(this, gameDescriptions);
		listView.setAdapter(adapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_menu, menu);
		return true;
	}

	public void onClickHostGame(View v) {

	}

	public void onClickStartGame(View v) {

	}
}
