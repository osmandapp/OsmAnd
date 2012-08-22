package net.osmand;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class TestSunriseSunset {
	
	public static final String AUGUST = "05.08.2012";
	public static final String OCTOBER = "05.10.2012";
	public static final String DECEMBER = "05.12.2012";
	public static final String FEBRUARY = "05.02.2012";
	public static final String MAY = "05.05.2012";
	public static final String[] DATES = new String[] {AUGUST, OCTOBER, DECEMBER, FEBRUARY, MAY};
	
	public static SunriseSunset printSunriseSunset(String date, float lat, float lon, TimeZone tz) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
		sdf.setTimeZone(tz);
		SimpleDateFormat prt = new SimpleDateFormat("dd.MM.yyyy HH:MM");
		prt	.setTimeZone(tz);
		SunriseSunset ss = new SunriseSunset(lat, lon, sdf.parse(date), tz);
		System.out.println("Sunrise : " + prt.format(ss.getSunrise()));
		System.out.println("Sunset  : " + prt.format(ss.getSunset()));
		return ss;
	}
	
	public static void testPCT() throws ParseException{
		System.out.println("California");
		for (String d : DATES) {
			printSunriseSunset(d, 34.08f, -118f, TimeZone.getTimeZone("PST"));
		}
	}
	
	public static void testCET() throws ParseException{
		System.out.println("Amsterdam");
		for (String d : DATES) {
			printSunriseSunset(d, 52.88f, 4.86f, TimeZone.getTimeZone("CET"));
		}
		System.out.println("Amsterdam 0");
		for (String d : DATES) {
			printSunriseSunset(d, 0, 4.86f, TimeZone.getTimeZone("UTC"));
		}
	}
	
	public static void testEDT() throws ParseException{
		System.out.println("New york");
		for (String d : DATES) {
			printSunriseSunset(d, 40.88f, -73.86f, TimeZone.getTimeZone("EDT"));
		}
	}
	
	public static void main(String[] args) throws ParseException {
		testPCT();
		testCET();
		testEDT();
	}
	
}
