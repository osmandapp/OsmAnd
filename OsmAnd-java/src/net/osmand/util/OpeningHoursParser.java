package net.osmand.util;
/* Can be commented out in order to run the main function separately */

import java.io.Serializable;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

import gnu.trove.list.array.TIntArrayList;

/**
 * Class used to parse opening hours
 * <p/>
 * the method "parseOpenedHours" will parse an OSM opening_hours string and
 * return an object of the type OpeningHours. That object can be used to check
 * if the OSM feature is open at a certain time.
 */
public class OpeningHoursParser {
	private static final String[] daysStr;
	private static final String[] localDaysStr;
	private static final String[] monthsStr;
	private static final String[] localMothsStr;

	static {
		DateFormatSymbols dateFormatSymbols = DateFormatSymbols.getInstance(Locale.US);
		monthsStr = dateFormatSymbols.getShortMonths();
		daysStr = getTwoLettersStringArray(dateFormatSymbols.getShortWeekdays());
		dateFormatSymbols = DateFormatSymbols.getInstance();
		localMothsStr = dateFormatSymbols.getShortMonths();
		localDaysStr = getTwoLettersStringArray(dateFormatSymbols.getShortWeekdays());
	}

	/**
	 * Default values for sunrise and sunset. Might be computed afterwards, not final.
	 */
	private static String sunrise = "07:00", sunset = "21:00";

	/**
	 * Hour of when you would expect a day to be ended.
	 * This is to be used when no end hour is known (like pubs that open at a certain time,
	 * but close at a variable time, depending on the number of clients).
	 * OsmAnd needs to show a value, so there is some arbitrary default value chosen.
	 */
	private static String endOfDay = "24:00";

	private static String[] getTwoLettersStringArray(String[] strings) {
		String[] newStrings = new String[strings.length];
		for (int i = 0; i < strings.length; i++) {
			if (strings[i] != null) {
				if (strings[i].length() > 2) {
					newStrings[i] = strings[i].substring(0, 2);
				} else {
					newStrings[i] = strings[i];
				}
			}
		}
		return newStrings;
	}

	private static int getDayIndex(int i) {
		switch (i) {
			case 0: return Calendar.MONDAY;
			case 1: return Calendar.TUESDAY;
			case 2: return Calendar.WEDNESDAY;
			case 3: return Calendar.THURSDAY;
			case 4: return Calendar.FRIDAY;
			case 5: return Calendar.SATURDAY;
			case 6: return Calendar.SUNDAY;
			default: return -1;
		}
	}

	/**
	 * This class contains the entire OpeningHours schema and
	 * offers methods to check directly weather something is open
	 *
	 * @author sander
	 */
	public static class OpeningHours implements Serializable {

		/**
		 * list of the different rules
		 */
		private ArrayList<OpeningHoursRule> rules;

		/**
		 * Constructor
		 *
		 * @param rules List of OpeningHoursRule to be given
		 */
		public OpeningHours(ArrayList<OpeningHoursRule> rules) {
			this.rules = rules;
		}

		/**
		 * Empty constructor
		 */
		public OpeningHours() {
			rules = new ArrayList<OpeningHoursRule>();
		}

		/**
		 * add a rule to the opening hours
		 *
		 * @param r rule to add
		 */
		public void addRule(OpeningHoursRule r) {
			rules.add(r);
		}

		/**
		 * return the list of rules
		 *
		 * @return the rules
		 */
		public ArrayList<OpeningHoursRule> getRules() {
			return rules;
		}

		/**
		 * check if the feature is opened at time "cal"
		 *
		 * @param cal the time to check
		 * @return true if feature is open
		 */
		public boolean isOpenedForTime(Calendar cal) {
			/*
			 * first check for rules that contain the current day
			 * afterwards check for rules that contain the previous
			 * day with overlapping times (times after midnight)
			 */
			boolean isOpenDay = false;
			for (OpeningHoursRule r : rules) {
				if (r.containsDay(cal) && r.containsMonth(cal)) {
					isOpenDay = r.isOpenedForTime(cal, false);
				}
			}
			boolean isOpenPrevious = false;
			for (OpeningHoursRule r : rules) {
				if (r.containsPreviousDay(cal) && r.containsMonth(cal)) {
					isOpenPrevious = r.isOpenedForTime(cal, true);
				}
			}
			return isOpenDay || isOpenPrevious;
		}

		public String getCurrentRuleTime(Calendar cal) {
			String ruleOpen = null;
			String ruleClosed = null;
			for (OpeningHoursRule r : rules) {
				if (r.containsPreviousDay(cal) && r.containsMonth(cal)) {
					if (r.isOpenedForTime(cal, true)) {
						ruleOpen = r.toRuleString(true);
					} else {
						ruleClosed = r.toRuleString(true);
					}
				}
			}
			for (OpeningHoursRule r : rules) {
				if (r.containsDay(cal) && r.containsMonth(cal)) {
					if (r.isOpenedForTime(cal, false)) {
						ruleOpen = r.toRuleString(true);
					} else {
						ruleClosed = r.toRuleString(true);
					}
				}
			}

			if (ruleOpen != null) {
				return ruleOpen;
			}
			return ruleClosed;
		}

		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();

			if (rules.isEmpty()) {
				return "";
			}

			for (OpeningHoursRule r : rules) {
				s.append(r.toString()).append("; ");
			}

			return s.substring(0, s.length() - 2);
		}

		public String toStringNoMonths() {
			StringBuilder s = new StringBuilder();
			if (rules.isEmpty()) {
				return "";
			}

			for (OpeningHoursRule r : rules) {
				s.append(r.toRuleString(true)).append("; ");
			}

			return s.substring(0, s.length() - 2);
		}

		public String toLocalStringNoMonths() {
			StringBuilder s = new StringBuilder();
			if (rules.isEmpty()) {
				return "";
			}

			for (OpeningHoursRule r : rules) {
				s.append(r.toLocalRuleString()).append("; ");
			}

			return s.substring(0, s.length() - 2);
		}
	}

	/**
	 * Interface to represent a single rule
	 * <p/>
	 * A rule consist out of
	 * - a collection of days/dates
	 * - a time range
	 */
	public static interface OpeningHoursRule extends Serializable {

		/**
		 * Check if, for this rule, the feature is opened for time "cal"
		 *
		 * @param cal           the time to check
		 * @param checkPrevious only check for overflowing times (after midnight) or don't check for it
		 * @return true if the feature is open
		 */
		public boolean isOpenedForTime(Calendar cal, boolean checkPrevious);

		/**
		 * Check if the previous day before "cal" is part of this rule
		 *
		 * @param cal; the time to check
		 * @return true if the previous day is part of the rule
		 */
		public boolean containsPreviousDay(Calendar cal);

		/**
		 * Check if the day of "cal" is part of this rule
		 *
		 * @param cal the time to check
		 * @return true if the day is part of the rule
		 */
		public boolean containsDay(Calendar cal);

		/**
		 * Check if the month of "cal" is part of this rule
		 *
		 * @param cal the time to check
		 * @return true if the month is part of the rule
		 */
		public boolean containsMonth(Calendar cal);


		public String toRuleString(boolean avoidMonths);

		public String toLocalRuleString();
	}

	/**
	 * implementation of the basic OpeningHoursRule
	 * <p/>
	 * This implementation only supports month, day of weeks and numeral times, or the value "off"
	 */
	public static class BasicOpeningHourRule implements OpeningHoursRule {
		/**
		 * represents the list on which days it is open.
		 * Day number 0 is MONDAY
		 */
		private boolean[] days = new boolean[7];

		/**
		 * represents the list on which month it is open.
		 * Day number 0 is JANUARY.
		 */
		private boolean[] months = new boolean[12];

		/**
		 * lists of equal size representing the start and end times
		 */
		private TIntArrayList startTimes = new TIntArrayList(), endTimes = new TIntArrayList();

		/**
		 * return an array representing the days of the rule
		 *
		 * @return the days of the rule
		 */
		public boolean[] getDays() {
			return days;
		}

		/**
		 * return an array representing the months of the rule
		 *
		 * @return the months of the rule
		 */
		public boolean[] getMonths() {
			return months;
		}

		/**
		 * set a single start time, erase all previously added start times
		 *
		 * @param s startTime to set
		 */
		public void setStartTime(int s) {
			setSingleValueForArrayList(startTimes, s);
			if (endTimes.size() != 1) {
				setSingleValueForArrayList(endTimes, 0);
			}
		}

		/**
		 * set a single end time, erase all previously added end times
		 *
		 * @param e endTime to set
		 */
		public void setEndTime(int e) {
			setSingleValueForArrayList(endTimes, e);
			if (startTimes.size() != 1) {
				setSingleValueForArrayList(startTimes, 0);
			}
		}

		/**
		 * Set single start time. If position exceeds index of last item by one
		 * then new value will be added.
		 * If value is between 0 and last index, then value in the position p will be overwritten
		 * with new one.
		 * Else exception will be thrown.
		 *
		 * @param s        - value
		 * @param position - position to add
		 */
		public void setStartTime(int s, int position) {
			if (position == startTimes.size()) {
				startTimes.add(s);
				endTimes.add(0);
			} else {
				startTimes.set(position, s);
			}
		}

		/**
		 * Set single end time. If position exceeds index of last item by one
		 * then new value will be added.
		 * If value is between 0 and last index, then value in the position p will be overwritten
		 * with new one.
		 * Else exception will be thrown.
		 *
		 * @param s        - value
		 * @param position - position to add
		 */
		public void setEndTime(int s, int position) {
			if (position == startTimes.size()) {
				endTimes.add(s);
				startTimes.add(0);
			} else {
				endTimes.set(position, s);
			}
		}

		/**
		 * get a single start time
		 *
		 * @return a single start time
		 */
		public int getStartTime() {
			if (startTimes.size() == 0) {
				return 0;
			}
			return startTimes.get(0);
		}

		/**
		 * get a single start time in position
		 *
		 * @param position position to get value from
		 * @return a single start time
		 */
		public int getStartTime(int position) {
			return startTimes.get(position);
		}

		/**
		 * get a single end time
		 *
		 * @return a single end time
		 */
		public int getEndTime() {
			if (endTimes.size() == 0) {
				return 0;
			}
			return endTimes.get(0);
		}

		/**
		 * get a single end time in position
		 *
		 * @param position position to get value from
		 * @return a single end time
		 */
		public int getEndTime(int position) {
			return endTimes.get(position);
		}

		/**
		 * get all start times as independent list
		 *
		 * @return all start times
		 */
		public TIntArrayList getStartTimes() {
			return new TIntArrayList(startTimes);
		}

		/**
		 * get all end times as independent list
		 *
		 * @return all end times
		 */
		public TIntArrayList getEndTimes() {
			return new TIntArrayList(endTimes);
		}

		/**
		 * Check if the weekday of time "cal" is part of this rule
		 *
		 * @param cal the time to check
		 * @return true if this day is part of the rule
		 */
		@Override
		public boolean containsDay(Calendar cal) {
			int i = cal.get(Calendar.DAY_OF_WEEK);
			int d = (i + 5) % 7;
			if (days[d]) {
				return true;
			}
			return false;
		}

		/**
		 * Check if the previous weekday of time "cal" is part of this rule
		 *
		 * @param cal the time to check
		 * @return true if the previous day is part of the rule
		 */
		@Override
		public boolean containsPreviousDay(Calendar cal) {
			int i = cal.get(Calendar.DAY_OF_WEEK);
			int p = (i + 4) % 7;
			if (days[p]) {
				return true;
			}
			return false;
		}

		/**
		 * Check if the month of "cal" is part of this rule
		 *
		 * @param cal the time to check
		 * @return true if the month is part of the rule
		 */
		@Override
		public boolean containsMonth(Calendar cal) {
			int i = cal.get(Calendar.MONTH);
			if (months[i]) {
				return true;
			}
			return false;
		}

		/**
		 * Check if this rule says the feature is open at time "cal"
		 *
		 * @param cal the time to check
		 * @return false in all other cases, also if only day is wrong
		 */
		@Override
		public boolean isOpenedForTime(Calendar cal, boolean checkPrevious) {
			int i = cal.get(Calendar.DAY_OF_WEEK);
			int d = (i + 5) % 7;
			int p = d - 1;
			if (p < 0) {
				p += 7;
			}
			int time = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE); // Time in minutes
			for (i = 0; i < startTimes.size(); i++) {
				int startTime = this.startTimes.get(i);
				int endTime = this.endTimes.get(i);
				if (startTime < endTime || endTime == -1) {
					// one day working like 10:00-20:00 (not 20:00-04:00)
					if (days[d] && !checkPrevious) {
						if (time >= startTime && (endTime == -1 || time <= endTime)) {
							return true;
						}
					}
				} else {
					// opening_hours includes day wrap like
					// "We 20:00-03:00" or "We 07:00-07:00"
					if (time >= startTime && days[d] && !checkPrevious) {
						return true;
					} else if (time < endTime && days[p] && checkPrevious) {
						// check in previous day
						return true;
					}
				}
			}
			return false;
		}


		@Override
		public String toRuleString(boolean avoidMonths) {
			return toRuleString(avoidMonths, daysStr, monthsStr);
		}

		private String toRuleString(boolean avoidMonths, String[] dayNames, String[] monthNames) {
			StringBuilder b = new StringBuilder(25);
			// Month
			boolean dash = false;
			boolean first = true;
			if (!avoidMonths) {
				for (int i = 0; i < 12; i++) {
					if (months[i]) {
						if (i > 0 && months[i - 1] && i < 11 && months[i + 1]) {
							if (!dash) {
								dash = true;
								b.append("-"); //$NON-NLS-1$
							}
							continue;
						}
						if (first) {
							first = false;
						} else if (!dash) {
							b.append(", "); //$NON-NLS-1$
						}
						b.append(monthNames[i]);
						dash = false;
					}
				}
				if (b.length() != 0) {
					b.append(": ");
				}
			}
			// Day
			boolean open24_7 = true;
			for (int i = 0; i < 7; i++) {
				if (!days[i]) {
					open24_7 = false;
					break;
				}
			}
			appendDaysString(b, dayNames);
			// Time
			if (startTimes == null || startTimes.size() == 0) {
				b.append(" off ");
			} else {
				for (int i = 0; i < startTimes.size(); i++) {
					int startTime = startTimes.get(i);
					int endTime = endTimes.get(i);
					if (open24_7 && startTime == 0 && endTime / 60 == 24) {
						return "24/7";
					}
					b.append(" "); //$NON-NLS-1$
					int stHour = startTime / 60;
					int stTime = startTime - stHour * 60;
					int enHour = endTime / 60;
					int enTime = endTime - enHour * 60;
					formatTime(stHour, stTime, b);
					b.append("-"); //$NON-NLS-1$
					formatTime(enHour, enTime, b);
					b.append(",");
				}
			}
			return b.substring(0, b.length() - 1);
		}

		@Override
		public String toLocalRuleString() {
			return toRuleString(true, localDaysStr, localMothsStr);
		}

		@Override
		public String toString() {
			return toRuleString(false);
		}

		public void appendDaysString(StringBuilder builder) {
			appendDaysString(builder, daysStr);
		}

		public void appendDaysString(StringBuilder builder, String[] daysNames) {
			boolean dash = false;
			boolean first = true;
			for (int i = 0; i < 7; i++) {
				if (days[i]) {
					if (i > 0 && days[i - 1] && i < 6 && days[i + 1]) {
						if (!dash) {
							dash = true;
							builder.append("-"); //$NON-NLS-1$
						}
						continue;
					}
					if (first) {
						first = false;
					} else if (!dash) {
						builder.append(", "); //$NON-NLS-1$
					}
					builder.append(daysNames[getDayIndex(i)]);
					dash = false;
				}
			}
		}

		/**
		 * Add a time range (startTime-endTime) to this rule
		 *
		 * @param startTime startTime to add
		 * @param endTime   endTime to add
		 */
		public void addTimeRange(int startTime, int endTime) {
			startTimes.add(startTime);
			endTimes.add(endTime);
		}

		public int timesSize() {
			return startTimes.size();
		}

		public void deleteTimeRange(int position) {
			startTimes.removeAt(position);
			endTimes.removeAt(position);
		}

		private static void setSingleValueForArrayList(TIntArrayList arrayList, int s) {
			if (arrayList.size() > 0) {
				arrayList.remove(0, arrayList.size());
			}
			arrayList.add(s);
		}
	}

	public static class UnparseableRule implements OpeningHoursParser.OpeningHoursRule {
		private String ruleString;

		public UnparseableRule(String ruleString) {
			this.ruleString = ruleString;
		}

		@Override
		public boolean isOpenedForTime(Calendar cal, boolean checkPrevious) {
			return false;
		}

		@Override
		public boolean containsPreviousDay(Calendar cal) {
			return false;
		}

		@Override
		public boolean containsDay(Calendar cal) {
			return false;
		}

		@Override
		public boolean containsMonth(Calendar cal) {
			return false;
		}

		@Override
		public String toRuleString(boolean avoidMonths) {
			return ruleString;
		}

		@Override
		public String toLocalRuleString() {
			return toRuleString(false);
		}

		@Override
		public String toString() {
			return toRuleString(false);
		}
	}


	/**
	 * Parse an opening_hours string from OSM to an OpeningHours object which can be used to check
	 *
	 * @param r the string to parse
	 * @return BasicRule if the String is successfully parsed and UnparseableRule otherwise
	 */
	public static OpeningHoursParser.OpeningHoursRule parseRule(String r) {
		// replace words "sunrise" and "sunset" by real hours
		r = r.toLowerCase();
		final String[] daysStr = new String[]{"mo", "tu", "we", "th", "fr", "sa", "su"};
		final String[] monthsStr = new String[]{"jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};
		final String[] holidayStr = new String[]{"ph", "sh"};
		String sunrise = "07:00";
		String sunset = "21:00";
		String endOfDay = "24:00";

		String localRuleString = r.replaceAll("sunset", sunset).replaceAll("sunrise", sunrise)
				.replaceAll("\\+", "-" + endOfDay);
		int startDay = -1;
		int previousDay = -1;
		int startMonth = -1;
		int previousMonth = -1;
		int k = 0; // Position in opening_hours string

		BasicOpeningHourRule basic = new BasicOpeningHourRule();
		boolean[] days = basic.getDays();
		boolean[] months = basic.getMonths();
		// check 24/7
		if ("24/7".equals(localRuleString)) {
			Arrays.fill(days, true);
			Arrays.fill(months, true);
			basic.addTimeRange(0, 24 * 60);
			return basic;
		}

		for (; k < localRuleString.length(); k++) {
			char ch = localRuleString.charAt(k);
			if (Character.isDigit(ch)) {
				// time starts
				break;
			}
			if ((k + 2 < localRuleString.length())
					&& localRuleString.substring(k, k + 3).equals("off")) {
				// value "off" is found
				break;
			}
			if (Character.isWhitespace(ch) || ch == ',') {
			} else if (ch == '-') {
				if (previousDay != -1) {
					startDay = previousDay;
				} else if (previousMonth != -1) {
					startMonth = previousMonth;
				} else {
					return new UnparseableRule(r);
				}
			} else if (k < r.length() - 1) {
				int i = 0;
				for (String s : daysStr) {
					if (s.charAt(0) == ch && s.charAt(1) == r.charAt(k + 1)) {
						break;
					}
					i++;
				}
				if (i < daysStr.length) {
					if (startDay != -1) {
						for (int j = startDay; j <= i; j++) {
							days[j] = true;
						}
						if (startDay > i) {// overflow handling, e.g. Su-We
							for (int j = startDay; j <= 6; j++) {
								days[j] = true;
							}
							for (int j = 0; j <= i; j++) {
								days[j] = true;
							}
						}
						startDay = -1;
					} else {
						days[i] = true;
					}
					previousDay = i;
				} else {
					// Read Month
					int m = 0;
					for (String s : monthsStr) {
						if (s.charAt(0) == ch && s.charAt(1) == r.charAt(k + 1)
								&& s.charAt(2) == r.charAt(k + 2)) {
							break;
						}
						m++;
					}
					if (m < monthsStr.length) {
						if (startMonth != -1) {
							for (int j = startMonth; j <= m; j++) {
								months[j] = true;
							}
							if (startMonth > m) {// overflow handling, e.g. Oct-Mar
								for (int j = startMonth; j <= 11; j++) {
									months[j] = true;
								}
								for (int j = 0; j <= m; j++) {
									months[j] = true;
								}
							}
							startMonth = -1;
						} else {
							months[m] = true;
						}
						previousMonth = m;
					}
					if (previousMonth == -1) {
						int h = 0;
						for (String s : holidayStr) {
							if (s.charAt(0) == ch && s.charAt(1) == r.charAt(k + 1)) {
								return new UnparseableRule(r);
							}
						}
					}
				}
			} else {
				return new UnparseableRule(r);
			}
		}
		if (previousDay == -1) {
			// no days given => take all days.
			for (int i = 0; i < 7; i++) {
				days[i] = true;
			}
		}
		if (previousMonth == -1) {
			// no month given => take all months.
			for (int i = 0; i < 12; i++) {
				months[i] = true;
			}
		}
		String timeSubstr = localRuleString.substring(k);
		String[] times = timeSubstr.split(",");
		boolean timesExist = true;
		for (int i = 0; i < times.length; i++) {
			String time = times[i];
			time = time.trim();
			if (time.length() == 0) {
				continue;
			}
			if (time.equals("off")) {
				break; // add no time values
			}
			if (time.equals("24/7")) {
				// for some reason, this is used. See tagwatch.
				basic.addTimeRange(0, 24 * 60);
				break;
			}
			String[] stEnd = time.split("-"); //$NON-NLS-1$
			if (stEnd.length != 2) {
				if (i == times.length - 1 && basic.getStartTime() == 0 && basic.getEndTime() == 0) {
					return new UnparseableRule(r);
				}
				continue;
			}
			timesExist = true;
			int st;
			int end;
			try {
				int i1 = stEnd[0].indexOf(':');
				int i2 = stEnd[1].indexOf(':');
				int startHour, startMin, endHour, endMin;
				if (i1 == -1) {
					// if no minutes are given, try complete value as hour
					startHour = Integer.parseInt(stEnd[0].trim());
					startMin = 0;
				} else {
					startHour = Integer.parseInt(stEnd[0].substring(0, i1).trim());
					startMin = Integer.parseInt(stEnd[0].substring(i1 + 1).trim());
				}
				if (i2 == -1) {
					// if no minutes are given, try complete value as hour
					endHour = Integer.parseInt(stEnd[1].trim());
					endMin = 0;
				} else {
					endHour = Integer.parseInt(stEnd[1].substring(0, i2).trim());
					endMin = Integer.parseInt(stEnd[1].substring(i2 + 1).trim());
				}
				st = startHour * 60 + startMin;
				end = endHour * 60 + endMin;
			} catch (NumberFormatException e) {
				return new UnparseableRule(r);
			}
			basic.addTimeRange(st, end);
		}
		if (!timesExist) {
			return new UnparseableRule(r);
		}
		return basic;
	}

	/**
	 * parse OSM opening_hours string to an OpeningHours object
	 *
	 * @param format the string to parse
	 * @return null when parsing was unsuccessful
	 */
	public static OpeningHours parseOpenedHours(String format) {
		if (format == null) {
			return null;
		}
		// split the OSM string in multiple rules
		String[] rules = format.split(";"); //$NON-NLS-1$
		// FIXME: What if the semicolon is inside a quoted string?
		OpeningHours rs = new OpeningHours();
		for (String r : rules) {
			r = r.trim();
			if (r.length() == 0) {
				continue;
			}
			// check if valid
			final OpeningHoursRule r1 = parseRule(r);
			boolean rule = r1 instanceof BasicOpeningHourRule;
			if (rule) {
				rs.addRule(r1);
			}
		}
		return rs;
	}

	/**
	 * parse OSM opening_hours string to an OpeningHours object.
	 * Does not return null when parsing unsuccessful. When parsing rule is unsuccessful,
	 * such rule is stored as UnparseableRule.
	 *
	 * @param format the string to parse
	 * @return the OpeningHours object
	 */
	public static OpeningHoursParser.OpeningHours parseOpenedHoursHandleErrors(String format) {
		if (format == null) {
			return null;
		}
		String[] rules = format.split(";"); //$NON-NLS-1$
		OpeningHoursParser.OpeningHours rs = new OpeningHoursParser.OpeningHours();
		for (String r : rules) {
			r = r.trim();
			if (r.length() == 0) {
				continue;
			}
			// check if valid
			rs.addRule(OpeningHoursParser.parseRule(r));
		}
		return rs;
	}

	private static void formatTime(int h, int t, StringBuilder b) {
		if (h < 10) {
			b.append("0"); //$NON-NLS-1$
		}
		b.append(h).append(":"); //$NON-NLS-1$
		if (t < 10) {
			b.append("0"); //$NON-NLS-1$
		}
		b.append(t);
	}


	/**
	 * test if the calculated opening hours are what you expect
	 *
	 * @param time     the time to test in the format "dd.MM.yyyy HH:mm"
	 * @param hours    the OpeningHours object
	 * @param expected the expected state
	 */
	private static void testOpened(String time, OpeningHours hours, boolean expected) throws ParseException {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).parse(time));
		boolean calculated = hours.isOpenedForTime(cal);
		System.out.printf("  %sok: Expected %s: %b = %b (rule %s)\n",
				((calculated != expected) ? "NOT " : ""), time, expected, calculated, hours.getCurrentRuleTime(cal));
		if (calculated != expected) {
			throw new IllegalArgumentException("BUG!!!");
		}
	}

	private static void testParsedAndAssembledCorrectly(String timeString, OpeningHours hours) {
		String assembledString = hours.toStringNoMonths();
		boolean isCorrect = assembledString.equalsIgnoreCase(timeString);
		System.out.printf("  %sok: Expected: \"%s\" got: \"%s\"\n",
				(isCorrect ? "NOT " : ""), timeString, assembledString);
		if (!isCorrect) {
			throw new IllegalArgumentException("BUG!!!");
		}
	}

	public static void main(String[] args) throws ParseException {

		// Test basic case
		OpeningHours hours = parseOpenedHours("Mo-Fr 08:30-14:40"); //$NON-NLS-1$
		System.out.println(hours);
		testOpened("09.08.2012 11:00", hours, true);
		testOpened("09.08.2012 16:00", hours, false);
		hours = parseOpenedHours("mo-fr 07:00-19:00; sa 12:00-18:00");
		System.out.println(hours);

		hours = parseOpenedHours("Mo-Fr 11:30-15:00,17:30-23:00; Sa-Su,PH 11:30-23:00");
		System.out.println(hours);
		testOpened("7.09.2015 14:54", hours, true);
		testOpened("7.09.2015 15:05", hours, false);


		// two time and date ranges
		hours = parseOpenedHours("Mo-We, Fr 08:30-14:40,15:00-19:00"); //$NON-NLS-1$
		System.out.println(hours);
		testOpened("08.08.2012 14:00", hours, true);
		testOpened("08.08.2012 14:50", hours, false);
		testOpened("10.08.2012 15:00", hours, true);

		// test exception on general schema
		hours = parseOpenedHours("Mo-Sa 08:30-14:40; Tu 08:00 - 14:00"); //$NON-NLS-1$
		System.out.println(hours);
		testOpened("07.08.2012 14:20", hours, false);

		// test off value
		hours = parseOpenedHours("Mo-Sa 09:00-18:25; Th off"); //$NON-NLS-1$
		System.out.println(hours);
		testOpened("08.08.2012 12:00", hours, true);
		testOpened("09.08.2012 12:00", hours, false);

		//test 24/7
		hours = parseOpenedHours("24/7"); //$NON-NLS-1$
		System.out.println(hours);
		testOpened("08.08.2012 23:59", hours, true);
		testOpened("08.08.2012 12:23", hours, true);
		testOpened("08.08.2012 06:23", hours, true);

		// some people seem to use the following syntax:
		hours = parseOpenedHours("Sa-Su 24/7");
		System.out.println(hours);
		hours = parseOpenedHours("Mo-Fr 9-19");
		System.out.println(hours);
		hours = parseOpenedHours("09:00-17:00");
		System.out.println(hours);
		hours = parseOpenedHours("sunrise-sunset");
		System.out.println(hours);
		hours = parseOpenedHours("10:00+");
		System.out.println(hours);
		hours = parseOpenedHours("Su-Th sunset-24:00, 04:00-sunrise; Fr-Sa sunset-sunrise");
		System.out.println(hours);
		testOpened("12.08.2012 04:00", hours, true);
		testOpened("12.08.2012 23:00", hours, true);
		testOpened("08.08.2012 12:00", hours, false);
		testOpened("08.08.2012 05:00", hours, true);

		// test simple day wrap
		hours = parseOpenedHours("Mo 20:00-02:00");
		System.out.println(hours);
		testOpened("05.05.2013 10:30", hours, false);
		testOpened("05.05.2013 23:59", hours, false);
		testOpened("06.05.2013 10:30", hours, false);
		testOpened("06.05.2013 20:30", hours, true);
		testOpened("06.05.2013 23:59", hours, true);
		testOpened("07.05.2013 00:00", hours, true);
		testOpened("07.05.2013 00:30", hours, true);
		testOpened("07.05.2013 01:59", hours, true);
		testOpened("07.05.2013 20:30", hours, false);

		// test maximum day wrap
		hours = parseOpenedHours("Su 10:00-10:00");
		System.out.println(hours);
		testOpened("05.05.2013 09:59", hours, false);
		testOpened("05.05.2013 10:00", hours, true);
		testOpened("05.05.2013 23:59", hours, true);
		testOpened("06.05.2013 00:00", hours, true);
		testOpened("06.05.2013 09:59", hours, true);
		testOpened("06.05.2013 10:00", hours, false);

		// test day wrap as seen on OSM
		// Incorrectly evaluated: https://wiki.openstreetmap.org/w/index.php?title=Key:opening_hours/specification#explain:additional_rule_separator
		// <normal_rule_separator> does overwrite previous definitions.
		// VICTOR: Do we have a test for incorrectly evaluated?
		hours = parseOpenedHours("Tu-Th 07:00-2:00; Fr 17:00-4:00; Sa 18:00-05:00; Su,Mo off");
		System.out.println(hours);
		testOpened("05.05.2013 04:59", hours, true);
		testOpened("05.05.2013 05:00", hours, false);
		testOpened("05.05.2013 12:30", hours, false);
		testOpened("06.05.2013 10:30", hours, false);
		testOpened("07.05.2013 01:00", hours, false);
		testOpened("07.05.2013 20:25", hours, true);
		testOpened("07.05.2013 23:59", hours, true);
		testOpened("08.05.2013 00:00", hours, true);
		testOpened("08.05.2013 02:00", hours, false);

		// test day wrap as seen on OSM
		hours = parseOpenedHours("Mo-Th 09:00-03:00; Fr-Sa 09:00-04:00; Su off");
		testOpened("11.05.2015 08:59", hours, false);
		testOpened("11.05.2015 09:01", hours, true);
		testOpened("12.05.2015 02:59", hours, true);
		testOpened("12.05.2015 03:00", hours, false);
		testOpened("16.05.2015 03:59", hours, true);
		testOpened("16.05.2015 04:01", hours, false);
		testOpened("17.05.2015 01:00", hours, true);
		testOpened("17.05.2015 04:01", hours, false);

		hours = parseOpenedHours("Tu-Th 07:00-2:00; Fr 17:00-4:00; Sa 18:00-05:00; Su,Mo off");
		testOpened("11.05.2015 08:59", hours, false);
		testOpened("11.05.2015 09:01", hours, false);
		testOpened("12.05.2015 02:59", hours, false);
		testOpened("12.05.2015 03:00", hours, false);
		testOpened("16.05.2015 03:59", hours, true);
		testOpened("16.05.2015 04:01", hours, false);
		testOpened("17.05.2015 01:00", hours, true);
		testOpened("17.05.2015 05:01", hours, false);

		// tests single month value
		hours = parseOpenedHours("May: 07:00-19:00");
		System.out.println(hours);
		testOpened("05.05.2013 12:00", hours, true);
		testOpened("05.05.2013 05:00", hours, false);
		testOpened("05.05.2013 21:00", hours, false);
		testOpened("05.01.2013 12:00", hours, false);
		testOpened("05.01.2013 05:00", hours, false);

		// tests multi month value
		hours = parseOpenedHours("Apr-Sep: 8:00-22:00; Oct-Mar: 10:00-18:00");
		System.out.println(hours);
		testOpened("05.03.2013 15:00", hours, true);
		testOpened("05.03.2013 20:00", hours, false);

		testOpened("05.05.2013 20:00", hours, true);
		testOpened("05.05.2013 23:00", hours, false);

		testOpened("05.10.2013 15:00", hours, true);
		testOpened("05.10.2013 20:00", hours, false);

		// Test time with breaks
		hours = parseOpenedHours("Mo-Fr: 9:00-13:00, 14:00-18:00");
		System.out.println(hours);
		testOpened("02.12.2015 12:00", hours, true);
		testOpened("02.12.2015 13:30", hours, false);
		testOpened("02.12.2015 16:00", hours, true);

		testOpened("05.12.2015 16:00", hours, false);

		// Test holidays
		String hoursString = "mo-fr 11:00-21:00; ph off";
		hours = parseOpenedHoursHandleErrors(hoursString);
		testParsedAndAssembledCorrectly(hoursString, hours);
	}
}
