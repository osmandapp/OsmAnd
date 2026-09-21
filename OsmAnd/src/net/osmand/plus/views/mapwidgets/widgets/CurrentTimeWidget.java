package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.CURRENT_TIME;

import android.util.Pair;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;

public class CurrentTimeWidget extends SimpleWidget {

	private static final long UPDATE_INTERVAL_MILLIS = 5000;

	private long cachedTime;

	public CurrentTimeWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, CURRENT_TIME, customId, widgetsPanel);
	}

	public CurrentTimeWidget(@NonNull OsmandApplication app, @Nullable String customId, @Nullable WidgetsPanel panel) {
		super(app, CURRENT_TIME, customId, panel);
	}

	@Override
	protected void setupView(@NonNull View view) {
		super.setupView(view);
		setIcons(CURRENT_TIME);
		setText(null, null);
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		updateTimeText();
	}

	@Override
	protected void updateSimpleWidgetInfoForAndroidAuto(@Nullable DrawSettings drawSettings) {
		super.updateSimpleWidgetInfoForAndroidAuto(drawSettings);
		updateTimeText();
	}

	private void updateTimeText() {
		long time = System.currentTimeMillis();
		if (isUpdateNeeded() || time - cachedTime > UPDATE_INTERVAL_MILLIS) {
			cachedTime = time;
			setTimeText(time);
		}
	}

	// region android auto

	@Override
	public void initAndroidAuto() {
		super.initAndroidAuto();
		setIcons(widgetType);
	}

	// endregion
}