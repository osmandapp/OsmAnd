package net.osmand.util;
/* Can be commented out in order to run the main function separately */

import java.io.Serializable;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

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
	private static String[] localDaysStr;
	private static final String[] monthsStr;
	private static String[] localMothsStr;
	private static final Map<String, String> additionalStrings = new HashMap<>();

	private static final int LOW_TIME_LIMIT = 120;
	private static final int WITHOUT_TIME_LIMIT = -1;
	private static final int CURRENT_DAY_TIME_LIMIT = -2;

	private static boolean twelveHourFormatting;
	private static DateFormat twelveHourFormatter;
	private static DateFormat twelveHourFormatterAmPm;

	static {
		DateFormatSymbols dateFormatSymbols = DateFormatSymbols.getInstance(Locale.US);
		monthsStr = dateFormatSymbols.getShortMonths();
		daysStr = getLettersStringArray(dateFormatSymbols.getShortWeekdays(), 2);

		initLocalStrings();

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

	private static void initLocalStrings() {
		initLocalStrings(null);
	}

	public static void initLocalStrings(Locale locale) {
		DateFormatSymbols dateFormatSymbols = locale == null
				? DateFormatSymbols.getInstance()
				: DateFormatSymbols.getInstance(locale);
		localMothsStr = dateFormatSymbols.getShortMonths();
		localDaysStr = getLettersStringArray(dateFormatSymbols.getShortWeekdays(), 3);
	}

	public static void setTwelveHourFormattingEnabled(boolean enabled, Locale locale) {
		twelveHourFormatting = enabled;
		if (enabled) {
			initTwelveHourFormatters(locale);
		}
	}

	private static void initTwelveHourFormatters(Locale locale) {
		twelveHourFormatter = new SimpleDateFormat("h:mm", locale);
		twelveHourFormatterAmPm = DateFormat.getTimeInstance(DateFormat.SHORT, locale);
		TimeZone timeZone = TimeZone.getTimeZone("UTC");
		twelveHourFormatter.setTimeZone(timeZone);
		twelveHourFormatterAmPm.setTimeZone(timeZone);
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

	private static String[] getLettersStringArray(String[] strings, int letters) {
		String[] newStrings = new String[strings.length];
		for (int i = 0; i < strings.length; i++) {
			if (strings[i] != null) {
				if (strings[i].length() > letters) {
					newStrings[i] = Algorithms.capitalizeFirstLetter(strings[i].substring(0, letters));
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
			private boolean fallback;
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

			public boolean isFallback() {
				return fallback;
			}

			public String getInfo() {
				if (isOpened24_7()) {
					if (!isFallback()) {
						if (!Algorithms.isEmpty(ruleString)) {
							return additionalStrings.get("is_open") + " " + ruleString;
						} else {
							return additionalStrings.get("is_open_24_7");
						}
					} else {
						return !Algorithms.isEmpty(ruleString) ? ruleString : "";
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
				} else {
					return !Algorithms.isEmpty(ruleString) ? ruleString : "";
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
			info.fallback = isFallBackRule(sequenceIndex);
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
			// in (2) we need to check first rule even though it is against specification but many OSM still treat it
			ArrayList<OpeningHoursRule> rules = getRules(sequenceIndex);
			boolean overlap = hasRulesOverlapDayBackwardCompatible(rules);
			// start from the most specific rule
			for (int i = rules.size() - 1; i >= 0 ; i--) {
				OpeningHoursRule rule = rules.get(i);
				if (rule.contains(cal)) {
					boolean checkNextNotNeeded = overlap || !isCheckNextNeeded(cal, rules, i, rule);
					boolean open = rule.isOpenedForTime(cal);
					if (open || !checkNextNotNeeded) {
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
			Calendar openingTimeCal = null;
			OpeningHoursRule openingRule = null;
			for (OpeningHoursRule r : rules) {
				if (r.contains(cal)) {
					String time = r.getTime(cal, false, WITHOUT_TIME_LIMIT, true);
					if (Algorithms.isEmpty(time) || openingTimeCal == null || cal.before(openingTimeCal)
							|| r.hasOverlapTimes(cal, openingRule, false)) {
						openingTime = time;
						openingRule = r;
					}
					openingTimeCal = (Calendar) cal.clone();
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
				OpeningHoursRule openingRule = null;
				Calendar openingTimeCal = null;
				for (OpeningHoursRule r : rules) {
					if (r.contains(cal)) {
						String time = r.getTime(cal, false, WITHOUT_TIME_LIMIT, true);
						if (Algorithms.isEmpty(time) || openingTimeCal == null || cal.before(openingTimeCal)
								|| r.hasOverlapTimes(cal, openingRule, false)) {
							openingTime = time;
							openingRule = r;
						}
						openingTimeCal = (Calendar) cal.clone();
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
			OpeningHoursRule prevRule = null;
			for (OpeningHoursRule r : rules) {
				if (r.containsDay(cal) && r.containsMonth(cal)) {
					if (atTime.length() > 0 && prevRule != null && !r.hasOverlapTimes(cal, prevRule, true)) {
						return atTime;
					} else {
						atTime = r.getTime(cal, false, limit, opening);
					}
				}
				prevRule = r;
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

		public boolean isFallBackRule(int sequenceIndex) {
			if (sequenceIndex != ALL_SEQUENCES) {
				ArrayList<OpeningHoursRule> rules = getRules(sequenceIndex);
				return !rules.isEmpty() && rules.get(0).isFallbackRule();
			}
			return false;
		}

		public String getCurrentRuleTime(Calendar cal, int sequenceIndex) {
			// make exception for overlapping times i.e.
			// (1) Mo 14:00-16:00; Tu off
			// (2) Mo 14:00-02:00; Tu off
			// in (2) we need to check first rule even though it is against specification
			ArrayList<OpeningHoursRule> rules = getRules(sequenceIndex);
			String ruleClosed = null;
			boolean overlap = hasRulesOverlapDayBackwardCompatible(rules);
			// start from the most specific rule
			for (int i = rules.size() - 1; i >= 0; i--) {
				OpeningHoursRule rule = rules.get(i);
				if (rule.contains(cal)) {
					boolean checkNextNotNeeded = overlap || !isCheckNextNeeded(cal, rules, i, rule);
					boolean open = rule.isOpenedForTime(cal);
					if (open || !checkNextNotNeeded) {
						return rule.toLocalRuleString();
					} else {
						ruleClosed = rule.toLocalRuleString();
					}
				}
			}
			return ruleClosed;
		}

		private boolean isCheckNextNeeded(Calendar cal, ArrayList<OpeningHoursRule> rules, int i, OpeningHoursRule rule) {
			boolean checkNext = true;
			if (i > 0) {
				for (int j = i; j > 0; j--) {
					checkNext = rule.hasOverlapTimes(cal, rules.get(j - 1), false);
					if (checkNext) {
						break;
					}
				}
			}
			return checkNext;
		}

		private boolean hasRulesOverlapDayBackwardCompatible(ArrayList<OpeningHoursRule> rules) {
			boolean overlap = false;
			for (int i = rules.size() - 1; i >= 0; i--) {
				OpeningHoursRule r = rules.get(i);
				if (r.hasOverlapTimesOverDay()) {
					overlap = true;
					break;
				}
			}
			return overlap;
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
		public boolean hasOverlapTimesOverDay();

		/**
		 * Check if r rule times overlap with this rule times at "cal" date.
		 *
		 * @param cal the date to check
		 * @param r the rule to check
		 * @param strictOverlap detect overlap even if r rule time end at rule time start (2:00-5:00, 5:00-7:00)
		 * @return true if the this rule times overlap with r times
		 */
		public boolean hasOverlapTimes(Calendar cal, OpeningHoursRule r, boolean strictOverlap);

		/**
		 * @param cal
		 * @return true if rule applies for current time
		 */
		public boolean contains(Calendar cal);

		public int getSequenceIndex();

		boolean isFallbackRule();

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
		private boolean hasDays = false;

		/**
		 * represents the list on which month it is open.
		 * Day number 0 is JANUARY.
		 */
		private boolean[] months = new boolean[12];

		private int[] firstYearMonths = null; // stores YEAR == this.year for valid months [0, 0, ... 0, YEAR, YEAR, ... YEAR, 0...]
		private boolean[][] firstYearDayMonth;
		private int[] lastYearMonths = null;
		private boolean[][] lastYearDayMonth;
		private int year = 0;

		private boolean fallback;

		/**
		 * represents the list on which day it is open.
		 */
		private boolean[][] dayMonths = null;

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

		@Override
		public boolean isFallbackRule() {
			return fallback;
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
		public boolean[] getDayMonths(int month) {
			if (dayMonths == null) {
				dayMonths = new boolean[12][31];
			}
			return dayMonths[month];
		}
		public boolean hasDayMonths() {
			return dayMonths != null;
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

		public void setDays(boolean[] days) {
			if (this.days.length == days.length) {
				this.days = days;
			}
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
			return days[d];
		}

		@Override
		public boolean containsNextDay(Calendar cal) {
			int i = cal.get(Calendar.DAY_OF_WEEK);
			int p = (i + 6) % 7;
			return days[p];
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
			return days[p];
		}

		/**
		 * Check if the month of "cal" is part of this rule
		 *
		 * @param cal the time to check
		 * @return true if the month is part of the rule
		 */
		public boolean containsMonth(Calendar cal) {
			int month = cal.get(Calendar.MONTH);
			int year = cal.get(Calendar.YEAR);
			if (!hasYears()) {
				return (this.year == 0 || this.year == year) && months[month];
			}
			if (this.year > year) {
				return false;
			} else if(this.year < year) {
				if (lastYearMonths == null) {
					return false;
				}
				int lastYear = lastYearMonths[month];
				return lastYear > 0 && year <= lastYear;
			} else {
				return firstYearMonths[month] > 0;
			}
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
			return toRuleString(false);
		}

		private String toRuleString(boolean useLocalization) {
			String[] dayNames = useLocalization ? localDaysStr : daysStr;
			String[] monthNames = useLocalization ? localMothsStr : monthsStr;
			String offStr = useLocalization ? additionalStrings.get("off") : "off";

			StringBuilder b = new StringBuilder(25);
			boolean allMonths = true;
			for (int i = 0; i < months.length; i++) {
				if (!months[i]) {
					allMonths = false;
					break;
				}
			}
			boolean allDays = !hasDayMonths();
			if (!allDays) {
				boolean dash = false;
				boolean first = true;
				int monthAdded = -1;
				int dayAdded = -1;
				int excludedMonthEnd = -1;
				int excludedDayEnd = -1;
				int excludedMonthStart = -1;
				int excludedDayStart = -1;
				if (dayMonths[0][0] && dayMonths[11][30]) {
					int prevMonth = 0;
					int prevDay = 0;
					for (int month = 0; month < dayMonths.length; month++) {
						for (int day = 0; day < dayMonths[month].length; day++) {
							if (day == 1) {
								prevMonth = month;
							}
							if (!dayMonths[month][day]) {
								excludedMonthEnd = prevMonth;
								excludedDayEnd = prevDay;
								break;
							}
							prevDay = day;
						}
						if (excludedDayEnd != -1) {
							break;
						}
					}
					prevMonth = dayMonths.length - 1;
					prevDay = dayMonths[prevMonth].length - 1;
					for (int month = dayMonths.length - 1; month >= 0; month--) {
						for (int day = dayMonths[month].length - 1; day >= 0; day--) {
							if (day == dayMonths[month].length - 2) {
								prevMonth = month;
							}
							if (!dayMonths[month][day]) {
								excludedMonthStart = prevMonth;
								excludedDayStart = prevDay;
								break;
							}
							prevDay = day;
						}
						if (excludedDayStart != -1) {
							break;
						}
					}
				}
				boolean yearAdded = false;
				for (int month = 0; month < dayMonths.length; month++) {
					for (int day = 0; day < dayMonths[month].length; day++) {
						if (excludedDayStart != -1 && excludedDayEnd != -1) {
							if (month < excludedMonthEnd || (month == excludedMonthEnd && day <= excludedDayEnd)) {
								continue;
							} else if (month > excludedMonthStart || (month == excludedMonthStart && day >= excludedDayStart)) {
								continue;
							}
						}
						if (dayMonths[month][day]) {
							if (day == 0 && dash && dayMonths[month][1]) {
								continue;
							}
							if (day > 0 && dayMonths[month][day - 1]
									&& ((day < dayMonths[month].length - 1 && dayMonths[month][day + 1]) || (day == dayMonths[month].length - 1 && month < dayMonths.length - 1 && dayMonths[month + 1][0]))) {
								if (!dash) {
									dash = true;
									if (!first) {
										b.append("-");
									}
								}
								continue;
							}
							if (first) {
								first = false;
							} else if (!dash) {
								b.append(", ");
								monthAdded = -1;
							}
							yearAdded = appendYearString(b, dash ? lastYearMonths : firstYearMonths, month);
							if (monthAdded != month || yearAdded) {
								b.append(monthNames[month]).append(" ");
								monthAdded = month;
							}
							dayAdded = day + 1;
							b.append(dayAdded);
							dash = false;
						}
					}
				}
				if (excludedDayStart != -1 && excludedDayEnd != -1) {
					if (first) {
						first = false;
					} else if (!dash) {
						b.append(", ");
					}
					appendYearString(b, firstYearMonths, excludedMonthStart);
					b.append(monthNames[excludedMonthStart]).append(" ").append(excludedDayStart + 1)
							.append("-");
					appendYearString(b, lastYearMonths, excludedMonthEnd);
					b.append(monthNames[excludedMonthEnd]).append(" ").append(excludedDayEnd + 1);
				} else if (yearAdded && !dash && monthAdded != -1 && lastYearMonths != null) {
					b.append("-");
					appendYearString(b, lastYearMonths, monthAdded);
					b.append(monthNames[monthAdded]);
					if (dayAdded != -1) {
						b.append(" ").append(dayAdded);
					}
				}
				if (!first) {
					b.append(" ");
				}
			} else if (!allMonths) {
				addArray(months, monthNames, b);
			}

			// Day
			appendDaysString(b, dayNames);
			// Time
			if (startTimes == null || startTimes.size() == 0) {
				if (isOpened24_7()) {
					b.setLength(0);
					if (!isFallbackRule()) {
						b.append("24/7 ");
					}
				}
				if (off) {
					b.append(offStr);
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
						formatTimeRange(startTime, endTime, b);
					}
					if (off) {
						b.append(" ").append(offStr);
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

		private boolean appendYearString(StringBuilder b, int[] yearMonths, int month) {
			if (yearMonths != null && yearMonths[month] > 0) {
				b.append(yearMonths[month]).append(" ");
				return true;
			} else if (year > 0) {
				b.append(year).append(" ");
				return true;
			}
			return false;
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
			return toRuleString(true);
		}

		@Override
		public boolean isOpened24_7() {
			boolean opened24_7 = isOpenedEveryDay();

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

		public boolean isOpenedEveryDay() {
			boolean openedEveryDay = true;
			for (int i = 0; i < 7; i++) {
				if (!days[i]) {
					openedEveryDay = false;
					break;
				}
			}
			return openedEveryDay;
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
		public boolean hasOverlapTimesOverDay() {
			for (int i = 0; i < this.startTimes.size(); i++) {
				int startTime = this.startTimes.get(i);
				int endTime = this.endTimes.get(i);
				if (startTime >= endTime && endTime > 0) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean hasOverlapTimes(Calendar cal, OpeningHoursRule r, boolean strictOverlap) {
			if (off) {
				return true;
			}
			if (r != null && r.contains(cal) && r instanceof BasicOpeningHourRule) {
				BasicOpeningHourRule rule = (BasicOpeningHourRule) r;
				if (startTimes.size() > 0 && rule.startTimes.size() > 0) {
					for (int i = 0; i < this.startTimes.size(); i++) {
						int startTime = this.startTimes.get(i);
						int endTime = this.endTimes.get(i);
						if (endTime == -1) {
							endTime = 24 * 60;
						} else if (startTime >= endTime) {
							endTime = 24 * 60 + endTime;
						}
						for (int k = 0; k < rule.startTimes.size(); k++) {
							int rStartTime = rule.startTimes.get(k);
							int rEndTime = rule.endTimes.get(k);
							if (rEndTime == -1) {
								rEndTime = 24 * 60;
							} else if (rStartTime >= rEndTime) {
								rEndTime = 24 * 60 + rEndTime;
							}
							if ((rStartTime >= startTime && (strictOverlap ? rStartTime <= endTime : rStartTime < endTime))
									|| (startTime >= rStartTime && (strictOverlap ? startTime <= rEndTime : startTime < rEndTime))) {
								return true;
							}
						}
					}
				}
			}
			return false;
		}

		private int calculate(Calendar cal) {
			int year = cal.get(Calendar.YEAR);
			int month = cal.get(Calendar.MONTH);
			if (!containsMonth(cal)) {
				return 0;
			}
			int dmonth = cal.get(Calendar.DAY_OF_MONTH) - 1;
			int i = cal.get(Calendar.DAY_OF_WEEK);
			int day = (i + 5) % 7;
			int previous = (day + 6) % 7;
			boolean thisDay = true; //hasDays || hasDayMonths() || hasFullYears(); // CHECK?
			if (hasYears()) {
				thisDay = isOpened(year, month, dmonth);
			} else {
				if (thisDay && hasDayMonths()) {
					thisDay = dayMonths[month][dmonth];
				}
			}
			if (thisDay && hasDays) {
				thisDay = days[day];
			}
			// potential error for Dec 31 12:00-01:00
			boolean previousDay = true; // hasDays || hasDayMonths() || hasFullYears(); // CHECK?
			if (hasYears()) {
				if (dmonth > 0) {
					previousDay = isOpened(year, month, dmonth - 1);
				}
			} else {
				if (previousDay && hasDayMonths() && dmonth > 0) {
					previousDay = dayMonths[month][dmonth - 1];
				}
			}
			if (previousDay && hasDays) {
				previousDay = days[previous];
			}
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

		private boolean isOpened(int year, int month, int dmonth) {
			boolean opened = hasDayMonths() && dayMonths[month][dmonth];
			if (hasYears()) {
				if (year < this.year) {
					opened = false;
				} else if (year == this.year) {
					if (firstYearDayMonth != null) {
						opened = firstYearDayMonth[month][dmonth];
					}
				} else {
					int lastYear = lastYearMonths[month];
					if (year < lastYear) {
						opened = true;
					} else if (year == lastYear) {
						opened = lastYearDayMonth[month][dmonth];
					} else {
						opened = false;
					}
				}
			}
			return opened;
		}

		private boolean hasYears() {
			return firstYearMonths != null ;
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
		public boolean hasOverlapTimesOverDay() {
			return false;
		}

		@Override
		public boolean hasOverlapTimes(Calendar cal, OpeningHoursRule r, boolean strictOverlap) {
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

		@Override
		public boolean isFallbackRule() {
			return false;
		}
	}
	
	private enum TokenType { 
		TOKEN_UNKNOWN(0),
		TOKEN_COLON(1),
		TOKEN_COMMA(2),
		TOKEN_DASH(3),
		// order is important
		TOKEN_YEAR(4),
		TOKEN_MONTH(5),
		TOKEN_DAY_MONTH(6),
		TOKEN_HOLIDAY(7),
		TOKEN_DAY_WEEK(7),
		TOKEN_HOUR_MINUTES (8),
		TOKEN_OFF_ON(9),
		TOKEN_COMMENT(10);

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
		public Token(TokenType tokenType, int tokenMainNumber) {
			type = tokenType;
			mainNumber = tokenMainNumber;
			text = Integer.toString(mainNumber);
		}
		int mainNumber = -1;
		TokenType type;
		String text;
		Token parent;

		@Override
		public String toString() {
			if (parent != null) {
				return parent.text + " [" + parent.type + "] (" + text + " [" + type + "]) ";
			} else {
				return text + " [" + type + "] ";
			}
		}
	}

	public static void parseRuleV2(String r, int sequenceIndex, List<OpeningHoursRule> rules) {
		r = r.trim();

		final String[] daysStr = new String[]{"mo", "tu", "we", "th", "fr", "sa", "su"};
		final String[] monthsStr = new String[]{"jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};
		final String[] holidayStr = new String[]{"ph", "sh", "easter"};
		String sunrise = "07:00";
		String sunset = "21:00";
		String endOfDay = "24:00";
		r = r.replace('(', ' '); // avoid "(mo-su 17:00-20:00"
		r = r.replace(')', ' ');
		BasicOpeningHourRule basic = new BasicOpeningHourRule(sequenceIndex);
		if (r.startsWith("|| ")) {
			r = r.replace("|| ", "");
			basic.fallback = true;
		}
		String localRuleString = r.replaceAll("(?i)sunset", sunset).replaceAll("(?i)sunrise", sunrise)
				.replaceAll("\\+", "-" + endOfDay);
		boolean[] days = basic.getDays();
		boolean[] months = basic.getMonths();
		//boolean[][] dayMonths = basic.getDayMonths();
		if ("24/7".equals(localRuleString)) {
			Arrays.fill(days, true);
			basic.hasDays = true;
			Arrays.fill(months, true);
			basic.addTimeRange(0, 24 * 60);
			rules.add(basic);
			return;
		}
		List<Token> tokens = new ArrayList<>();
		int startWord = 0;
		StringBuilder commentStr = new StringBuilder();
		boolean comment = false;
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
			} else if (ch == '"') {
				if (comment) {
					if (commentStr.length() > 0) {
						tokens.add(new Token(TokenType.TOKEN_COMMENT, commentStr.toString()));
					}
					startWord = i + 1;
					commentStr.setLength(0);
					comment = false;
				} else {
					comment = true;
					continue;
				}
			}
			if (comment) {
				commentStr.append(ch);
			} else if (delimiter || del != null) {
				String wrd = localRuleString.substring(startWord, i).trim();
				if (wrd.length() > 0) {
					tokens.add(new Token(TokenType.TOKEN_UNKNOWN, wrd.toLowerCase()));
				}
				startWord = i + 1;
				if (del != null) {
					tokens.add(del);
				}
			}
		}
		// recognize day of week
		for (Token t : tokens) {
			if (t.type == TokenType.TOKEN_UNKNOWN) {
				findInArray(t, daysStr, TokenType.TOKEN_DAY_WEEK);
			}
			if (t.type == TokenType.TOKEN_UNKNOWN) {
				findInArray(t, monthsStr, TokenType.TOKEN_MONTH);
			}
			if (t.type == TokenType.TOKEN_UNKNOWN) {
				findInArray(t, holidayStr, TokenType.TOKEN_HOLIDAY);
			}
			if (t.type == TokenType.TOKEN_UNKNOWN && ("off".equals(t.text) || "closed".equals(t.text))) {
				t.type = TokenType.TOKEN_OFF_ON;
				t.mainNumber = 0;
			}
			if (t.type == TokenType.TOKEN_UNKNOWN && ("24/7".equals(t.text) || "open".equals(t.text))) {
				t.type = TokenType.TOKEN_OFF_ON;
				t.mainNumber = 1;
			}
		}
		// recognize hours minutes ( Dec 25: 08:30-20:00)
		for (int i = tokens.size() - 1; i > 0; i--) {
			if (tokens.get(i).type == TokenType.TOKEN_COLON) {
				if (i < tokens.size() - 1) {
					if (tokens.get(i - 1).type == TokenType.TOKEN_UNKNOWN && tokens.get(i - 1).mainNumber != -1 &&
							tokens.get(i + 1).type == TokenType.TOKEN_UNKNOWN && tokens.get(i + 1).mainNumber != -1) {
						tokens.get(i).mainNumber = 60 * tokens.get(i - 1).mainNumber + tokens.get(i + 1).mainNumber;
						tokens.get(i).type = TokenType.TOKEN_HOUR_MINUTES;
						tokens.remove(i + 1);
						tokens.remove(i - 1);
					}
				}
			} else if (tokens.get(i).type == TokenType.TOKEN_OFF_ON
					&& tokens.get(i - 1).type == TokenType.TOKEN_OFF_ON) {
				tokens.remove(i - 1);
			}
		}
		// recognize other numbers
		boolean monthSpecified = false;
		for (Token t : tokens) {
			if (t.type == TokenType.TOKEN_MONTH) {
				monthSpecified = true;
				break;
			}
		}
		for(int i = 0; i < tokens.size(); i ++) {
			Token t = tokens.get(i);
			if (t.type == TokenType.TOKEN_UNKNOWN && t.mainNumber >= 0) {
				if (monthSpecified && t.mainNumber <= 31) {
					t.type = TokenType.TOKEN_DAY_MONTH;
					t.mainNumber = t.mainNumber - 1;
				} else if (t.mainNumber > 1000) {
					t.type = TokenType.TOKEN_YEAR;
				}
			}
		}
		buildRule(basic, tokens, rules);
	}

	private static void buildRule(BasicOpeningHourRule basic, List<Token> tokens, List<OpeningHoursRule> rules) {
		// order MONTH MONTH_DAY DAY_WEEK HOUR_MINUTE OPEN_OFF
		TokenType currentParse = TokenType.TOKEN_UNKNOWN;
		TokenType currentParseParent = TokenType.TOKEN_UNKNOWN;
		List<Token[]> listOfPairs = new ArrayList<>();
		Set<TokenType> presentTokens = EnumSet.noneOf(TokenType.class);
		Token[] currentPair = new Token[2];
		listOfPairs.add(currentPair);
		Token prevToken = null;
		Token prevYearToken = null;
		int indexP = 0;
		for(int i = 0; i <= tokens.size(); i++) {
			Token t = i == tokens.size() ? null : tokens.get(i);
			if (i == 0 && t != null && t.type == TokenType.TOKEN_UNKNOWN) {
				// skip rule if the first token unknown
				return;
			}
			if (t == null || t.type.ord() > currentParse.ord()) {
				presentTokens.add(currentParse);
				if (currentParse == TokenType.TOKEN_MONTH || currentParse == TokenType.TOKEN_DAY_MONTH
						|| currentParse == TokenType.TOKEN_DAY_WEEK || currentParse == TokenType.TOKEN_HOLIDAY) {

					boolean tokenDayMonth = currentParse == TokenType.TOKEN_DAY_MONTH;
					boolean[] array = (currentParse == TokenType.TOKEN_MONTH) ? basic.getMonths()
							: tokenDayMonth ? null : basic.getDays();
					for (Token[] pair : listOfPairs) {
						if (pair[0] != null && pair[1] != null) {
							Token firstMonthToken = pair[0].parent == null && pair[0].type == TokenType.TOKEN_MONTH ? pair[0] : pair[0].parent;
							Token lastMonthToken = pair[1].parent == null && pair[1].type == TokenType.TOKEN_MONTH ? pair[1] : pair[1].parent;
							if (tokenDayMonth && firstMonthToken != null) {
								if (lastMonthToken != null && lastMonthToken.mainNumber != firstMonthToken.mainNumber) {
									Token[] p = new Token[]{firstMonthToken, lastMonthToken};
									fillRuleArray(basic.getMonths(), p);

									Token t1 = new Token(TokenType.TOKEN_DAY_MONTH, pair[0].mainNumber);
									Token t2 = new Token(TokenType.TOKEN_DAY_MONTH, 30);
									p = new Token[]{t1, t2};
									array = basic.getDayMonths(firstMonthToken.mainNumber);
									fillRuleArray(array, p);

									t1 = new Token(TokenType.TOKEN_DAY_MONTH, 0);
									t2 = new Token(TokenType.TOKEN_DAY_MONTH, pair[1].mainNumber);
									p = new Token[]{t1, t2};
									array = basic.getDayMonths(lastMonthToken.mainNumber);
									fillRuleArray(array, p);

									if (firstMonthToken.mainNumber <= lastMonthToken.mainNumber) {
										for (int month = firstMonthToken.mainNumber + 1; month < lastMonthToken.mainNumber; month++) {
											Arrays.fill(basic.getDayMonths(month), true);
										}
									} else {
										for (int month = firstMonthToken.mainNumber + 1; month < 12; month++) {
											Arrays.fill(basic.getDayMonths(month), true);
										}
										for (int month = 0; month < lastMonthToken.mainNumber; month++) {
											Arrays.fill(basic.getDayMonths(month), true);
										}
									}
								} else {
									array = basic.getDayMonths(firstMonthToken.mainNumber);
									fillRuleArray(array, pair);
								}
							} else if (array != null) {
								fillRuleArray(array, pair);
							}
							int ruleYear = basic.year;
							if ((ruleYear > 0 || prevYearToken != null) && firstMonthToken != null && lastMonthToken != null) {
								int endYear = prevYearToken != null ? prevYearToken.mainNumber : ruleYear;
								int startYear = ruleYear > 0 ? ruleYear : endYear;
								if (basic.firstYearMonths == null) {
									basic.firstYearMonths = new int[12];
								}
								Arrays.fill(basic.firstYearMonths, firstMonthToken.mainNumber, 12, startYear);
								if (endYear > startYear) {
									if (basic.lastYearMonths == null) {
										basic.lastYearMonths = new int[12];
									}
									Arrays.fill(basic.lastYearMonths, 0, lastMonthToken.mainNumber + 1, endYear);
									if (endYear - startYear > 1) {
										int startInd = lastMonthToken.mainNumber + 1;
										Arrays.fill(basic.lastYearMonths, startInd, 12, endYear - 1);
									} else {
										int startInd = Math.max(lastMonthToken.mainNumber + 1, firstMonthToken.mainNumber);
										Arrays.fill(basic.lastYearMonths, startInd, 12, startYear);
									}
									fillFirstLastYearsDayOfMonth(basic, pair);
									if (firstMonthToken.mainNumber >= lastMonthToken.mainNumber) {
										Arrays.fill(basic.months, true);
									}
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
								Token firstMonthToken = pair[0].parent;
								if (tokenDayMonth && firstMonthToken != null) {
									array = basic.getDayMonths(firstMonthToken.mainNumber);
								}
								if (array != null) {
									array[pair[0].mainNumber] = true;
									if (prevYearToken != null) {
										basic.year = prevYearToken.mainNumber;
									}
								}
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
				} else if (currentParse == TokenType.TOKEN_COMMENT) {
					Token[] l = listOfPairs.get(0);
					if (l[0] != null && !Algorithms.isEmpty(l[0].text)) {
						basic.comment = l[0].text;
					}
				} else if (currentParse == TokenType.TOKEN_YEAR) {
					Token[] l = listOfPairs.get(0);
					if (l[0] != null && l[0].mainNumber > 1000) {
						prevYearToken = l[0];
					}
				}
				listOfPairs.clear();
				currentPair = new Token[2];
				indexP = 0;
				listOfPairs.add(currentPair);
				currentPair[indexP++] = t;
				if (t != null) {
					currentParse = t.type;
					currentParseParent = currentParse;
					if (t.type == TokenType.TOKEN_DAY_MONTH && prevToken != null && prevToken.type == TokenType.TOKEN_MONTH) {
						t.parent = prevToken;
						currentParseParent = prevToken.type;
					} else if (t.type == TokenType.TOKEN_MONTH && prevToken != null && prevToken.type == TokenType.TOKEN_YEAR) {
						basic.year = prevToken.mainNumber; // add first year for ("2019 Oct - 2024 dec")
					}
				}
			} else if (t.type.ord() < currentParseParent.ord() && indexP == 0 && tokens.size() > i) {
				BasicOpeningHourRule newRule = new BasicOpeningHourRule(basic.getSequenceIndex());
				newRule.setComment(basic.getComment());
				buildRule(newRule, tokens.subList(i, tokens.size()), rules);
				tokens = tokens.subList(0, i + 1);
			} else if (t.type == TokenType.TOKEN_COMMA) {
				if (tokens.size() > i + 1 && tokens.get(i + 1) != null && tokens.get(i + 1).type.ord() < currentParseParent.ord()) {
					indexP = 0;
				} else {
					currentPair = new Token[2];
					indexP = 0;
					listOfPairs.add(currentPair);
				}
			} else if (t.type == TokenType.TOKEN_DASH) {

			} else if (t.type == TokenType.TOKEN_YEAR) {
				prevYearToken = t;
			} else if (t.type.ord() == currentParse.ord()) {
				if (indexP < 2) {
					currentPair[indexP++] = t;
					if (t.type == TokenType.TOKEN_DAY_MONTH && prevToken != null && prevToken.type == TokenType.TOKEN_MONTH) {
						t.parent = prevToken;
					}
				}
			}
			prevToken = t;
		}
		if (!presentTokens.contains(TokenType.TOKEN_MONTH)) {
			Arrays.fill(basic.getMonths(), true);
		}
		if (!presentTokens.contains(TokenType.TOKEN_DAY_WEEK) && !presentTokens.contains(TokenType.TOKEN_HOLIDAY) &&
				!presentTokens.contains(TokenType.TOKEN_DAY_MONTH)) {
			Arrays.fill(basic.getDays(), true);
			basic.hasDays = true;
		} else if (presentTokens.contains(TokenType.TOKEN_DAY_WEEK) || presentTokens.contains(TokenType.TOKEN_HOLIDAY)) {
			basic.hasDays = true;
		}
		rules.add(0, basic);
	}

	private static void fillFirstLastYearsDayOfMonth(BasicOpeningHourRule basic, Token[] pair) {
		int startMonth = pair[0].parent == null ? pair[0].mainNumber : pair[0].parent.mainNumber;
		int startDayOfMonth = pair[0].parent == null ? 0 : pair[0].mainNumber;
		basic.firstYearDayMonth = new boolean[12][31];
		Arrays.fill(basic.firstYearDayMonth[startMonth], startDayOfMonth, 31, true);
		for (int month = startMonth + 1; month < 12; month++) {
			Arrays.fill(basic.firstYearDayMonth[month], true);
		}
		int endMonth = pair[1].parent == null ? pair[1].mainNumber : pair[1].parent.mainNumber;
		int endDayOfMonth = pair[1].parent == null ? 30 : pair[1].mainNumber;
		basic.lastYearDayMonth = new boolean[12][31];
		Arrays.fill(basic.lastYearDayMonth[endMonth], 0, endDayOfMonth + 1, true);
		for (int month = 0; month < endMonth; month++) {
			Arrays.fill(basic.lastYearDayMonth[month], true);
		}
	}

	private static void fillRuleArray(boolean[] array, Token[] pair) {
		if (pair[0].mainNumber <= pair[1].mainNumber) {
			for (int j = pair[0].mainNumber; j <= pair[1].mainNumber && j >= 0 && j < array.length; j++) {
				array[j] = true;
			}
		} else {
			// overflow
			for (int j = pair[0].mainNumber; j >= 0 && j < array.length; j++) {
				array[j] = true;
			}
			for (int j = 0; j <= pair[1].mainNumber && j < array.length; j++) {
				array[j] = true;
			}
		}
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
		String[] sequences = format.split("(?= \\|\\| )");
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
			if (sequences.size() > 1) {
				for (BasicOpeningHourRule bRule : basicRules) {
					if (!Algorithms.isEmpty(bRule.getComment())) {
						basicRuleComment = bRule.getComment();
						break;
					}
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
		return rs.rules.size() > 0 ? rs : null;
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

	private static void formatTimeRange(int startMinute, int endMinute, StringBuilder stringBuilder) {
		int startHour = (startMinute / 60) % 24;
		int endHour = (endMinute / 60) % 24;
		boolean sameDayPart = Math.max(startHour, endHour) < 12 || Math.min(startHour, endHour) >= 12;
		if (twelveHourFormatting && sameDayPart) {
			boolean amPmOnLeft = isAmPmOnLeft(startMinute);
			formatTime(startMinute, stringBuilder, amPmOnLeft);
			stringBuilder.append("-");
			formatTime(endMinute, stringBuilder, !amPmOnLeft);
		} else {
			formatTime(startMinute, stringBuilder);
			stringBuilder.append("-");
			formatTime(endMinute, stringBuilder);
		}
	}

	private static boolean isAmPmOnLeft(int startMinute) {
		StringBuilder sb = new StringBuilder();
		formatTime(startMinute, sb);
		return !Character.isDigit(sb.charAt(0));
	}

	private static void formatTime(int minutes, StringBuilder sb) {
		formatTime(minutes, sb, true);
	}

	private static void formatTime(int minutes, StringBuilder sb, boolean appendAmPM) {
		int hour = minutes / 60;
		int time = minutes - hour * 60;
		formatTime(hour, time, sb, appendAmPM);
	}

	private static void formatTime(int hours, int minutes, StringBuilder b, boolean appendAmPm) {
		if (twelveHourFormatting) {
			long millis = (hours * 60L + minutes) * 60 * 1000;
			Date date = new Date(millis);
			String time = appendAmPm ? twelveHourFormatterAmPm.format(date) : twelveHourFormatter.format(date);
			b.append(time);
		} else {
			if (hours < 10) {
				b.append("0"); //$NON-NLS-1$
			}
			b.append(hours).append(":"); //$NON-NLS-1$
			if (minutes < 10) {
				b.append("0"); //$NON-NLS-1$
			}
			b.append(minutes);
		}
	}
}