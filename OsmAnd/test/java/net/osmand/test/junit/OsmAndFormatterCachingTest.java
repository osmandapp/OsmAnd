package net.osmand.test.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.FormattedValue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * {@code OsmAndFormatter.formatValue()} used to build a {@link DecimalFormat} (and with it a
 * {@link DecimalFormatSymbols}, which resolves the locale currency through ICU) on every single
 * call. It is called for every visible row of a list on every compass update, so the allocation
 * churn kept the collector busy and showed up as a main-thread ANR inside
 * {@code art::gc::Heap::WaitForGcToComplete} during {@code ListView.layoutChildren}.
 *
 * <p>These tests pin both halves of the fix: the formatting result must not change, and formatting
 * must no longer pay for building a formatter on every call.
 */
@RunWith(AndroidJUnit4.class)
public class OsmAndFormatterCachingTest {

	private static final int WARMUP_ITERATIONS = 200;
	private static final int MEASURED_ITERATIONS = 3000;

	/** The cached path has to be at least this much cheaper than building a formatter per call. */
	private static final int MIN_SPEEDUP = 5;

	private OsmandApplication app;

	@Before
	public void setup() {
		Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
		app = (OsmandApplication) targetContext.getApplicationContext();
	}

	/**
	 * The cache must not change a single formatted string. The reference implementation below is
	 * the code that stood in {@code formatValue()} before the fix.
	 */
	@Test
	public void formattedValuesAreUnchanged() {
		List<String> mismatches = new ArrayList<>();
		for (float value : values()) {
			for (int decimalPlaces : new int[] {0, 1, 2, 3}) {
				for (boolean forceTrailingZeroes : new boolean[] {false, true}) {
					FormattedValue actual = OsmAndFormatter.formatValue(value, "m",
							forceTrailingZeroes, decimalPlaces, app);
					String expected = formatValueBeforeFix(value, forceTrailingZeroes, decimalPlaces);
					if (!expected.equals(actual.value)) {
						mismatches.add(value + " places=" + decimalPlaces
								+ " trailingZeroes=" + forceTrailingZeroes
								+ ": expected '" + expected + "' but was '" + actual.value + "'");
					}
				}
			}
		}
		assertEquals("formatted values changed", "", String.join("\n", mismatches));
	}

	/** Repeated formatting has to reuse the formatter instead of building a new one every time. */
	@Test
	public void repeatedFormattingDoesNotRebuildTheFormatter() {
		// Warm up both paths so that class loading and ICU's own caches are not being measured.
		measureCached(WARMUP_ITERATIONS);
		measureFormatterPerCall(WARMUP_ITERATIONS);

		long perCallNanos = measureFormatterPerCall(MEASURED_ITERATIONS);
		long cachedNanos = measureCached(MEASURED_ITERATIONS);

		String detail = "building a formatter per call: " + perCallNanos / 1_000_000f + " ms, "
				+ "OsmAndFormatter.formatValue(): " + cachedNanos / 1_000_000f + " ms for "
				+ MEASURED_ITERATIONS + " calls";
		Log.i("OsmAndFormatterCachingTest", detail);
		assertTrue("formatValue() still builds a DecimalFormat on every call - " + detail,
				cachedNanos * MIN_SPEEDUP < perCallNanos);
	}

	private long measureCached(int iterations) {
		long start = System.nanoTime();
		for (int i = 0; i < iterations; i++) {
			OsmAndFormatter.formatValue(i + 0.5f, "m", false, 1, app);
		}
		return System.nanoTime() - start;
	}

	private long measureFormatterPerCall(int iterations) {
		long start = System.nanoTime();
		for (int i = 0; i < iterations; i++) {
			formatValueBeforeFix(i + 0.5f, false, 1);
		}
		return System.nanoTime() - start;
	}

	private static float[] values() {
		return new float[] {
				0f, 0.04f, 0.5f, 1f, 1.25f, 9.999f, 12f, 99.95f, 100f, 999.9f,
				1000f, 9999.99f, 10_000f, 12_345.678f, 999_999f, 1_234_567f,
				-0.5f, -12.34f, -10_000.5f
		};
	}

	/** Verbatim copy of {@code OsmAndFormatter.formatValue()} as it was before the fix. */
	private String formatValueBeforeFix(float value, boolean forceTrailingZeroes,
	                                    int decimalPlacesNumber) {
		String pattern = "0";
		if (decimalPlacesNumber > 0) {
			char fractionDigitPattern = forceTrailingZeroes ? '0' : '#';
			char[] fractionDigitsPattern = new char[decimalPlacesNumber];
			Arrays.fill(fractionDigitsPattern, fractionDigitPattern);
			pattern += "." + String.valueOf(fractionDigitsPattern);
		}

		Locale preferredLocale = app.getLocaleHelper().getPreferredLocale();
		Locale locale = preferredLocale != null ? preferredLocale : Locale.getDefault();

		DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(locale);
		decimalFormatSymbols.setGroupingSeparator(' ');

		DecimalFormat decimalFormat = new DecimalFormat(pattern);
		decimalFormat.setDecimalFormatSymbols(decimalFormatSymbols);

		boolean fiveOrMoreDigits = Math.abs(value) >= 10_000;
		if (fiveOrMoreDigits) {
			decimalFormat.setGroupingUsed(true);
			decimalFormat.setGroupingSize(3);
		}

		MessageFormat messageFormat = new MessageFormat("{0}");
		messageFormat.setFormatByArgumentIndex(0, decimalFormat);
		return messageFormat.format(new Object[] {value}).replace('\n', ' ');
	}
}
