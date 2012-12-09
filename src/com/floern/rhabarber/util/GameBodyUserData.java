package com.floern.rhabarber.util;

import java.util.HashMap;

import android.util.Log;
import at.emini.physics2D.UserData;

/* This class handles additional data passed to a Body via the World-File.
 * It is used to add additional fields to a Body, eg. marking it as a 'Treasure' and then loading it as such.
 */

public class GameBodyUserData implements UserData {
	
	public boolean is_game_element = false;
	public HashMap<String,String> data;

	public UserData copy() {
		// TODO Auto-generated method stub
		GameBodyUserData d = new GameBodyUserData();
		
		d.is_game_element = this.is_game_element;
		d.data            = this.data;
		
		return d;
	}

	public UserData createNewUserData(String data, int type) {
		
		GameBodyUserData userdata = new GameBodyUserData();
		userdata.is_game_element = !(data.equals(""));
		
		if (userdata.is_game_element) {
			userdata.data = KeyValueParser.parse(data);
		}
		
		return userdata;
	}

}
