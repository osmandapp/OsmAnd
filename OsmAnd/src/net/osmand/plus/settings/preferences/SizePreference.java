package net.osmand.plus.settings.preferences;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.DialogPreference;

import net.osmand.plus.R;
import net.osmand.plus.settings.vehiclesize.SizeType;
import net.osmand.plus.settings.vehiclesize.VehicleSizes;
import net.osmand.plus.settings.vehiclesize.containers.Metric;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class SizePreference extends DialogPreference {

	private SizeType sizeType;
	private Metric metric;
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

	public void setMetric(@NonNull Metric metric) {
		this.metric = metric;
	}

	@NonNull
	public Metric getMetric() {
		return metric;
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
			String pattern = getString(R.string.ltr_or_rtl_combine_via_space);
			String valueStr = formatter.format(value);
			String metricStr = getString(vehicleSizes.getMetricShortStringId(sizeType, metric));
			return String.format(pattern, valueStr, metricStr);
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
