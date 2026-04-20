package net.osmand.plus.settings.preferences;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.DialogPreference;

import net.osmand.plus.R;
import net.osmand.shared.vehicle.specification.data.VehicleValueConverter;
import net.osmand.shared.vehicle.specification.domain.SpecificationType;
import net.osmand.shared.vehicle.specification.domain.profiles.VehicleSpecs;
import net.osmand.shared.units.MeasurementUnit;
import net.osmand.shared.util.SharedNumberFormatter;

public class VehicleSpecificationPreference extends DialogPreference {

	private SpecificationType specificationType;
	private VehicleSpecs specifications;
	private boolean isMetric;
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

	public void setMetric(boolean metric) {
		this.isMetric = metric;
	}

	public boolean isMetric() {
		return isMetric;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	@Override
	public CharSequence getSummary() {
		MeasurementUnit<?> units = specifications.getMeasurementUnits(specificationType, isMetric);
		double value = VehicleValueConverter.readSavedValue(getValue(), units);
		String none = getString(R.string.shared_string_none);
		if (value == 0.0) {
			return none;
		}
		try {
			String pattern = getString(R.string.ltr_or_rtl_combine_via_space);
			String valueStr = SharedNumberFormatter.formatDecimal(value, 1);
			String symbolStr = units.getSymbol();
			return String.format(pattern, valueStr, symbolStr);
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
