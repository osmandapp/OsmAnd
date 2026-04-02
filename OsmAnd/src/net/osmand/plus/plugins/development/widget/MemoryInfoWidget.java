package net.osmand.plus.plugins.development.widget;

import static net.osmand.plus.views.mapwidgets.WidgetType.DEV_MEMORY;

import android.os.Debug;
import android.os.Debug.MemoryInfo;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;
import net.osmand.plus.views.mapwidgets.widgetstates.MemoryWidgetState;
import net.osmand.plus.views.mapwidgets.widgetstates.MemoryWidgetState.MemoryInfoType;
import net.osmand.util.Algorithms;

public class MemoryInfoWidget extends SimpleWidget {

	private static final long UPDATE_INTERVAL_MS = 1000;

	private final MemoryWidgetState widgetState;

	private MemoryInfoType memoryInfoType;

	private MemoryInfo memoryInfo;
	private long lastUpdateTime;
	private long cachedMemoryValue;

	public MemoryInfoWidget(@NonNull MapActivity mapActivity, @NonNull MemoryWidgetState widgetState,
			@Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, DEV_MEMORY, customId, widgetsPanel);
		this.widgetState = widgetState;
	}

	@NonNull
	public OsmandPreference<MemoryInfoType> getMemoryInfoTypePref() {
		return widgetState.getMemoryInfoTypePref();
	}

	@Override
	protected void setupView(@NonNull View view) {
		super.setupView(view);
		updateSimpleWidgetInfo(null);
		setIcons(DEV_MEMORY);
	}

	@Override
	protected OnClickListener getOnClickListener() {
		return v -> {
			widgetState.changeToNextState();
			updateSimpleWidgetInfo(null);
			updateWidgetName();
		};
	}

	@Nullable
	@Override
	protected String getWidgetName() {
		return widgetState != null ? app.getString(widgetState.getMemoryInfoType().titleId) : null;
	}

	@Nullable
	@Override
	public MemoryWidgetState getWidgetState() {
		return widgetState;
	}

	@Override
	public void copySettingsFromMode(@NonNull ApplicationMode sourceAppMode, @NonNull ApplicationMode appMode, @Nullable String customId) {
		super.copySettingsFromMode(sourceAppMode, appMode, customId);
		widgetState.copyPrefsFromMode(sourceAppMode, appMode, customId);
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		MemoryInfoType newType = widgetState.getMemoryInfoType();
		boolean typeChanged = newType != memoryInfoType;

		if (isTimeToUpdate() || typeChanged) {
			lastUpdateTime = System.currentTimeMillis();

			long newMemoryValue = calculateMemoryValue(newType);
			if (typeChanged || cachedMemoryValue != newMemoryValue) {
				memoryInfoType = newType;
				cachedMemoryValue = newMemoryValue;

				String text = AndroidUtils.formatSize(app, cachedMemoryValue, true);
				setText(text, memoryInfoType.shortName);
			}
		}
	}

	private boolean isTimeToUpdate() {
		return System.currentTimeMillis() - lastUpdateTime > UPDATE_INTERVAL_MS;
	}

	private long calculateMemoryValue(@NonNull MemoryInfoType infoType) {
		Runtime runtime = Runtime.getRuntime();
		long maxMemory = runtime.maxMemory();
		long freeMemory = runtime.freeMemory();
		long totalMemory = runtime.totalMemory();
		long usedMemory = totalMemory - freeMemory;
		long effectiveMax = (maxMemory == Long.MAX_VALUE) ? totalMemory : maxMemory;

		switch (infoType) {
			case LIMIT:
				return effectiveMax;
			case ALLOCATED:
				return totalMemory;
			case AVAILABLE:
				return Math.max(0L, effectiveMax - usedMemory);
			case NATIVE:
				return Debug.getNativeHeapAllocatedSize();
			case GRAPHICS:
				if (memoryInfo == null) {
					memoryInfo = new Debug.MemoryInfo();
				}
				Debug.getMemoryInfo(memoryInfo);
				String graphics = memoryInfo.getMemoryStat("summary.graphics");
				return Algorithms.parseLongSilently(graphics, 0) * 1024L;
			case USED:
			default:
				return usedMemory;
		}
	}
}
