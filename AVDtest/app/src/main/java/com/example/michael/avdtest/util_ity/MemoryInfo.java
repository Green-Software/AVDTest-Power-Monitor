package com.example.michael.avdtest.util_ity;


import java.util.ArrayList;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.util.Log;

/**
 * operate memory information
 * 
 * @author andrewleo
 */
public class MemoryInfo {

	private RootFileGrabber root;

	private static final String LOG_TAG = "Energy-"
			+ MemoryInfo.class.getSimpleName();

	public MemoryInfo(){
		root = new RootFileGrabber();
		root.initializeInteractiveShell();
	}

	/**
	 * get total memory of certain device.
	 * 
	 * @return total memory of device
	 */
	public long getTotalMemory() {
		String memInfoPath = "/proc/meminfo";
		String readTemp = "";
		String memTotal = "";
		long memory = 0;

			ArrayList<String> memString= root.runShellCommand("cat " + memInfoPath);
			if ((readTemp = memString.get(0)) != null) {
				if (readTemp.contains("MemTotal")) {
					String[] total = readTemp.split(":");
					memTotal = total[1].trim();
				}
			}
			String[] memKb = memTotal.split(" ");
			memTotal = memKb[0].trim();
			memory = Long.parseLong(memTotal);
			Log.d(LOG_TAG, "memTotal: " + memTotal);

		return memory;
	}

	/**
	 * get free memory.
	 * 
	 * @return free memory of device
	 * 
	 */
	public long getFreeMemorySize(Context context) {
		ActivityManager.MemoryInfo outInfo = new ActivityManager.MemoryInfo();
		ActivityManager am = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		am.getMemoryInfo(outInfo);
		long avaliMem = outInfo.availMem;
		return avaliMem / 1024;
	}

	/**
	 * get the memory of process with certain pid.
	 * 
	 * @param pid
	 *            pid of process
	 * @param context
	 *            context of certain activity
	 * @return memory usage of certain process
	 */
	public int getPidMemorySize(int pid, Context context) {
		ActivityManager am = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		int[] myMempid = new int[] { pid };
		Debug.MemoryInfo[] memoryInfo = am.getProcessMemoryInfo(myMempid);
		memoryInfo[0].getTotalSharedDirty();

		// int memSize = memoryInfo[0].dalvikPrivateDirty;
		// TODO PSS
		int memSize = memoryInfo[0].getTotalPss();
		// int memSize = memoryInfo[0].getTotalPrivateDirty();
		return memSize;
	}

	/**
	 * get the sdk version of phone.
	 * 
	 * @return sdk version
	 */
	public String getSDKVersion() {
		return android.os.Build.VERSION.RELEASE;
	}

	/**
	 * get phone type.
	 * 
	 * @return phone type
	 */
	public String getPhoneType() {
		return android.os.Build.MODEL;
	}

}

