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
import net.osmand.plus.utils.AndroidUtils.FormattedSize;
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
		this.memoryInfoType = widgetState.getMemoryInfoType();
	}

	@NonNull
	public OsmandPreference<MemoryInfoType> getMemoryInfoTypePref() {
		return widgetState.getMemoryInfoTypePref();
	}

	@Override
	protected void setupView(@NonNull View view) {
		super.setupView(view);
		updateSimpleWidgetInfo(null);
		updateIcons();
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
		return memoryInfoType.getMapLabel(app);
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

		if (!typeChanged && !isTimeToUpdate()) {
			return;
		}
		lastUpdateTime = System.currentTimeMillis();
		long newMemoryValue = calculateMemoryValue(newType);

		if (!typeChanged && cachedMemoryValue == newMemoryValue) {
			return;
		}
		memoryInfoType = newType;
		cachedMemoryValue = newMemoryValue;

		if (typeChanged) {
			updateIcons();
		}
		FormattedSize formattedSize = AndroidUtils.formatSize(cachedMemoryValue, true);
		if (formattedSize != null) {
			setText(formattedSize.num, formattedSize.numSuffix);
		} else {
			setText(NO_VALUE, null);
		}
	}

	private void updateIcons() {
		setIcons(memoryInfoType.getIconId(false), memoryInfoType.getIconId(true));
	}

	private boolean isTimeToUpdate() {
		return System.currentTimeMillis() - lastUpdateTime > UPDATE_INTERVAL_MS;
	}

	private long calculateMemoryValue(@NonNull MemoryInfoType infoType) {
		Runtime runtime = Runtime.getRuntime();

		return switch (infoType) {
			case USED -> runtime.totalMemory() - runtime.freeMemory();
			case ALLOCATED -> runtime.totalMemory();
			case LIMIT -> {
				long maxMemory = runtime.maxMemory();
				yield maxMemory == Long.MAX_VALUE ? runtime.totalMemory() : maxMemory;
			}
			case NATIVE -> Debug.getNativeHeapAllocatedSize();
			case GRAPHICS -> {
				String graphics = getMemoryInfo().getMemoryStat("summary.graphics");
				yield Algorithms.parseLongSilently(graphics, 0) * 1024L;
			}
		};
	}

	@NonNull
	private Debug.MemoryInfo getMemoryInfo() {
		if (memoryInfo == null) {
			memoryInfo = new Debug.MemoryInfo();
		}
		Debug.getMemoryInfo(memoryInfo);
		return memoryInfo;
	}
}
