package net.osmand;

import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

public class OperationLog {

	private final static Log LOG = PlatformUtil.getLog(OperationLog.class);

	private final String operationName;
	private boolean debug = false;
	private long logThreshold = 100; // 100 ms by default

	private long startTime = System.currentTimeMillis();
	private boolean startLogged = false;

	public OperationLog(String operationName) {
		this.operationName = operationName;
	}

	public OperationLog(String operationName, boolean debug) {
		this.operationName = operationName;
		this.debug = debug;
	}

	public OperationLog(String operationName, boolean debug, long logThreshold) {
		this.operationName = operationName;
		this.debug = debug;
		this.logThreshold = logThreshold;
	}

	public void startOperation() {
		startOperation(null);
	}

	public void startOperation(String message) {
		this.startTime = System.currentTimeMillis();
		logImpl(operationName + " BEGIN " + (!Algorithms.isEmpty(message) ? message : ""), debug);
		startLogged = debug;
	}

	public void finishOperation() {
		finishOperation(null);
	}

	public void finishOperation(String message) {
		long elapsedTime = System.currentTimeMillis() - startTime;
		if (startLogged || debug || elapsedTime > logThreshold) {
			logImpl(operationName + " END (" + elapsedTime + " ms)"
					+ (!Algorithms.isEmpty(message) ? " " + message : ""), true);
		}
	}

	public void log(String message) {
		log(message, false);
	}

	public void log(String message, boolean forceLog) {
		if (debug || forceLog) {
			LOG.debug(operationName + (!Algorithms.isEmpty(message) ? " " + message : ""));
		}
	}

	private void logImpl(String message, boolean forceLog) {
		if (debug || forceLog) {
			LOG.debug(message);
		}
	}
}