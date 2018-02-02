package net.osmand.util;
/* Can be commented out in order to run the main function separately */

import java.io.Serializable;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
	private static final Map<String, String> additionalStrings = new HashMap<>();

	private static final int LOW_TIME_LIMIT = 120;
	private static final int WITHOUT_TIME_LIMIT = -1;
	private static final int CURRENT_DAY_TIME_LIMIT = -2;

	static {
		DateFormatSymbols dateFormatSymbols = DateFormatSymbols.getInstance(Locale.US);
		monthsStr = dateFormatSymbols.getShortMonths();
		daysStr = getTwoLettersStringArray(dateFormatSymbols.getShortWeekdays());
		dateFormatSymbols = DateFormatSymbols.getInstance();
		localMothsStr = dateFormatSymbols.getShortMonths();
		localDaysStr = getTwoLettersStringArray(dateFormatSymbols.getShortWeekdays());

		additionalStrings.put("off", "off");
		additionalStrings.put("is_open", "Open");
		additionalStrings.put("is_open_24_7", "Open 24/7");
		additionalStrings.put("will_open_at", "Will open at");
		additionalStrings.put("open_from", "Open from");
		additionalStrings.put("will_close_at", "Will close at");
		additionalStrings.put("open_till", "Open till");
		additionalStrings.put("will_open_tomorrow_at", "Will open tomorrow at");
		additionalStrings.put("will_open_on", "Will open on");
	}

	/**
	 * Set additional localized strings like "off", etc.
	 */
	public static void setAdditionalString(String key, String value) {
		additionalStrings.put(key, value);
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
					newStrings[i] = Algorithms.capitalizeFirstLetter(strings[i].substring(0, 2));
				} else {
					newStrings[i] = Algorithms.capitalizeFirstLetter(strings[i]);
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

		public static final int ALL_SEQUENCES = -1;

		/**
		 * list of the different rules
		 */
		private ArrayList<OpeningHoursRule> rules;
		private String original;
		private int sequenceCount;

		public static class Info {

			private boolean opened;
			private boolean opened24_7;
			private String openingTime;
			private String nearToOpeningTime;
			private String closingTime;
			private String nearToClosingTime;
			private String openingTomorrow;
			private String openingDay;
			private String ruleString;

			public boolean isOpened() {
				return opened;
			}

			public boolean isOpened24_7() {
				return opened24_7;
			}

			public String getInfo() {
				if (isOpened24_7()) {
					if (!Algorithms.isEmpty(ruleString)) {
						return additionalStrings.get("is_open") + " " + ruleString;
					} else {
						return additionalStrings.get("is_open_24_7");
					}
				} else if (!Algorithms.isEmpty(nearToOpeningTime)) {
					return additionalStrings.get("will_open_at") + " " + nearToOpeningTime;
				} else if (!Algorithms.isEmpty(openingTime)) {
					return additionalStrings.get("open_from") + " " + openingTime;
				} else if (!Algorithms.isEmpty(nearToClosingTime)) {
					return additionalStrings.get("will_close_at") + " " + nearToClosingTime;
				} else if (!Algorithms.isEmpty(closingTime)) {
					return additionalStrings.get("open_till") + " " + closingTime;
				} else if (!Algorithms.isEmpty(openingTomorrow)) {
					return additionalStrings.get("will_open_tomorrow_at") + " " + openingTomorrow;
				} else if (!Algorithms.isEmpty(openingDay)) {
					return additionalStrings.get("will_open_on") + " " + openingDay + ".";
				} else if (!Algorithms.isEmpty(ruleString)) {
					return ruleString;
				} else {
					return "";
				}
			}
		}

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

		public List<Info> getInfo() {
			return getInfo(Calendar.getInstance());
		}
		
		public List<Info> getInfo(Calendar cal) {
			List<Info> res = new ArrayList<>();
			for (int i = 0; i < sequenceCount; i++) {
				Info info = getInfo(cal, i);
				res.add(info);
			}
			return res.isEmpty() ? null : res;
		}

		public Info getCombinedInfo() {
			return getCombinedInfo(Calendar.getInstance());
		}
		
		public Info getCombinedInfo(Calendar cal) {
			return getInfo(cal, ALL_SEQUENCES);
		}

		private Info getInfo(Calendar cal, int sequenceIndex) {
			Info info = new Info();
			boolean opened = isOpenedForTimeV2(cal, sequenceIndex);
			info.opened = opened;
			info.ruleString = getCurrentRuleTime(cal, sequenceIndex);
			if (opened) {
				info.opened24_7 = isOpened24_7(sequenceIndex);
				info.closingTime = getClosingTime(cal, sequenceIndex);
				info.nearToClosingTime = getNearToClosingTime(cal, sequenceIndex);
			} else {
				info.openingTime = getOpeningTime(cal, sequenceIndex);
				info.nearToOpeningTime = getNearToOpeningTime(cal, sequenceIndex);
				info.openingTomorrow = getOpeningTomorrow(cal, sequenceIndex);
				info.openingDay = getOpeningDay(cal, sequenceIndex);
			}
			return info;
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
		 * add rules to the opening hours
		 *
		 * @param rules to add
		 */
		public void addRules(List<? extends OpeningHoursRule> rules) {
			this.rules.addAll(rules);
		}

		public int getSequenceCount() {
			return sequenceCount;
		}

		public void setSequenceCount(int sequenceCount) {
			this.sequenceCount = sequenceCount;
		}

		/**
		 * return the list of rules
		 *
		 * @return the rules
		 */
		public ArrayList<OpeningHoursRule> getRules() {
			return rules;
		}

		public ArrayList<OpeningHoursRule> getRules(int sequenceIndex) {
			if (sequenceIndex == ALL_SEQUENCES) {
				return rules;
			} else {
				ArrayList<OpeningHoursRule> sequenceRules = new ArrayList<>();
				for (OpeningHoursRule r : rules) {
					if (r.getSequenceIndex() == sequenceIndex) {
						sequenceRules.add(r);
					}
				}
				return sequenceRules;
			}
		}

		/**
		 * check if the feature is opened at time "cal"
		 *
		 * @param cal the time to check
		 * @return true if feature is open
		 */
		public boolean isOpenedForTimeV2(Calendar cal, int sequenceIndex) {
			// make exception for overlapping times i.e.
			// (1) Mo 14:00-16:00; Tu off
			// (2) Mo 14:00-02:00; Tu off
			// in (2) we need to check first rule even though it is against specification
			ArrayList<OpeningHoursRule> rules = getRules(sequenceIndex);
			boolean overlap = false;
			for (int i = rules.size() - 1; i >= 0 ; i--) {
				OpeningHoursRule r = rules.get(i);
				if (r.hasOverlapTimes()) {
					overlap = true;
					break;
				}
			}
			// start from the most specific rule
			for (int i = rules.size() - 1; i >= 0 ; i--) {
				OpeningHoursRule r = rules.get(i);
				if (r.contains(cal)) {
					boolean open = r.isOpenedForTime(cal);
					if (!open && overlap ) {
						continue;
					} else {
						return open;
					}
				}
			}
			return false;
		}

		/**
		 * check if the feature is opened at time "cal"
		 *
		 * @param cal the time to check
		 * @return true if feature is open
		 */
		public boolean isOpenedForTime(Calendar cal) {
			return isOpenedForTimeV2(cal, ALL_SEQUENCES);
		}

		/**
		 * check if the feature is opened at time "cal"
		 *
		 * @param cal the time to check
		 * @param sequenceIndex the sequence index to check
		 * @return true if feature is open
		 */
		public boolean isOpenedForTime(Calendar cal, int sequenceIndex) {
			/*
			 * first check for rules that contain the current day
			 * afterwards check for rules that contain the previous
			 * day with overlapping times (times after midnight)
			 */
			boolean isOpenDay = false;
			ArrayList<OpeningHoursRule> rules = getRules(sequenceIndex);
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

		public boolean isOpened24_7(int sequenceIndex) {
			boolean opened24_7 = false;
			ArrayList<OpeningHoursRule> rules = getRules(sequenceIndex);
			for (OpeningHoursRule r : rules) {
				opened24_7 = r.isOpened24_7();
			}
			return opened24_7;
		}

		public String getNearToOpeningTime(Calendar cal, int sequenceIndex) {
			return getTime(cal, LOW_TIME_LIMIT, true, sequenceIndex);
		}

		public String getOpeningTime(Calendar cal, int sequenceIndex) {
			return getTime(cal, CURRENT_DAY_TIME_LIMIT, true, sequenceIndex);
		}

		public String getNearToClosingTime(Calendar cal, int sequenceIndex) {
			return getTime(cal, LOW_TIME_LIMIT, false, sequenceIndex);
		}

		public String getClosingTime(Calendar cal, int sequenceIndex) {
			return getTime(cal, WITHOUT_TIME_LIMIT, false, sequenceIndex);
		}

		public String getOpeningTomorrow(Calendar calendar, int sequenceIndex) {
			Calendar cal = (Calendar) calendar.clone();
			String openingTime = "";
			ArrayList<OpeningHoursRule> rules = getRules(sequenceIndex);
			cal.add(Calendar.DAY_OF_MONTH, 1);
			for (OpeningHoursRule r : rules) {
				if (r.containsDay(cal) && r.containsMonth(cal)) {
					openingTime = r.getTime(cal, false, WITHOUT_TIME_LIMIT, true);
				}
			}
			return openingTime;
		}

		public String getOpeningDay(Calendar calendar, int sequenceIndex) {
			Calendar cal = (Calendar) calendar.clone();
			String openingTime = "";
			ArrayList<OpeningHoursRule> rules = getRules(sequenceIndex);
			for (int i = 0; i < 7; i++) {
				cal.add(Calendar.DAY_OF_MONTH, 1);
				for (OpeningHoursRule r : rules) {
					if (r.containsDay(cal) && r.containsMonth(cal)) {
						openingTime = r.getTime(cal, false, WITHOUT_TIME_LIMIT, true);
					}
				}
				if (!Algorithms.isEmpty(openingTime)) {
					openingTime += " " + localDaysStr[cal.get(Calendar.DAY_OF_WEEK)];
					break;
				}
			}
			return openingTime;
		}

		private String getTime(Calendar cal, int limit, boolean opening, int sequenceIndex) {
			String time = getTimeDay(cal, limit, opening, sequenceIndex);
			if (Algorithms.isEmpty(time)) {
				time = getTimeAnotherDay(cal, limit, opening, sequenceIndex);
			}
			return time;
		}

		private String getTimeDay(Calendar cal, int limit, boolean opening, int sequenceIndex) {
			String atTime = "";
			ArrayList<OpeningHoursRule> rules = getRules(sequenceIndex);
			for (OpeningHoursRule r : rules) {
				if (r.containsDay(cal) && r.containsMonth(cal)) {
					atTime = r.getTime(cal, false, limit, opening);
				}
			}
			return atTime;
		}

		private String getTimeAnotherDay(Calendar cal, int limit, boolean opening, int sequenceIndex) {
			String atTime = "";
			ArrayList<OpeningHoursRule> rules = getRules(sequenceIndex);
			for (OpeningHoursRule r : rules) {
				if (((opening && r.containsPreviousDay(cal)) || (!opening && r.containsNextDay(cal))) && r.containsMonth(cal)) {
					atTime = r.getTime(cal, true, limit, opening);
				}
			}
			return atTime;
		}

		public String getCurrentRuleTime(Calendar cal) {
			return getCurrentRuleTime(cal, ALL_SEQUENCES);
		}

		public String getCurrentRuleTime(Calendar cal, int sequenceIndex) {
			// make exception for overlapping times i.e.
			// (1) Mo 14:00-16:00; Tu off
			// (2) Mo 14:00-02:00; Tu off
			// in (2) we need to check first rule even though it is against specification
			ArrayList<OpeningHoursRule> rules = getRules(sequenceIndex);
			String ruleClosed = null;
			boolean overlap = false;
			for (int i = rules.size() - 1; i >= 0; i--) {
				OpeningHoursRule r = rules.get(i);
				if (r.hasOverlapTimes()) {
					overlap = true;
					break;
				}
			}
			// start from the most specific rule
			for (int i = rules.size() - 1; i >= 0; i--) {
				OpeningHoursRule r = rules.get(i);
				if (r.contains(cal)) {
					boolean open = r.isOpenedForTime(cal);
					if (!open && overlap) {
						ruleClosed = r.toLocalRuleString();
					} else {
						return r.toLocalRuleString();
					}
				}
			}
			return ruleClosed;
		}
		
		public String getCurrentRuleTimeV1(Calendar cal) {
			String ruleOpen = null;
			String ruleClosed = null;
			for (OpeningHoursRule r : rules) {
				if (r.containsPreviousDay(cal) && r.containsMonth(cal)) {
					if (r.isOpenedForTime(cal, true)) {
						ruleOpen = r.toLocalRuleString();
					} else {
						ruleClosed = r.toLocalRuleString();
					}
				}
			}
			for (OpeningHoursRule r : rules) {
				if (r.containsDay(cal) && r.containsMonth(cal)) {
					if (r.isOpenedForTime(cal, false)) {
						ruleOpen = r.toLocalRuleString();
					} else {
						ruleClosed = r.toLocalRuleString();
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

		public String toLocalString() {
			StringBuilder s = new StringBuilder();
			if (rules.isEmpty()) {
				return "";
			}

			for (OpeningHoursRule r : rules) {
				s.append(r.toLocalRuleString()).append("; ");
			}

			return s.substring(0, s.length() - 2);
		}

		public void setOriginal(String original) {
			this.original = original;
		}
		
		public String getOriginal() {
			return original;
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
		 * Check if, for this rule, the feature is opened for time "cal" 
		 * @param cal
		 * @return true if the feature is open
		 */
		public boolean isOpenedForTime(Calendar cal);

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
		 * Check if the next day after "cal" is part of this rule
		 *
		 * @param cal the time to check
		 * @return true if the next day is part of the rule
		 */
		boolean containsNextDay(Calendar cal);

		/**
		 * Check if the month of "cal" is part of this rule
		 *
		 * @param cal the time to check
		 * @return true if the month is part of the rule
		 */
		public boolean containsMonth(Calendar cal);
		
		/**
		 * @return true if the rule overlap to the next day
		 */
		public boolean hasOverlapTimes();
		
		/**
		 * @param cal
		 * @return true if rule applies for current time
		 */
		public boolean contains(Calendar cal);

		public int getSequenceIndex();

		public String toRuleString();

		public String toLocalRuleString();

		boolean isOpened24_7();

		String getTime(Calendar cal, boolean checkAnotherDay, int limit, boolean opening);
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
		 * represents the list on which day it is open.
		 */
		private boolean[] dayMonths = new boolean[31];

		/**
		 * lists of equal size representing the start and end times
		 */
		private TIntArrayList startTimes = new TIntArrayList(), endTimes = new TIntArrayList();
		
		private boolean publicHoliday = false;
		private boolean schoolHoliday = false;
		private boolean easter = false;
		
		/**
		 * Flag that means that time is off
		 */
		private boolean off = false;

		/**
		 * Additional information or limitation.
		 * https://wiki.openstreetmap.org/wiki/Key:opening_hours/specification#explain:comment
		 */
		private String comment;

		private int sequenceIndex;

		public BasicOpeningHourRule() {
			this.sequenceIndex = 0;
		}

		public BasicOpeningHourRule(int sequenceIndex) {
			this.sequenceIndex = sequenceIndex;
		}

		public int getSequenceIndex() {
			return sequenceIndex;
		}

		/**
		 * return an array representing the days of the rule
		 *
		 * @return the days of the rule
		 */
		public boolean[] getDays() {
			return days;
		}
		
		/**
		 * @return the day months of the rule
		 */
		public boolean[] getDayMonths() {
			return dayMonths;
		}

		/**
		 * return an array representing the months of the rule
		 *
		 * @return the months of the rule
		 */
		public boolean[] getMonths() {
			return months;
		}
		
		public boolean appliesToPublicHolidays() {
			return publicHoliday;
		}
		
		public boolean appliesEaster() {
			return easter;
		}
		
		public boolean appliesToSchoolHolidays() {
			return schoolHoliday;
		}

		public String getComment() {
			return comment;
		}

		public void setComment(String comment) {
			this.comment = comment;
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

		@Override
		public boolean containsNextDay(Calendar cal) {
			int i = cal.get(Calendar.DAY_OF_WEEK);
			int p = (i + 6) % 7;
			if (days[p]) {
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
			int d = getCurrentDay(cal);
			int p = getPreviousDay(d);
			int time = getCurrentTimeInMinutes(cal); // Time in minutes
			for (int i = 0; i < startTimes.size(); i++) {
				int startTime = this.startTimes.get(i);
				int endTime = this.endTimes.get(i);
				if (startTime < endTime || endTime == -1) {
					// one day working like 10:00-20:00 (not 20:00-04:00)
					if (days[d] && !checkPrevious) {
						if (time >= startTime && (endTime == -1 || time <= endTime)) {
							return !off;
						}
					}
				} else {
					// opening_hours includes day wrap like
					// "We 20:00-03:00" or "We 07:00-07:00"
					if (time >= startTime && days[d] && !checkPrevious) {
						return !off;
					} else if (time < endTime && days[p] && checkPrevious) {
						// check in previous day
						return !off;
					}
				}
			}
			return false;
		}

		private int getCurrentDay(Calendar cal) {
			int i = cal.get(Calendar.DAY_OF_WEEK);
			return (i + 5) % 7;
		}

		private int getPreviousDay(int currentDay) {
			int p = currentDay - 1;
			if (p < 0) {
				p += 7;
			}
			return p;
		}

		private int getNextDay(int currentDay) {
			int n = currentDay + 1;
			if (n > 6) {
				n -= 7;
			}
			return n;
		}

		private int getCurrentTimeInMinutes(Calendar cal) {
			return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
		}

		@Override
		public String toRuleString() {
			return toRuleString(daysStr, monthsStr);
		}

		private String toRuleString(String[] dayNames, String[] monthNames) {
			StringBuilder b = new StringBuilder(25);
			boolean allMonths = true;
			for (int i = 0; i < months.length; i++) {
				if (!months[i]) {
					allMonths = false;
					break;
				}
			}
			// Month
			if (!allMonths) {
				addArray(months, monthNames, b);
			}
			boolean allDays = true;
			for (int i = 0; i < dayMonths.length; i++) {
				if (!dayMonths[i]) {
					allDays = false;
					break;
				}
			}
			if (!allDays) {
				addArray(dayMonths, null, b);
			}
			// Day
			appendDaysString(b, dayNames);
			// Time
			if (startTimes == null || startTimes.size() == 0) {
				if (isOpened24_7()) {
					b.setLength(0);
					b.append("24/7 ");
				}
				if (off) {
					b.append(additionalStrings.get("off"));
				}
			} else {
				if (isOpened24_7()) {
					b.setLength(0);
					b.append("24/7");
				} else {
					for (int i = 0; i < startTimes.size(); i++) {
						int startTime = startTimes.get(i);
						int endTime = endTimes.get(i);
						if (i > 0) {
							b.append(", ");
						}
						int stHour = startTime / 60;
						int stTime = startTime - stHour * 60;
						int enHour = endTime / 60;
						int enTime = endTime - enHour * 60;
						formatTime(stHour, stTime, b);
						b.append("-"); //$NON-NLS-1$
						formatTime(enHour, enTime, b);
					}
					if (off) {
						b.append(" ").append(additionalStrings.get("off"));
					}
				}
			}
			if (!Algorithms.isEmpty(comment)) {
				if (b.length() > 0) {
					if (b.charAt(b.length() - 1) != ' ') {
						b.append(" ");
					}
					b.append("- ").append(comment);
				} else {
					b.append(comment);
				}
			}
			return b.toString();
		}

		private void addArray(boolean[] array, String[] arrayNames, StringBuilder b) {
			boolean dash = false;
			boolean first = true;
			for (int i = 0; i < array.length; i++) {
				if (array[i]) {
					if (i > 0 && array[i - 1] && i < array.length - 1 && array[i + 1]) {
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
					b.append(arrayNames == null ? (i + 1) : arrayNames[i]);
					dash = false;
				}
			}
			if(!first) {
				b.append(" ");
			}
		}

		@Override
		public String toLocalRuleString() {
			return toRuleString(localDaysStr, localMothsStr);
		}

		@Override
		public boolean isOpened24_7() {
			boolean opened24_7 = true;
			for (int i = 0; i < 7; i++) {
				if (!days[i]) {
					opened24_7 = false;
					break;
				}
			}

			if (opened24_7) {
				if (startTimes != null && startTimes.size() > 0) {
					for (int i = 0; i < startTimes.size(); i++) {
						int startTime = startTimes.get(i);
						int endTime = endTimes.get(i);
						if (startTime == 0 && endTime / 60 == 24) {
							return true;
						}
					}
				} else {
					return true;
				}
			}
			return false;
		}

		@Override
		public String getTime(Calendar cal, boolean checkAnotherDay, int limit, boolean opening) {
			StringBuilder sb = new StringBuilder();
			int d = getCurrentDay(cal);
			int ad = opening ? getNextDay(d) : getPreviousDay(d);
			int time = getCurrentTimeInMinutes(cal);
			for (int i = 0; i < startTimes.size(); i++) {
				int startTime = startTimes.get(i);
				int endTime = endTimes.get(i);
				if (opening != off) {
					if (startTime < endTime || endTime == -1) {
						if (days[d] && !checkAnotherDay) {
							int diff = startTime - time;
							if (limit == WITHOUT_TIME_LIMIT || (time <= startTime && (diff <= limit || limit == CURRENT_DAY_TIME_LIMIT))) { 
								formatTime(startTime, sb);
								break;
							}
						}
					} else {
						int diff = -1;
						if (time <= startTime && days[d] && !checkAnotherDay) {
							diff = startTime - time;
						} else if (time > endTime && days[ad] && checkAnotherDay) {
							diff = 24 * 60 - endTime  + time;
						}
						if (limit == WITHOUT_TIME_LIMIT || ((diff != -1 && diff <= limit) || limit == CURRENT_DAY_TIME_LIMIT)) {
							formatTime(startTime, sb);
							break;
						}
					}
				} else {
					if (startTime < endTime && endTime != -1) {
						if (days[d] && !checkAnotherDay) {
							int diff = endTime - time;
							if ((limit == WITHOUT_TIME_LIMIT && diff >= 0) || (time <= endTime && diff <= limit)) {
								formatTime(endTime, sb);
								break;
							}
						}
					} else {
						int diff = -1;
						if (time <= endTime && days[d] && !checkAnotherDay) {
							diff = 24 * 60 - time + endTime;
						} else if (time < endTime && days[ad] && checkAnotherDay) {
							diff = endTime - time;
						}
						if (limit == WITHOUT_TIME_LIMIT || (diff != -1 && diff <= limit)) {
							formatTime(endTime, sb);
							break;
						}
					}
				}
			}
			String res = sb.toString();
			if (res.length() > 0 && !Algorithms.isEmpty(comment)) {
				res += " - " + comment;
			}
			return res;
		}

		@Override
		public String toString() {
			return toRuleString();
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
			if (publicHoliday) {
				if (!first) {
					builder.append(", ");
				}
				builder.append("PH");
				first = false;
			}
			if (schoolHoliday) {
				if (!first) {
					builder.append(", ");
				}
				builder.append("SH");
				first = false;
			}
			if (easter) {
				if (!first) {
					builder.append(", ");
				}
				builder.append("Easter");
				first = false;
			}
			if(!first) {
				builder.append(" ");
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

		@Override
		public boolean isOpenedForTime(Calendar cal) {
			int c = calculate(cal);
			return c > 0;
		}

		@Override
		public boolean contains(Calendar cal) {
			int c = calculate(cal);
			return c != 0;
		}

		@Override
		public boolean hasOverlapTimes() {
			for (int i = 0; i < startTimes.size(); i++) {
				int startTime = this.startTimes.get(i);
				int endTime = this.endTimes.get(i);
				if (startTime >= endTime && endTime != -1) {
					return true;
				}
			}
			return false;
		}

		private int calculate(Calendar cal) {
			int month = cal.get(Calendar.MONTH);
			if (!months[month]) {
				return 0;
			}
			int dmonth = cal.get(Calendar.DAY_OF_MONTH) - 1;
			int i = cal.get(Calendar.DAY_OF_WEEK);
			int day = (i + 5) % 7;
			int previous = (day + 6) % 7;
			boolean thisDay = days[day] || dayMonths[dmonth];
			// potential error for Dec 31 12:00-01:00
			boolean previousDay = days[previous] || (dmonth > 0 && dayMonths[dmonth - 1]);
			if (!thisDay && !previousDay) {
				return 0;
			}
			int time = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE); // Time in minutes
			for (i = 0; i < startTimes.size(); i++) {
				int startTime = this.startTimes.get(i);
				int endTime = this.endTimes.get(i);
				if (startTime < endTime || endTime == -1) {
					// one day working like 10:00-20:00 (not 20:00-04:00)
					if (time >= startTime && (endTime == -1 || time <= endTime) && thisDay) {
						return off ? -1 : 1;
					}
				} else {
					// opening_hours includes day wrap like
					// "We 20:00-03:00" or "We 07:00-07:00"
					if (time >= startTime && thisDay) {
						return off ? -1 : 1;
					} else if (time < endTime && previousDay) {
						return off ? -1 : 1;
					}
				}
			}
			if (thisDay && (startTimes == null || startTimes.isEmpty()) && !off) {
				return 1;
			} else if (thisDay && (startTimes == null || startTimes.isEmpty() || !off)) {
				return -1;
			}
			return 0;
		}
	}

	public static class UnparseableRule implements OpeningHoursRule {
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
		public boolean hasOverlapTimes() {
			return false;
		}

		@Override
		public boolean containsDay(Calendar cal) {
			return false;
		}

		@Override
		public boolean containsNextDay(Calendar cal) {
			return false;
		}

		@Override
		public boolean containsMonth(Calendar cal) {
			return false;
		}

		@Override
		public String toRuleString() {
			return ruleString;
		}

		@Override
		public String toLocalRuleString() {
			return toRuleString();
		}

		@Override
		public boolean isOpened24_7() {
			return false;
		}

		@Override
		public String getTime(Calendar cal, boolean checkAnotherDay, int limit, boolean opening) {
			return "";
		}

		@Override
		public String toString() {
			return toRuleString();
		}

		@Override
		public boolean isOpenedForTime(Calendar cal) {
			return false;
		}

		@Override
		public boolean contains(Calendar cal) {
			return false;
		}

		@Override
		public int getSequenceIndex() {
			return 0;
		}
	}
	
	private enum TokenType { 
		TOKEN_UNKNOWN(0),
		TOKEN_COLON(1),
		TOKEN_COMMA(2),
		TOKEN_DASH(3),
		// order is important
		TOKEN_MONTH(4),
		TOKEN_DAY_MONTH(5),
		TOKEN_HOLIDAY(6),
		TOKEN_DAY_WEEK(6),
		TOKEN_HOUR_MINUTES (7),
		TOKEN_OFF_ON(8);
		public final int ord;

		private TokenType(int ord) {
			this.ord = ord;
		}

		public int ord() {
			return ord;
		}

	}
	
	private static class Token {
		public Token(TokenType tokenType, String string) {
			type = tokenType;
			text = string;
			try {
				mainNumber = Integer.parseInt(string);
			} catch(NumberFormatException e){
			}
		}
		int mainNumber = -1;
		TokenType type;
		String text;
		
		@Override
		public String toString() {
			return text + " [" + type + "] ";
		}
	}

	public static void parseRuleV2(String r, int sequenceIndex, List<OpeningHoursRule> rules) {
		String comment = null;
		int q1Index = r.indexOf('"');
		if (q1Index >= 0) {
			int q2Index = r.indexOf('"', q1Index + 1);
			if (q2Index >= 0) {
				comment = r.substring(q1Index + 1, q2Index);
				String a = r.substring(0, q1Index);
				String b = "";
				if (r.length() > q2Index + 1) {
					b = r.substring(q2Index + 1);
				}
				r = a + b;
			}
		}
		r = r.toLowerCase().trim();
		
		final String[] daysStr = new String[]{"mo", "tu", "we", "th", "fr", "sa", "su"};
		final String[] monthsStr = new String[]{"jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};
		final String[] holidayStr = new String[]{"ph", "sh", "easter"};
		String sunrise = "07:00";
		String sunset = "21:00";
		String endOfDay = "24:00";
		r = r.replace('(', ' '); // avoid "(mo-su 17:00-20:00"
		r = r.replace(')', ' ');
		String localRuleString = r.replaceAll("sunset", sunset).replaceAll("sunrise", sunrise)
				.replaceAll("\\+", "-" + endOfDay);
		BasicOpeningHourRule basic = new BasicOpeningHourRule(sequenceIndex);
		basic.setComment(comment);
		boolean[] days = basic.getDays();
		boolean[] months = basic.getMonths();
		boolean[] dayMonths = basic.getDayMonths();
		if ("24/7".equals(localRuleString)) {
			Arrays.fill(days, true);
			Arrays.fill(months, true);
			basic.addTimeRange(0, 24 * 60);
			rules.add(basic);
			return;
		}
		List<Token> tokens = new ArrayList<>();
		int startWord = 0;
		for (int i = 0; i <= localRuleString.length(); i++) {
			char ch = i == localRuleString.length() ? ' ' : localRuleString.charAt(i);
			boolean delimiter = false;
			Token del = null;
			if (Character.isWhitespace(ch)) {
				delimiter = true;
			} else if (ch == ':') {
				del = new Token(TokenType.TOKEN_COLON, ":");
			} else if (ch == '-') {
				del = new Token(TokenType.TOKEN_DASH, "-");
			} else if (ch == ',') {
				del = new Token(TokenType.TOKEN_COMMA, ",");
			}
			if (delimiter || del != null) {
				String wrd = localRuleString.substring(startWord, i).trim();
				if(wrd.length() > 0) {
					tokens.add(new Token(TokenType.TOKEN_UNKNOWN, wrd));
				}
				startWord = i + 1;
				if (del != null) {
					tokens.add(del);
				}
			}
		}
		// recognize day of week
		for (Token t : tokens) {
			if(t.type == TokenType.TOKEN_UNKNOWN) {
				findInArray(t, daysStr, TokenType.TOKEN_DAY_WEEK);
			}
			if(t.type == TokenType.TOKEN_UNKNOWN) {
				findInArray(t, monthsStr, TokenType.TOKEN_MONTH);
			}
			if(t.type == TokenType.TOKEN_UNKNOWN) {
				findInArray(t, holidayStr, TokenType.TOKEN_HOLIDAY);
			}
			if(t.type == TokenType.TOKEN_UNKNOWN && ("off".equals(t.text) || "closed".equals(t.text))) {
				t.type = TokenType.TOKEN_OFF_ON;
				t.mainNumber = 0;
			}
			if(t.type == TokenType.TOKEN_UNKNOWN && ("24/7".equals(t.text) || "open".equals(t.text))) {
				t.type = TokenType.TOKEN_OFF_ON;
				t.mainNumber = 1;
			}
		}
		// recognize hours minutes ( Dec 25: 08:30-20:00)
		for(int i = tokens.size() - 1; i >= 0; i --) {
			if(tokens.get(i).type == TokenType.TOKEN_COLON) {
				if(i > 0 && i < tokens.size() - 1) {
					if(tokens.get(i - 1).type == TokenType.TOKEN_UNKNOWN && tokens.get(i - 1).mainNumber != -1 && 
							tokens.get(i + 1).type == TokenType.TOKEN_UNKNOWN && tokens.get(i + 1).mainNumber != -1 ) {
						tokens.get(i).mainNumber = 60 * tokens.get(i - 1).mainNumber + tokens.get(i + 1).mainNumber;
						tokens.get(i).type = TokenType.TOKEN_HOUR_MINUTES;
						tokens.remove(i + 1);
						tokens.remove(i - 1);
					}
				}
				
			}
		}
		// recognize other numbers
		// if there is no on/off and minutes/hours
		boolean hoursSpecified = false;
		for(int i = 0; i < tokens.size(); i ++) {
			if(tokens.get(i).type == TokenType.TOKEN_HOUR_MINUTES || 
					tokens.get(i).type == TokenType.TOKEN_OFF_ON) {
				hoursSpecified = true;
				break;
			}
		}
		for(int i = 0; i < tokens.size(); i ++) {
			if(tokens.get(i).type == TokenType.TOKEN_UNKNOWN && tokens.get(i).mainNumber >= 0) {
				tokens.get(i).type = hoursSpecified ? TokenType.TOKEN_DAY_MONTH : TokenType.TOKEN_HOUR_MINUTES;
				if(tokens.get(i).type == TokenType.TOKEN_HOUR_MINUTES) {
					tokens.get(i).mainNumber = tokens.get(i).mainNumber * 60;
				} else {
					tokens.get(i).mainNumber = tokens.get(i).mainNumber - 1;
				}
			}
		}
		buildRule(basic, tokens, rules);
	}

	private static void buildRule(BasicOpeningHourRule basic, List<Token> tokens, List<OpeningHoursRule> rules) {
		// order MONTH MONTH_DAY DAY_WEEK HOUR_MINUTE OPEN_OFF
		TokenType currentParse = TokenType.TOKEN_UNKNOWN;
		List<Token[]> listOfPairs = new ArrayList<>();
		Set<TokenType> presentTokens = new HashSet<>();
		Token[] currentPair = new Token[2];
		listOfPairs.add(currentPair);
		int indexP = 0;
		for(int i = 0; i <= tokens.size(); i++) {
			Token t = i == tokens.size() ? null : tokens.get(i);
			if (t == null || t.type.ord() > currentParse.ord()) {
				presentTokens.add(currentParse);
				if (currentParse == TokenType.TOKEN_MONTH || currentParse == TokenType.TOKEN_DAY_MONTH
						|| currentParse == TokenType.TOKEN_DAY_WEEK || currentParse == TokenType.TOKEN_HOLIDAY) {

					boolean[] array = (currentParse == TokenType.TOKEN_MONTH) ? basic.getMonths()
							: (currentParse == TokenType.TOKEN_DAY_MONTH) ? basic.getDayMonths() : basic.getDays();
					for (Token[] pair : listOfPairs) {
						if (pair[0] != null && pair[1] != null) {
							if (pair[0].mainNumber <= pair[1].mainNumber) {
								for (int j = pair[0].mainNumber; j <= pair[1].mainNumber && j < array.length; j++) {
									array[j] = true;
								}
							} else {
								// overflow
								for (int j = pair[0].mainNumber; j < array.length; j++) {
									array[j] = true;
								}
								for (int j = 0; j <= pair[1].mainNumber; j++) {
									array[j] = true;
								}
							}
						} else if (pair[0] != null) {
							if (pair[0].type == TokenType.TOKEN_HOLIDAY) {
								if (pair[0].mainNumber == 0) {
									basic.publicHoliday = true;
								} else if (pair[0].mainNumber == 1) {
									basic.schoolHoliday = true;
								} else if (pair[0].mainNumber == 2) {
									basic.easter = true;
								}
							} else if (pair[0].mainNumber >= 0) {
								array[pair[0].mainNumber] = true;
							}
						}
					}
				} else if (currentParse == TokenType.TOKEN_HOUR_MINUTES) {
					for (Token[] pair : listOfPairs) {
						if (pair[0] != null && pair[1] != null) {
							basic.addTimeRange(pair[0].mainNumber, pair[1].mainNumber);
						}
					}
				} else if (currentParse == TokenType.TOKEN_OFF_ON) {
					Token[] l = listOfPairs.get(0);
					if (l[0] != null && l[0].mainNumber == 0) {
						basic.off = true;
					}
				}
				listOfPairs.clear();
				currentPair = new Token[2];
				indexP = 0;
				listOfPairs.add(currentPair);
				currentPair[indexP++] = t;
				if (t != null) {
					currentParse = t.type;
				}
			} else if (t.type.ord() < currentParse.ord() && indexP == 0 && tokens.size() > i) {
				BasicOpeningHourRule newRule = new BasicOpeningHourRule(basic.getSequenceIndex());
				newRule.setComment(basic.getComment());
				buildRule(newRule, tokens.subList(i, tokens.size()), rules);
				tokens = tokens.subList(0, i + 1);
			} else if (t.type == TokenType.TOKEN_COMMA) {
				if (tokens.size() > i + 1 && tokens.get(i + 1) != null && tokens.get(i + 1).type.ord() < currentParse.ord()) {
					indexP = 0;
				} else {
					currentPair = new Token[2];
					indexP = 0;
					listOfPairs.add(currentPair);
				}
			} else if (t.type == TokenType.TOKEN_DASH) {

			} else if (t.type.ord() == currentParse.ord()) {
				if(indexP < 2) {
					currentPair[indexP++] = t;
				}
			}
		}
		if(!presentTokens.contains(TokenType.TOKEN_MONTH)) {
			Arrays.fill(basic.getMonths(), true);
		}
//		if(!presentTokens.contains(TokenType.TOKEN_DAY_MONTH)) {
//			Arrays.fill(basic.getDayMonths(), true);
//		}
		if(!presentTokens.contains(TokenType.TOKEN_DAY_WEEK) && !presentTokens.contains(TokenType.TOKEN_HOLIDAY) &&
				!presentTokens.contains(TokenType.TOKEN_DAY_MONTH)) {
			Arrays.fill(basic.getDays(), true);
		}
//		if(!presentTokens.contains(TokenType.TOKEN_HOUR_MINUTES)) {
//			basic.addTimeRange(0, 24 * 60);
//		}
//		System.out.println(r + " " +  tokens);
		rules.add(0, basic);
	}

	private static void findInArray(Token t, String[] list, TokenType tokenType) {
		for(int i = 0; i < list.length; i++) {
			if(list[i].equals(t.text)) {
				t.type = tokenType;
				t.mainNumber = i;
				break;
			}
		}
	}

	private static List<List<String>> splitSequences(String format) {
		if (format == null) {
			return null;
		}
		List<List<String>> res = new ArrayList<>();
		String[] sequences = format.split("\\|\\|");
		for (String seq : sequences) {
			seq = seq.trim();
			if (seq.length() == 0) {
				continue;
			}

			List<String> rules = new ArrayList<>();
			boolean comment = false;
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < seq.length(); i++) {
				char c = seq.charAt(i);
				if (c == '"') {
					comment = !comment;
					sb.append(c);
				} else if (c == ';' && !comment) {
					if (sb.length() > 0) {
						String s = sb.toString().trim();
						if (s.length() > 0) {
							rules.add(s);
						}
						sb.setLength(0);
					}
				} else {
					sb.append(c);
				}
			}
			if (sb.length() > 0) {
				rules.add(sb.toString());
				sb.setLength(0);
			}
			res.add(rules);
		}
		return res;
	}

	/**
	 * Parse an opening_hours string from OSM to an OpeningHours object which can be used to check
	 *
	 * @param r the string to parse
	 * @return BasicRule if the String is successfully parsed and UnparseableRule otherwise
	 */
	public static void parseRules(String r, int sequenceIndex, List<OpeningHoursRule> rules) {
		parseRuleV2(r, sequenceIndex, rules);
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
		OpeningHours rs = new OpeningHours();
		rs.setOriginal(format);
		// split the OSM string in multiple rules
		List<List<String>> sequences = splitSequences(format);
		for (int i = 0; i < sequences.size(); i++) {
			List<String> rules = sequences.get(i);
			List<BasicOpeningHourRule> basicRules = new ArrayList<>();
			for (String r : rules) {
				// check if valid
				List<OpeningHoursRule> rList = new ArrayList<>();
				parseRules(r, i, rList);
				for (OpeningHoursRule rule : rList) {
					if (rule instanceof BasicOpeningHourRule) {
						basicRules.add((BasicOpeningHourRule) rule);
					}
				}
			}
			String basicRuleComment = null;
			for (BasicOpeningHourRule bRule : basicRules) {
				if (!Algorithms.isEmpty(bRule.getComment())) {
					basicRuleComment = bRule.getComment();
					break;
				}
			}
			if (!Algorithms.isEmpty(basicRuleComment)) {
				for (BasicOpeningHourRule bRule : basicRules) {
					bRule.setComment(basicRuleComment);
				}
			}
			rs.addRules(basicRules);
		}
		rs.setSequenceCount(sequences.size());
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
		OpeningHoursParser.OpeningHours rs = new OpeningHoursParser.OpeningHours();
		rs.setOriginal(format);
		List<List<String>> sequences = splitSequences(format);
		for (int i = sequences.size() - 1; i >= 0; i--) {
			List<String> rules = sequences.get(i);
			for (String r : rules) {
				r = r.trim();
				if (r.length() == 0) {
					continue;
				}
				// check if valid
				List<OpeningHoursRule> rList = new ArrayList<>();
				parseRules(r, i, rList);
				rs.addRules(rList);
			}
		}
		rs.setSequenceCount(sequences.size());
		return rs;
	}

	public static List<OpeningHours.Info> getInfo(String format) {
		OpeningHours openingHours = OpeningHoursParser.parseOpenedHours(format);
		if (openingHours == null) {
			return null;
		} else {
			return openingHours.getInfo();
		}
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

	private static void formatTime(int minutes, StringBuilder sb) {
		int hour = minutes / 60;
		int time = minutes - hour * 60;
		formatTime(hour, time, sb);
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
		boolean calculated = hours.isOpenedForTimeV2(cal, OpeningHours.ALL_SEQUENCES);
		System.out.printf("  %sok: Expected %s: %b = %b (rule %s)\n",
				((calculated != expected) ? "NOT " : ""), time, expected, calculated, hours.getCurrentRuleTime(cal, OpeningHours.ALL_SEQUENCES));
		if (calculated != expected) {
			throw new IllegalArgumentException("BUG!!!");
		}
	}

	/**
	 * test if the calculated opening hours are what you expect
	 *
	 * @param time        the time to test in the format "dd.MM.yyyy HH:mm"
	 * @param hours       the OpeningHours object
	 * @param expected    the expected string in format:
	 *                         "Open from HH:mm"     - open in 5 hours
	 *                         "Will open at HH:mm"  - open in 2 hours
	 *                         "Open till HH:mm"     - close in 5 hours
	 *                         "Will close at HH:mm" - close in 2 hours
	 *                         "Will open on HH:mm (Mo,Tu,We,Th,Fr,Sa,Su)" - open in >5 hours
	 *                         "Will open tomorrow at HH:mm" - open in >5 hours tomorrow
	 *                         "Open 24/7"           - open 24/7
	 */
	private static void testInfo(String time, OpeningHours hours, String expected) throws ParseException
	{
		testInfo(time, hours, expected, OpeningHours.ALL_SEQUENCES);
	}

	/**
	 * test if the calculated opening hours are what you expect
	 *
	 * @param time        the time to test in the format "dd.MM.yyyy HH:mm"
	 * @param hours       the OpeningHours object
	 * @param expected    the expected string in format:
	 *                         "Open from HH:mm"     - open in 5 hours
	 *                         "Will open at HH:mm"  - open in 2 hours
	 *                         "Open till HH:mm"     - close in 5 hours
	 *                         "Will close at HH:mm" - close in 2 hours
	 *                         "Will open on HH:mm (Mo,Tu,We,Th,Fr,Sa,Su)" - open in >5 hours
	 *                         "Will open tomorrow at HH:mm" - open in >5 hours tomorrow
	 *                         "Open 24/7"           - open 24/7
	 * @param sequenceIndex sequence index of rules separated by ||
	 */
	private static void testInfo(String time, OpeningHours hours, String expected, int sequenceIndex) throws ParseException
	{
		Calendar cal = Calendar.getInstance();
		cal.setTime(new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).parse(time));

		String description;
		boolean result;
		if (sequenceIndex == OpeningHours.ALL_SEQUENCES) {
			OpeningHours.Info info = hours.getCombinedInfo(cal);
			description = info.getInfo();
			result = expected.equalsIgnoreCase(description);
		} else {
			List<OpeningHours.Info> infos = hours.getInfo(cal);
			OpeningHours.Info info = infos.get(sequenceIndex);
			description = info.getInfo();
			result = expected.equalsIgnoreCase(description);
		}

		System.out.printf("  %sok: Expected %s (%s): %s (rule %s)\n",
				(!result ? "NOT " : ""), time, expected, description, hours.getCurrentRuleTime(cal, sequenceIndex));

	    if (!result)
			throw new IllegalArgumentException("BUG!!!");
	}
	
	private static void testParsedAndAssembledCorrectly(String timeString, OpeningHours hours) {
		String assembledString = hours.toString();
		boolean isCorrect = assembledString.equalsIgnoreCase(timeString);
		System.out.printf("  %sok: Expected: \"%s\" got: \"%s\"\n",
				(!isCorrect ? "NOT " : ""), timeString, assembledString);
		if (!isCorrect) {
			throw new IllegalArgumentException("BUG!!!");
		}
	}

	public static void main(String[] args) throws ParseException {
		// 0. not supported MON DAY-MON DAY (only supported Feb 2-14 or Feb-Oct: 09:00-17:30)
		// parseOpenedHours("Feb 16-Oct 15: 09:00-18:30; Oct 16-Nov 15: 09:00-17:30; Nov 16-Feb 15: 09:00-16:30");
		
		// 1. not properly supported
		// hours = parseOpenedHours("Mo-Su (sunrise-00:30)-(sunset+00:30)");
		
		// test basic case
		OpeningHours hours = parseOpenedHours("Mo-Fr 08:30-14:40"); //$NON-NLS-1$
		System.out.println(hours);
		testOpened("09.08.2012 11:00", hours, true);
		testOpened("09.08.2012 16:00", hours, false);
		hours = parseOpenedHours("mo-fr 07:00-19:00; sa 12:00-18:00");

		String string = "Mo-Fr 11:30-15:00, 17:30-23:00; Sa, Su, PH 11:30-23:00";
		hours = parseOpenedHours(string);
		testParsedAndAssembledCorrectly(string, hours);
		System.out.println(hours);
		testOpened("7.09.2015 14:54", hours, true); // monday
		testOpened("7.09.2015 15:05", hours, false);
		testOpened("6.09.2015 16:05", hours, true);


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
		testOpened("07.08.2012 08:15", hours, true); // Tuesday

		// test off value
		hours = parseOpenedHours("Mo-Sa 09:00-18:25; Th off"); //$NON-NLS-1$
		System.out.println(hours);
		testOpened("08.08.2012 12:00", hours, true);
		testOpened("09.08.2012 12:00", hours, false);

		// test 24/7
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
		hours = parseOpenedHours("Tu-Th 07:00-2:00; Fr 17:00-4:00; Sa 18:00-05:00; Su,Mo off");
		System.out.println(hours);
		testOpened("05.05.2013 04:59", hours, true); // sunday 05.05.2013
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
		testOpened("12.05.2015 01:59", hours, false);
		testOpened("12.05.2015 02:59", hours, false);
		testOpened("12.05.2015 03:00", hours, false);
		testOpened("13.05.2015 01:59", hours, true);
		testOpened("13.05.2015 02:59", hours, false);
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
		hours = parseOpenedHours("Apr-Sep 8:00-22:00; Oct-Mar 10:00-18:00");
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
		
		hours = parseOpenedHours("Mo-Su 07:00-23:00; Dec 25 08:00-20:00");
		System.out.println(hours);
		testOpened("25.12.2015 07:00", hours, false);
		testOpened("24.12.2015 07:00", hours, true);
		testOpened("24.12.2015 22:00", hours, true);
		testOpened("25.12.2015 08:00", hours, true);
		testOpened("25.12.2015 22:00", hours, false);
		
		hours = parseOpenedHours("Mo-Su 07:00-23:00; Dec 25 off");
		System.out.println(hours);
		testOpened("25.12.2015 14:00", hours, false);
		testOpened("24.12.2015 08:00", hours, true);
		
		// easter itself as public holiday is not supported
		hours = parseOpenedHours("Mo-Su 07:00-23:00; Easter off; Dec 25 off");
		System.out.println(hours);
		testOpened("25.12.2015 14:00", hours, false);
		testOpened("24.12.2015 08:00", hours, true);

		// test time off (not days
		hours = parseOpenedHours("Mo-Fr 08:30-17:00; 12:00-12:40 off;");
		System.out.println(hours);
		testOpened("07.05.2017 14:00", hours, false); // Sunday
		testOpened("06.05.2017 12:15", hours, false); // Saturday
		testOpened("05.05.2017 14:00", hours, true); // Friday
		testOpened("05.05.2017 12:15", hours, false);
		testOpened("05.05.2017 12:00", hours, false);
		testOpened("05.05.2017 11:45", hours, true);
		
		// Test holidays
		String hoursString = "mo-fr 11:00-21:00; PH off";
		hours = parseOpenedHoursHandleErrors(hoursString);
		testParsedAndAssembledCorrectly(hoursString, hours);

	    // test open from/till
		hours = parseOpenedHours("Mo-Fr 08:30-17:00; 12:00-12:40 off;");
		System.out.println(hours);
	    testInfo("15.01.2018 09:00", hours, "Open till 12:00");
	    testInfo("15.01.2018 11:00", hours, "Will close at 12:00");
	    testInfo("15.01.2018 12:00", hours, "Will open at 12:40");

		hours = parseOpenedHours("Mo-Fr: 9:00-13:00, 14:00-18:00");
		System.out.println(hours);
	    testInfo("15.01.2018 08:00", hours, "Will open at 09:00");
	    testInfo("15.01.2018 09:00", hours, "Open till 13:00");
	    testInfo("15.01.2018 12:00", hours, "Will close at 13:00");
	    testInfo("15.01.2018 13:10", hours, "Will open at 14:00");
	    testInfo("15.01.2018 14:00", hours, "Open till 18:00");
	    testInfo("15.01.2018 16:00", hours, "Will close at 18:00");
	    testInfo("15.01.2018 18:10", hours, "Will open tomorrow at 09:00");
		
	    hours = parseOpenedHours("Mo-Sa 02:00-10:00; Th off");
		System.out.println(hours);
	    testInfo("15.01.2018 23:00", hours, "Will open tomorrow at 02:00");

	    hours = parseOpenedHours("Mo-Sa 23:00-02:00; Th off");
		System.out.println(hours);
	    testInfo("15.01.2018 22:00", hours, "Will open at 23:00");
	    testInfo("15.01.2018 23:00", hours, "Open till 02:00");
	    testInfo("16.01.2018 00:30", hours, "Will close at 02:00");
	    testInfo("16.01.2018 02:00", hours, "Open from 23:00");

	    hours = parseOpenedHours("Mo-Sa 08:30-17:00; Th off");
		System.out.println(hours);
	    testInfo("17.01.2018 20:00", hours, "Will open on 08:30 Fr.");
	    testInfo("18.01.2018 05:00", hours, "Will open tomorrow at 08:30");
	    testInfo("20.01.2018 05:00", hours, "Open from 08:30");
	    testInfo("21.01.2018 05:00", hours, "Will open tomorrow at 08:30");
	    testInfo("22.01.2018 02:00", hours, "Open from 08:30");
	    testInfo("22.01.2018 04:00", hours, "Open from 08:30");
	    testInfo("22.01.2018 07:00", hours, "Will open at 08:30");
	    testInfo("23.01.2018 10:00", hours, "Open till 17:00");
	    testInfo("23.01.2018 16:00", hours, "Will close at 17:00");

	    hours = parseOpenedHours("24/7");
		System.out.println(hours);
	    testInfo("24.01.2018 02:00", hours, "Open 24/7");
	    
	    hours = parseOpenedHours("Mo-Su 07:00-23:00, Fr 08:00-20:00");
		System.out.println(hours);
		testOpened("15.01.2018 06:45", hours, false);
		testOpened("15.01.2018 07:45", hours, true);
		testOpened("15.01.2018 23:45", hours, false);
		testOpened("19.01.2018 07:45", hours, false);
		testOpened("19.01.2018 08:45", hours, true);
		testOpened("19.01.2018 20:45", hours, false);

		// test fallback case
		hours = parseOpenedHours("07:00-01:00 open \"Restaurant\" || Mo 00:00-04:00,07:00-04:00; Tu-Th 07:00-04:00; Fr 07:00-24:00; Sa,Su 00:00-24:00 open \"McDrive\"");
		System.out.println(hours);
		testOpened("22.01.2018 00:30", hours, true);
		testOpened("22.01.2018 08:00", hours, true);
		testOpened("22.01.2018 03:30", hours, true);
		testOpened("22.01.2018 05:00", hours, false);
		testOpened("23.01.2018 05:00", hours, false);
		testOpened("27.01.2018 05:00", hours, true);
		testOpened("28.01.2018 05:00", hours, true);

		testInfo("22.01.2018 05:00", hours, "Will open at 07:00 (Restaurant)", 0);
		testInfo("26.01.2018 00:00", hours, "Will close at 01:00 (Restaurant)", 0);
		testInfo("22.01.2018 05:00", hours, "Will open at 07:00 (McDrive)", 1);
		testInfo("22.01.2018 00:00", hours, "Open till 04:00 (McDrive)", 1);
		testInfo("22.01.2018 02:00", hours, "Will close at 04:00 (McDrive)", 1);
		testInfo("27.01.2018 02:00", hours, "Open till 24:00 (McDrive)", 1);
		
		hours = parseOpenedHours("07:00-03:00 open \"Restaurant\" || 24/7 open \"McDrive\"");
		System.out.println(hours);
		testOpened("22.01.2018 02:00", hours, true);
		testOpened("22.01.2018 17:00", hours, true);
		testInfo("22.01.2018 05:00", hours, "Will open at 07:00 (Restaurant)", 0);
		testInfo("22.01.2018 04:00", hours, "Open 24/7 (McDrive)", 1);

	}
}
