package net.osmand.plus.settings.preferences;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.DialogPreference;

import net.osmand.plus.R;
import net.osmand.plus.settings.vehiclesize.SizeType;
import net.osmand.plus.settings.vehiclesize.VehicleSizes;
import net.osmand.plus.settings.enums.MetricsConstants;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class SizePreference extends DialogPreference {

	private SizeType sizeType;
	private MetricsConstants lengthMetricSystem;
	private VehicleSizes vehicleSizes;
	private String defaultValue;

	public SizePreference(Context context) {
		super(context);
	}

	@NonNull
	public VehicleSizes getVehicleSizes() {
		return vehicleSizes;
	}

	public void setVehicleSizes(VehicleSizes vehicleSizes) {
		this.vehicleSizes = vehicleSizes;
	}

	@NonNull
	public SizeType getSizeType() {
		return sizeType;
	}

	public void setSizeType(@NonNull SizeType sizeType) {
		this.sizeType = sizeType;
	}

	@NonNull
	public MetricsConstants getLengthMetricSystem() {
		return lengthMetricSystem;
	}

	public void setLengthMetricSystem(@NonNull MetricsConstants lengthMetric) {
		this.lengthMetricSystem = lengthMetric;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	@Override
	public CharSequence getSummary() {
		float value = vehicleSizes.readSavedValue(this);
		String none = getString(R.string.shared_string_none);
		if (value == 0.0f) {
			return none;
		}
		try {
			DecimalFormat formatter = new DecimalFormat("#.####", new DecimalFormatSymbols(Locale.US));
			String valueStr = formatter.format(value);
			String pattern = getString(R.string.ltr_or_rtl_combine_via_space);
			String metricShort = getString(vehicleSizes.getMetricShortStringId(sizeType, lengthMetricSystem));
			return String.format(pattern, valueStr, metricShort);
		} catch (NumberFormatException e) {
			return none;
		}
	}

	private String getString(int stringId) {
		return getContext().getString(stringId);
	}

	public String getValue () {
		return getPersistedString(defaultValue);
	}
}
