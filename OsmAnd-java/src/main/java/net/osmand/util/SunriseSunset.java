package net.osmand.util;
/******************************************************************************
*
*							SunriseSunset.java
*
*******************************************************************************
* 
* Java Class: SunriseSunset
* 
*	This Java class is part of a collection of classes developed for the 
*	reading and processing of oceanographic and meterological data collected 
*	since 1970 by environmental buoys and stations.  This dataset is 
*	maintained by the National Oceanographic Data Center and is publicly 
*	available.  These Java classes were written for the US Environmental 
*	Protection Agency's National Exposure Research Laboratory under Contract 
*	No. GS-10F-0073K with Neptune and Company of Los Alamos, New Mexico.
* 
* Purpose:
* 
* 	This Java class performs calculations to determine the time of 
*	sunrise and sunset given lat, long, and date.
* 
* Inputs:
* 
* 	Latitude, longitude, date/time, and time zone.
* 
* Outputs:
* 
* 	Local time of sunrise and sunset as calculated by the 
*	  program.
*	If no sunrise or no sunset occurs, or if the sun is up all day 
*	  or down all day, appropriate boolean values are set.
*	A boolean is provided to identify if the time provided is during the day.
*
*	The above values are accessed by the following methods:
*
*		Date	getSunrise()	returns date/time of sunrise
*		Date	getSunset()		returns date/time of sunset
*		boolean	isSunrise()		returns true if there was a sunrise, else false
*		boolean	isSunset()		returns true if there was a sunset, else false
*		boolean	isSunUp()		returns true if sun is up all day, else false
*		boolean	isSunDown()		returns true if sun is down all day, else false
*		boolean	isDaytime()		returns true if sun is up at the time 
*									specified, else false
* 
* Required classes from the Java library:
* 
* 	java.util.Date
* 	java.text.SimpleDateFormat
* 	java.text.ParseException;
* 	java.math.BigDecimal;
* 
* Package of which this class is a member:
* 
*	default
* 
* Known limitations:
* 
*	It is assumed that the data provided are within value ranges
*	(i.e. latitude between -90 and +90, longitude between 0 and 360,
*	a valid date, and time zone between -14 and +14.
* 
* Compatibility:
* 
*	Java 1.1.8
* 
* References:
*	
*	The mathematical algorithms used in this program are patterned 
*	after those debveloped by Roger Sinnott in his BASIC program, 
*	SUNUP.BAS, published in Sky & Telescope magazine:
*	Sinnott, Roger W. "Sunrise and Sunset: A Challenge"
*	Sky & Telescope, August, 1994 p.84-85
* 
*	The following is a cross-index of variables used in SUNUP.BAS.
*	A single definition from multiple reuse of variable names in 
*	SUNUP.BAS was clarified with various definitions in this program.
*
*	SUNUP.BAS	this class
* 
*	A			dfA
*	A(2)		dfAA1, dfAA2
*	A0			dfA0
*	A2			dfA2
*	A5			dfA5
*	AZ			Not used
*	C			dfCosLat
*	C0			dfC0
*	D			iDay
*	D(2)		dfDD1, dfDD2
*	D0			dfD0
*	D1			dfD1
*	D2			dfD2
*	D5			dfD5
*	D7			Not used
*	DA			dfDA
*	DD			dfDD
*	G			bGregorian, dfGG
*	H			dfTimeZone
*	H0			dfH0
*	H1			dfH1
*	H2			dfH2
*	H3			dfHourRise, dfHourSet
*	H7			Not used
*	J			dfJ
*	J3			dfJ3
*	K1			dfK1
*	L			dfLL
*	L0			dfL0
*	L2			dfL2
*	L5			dfLon
*	M			iMonth
*	M3			dfMinRise, dfMinSet
*	N7			Not used
*	P			dfP
*	S			iSign, dfSinLat, dfSS
*	T			dfT
*	T0			dfT0
*	T3			not used
*	TT			dfTT
*	U			dfUU
*	V			dfVV
*	V0			dfV0
*	V1			dfV1
*	V2			dfV2
*	W			dfWW
*	Y			iYear
*	Z			dfZenith
*	Z0			dfTimeZone
*	
*  
* Author/Company:
* 
* 	JDT: John Tauxe, Neptune and Company
*	JMG: Jo Marie Green
* 
* Change log:
*  
*	date       ver    by	description of change
*	_________  _____  ___	______________________________________________
*	 5 Jan 01  0.006  JDT	Excised from ssapp.java v. 0.005.
*	11 Jan 01  0.007  JDT	Minor modifications to comments based on 
*							  material from Sinnott, 1994.
*	 7 Feb 01  0.008  JDT	Fixed backwards time zone.  The standard is that 
*							  local time zone is specified in hours EAST of 
*							  Greenwich, so that EST would be -5, for example.
*							  For some reason, SUNUP.BAS does this backwards 
*							  (probably an americocentric perspective) and 
*							  SunriseSunset adopted that convention.  Oops.
*							  So the sign in the math is changed.
*	 7 Feb 01  0.009  JDT	Well, that threw off the azimuth calculation...
*							  Removed the azimuth calculations.
*	14 Feb 01  0.010  JDT	Added ability to accept a time (HH:mm) in 
*							  dateInput, and decide if that time is daytime 
*							  or nighttime.
*	27 Feb 01  0.011  JDT	Added accessor methods in place of having public 
*							  variables to get results. 
*	28 Feb 01  0.012  JDT	Cleaned up list of imported classes. 
*	28 Mar 01  1.10   JDT	Final version accompanying deliverable 1b.
*    4 Apr 01  1.11   JDT	Moved logic supporting .isDaytime into method.
*							  Moved calculations out of constructor.
*   01 May 01  1.12   JMG   Added 'GMT' designation and testing lines.
*   16 May 01  1.13   JDT   Added setLenient( false ) and setTimeZone( tz )
*                           to dfmtDay, dfmtMonth, and dfmtYear in 
*							doCalculations.
*   27 Jun 01  1.14   JDT	Removed reliance on StationConstants (GMT).
*	13 Aug 01  1.20   JDT	Final version accompanying deliverable 1c.
*	 6 Sep 01  1.21   JDT	Thorough code and comment review.
*	21 Sep 01  1.30   JDT	Final version accompanying deliverable 2.
*	17 Dec 01  1.40   JDT	Version accompanying final deliverable.
*				
*----------------------------------------------------------------------------*/

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


/******************************************************************************
*	class:					SunriseSunset class
*******************************************************************************
*
* 	This Java class performs calculations to determine the time of 
*	sunrise and sunset given lat, long, and date.
*
*	It is assumed that the data provided are within value ranges
*	(i.e. latitude between -90 and +90, longitude between 0 and 360,
*	a valid date, and time zone between -14 and +14.
*  
*----------------------------------------------------------------------------*/
public class SunriseSunset
{
	
	// Declare and initialize variables
	private double	dfLat;					// latitude from user
	private double	dfLon;					// latitude from user
	private Date	dateInput;				// date/time from user
	private double	dfTimeZone;				// time zone from user

	private Date	dateSunrise;			// date and time of sunrise
	private Date	dateSunset;				// date and time of sunset
	private boolean	bSunriseToday  = false;	// flag for sunrise on this date
	private boolean	bSunsetToday   = false;	// flag for sunset on this date
	private boolean	bSunUpAllDay   = false;	// flag for sun up all day
	private boolean	bSunDownAllDay = false;	// flag for sun down all day
	private boolean	bDaytime	   = false;	// flag for daytime, given 
											// hour and min in dateInput
	private boolean	bSunrise = false;		// sunrise during hour checked
	private boolean	bSunset  = false;		// sunset during hour checked
	private boolean	bGregorian = false;		// flag for Gregorian calendar
	private int		iJulian;				// Julian day
	private	int		iYear;					// year of date of interest
	private	int		iMonth;					// month of date of interest
	private	int		iDay;					// day of date of interest
	private	int		iCount;					// a simple counter
	private	int		iSign;					// SUNUP.BAS: S
	private int  	dfHourRise, dfHourSet;	// hour of event: SUNUP.BAS H3
	private int 	dfMinRise, dfMinSet;	// minute of event: SUNUP.BAS M3
	private	double	dfSinLat, dfCosLat;		// sin and cos of latitude
	private	double	dfZenith;				// SUNUP.BAS Z: Zenith
	// Many variables in SUNUP.BAS have undocumented meanings, 
	// and so are translated rather directly to avoid confusion:
	private	double	dfAA1 = 0, dfAA2 = 0;	// SUNUP.BAS A(2)
	private	double	dfDD1 = 0, dfDD2 = 0;	// SUNUP.BAS D(2)
	private	double	dfC0;					// SUNUP.BAS C0
	private	double	dfK1;					// SUNUP.BAS K1
	private	double	dfP;					// SUNUP.BAS P
	private	double	dfJ;					// SUNUP.BAS J
	private	double	dfJ3;					// SUNUP.BAS J3
	private	double	dfA;					// SUNUP.BAS A
	private	double	dfA0, dfA2, dfA5;		// SUNUP.BAS A0, A2, A5
	private	double	dfD0, dfD1, dfD2, dfD5;	// SUNUP.BAS D0, D1, D2, D5
	private	double	dfDA, dfDD;				// SUNUP.BAS DA, DD
	private	double	dfH0, dfH1, dfH2;		// SUNUP.BAS H0, H1, H2
	private	double	dfL0, dfL2;				// SUNUP.BAS L0, L2
	private	double	dfT, dfT0, dfTT;		// SUNUP.BAS T, T0, TT
	private	double	dfV0, dfV1, dfV2;		// SUNUP.BAS V0, V1, V2
	
	
/******************************************************************************
*	method:					SunriseSunset
*******************************************************************************
*
*	Constructor for SunriseSunset class.
* 
*----------------------------------------------------------------------------*/
	public SunriseSunset( 
				  double dfLatIn,				// latitude 
				  double dfLonIn,				// longitude 
				  Date	 dateInputIn,			// date
				  TimeZone tzIn			        // time zone
				  )
	{
		// Calculate internal representation of timezone offset as fraction of hours from GMT
		// Our calculations consider offsets to the West as positive, so we must invert
		// the signal of the values provided by the standard library
		double dfTimeZoneIn = 1.0 * tzIn.getOffset(dateInputIn.getTime()) / 3600000;

		// Copy values supplied as arguments to local variables.
		dfLat 		= dfLatIn;
		dfLon 		= dfLonIn;
		dateInput 	= dateInputIn;
		dfTimeZone 	= dfTimeZoneIn;

		// Call the method to do the calculations.
		doCalculations();

	} // end of class constructor

	
/******************************************************************************
*	method:					doCalculations
*******************************************************************************
*
*	Method for performing the calculations done in SUNUP.BAS.
* 
*----------------------------------------------------------------------------*/
	private void doCalculations()
	{
			// Break out day, month, and year from date provided using local time zone.
			// (This is necessary for the math algorithms.)
			Calendar cin = Calendar.getInstance();
			cin.setTime(dateInput);
	
			iYear  = cin.get(Calendar.YEAR); 
			iMonth = cin.get(Calendar.MONTH) + 1; 
			iDay   = cin.get(Calendar.DAY_OF_MONTH); 
	
			// Convert time zone hours to decimal days (SUNUP.BAS line 50)
			dfTimeZone = dfTimeZone / 24.0;
	
			// NOTE: (7 Feb 2001) Here is a non-standard part of SUNUP.BAS:
			// It (and this algorithm) assumes that the time zone is 
			// positive west, instead of the standard negative west.
			// Classes calling SunriseSunset will be assuming that 
			// times zones are specified in negative west, so here the 
			// sign is changed so that the SUNUP algorithm works:
			dfTimeZone = -dfTimeZone;
	
			// Convert longitude to fraction (SUNUP.BAS line 50)
			dfLon = dfLon / 360.0;
	
			// Convert calendar date to Julian date:
			// Check to see if it's later than 1583: Gregorian calendar
			// When declared, bGregorian is initialized to false.
			// ** Consider making a separate class of this function. **
			if( iYear >= 1583 ) bGregorian = true;
			// SUNUP.BAS 1210
			dfJ = -Math.floor( 7.0		// SUNUP used INT, not floor
					* ( Math.floor( 
							( iMonth + 9.0 )
							/ 12.0
							) + iYear
							) / 4.0
					)
					// add SUNUP.BAS 1240 and 1250 for G = 0
					+ Math.floor( iMonth * 275.0 / 9.0 )
					+ iDay
					+ 1721027.0
					+ iYear * 367.0;
	
			if ( bGregorian )
			{
				// SUNUP.BAS 1230
				if ( ( iMonth - 9.0 ) < 0.0 ) iSign = -1;
				else iSign = 1;
				dfA = Math.abs( iMonth - 9.0 );
				// SUNUP.BAS 1240 and 1250
				dfJ3 = -Math.floor(
						(
								Math.floor(
										Math.floor( iYear 
												+ (double)iSign 
												* Math.floor( dfA / 7.0 )
												)
												/ 100.0
										) + 1.0
								) * 0.75
						);
				// correct dfJ as in SUNUP.BAS 1240 and 1250 for G = 1
				dfJ = dfJ + dfJ3 + 2.0;
			}
			// SUNUP.BAS 1290
			iJulian = (int)dfJ - 1;
	
			// SUNUP.BAS 60 and 70 (see also line 1290)
			dfT = (double)iJulian - 2451545.0 + 0.5;
			dfTT = dfT / 36525.0 + 1.0;				// centuries since 1900
	
			// Calculate local sidereal time at 0h in zone time
			// SUNUP.BAS 410 through 460
			dfT0 = ( dfT * 8640184.813 / 36525.0
					+ 24110.5
					+ dfTimeZone * 86636.6
					+ dfLon * 86400.0
					)
					/ 86400.0;
			dfT0 = dfT0 - Math.floor( dfT0 );	// NOTE: SUNUP.BAS uses INT()
			dfT0 = dfT0 * 2.0 * Math.PI;
			// SUNUP.BAS 90
			dfT = dfT + dfTimeZone;
	
			// SUNUP.BAS 110: Get Sun's position
			for( iCount=0; iCount<=1; iCount++ )	// Loop thru only twice
			{
				// Calculate Sun's right ascension and declination
				//   at the start and end of each day.
				// SUNUP.BAS 910 - 1160: Fundamental arguments
				//   from van Flandern and Pulkkinen, 1979
	
				// declare local temporary doubles for calculations
				double	dfGG;						// SUNUP.BAS G
				double	dfLL;						// SUNUP.BAS L
				double	dfSS;						// SUNUP.BAS S
				double	dfUU;						// SUNUP.BAS U
				double	dfVV;						// SUNUP.BAS V
				double	dfWW;						// SUNUP.BAS W
	
				dfLL = 0.779072 + 0.00273790931 * dfT;
				dfLL = dfLL - Math.floor( dfLL );
				dfLL = dfLL * 2.0 * Math.PI;
	
				dfGG = 0.993126 + 0.0027377785 * dfT;
				dfGG = dfGG - Math.floor( dfGG );
				dfGG = dfGG * 2.0 * Math.PI;
	
				dfVV =   0.39785 * Math.sin( dfLL )
						- 0.01000 * Math.sin( dfLL - dfGG )
						+ 0.00333 * Math.sin( dfLL + dfGG )
						- 0.00021 * Math.sin( dfLL ) * dfTT;
	
				dfUU = 1
						- 0.03349 * Math.cos( dfGG )
						- 0.00014 * Math.cos( dfLL * 2.0 )
						+ 0.00008 * Math.cos( dfLL );
	
				dfWW = - 0.00010
						- 0.04129 * Math.sin( dfLL * 2.0 )
						+ 0.03211 * Math.sin( dfGG )
						- 0.00104 * Math.sin( 2.0 * dfLL - dfGG )
						- 0.00035 * Math.sin( 2.0 * dfLL + dfGG )
						- 0.00008 * Math.sin( dfGG ) * dfTT;
	
				// Compute Sun's RA and Dec; SUNUP.BAS 1120 - 1140
				dfSS = dfWW / Math.sqrt( dfUU - dfVV * dfVV );
				dfA5 = dfLL 
						+ Math.atan( dfSS / Math.sqrt( 1.0 - dfSS * dfSS ));
	
				dfSS = dfVV / Math.sqrt( dfUU );
				dfD5 = Math.atan( dfSS / Math.sqrt( 1 - dfSS * dfSS ));					
	
				// Set values and increment t
				if ( iCount == 0 )		// SUNUP.BAS 125
				{
					dfAA1 = dfA5;
					dfDD1 = dfD5;
				}
				else					// SUNUP.BAS 145
				{
					dfAA2 = dfA5;
					dfDD2 = dfD5;
				}
				dfT = dfT + 1.0;		// SUNUP.BAS 130
			}	// end of Get Sun's Position for loop
	
			if ( dfAA2 < dfAA1 ) dfAA2 = dfAA2 + 2.0 * Math.PI;
			// SUNUP.BAS 150
	
			dfZenith = Math.PI * 90.833 / 180.0;			// SUNUP.BAS 160
			dfSinLat = Math.sin( dfLat * Math.PI / 180.0 );	// SUNUP.BAS 170
			dfCosLat = Math.cos( dfLat * Math.PI / 180.0 );	// SUNUP.BAS 170
	
			dfA0 = dfAA1;									// SUNUP.BAS 190
			dfD0 = dfDD1;									// SUNUP.BAS 190
			dfDA = dfAA2 - dfAA1;							// SUNUP.BAS 200
			dfDD = dfDD2 - dfDD1;							// SUNUP.BAS 200
	
			dfK1 = 15.0 * 1.0027379 * Math.PI / 180.0;		// SUNUP.BAS 330
	
			// Initialize sunrise and sunset times, and other variables
			// hr and min are set to impossible times to make errors obvious
			dfHourRise = 99;
			dfMinRise  = 99;
			dfHourSet  = 99;
			dfMinSet   = 99;
			dfV0 = 0.0;		// initialization implied by absence in SUNUP.BAS
			dfV2 = 0.0;		// initialization implied by absence in SUNUP.BAS
	
			// Test each hour to see if the Sun crosses the horizon
			//   and which way it is heading.
			for( iCount=0; iCount<24; iCount++ )			// SUNUP.BAS 210
			{
				double	tempA;								// SUNUP.BAS A
				double	tempB;								// SUNUP.BAS B
				double	tempD;								// SUNUP.BAS D
				double	tempE;								// SUNUP.BAS E
	
				dfC0 = (double)iCount;
				dfP = ( dfC0 + 1.0 ) / 24.0;				// SUNUP.BAS 220
				dfA2 = dfAA1 + dfP * dfDA;					// SUNUP.BAS 230
				dfD2 = dfDD1 + dfP * dfDD;					// SUNUP.BAS 230
				dfL0 = dfT0 + dfC0 * dfK1;					// SUNUP.BAS 500
				dfL2 = dfL0 + dfK1;							// SUNUP.BAS 500
				dfH0 = dfL0 - dfA0;							// SUNUP.BAS 510
				dfH2 = dfL2 - dfA2;							// SUNUP.BAS 510
				// hour angle at half hour
				dfH1 = ( dfH2 + dfH0 ) / 2.0;				// SUNUP.BAS 520
				// declination at half hour
				dfD1 = ( dfD2 + dfD0 ) / 2.0;				// SUNUP.BAS 530
	
				// Set value of dfV0 only if this is the first hour, 
				// otherwise, it will get set to the last dfV2 (SUNUP.BAS 250)
				if ( iCount == 0 )							// SUNUP.BAS 550
				{	
					dfV0 = dfSinLat * Math.sin( dfD0 )
							+ dfCosLat * Math.cos( dfD0 ) * Math.cos( dfH0 )
							- Math.cos( dfZenith );			// SUNUP.BAS 560
				}
				else
					dfV0 = dfV2;	// That is, dfV2 from the previous hour.
	
				dfV2 = dfSinLat * Math.sin( dfD2 )
						+ dfCosLat * Math.cos( dfD2 ) * Math.cos( dfH2 )
						- Math.cos( dfZenith );			// SUNUP.BAS 570
	
				// if dfV0 and dfV2 have the same sign, then proceed to next hr
				if ( 
						( dfV0 >= 0.0 && dfV2 >= 0.0 )		// both are positive
						||								// or
						( dfV0 < 0.0 && dfV2 < 0.0 ) 		// both are negative
						)
				{
					// Break iteration and proceed to test next hour
					dfA0 = dfA2;							// SUNUP.BAS 250
					dfD0 = dfD2;							// SUNUP.BAS 250
					continue;								// SUNUP.BAS 610
				}
	
				dfV1 = dfSinLat * Math.sin( dfD1 )
						+ dfCosLat * Math.cos( dfD1 ) * Math.cos( dfH1 )
						- Math.cos( dfZenith );				// SUNUP.BAS 590
	
				tempA = 2.0 * dfV2 - 4.0 * dfV1 + 2.0 * dfV0;
				// SUNUP.BAS 600
				tempB = 4.0 * dfV1 - 3.0 * dfV0 - dfV2;		// SUNUP.BAS 600
				tempD = tempB * tempB - 4.0 * tempA * dfV0;	// SUNUP.BAS 610
	
				if ( tempD < 0.0 ) 
				{
					// Break iteration and proceed to test next hour
					dfA0 = dfA2;							// SUNUP.BAS 250
					dfD0 = dfD2;							// SUNUP.BAS 250
					continue;								// SUNUP.BAS 610
				}
	
				tempD = Math.sqrt( tempD );					// SUNUP.BAS 620
	
				// Determine occurrence of sunrise or sunset.
	
				// Flags to identify occurrence during this day are 
				// bSunriseToday and bSunsetToday, and are initialized false.
				// These are set true only if sunrise or sunset occurs 
				// at any point in the hourly loop. Never set to false.
	
				// Flags to identify occurrence during this hour:
				bSunrise = false;				// reset before test
				bSunset  = false;				// reset before test
	
				if ( dfV0 < 0.0 && dfV2 > 0.0 )	// sunrise occurs this hour
				{
					bSunrise = true;			// SUNUP.BAS 640
					bSunriseToday = true;		// sunrise occurred today
				}
	
				if ( dfV0 > 0.0 && dfV2 < 0.0 )	// sunset occurs this hour
				{
					bSunset = true;				// SUNUP.BAS 660
					bSunsetToday = true;		// sunset occurred today
				}
	
				tempE = ( tempD - tempB ) / ( 2.0 * tempA );
				if ( tempE > 1.0 || tempE < 0.0 )	// SUNUP.BAS 670, 680
					tempE = ( -tempD - tempB ) / ( 2.0 * tempA );					
	
				// Set values of hour and minute of sunset or sunrise
				// only if sunrise/set occurred this hour.
				if ( bSunrise )
				{
					dfHourRise = (int)( dfC0 + tempE + 1.0/120.0 );
					dfMinRise  = (int) ( 
							( dfC0 + tempE + 1.0/120.0 
									- dfHourRise 
									)
									* 60.0
							);
				}
	
				if ( bSunset )
				{
					dfHourSet  = (int) ( dfC0 + tempE + 1.0/120.0 );
					dfMinSet   = (int)( 
							( dfC0 + tempE + 1.0/120.0
									- dfHourSet 
									) 
									* 60.0
							);
				}
	
				// Change settings of variables for next loop
				dfA0 = dfA2;								// SUNUP.BAS 250
				dfD0 = dfD2;								// SUNUP.BAS 250
	
			}	// end of loop testing each hour for an event	
	
			// After having checked all hours, set flags if no rise or set
			// bSunUpAllDay and bSundownAllDay are initialized as false
			if ( !bSunriseToday && !bSunsetToday )
			{
				if ( dfV2 < 0.0 )
					bSunDownAllDay = true;
				else
					bSunUpAllDay = true;
			}
	
			// Load dateSunrise with data
	
			if( bSunriseToday )
			{
				Calendar c = Calendar.getInstance();
				c.set(Calendar.YEAR, iYear);
				c.set(Calendar.MONTH, iMonth-1);
				c.set(Calendar.DAY_OF_MONTH, iDay);
				c.set(Calendar.HOUR_OF_DAY, dfHourRise);
				c.set(Calendar.MINUTE, dfMinRise);
				dateSunrise = c.getTime();
			}
	
			// Load dateSunset with data
			if( bSunsetToday )
			{
				Calendar c = Calendar.getInstance();
				c.set(Calendar.YEAR, iYear);
				c.set(Calendar.MONTH, iMonth-1);
				c.set(Calendar.DAY_OF_MONTH, iDay);
				c.set(Calendar.HOUR_OF_DAY, dfHourSet);
				c.set(Calendar.MINUTE, dfMinSet);
				dateSunset = c.getTime();
			}
	}
	
	
/******************************************************************************
*	method:					getSunrise()
*******************************************************************************
*
*   Gets the date and time of sunrise.  If there is no sunrise, returns null.
*				
*	Member of SunriseSunset class
*				
* -------------------------------------------------------------------------- */
	public Date getSunrise()
	{
		if ( bSunriseToday )
			return( dateSunrise );
		else
			return( null );
	}

	
/******************************************************************************
*	method:					getSunset()
*******************************************************************************
*
*   Gets the date and time of sunset.  If there is no sunset, returns null.
*				
*	Member of SunriseSunset class
*				
* -------------------------------------------------------------------------- */
	public Date getSunset()
	{
		if ( bSunsetToday )
			return( dateSunset );
		else
			return( null );
	}

	
/******************************************************************************
*	method:					isSunrise()
*******************************************************************************
*
*   Returns a boolean identifying if there was a sunrise.
*				
*	Member of SunriseSunset class
*				
* -------------------------------------------------------------------------- */
	public boolean isSunrise()
	{
		return( bSunriseToday );
	}

	
/******************************************************************************
*	method:					isSunset()
*******************************************************************************
*
*   Returns a boolean identifying if there was a sunset.
*				
*	Member of SunriseSunset class
*				
* -------------------------------------------------------------------------- */
	public boolean isSunset()
	{
		return( bSunsetToday );
	}

	
/******************************************************************************
*	method:					isSunUp()
*******************************************************************************
*
*   Returns a boolean identifying if the sun is up all day.
*				
*	Member of SunriseSunset class
*				
* -------------------------------------------------------------------------- */
	public boolean isSunUp()
	{
		return( bSunUpAllDay );
	}

	
/******************************************************************************
*	method:					isSunDown()
*******************************************************************************
*
*   Returns a boolean identifying if the sun is down all day.
*				
*	Member of SunriseSunset class
*				
* -------------------------------------------------------------------------- */
	public boolean isSunDown()
	{
		return( bSunDownAllDay );
	}

	
/******************************************************************************
*	method:					isDaytime()
*******************************************************************************
*
*   Returns a boolean identifying if it is daytime at the hour contained in 
*	the Date object passed to SunriseSunset on construction.
*				
*	Member of SunriseSunset class
*				
* -------------------------------------------------------------------------- */
	public boolean isDaytime()
	{
		// Determine if it is daytime (at sunrise or later) 
		//	or nighttime (at sunset or later) at the location of interest
		//	but expressed in the time zone requested.
		if ( bSunriseToday && bSunsetToday ) 	// sunrise and sunset
		{
			if ( dateSunrise.before( dateSunset ) )	// sunrise < sunset
			{
				if ( 
					 (	
						dateInput.after( dateSunrise ) 
						||
						dateInput.equals( dateSunrise ) 
					 )
					 &&
					 dateInput.before( dateSunset )
				   )
					bDaytime = true;
				else
				bDaytime = false;
				}
			else 	// sunrise comes after sunset (in opposite time zones)
			{
				if ( 
					 (	
						dateInput.after( dateSunrise ) 
						||
						dateInput.equals( dateSunrise ) 
					 )
					 ||			// use OR rather than AND
					 dateInput.before( dateSunset )
				   )
					bDaytime = true;
				else
				bDaytime = false;
				}
		}
		else if ( bSunUpAllDay ) 				// sun is up all day
			bDaytime = true;
		else if ( bSunDownAllDay )				// sun is down all day
			bDaytime = false;
		else if ( bSunriseToday ) 				// sunrise but no sunset
		{
			if ( dateInput.before( dateSunrise ) )
				bDaytime = false;
			else
				bDaytime = true;
		}
		else if ( bSunsetToday ) 				// sunset but no sunrise
		{
			if ( dateInput.before( dateSunset ) )
				bDaytime = true;
			else
				bDaytime = false;
		}
		else bDaytime = false;					// this should never execute

		return( bDaytime );
	}
} // end of class 

/*-----------------------------------------------------------------------------
*							end of class
*----------------------------------------------------------------------------*/

