package com.osmand;

/**
 * Basic algorithms that are not in jdk 
 */
public class Algoritms {
	
	public static boolean isEmpty(String s){
		return s == null || s.length() == 0;
	}
	
	
	public static boolean objectEquals(Object a, Object b){
		if(a == null){
			return b == null;
		} else {
			return a.equals(b);
		}
	}
	
}
