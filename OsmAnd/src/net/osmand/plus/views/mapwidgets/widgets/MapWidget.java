package net.osmand.plus.views.mapwidgets.widgets;

import android.view.View;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class MapWidget {

	protected final OsmandApplication app;
	protected final MapActivity mapActivity;
	protected final UiUtilities iconsCache;

	private boolean nightMode;

	protected final View view;

	public MapWidget(@NonNull MapActivity mapActivity) {
		this.app = mapActivity.getMyApplication();
		this.mapActivity = mapActivity;
		this.iconsCache = app.getUIUtilities();
		this.nightMode = app.getDaynightHelper().isNightMode();
		this.view = UiUtilities.getInflater(mapActivity, nightMode).inflate(getLayoutId(), null);
	}

	@LayoutRes
	protected abstract int getLayoutId();

	@NonNull
	public View getView() {
		return view;
	}

	public void setNightMode(boolean nightMode) {
		this.nightMode = nightMode;
	}

	public boolean isNightMode() {
		return nightMode;
	}

	public void updateInfo(@Nullable DrawSettings drawSettings) {
		// Not implemented
	}

	protected boolean updateVisibility(boolean visible) {
		return AndroidUiHelper.updateVisibility(view, visible);
	}
}