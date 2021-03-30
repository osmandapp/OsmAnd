package net.osmand;

import java.text.ParseException;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Period {

	public enum PeriodUnit {
		YEAR("Y", Calendar.YEAR),
		MONTH("M", Calendar.MONTH),
		WEEK("W", Calendar.WEEK_OF_YEAR),
		DAY("D", Calendar.DATE);

		private String unitStr;
		private int calendarIdx;

		PeriodUnit(String unitStr, int calendarIdx) {
			this.calendarIdx = calendarIdx;
			this.unitStr = unitStr;
		}

		public String getUnitStr() {
			return unitStr;
		}

		public int getCalendarIdx() {
			return calendarIdx;
		}

		public double getMonthsValue() {
			switch (this) {
				case YEAR:
					return 12d;
				case MONTH:
					return 1d;
				case WEEK:
					return 1/4d;
				case DAY:
					return 1/30d;
			}
			return 0d;
		}

		public static PeriodUnit parseUnit(String unitStr) {
			for (PeriodUnit unit : values()) {
				if (unit.unitStr.equals(unitStr)) {
					return unit;
				}
			}
			return null;
		}
	}

	private static final Pattern PATTERN =
			Pattern.compile("^P(?:([-+]?[0-9]+)([YMWD]))?$", Pattern.CASE_INSENSITIVE);

	private PeriodUnit unit;

	private final int numberOfUnits;

	public static Period ofYears(int years) {
		return new Period(PeriodUnit.YEAR, years);
	}

	public static Period ofMonths(int months) {
		return new Period(PeriodUnit.MONTH, months);
	}

	public static Period ofWeeks(int weeks) {
		return new Period(PeriodUnit.WEEK, weeks);
	}

	public static Period ofDays(int days) {
		return new Period(PeriodUnit.DAY, days);
	}

	public PeriodUnit getUnit() {
		return unit;
	}

	public int getNumberOfUnits() {
		return numberOfUnits;
	}

	/**
	 * Obtains a {@code Period} from a text string such as {@code PnY PnM PnD PnW}.
	 * <p>
	 * This will parse the string produced by {@code toString()} which is
	 * based on the ISO-8601 period formats {@code PnY PnM PnD PnW}.
	 * <p>
	 * The string cannot start with negative sign.
	 * The ASCII letter "P" is next in upper or lower case.
	 * <p>
	 * For example, the following are valid inputs:
	 * <pre>
	 *   "P2Y"             -- Period.ofYears(2)
	 *   "P3M"             -- Period.ofMonths(3)
	 *   "P4W"             -- Period.ofWeeks(4)
	 *   "P5D"             -- Period.ofDays(5)
	 * </pre>
	 *
	 * @param text the text to parse, not null
	 * @return the parsed period, not null
	 * @throws ParseException if the text cannot be parsed to a period
	 */
	public static Period parse(CharSequence text) throws ParseException {
		Matcher matcher = PATTERN.matcher(text);
		if (matcher.matches()) {
			String numberOfUnitsMatch = matcher.group(1);
			String unitMatch = matcher.group(2);
			if (numberOfUnitsMatch != null && unitMatch != null) {
				try {
					int numberOfUnits = parseNumber(numberOfUnitsMatch);
					PeriodUnit unit = PeriodUnit.parseUnit(unitMatch);
					return new Period(unit, numberOfUnits);
				} catch (IllegalArgumentException ex) {
					throw new ParseException("Text cannot be parsed to a Period: " + text, 0);
				}
			}
		}
		throw new ParseException("Text cannot be parsed to a Period: " + text, 0);
	}

	private static int parseNumber(String str) throws ParseException {
		if (str == null) {
			return 0;
		}
		return Integer.parseInt(str);
	}

	public Period(PeriodUnit unit, int numberOfUnits) {
		if (unit == null) {
			throw new IllegalArgumentException("PeriodUnit cannot be null");
		}
		this.unit = unit;
		this.numberOfUnits = numberOfUnits;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof Period) {
			Period other = (Period) obj;
			return unit.ordinal() == other.unit.ordinal() && numberOfUnits == other.numberOfUnits;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return unit.ordinal() + Integer.rotateLeft(numberOfUnits, 8);
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append('P').append(numberOfUnits);
		switch (unit) {
			case YEAR:
				buf.append('Y');
				break;
			case MONTH:
				buf.append('M');
				break;
			case WEEK:
				buf.append('W');
				break;
			case DAY:
				buf.append('D');
				break;
		}
		return buf.toString();
	}
}
