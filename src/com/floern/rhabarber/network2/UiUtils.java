package com.floern.rhabarber.network2;

import android.app.Activity;
import android.widget.Toast;

public final class UiUtils {

	/**
	 * Create a Toast message on the user interface thread
	 * @param activity
	 * @param text The text to show.
	 * @param duration How long to display the message. Either Toast.LENGTH_SHORT or Toast.LENGTH_LONG
	 */
	public static void toastOnUiThread(final Activity activity, final String text, final int duration) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(activity, text, duration).show();
			}
		});
		
	}
	
}
