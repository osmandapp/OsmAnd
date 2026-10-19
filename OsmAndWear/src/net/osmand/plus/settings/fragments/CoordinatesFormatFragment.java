package net.osmand.plus.settings.fragments;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;

import net.osmand.Location;
import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.bottomsheets.ChangeGeneralProfilesPrefBottomSheet;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.widgets.style.CustomClickableSpan;
import net.osmand.plus.widgets.tools.ClickableSpanTouchListener;


public class CoordinatesFormatFragment extends BaseSettingsFragment {

	public static final String TAG = CoordinatesFormatFragment.class.getSimpleName();

	private static final String FORMAT_DEGREES = "format_degrees";
	private static final String FORMAT_MINUTES = "format_minutes";
	private static final String FORMAT_SECONDS = "format_seconds";
	private static final String UTM_FORMAT = "utm_format";
	private static final String MGRS_FORMAT = "mgrs_format";
	private static final String OLC_FORMAT = "olc_format";
	private static final String SWISS_GRID_FORMAT = "swiss_grid_format";
	private static final String SWISS_GRID_PLUS_FORMAT = "swiss_grid_plus_format";

	@Override
	protected void setupPreferences() {
		CheckBoxPreference degreesPref = findPreference(FORMAT_DEGREES);
		CheckBoxPreference minutesPref = findPreference(FORMAT_MINUTES);
		CheckBoxPreference secondsPref = findPreference(FORMAT_SECONDS);
		CheckBoxPreference utmPref = findPreference(UTM_FORMAT);
		CheckBoxPreference mgrsPref = findPreference(MGRS_FORMAT);
		CheckBoxPreference olcPref = findPreference(OLC_FORMAT);
		CheckBoxPreference swissGridPref = findPreference(SWISS_GRID_FORMAT);
		CheckBoxPreference swissGridPlusPref = findPreference(SWISS_GRID_PLUS_FORMAT);

		Location loc = app.getLocationProvider().getLastKnownLocation();

		degreesPref.setSummary(getCoordinatesFormatSummary(loc, PointDescription.FORMAT_DEGREES));
		minutesPref.setSummary(getCoordinatesFormatSummary(loc, PointDescription.FORMAT_MINUTES));
		secondsPref.setSummary(getCoordinatesFormatSummary(loc, PointDescription.FORMAT_SECONDS));
		utmPref.setSummary(getCoordinatesFormatSummary(loc, PointDescription.UTM_FORMAT));
		mgrsPref.setSummary(getCoordinatesFormatSummary(loc, PointDescription.MGRS_FORMAT));
		olcPref.setSummary(getCoordinatesFormatSummary(loc, PointDescription.OLC_FORMAT));
		swissGridPref.setSummary(getCoordinatesFormatSummary(loc, PointDescription.SWISS_GRID_FORMAT));
		swissGridPlusPref.setSummary(getCoordinatesFormatSummary(loc, PointDescription.SWISS_GRID_PLUS_FORMAT));

		int currentFormat = settings.COORDINATES_FORMAT.getModeValue(getSelectedAppMode());
		String currentPrefKey = getCoordinatesKeyForFormat(currentFormat);
		updateSelectedFormatPrefs(currentPrefKey);
	}

	@Override
	protected void onBindPreferenceViewHolder(@NonNull Preference preference, @NonNull PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);

		if (UTM_FORMAT.equals(preference.getKey())) {
			TextView summaryView = (TextView) holder.findViewById(android.R.id.summary);
			if (summaryView != null) {
				summaryView.setOnTouchListener(new ClickableSpanTouchListener());
			}
		}
		if (MGRS_FORMAT.equals(preference.getKey())) {
			TextView summaryView = (TextView) holder.findViewById(android.R.id.summary);
			if (summaryView != null) {
				summaryView.setOnTouchListener(new ClickableSpanTouchListener());
			}
		}
	}

	private CharSequence getCoordinatesFormatSummary(Location loc, int format) {
		double lat = loc != null ? loc.getLatitude() : 49.41869;
		double lon = loc != null ? loc.getLongitude() : 8.67339;

		String formattedCoordinates = OsmAndFormatter.getFormattedCoordinates(lat, lon, format);
		if (format == PointDescription.UTM_FORMAT) {
			SpannableStringBuilder spannableBuilder = new SpannableStringBuilder();
			String combined = getString(R.string.ltr_or_rtl_combine_via_colon, getString(R.string.shared_string_example), formattedCoordinates);
			spannableBuilder.append(combined);
			spannableBuilder.append("\n");
			spannableBuilder.append(getString(R.string.utm_format_descr));

			int start = spannableBuilder.length();
			spannableBuilder.append(" ");
			spannableBuilder.append(getString(R.string.shared_string_read_more));

			ClickableSpan clickableSpan = new CustomClickableSpan() {
				@Override
				public void onClick(@NonNull View widget) {
					Context ctx = getContext();
					if (ctx != null) {
						AndroidUtils.openUrl(ctx, R.string.url_wikipedia_utm_format, isNightMode());
					}
				}
			};
			spannableBuilder.setSpan(clickableSpan, start, spannableBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

			return spannableBuilder;
		}
		if (format == PointDescription.MGRS_FORMAT) {
			SpannableStringBuilder spannableBuilder = new SpannableStringBuilder();
			String combined = getString(R.string.ltr_or_rtl_combine_via_colon, getString(R.string.shared_string_example), formattedCoordinates);
			spannableBuilder.append(combined);
			spannableBuilder.append("\n");
			spannableBuilder.append(getString(R.string.mgrs_format_descr));

			int start = spannableBuilder.length();
			spannableBuilder.append(" ");
			spannableBuilder.append(getString(R.string.shared_string_read_more));

			ClickableSpan clickableSpan = new CustomClickableSpan() {
				@Override
				public void onClick(@NonNull View widget) {
					Context ctx = getContext();
					if (ctx != null) {
						AndroidUtils.openUrl(ctx, R.string.url_wikipedia_mgrs_format, isNightMode());
					}
				}
			};
			spannableBuilder.setSpan(clickableSpan, start, spannableBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

			return spannableBuilder;
		}
		return  formattedCoordinates;
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String key = preference.getKey();

		int newFormat = getCoordinatesFormatForKey(key);
		if (newFormat != -1) {
			ApplicationMode selectedAppMode = getSelectedAppMode();
			if (!settings.COORDINATES_FORMAT.getModeValue(selectedAppMode).equals(newFormat)) {
				updateSelectedFormatPrefs(key);

				FragmentManager fragmentManager = getFragmentManager();
				if (fragmentManager != null) {
					ChangeGeneralProfilesPrefBottomSheet.showInstance(fragmentManager,
							settings.COORDINATES_FORMAT.getId(), newFormat, this, false, getSelectedAppMode());
				}
			}
		}

		return false;
	}

	@Override
	public boolean shouldDismissOnChange() {
		return true;
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
			case MGRS_FORMAT:
				return PointDescription.MGRS_FORMAT;
			case OLC_FORMAT:
				return PointDescription.OLC_FORMAT;
			case SWISS_GRID_FORMAT:
				return PointDescription.SWISS_GRID_FORMAT;
			case SWISS_GRID_PLUS_FORMAT:
				return PointDescription.SWISS_GRID_PLUS_FORMAT;
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
			case PointDescription.MGRS_FORMAT:
				return MGRS_FORMAT;
			case PointDescription.OLC_FORMAT:
				return OLC_FORMAT;
			case PointDescription.SWISS_GRID_FORMAT:
				return SWISS_GRID_FORMAT;
			case PointDescription.SWISS_GRID_PLUS_FORMAT:
				return SWISS_GRID_PLUS_FORMAT;
			default:
				return "Unknown format";
		}
	}
}