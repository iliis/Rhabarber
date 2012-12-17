package com.floern.rhabarber.network2;

import java.util.List;

import com.floern.rhabarber.R;
import com.floern.rhabarber.network2.GameServerService.UserInfo;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class UserListAdapter extends ArrayAdapter<UserInfo> {
	
	private final Context context;
	private final List<UserInfo> data;
	
	private final static int LIST_ITEM_LAYOUT = R.layout.list_item_leftright;
	
	public UserListAdapter(Context context, List<UserInfo> objects) {
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
		UserInfo user       = data.get(pos);
		TextView text_left  = ((TextView) viewToUse.findViewById(R.id.text_left));
		TextView text_right = ((TextView) viewToUse.findViewById(R.id.text_right));
		text_left .setText(user.ip);
		text_right.setText(user.port > 0 ? Integer.toString(user.port) : "");
		viewToUse.setTag(user);
		return viewToUse;
	}
}
