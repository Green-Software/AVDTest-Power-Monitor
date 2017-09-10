package com.example.michael.avdtest.util_ity;

import java.io.File;
import java.util.Locale;
import java.util.ArrayList;



import android.os.Build;
import android.util.Log;


public class CurrentInfo {
	private static final String LOG_TAG = "Energy-CurrentInfo";
	private static final String BUILD_MODEL = Build.MODEL.toLowerCase(Locale.ENGLISH);

	//The location of the file with the current battery charge is different on many devices
	//  this has a list of the known locations and picks the first one that works.
	private static final String[] CURRENT_NOW = {"/sys/class/power_supply/ab8500_fg/current_now",
			"/sys/class/power_supply/android-battery/current_now",
			"/sys/class/power_supply/battery/batt_attr_text",
			"/sys/class/power_supply/bq_bk_battery/current_now",
			"/sys/class/power_supply/battery/batt_chg_current",
			"/sys/class/power_supply/battery/batt_current",
			"/sys/class/power_supply/battery/batt_current_adc",
			"/sys/class/power_supply/battery/batt_current_now",
			"/sys/class/power_supply/battery/BatteryAverageCurrent",
			"/sys/class/power_supply/battery/charger_current",
			"/sys/class/power_supply/battery/current_avg",
			"/sys/class/power_supply/battery/current_max",
			"/sys/class/power_supply/battery/current_now",
			"/sys/class/power_supply/Battery/current_now",
			"/sys/class/power_supply/battery/smem_text",
			"/sys/class/power_supply/bq27520/current_now",
			"/sys/class/power_supply/da9052-bat/current_avg",
			"/sys/class/power_supply/ds2784-fuelgauge/current_now",
			"/sys/class/power_supply/max17042-0/current_now",
			"/sys/class/power_supply/max170xx_battery/current_now",
			"/sys/devices/platform/battery/power_supply/battery/BatteryAverageCurrent",
			"/sys/devices/platform/battery/power_supply/battery/power_supply/Battery/current_now",
			"/sys/devices/platform/cpcap_battery/power_supply/usb/current_now",
			"/sys/devices/platform/ds2784-battery/getcurrent",
			"/sys/devices/platform/i2c-adapter/i2c-0/0-0036/power_supply/battery/current_now",
			"/sys/devices/platform/i2c-adapter/i2c-0/0-0036/power_supply/ds2746-battery/current_now",
			"/sys/devices/platform/msm-charger/power_supply/battery_gauge/current_now",
			"/sys/devices/platform/mt6320-battery/power_supply/battery/BatteryAverageCurrent",
			"/sys/devices/platform/mt6329-battery/FG_Battery_CurrentConsumption",
			"/sys/EcControl/BatCurrent"};

	private static final String VOLTAGE_NOW = "/sys/devices/platform/battery/power_supply/Battery/voltage_now";
	private static final String TEMPERATURE = "/sys/devices/platform/battery/power_supply/Battery/temp";

	private RootFileGrabber root;


	public CurrentInfo() {
		root = new RootFileGrabber();
		root.initializeInteractiveShell();
	}

	public Double getCurrentValue() {
		File f = null;
		String filePath = null;
		Log.d(LOG_TAG, BUILD_MODEL);
		for (int i = 0; i < 28; i++) {
			f = new File(CURRENT_NOW[i]);
			if (f.exists()) {
				filePath = CURRENT_NOW[i];
			}
		}


		ArrayList<String> result = new ArrayList<String>();
		result = root.runShellCommand("cat " + filePath);

		if (result != null && !result.isEmpty())
			return Double.valueOf(result.get(0));
		else
			return 0.0;

	}

	public Double getVoltageValue() {
		File f = null;
		Log.d(LOG_TAG, BUILD_MODEL);
		f = new File(VOLTAGE_NOW);
		if (f.exists()) {

			ArrayList<String> result = new ArrayList<String>();
			result = root.runShellCommand("cat " + VOLTAGE_NOW);

			if (result != null && !result.isEmpty())
				return Double.valueOf(result.get(0));
			else
				return 0.0;

		}
		return 0.0;
	}

	public Double getTemperatureValue() {
		File f = null;
		Log.d(LOG_TAG, BUILD_MODEL);
		f = new File(TEMPERATURE);
		if (f.exists()) {

			ArrayList<String> result = new ArrayList<String>();
			result = root.runShellCommand("cat " + TEMPERATURE);

			if (result != null && !result.isEmpty())
				return Double.valueOf(result.get(0));
			else
				return 0.0;

		}
		return 0.0;
	}
}


