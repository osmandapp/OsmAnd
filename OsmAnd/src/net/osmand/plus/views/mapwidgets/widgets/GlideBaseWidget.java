package net.osmand.plus.views.mapwidgets.widgets;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.mapwidgets.WidgetType;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

public abstract class GlideBaseWidget extends TextInfoWidget {

	private static final String FORMAT_PATTERN = "#.#";

	private static final int UPDATE_INTERVAL_MILLIS = 1000;

	protected long lastUpdateTime;

	public GlideBaseWidget(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType) {
		super(mapActivity, widgetType);
		setIcons(widgetType);
	}

	protected boolean isTimeToUpdate() {
		return System.currentTimeMillis() - lastUpdateTime > UPDATE_INTERVAL_MILLIS;
	}

	@NonNull
	public String format(float value) {
		DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.US);
		NumberFormat formatter = new DecimalFormat(FORMAT_PATTERN, dfs);
		String formattedValue = formatter.format(value);
		return app.getString(R.string.ltr_or_rtl_combine_via_colon_with_space, formattedValue, "1");
	}
}
