package net.osmand.shared.util

import net.osmand.shared.util.collections.KTIntArrayList
import kotlin.jvm.JvmStatic

/**
 * Parses the OSM `opening_hours` tag and answers whether a feature is open at a given time.
 *
 * Port of `net.osmand.util.OpeningHoursParser`. Structure and method order follow the Java original
 * so the two can be read side by side, with two deliberate differences:
 *
 * - the wall clock reading is [OpeningHoursTime] instead of `java.util.Calendar`, keeping the same
 *   field numbering so the rule logic is unchanged;
 * - the canonical OSM day and month names are constants here. The Java version derived them from
 *   `DateFormatSymbols.getInstance(Locale.US)`, which made the canonical output of [toRuleString]
 *   depend on the JVM's locale data. They are fixed OSM vocabulary, not locale data.
 *
 * Localized names and 12 hour formatting still come from the platform, through [PlatformDateNames].
 */
object OpeningHoursParser {

	/** Canonical OSM weekday tokens, indexed like `Calendar.DAY_OF_WEEK`: 1 is Sunday. */
	private val daysStr = arrayOf("", "Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")

	/** Canonical OSM month tokens, index 0 is January. */
	private val monthsStr =
		arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

	private var localDaysStr: Array<String> = daysStr
	private var localMothsStr: Array<String> = monthsStr

	private val additionalStrings = mutableMapOf(
		"off" to "off",
		"is_open" to "Open",
		"is_open_24_7" to "Open 24/7",
		"is_open_24_7_short" to "24/7",
		"will_open_at" to "Will open at",
		"will_open_at_short" to "From",
		"open_from" to "Open from",
		"open_from_short" to "From",
		"will_close_at" to "Will close at",
		"will_close_at_short" to "Until",
		"open_till" to "Open until",
		"open_till_short" to "Until",
		"will_open_tomorrow_at" to "Will open tomorrow at",
		"will_open_tomorrow_at_short" to "Tomorrow",
		"will_open_on" to "Will open on",
		"will_open_on_short" to "From"
	)

	private const val LOW_TIME_LIMIT = 120
	private const val WITHOUT_TIME_LIMIT = -1
	private const val CURRENT_DAY_TIME_LIMIT = -2

	private const val NO_TIME_MINUTES = -1
	private const val NO_NTH_WEEKDAY = 0
	private const val FIRST_NTH_WEEKDAY = 1
	private const val LAST_NTH_WEEKDAY = 5
	private const val DAYS_IN_WEEK = 7
	private const val NTH_WEEKDAY_FROM_END_OFFSET = LAST_NTH_WEEKDAY

	private var twelveHourFormatting = false
	private var twelveHourLanguageTag: String? = null

	init {
		initLocalStrings(null)
	}

	/**
	 * Reloads the localized day and month names for [languageTag], or the device language when null.
	 */
	@JvmStatic
	fun initLocalStrings(languageTag: String?) {
		localMothsStr = PlatformDateNames.shortMonths(languageTag)
		localDaysStr = getLettersStringArray(PlatformDateNames.shortWeekdays(languageTag), 3)
	}

	@JvmStatic
	fun setTwelveHourFormattingEnabled(enabled: Boolean, languageTag: String?) {
		twelveHourFormatting = enabled
		twelveHourLanguageTag = languageTag
	}

	/** Sets a localized string such as "off". */
	@JvmStatic
	fun setAdditionalString(key: String, value: String) {
		additionalStrings[key] = value
	}

	private fun getLettersStringArray(strings: Array<String>, letters: Int): Array<String> =
		Array(strings.size) { i ->
			val value = strings[i]
			val trimmed = if (value.length > letters) value.substring(0, letters) else value
			KAlgorithms.capitalizeFirstLetter(trimmed) ?: ""
		}

	private fun getDayIndex(i: Int): Int = when (i) {
		// the values of Calendar.MONDAY through Calendar.SUNDAY
		0 -> 2
		1 -> 3
		2 -> 4
		3 -> 5
		4 -> 6
		5 -> 7
		6 -> 1
		else -> -1
	}

	/**
	 * The whole opening hours schema of one feature, able to answer directly whether it is open.
	 */
	class OpeningHours : PlatformSerializable {

		private val rules: MutableList<OpeningHoursRule>
		private var original: String? = null
		private var sequenceCount = 0

		constructor(rules: MutableList<OpeningHoursRule>) {
			this.rules = rules
		}

		constructor() {
			this.rules = ArrayList()
		}

		/** A human readable summary of the state at one moment. */
		class Info {

			var opened = false
				internal set
			var opened24_7 = false
				internal set
			var fallback = false
				internal set
			internal var openingTime: String? = null
			internal var nearToOpeningTime: String? = null
			internal var closingTime: String? = null
			internal var nearToClosingTime: String? = null
			internal var openingTomorrow: String? = null
			internal var openingDay: String? = null
			internal var ruleString: String? = null

			fun isOpened(): Boolean = opened

			fun isOpened24_7(): Boolean = opened24_7

			fun isFallback(): Boolean = fallback

			fun getInfo(): String = getInfo(false)

			fun getShortInfo(): String = getInfo(true)

			private fun getInfo(brief: Boolean): String {
				return if (isOpened24_7()) {
					if (!isFallback()) {
						if (!KAlgorithms.isEmpty(ruleString)) {
							if (brief) ruleString!! else additional("is_open") + " " + ruleString
						} else {
							additional(if (brief) "is_open_24_7_short" else "is_open_24_7")
						}
					} else {
						if (!KAlgorithms.isEmpty(ruleString)) ruleString!! else ""
					}
				} else if (!KAlgorithms.isEmpty(nearToOpeningTime)) {
					formatInfo(if (brief) "will_open_at_short" else "will_open_at", nearToOpeningTime!!)
				} else if (!KAlgorithms.isEmpty(openingTime)) {
					formatInfo(if (brief) "open_from_short" else "open_from", openingTime!!)
				} else if (!KAlgorithms.isEmpty(nearToClosingTime)) {
					formatInfo(if (brief) "will_close_at_short" else "will_close_at", nearToClosingTime!!)
				} else if (!KAlgorithms.isEmpty(closingTime)) {
					formatInfo(if (brief) "open_till_short" else "open_till", closingTime!!)
				} else if (!KAlgorithms.isEmpty(openingTomorrow)) {
					formatInfo(if (brief) "will_open_tomorrow_at_short" else "will_open_tomorrow_at", openingTomorrow!!)
				} else if (!KAlgorithms.isEmpty(openingDay)) {
					val value = if (brief) openingDay!! else openingDay + "."
					formatInfo(if (brief) "will_open_on_short" else "will_open_on", value)
				} else {
					if (!KAlgorithms.isEmpty(ruleString)) ruleString!! else ""
				}
			}

			private fun additional(key: String): String = additionalStrings[key] ?: ""

			private fun formatInfo(key: String, value: String): String {
				val prefix = additionalStrings[key]
				return if (KAlgorithms.isEmpty(prefix)) value else "$prefix $value"
			}
		}

		fun getInfo(): List<Info>? = getInfo(OpeningHoursTime.now())

		fun getInfo(cal: OpeningHoursTime): List<Info>? {
			val res = ArrayList<Info>()
			for (i in 0 until sequenceCount) {
				res.add(getInfo(cal, i))
			}
			return if (res.isEmpty()) null else res
		}

		fun getCombinedInfo(): Info = getCombinedInfo(OpeningHoursTime.now())

		fun getCombinedInfo(cal: OpeningHoursTime): Info = getInfo(cal, ALL_SEQUENCES)

		private fun getInfo(cal: OpeningHoursTime, sequenceIndex: Int): Info {
			val info = Info()
			val opened = isOpenedForTimeV2(cal, sequenceIndex)
			info.fallback = isFallBackRule(sequenceIndex)
			info.opened = opened
			info.ruleString = getCurrentRuleTime(cal, sequenceIndex)
			if (opened) {
				info.opened24_7 = isOpened24_7(sequenceIndex)
				info.closingTime = getClosingTime(cal, sequenceIndex)
				info.nearToClosingTime = getNearToClosingTime(cal, sequenceIndex)
			} else {
				info.openingTime = getOpeningTime(cal, sequenceIndex)
				info.nearToOpeningTime = getNearToOpeningTime(cal, sequenceIndex)
				info.openingTomorrow = getOpeningTomorrow(cal, sequenceIndex)
				info.openingDay = getOpeningDay(cal, sequenceIndex)
			}
			return info
		}

		fun addRule(r: OpeningHoursRule) {
			rules.add(r)
		}

		fun addRules(rules: List<OpeningHoursRule>) {
			this.rules.addAll(rules)
		}

		fun getSequenceCount(): Int = sequenceCount

		fun setSequenceCount(sequenceCount: Int) {
			this.sequenceCount = sequenceCount
		}

		fun getRules(): MutableList<OpeningHoursRule> = rules

		fun getRules(sequenceIndex: Int): MutableList<OpeningHoursRule> {
			if (sequenceIndex == ALL_SEQUENCES) {
				return rules
			}
			val sequenceRules = ArrayList<OpeningHoursRule>()
			for (r in rules) {
				if (r.getSequenceIndex() == sequenceIndex) {
					sequenceRules.add(r)
				}
			}
			return sequenceRules
		}

		/**
		 * Checks whether the feature is open at [cal], starting from the most specific rule.
		 */
		fun isOpenedForTimeV2(cal: OpeningHoursTime, sequenceIndex: Int): Boolean {
			// make an exception for overlapping times, i.e.
			// (1) Mo 14:00-16:00; Tu off
			// (2) Mo 14:00-02:00; Tu off
			// in (2) the first rule must be checked even though it is against the specification,
			// because many OSM features are still tagged that way
			val rules = getRules(sequenceIndex)
			val overlap = hasRulesOverlapDayBackwardCompatible(rules)
			for (i in rules.indices.reversed()) {
				val rule = rules[i]
				if (rule.contains(cal)) {
					if (isTimeRestrictedOffRule(rule)) {
						return false
					}
					val checkNextNotNeeded = overlap || !isCheckNextNeeded(cal, rules, i, rule)
					val open = rule.isOpenedForTime(cal)
					if (open || !checkNextNotNeeded) {
						return open
					}
				}
			}
			return false
		}

		fun isOpenedForTime(cal: OpeningHoursTime): Boolean = isOpenedForTimeV2(cal, ALL_SEQUENCES)

		/** Convenience for callers holding an instant rather than a wall clock reading. */
		fun isOpenedForTime(epochMillis: Long): Boolean =
			isOpenedForTime(OpeningHoursTime.ofEpochMillis(epochMillis))

		fun isOpenedForTime(cal: OpeningHoursTime, sequenceIndex: Int): Boolean {
			// first check the rules that contain the current day, then the rules that contain the
			// previous day with times overlapping past midnight
			var isOpenDay = false
			val rules = getRules(sequenceIndex)
			for (r in rules) {
				if (r.containsDay(cal) && r.containsMonth(cal)) {
					isOpenDay = r.isOpenedForTime(cal, false)
				}
			}
			var isOpenPrevious = false
			for (r in rules) {
				if (r.containsPreviousDay(cal) && r.containsMonth(cal)) {
					isOpenPrevious = r.isOpenedForTime(cal, true)
				}
			}
			return isOpenDay || isOpenPrevious
		}

		fun isOpened24_7(sequenceIndex: Int): Boolean {
			var opened24_7 = false
			for (r in getRules(sequenceIndex)) {
				opened24_7 = r.isOpened24_7()
			}
			return opened24_7
		}

		fun getNearToOpeningTime(cal: OpeningHoursTime, sequenceIndex: Int): String =
			getTime(cal, LOW_TIME_LIMIT, true, sequenceIndex)

		fun getOpeningTime(cal: OpeningHoursTime, sequenceIndex: Int): String =
			getTime(cal, CURRENT_DAY_TIME_LIMIT, true, sequenceIndex)

		fun getNearToClosingTime(cal: OpeningHoursTime, sequenceIndex: Int): String =
			getTime(cal, LOW_TIME_LIMIT, false, sequenceIndex)

		fun getClosingTime(cal: OpeningHoursTime, sequenceIndex: Int): String =
			getTime(cal, WITHOUT_TIME_LIMIT, false, sequenceIndex)

		fun getOpeningTomorrow(calendar: OpeningHoursTime, sequenceIndex: Int): String {
			val cal = calendar.copy()
			var openingTime = ""
			val rules = getRules(sequenceIndex)
			cal.addDays(1)
			var openingTimeCal: OpeningHoursTime? = null
			var openingRule: OpeningHoursRule? = null
			for (r in rules) {
				if (r.contains(cal)) {
					val time = r.getTime(cal, false, WITHOUT_TIME_LIMIT, true)
					if (KAlgorithms.isEmpty(time) || openingTimeCal == null || cal.isBefore(openingTimeCal)
						|| r.hasOverlapTimes(cal, openingRule, false)
					) {
						openingTime = time
						openingRule = r
					}
					openingTimeCal = cal.copy()
				}
			}
			return openingTime
		}

		fun getOpeningDay(calendar: OpeningHoursTime, sequenceIndex: Int): String {
			val cal = calendar.copy()
			var openingTime = ""
			val rules = getRules(sequenceIndex)
			for (i in 0 until 7) {
				cal.addDays(1)
				var openingRule: OpeningHoursRule? = null
				var openingTimeCal: OpeningHoursTime? = null
				for (r in rules) {
					if (r.contains(cal)) {
						val time = r.getTime(cal, false, WITHOUT_TIME_LIMIT, true)
						if (KAlgorithms.isEmpty(time) || openingTimeCal == null || cal.isBefore(openingTimeCal)
							|| r.hasOverlapTimes(cal, openingRule, false)
						) {
							openingTime = time
							openingRule = r
						}
						openingTimeCal = cal.copy()
					}
				}
				if (!KAlgorithms.isEmpty(openingTime)) {
					openingTime += " " + localDaysStr[cal.dayOfWeek]
					break
				}
			}
			return openingTime
		}

		private fun getTime(cal: OpeningHoursTime, limit: Int, opening: Boolean, sequenceIndex: Int): String {
			if (!opening) {
				// an overnight session of the previous day which is still open ends before anything
				// the rules of the current day contribute (like "Fr 09:00-16:30" while a
				// "Mo-Th 09:00-00:30" session is running at Friday 00:15)
				val spillOverClosing = getSpillOverClosing(cal, limit, sequenceIndex)
				if (!KAlgorithms.isEmpty(spillOverClosing)) {
					return spillOverClosing
				}
			}
			var time = getTimeDay(cal, limit, opening, sequenceIndex)
			if (KAlgorithms.isEmpty(time)) {
				time = getTimeAnotherDay(cal, limit, opening, sequenceIndex)
			}
			return time
		}

		private fun getSpillOverClosing(cal: OpeningHoursTime, limit: Int, sequenceIndex: Int): String {
			for (r in getRules(sequenceIndex)) {
				if (r.containsPreviousDay(cal) && r.containsMonth(cal) && r.isOpenedForTime(cal, true)) {
					return r.getTime(cal, true, limit, false)
				}
			}
			return ""
		}

		private fun getTimeDay(cal: OpeningHoursTime, limit: Int, opening: Boolean, sequenceIndex: Int): String {
			var atTime = ""
			var atTimeMinutes = NO_TIME_MINUTES
			val rules = getRules(sequenceIndex)
			var prevRule: OpeningHoursRule? = null
			for (r in rules) {
				val appliesToDay = appliesToDay(r, cal)
				val timeRestrictedOff = isTimeRestrictedOffRule(r)
				val checkOffRule = timeRestrictedOff && (appliesToDay || (!opening && atTimeMinutes >= 0))
				if (!appliesToDay && !checkOffRule) {
					continue
				}
				if (appliesToDay && atTime.isNotEmpty() && prevRule != null && !r.hasOverlapTimes(cal, prevRule, true)) {
					return atTime
				}
				if (checkOffRule && r is BasicOpeningHourRule) {
					// rules like "Jul-Aug 19:00-19:30 off" make only their own time ranges "off",
					// so they adjust a time found by previous rules instead of discarding it
					val currentTimeMinutes = cal.hourOfDay * 60 + cal.minute
					if (opening) {
						val offLimit = if (limit == CURRENT_DAY_TIME_LIMIT) WITHOUT_TIME_LIMIT else limit
						val offTimeMinutes = r.getTimeMinutes(cal, false, offLimit, true)
						val currentlyOff = offTimeMinutes != NO_TIME_MINUTES && r.containsTime(currentTimeMinutes)
								&& prevRule is BasicOpeningHourRule && prevRule.containsTime(offTimeMinutes)
						if (offTimeMinutes != NO_TIME_MINUTES &&
							((atTimeMinutes >= 0 && r.containsTime(atTimeMinutes)) || currentlyOff)
						) {
							// the opening time found before is turned off, it moves to the end of the "off" range
							atTimeMinutes = offTimeMinutes
							atTime = r.formatResult(offTimeMinutes)
						}
					} else {
						val offTimeAbsolute = getNextTimeRestrictedOffStart(cal, r, limit)
						val atTimeAbsolute =
							if (atTimeMinutes < currentTimeMinutes) atTimeMinutes + 24 * 60 else atTimeMinutes
						if (offTimeAbsolute != NO_TIME_MINUTES && offTimeAbsolute < atTimeAbsolute) {
							// the "off" range starts before the closing time found before, so it closes earlier
							atTimeMinutes = offTimeAbsolute % (24 * 60)
							atTime = r.formatResult(atTimeMinutes)
						}
					}
				} else if (appliesToDay && r is BasicOpeningHourRule) {
					atTimeMinutes = r.getTimeMinutes(cal, false, limit, opening)
					val displayTimeMinutes = atTimeMinutes
					if (!opening && atTimeMinutes == NO_TIME_MINUTES && limit != WITHOUT_TIME_LIMIT) {
						atTimeMinutes = r.getTimeMinutes(cal, false, WITHOUT_TIME_LIMIT, false)
					}
					atTime = r.formatResult(displayTimeMinutes)
				} else if (appliesToDay) {
					atTime = r.getTime(cal, false, limit, opening)
					atTimeMinutes = NO_TIME_MINUTES
				}
				if (appliesToDay) {
					prevRule = r
				}
			}
			return atTime
		}

		private fun getNextTimeRestrictedOffStart(cal: OpeningHoursTime, offRule: BasicOpeningHourRule, limit: Int): Int {
			val currentTimeMinutes = cal.hourOfDay * 60 + cal.minute
			var nextTime = NO_TIME_MINUTES
			val ruleCal = cal.copy()
			for (dayOffset in 0..1) {
				if (appliesToDay(offRule, ruleCal)) {
					for (i in 0 until offRule.timesSize()) {
						val timeMinutes = offRule.getStartTime(i)
						val absoluteMinutes = dayOffset * 24 * 60 + timeMinutes
						val diff = absoluteMinutes - currentTimeMinutes
						if (diff >= 0 && (limit == WITHOUT_TIME_LIMIT || diff <= limit)
							&& (nextTime == NO_TIME_MINUTES || absoluteMinutes < nextTime)
						) {
							nextTime = absoluteMinutes
						}
					}
				}
				ruleCal.addDays(1)
			}
			return nextTime
		}

		/**
		 * Whether the rule applies to the calendar day of [cal], including rules defined by day-month
		 * or year ranges (like "Dec 24-Dec 31 off") which set no weekdays.
		 */
		private fun appliesToDay(r: OpeningHoursRule, cal: OpeningHoursTime): Boolean {
			if (r is BasicOpeningHourRule) {
				return r.appliesToDay(cal)
			}
			return r.containsDay(cal) && r.containsMonth(cal)
		}

		private fun isTimeRestrictedOffRule(r: OpeningHoursRule): Boolean =
			r is BasicOpeningHourRule && r.isOff() && r.timesSize() > 0

		private fun getTimeAnotherDay(cal: OpeningHoursTime, limit: Int, opening: Boolean, sequenceIndex: Int): String {
			var atTime = ""
			for (r in getRules(sequenceIndex)) {
				if (((opening && r.containsPreviousDay(cal)) || (!opening && r.containsNextDay(cal))) && r.containsMonth(cal)) {
					atTime = r.getTime(cal, true, limit, opening)
				}
			}
			return atTime
		}

		fun getCurrentRuleTime(cal: OpeningHoursTime): String? = getCurrentRuleTime(cal, ALL_SEQUENCES)

		fun isFallBackRule(sequenceIndex: Int): Boolean {
			if (sequenceIndex != ALL_SEQUENCES) {
				val rules = getRules(sequenceIndex)
				return rules.isNotEmpty() && rules[0].isFallbackRule()
			}
			return false
		}

		fun getCurrentRuleTime(cal: OpeningHoursTime, sequenceIndex: Int): String? {
			// see the note in isOpenedForTimeV2 about overlapping times
			val rules = getRules(sequenceIndex)
			var ruleClosed: String? = null
			val overlap = hasRulesOverlapDayBackwardCompatible(rules)
			for (i in rules.indices.reversed()) {
				val rule = rules[i]
				if (rule.contains(cal)) {
					if (isTimeRestrictedOffRule(rule)) {
						return rule.toLocalRuleString()
					}
					val checkNextNotNeeded = overlap || !isCheckNextNeeded(cal, rules, i, rule)
					val open = rule.isOpenedForTime(cal)
					if (open || !checkNextNotNeeded) {
						return rule.toLocalRuleString()
					} else {
						ruleClosed = rule.toLocalRuleString()
					}
				}
			}
			return ruleClosed
		}

		private fun isCheckNextNeeded(
			cal: OpeningHoursTime,
			rules: MutableList<OpeningHoursRule>,
			i: Int,
			rule: OpeningHoursRule
		): Boolean {
			var checkNext = true
			if (i > 0) {
				for (j in i downTo 1) {
					checkNext = rule.hasOverlapTimes(cal, rules[j - 1], false)
					if (checkNext) {
						break
					}
				}
			}
			return checkNext
		}

		private fun hasRulesOverlapDayBackwardCompatible(rules: MutableList<OpeningHoursRule>): Boolean {
			for (i in rules.indices.reversed()) {
				if (rules[i].hasOverlapTimesOverDay()) {
					return true
				}
			}
			return false
		}

		fun getCurrentRuleTimeV1(cal: OpeningHoursTime): String? {
			var ruleOpen: String? = null
			var ruleClosed: String? = null
			for (r in rules) {
				if (r.containsPreviousDay(cal) && r.containsMonth(cal)) {
					if (r.isOpenedForTime(cal, true)) {
						ruleOpen = r.toLocalRuleString()
					} else {
						ruleClosed = r.toLocalRuleString()
					}
				}
			}
			for (r in rules) {
				if (r.containsDay(cal) && r.containsMonth(cal)) {
					if (r.isOpenedForTime(cal, false)) {
						ruleOpen = r.toLocalRuleString()
					} else {
						ruleClosed = r.toLocalRuleString()
					}
				}
			}
			return ruleOpen ?: ruleClosed
		}

		override fun toString(): String {
			if (rules.isEmpty()) {
				return ""
			}
			val s = StringBuilder()
			for (r in rules) {
				s.append(r.toString()).append("; ")
			}
			return s.substring(0, s.length - 2)
		}

		fun toLocalString(): String {
			if (rules.isEmpty()) {
				return ""
			}
			val s = StringBuilder()
			for (r in rules) {
				s.append(r.toLocalRuleString()).append("; ")
			}
			return s.substring(0, s.length - 2)
		}

		fun setOriginal(original: String?) {
			this.original = original
		}

		fun getOriginal(): String? = original

		companion object {
			const val ALL_SEQUENCES = -1
		}
	}

	/**
	 * One rule: a collection of days or dates, plus a time range.
	 */
	interface OpeningHoursRule : PlatformSerializable {

		/**
		 * Whether the feature is open at [cal] for this rule. [checkPrevious] selects only the times
		 * that overflow past midnight from the previous day.
		 */
		fun isOpenedForTime(cal: OpeningHoursTime, checkPrevious: Boolean): Boolean

		fun isOpenedForTime(cal: OpeningHoursTime): Boolean

		/** Whether the day before [cal] is part of this rule. */
		fun containsPreviousDay(cal: OpeningHoursTime): Boolean

		/** Whether the day of [cal] is part of this rule. */
		fun containsDay(cal: OpeningHoursTime): Boolean

		/** Whether the day after [cal] is part of this rule. */
		fun containsNextDay(cal: OpeningHoursTime): Boolean

		/** Whether the month of [cal] is part of this rule. */
		fun containsMonth(cal: OpeningHoursTime): Boolean

		/** Whether the rule overlaps into the next day. */
		fun hasOverlapTimesOverDay(): Boolean

		/**
		 * Whether the times of [r] overlap the times of this rule at [cal]. [strictOverlap] also
		 * counts a touching boundary, where one range ends exactly as the other starts.
		 */
		fun hasOverlapTimes(cal: OpeningHoursTime, r: OpeningHoursRule?, strictOverlap: Boolean): Boolean

		/** Whether the rule applies at all at [cal], open or closed. */
		fun contains(cal: OpeningHoursTime): Boolean

		fun getSequenceIndex(): Int

		fun isFallbackRule(): Boolean

		fun toRuleString(): String

		fun toLocalRuleString(): String

		fun isOpened24_7(): Boolean

		fun getTime(cal: OpeningHoursTime, checkAnotherDay: Boolean, limit: Int, opening: Boolean): String
	}

	/**
	 * The basic rule: months, days of the week and numeric times, or the value "off".
	 */
	class BasicOpeningHourRule(private val sequenceIndex: Int) : OpeningHoursRule {

		constructor() : this(0)

		/** Open days, index 0 is Monday. */
		private var days = BooleanArray(7)

		internal var hasDays = false

		/** Per day masks for nth weekday restrictions such as "Su[1]", see parseNthMask. */
		private val dayNth = IntArray(7)

		/** Open months, index 0 is January. */
		internal val months = BooleanArray(12)

		/** Holds YEAR for the valid months of the first year: [0, 0, ... YEAR, YEAR, ... 0]. */
		internal var firstYearMonths: IntArray? = null
		internal var firstYearDayMonth: Array<BooleanArray>? = null
		internal var lastYearMonths: IntArray? = null
		internal var lastYearDayMonth: Array<BooleanArray>? = null
		internal var year = 0

		internal var fallback = false

		/** Open days per month. */
		private var dayMonths: Array<BooleanArray>? = null

		/** Equally sized lists of the start and end times. */
		internal val startTimes = KTIntArrayList()
		internal val endTimes = KTIntArrayList()

		internal var publicHoliday = false
		internal var schoolHoliday = false
		internal var easter = false

		/** Whether the times of this rule mean the feature is closed. */
		internal var off = false

		/**
		 * Free text limitation,
		 * see https://wiki.openstreetmap.org/wiki/Key:opening_hours/specification#explain:comment
		 */
		private var comment: String? = null

		override fun getSequenceIndex(): Int = sequenceIndex

		override fun isFallbackRule(): Boolean = fallback

		fun getDays(): BooleanArray = days

		fun getDayMonths(month: Int): BooleanArray {
			val current = dayMonths ?: Array(12) { BooleanArray(31) }.also { dayMonths = it }
			return current[month]
		}

		fun hasDayMonths(): Boolean = dayMonths != null

		fun getMonths(): BooleanArray = months

		fun appliesToPublicHolidays(): Boolean = publicHoliday

		fun appliesEaster(): Boolean = easter

		fun appliesToSchoolHolidays(): Boolean = schoolHoliday

		fun getComment(): String? = comment

		fun setComment(comment: String?) {
			this.comment = comment
		}

		/** Sets a single start time, erasing all previously added start times. */
		fun setStartTime(s: Int) {
			setSingleValue(startTimes, s)
			if (endTimes.size != 1) {
				setSingleValue(endTimes, 0)
			}
		}

		/** Sets a single end time, erasing all previously added end times. */
		fun setEndTime(e: Int) {
			setSingleValue(endTimes, e)
			if (startTimes.size != 1) {
				setSingleValue(startTimes, 0)
			}
		}

		/**
		 * Writes the start time at [position], appending when [position] is one past the last item.
		 */
		fun setStartTime(s: Int, position: Int) {
			if (position == startTimes.size) {
				startTimes.add(s)
				endTimes.add(0)
			} else {
				startTimes[position] = s
			}
		}

		/**
		 * Writes the end time at [position], appending when [position] is one past the last item.
		 */
		fun setEndTime(s: Int, position: Int) {
			if (position == startTimes.size) {
				endTimes.add(s)
				startTimes.add(0)
			} else {
				endTimes[position] = s
			}
		}

		fun getStartTime(): Int = if (startTimes.size == 0) 0 else startTimes[0]

		fun getStartTime(position: Int): Int = startTimes[position]

		fun getEndTime(): Int = if (endTimes.size == 0) 0 else endTimes[0]

		fun getEndTime(position: Int): Int = endTimes[position]

		/** An independent copy of all start times. */
		fun getStartTimes(): KTIntArrayList = KTIntArrayList(startTimes.toArray())

		/** An independent copy of all end times. */
		fun getEndTimes(): KTIntArrayList = KTIntArrayList(endTimes.toArray())

		fun setDays(days: BooleanArray) {
			if (this.days.size == days.size) {
				this.days = days
			}
		}

		override fun containsDay(cal: OpeningHoursTime): Boolean {
			val i = cal.dayOfWeek
			val d = (i + 5) % 7
			return days[d]
		}

		override fun containsNextDay(cal: OpeningHoursTime): Boolean {
			val i = cal.dayOfWeek
			val p = (i + 6) % 7
			return days[p]
		}

		override fun containsPreviousDay(cal: OpeningHoursTime): Boolean {
			val i = cal.dayOfWeek
			val p = (i + 4) % 7
			return days[p]
		}

		override fun containsMonth(cal: OpeningHoursTime): Boolean {
			val month = cal.month
			val year = cal.year
			if (!hasYears()) {
				return (this.year == 0 || this.year == year) && months[month]
			}
			if (this.year > year) {
				return false
			} else if (this.year < year) {
				val lastYearMonths = this.lastYearMonths ?: return false
				val lastYear = lastYearMonths[month]
				return lastYear > 0 && year <= lastYear
			} else {
				return firstYearMonths!![month] > 0
			}
		}

		override fun isOpenedForTime(cal: OpeningHoursTime, checkPrevious: Boolean): Boolean {
			val d = getCurrentDay(cal)
			val p = getPreviousDay(d)
			val time = getCurrentTimeInMinutes(cal)
			for (i in 0 until startTimes.size) {
				val startTime = startTimes[i]
				val endTime = endTimes[i]
				if (startTime < endTime || endTime == -1) {
					// a single day range such as 10:00-20:00, not 20:00-04:00
					if (days[d] && matchesDayNth(d, cal) && !checkPrevious) {
						if (time >= startTime && (endTime == -1 || time <= endTime)) {
							return !off
						}
					}
				} else {
					// the range wraps past midnight, like "We 20:00-03:00" or "We 07:00-07:00"
					if (time >= startTime && days[d] && matchesDayNth(d, cal) && !checkPrevious) {
						return !off
					} else if (time < endTime && days[p] && matchesPreviousDayNth(p, cal) && checkPrevious) {
						return !off
					}
				}
			}
			return false
		}

		private fun getCurrentDay(cal: OpeningHoursTime): Int = (cal.dayOfWeek + 5) % 7

		private fun getPreviousDay(currentDay: Int): Int {
			var p = currentDay - 1
			if (p < 0) {
				p += 7
			}
			return p
		}

		private fun getNextDay(currentDay: Int): Int {
			var n = currentDay + 1
			if (n > 6) {
				n -= 7
			}
			return n
		}

		private fun getCurrentTimeInMinutes(cal: OpeningHoursTime): Int = cal.hourOfDay * 60 + cal.minute

		override fun toRuleString(): String = toRuleString(false)

		private fun toRuleString(useLocalization: Boolean): String {
			val dayNames = if (useLocalization) localDaysStr else daysStr
			val monthNames = if (useLocalization) localMothsStr else monthsStr
			val offStr = if (useLocalization) (additionalStrings["off"] ?: "off") else "off"

			val b = StringBuilder(25)
			var allMonths = true
			for (month in months) {
				if (!month) {
					allMonths = false
					break
				}
			}
			val allDays = !hasDayMonths()
			if (!allDays) {
				val dayMonths = this.dayMonths!!
				var dash = false
				var first = true
				var monthAdded = -1
				var dayAdded = -1
				var excludedMonthEnd = -1
				var excludedDayEnd = -1
				var excludedMonthStart = -1
				var excludedDayStart = -1
				if (dayMonths[0][0] && dayMonths[11][30]) {
					var prevMonth = 0
					var prevDay = 0
					for (month in dayMonths.indices) {
						for (day in dayMonths[month].indices) {
							if (day == 1) {
								prevMonth = month
							}
							if (!dayMonths[month][day]) {
								excludedMonthEnd = prevMonth
								excludedDayEnd = prevDay
								break
							}
							prevDay = day
						}
						if (excludedDayEnd != -1) {
							break
						}
					}
					prevMonth = dayMonths.size - 1
					prevDay = dayMonths[prevMonth].size - 1
					for (month in dayMonths.indices.reversed()) {
						for (day in dayMonths[month].indices.reversed()) {
							if (day == dayMonths[month].size - 2) {
								prevMonth = month
							}
							if (!dayMonths[month][day]) {
								excludedMonthStart = prevMonth
								excludedDayStart = prevDay
								break
							}
							prevDay = day
						}
						if (excludedDayStart != -1) {
							break
						}
					}
				}
				var yearAdded = false
				for (month in dayMonths.indices) {
					for (day in dayMonths[month].indices) {
						if (excludedDayStart != -1 && excludedDayEnd != -1) {
							if (month < excludedMonthEnd || (month == excludedMonthEnd && day <= excludedDayEnd)) {
								continue
							} else if (month > excludedMonthStart || (month == excludedMonthStart && day >= excludedDayStart)) {
								continue
							}
						}
						if (dayMonths[month][day]) {
							if (day == 0 && dash && dayMonths[month][1]) {
								continue
							}
							if (day > 0 && dayMonths[month][day - 1]
								&& ((day < dayMonths[month].size - 1 && dayMonths[month][day + 1])
										|| (day == dayMonths[month].size - 1 && month < dayMonths.size - 1 && dayMonths[month + 1][0]))
							) {
								if (!dash) {
									dash = true
									if (!first) {
										b.append("-")
									}
								}
								continue
							}
							if (first) {
								first = false
							} else if (!dash) {
								b.append(", ")
								monthAdded = -1
							}
							yearAdded = appendYearString(b, if (dash) lastYearMonths else firstYearMonths, month)
							if (monthAdded != month || yearAdded) {
								b.append(monthNames[month]).append(" ")
								monthAdded = month
							}
							dayAdded = day + 1
							b.append(dayAdded)
							dash = false
						}
					}
				}
				if (excludedDayStart != -1 && excludedDayEnd != -1) {
					if (first) {
						first = false
					} else if (!dash) {
						b.append(", ")
					}
					appendYearString(b, firstYearMonths, excludedMonthStart)
					b.append(monthNames[excludedMonthStart]).append(" ").append(excludedDayStart + 1).append("-")
					appendYearString(b, lastYearMonths, excludedMonthEnd)
					b.append(monthNames[excludedMonthEnd]).append(" ").append(excludedDayEnd + 1)
				} else if (yearAdded && !dash && monthAdded != -1 && lastYearMonths != null) {
					b.append("-")
					appendYearString(b, lastYearMonths, monthAdded)
					b.append(monthNames[monthAdded])
					if (dayAdded != -1) {
						b.append(" ").append(dayAdded)
					}
				}
				if (!first) {
					b.append(" ")
				}
			} else if (!allMonths) {
				addArray(months, monthNames, b)
			}

			appendDaysString(b, dayNames)

			if (startTimes.size == 0) {
				if (isOpened24_7()) {
					b.setLength(0)
					if (!isFallbackRule()) {
						b.append("24/7 ")
					}
				}
				if (off) {
					b.append(offStr)
				}
			} else {
				if (isOpened24_7()) {
					b.setLength(0)
					b.append("24/7")
				} else {
					for (i in 0 until startTimes.size) {
						if (i > 0) {
							b.append(", ")
						}
						formatTimeRange(startTimes[i], endTimes[i], b)
					}
					if (off) {
						b.append(" ").append(offStr)
					}
				}
			}
			val comment = this.comment
			if (!KAlgorithms.isEmpty(comment)) {
				if (b.isNotEmpty()) {
					if (b[b.length - 1] != ' ') {
						b.append(" ")
					}
					b.append("- ").append(comment)
				} else {
					b.append(comment)
				}
			}
			return b.toString().trim()
		}

		private fun appendYearString(b: StringBuilder, yearMonths: IntArray?, month: Int): Boolean {
			if (yearMonths != null && yearMonths[month] > 0) {
				b.append(yearMonths[month]).append(" ")
				return true
			} else if (year > 0) {
				b.append(year).append(" ")
				return true
			}
			return false
		}

		private fun addArray(array: BooleanArray, arrayNames: Array<String>?, b: StringBuilder) {
			var dash = false
			var first = true
			for (i in array.indices) {
				if (array[i]) {
					if (i > 0 && array[i - 1] && i < array.size - 1 && array[i + 1]) {
						if (!dash) {
							dash = true
							b.append("-")
						}
						continue
					}
					if (first) {
						first = false
					} else if (!dash) {
						b.append(", ")
					}
					b.append(if (arrayNames == null) (i + 1).toString() else arrayNames[i])
					dash = false
				}
			}
			if (!first) {
				b.append(" ")
			}
		}

		override fun toLocalRuleString(): String = toRuleString(true)

		override fun isOpened24_7(): Boolean {
			if (isOpenedEveryDay()) {
				if (startTimes.size > 0) {
					for (i in 0 until startTimes.size) {
						if (startTimes[i] == 0 && endTimes[i] / 60 == 24) {
							return true
						}
					}
				} else {
					return true
				}
			}
			return false
		}

		fun isOpenedEveryDay(): Boolean {
			for (i in 0 until 7) {
				if (!days[i]) {
					return false
				}
			}
			return true
		}

		override fun getTime(cal: OpeningHoursTime, checkAnotherDay: Boolean, limit: Int, opening: Boolean): String =
			formatResult(getTimeMinutes(cal, checkAnotherDay, limit, opening))

		internal fun getTimeMinutes(cal: OpeningHoursTime, checkAnotherDay: Boolean, limit: Int, opening: Boolean): Int {
			val d = getCurrentDay(cal)
			val ad = if (opening) getNextDay(d) else getPreviousDay(d)
			val time = getCurrentTimeInMinutes(cal)
			for (i in 0 until startTimes.size) {
				val startTime = startTimes[i]
				val endTime = endTimes[i]
				if (opening != off) {
					if (startTime < endTime || endTime == -1) {
						if (days[d] && !checkAnotherDay) {
							val diff = startTime - time
							// for "off" rules skip time ranges that are already over
							if ((limit == WITHOUT_TIME_LIMIT && (!off || diff >= 0))
								|| (time <= startTime && (diff <= limit || limit == CURRENT_DAY_TIME_LIMIT))
							) {
								return startTime
							}
						}
					} else {
						var diff = -1
						if (time <= startTime && days[d] && !checkAnotherDay) {
							diff = startTime - time
						} else if (time > endTime && days[ad] && checkAnotherDay) {
							diff = 24 * 60 - endTime + time
						}
						// don't accept the time if the day checks above didn't match (diff == -1),
						// otherwise a "Mo-Th 09:00-00:30" rule reports Friday night as opening at 09:00
						if (limit == WITHOUT_TIME_LIMIT || (diff != -1 && (diff <= limit || limit == CURRENT_DAY_TIME_LIMIT))) {
							return startTime
						}
					}
				} else {
					if (startTime < endTime && endTime != -1) {
						if (days[d] && !checkAnotherDay) {
							val diff = endTime - time
							if ((limit == WITHOUT_TIME_LIMIT && diff >= 0) || (time <= endTime && diff <= limit)) {
								return endTime
							}
						}
					} else {
						var diff = -1
						if ((time >= startTime || time <= endTime) && days[d] && !checkAnotherDay) {
							// still inside an overnight session of the current day: the closing time is
							// "endTime" on the next calendar day, so during the evening part
							// (time >= startTime) the minutes to close must cross midnight
							diff = 24 * 60 - time + endTime
						} else if (time < endTime && days[ad] && checkAnotherDay) {
							diff = endTime - time
						}
						if (limit == WITHOUT_TIME_LIMIT || (diff != -1 && diff <= limit)) {
							return endTime
						}
					}
				}
			}
			return NO_TIME_MINUTES
		}

		internal fun formatResult(timeMinutes: Int): String {
			if (timeMinutes == NO_TIME_MINUTES) {
				return ""
			}
			val sb = StringBuilder()
			formatTime(timeMinutes, sb)
			var res = sb.toString()
			if (res.isNotEmpty() && !KAlgorithms.isEmpty(comment)) {
				res += " - " + comment
			}
			return res
		}

		/** Whether [timeMinutes], counted from midnight, falls inside the time ranges of this rule. */
		internal fun containsTime(timeMinutes: Int): Boolean {
			for (i in 0 until startTimes.size) {
				val startTime = startTimes[i]
				val endTime = endTimes[i]
				if (endTime == -1 || startTime >= endTime) {
					// open ended or wrapping past midnight
					if (timeMinutes >= startTime || (endTime != -1 && timeMinutes < endTime)) {
						return true
					}
				} else if (timeMinutes >= startTime && timeMinutes < endTime) {
					return true
				}
			}
			return false
		}

		/**
		 * Whether the rule applies to the calendar day of [cal], including rules defined by day-month
		 * or year ranges which set no weekdays.
		 */
		fun appliesToDay(cal: OpeningHoursTime): Boolean {
			if (!containsMonth(cal)) {
				return false
			}
			val month = cal.month
			val dmonth = cal.dayOfMonth - 1
			var thisDay = true
			if (hasYears()) {
				thisDay = isOpened(cal.year, month, dmonth)
			} else if (hasDayMonths()) {
				thisDay = dayMonths!![month][dmonth]
			}
			return thisDay && (!hasDays || (containsDay(cal) && matchesDayNth(getCurrentDay(cal), cal)))
		}

		fun isOff(): Boolean = off

		internal fun setDayNthMask(day: Int, mask: Int) {
			dayNth[day] = mask
		}

		/** Whether the day of [cal] matches the nth weekday restriction of [day], such as "Su[1]". */
		private fun matchesDayNth(day: Int, cal: OpeningHoursTime): Boolean {
			if (dayNth[day] == NO_NTH_WEEKDAY) {
				return true
			}
			val mask = dayNth[day]
			val dayOfMonth = cal.dayOfMonth
			val nthFromStart = (dayOfMonth - 1) / DAYS_IN_WEEK + 1
			val nthFromEnd = (cal.daysInMonth - dayOfMonth) / DAYS_IN_WEEK + 1
			return hasNthWeekday(mask, nthFromStart) || hasNthWeekday(mask, -nthFromEnd)
		}

		private fun matchesPreviousDayNth(previousDay: Int, cal: OpeningHoursTime): Boolean {
			if (dayNth[previousDay] == NO_NTH_WEEKDAY) {
				return true
			}
			val pcal = cal.copy()
			pcal.addDays(-1)
			return matchesDayNth(previousDay, pcal)
		}

		override fun toString(): String = toRuleString()

		fun appendDaysString(builder: StringBuilder) {
			appendDaysString(builder, daysStr)
		}

		fun appendDaysString(builder: StringBuilder, daysNames: Array<String>) {
			var dash = false
			var first = true
			for (i in 0 until 7) {
				if (days[i]) {
					if (i > 0 && days[i - 1] && i < 6 && days[i + 1]) {
						if (!dash) {
							dash = true
							builder.append("-")
						}
						continue
					}
					if (first) {
						first = false
					} else if (!dash) {
						builder.append(", ")
					}
					builder.append(daysNames[getDayIndex(i)])
					if (dayNth[i] != NO_NTH_WEEKDAY) {
						appendNthString(builder, dayNth[i])
					}
					dash = false
				}
			}
			if (publicHoliday) {
				if (!first) {
					builder.append(", ")
				}
				builder.append("PH")
				first = false
			}
			if (schoolHoliday) {
				if (!first) {
					builder.append(", ")
				}
				builder.append("SH")
				first = false
			}
			if (easter) {
				if (!first) {
					builder.append(", ")
				}
				builder.append("Easter")
				first = false
			}
			if (!first) {
				builder.append(" ")
			}
		}

		/** Adds a time range to this rule. */
		fun addTimeRange(startTime: Int, endTime: Int) {
			startTimes.add(startTime)
			endTimes.add(endTime)
		}

		fun timesSize(): Int = startTimes.size

		fun deleteTimeRange(position: Int) {
			startTimes.removeAt(position)
			endTimes.removeAt(position)
		}

		override fun isOpenedForTime(cal: OpeningHoursTime): Boolean = calculate(cal) > 0

		override fun contains(cal: OpeningHoursTime): Boolean = calculate(cal) != 0

		override fun hasOverlapTimesOverDay(): Boolean {
			for (i in 0 until startTimes.size) {
				if (startTimes[i] >= endTimes[i] && endTimes[i] > 0) {
					return true
				}
			}
			return false
		}

		override fun hasOverlapTimes(cal: OpeningHoursTime, r: OpeningHoursRule?, strictOverlap: Boolean): Boolean {
			if (off) {
				return true
			}
			if (r != null && r.contains(cal) && r is BasicOpeningHourRule) {
				if (startTimes.size > 0 && r.startTimes.size > 0) {
					for (i in 0 until startTimes.size) {
						val startTime = startTimes[i]
						var endTime = endTimes[i]
						if (endTime == -1) {
							endTime = 24 * 60
						} else if (startTime >= endTime) {
							endTime = 24 * 60 + endTime
						}
						for (k in 0 until r.startTimes.size) {
							val rStartTime = r.startTimes[k]
							var rEndTime = r.endTimes[k]
							if (rEndTime == -1) {
								rEndTime = 24 * 60
							} else if (rStartTime >= rEndTime) {
								rEndTime = 24 * 60 + rEndTime
							}
							if ((rStartTime >= startTime && (if (strictOverlap) rStartTime <= endTime else rStartTime < endTime))
								|| (startTime >= rStartTime && (if (strictOverlap) startTime <= rEndTime else startTime < rEndTime))
							) {
								return true
							}
						}
					}
				}
			}
			return false
		}

		private fun calculate(cal: OpeningHoursTime): Int {
			val year = cal.year
			val month = cal.month
			if (!containsMonth(cal)) {
				return 0
			}
			val dmonth = cal.dayOfMonth - 1
			val i = cal.dayOfWeek
			val day = (i + 5) % 7
			val previous = (day + 6) % 7
			var thisDay = true
			if (hasYears()) {
				thisDay = isOpened(year, month, dmonth)
			} else if (hasDayMonths()) {
				thisDay = dayMonths!![month][dmonth]
			}
			if (thisDay && hasDays) {
				thisDay = days[day] && matchesDayNth(day, cal)
			}
			// potential error for Dec 31 12:00-01:00
			var previousDay = true
			if (hasYears()) {
				if (dmonth > 0) {
					previousDay = isOpened(year, month, dmonth - 1)
				}
			} else if (hasDayMonths() && dmonth > 0) {
				previousDay = dayMonths!![month][dmonth - 1]
			}
			if (previousDay && hasDays) {
				previousDay = days[previous] && matchesPreviousDayNth(previous, cal)
			}
			if (!thisDay && !previousDay) {
				return 0
			}
			val time = cal.hourOfDay * 60 + cal.minute
			for (k in 0 until startTimes.size) {
				val startTime = startTimes[k]
				val endTime = endTimes[k]
				if (startTime < endTime || endTime == -1) {
					// a single day range such as 10:00-20:00, not 20:00-04:00
					if (time >= startTime && (endTime == -1 || time <= endTime) && thisDay) {
						return if (off) -1 else 1
					}
				} else {
					// the range wraps past midnight, like "We 20:00-03:00" or "We 07:00-07:00"
					if (time >= startTime && thisDay) {
						return if (off) -1 else 1
					} else if (time < endTime && previousDay) {
						return if (off) -1 else 1
					}
				}
			}
			if (thisDay && startTimes.isEmpty() && !off) {
				return 1
			} else if (thisDay && (startTimes.isEmpty() || !off)) {
				return -1
			}
			return 0
		}

		private fun isOpened(year: Int, month: Int, dmonth: Int): Boolean {
			var opened = hasDayMonths() && dayMonths!![month][dmonth]
			if (hasYears()) {
				if (year < this.year) {
					opened = false
				} else if (year == this.year) {
					val firstYearDayMonth = this.firstYearDayMonth
					opened = if (firstYearDayMonth != null) {
						firstYearDayMonth[month][dmonth]
					} else {
						// year-only and year+month rules populate no day-month masks, so the month range decides
						firstYearMonths!![month] > 0 && (!hasDayMonths() || dayMonths!![month][dmonth])
					}
				} else {
					val lastYear = lastYearMonths!![month]
					opened = if (year < lastYear) {
						true
					} else if (year == lastYear) {
						val lastYearDayMonth = this.lastYearDayMonth
						if (lastYearDayMonth != null) {
							lastYearDayMonth[month][dmonth]
						} else {
							// mirror the first-year fallback for the final year of a multi-year range
							lastYearMonths!![month] > 0 && (!hasDayMonths() || dayMonths!![month][dmonth])
						}
					} else {
						false
					}
				}
			}
			return opened
		}

		private fun hasYears(): Boolean = firstYearMonths != null

		companion object {

			private fun setSingleValue(list: KTIntArrayList, s: Int) {
				list.clear()
				list.add(s)
			}

			private fun appendNthString(builder: StringBuilder, mask: Int) {
				builder.append("[")
				var first = true
				for (nth in FIRST_NTH_WEEKDAY..LAST_NTH_WEEKDAY) {
					first = appendNthValue(builder, mask, nth, first)
				}
				for (nth in -FIRST_NTH_WEEKDAY downTo -LAST_NTH_WEEKDAY) {
					first = appendNthValue(builder, mask, nth, first)
				}
				builder.append("]")
			}

			private fun appendNthValue(builder: StringBuilder, mask: Int, nth: Int, first: Boolean): Boolean {
				if (!hasNthWeekday(mask, nth)) {
					return first
				}
				if (!first) {
					builder.append(",")
				}
				builder.append(nth)
				return false
			}
		}
	}

	/** A rule that could not be parsed; it never reports the feature as open. */
	class UnparseableRule(private val ruleString: String) : OpeningHoursRule {

		override fun isOpenedForTime(cal: OpeningHoursTime, checkPrevious: Boolean): Boolean = false

		override fun isOpenedForTime(cal: OpeningHoursTime): Boolean = false

		override fun containsPreviousDay(cal: OpeningHoursTime): Boolean = false

		override fun containsDay(cal: OpeningHoursTime): Boolean = false

		override fun containsNextDay(cal: OpeningHoursTime): Boolean = false

		override fun containsMonth(cal: OpeningHoursTime): Boolean = false

		override fun hasOverlapTimesOverDay(): Boolean = false

		override fun hasOverlapTimes(cal: OpeningHoursTime, r: OpeningHoursRule?, strictOverlap: Boolean): Boolean = false

		override fun contains(cal: OpeningHoursTime): Boolean = false

		override fun getSequenceIndex(): Int = 0

		override fun isFallbackRule(): Boolean = false

		override fun toRuleString(): String = ruleString

		override fun toLocalRuleString(): String = toRuleString()

		override fun isOpened24_7(): Boolean = false

		override fun getTime(cal: OpeningHoursTime, checkAnotherDay: Boolean, limit: Int, opening: Boolean): String = ""

		override fun toString(): String = toRuleString()
	}

	private enum class TokenType(val ord: Int) {
		TOKEN_UNKNOWN(0),
		TOKEN_COLON(1),
		TOKEN_COMMA(2),
		TOKEN_DASH(3),

		// the order matters
		TOKEN_YEAR(4),
		TOKEN_MONTH(5),
		TOKEN_DAY_MONTH(6),
		TOKEN_HOLIDAY(7),
		TOKEN_DAY_WEEK(7),
		TOKEN_HOUR_MINUTES(8),
		TOKEN_OFF_ON(9),
		TOKEN_COMMENT(10)
	}

	private class Token {

		var mainNumber = -1
		var nthMask = 0
		var type: TokenType
		var text: String
		var parent: Token? = null

		constructor(tokenType: TokenType, string: String) {
			type = tokenType
			text = string
			if (string.isNotEmpty() && string[string.length - 1].isDigit()
				&& (string[0].isDigit() || string[0] == '-' || string[0] == '+')
			) {
				mainNumber = KAlgorithms.parseIntSilently(string, -1)
			}
		}

		constructor(tokenType: TokenType, tokenMainNumber: Int) {
			type = tokenType
			mainNumber = tokenMainNumber
			text = mainNumber.toString()
		}

		override fun toString(): String {
			val parent = this.parent
			return if (parent != null) {
				"${parent.text} [${parent.type}] ($text [$type]) "
			} else {
				"$text [$type] "
			}
		}
	}

	@JvmStatic
	fun parseRuleV2(rule: String, sequenceIndex: Int, rules: MutableList<OpeningHoursRule>) {
		var r = rule.trim()

		val daysStr = arrayOf("mo", "tu", "we", "th", "fr", "sa", "su")
		val monthsStr = arrayOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
		val holidayStr = arrayOf("ph", "sh", "easter")
		val sunrise = "07:00"
		val sunset = "21:00"
		val endOfDay = "24:00"
		r = r.replace('(', ' ') // avoid "(mo-su 17:00-20:00"
		r = r.replace(')', ' ')
		val basic = BasicOpeningHourRule(sequenceIndex)
		if (r.startsWith("|| ")) {
			r = r.replace("|| ", "")
			basic.fallback = true
		}
		val localRuleString = Regex("sunset", RegexOption.IGNORE_CASE).replace(r, sunset)
			.let { Regex("sunrise", RegexOption.IGNORE_CASE).replace(it, sunrise) }
			.replace("+", "-$endOfDay")
		val days = basic.getDays()
		val months = basic.getMonths()
		if ("24/7" == localRuleString) {
			days.fill(true)
			basic.hasDays = true
			months.fill(true)
			basic.addTimeRange(0, 24 * 60)
			rules.add(basic)
			return
		}
		val tokens = ArrayList<Token>()
		var startWord = 0
		val commentStr = StringBuilder()
		var comment = false
		var bracket = false
		for (i in 0..localRuleString.length) {
			val ch = if (i == localRuleString.length) ' ' else localRuleString[i]
			if (i == localRuleString.length) {
				bracket = false
			}
			var delimiter = false
			var del: Token? = null
			if (ch == '[' && !comment) {
				// keep nth weekday brackets like "Su[1]" or "Su[-1]" together as one token
				bracket = true
			} else if (ch == ']' && !comment) {
				bracket = false
			} else if (ch.isWhitespace()) {
				delimiter = !bracket
			} else if (ch == ':') {
				del = Token(TokenType.TOKEN_COLON, ":")
			} else if (ch == '-' && !bracket) {
				del = Token(TokenType.TOKEN_DASH, "-")
			} else if (ch == ',' && !bracket) {
				del = Token(TokenType.TOKEN_COMMA, ",")
			} else if (ch == '"') {
				if (comment) {
					if (commentStr.isNotEmpty()) {
						tokens.add(Token(TokenType.TOKEN_COMMENT, commentStr.toString()))
					}
					startWord = i + 1
					commentStr.setLength(0)
					comment = false
				} else {
					comment = true
					continue
				}
			}
			if (comment) {
				commentStr.append(ch)
			} else if (delimiter || del != null) {
				val wrd = localRuleString.substring(startWord, i).trim()
				if (wrd.isNotEmpty()) {
					tokens.add(Token(TokenType.TOKEN_UNKNOWN, wrd.lowercase()))
				}
				startWord = i + 1
				if (del != null) {
					tokens.add(del)
				}
			}
		}
		// recognise days of the week
		for (t in tokens) {
			if (t.type == TokenType.TOKEN_UNKNOWN) {
				findInArray(t, daysStr, TokenType.TOKEN_DAY_WEEK)
			}
			if (t.type == TokenType.TOKEN_UNKNOWN) {
				findNthWeekday(t, daysStr)
			}
			if (t.type == TokenType.TOKEN_UNKNOWN) {
				findInArray(t, monthsStr, TokenType.TOKEN_MONTH)
			}
			if (t.type == TokenType.TOKEN_UNKNOWN) {
				findInArray(t, holidayStr, TokenType.TOKEN_HOLIDAY)
			}
			if (t.type == TokenType.TOKEN_UNKNOWN && ("off" == t.text || "closed" == t.text)) {
				t.type = TokenType.TOKEN_OFF_ON
				t.mainNumber = 0
			}
			if (t.type == TokenType.TOKEN_UNKNOWN && ("24/7" == t.text || "open" == t.text)) {
				t.type = TokenType.TOKEN_OFF_ON
				t.mainNumber = 1
			}
		}
		// recognise hours and minutes, as in "Dec 25: 08:30-20:00"
		for (i in tokens.size - 1 downTo 1) {
			if (tokens[i].type == TokenType.TOKEN_COLON) {
				if (i < tokens.size - 1) {
					if (tokens[i - 1].type == TokenType.TOKEN_UNKNOWN && tokens[i - 1].mainNumber != -1
						&& tokens[i + 1].type == TokenType.TOKEN_UNKNOWN && tokens[i + 1].mainNumber != -1
					) {
						tokens[i].mainNumber = 60 * tokens[i - 1].mainNumber + tokens[i + 1].mainNumber
						tokens[i].type = TokenType.TOKEN_HOUR_MINUTES
						tokens.removeAt(i + 1)
						tokens.removeAt(i - 1)
					}
				}
			} else if (tokens[i].type == TokenType.TOKEN_OFF_ON && tokens[i - 1].type == TokenType.TOKEN_OFF_ON) {
				tokens.removeAt(i - 1)
			}
		}
		// recognise the remaining numbers
		var monthSpecified = false
		for (t in tokens) {
			if (t.type == TokenType.TOKEN_MONTH) {
				monthSpecified = true
				break
			}
		}
		for (t in tokens) {
			if (t.type == TokenType.TOKEN_UNKNOWN && t.mainNumber >= 0) {
				if (monthSpecified && t.mainNumber <= 31) {
					t.type = TokenType.TOKEN_DAY_MONTH
					t.mainNumber = t.mainNumber - 1
				} else if (t.mainNumber > 1000) {
					t.type = TokenType.TOKEN_YEAR
				}
			}
		}
		buildRule(basic, tokens, rules)
	}

	private fun buildRule(basic: BasicOpeningHourRule, allTokens: List<Token>, rules: MutableList<OpeningHoursRule>) {
		// order: MONTH MONTH_DAY DAY_WEEK HOUR_MINUTE OPEN_OFF
		var tokens = allTokens
		var currentParse = TokenType.TOKEN_UNKNOWN
		var currentParseParent = TokenType.TOKEN_UNKNOWN
		val listOfPairs = ArrayList<Array<Token?>>()
		val presentTokens = mutableSetOf<TokenType>()
		var currentPair = arrayOfNulls<Token>(2)
		listOfPairs.add(currentPair)
		var prevToken: Token? = null
		var prevYearToken: Token? = null
		var indexP = 0
		var i = 0
		while (i <= tokens.size) {
			val t = if (i == tokens.size) null else tokens[i]
			if (i == 0 && t != null && t.type == TokenType.TOKEN_UNKNOWN) {
				// skip the rule when the first token is unknown
				return
			}
			if (t == null || t.type.ord > currentParse.ord) {
				presentTokens.add(currentParse)
				if (currentParse == TokenType.TOKEN_MONTH || currentParse == TokenType.TOKEN_DAY_MONTH
					|| currentParse == TokenType.TOKEN_DAY_WEEK || currentParse == TokenType.TOKEN_HOLIDAY
				) {
					val tokenDayMonth = currentParse == TokenType.TOKEN_DAY_MONTH
					var array: BooleanArray? = if (currentParse == TokenType.TOKEN_MONTH) basic.getMonths()
					else if (tokenDayMonth) null else basic.getDays()
					for (pair in listOfPairs) {
						val first = pair[0]
						val second = pair[1]
						if (first != null && second != null) {
							val holidayToken = if (first.type == TokenType.TOKEN_HOLIDAY) first
							else if (second.type == TokenType.TOKEN_HOLIDAY) second else null
							if (holidayToken != null
								&& (first.type == TokenType.TOKEN_DAY_WEEK || second.type == TokenType.TOKEN_DAY_WEEK)
							) {
								// "PH Su" means "public holidays falling on Sunday", so the weekday only
								// restricts the holiday and must not fill the weekday range Mo-Su (#23990)
								setHolidayFlag(basic, holidayToken.mainNumber)
								continue
							}
							val firstMonthToken =
								if (first.parent == null && first.type == TokenType.TOKEN_MONTH) first else first.parent
							val lastMonthToken =
								if (second.parent == null && second.type == TokenType.TOKEN_MONTH) second else second.parent
							if (tokenDayMonth && firstMonthToken != null) {
								if (lastMonthToken != null && lastMonthToken.mainNumber != firstMonthToken.mainNumber) {
									fillRuleArray(basic.getMonths(), arrayOf<Token?>(firstMonthToken, lastMonthToken))

									var t1 = Token(TokenType.TOKEN_DAY_MONTH, first.mainNumber)
									var t2 = Token(TokenType.TOKEN_DAY_MONTH, 30)
									array = basic.getDayMonths(firstMonthToken.mainNumber)
									fillRuleArray(array, arrayOf<Token?>(t1, t2))

									t1 = Token(TokenType.TOKEN_DAY_MONTH, 0)
									t2 = Token(TokenType.TOKEN_DAY_MONTH, second.mainNumber)
									array = basic.getDayMonths(lastMonthToken.mainNumber)
									fillRuleArray(array, arrayOf<Token?>(t1, t2))

									if (firstMonthToken.mainNumber <= lastMonthToken.mainNumber) {
										for (month in firstMonthToken.mainNumber + 1 until lastMonthToken.mainNumber) {
											basic.getDayMonths(month).fill(true)
										}
									} else {
										for (month in firstMonthToken.mainNumber + 1 until 12) {
											basic.getDayMonths(month).fill(true)
										}
										for (month in 0 until lastMonthToken.mainNumber) {
											basic.getDayMonths(month).fill(true)
										}
									}
								} else {
									array = basic.getDayMonths(firstMonthToken.mainNumber)
									fillRuleArray(array, pair)
								}
							} else if (array != null) {
								fillRuleArray(array, pair)
							}
							val ruleYear = basic.year
							if ((ruleYear > 0 || prevYearToken != null) && firstMonthToken != null && lastMonthToken != null) {
								// support shorthand like "2024-2025 Jan 1-Dec 31" by treating the last seen
								// year as the range end while keeping the first year already stored
								val endYear = prevYearToken?.mainNumber ?: ruleYear
								val startYear = if (ruleYear > 0) ruleYear else endYear
								if (basic.firstYearMonths == null) {
									basic.firstYearMonths = IntArray(12)
								}
								basic.firstYearMonths!!.fill(startYear, firstMonthToken.mainNumber, 12)
								if (endYear > startYear) {
									if (basic.lastYearMonths == null) {
										basic.lastYearMonths = IntArray(12)
									}
									basic.lastYearMonths!!.fill(endYear, 0, lastMonthToken.mainNumber + 1)
									if (endYear - startYear > 1) {
										val startInd = lastMonthToken.mainNumber + 1
										basic.lastYearMonths!!.fill(endYear - 1, startInd, 12)
									} else {
										val startInd = maxOf(lastMonthToken.mainNumber + 1, firstMonthToken.mainNumber)
										basic.lastYearMonths!!.fill(startYear, startInd, 12)
									}
									fillFirstLastYearsDayOfMonth(basic, pair)
									if (firstMonthToken.mainNumber >= lastMonthToken.mainNumber) {
										basic.months.fill(true)
									}
								}
							}
						} else if (first != null) {
							if (first.type == TokenType.TOKEN_HOLIDAY) {
								setHolidayFlag(basic, first.mainNumber)
							} else if (first.mainNumber >= 0) {
								val firstMonthToken = first.parent
								if (tokenDayMonth && firstMonthToken != null) {
									array = basic.getDayMonths(firstMonthToken.mainNumber)
								}
								val target = array
								if (target != null) {
									target[first.mainNumber] = true
									if (first.type == TokenType.TOKEN_DAY_WEEK && first.nthMask != NO_NTH_WEEKDAY) {
										basic.setDayNthMask(first.mainNumber, first.nthMask)
									}
								}
							}
						}
					}
				} else if (currentParse == TokenType.TOKEN_HOUR_MINUTES) {
					for (pair in listOfPairs) {
						val first = pair[0]
						val second = pair[1]
						if (first != null && second != null) {
							basic.addTimeRange(first.mainNumber, second.mainNumber)
						}
					}
				} else if (currentParse == TokenType.TOKEN_OFF_ON) {
					val l = listOfPairs[0]
					if (l[0] != null && l[0]!!.mainNumber == 0) {
						basic.off = true
					}
				} else if (currentParse == TokenType.TOKEN_COMMENT) {
					val l = listOfPairs[0]
					val token = l[0]
					if (token != null && !KAlgorithms.isEmpty(token.text)) {
						basic.setComment(token.text)
					}
				} else if (currentParse == TokenType.TOKEN_YEAR) {
					if (listOfPairs.size > 1) {
						// comma separated years have set semantics, so expand each year or year range
						// into an independent rule that shares the same month and day tail
						for (pair in listOfPairs) {
							val newRule = BasicOpeningHourRule(basic.getSequenceIndex())
							newRule.fallback = basic.fallback
							newRule.setComment(basic.getComment())
							val yearTokens = ArrayList<Token>()
							pair[0]?.let { yearTokens.add(it) }
							pair[1]?.let { yearTokens.add(it) }
							if (i < tokens.size) {
								yearTokens.addAll(tokens.subList(i, tokens.size))
							}
							buildRule(newRule, yearTokens, rules)
						}
						return
					}
					var firstYearToken: Token? = null
					var lastYearToken: Token? = null
					for (pair in listOfPairs) {
						for (yearToken in pair) {
							if (yearToken != null && yearToken.mainNumber > 1000) {
								if (firstYearToken == null) {
									firstYearToken = yearToken
								}
								lastYearToken = yearToken
							}
						}
					}
					if (firstYearToken != null) {
						if (basic.year == 0) {
							basic.year = firstYearToken.mainNumber
						}
						prevYearToken = lastYearToken
					}
				}
				listOfPairs.clear()
				currentPair = arrayOfNulls(2)
				indexP = 0
				listOfPairs.add(currentPair)
				currentPair[indexP++] = t
				if (t != null) {
					currentParse = t.type
					currentParseParent = currentParse
					if (t.type == TokenType.TOKEN_DAY_MONTH && prevToken != null && prevToken.type == TokenType.TOKEN_MONTH) {
						t.parent = prevToken
						currentParseParent = prevToken.type
					} else if (t.type == TokenType.TOKEN_MONTH && prevToken != null && prevToken.type == TokenType.TOKEN_YEAR) {
						if (basic.year == 0) {
							// the first year of a range such as "2019 Oct - 2024 dec"
							basic.year = prevToken.mainNumber
						}
					}
				}
			} else if (t.type.ord < currentParseParent.ord && indexP == 0 && tokens.size > i) {
				val newRule = BasicOpeningHourRule(basic.getSequenceIndex())
				newRule.setComment(basic.getComment())
				buildRule(newRule, tokens.subList(i, tokens.size), rules)
				tokens = tokens.subList(0, i + 1)
			} else if (t.type == TokenType.TOKEN_COMMA) {
				if (tokens.size > i + 1 && tokens[i + 1].type.ord < currentParseParent.ord) {
					indexP = 0
				} else {
					currentPair = arrayOfNulls(2)
					indexP = 0
					listOfPairs.add(currentPair)
				}
			} else if (t.type == TokenType.TOKEN_DASH) {
				// nothing to do, the dash only joins the two ends of a pair
			} else if (t.type.ord == currentParse.ord) {
				if (indexP < 2) {
					currentPair[indexP++] = t
					if (t.type == TokenType.TOKEN_DAY_MONTH && prevToken != null && prevToken.type == TokenType.TOKEN_MONTH) {
						t.parent = prevToken
					} else if (t.type == TokenType.TOKEN_YEAR) {
						// keep the second year inside the current pair so the month and day range code
						// can build a proper multi-year span instead of collapsing to the first year
						prevYearToken = t
					}
				}
			} else if (t.type == TokenType.TOKEN_YEAR) {
				prevYearToken = t
			}
			prevToken = t
			i++
		}
		if (!presentTokens.contains(TokenType.TOKEN_MONTH)) {
			basic.getMonths().fill(true)
		}
		if (!presentTokens.contains(TokenType.TOKEN_DAY_WEEK) && !presentTokens.contains(TokenType.TOKEN_HOLIDAY)
			&& !presentTokens.contains(TokenType.TOKEN_DAY_MONTH)
		) {
			basic.getDays().fill(true)
			basic.hasDays = true
		} else if (presentTokens.contains(TokenType.TOKEN_DAY_WEEK) || presentTokens.contains(TokenType.TOKEN_HOLIDAY)) {
			basic.hasDays = true
		}
		rules.add(0, basic)
	}

	private fun setHolidayFlag(basic: BasicOpeningHourRule, holidayIndex: Int) {
		when (holidayIndex) {
			0 -> basic.publicHoliday = true
			1 -> basic.schoolHoliday = true
			2 -> basic.easter = true
		}
	}

	private fun fillFirstLastYearsDayOfMonth(basic: BasicOpeningHourRule, pair: Array<Token?>) {
		val first = pair[0]!!
		val second = pair[1]!!
		val startMonth = if (first.parent == null) first.mainNumber else first.parent!!.mainNumber
		val startDayOfMonth = if (first.parent == null) 0 else first.mainNumber
		val firstYearDayMonth = Array(12) { BooleanArray(31) }
		basic.firstYearDayMonth = firstYearDayMonth
		firstYearDayMonth[startMonth].fill(true, startDayOfMonth, 31)
		for (month in startMonth + 1 until 12) {
			firstYearDayMonth[month].fill(true)
		}
		val endMonth = if (second.parent == null) second.mainNumber else second.parent!!.mainNumber
		val endDayOfMonth = if (second.parent == null) 30 else second.mainNumber
		val lastYearDayMonth = Array(12) { BooleanArray(31) }
		basic.lastYearDayMonth = lastYearDayMonth
		lastYearDayMonth[endMonth].fill(true, 0, endDayOfMonth + 1)
		for (month in 0 until endMonth) {
			lastYearDayMonth[month].fill(true)
		}
	}

	private fun fillRuleArray(array: BooleanArray, pair: Array<Token?>) {
		val from = pair[0]!!.mainNumber
		val to = pair[1]!!.mainNumber
		if (from <= to) {
			var j = from
			while (j <= to && j >= 0 && j < array.size) {
				array[j] = true
				j++
			}
		} else {
			// the range wraps around the end of the year
			var j = from
			while (j >= 0 && j < array.size) {
				array[j] = true
				j++
			}
			j = 0
			while (j <= to && j < array.size) {
				array[j] = true
				j++
			}
		}
	}

	private fun findInArray(t: Token, list: Array<String>, tokenType: TokenType) {
		for (i in list.indices) {
			if (list[i] == t.text) {
				t.type = tokenType
				t.mainNumber = i
				break
			}
		}
	}

	/** Recognises nth weekday tokens such as "su[1]", "su[-1]" or "su[1,3]". */
	private fun findNthWeekday(t: Token, daysStr: Array<String>) {
		val bracket = t.text.indexOf('[')
		if (bracket <= 0 || !t.text.endsWith("]")) {
			return
		}
		val day = t.text.substring(0, bracket)
		for (i in daysStr.indices) {
			if (daysStr[i] == day) {
				val mask = parseNthMask(t.text.substring(bracket + 1, t.text.length - 1))
				if (mask != NO_NTH_WEEKDAY) {
					t.type = TokenType.TOKEN_DAY_WEEK
					t.mainNumber = i
					t.nthMask = mask
				}
				return
			}
		}
	}

	/**
	 * Parses an nth weekday list such as "1", "-1" or "1,3" into a bit mask, where positive values
	 * count from the start of the month and negative values count from its end.
	 */
	private fun parseNthMask(list: String): Int {
		var mask = NO_NTH_WEEKDAY
		for (part in list.split(",")) {
			val n = KAlgorithms.parseIntSilently(part.trim(), 0)
			val nthMask = getNthWeekdayMask(n)
			if (nthMask == NO_NTH_WEEKDAY) {
				return NO_NTH_WEEKDAY
			}
			mask = mask or nthMask
		}
		return mask
	}

	private fun hasNthWeekday(mask: Int, nth: Int): Boolean {
		val nthMask = getNthWeekdayMask(nth)
		return nthMask != NO_NTH_WEEKDAY && (mask and nthMask) != 0
	}

	private fun getNthWeekdayMask(nth: Int): Int {
		if (nth in FIRST_NTH_WEEKDAY..LAST_NTH_WEEKDAY) {
			return 1 shl (nth - FIRST_NTH_WEEKDAY)
		}
		if (nth <= -FIRST_NTH_WEEKDAY && nth >= -LAST_NTH_WEEKDAY) {
			return 1 shl (NTH_WEEKDAY_FROM_END_OFFSET + (-nth - FIRST_NTH_WEEKDAY))
		}
		return NO_NTH_WEEKDAY
	}

	private fun splitSequences(format: String?): List<List<String>>? {
		if (format == null) {
			return null
		}
		val res = ArrayList<List<String>>()
		for (sequence in Regex("(?= \\|\\| )").split(format)) {
			val seq = sequence.trim()
			if (seq.isEmpty()) {
				continue
			}
			val rules = ArrayList<String>()
			var comment = false
			val sb = StringBuilder()
			for (c in seq) {
				if (c == '"') {
					comment = !comment
					sb.append(c)
				} else if (c == ';' && !comment) {
					if (sb.isNotEmpty()) {
						val s = sb.toString().trim()
						if (s.isNotEmpty()) {
							rules.add(s)
						}
						sb.setLength(0)
					}
				} else {
					sb.append(c)
				}
			}
			if (sb.isNotEmpty()) {
				rules.add(sb.toString())
				sb.setLength(0)
			}
			res.add(rules)
		}
		return res
	}

	@JvmStatic
	fun parseRules(r: String, sequenceIndex: Int, rules: MutableList<OpeningHoursRule>) {
		parseRuleV2(r, sequenceIndex, rules)
	}

	/**
	 * Parses an OSM `opening_hours` string, returning null when nothing could be parsed.
	 */
	@JvmStatic
	fun parseOpenedHours(format: String?): OpeningHours? {
		if (format == null) {
			return null
		}
		val rs = OpeningHours()
		rs.setOriginal(format)
		val sequences = splitSequences(format)!!
		for (i in sequences.indices) {
			val basicRules = ArrayList<BasicOpeningHourRule>()
			for (r in sequences[i]) {
				val rList = ArrayList<OpeningHoursRule>()
				parseRules(r, i, rList)
				for (rule in rList) {
					if (rule is BasicOpeningHourRule) {
						basicRules.add(rule)
					}
				}
			}
			var basicRuleComment: String? = null
			if (sequences.size > 1) {
				for (bRule in basicRules) {
					if (!KAlgorithms.isEmpty(bRule.getComment())) {
						basicRuleComment = bRule.getComment()
						break
					}
				}
			}
			if (!KAlgorithms.isEmpty(basicRuleComment)) {
				for (bRule in basicRules) {
					bRule.setComment(basicRuleComment)
				}
			}
			rs.addRules(basicRules)
		}
		rs.setSequenceCount(sequences.size)
		return if (rs.getRules().size > 0) rs else null
	}

	/**
	 * Parses an OSM `opening_hours` string, keeping anything it fails to parse as an
	 * [UnparseableRule] rather than returning null.
	 */
	@JvmStatic
	fun parseOpenedHoursHandleErrors(format: String?): OpeningHours? {
		if (format == null) {
			return null
		}
		val rs = OpeningHours()
		rs.setOriginal(format)
		val sequences = splitSequences(format)!!
		for (i in sequences.indices.reversed()) {
			for (rule in sequences[i]) {
				val r = rule.trim()
				if (r.isEmpty()) {
					continue
				}
				val rList = ArrayList<OpeningHoursRule>()
				parseRules(r, i, rList)
				rs.addRules(rList)
			}
		}
		rs.setSequenceCount(sequences.size)
		return rs
	}

	@JvmStatic
	fun getInfo(format: String): List<OpeningHours.Info>? = parseOpenedHours(format)?.getInfo()

	private fun formatTimeRange(startMinute: Int, endMinute: Int, stringBuilder: StringBuilder) {
		val startHour = (startMinute / 60) % 24
		val endHour = (endMinute / 60) % 24
		val sameDayPart = maxOf(startHour, endHour) < 12 || minOf(startHour, endHour) >= 12
		if (twelveHourFormatting && sameDayPart) {
			val amPmOnLeft = isAmPmOnLeft(startMinute)
			formatTime(startMinute, stringBuilder, amPmOnLeft)
			stringBuilder.append("-")
			formatTime(endMinute, stringBuilder, !amPmOnLeft)
		} else {
			formatTime(startMinute, stringBuilder)
			stringBuilder.append("-")
			formatTime(endMinute, stringBuilder)
		}
	}

	private fun isAmPmOnLeft(startMinute: Int): Boolean {
		val sb = StringBuilder()
		formatTime(startMinute, sb)
		return !sb[0].isDigit()
	}

	private fun formatTime(minutes: Int, sb: StringBuilder) {
		formatTime(minutes, sb, true)
	}

	private fun formatTime(minutes: Int, sb: StringBuilder, appendAmPm: Boolean) {
		val hour = minutes / 60
		formatTime(hour, minutes - hour * 60, sb, appendAmPm)
	}

	private fun formatTime(hours: Int, minutes: Int, b: StringBuilder, appendAmPm: Boolean) {
		if (twelveHourFormatting) {
			b.append(PlatformDateNames.formatTwelveHourTime(hours * 60 + minutes, twelveHourLanguageTag, appendAmPm))
		} else {
			if (hours < 10) {
				b.append("0")
			}
			b.append(hours).append(":")
			if (minutes < 10) {
				b.append("0")
			}
			b.append(minutes)
		}
	}
}
