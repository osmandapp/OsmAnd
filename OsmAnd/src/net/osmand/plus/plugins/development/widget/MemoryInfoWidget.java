package net.osmand.plus.plugins.development.widget;

import static net.osmand.plus.views.mapwidgets.WidgetType.DEV_MEMORY;

import android.os.Debug;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;

import org.apache.commons.logging.Log;

public class MemoryInfoWidget extends TextInfoWidget {

	private static final Log log = PlatformUtil.getLog(MemoryInfoWidget.class);

	private static final int UPDATE_INTERVAL_MS = 1000;

	private long cachedDalvikSize;
	private long cachedTotalMemory;
	private long cachedAvailableMemory;

	private long lastUpdateTime;
	private boolean memoryChanged;

	public MemoryInfoWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity, DEV_MEMORY);
		updateInfo(null);
		setIcons(DEV_MEMORY);
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		if (checkMemoryChanged() && isTimeToUpdate(UPDATE_INTERVAL_MS)) {
			memoryChanged = false;
			lastUpdateTime = System.currentTimeMillis();
			setText(getFormattedValue(), null);
		}
	}

	private boolean checkMemoryChanged() {
		Runtime runtime = Runtime.getRuntime();

		long dalvikSize = Debug.getNativeHeapAllocatedSize(); // not used currently
		long totalMemory = runtime.totalMemory();
		long availableMemory = totalMemory - runtime.freeMemory();

		if (this.cachedDalvikSize != dalvikSize
				|| this.cachedTotalMemory != totalMemory
				|| this.cachedAvailableMemory != availableMemory) {
			this.cachedDalvikSize = dalvikSize;
			this.cachedTotalMemory = totalMemory;
			this.cachedAvailableMemory = availableMemory;
			memoryChanged = true;
		}
		return memoryChanged;
	}

	private boolean isTimeToUpdate(long interval) {
		return System.currentTimeMillis() - lastUpdateTime > interval;
	}

	private String getFormattedValue() {
		return AndroidUtils.formatRatioOfSizes(app, cachedAvailableMemory, cachedTotalMemory);
	}

}
