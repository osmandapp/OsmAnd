package com.osmand;

import org.apache.commons.logging.Log;

public class ExceptionHandler {
	private static final Log log = LogUtil.getLog(ExceptionHandler.class);
	
	public static void handle(Exception e){
		e.printStackTrace();
		log.error("Error occurred", e);
	}
	
	public static void handle(String msg, Exception e){
		e.printStackTrace();
		log.error(msg, e);
	}

}
