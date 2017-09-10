/*
 * Copyright (c) 2012-2013 NetEase, Inc. and other contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.example.michael.avdtest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;


/**
 * TODO Implement settings for saving options like setTime
 */
public class SettingsActivity extends Activity {

	private static final String LOG_TAG = "Energy-"
			+ SettingsActivity.class.getSimpleName();

	private CheckBox chkFloat;
	private EditText edtTime;
	private String time;
	private String settingTempFile;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(LOG_TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);

		Intent intent = this.getIntent();
		settingTempFile = intent.getStringExtra("settingTempFile");

		chkFloat = (CheckBox) findViewById(R.id.floating);
		edtTime = (EditText) findViewById(R.id.time);
		Button btnSave = (Button) findViewById(R.id.save);
		boolean floatingTag = true;

		try {
			Properties properties = new Properties();
			properties.load(new FileInputStream(settingTempFile));
			String interval = properties.getProperty("interval").trim();
			String isfloat = properties.getProperty("isfloat").trim();
			time = "".equals(interval) ? "0.001" : interval;
			floatingTag = "false".equals(isfloat) ? false : true;
		} catch (FileNotFoundException e) {
			Log.e(LOG_TAG, "FileNotFoundException: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(LOG_TAG, "IOException: " + e.getMessage());
			e.printStackTrace();
		}
		edtTime.setText(time);
		chkFloat.setChecked(floatingTag);
		// edtTime.setInputType(InputType.TYPE_CLASS_NUMBER);
		btnSave.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				time = edtTime.getText().toString().trim();
				
				if (!isNumeric(time)) {
					Toast.makeText(SettingsActivity.this, "input is wrong, try again",
							Toast.LENGTH_LONG).show();
					edtTime.setText("");
				} else if ("".equals(time) || Long.parseLong(time) == 0) {
					Toast.makeText(SettingsActivity.this, "input is empty, try again",
							Toast.LENGTH_LONG).show();
					edtTime.setText("");
				} else if (Integer.parseInt(time) > 600) {
					Toast.makeText(SettingsActivity.this, "input has surpass 600, try again",
							Toast.LENGTH_LONG).show();
				} else {
					try {
						Properties properties = new Properties();
						properties.setProperty("interval", time);
						properties.setProperty("isfloat",
								chkFloat.isChecked() ? "true" : "false");
						FileOutputStream fos = new FileOutputStream(
								settingTempFile);
						properties.store(fos, "Setting Data");
						fos.close();
						Toast.makeText(SettingsActivity.this, "save successfully",
								Toast.LENGTH_LONG).show();
						Intent intent = new Intent();
						setResult(Activity.RESULT_FIRST_USER, intent);
						SettingsActivity.this.finish();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	@Override
	public void finish() {
		super.finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	
	/**
	 * is input a number.
	 * 
	 * @param inputStr
	 *            input string
	 * @return true is numeric
	 */
	private boolean isNumeric(String inputStr) {
		for (int i = inputStr.length(); --i >= 0;) {
			if (!Character.isDigit(inputStr.charAt(i))) {
				return false;
			}
		}
		return true;
	}

}
