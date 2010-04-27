package com.osmand;

public class WayUtil {

	
	public static boolean wayForCar(String tagHighway){
		if(tagHighway != null){
			String[] cars = new String[]{"trunk", "motorway", "primary", "secondary", "tertiary", "service", "residential",
										"trunk_link", "motorway_link", "primary_link", "secondary_link", "residential_link", 
										"tertiary_link", "track" };
			for(String c : cars){
				if(c.equals(tagHighway)){
					return true;
				}
			}
		}
		return false;
	}
}
