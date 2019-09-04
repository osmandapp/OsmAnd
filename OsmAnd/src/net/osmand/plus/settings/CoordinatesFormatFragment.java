package net.osmand.plus.settings;

import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.Location;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;


public class CoordinatesFormatFragment extends BaseSettingsFragment {

	public static final String TAG = "CoordinatesFormatFragment";

	private static final String FORMAT_DEGREES = "format_degrees";
	private static final String FORMAT_MINUTES = "format_minutes";
	private static final String FORMAT_SECONDS = "format_seconds";
	private static final String UTM_FORMAT = "utm_format";
	private static final String OLC_FORMAT = "olc_format";

	@Override
	protected int getPreferencesResId() {
		return R.xml.coordinates_format;
	}

	@Override
	protected int getToolbarResId() {
		return R.layout.profile_preference_toolbar;
	}

	@Override
	protected String getToolbarTitle() {
		return getString(R.string.coordinates_format);
	}

	@Override
	protected void setupPreferences() {
		PreferenceScreen screen = getPreferenceScreen();
		screen.setOrderingAsAdded(false);

		Preference generalSettings = findPreference("coordinates_format_info");
		generalSettings.setIcon(getContentIcon(R.drawable.ic_action_info_dark));

		CheckBoxPreference degreesPref = (CheckBoxPreference) findPreference(FORMAT_DEGREES);
		CheckBoxPreference minutesPref = (CheckBoxPreference) findPreference(FORMAT_MINUTES);
		CheckBoxPreference secondsPref = (CheckBoxPreference) findPreference(FORMAT_SECONDS);
		CheckBoxPreference olcPref = (CheckBoxPreference) findPreference(OLC_FORMAT);

		CheckBoxPreference utmPref = createUtmFormatPref();
		screen.addPreference(utmPref);

		Location loc = app.getLocationProvider().getLastKnownLocation();

		degreesPref.setSummary(getCoordinatesFormatSummary(loc, PointDescription.FORMAT_DEGREES));
		minutesPref.setSummary(getCoordinatesFormatSummary(loc, PointDescription.FORMAT_MINUTES));
		secondsPref.setSummary(getCoordinatesFormatSummary(loc, PointDescription.FORMAT_SECONDS));
		utmPref.setSummary(getCoordinatesFormatSummary(loc, PointDescription.UTM_FORMAT));
		olcPref.setSummary(getCoordinatesFormatSummary(loc, PointDescription.OLC_FORMAT));

		int currentFormat = settings.COORDINATES_FORMAT.get();
		String currentPrefKey = getCoordinatesKeyForFormat(currentFormat);
		updateSelectedFormatPrefs(currentPrefKey);
	}

	private CheckBoxPreference createUtmFormatPref() {
		CheckBoxPreference utmPref = new CheckBoxPreference(app) {

			@Override
			public void onBindViewHolder(PreferenceViewHolder holder) {
				super.onBindViewHolder(holder);
				TextView summaryView = (TextView) holder.findViewById(android.R.id.summary);
				if (summaryView != null) {
					summaryView.setOnTouchListener(getSummaryTouchListener());
				}
			}
		};
		utmPref.setKey(UTM_FORMAT);
		utmPref.setTitle(R.string.navigate_point_format_utm);
		utmPref.setPersistent(false);
		utmPref.setOrder(4);
		utmPref.setLayoutResource(R.layout.preference_radio_button);
		return utmPref;
	}

	private View.OnTouchListener getSummaryTouchListener() {
		return new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int action = event.getAction();

				if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
					TextView widget = (TextView) v;

					int x = (int) event.getX();
					int y = (int) event.getY();

					x -= widget.getTotalPaddingLeft();
					y -= widget.getTotalPaddingTop();

					x += widget.getScrollX();
					y += widget.getScrollY();

					Layout layout = widget.getLayout();
					int line = layout.getLineForVertical(y);
					int off = layout.getOffsetForHorizontal(line, x);

					Spannable spannable = new SpannableString(widget.getText());
					ClickableSpan[] links = spannable.getSpans(off, off, ClickableSpan.class);

					if (links.length != 0) {
						if (action == MotionEvent.ACTION_UP) {
							links[0].onClick(widget);
						} else {
							Selection.setSelection(spannable, spannable.getSpanStart(links[0]), spannable.getSpanEnd(links[0]));
						}
						return true;
					} else {
						Selection.removeSelection(spannable);
					}
				}

				return false;
			}
		};
	}

	private CharSequence getCoordinatesFormatSummary(Location loc, int format) {
		double lat = loc != null ? loc.getLatitude() : 49.41869;
		double lon = loc != null ? loc.getLongitude() : 8.67339;

		String formattedCoordinates = OsmAndFormatter.getFormattedCoordinates(lat, lon, format);
		if (format == PointDescription.UTM_FORMAT) {
			SpannableStringBuilder spannableBuilder = new SpannableStringBuilder();
			spannableBuilder.append(getString(R.string.shared_string_example));
			spannableBuilder.append(": ");
			spannableBuilder.append(formattedCoordinates);
			spannableBuilder.append("\n");
			spannableBuilder.append(getString(R.string.utm_format_descr));

			int start = spannableBuilder.length();
			spannableBuilder.append(" ");
			spannableBuilder.append(getString(R.string.shared_string_read_more));

			final int color = ContextCompat.getColor(app, isNightMode() ? R.color.active_color_primary_dark : R.color.active_color_primary_light);

			ClickableSpan clickableSpan = new ClickableSpan() {
				@Override
				public void onClick(@NonNull View widget) {
					Toast.makeText(widget.getContext(), getString(R.string.shared_string_read_more), Toast.LENGTH_LONG).show();
				}

				@Override
				public void updateDrawState(@NonNull TextPaint ds) {
					ds.setColor(color);
					ds.setUnderlineText(false);
				}
			};
			spannableBuilder.setSpan(clickableSpan, start, spannableBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

			return spannableBuilder;
		}
		return getString(R.string.shared_string_example) + ": " + formattedCoordinates;
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String key = preference.getKey();

		updateSelectedFormatPrefs(key);

		int newFormat = getCoordinatesFormatForKey(key);
		if (newFormat != -1) {
			settings.COORDINATES_FORMAT.set(newFormat);
			return true;
		}

		return super.onPreferenceClick(preference);
	}

	private void updateSelectedFormatPrefs(String key) {
		PreferenceScreen screen = getPreferenceScreen();
		if (screen != null) {
			for (int i = 0; i < screen.getPreferenceCount(); i++) {
				Preference pref = screen.getPreference(i);
				if (pref instanceof CheckBoxPreference) {
					CheckBoxPreference checkBoxPreference = ((CheckBoxPreference) pref);
					boolean checked = checkBoxPreference.getKey().equals(key);
					checkBoxPreference.setChecked(checked);
				}
			}
		}
	}

	private int getCoordinatesFormatForKey(String key) {
		switch (key) {
			case FORMAT_DEGREES:
				return PointDescription.FORMAT_DEGREES;
			case FORMAT_MINUTES:
				return PointDescription.FORMAT_MINUTES;
			case FORMAT_SECONDS:
				return PointDescription.FORMAT_SECONDS;
			case UTM_FORMAT:
				return PointDescription.UTM_FORMAT;
			case OLC_FORMAT:
				return PointDescription.OLC_FORMAT;
			default:
				return -1;
		}
	}

	private String getCoordinatesKeyForFormat(int format) {
		switch (format) {
			case PointDescription.FORMAT_DEGREES:
				return FORMAT_DEGREES;
			case PointDescription.FORMAT_MINUTES:
				return FORMAT_MINUTES;
			case PointDescription.FORMAT_SECONDS:
				return FORMAT_SECONDS;
			case PointDescription.UTM_FORMAT:
				return UTM_FORMAT;
			case PointDescription.OLC_FORMAT:
				return OLC_FORMAT;
			default:
				return "Unknown format";
		}
	}
}