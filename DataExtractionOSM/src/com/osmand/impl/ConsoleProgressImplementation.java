package com.osmand.impl;

import java.text.MessageFormat;

import com.osmand.Algoritms;
import com.osmand.IProgress;

public class ConsoleProgressImplementation implements IProgress {
	public static double deltaPercentsToPrint = 3.5; 
	
	String currentTask;
	int work;
	int currentDone;
	double delta;
	private long previousTaskStarted = 0;
	
	double lastPercentPrint = 0;
	public ConsoleProgressImplementation(){
		delta = deltaPercentsToPrint;
	}
	
	public ConsoleProgressImplementation(double deltaToPrint){
		delta = deltaToPrint;
	}
	
	@Override
	public void finishTask() {
		System.out.println("Task " + currentTask + " is finished "); //$NON-NLS-1$ //$NON-NLS-2$
		this.currentTask = null;
		
	}

	@Override
	public boolean isIndeterminate() {
		return work == -1;
	}

	@Override
	public void progress(int deltaWork) {
		this.currentDone += deltaWork;
		printIfNeeded();
	}
	
	private void printIfNeeded() {
		if(getCurrentPercent() - lastPercentPrint >= delta){
			System.out.println(MessageFormat.format("Done {0} %.", getCurrentPercent())); //$NON-NLS-1$
			this.lastPercentPrint = getCurrentPercent();
		}
	}

	public double getCurrentPercent(){
		return (double) currentDone * 100d / work;
	}

	@Override
	public void remaining(int remainingWork) {
		this.currentDone = work - remainingWork;
		printIfNeeded();
	}

	@Override
	public void startTask(String taskName, int work) {
		if(!Algoritms.objectEquals(currentTask, taskName)){
			this.currentTask = taskName;
			System.out.println("Memory before task exec: " + Runtime.getRuntime().totalMemory() + " free : " + Runtime.getRuntime().freeMemory()); //$NON-NLS-1$ //$NON-NLS-2$
			if (previousTaskStarted == 0) {
				System.out.println(taskName + " started - " + work); //$NON-NLS-1$
			} else {
				System.out.println(taskName + " started after " + (System.currentTimeMillis() - previousTaskStarted) + " ms" + " - " + work); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			previousTaskStarted = System.currentTimeMillis();
		}
		startWork(work);
	}

	@Override
	public void startWork(int work) {
		if(this.work != work){
			this.work = work;
			System.out.println("Amount of work was changed to " + work); //$NON-NLS-1$
		}
		this.currentDone = 0;
		this.lastPercentPrint = 0;
	}

	@Override
	public boolean isInterrupted() {
		return false;
	}

	@Override
	public void setGeneralProgress(String genProgress) {
	}

}
