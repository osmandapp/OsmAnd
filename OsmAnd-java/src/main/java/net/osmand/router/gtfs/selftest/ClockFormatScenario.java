package net.osmand.router.gtfs.selftest;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.osmand.router.TimedTransportRoute;
import net.osmand.router.TransportClockFormat;

/** TransportClockFormat in the device locale and with 12 and 24 hour forced, against fixed expected strings. */
final class ClockFormatScenario {

	private static final ZoneId NY = ZoneId.of("America/New_York");
	private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");

	private ClockFormatScenario() {
	}

	private static long ny(int month, int day, int h, int m, int s) {
		return LocalDateTime.of(2026, month, day, h, m, s).atZone(NY).toEpochSecond();
	}

	/** Narrow no-break space (U+202F) and no-break space become a plain space. */
	private static String plain(String s) {
		return s.replace(' ', ' ').replace(' ', ' ');
	}

	private static String codepoints(String s) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			sb.append(c < 128 && c >= 32 ? String.valueOf(c) : String.format(Locale.ROOT, "\\u%04x", (int) c));
		}
		return sb.toString();
	}

	static void run(SelfTestEnv env, ScenarioResult r) {
		long[] t = {ny(11, 17, 8, 5, 0), ny(11, 17, 0, 0, 0), ny(11, 17, 12, 0, 0), ny(11, 17, 23, 59, 59), ny(11, 17, 0, 30, 0),
				ny(11, 17, 13, 7, 30)};
		String[] want24 = {"08:05", "00:00", "12:00", "23:59", "00:30", "13:07"};
		String[] want12 = {"8:05 AM", "12:00 AM", "12:00 PM", "11:59 PM", "12:30 AM", "1:07 PM"};

		TransportClockFormat us24 = new TransportClockFormat(true, Locale.US);
		TransportClockFormat us12 = new TransportClockFormat(false, Locale.US);
		List<Map<String, Object>> rows = new ArrayList<>();
		for (int i = 0; i < t.length; i++) {
			String a = us24.format(t[i], NY), b = us12.format(t[i], NY);
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("epoch", t[i]);
			row.put("us24", a);
			row.put("us12", b);
			row.put("us12Codepoints", codepoints(b));
			row.put("want24", want24[i]);
			row.put("want12", want12[i]);
			rows.add(row);
			r.expect(want24[i].equals(a), "24h Locale.US: expected '" + want24[i] + "' got '" + a + "'");
			r.expect(want12[i].equals(plain(b)), "12h Locale.US: expected '" + want12[i] + "' got '" + codepoints(b) + "'");
		}
		r.put("usCases", rows);
		r.put("us12UsesNarrowNbsp", us12.format(t[0], NY).indexOf(' ') >= 0);

		// the agency zone, not the device zone, decides the text
		r.expect("22:05".equals(us24.format(t[0], TOKYO)), "same instant in Tokyo: " + us24.format(t[0], TOKYO));
		r.expect("08:05".equals(us24.format(t[0], NY)), "same instant in New York");

		// DST fall-back day (2026-11-01): 01:30 happens twice, one hour apart, and both print 01:30
		long first = LocalDateTime.of(2026, 11, 1, 1, 30).atZone(NY).withEarlierOffsetAtOverlap().toEpochSecond();
		long second = LocalDateTime.of(2026, 11, 1, 1, 30).atZone(NY).withLaterOffsetAtOverlap().toEpochSecond();
		r.put("dstOverlapSecondsApart", second - first);
		r.expect(second - first == 3600, "DST overlap not one hour apart");
		r.expect("01:30".equals(us24.format(first, NY)) && "01:30".equals(us24.format(second, NY)), "DST overlap text");
		// spring forward (2027-03-14): 02:30 does not exist, the clock jumps 01:59 -> 03:00
		long before = LocalDateTime.of(2027, 3, 14, 1, 59).atZone(NY).toEpochSecond();
		r.expect("03:00".equals(us24.format(before + 60, NY)), "spring forward: " + us24.format(before + 60, NY));

		// a range
		final long dep = t[0], arr = ny(11, 17, 8, 54, 0);
		TimedTransportRoute route = new TimedTransportRoute() {
			@Override
			public long getDepartureTimeSec() {
				return dep;
			}

			@Override
			public long getArrivalTimeSec() {
				return arr;
			}

			@Override
			public ZoneId getTimeZone() {
				return NY;
			}
		};
		String range = us24.formatRange(route);
		r.put("range24", range);
		r.expect("08:05 – 08:54".equals(range), "range text '" + range + "'");

		// other locales and forced modes: digits and the right style, not exact strings except German 24 h
		Locale[] locales = {Locale.GERMANY, Locale.FRANCE, Locale.JAPAN, new Locale("ar", "EG"), Locale.UK, Locale.forLanguageTag("hi-IN")};
		List<Map<String, Object>> other = new ArrayList<>();
		for (Locale l : locales) {
			Map<String, Object> row = new LinkedHashMap<>();
			String f24 = new TransportClockFormat(true, l).format(t[0], NY);
			String f12 = new TransportClockFormat(false, l).format(t[3], NY);
			row.put("locale", l.toLanguageTag());
			row.put("24h", f24);
			row.put("12h(23:59)", f12);
			other.add(row);
			r.expect(f24.codePoints().anyMatch(Character::isDigit), l + " 24h text '" + f24 + "'");
			r.expect(!f12.isEmpty() && !f12.equals(f24), l + " 12h text '" + f12 + "'");
		}
		r.put("otherLocales", other);
		r.expect("08:05".equals(new TransportClockFormat(true, Locale.GERMANY).format(t[0], NY)), "de 24h");

		// the device: locale, and the app's own helper when the host has one
		r.put("deviceLocale", env.locale.toLanguageTag());
		TransportClockFormat dev24 = new TransportClockFormat(true, env.locale);
		TransportClockFormat dev12 = new TransportClockFormat(false, env.locale);
		r.put("deviceForced24", dev24.format(t[0], NY));
		r.put("deviceForced12", dev12.format(t[0], NY));
		r.put("deviceForced12Codepoints", codepoints(dev12.format(t[0], NY)));
		r.expect(!dev24.format(t[0], NY).isEmpty() && !dev12.format(t[0], NY).isEmpty(), "empty text in device locale");
		if (env.deviceClock != null) {
			boolean is24 = env.deviceClock.is24Hour();
			r.put("device24hSetting", is24);
			TransportClockFormat mirror = new TransportClockFormat(is24, env.locale);
			List<String> cmp = new ArrayList<>();
			for (long epoch : t) {
				String app = env.deviceClock.format(epoch, NY), mine = mirror.format(epoch, NY);
				cmp.add(app);
				r.expect(app.equals(mine), "app helper '" + app + "' != formatter with the device setting '" + mine + "'");
			}
			r.put("appHelperOutput", cmp);
			if (env.locale.getLanguage().equals("en") && "US".equals(env.locale.getCountry())) {
				String[] want = is24 ? want24 : want12;
				for (int i = 0; i < t.length; i++) {
					r.expect(want[i].equals(plain(cmp.get(i))), "device en-US " + (is24 ? "24h" : "12h") + " expected '" + want[i]
							+ "' got '" + codepoints(cmp.get(i)) + "'");
				}
			}
		} else {
			r.note("no device clock probe (JVM run)");
		}
	}
}
