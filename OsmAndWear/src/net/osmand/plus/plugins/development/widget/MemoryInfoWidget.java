package net.osmand.plus.plugins.development.widget;

import static net.osmand.plus.views.mapwidgets.WidgetType.DEV_MEMORY;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;

public class MemoryInfoWidget extends SimpleWidget {


	private static final long UPDATE_INTERVAL_MS = 1000;

	private long cachedUsedMemory;

	private long lastUpdateTime;
	private boolean memoryChanged;

	public MemoryInfoWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, DEV_MEMORY, customId, widgetsPanel);
		updateSimpleWidgetInfo(null);
		setIcons(DEV_MEMORY);
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		if (checkMemoryChanged() && isTimeToUpdate()) {
			memoryChanged = false;
			lastUpdateTime = System.currentTimeMillis();
			setText(getFormattedValue(), null);
		}
	}

	private boolean checkMemoryChanged() {
		Runtime runtime = Runtime.getRuntime();
		long usedMemory = runtime.totalMemory() - runtime.freeMemory();

		if (cachedUsedMemory != usedMemory) {
			cachedUsedMemory = usedMemory;
			memoryChanged = true;
		}
		return memoryChanged;
	}

	private boolean isTimeToUpdate() {
		return System.currentTimeMillis() - lastUpdateTime > UPDATE_INTERVAL_MS;
	}

	@NonNull
	private String getFormattedValue() {
		return AndroidUtils.formatSize(app, cachedUsedMemory, true);
	}
}
