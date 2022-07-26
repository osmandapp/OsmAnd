package net.osmand.plus.views.controls.maphudbuttons;

import android.graphics.drawable.Drawable;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.WidgetsVisibilityHelper;
import net.osmand.util.Algorithms;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;

import static net.osmand.plus.views.layers.base.OsmandMapLayer.setMapButtonIcon;

public abstract class MapButton {

	@ColorRes
	private static final int DEFAULT_DAY_ICON_COLOR_ID = R.color.map_button_icon_color_light;
	@ColorRes
	private static final int DEFAULT_NIGHT_ICON_COLOR_ID = R.color.map_button_icon_color_dark;

	protected final OsmandApplication app;
	protected final OsmandSettings settings;
	protected final MapActivity mapActivity;
	protected final UiUtilities iconsCache;
	protected final WidgetsVisibilityHelper widgetsVisibilityHelper;

	public final ImageView view;
	public final String id;

	@DrawableRes
	private int dayIconId;
	@DrawableRes
	private int nightIconId;

	@DrawableRes
	private int dayBackgroundId;
	@DrawableRes
	private int nightBackgroundId;

	@ColorRes
	private int dayIconColorId = DEFAULT_DAY_ICON_COLOR_ID;
	@ColorRes
	private int nightIconColorId = DEFAULT_NIGHT_ICON_COLOR_ID;

	@ColorInt
	private Integer iconColor;

	private boolean nightMode;
	private boolean forceUpdate = true;

	private boolean routeDialogOpened;
	private boolean showBottomButtons;

	public MapButton(@NonNull MapActivity mapActivity, @NonNull ImageView view, @NonNull String id) {
		this.app = mapActivity.getMyApplication();
		this.settings = app.getSettings();
		this.mapActivity = mapActivity;
		this.iconsCache = app.getUIUtilities();
		this.widgetsVisibilityHelper = mapActivity.getWidgetsVisibilityHelper();
		this.view = view;
		this.id = id;
	}

	protected void setRoundTransparentBackground() {
		setBackground(R.drawable.btn_circle_trans, R.drawable.btn_circle_night);
	}

	protected void setBackground(@DrawableRes int backgroundId) {
		if (dayBackgroundId != backgroundId || nightBackgroundId != backgroundId) {
			dayBackgroundId = backgroundId;
			nightBackgroundId = backgroundId;
			forceUpdate = true;
		}
	}

	protected void setBackground(@DrawableRes int dayBackgroundId, @DrawableRes int nightBackgroundId) {
		if (this.dayBackgroundId != dayBackgroundId || this.nightBackgroundId != nightBackgroundId) {
			this.dayBackgroundId = dayBackgroundId;
			this.nightBackgroundId = nightBackgroundId;
			this.forceUpdate = true;
		}
	}

	protected void setIconId(@DrawableRes int iconId) {
		setIconId(iconId, iconId);
	}

	protected void setIconId(@DrawableRes int dayIconId, @DrawableRes int nightIconId) {
		if (this.dayIconId != dayIconId || this.nightIconId != nightIconId) {
			this.dayIconId = dayIconId;
			this.nightIconId = nightIconId;
			this.forceUpdate = true;
		}
	}

	protected void resetIconColors() {
		if (dayIconColorId != DEFAULT_DAY_ICON_COLOR_ID || nightIconColorId != DEFAULT_NIGHT_ICON_COLOR_ID || iconColor != null) {
			dayIconColorId = DEFAULT_DAY_ICON_COLOR_ID;
			nightIconColorId = DEFAULT_NIGHT_ICON_COLOR_ID;
			iconColor = null;
			forceUpdate = true;
		}
	}

	protected void setIconColorId(@ColorRes int iconColorId) {
		setIconColorId(iconColorId, iconColorId);
	}

	protected void setIconColorId(@ColorRes int dayIconColorId, @ColorRes int nightIconColorId) {
		if (this.dayIconColorId != dayIconColorId || this.nightIconColorId != nightIconColorId) {
			this.dayIconColorId = dayIconColorId;
			this.nightIconColorId = nightIconColorId;
			this.forceUpdate = true;
		}
	}

	protected void setIconColor(@ColorInt int iconColor) {
		if (!Algorithms.objectEquals(this.iconColor, iconColor)) {
			this.iconColor = iconColor;
			this.forceUpdate = true;
		}
	}

	public void update(boolean nightMode, boolean routeDialogOpened, boolean bottomButtonsAllowed) {
		this.routeDialogOpened = routeDialogOpened;
		this.showBottomButtons = bottomButtonsAllowed;

		boolean show = shouldShow();
		updateVisibility(show);

		if (!show) {
			return;
		}

		updateState(nightMode);

		if (this.nightMode == nightMode && !forceUpdate) {
			return;
		}
		this.forceUpdate = false;
		this.nightMode = nightMode;

		if (nightBackgroundId != 0 && dayBackgroundId != 0) {
			view.setBackground(AppCompatResources.getDrawable(app, nightMode ? nightBackgroundId : dayBackgroundId));
		}

		Drawable drawable = null;
		int iconId = nightMode ? nightIconId : dayIconId;
		if (iconId != 0) {
			if (iconColor != null) {
				drawable = iconsCache.getPaintedIcon(iconId, iconColor);
			} else {
				drawable = iconsCache.getIcon(iconId, nightMode ? nightIconColorId : dayIconColorId);
			}
		}

		if (drawable != null) {
			setDrawable(drawable);
		}
	}

	protected abstract boolean shouldShow();

	protected void updateState(boolean nightMode) {
		// Not implemented
	}

	protected void setDrawable(@NonNull Drawable drawable) {
		setMapButtonIcon(view, drawable);
	}

	protected boolean updateVisibility(boolean visible) {
		if (visible) {
			visible = app.getAppCustomization().isFeatureEnabled(id);
		}
		return AndroidUiHelper.updateVisibility(view, visible);
	}

	protected boolean isNightMode() {
		return nightMode;
	}

	protected boolean isRouteDialogOpened() {
		return routeDialogOpened;
	}

	protected boolean isShowBottomButtons() {
		return showBottomButtons;
	}

	protected void setOnClickListener(@Nullable OnClickListener listener) {
		view.setOnClickListener(listener);
	}

	protected void setOnLongClickListener(@Nullable OnLongClickListener listener) {
		view.setOnLongClickListener(listener);
	}

	protected void setContentDesc(@StringRes int descId) {
		view.setContentDescription(app.getString(descId));
	}
}