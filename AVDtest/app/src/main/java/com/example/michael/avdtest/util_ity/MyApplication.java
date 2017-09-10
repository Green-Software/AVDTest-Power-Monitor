
package com.example.michael.avdtest.util_ity;

import android.app.Application;
import android.view.WindowManager;

/**
 * my application class
 * 
 * @author andrewleo
 */
public class MyApplication extends Application {

	private WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();

	public WindowManager.LayoutParams getMywmParams() {
		return wmParams;
	}
}
