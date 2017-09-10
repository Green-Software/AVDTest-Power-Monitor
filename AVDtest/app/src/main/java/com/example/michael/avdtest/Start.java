package com.example.michael.avdtest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import com.example.michael.avdtest.util_ity.ProcessInfo;
import com.example.michael.avdtest.util_ity.Programe;
import com.example.michael.avdtest.util_ity.RootFileGrabber;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class Start extends Activity {
    
	private static final String LOG_TAG = "Green Computing Lab" + Start.class.getSimpleName();
	private static final int TIMEOUT = 20000;
	private ListView programLst;
	private Button btn;
	private Button btnAuto;
	private Intent monitorIntent;
	boolean isRadioChecked = false;
	private String packageName, settingTempFile, processName;
	private ProcessInfo processInfo;
	private RootFileGrabber root;
	private boolean isServiceStop = false;
	private UpdateReceiver receiver;

    /**
     * First method called when app is opened.
     * sets up control for the first screen
     *
     * @param savedInstanceState
     */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start);

		createNewFile();

		root = new RootFileGrabber();
		root.initializeInteractiveShell();

		processInfo = new ProcessInfo();
		programLst = (ListView)findViewById(R.id.programLst);
		btn = (Button)findViewById(R.id.btn);
		btnAuto = (Button)findViewById(R.id.btnAuto);

		//sets on click event for "Begin Test" button
		btn.setOnClickListener(new OnClickListener(){	
			@Override
			public void onClick(View v){
				
				monitorIntent = new Intent();
				monitorIntent.setClass(Start.this,MonitorService.class);
				if ("Begin Test".equals(btn.getText().toString())) {
					if(isRadioChecked){
						String startAct = " ";
						Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
					    Log.d(LOG_TAG, "Package Name: " + packageName);
					    try{
					    	Runtime.getRuntime().exec("logcat -c");// get time
					    }catch(IOException e){
					    	Log.d(LOG_TAG, e.getMessage());
					    }
					    try{
						    startAct = intent.resolveActivity(getPackageManager()).getShortClassName();
					        startActivity(intent);
					    }catch(Exception e){
						    Toast.makeText(Start.this,"This program can't start",Toast.LENGTH_LONG).show();
					        return;
					    }
						monitorIntent.putExtra("packageName", processName);
                        monitorIntent.putExtra("auto", false);
					    startService(monitorIntent);
					    btn.setText("Stop Test");
					} else {
					    Toast.makeText(Start.this,"Please choose App you want to test",Toast.LENGTH_LONG).show();
				    }
				} else {
					btn.setText("Begin Test");
					Toast.makeText(Start.this, "Test outcome file��" + MonitorService.resultFilePath, Toast.LENGTH_LONG).show();
					stopService(monitorIntent);
				}

			}
		});

        //sets on click event for "Auto" button
		btnAuto.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				monitorIntent = new Intent();
				monitorIntent.setClass(Start.this, MonitorService.class);
				if (isRadioChecked) {
					String startAct = " ";
					Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
					Log.d(LOG_TAG, "Package Name: " + packageName);
					try {
						Runtime.getRuntime().exec("logcat -c");// get time
					} catch (IOException e) {
						Log.d(LOG_TAG, e.getMessage());
					}
					try {
						startAct = intent.resolveActivity(getPackageManager()).getShortClassName();
						startActivity(intent);
					} catch (Exception e) {
						Toast.makeText(Start.this, "This program can't start", Toast.LENGTH_LONG).show();
						return;
					}
					monitorIntent.putExtra("packageName", processName);
					monitorIntent.putExtra("auto", true);
					startService(monitorIntent);
					btn.setText("Stop Test");
				} else {
					Toast.makeText(Start.this, "Please choose App you want to test", Toast.LENGTH_LONG).show();
				}
			}
		});

        //creates list of available programs to choose from
		programLst.setAdapter(new ListAdapter());
	}

	/**
	 * Sets up Program list that will be displayed on screen
	 *
	 */
	private class ListAdapter extends BaseAdapter{
	    List<Programe> programe;
	    int tempPosition = -1;
	
        class ViewHolder{
	      TextView txtAppName;
	      ImageView appIcon;
	      RadioButton rBtn;
  		}

  		/**
  		 * On instantiation, use ProcessInfo to get a list of available apps to show in the list.
  		 */
  		public ListAdapter(){
	        programe = processInfo.getRunningProcess(getBaseContext());
        }

		@Override
		public int getCount(){
		    return programe.size();
	    }
		@Override
		public Object getItem(int position) {
		    return programe.get(position);
	    }

		public long getItemId(int position){
		return position;
	}

		public View getView(int position, View convertView, ViewGroup parent){
			ViewHolder viewholder = new ViewHolder();
			final int i = position;
			convertView = Start.this.getLayoutInflater().inflate(R.layout.list_item, null);
		    viewholder.appIcon = (ImageView) convertView.findViewById(R.id.image);
		    viewholder.txtAppName = (TextView) convertView.findViewById(R.id.text);
			viewholder.rBtn = (RadioButton)convertView.findViewById(R.id.rb);
	    	viewholder.rBtn.setId(position);
	    	viewholder.rBtn.setOnCheckedChangeListener(new OnCheckedChangeListener() {
	    	@Override
	    	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
	    	    if(isChecked){
	    	    	isRadioChecked = true;
	    	    	if(tempPosition != -1){
	    	    		RadioButton tempButton = (RadioButton) findViewById(tempPosition);
	    	    	    if((tempButton != null)&&(tempPosition != i)){	
	    	    	    	tempButton.setChecked(false);
	    	    	    }
	    	    	}
	    	    	tempPosition = buttonView.getId();
	    	    	packageName = programe.get(tempPosition).getPackageName();
					processName = programe.get(tempPosition).getProcessName();
	    	    }
	    		}
	    	});
	    	if(tempPosition == position){
	    		if(!viewholder.rBtn.isChecked())
	    			viewholder.rBtn.setChecked(true);
	   		}
	    	Programe pr = (Programe) programe.get(position);
	    	viewholder.appIcon.setImageDrawable(pr.getIcon());
	    	viewholder.txtAppName.setText(pr.getProcessName());
	    	return convertView;
		}
	}

    /**
     * Creates UpdateReceiver to check when a given test is finished, so another test can
     * be started without needed to restart the app
     */
    public class UpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            isServiceStop = intent.getExtras().getBoolean("isServiceStop");
            if (isServiceStop) {
                btn.setText("Begin Test");
            }
        }
    }

    @Override
    protected void onStart() {
        Log.d(LOG_TAG, "onStart");
        receiver = new UpdateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.action.MonitorService");
        this.registerReceiver(receiver, filter);
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onResume");
        if (MonitorService.isStop) {
            btn.setText("Begin Test");
        }
    }

    /**
     * Create local file to save settings data
     *
     * TODO implement into Mate9 version
     */
	private void createNewFile() {
		Log.i(LOG_TAG, "create new file to save setting data");
		settingTempFile = getBaseContext().getFilesDir().getPath() + "\\MonitorSettings.properties";
		Log.i(LOG_TAG, "settingFile = " + settingTempFile);
		File settingFile = new File(settingTempFile);
		if (!settingFile.exists()) {
			try {
				settingFile.createNewFile();
				Properties properties = new Properties();
				properties.setProperty("interval", "1");
				properties.setProperty("isfloat", "true");
				FileOutputStream fos = new FileOutputStream(settingTempFile);
				properties.store(fos, "Setting Data");
				fos.close();
			} catch (IOException e) {
				Log.d(LOG_TAG, "create new file exception :" + e.getMessage());
			}
		}
	}

	public boolean onKeyDown(int keyCode , KeyEvent event){
		if(keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount()==0){
			showDialog(0);
		}
		return super.onKeyDown(keyCode,event);
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, Menu.FIRST, 0, "Exit").setIcon(android.R.drawable.ic_menu_delete);
		menu.add(0, Menu.FIRST, 1, "Setting").setIcon(android.R.drawable.ic_menu_directions);
		return true;
	}

	protected Dialog onCreateDialog(int id){
		switch(id) {
			case 0:
				return new AlertDialog.Builder(this).setTitle("Want to close ? ").setPositiveButton("Yes", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						if(monitorIntent != null){
							Log.d(LOG_TAG,"stop service");
							stopService(monitorIntent);
						}
						//Log.d(LOG_TAG,"exit Energy Profiler");
						//Service.closeOpenedStream();
						finish();
						System.exit(0);
					}
				}).setNegativeButton("No",null).create();
			default:
				return null;
		}
	}
}
