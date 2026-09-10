package net.osmand.plus.plugins.panoramax;

import static net.osmand.plus.views.mapwidgets.WidgetType.PANORAMAX;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;

public class PanoramaxMapWidget extends SimpleWidget {

	public PanoramaxMapWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, PANORAMAX, customId, widgetsPanel);
	}

	@Override
	protected void setupView(@NonNull View view) {
		super.setupView(view);
		setText(app.getString(R.string.panoramax), "");
		setIcons(PANORAMAX);
	}

	@Override
	protected View.OnClickListener getOnClickListener() {
		return v -> PanoramaxPlugin.openPanoramax(mapActivity, null);
	}
}
