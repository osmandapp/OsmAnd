package com.osmand;

import java.util.ResourceBundle;

public class Messages {

	private static ResourceBundle bundle = ResourceBundle.getBundle("messages"); //$NON-NLS-1$
	public static final String KEY_M = "m"; //$NON-NLS-1$
	public static final String KEY_KM = "km"; //$NON-NLS-1$
	public static final String KEY_KM_H = "km_h"; //$NON-NLS-1$

	public static String getMessage(String key){
		return bundle.getString(key);
	}

}
