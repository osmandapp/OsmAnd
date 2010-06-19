package com.osmand;

import java.util.ResourceBundle;

public class Messages {


	private static ResourceBundle bundle = ResourceBundle.getBundle("messages");
	

	public static String getMessage(String key){
		return bundle.getString(key);
	}

}
