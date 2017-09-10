package com.example.michael.avdtest.util_ity;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import com.example.michael.avdtest.MonitorService;

import android.content.Context;
import android.util.Log;


public class CpuInfo {

	private static final String LOG_TAG = "Energy-" + CpuInfo.class.getSimpleName();

	private Context context;
	private long idleCpu;
	private double littleCpu;
	private double bigCpu;
	private long[] idleCpus = new long[8];
	private long[] cpus = new long[8];
	private long totalCpu;
	private SimpleDateFormat formatterFile;
	private MemoryInfo mi;
	private long totalMemorySize;
	private boolean isInitialStatics = true;
	private ArrayList<String> cpuUsedRatio;
	private long totalCpu2;
	private long[] cpu_2s = new long[8];
	private long idleCpu_2;
	private long[] idleCpu_2s = new long[8];
	private String[] CpuRatios = new String[8];
	private String totalCpuRatio = "";
	private String cpuFrequencyLittle;
	private String cpuFrequencyBig;
	private String gpuFrequency;
	private String gpuBandwidth;
	private RootFileGrabber root;

	public CpuInfo(Context context) {
		this.context = context;
		formatterFile = new SimpleDateFormat("HH:mm:ss SSS");
		mi = new MemoryInfo();
		totalMemorySize = mi.getTotalMemory();
		cpuUsedRatio = new ArrayList<String>();

		root = new RootFileGrabber();
		root.initializeInteractiveShell();
	}

	/**
	 * read the status of CPU.
	 *
	 */
	public void readCpuStat() {

		//monitor Total CPU utilization as well as individual core utilization
		ArrayList<String> cpuInfo = root.runShellCommand("cat /proc/stat");
		if(cpuInfo != null && !cpuInfo.isEmpty()) {
			String[] toks = cpuInfo.get(0).split("\\s+");
			idleCpu = Long.parseLong(toks[4]) + Long.parseLong(toks[5]) ;
			totalCpu = Long.parseLong(toks[1]) + Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4])
					+ Long.parseLong(toks[6]) + Long.parseLong(toks[5]) + Long.parseLong(toks[7]);
			for (int i = 0; i < 8; i++) {
				toks = cpuInfo.get(i + 1).split("\\s+");
				idleCpus[i] = Long.parseLong(toks[4]) + Long.parseLong(toks[5]);
				cpus[i] = Long.parseLong(toks[1]) + Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4])
						+ Long.parseLong(toks[6]) + Long.parseLong(toks[5]) + Long.parseLong(toks[7]);
			}
		}

		//monitor CPU & GPU MHz
		ArrayList<String> cpuFreq= root.runShellCommand("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq");
		if(cpuFreq != null && !cpuFreq.isEmpty())
			cpuFrequencyLittle = cpuFreq.get(0);

		cpuFreq = root.runShellCommand("cat /sys/devices/system/cpu/cpu4/cpufreq/scaling_cur_freq");
		if(cpuFreq != null && !cpuFreq.isEmpty())
			cpuFrequencyBig = cpuFreq.get(0);

		/*cpuFreq = root.runShellCommand("cat /sys/devices/platform/e82c0000.mali/devfreq/gpufreq/cur_freq");
		if(cpuFreq != null && !cpuFreq.isEmpty())
			gpuFrequency = cpuFreq.get(0);*/

		cpuFreq = root.runShellCommand("cat /sys/devices/platform/e82c0000.mali/devfreq/gpufreq/mali_ondemand/utilisation");
		if(cpuFreq != null && !cpuFreq.isEmpty())
			gpuFrequency = cpuFreq.get(0);

		cpuFreq = root.runShellCommand("cat /sys/devices/platform/fffc0000.ddr_devfreq/devfreq/ddrfreq/ddr_bandwidth");
		if(cpuFreq != null && !cpuFreq.isEmpty())
			gpuBandwidth = cpuFreq.get(0);


	}

	/**
	 * get CPU name.
	 * 
	 * @return CPU name
	 */
	public String getCpuName() {
		try {
			RandomAccessFile cpuStat = new RandomAccessFile("/proc/cpuinfo", "r");
			String[] cpu = cpuStat.readLine().split(":");
			cpuStat.close();
			return cpu[1];
		} catch (IOException e) {
			Log.e(LOG_TAG, "IOException: " + e.getMessage());
		}
		return "";
	}

	/**
	 * reserve used ratio of total CPU, meanwhile collect
	 * network traffic.
	 *
	 * Also writes the CPU, GPU, and Battery information to the result .csv
	 * 
	 * @return network traffic ,used ratio of process CPU and total CPU in
	 *         certain interval
	 */
	public ArrayList<String> getCpuRatioInfo(String currentBatt, String temperature, String voltage, String currentPower) {

		DecimalFormat fomart = new DecimalFormat();
		fomart.setMaximumFractionDigits(2);
		fomart.setMinimumFractionDigits(2);

		readCpuStat();
		cpuUsedRatio.clear();

		try {
			String mDateTime2;
			Calendar cal = Calendar.getInstance();
			mDateTime2 = formatterFile.format(cal.getTime().getTime());

			if (isInitialStatics) {
				isInitialStatics = false;
			} else {
				double ratio = 0;
				for (int i = 0; i < 8; i++) {
					ratio = 100 * ((double) ((cpus[i] - idleCpus[i]) - (cpu_2s[i] - idleCpu_2s[i])) / (double) (totalCpu - totalCpu2));
					CpuRatios[i] = fomart.format(ratio);
					if(i<4)
						littleCpu += ratio;
					else
						bigCpu += ratio;
				}
				totalCpuRatio = fomart.format(100 * ((double) ((totalCpu - idleCpu) - (totalCpu2 - idleCpu_2)) / (double) (totalCpu - totalCpu2)));
				long freeMemory = mi.getFreeMemorySize(context);
				double fMemory = (double) freeMemory / 1024;
				String percent = "stat wrong";
				if (totalMemorySize != 0) {
					percent = fomart.format(( fMemory / (double) totalMemorySize) * 100);
				}

				if (isPositive(totalCpuRatio)) {
					MonitorService.bw.write(mDateTime2 + "," + percent + ","
							+ totalCpuRatio +"," + cpuFrequencyLittle+ "," + cpuFrequencyBig+ ","
							+ gpuFrequency +"," + gpuBandwidth + "," + currentBatt + "," + temperature
							+ "," + voltage +"," + currentPower
							+ "," + CpuRatios[0] + "," + CpuRatios[1] + "," + CpuRatios[2]
							+ "," + CpuRatios[3] + "," + fomart.format(littleCpu)
							+ "," + CpuRatios[4] + "," + CpuRatios[5]
							+ "," + CpuRatios[6] + "," + CpuRatios[7]
							+ "," + fomart.format(bigCpu) + "\r\n");

					totalCpu2 = totalCpu;
					idleCpu_2 = idleCpu;
					for (int i = 0; i < 8; i++) {
						cpu_2s[i] = cpus[i];
						idleCpu_2s[i] = idleCpus[i];
					}
					cpuUsedRatio.add(totalCpuRatio);
					littleCpu = 0;
					bigCpu = 0;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return cpuUsedRatio;
	}

	/**
	 * is text a positive number
	 * 
	 * @param text
	 * @return
	 */
	private boolean isPositive(String text) {
		Double num;
		try {
			num = Double.parseDouble(text);
		} catch (NumberFormatException e) {
			return false;
		}
		return num >= 0;
	}

}

