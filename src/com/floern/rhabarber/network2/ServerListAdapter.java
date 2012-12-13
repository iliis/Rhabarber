package com.floern.rhabarber.network2;

import java.util.ArrayList;

import com.floern.rhabarber.R;
import com.floern.rhabarber.network2.ServerAdvertisingListener.ServerInfo;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ServerListAdapter extends ArrayAdapter<ServerInfo> {
	
	private final Context context;
	private final ArrayList<ServerInfo> data;
	
	private final static int LIST_ITEM_LAYOUT = R.layout.list_item_leftright;
	
	public ServerListAdapter(Context context, ArrayList<ServerInfo> objects) {
		super(context, LIST_ITEM_LAYOUT, objects);
		this.context = context;
		this.data    = objects;
	}
	
	@Override
	public View getView(int pos, View viewToUse, ViewGroup parent) {
		// recycle view
		if (viewToUse == null) {
			viewToUse = ((Activity)context).getLayoutInflater().inflate(LIST_ITEM_LAYOUT, parent, false);
		}
		// update view
		ServerInfo server   = data.get(pos);
		TextView text_left  = ((TextView) viewToUse.findViewById(R.id.text_left));
		TextView text_right = ((TextView) viewToUse.findViewById(R.id.text_right));
		text_left.setText(server.address);
		text_right.setText(Integer.toString(server.port));
		viewToUse.setTag(server);
		return viewToUse;
	}
}
