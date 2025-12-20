package net.osmand.plus.settings.preferences;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.DialogPreference;

import net.osmand.plus.R;
import net.osmand.plus.settings.enums.MeasurementUnits;
import net.osmand.plus.settings.vehiclespecs.SpecificationType;
import net.osmand.plus.settings.vehiclespecs.profiles.VehicleSpecs;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class VehicleSpecificationPreference extends DialogPreference {

	private SpecificationType specificationType;
	private VehicleSpecs specifications;
	private boolean useMetricSystem;
	private String defaultValue;

	public VehicleSpecificationPreference(Context context) {
		super(context);
	}

	@NonNull
	public VehicleSpecs getSpecifications() {
		return specifications;
	}

	public void setSpecifications(@NonNull VehicleSpecs vehicleSpecs) {
		this.specifications = vehicleSpecs;
	}

	@NonNull
	public SpecificationType getSpecificationType() {
		return specificationType;
	}

	public void setSpecificationType(@NonNull SpecificationType specificationType) {
		this.specificationType = specificationType;
	}

	public void setUseMetricSystem(boolean useMetricSystem) {
		this.useMetricSystem = useMetricSystem;
	}

	public boolean isUseMetricSystem() {
		return useMetricSystem;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	@Override
	public CharSequence getSummary() {
		float value = specifications.readSavedValue(this);
		String none = getString(R.string.shared_string_none);
		if (value == 0.0f) {
			return none;
		}
		try {
			MeasurementUnits units = specifications.getMeasurementUnits(specificationType, useMetricSystem);
			DecimalFormat formatter = new DecimalFormat("#.####", new DecimalFormatSymbols(Locale.US));
			String pattern = getString(R.string.ltr_or_rtl_combine_via_space);
			String valueStr = formatter.format(value);
			String metricStr = getString(units.getSymbolResId());
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
