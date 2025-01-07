package net.osmand.plus.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

import java.text.MessageFormat;

public class FormattedValue {

	public final String value;
	public final String unit;
	public final float valueSrc;
	public final int unitId;

	private final boolean separateWithSpace;

	public FormattedValue(float valueSrc, String value, String unit) {
		this(valueSrc, value, unit, true);
	}

	public FormattedValue(float valueSrc, String value, String unit, boolean separateWithSpace) {
		this(valueSrc, value, unit, -1, separateWithSpace);
	}

	public FormattedValue(float valueSrc, String value, String unit, @StringRes int unitId,
			boolean separateWithSpace) {
		this.value = value;
		this.valueSrc = valueSrc;
		this.unit = unit;
		this.separateWithSpace = separateWithSpace;
		this.unitId = unitId;
	}

	@NonNull
	public String format(@NonNull Context context) {
		return format(context, value, unit, separateWithSpace);
	}

	@NonNull
	public static String format(@NonNull Context context, @NonNull String value,
			@NonNull String unit, boolean separateWithSpace) {
		return separateWithSpace
				? context.getString(R.string.ltr_or_rtl_combine_via_space, value, unit)
				: new MessageFormat("{0}{1}").format(new Object[] {value, unit});
	}
}
