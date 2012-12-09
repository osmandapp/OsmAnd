/*
 * TODO put header 
 */
package eu.lighthouselabs.obd.reader;

import eu.lighthouselabs.obd.reader.io.ObdCommandJob;

/**
 * TODO put description
 */
public interface IPostMonitor {
	void setListener(IPostListener callback);

	boolean isRunning();

	void executeQueue();
	
	void addJobToQueue(ObdCommandJob job);
}