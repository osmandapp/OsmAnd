package net.osmand.plus.plugins.development.widget;

import static net.osmand.plus.views.mapwidgets.WidgetType.DEV_MEMORY;

import android.os.Debug;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;

import org.apache.commons.logging.Log;

public class MemoryInfoWidget extends TextInfoWidget {

	private static final Log log = PlatformUtil.getLog(MemoryInfoWidget.class);

	private static final long BYTES_TO_MB = 1024 * 1024;

	private long dalvikSize;
	private long totalMemory;
	private long availableMemory;

	public MemoryInfoWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity, DEV_MEMORY);
		updateInfo(null);
		setIcons(DEV_MEMORY);
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		Runtime runtime = Runtime.getRuntime();

		long dalvikSize = Debug.getNativeHeapAllocatedSize() / BYTES_TO_MB;
		long totalMemory = runtime.totalMemory() / BYTES_TO_MB;
		long availableMemory = totalMemory - runtime.freeMemory() / BYTES_TO_MB;

		if (this.dalvikSize != dalvikSize || this.totalMemory != totalMemory || this.availableMemory != availableMemory) {
			this.dalvikSize = dalvikSize;
			this.totalMemory = totalMemory;
			this.availableMemory = availableMemory;

			setText(availableMemory + "-" + totalMemory, String.valueOf(dalvikSize));
		}
	}
}
