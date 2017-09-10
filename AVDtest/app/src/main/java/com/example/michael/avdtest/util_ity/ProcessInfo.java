package com.example.michael.avdtest.util_ity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;


public class ProcessInfo {

	private static final String LOG_TAG = "Energy_" + ProcessInfo.class.getSimpleName();

	private static final String PACKAGE_NAME = "com.example.michael.avdtest";

	/**
	 * get information of all running processes,including package name ,process
	 * name ,icon ,pid and uid.
	 * 
	 * @param context
	 *            context of activity
	 *
	 * @return List of all Apps able to be profiled
	 */
	public List<Programe> getRunningProcess(Context context) {
		Log.i(LOG_TAG, "get running processes");

		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> run = am.getRunningAppProcesses(); //<-- no longer supported in Android5.11

		PackageManager pm = context.getPackageManager();
		List<Programe> progressList = new ArrayList<Programe>();

		for (ApplicationInfo appinfo : getPackagesInfo(context)) {
			Programe programe = new Programe();

			//If the app is a system app or AVDTest, skip it
			if (!(((appinfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0) || ((appinfo.processName != null) && (appinfo.processName.equals(PACKAGE_NAME))))) {
                //for every task that's running, get their pid and uid
				for (RunningAppProcessInfo runningProcess : run) {
					String procn;
					if ((runningProcess.processName != null) && runningProcess.processName.equals(appinfo.processName)) {
						procn = runningProcess.processName;
						Log.i(LOG_TAG, "procn: " + procn);
						programe.setPid(runningProcess.pid);
						programe.setUid(runningProcess.uid);
						break;
					}
				}


				programe.setPackageName(appinfo.processName);
				programe.setProcessName(appinfo.loadLabel(pm).toString());
				programe.setIcon(appinfo.loadIcon(pm));
				progressList.add(programe);
			}
		}
		Collections.sort(progressList);
		return progressList;
	}

	/**
	 * get information of all applications.
	 * 
	 * @param context
	 *            context of activity
	 * @return packages information of all applications
	 */
	private List<ApplicationInfo> getPackagesInfo(Context context) {
		PackageManager pm = context.getApplicationContext().getPackageManager();
		List<ApplicationInfo> appList = pm.getInstalledApplications(0);
		return appList;
	}
}
