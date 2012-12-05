package com.floern.rhabarber.network;

import java.util.ArrayList;

import com.floern.rhabarber.R;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class MyArrayAdapter extends ArrayAdapter<GameDescription> {
	private final Context context;
	ArrayList<GameDescription> gameDescriptions = new ArrayList<GameDescription>();

	public MyArrayAdapter(Context context, ArrayList<GameDescription> messages) {
		super(context, R.layout.game_description, messages);
		this.context = context;
		this.gameDescriptions = messages;
	}

	static class ViewHolder {
		public TextView textViewGameName;
		public TextView textViewMapName;
		public TextView textViewPlayerCount;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// Performance optimization: reuse views outside of visible area if
		// possible
		View messageView = convertView;
		if (messageView == null) {
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			messageView = inflater.inflate(R.layout.game_description, parent,
					false);
			// Performance optimization: enables faster access to view via
			// static class
			ViewHolder viewHolder = new ViewHolder();
			viewHolder.textViewGameName = (TextView) messageView
					.findViewById(R.id.textViewGameName);
			viewHolder.textViewMapName = (TextView) messageView
					.findViewById(R.id.textViewMapName);
			viewHolder.textViewPlayerCount = (TextView) messageView
					.findViewById(R.id.textViewPlayerCount);
			messageView.setTag(viewHolder);
		}

		ViewHolder holder = (ViewHolder) messageView.getTag();

		holder.textViewGameName.setText(this.gameDescriptions.get(position)
				.getGameName());
		holder.textViewMapName.setText(this.gameDescriptions.get(position)
				.getMapName());
		holder.textViewPlayerCount.setText(this.gameDescriptions.get(position)
				.getPlayerCount()+"");
		return messageView;
	}
}
