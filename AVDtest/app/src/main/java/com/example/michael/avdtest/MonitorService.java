package com.example.michael.avdtest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimerTask;
import java.util.Timer;

import com.example.michael.avdtest.util_ity.CurrentInfo;
import com.example.michael.avdtest.util_ity.MemoryInfo;
import com.example.michael.avdtest.util_ity.CpuInfo;
import com.example.michael.avdtest.util_ity.ProcessInfo;
import com.example.michael.avdtest.util_ity.RootFileGrabber;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import static java.lang.Runtime.getRuntime;

public class MonitorService extends Service{

	private static final String LOG_TAG = "GreenComputingProfiler";
    private static final int SET_TIME = 30*1000;//in milliseconds
    private static final int INIT_TIME = 2*1000;//in milliseconds
	private static boolean isServiceStop= false;
	public static boolean isStop = false;
	public static boolean isStart = false;
    private static boolean auto = false;
	private MemoryInfo memoryInfo;
	private DecimalFormat fomart;
	private CurrentInfo currentInfo;

	private String voltage;
	private String temperature;

	private static String packageName = "EnergyProfiler";
    private int delaytime;
    private CpuInfo cpuInfo;
    
    private boolean isFloating;
    
	TextView txtTotalMem;
	TextView txtUnusedMem;
	TextView txtTraffic;
	
	Button btnStop;
    Button btnSetTime;
	public static String resultFilePath;
	public static FileOutputStream out;
	public static OutputStreamWriter osw;
	public static BufferedWriter bw;
	public boolean isCSVCreated = false;
	
	private static final String START_TIME = "#startTime";
	
	private View viFloatingWindow;
	private float mTouchStartX;
	private float mTouchStartY;
	private float startX;
	private float startY;
	private float x;
	private float y;
	
	private WindowManager windowManager = null;
	private WindowManager.LayoutParams wmParams = null;
	
	private Handler handler = new Handler();
	private Timer timer = new Timer();

	private int getStartTimeCount = 0;
	private boolean isGetStartTime = true;
	private String startTime = "";
	private static final int MAX_START_TIME_COUNT = 1;
    
	
	public void onCreate(){
		Log.i(LOG_TAG,"OnCreate");
		super.onCreate();
		isServiceStop = false;
		isStart = false;
		isStop = false;
		memoryInfo = new MemoryInfo();
		fomart = new DecimalFormat();
		fomart.setMaximumFractionDigits(2);
		fomart.setMinimumFractionDigits(0);
		currentInfo = new CurrentInfo();
	}

	/**
	 * Runs when Intent is sent from Start()
	 *
	 * @param intent
	 *     intent that started the service
	 * @param startId
	 *
	 * Sets up the interface over the running app including
	 *   the buttons and data display text
	 *
	 * Starts the process to create result file
	 *
	 * initiates data collection
	 */
	public void onStart(Intent intent, int startId) {
        Log.i(LOG_TAG, "onStart");

        PendingIntent contentIntent = PendingIntent.getActivity(getBaseContext(), 0, new Intent(this, Start.class), 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentIntent(contentIntent).setWhen(System.currentTimeMillis()).setAutoCancel(true);
        startForeground(startId, builder.build());

        Bundle extras = intent.getExtras();
        if (extras != null) {
            if(extras.containsKey("packageName"))
                packageName = extras.getString("packageName");

            if (extras.containsKey("auto"))
                auto = extras.getBoolean("auto");
        }

        cpuInfo = new CpuInfo(getBaseContext());
        delaytime = 100;//time in ms between data collection, if too low, memory overflow
        isFloating = true;

        viFloatingWindow = LayoutInflater.from(this).inflate(R.layout.floating, null);
        txtUnusedMem = (TextView) viFloatingWindow.findViewById(R.id.memunused);
        txtTotalMem = (TextView) viFloatingWindow.findViewById(R.id.memtotal);
        txtTraffic = (TextView) viFloatingWindow.findViewById(R.id.traffic);

        txtUnusedMem.setTextColor(android.graphics.Color.RED);
        txtTotalMem.setTextColor(android.graphics.Color.GREEN);
        txtTraffic.setTextColor(android.graphics.Color.BLUE);

        btnStop = (Button) viFloatingWindow.findViewById(R.id.stop);
        btnSetTime = (Button) viFloatingWindow.findViewById(R.id.setTime);
        btnStop.setTextColor(android.graphics.Color.BLACK);
        btnSetTime.setTextColor(android.graphics.Color.BLACK);

        //if auto was pushed, start dataRefresh after INIT_TIME and kills after SET_TIME+INIT_TIME
        if (auto){
            btnStop.setText("Stop");
            btnSetTime.setVisibility(View.GONE);

            btnStop.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent();
                    intent.putExtra("isServiceStop", true);
                    intent.putExtra("packageName", packageName);
                    intent.setAction("com.example.action.MonitorService");
                    sendBroadcast(intent);
                    Toast.makeText(MonitorService.this, "the log file:" + resultFilePath, Toast.LENGTH_LONG).show();
                    stopSelf();
                }
            });

            createFloatingWindow();
            createResultCsv();

            //Wait for the app to be running before starting data collection
            handler.postDelayed(task,INIT_TIME);

            txtUnusedMem.setText("Recording Data...");
            isStart = true;
            timer.schedule(killer, SET_TIME+INIT_TIME);
        } else{
	    
	    	txtUnusedMem.setText("Press Start when ready");

            //Sets up on click event for "Start" button
			btnStop.setText("Start");
            btnSetTime.setText("Run for " + String.valueOf(SET_TIME/1000) + " sec");
	    	btnStop.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v){
                    if(isStart) {
                        Intent intent = new Intent();
                        intent.putExtra("isServiceStop", true);
						intent.putExtra("packageName", packageName);
                        intent.setAction("com.example.action.MonitorService");
                        sendBroadcast(intent);
                        Toast.makeText(MonitorService.this, "the log file:" + resultFilePath, Toast.LENGTH_LONG).show();
                        stopSelf();
                    } else {
                        txtUnusedMem.setText("Recording Data...");
                        isStart = true;
                        btnStop.setText("Stop");
                        btnSetTime.setVisibility(View.GONE);
                    }
                }
            });

            //Sets up on click event for "Run for SET_TIME" button
            btnSetTime.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v){
                    if(isStart) {
                        Intent intent = new Intent();
                        intent.putExtra("isServiceStop", true);
						intent.putExtra("packageName", packageName);

                        intent.setAction("com.example.action.MonitorService");
                        sendBroadcast(intent);
                        Toast.makeText(MonitorService.this, "the log file:" + resultFilePath, Toast.LENGTH_LONG).show();
                        stopSelf();
                    } else {
                        txtUnusedMem.setText("Recording Data...");
                        isStart = true;
                        timer.schedule(killer, SET_TIME+INIT_TIME);
                        btnStop.setVisibility(View.GONE);
                        btnSetTime.setText("Stop");
                    }
                }
            });

	        createFloatingWindow();
            createResultCsv();

            //Wait for the app to be running before starting data collection
            handler.postDelayed(task,INIT_TIME);
	    }
	}

	/**
	 *  Creates and adds header to the file where the data will be stored
	 *
	 *  resulting CSV will use commas as delimiters.
	 */
	private void createResultCsv(){
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
		String mDateTime;

		mDateTime = formatter.format(cal.getTime().getTime());
		if(android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)){
			resultFilePath = android.os.Environment.getExternalStorageDirectory()+File.separator+packageName + "_Result_" + mDateTime +".csv";
		}
		else{
			resultFilePath = getBaseContext().getFilesDir().getPath() +File.separator+ packageName + "_Result_" + mDateTime + ".csv";
		}
		try{

			resultFilePath = getBaseContext().getFilesDir().getPath() +File.separator+ packageName + "_Result_" + mDateTime + ".csv";
			String resultFileName = packageName + "_Result_" + mDateTime + ".csv";
			out = getBaseContext().openFileOutput(resultFileName, 0);
			osw = new OutputStreamWriter(out,"UTF-8");
			bw = new BufferedWriter(osw);

			bw.write(packageName + "\r\n");
			bw.write("Time" + "," + "MemOccupy %Free"  + ","  + "CpuTotalUsage(%)" + ","
					+ "Little CPU Freq(Hz)"+ "," + "Big CPU Freq(Hz)" + "," + "GPU Utilization(%)" + ","
					+ "GPUBandwidth(MB/s)" + "," + "Current(mA)" + "," + "Temperature(C)" + ","
					+ "Volatge(V)" + "," + "Power(W)" + "," + "cpu0 util" + "," + "cpu1 util" + ","
					+ "cpu2 util" + "," + "cpu3 util" + "," + "LITTLE util" + ","
					+ "cpu4 util" + "," + "cpu5 util" + "," + "cpu6 util" + "," + "cpu7 util" + ","
					+ "Big util" + "\r\n");
		}catch(IOException e){
			Log.e(LOG_TAG, e.getMessage());
		}
		isCSVCreated = true;
	}

	/**
	 * Set up Profiler interface and display over running app
     *
     *
	 */
	private void createFloatingWindow() {
		SharedPreferences shared = getSharedPreferences("float_flag", Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = shared.edit();
		editor.putInt("float", 1);
		editor.commit();
		windowManager = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
		wmParams = new WindowManager.LayoutParams();
		wmParams.type = WindowManager.LayoutParams.TYPE_TOAST;
		wmParams.flags |= 8;
		wmParams.gravity = Gravity.START | Gravity.TOP;
		wmParams.x = 0;
		wmParams.y = 0;
		wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
		wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
		wmParams.format = 1;
		windowManager.addView(viFloatingWindow, wmParams);

        //Allows the window to be moved around by dragging with the finger
		viFloatingWindow.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				x = event.getRawX();
				y = event.getRawY() - 25;
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					startX = x;
					startY = y;
					mTouchStartX = event.getX();
					mTouchStartY = event.getY();
					Log.d("startP", "startX" + mTouchStartX + "====startY" + mTouchStartY);
					break;
				case MotionEvent.ACTION_MOVE:
					updateViewPosition();
					break;

				case MotionEvent.ACTION_UP:
					updateViewPosition();
					mTouchStartX = mTouchStartY = 0;
					break;
				}
				return true;
			}
		});

	}

	private void updateViewPosition() {
		wmParams.x = (int) (x - mTouchStartX);
		wmParams.y = (int) (y - mTouchStartY);
		windowManager.updateViewLayout(viFloatingWindow, wmParams);
	}

	/**
	 * Executes and updates the data on screen and in the file
	 * then runs again after delaytime
	 *
	 * Only records data if the app chosen to be profiled is running.
	 *
	 * Stops running when isServiceStop == false
	 */
	private Runnable task = new Runnable() {

		public void run() {
			if(isStart) {
				if (!isServiceStop) {
					dataRefresh();
					handler.postDelayed(this, delaytime);
					if (isFloating) {
						windowManager.updateViewLayout(viFloatingWindow, wmParams);
					}
					// get app start time from logcat on every task running
					getStartTimeFromLogcat();
				} else {
					Intent intent = new Intent();
					intent.putExtra("isServiceStop", true);
					intent.putExtra("packageName", packageName);
					intent.setAction("com.example.action.MonitorService");
					sendBroadcast(intent);
					stopSelf();
				}
			} else {
				handler.postDelayed(this, delaytime);
			}
		}
	};

	/**
	 * task that runs after the timer for setTimeBtn has finished
	 *
	 * stops data recording and kills the service
	 */
	private TimerTask killer = new TimerTask() {
		@Override
		public void run() {
			isServiceStop = true;
			return;
		}
	};

	/**
	 *First time data is recorded, grab the exact time the app started by
	 *  parsing it from the logcat output
	 */
	private void getStartTimeFromLogcat() {
		if (!isGetStartTime || getStartTimeCount >= MAX_START_TIME_COUNT) {
			return;
		}
		try {
			// filter logcat by Tag:ActivityManager and Level:Info
			String logcatCommand = "logcat -v time -d ActivityManager:I *:S";
			Process process = getRuntime().exec(logcatCommand);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			StringBuilder strBuilder = new StringBuilder();
			String line = "";

			while ((line = bufferedReader.readLine()) != null) {
				strBuilder.append(line);
				strBuilder.append("\r\n");
				String regex = ".*Displayed.*" + "" + ".*\\+(.*)ms.*";
				Log.d("my logs", regex);
				if (line.matches(regex)) {
					Log.w("my logs", line);
					if (line.contains("total")) {
						line = line.substring(0, line.indexOf("total"));
					}
					startTime = line.substring(line.lastIndexOf("+") + 1, line.lastIndexOf("ms") + 2);

					Toast.makeText(MonitorService.this, "StartTime:" + startTime, Toast.LENGTH_LONG).show();
					isGetStartTime = false;
					break;
				}
			}
			getStartTimeCount++;
		} catch (IOException e) {
			Log.d(LOG_TAG, e.getMessage());
		}
	}

	/**
	 * Calculates the current, voltage, power, memory, and temperature
	 * then sends them to the CpuInfo to write to the file
	 *
	 * Updates the display with the most recent current and utilization info
	 */
	private void dataRefresh() {
		if(isCSVCreated){
			Double current = currentInfo.getCurrentValue();
			Double volts = currentInfo.getVoltageValue()/1000000;
			Double temp = currentInfo.getTemperatureValue();
			voltage = String.valueOf(volts);
			temperature = String.valueOf(temp);
			String currentBatt = String.valueOf(current);
			DecimalFormat df = new DecimalFormat("0.00");
			String currentPower = df.format(current/1000 * volts);
		

			ArrayList<String> processInfo = cpuInfo.getCpuRatioInfo(currentBatt, temperature, voltage, currentPower);
			if (isFloating) {
				String totalCpuRatio = "0";

				if (!processInfo.isEmpty()) {
					totalCpuRatio = processInfo.get(0);

					if (totalCpuRatio != null) {
						txtTotalMem.setText("Total_CPU: " + totalCpuRatio + "%");
						txtTraffic.setText("Current:" + currentBatt);
					}

					if ("0.00".equals(totalCpuRatio)) {
						closeOpenedStream();
						isServiceStop = true;
						return;
					}
				}
			}
		}
	}
	
	public static void closeOpenedStream() {
		try {
			if (bw != null) {
				
				bw.close();
			}
			if (osw != null)
				osw.close();
			if (out != null)
				out.close();
		} catch (Exception e) {
			Log.e(LOG_TAG, e.getMessage());
		}
	}

	@Override
	public void onDestroy() {
		Log.i(LOG_TAG, "onDestroy");
		if (windowManager != null)
			windowManager.removeView(viFloatingWindow);
		handler.removeCallbacks(task);

		// replace the start time in file
		//if (!"".equals(startTime)) {
		//	replaceFileString(resultFilePath, START_TIME, "Start Time:" + startTime + "\r\n");
		//} else {
		//	replaceFileString(resultFilePath, START_TIME, "");
		//}

		closeOpenedStream();

		//move result file from app cache to documents folder
		try {
			RootFileGrabber.runShellCommand("cp \"" + resultFilePath + "\" /storage/emulated/0/documents");
		}
		catch (Exception e){
			Log.e(LOG_TAG, e.getMessage());
		}

		isStop = true;

		Toast.makeText(this, "The Result saved in: \"" + resultFilePath + "\"", Toast.LENGTH_LONG).show();

		RootFileGrabber.closeInteractiveShell();
		super.onDestroy();
		stopForeground(true);
	}
	
	private void replaceFileString(String filePath, String replaceType, String replaceString) {
		try {
			File file = new File(resultFilePath);
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = "", oldtext = "";
			while ((line = reader.readLine()) != null) {
				oldtext += line + "\r\n";
			}
			reader.close();
			// replace a word in a file
			String newtext = oldtext.replaceAll(replaceType, replaceString);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "UTF-8"));
			writer.write(newtext);
			writer.close();
		} catch (IOException e) {
			Log.d(LOG_TAG, e.getMessage());
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
