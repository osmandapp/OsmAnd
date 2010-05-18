package com.osmand;

/**
 * That common interface that could be used by background operations.
 * Implementation of it depends on chosen UI platform 
 */
public interface IProgress {
	
	/**
	 * @param taskName
	 * @param work - -1 means that indeterminate task, 
	 * otherwise number of could be specified 
	 */
	public void startTask(String taskName, int work);
	
	public void startWork(int work);
	
	public void progress(int deltaWork);
	
	public void remaining(int remainingWork);
	
	public void finishTask();
	
	public boolean isIndeterminate();
	
	public boolean isInterrupted();

}
