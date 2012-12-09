/*
 * TODO put header 
 */
package eu.lighthouselabs.obd.reader;

import eu.lighthouselabs.obd.reader.io.ObdCommandJob;

/**
 * TODO put description
 */
public interface IPostListener {

	void stateUpdate(ObdCommandJob job);
	
}