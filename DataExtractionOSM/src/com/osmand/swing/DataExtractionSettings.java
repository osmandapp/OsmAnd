package com.osmand.swing;

public class DataExtractionSettings {

	private static DataExtractionSettings settings = null;
	public static DataExtractionSettings getSettings(){
		if(settings == null){
			settings = new DataExtractionSettings();
		}
		return settings;
		
	}
	

	
	public void saveSettings(){
		
	}
}
