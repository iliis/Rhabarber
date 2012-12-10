/*
 * Copyright 2011, Qualcomm Innovation Center, Inc.
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.floern.rhabarber.network;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;

import java.util.List;

import com.floern.rhabarber.R;

public class DialogBuilder {
	private static final String TAG = "Dialogs";

	// done
	public Dialog createUseJoinDialog(final Activity activity,
			final NetworkController application) {
		Log.i(TAG, "createUseJoinDialog()");
		final Dialog dialog = new Dialog(activity);
		dialog.requestWindowFeature(dialog.getWindow().FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.usejoindialog);

		ArrayAdapter<String> channelListAdapter = new ArrayAdapter<String>(
				activity, android.R.layout.test_list_item);
		final ListView channelList = (ListView) dialog
				.findViewById(R.id.useJoinChannelList);
		channelList.setAdapter(channelListAdapter);

		List<String> channels = application.getFoundChannels();
		for (String channel : channels) {
			int lastDot = channel.lastIndexOf('.');
			if (lastDot < 0) {
				continue;
			}
			channelListAdapter.add(channel.substring(lastDot + 1));
		}
		channelListAdapter.notifyDataSetChanged();

		channelList.setOnItemClickListener(new ListView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				String name = channelList.getItemAtPosition(position)
						.toString();
				application.useSetChannelName(name);
				application.useJoinChannel();
				/*
				 * Android likes to reuse dialogs for performance reasons. If we
				 * reuse this one, the list of channels will eventually be wrong
				 * since it can change. We have to tell the Android application
				 * framework to forget about this dialog completely.
				 */
				activity.removeDialog(UseActivity.DIALOG_JOIN_ID);
			}
		});

		Button cancel = (Button) dialog.findViewById(R.id.useJoinCancel);
		cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				/*
				 * Android likes to reuse dialogs for performance reasons. If we
				 * reuse this one, the list of channels will eventually be wrong
				 * since it can change. We have to tell the Android application
				 * framework to forget about this dialog completely.
				 */
				activity.removeDialog(UseActivity.DIALOG_JOIN_ID);
			}
		});

		return dialog;
	}

	public Dialog createUseLeaveDialog(Activity activity,
			final NetworkController application) {
		Log.i(TAG, "createUseLeaveDialog()");
		final Dialog dialog = new Dialog(activity);
		dialog.requestWindowFeature(dialog.getWindow().FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.useleavedialog);

		Button yes = (Button) dialog.findViewById(R.id.useLeaveOk);
		yes.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				application.useLeaveChannel();
				application.useSetChannelName("Not set");
				dialog.cancel();
			}
		});

		Button no = (Button) dialog.findViewById(R.id.useLeaveCancel);
		no.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				dialog.cancel();
			}
		});

		return dialog;
	}

	// Done
	public Dialog createHostNameDialog(Activity activity,
			final NetworkController application) {
		Log.i(TAG, "createHostNameDialog()");
		final Dialog dialog = new Dialog(activity);
		dialog.requestWindowFeature(dialog.getWindow().FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.hostnamedialog);

		final EditText channel = (EditText) dialog
				.findViewById(R.id.hostNameChannel);
		channel.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			public boolean onEditorAction(TextView view, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_NULL
						&& event.getAction() == KeyEvent.ACTION_UP) {
					String name = view.getText().toString();
					application.hostSetChannelName(name);
					application.hostInitChannel();
					dialog.cancel();
				}
				return true;
			}
		});

		Button okay = (Button) dialog.findViewById(R.id.hostNameOk);
		okay.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				String name = channel.getText().toString();
				application.hostSetChannelName(name);
				application.hostInitChannel();
				dialog.cancel();
			}
		});

		Button cancel = (Button) dialog.findViewById(R.id.hostNameCancel);
		cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				dialog.cancel();
			}
		});

		return dialog;
	}

	// done
	public Dialog createHostStartDialog(Activity activity,
			final NetworkController application) {
		Log.i(TAG, "createHostStartDialog()");
		final Dialog dialog = new Dialog(activity);
		dialog.requestWindowFeature(dialog.getWindow().FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.hoststartdialog);

		Button yes = (Button) dialog.findViewById(R.id.hostStartOk);
		yes.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				application.hostStartChannel();
				dialog.cancel();
			}
		});

		Button no = (Button) dialog.findViewById(R.id.hostStartCancel);
		no.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				dialog.cancel();
			}
		});

		return dialog;
	}

	// done
	public Dialog createHostStopDialog(Activity activity,
			final NetworkController application) {
		Log.i(TAG, "createHostStopDialog()");
		final Dialog dialog = new Dialog(activity);
		dialog.requestWindowFeature(dialog.getWindow().FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.hoststopdialog);

		Button yes = (Button) dialog.findViewById(R.id.hostStopOk);
		yes.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				application.hostStopChannel();
				dialog.cancel();
			}
		});

		Button no = (Button) dialog.findViewById(R.id.hostStopCancel);
		no.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				dialog.cancel();
			}
		});

		return dialog;
	}

	public static Dialog createAllJoynErrorDialog(Activity activity,
			final NetworkController application) {
		return (new DialogFragment() {
			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				// Use the Builder class for convenient dialog construction
				AlertDialog.Builder builder = new AlertDialog.Builder(
						getActivity());
				builder.setMessage(application.getErrorString())
						.setPositiveButton(R.string.ok,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										getDialog().cancel();
									}
								});
				// Create the AlertDialog object and return it
				return builder.create();
			}
		}).getDialog();
	}

	public static Dialog createJoinDialog(Activity activity,
			final NetworkController application) {
		return (new DialogFragment() {
			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						getActivity());
				// Get the layout inflater
				LayoutInflater inflater = getActivity().getLayoutInflater();

				// Inflate and set the layout for the dialog
				// Pass null as the parent view because its going in the dialog
				// layout
				builder.setView(inflater.inflate(R.layout.dialog_join, null))
				// Add action buttons
						.setNegativeButton(R.string.cancel,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										getDialog().cancel();
									}
								});
				return builder.create();
			}

			@Override
			public void onStart() {
				super.onStart();

			}

			@Override
			public void onCancel(DialogInterface dialog) {
				super.onCancel(dialog);
			}
		}).getDialog();
	}

	public static DialogFragment createQuitDialog(Activity activity,
			final NetworkController application) {
		return new DialogFragment() {
			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				// Use the Builder class for convenient dialog construction
				AlertDialog.Builder builder = new AlertDialog.Builder(
						getActivity());
				builder.setMessage(R.string.dialog_quit_question)
						.setPositiveButton(R.string.yes,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										application.quit();
										getDialog().cancel();
									}
								})
						.setNegativeButton(R.string.no,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										getDialog().cancel();
									}
								});
				// Create the AlertDialog object and return it
				return builder.create();
			}
		};
	}

}
