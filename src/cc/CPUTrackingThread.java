package cc;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public abstract class CPUTrackingThread extends Thread {

	private ThreadMXBean bean;
	private long cpuTime;

	public CPUTrackingThread() {
		super();
		cpuTime = 0;
		bean = ManagementFactory.getThreadMXBean();
	}

	public abstract void runReal();

	public void run() {
		runReal();
		cpuTime = bean.getCurrentThreadCpuTime();
	}

	/**
	 * @return the cpuTime
	 */
	public long getCpuTime() {
		return cpuTime;
	}

}
