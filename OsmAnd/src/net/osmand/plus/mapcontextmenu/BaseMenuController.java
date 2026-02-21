package net.osmand.plus.mapcontextmenu;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;

public abstract class BaseMenuController {

	@NonNull
	protected final OsmandApplication app;
	private FragmentActivity activity;
	private boolean portraitMode;
	private int landscapeWidthPx;
	protected boolean nightMode;

	public BaseMenuController(@NonNull FragmentActivity activity) {
		this.app = (OsmandApplication) activity.getApplication();
		this.activity = activity;
		init();
	}

	private void init() {
		portraitMode = AndroidUiHelper.isOrientationPortrait(activity != null ? activity : app);
		landscapeWidthPx = app.getResources().getDimensionPixelSize(R.dimen.dashboard_land_width);
		updateNightMode();
	}

	@Nullable
	public MapActivity getMapActivity() {
		if (activity instanceof MapActivity mapActivity) {
			return mapActivity;
		}
		return null;
	}

	@NonNull
	public FragmentActivity getActivity() {
		return activity;
	}

	public void setMapActivity(@Nullable MapActivity mapActivity) {
		if (mapActivity != null && mapActivity != activity) {
			this.activity = mapActivity;
		}
		if (mapActivity != null) {
			init();
		}
	}

	public boolean isLight() {
		return !nightMode;
	}

	public void updateNightMode() {
		nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.OVER_MAP);
	}

	public boolean isLandscapeLayout() {
		return !portraitMode;
	}

	public int getLandscapeWidthPx() {
		return landscapeWidthPx;
	}

	public float getHalfScreenMaxHeightKoef() {
		return .75f;
	}

	public int getSlideInAnimation() {
		if (isLandscapeLayout()) {
			return AndroidUtils.isLayoutRtl(app)
					? R.anim.slide_in_right : R.anim.slide_in_left;
		} else {
			return R.anim.slide_in_bottom;
		}
	}

	public int getSlideOutAnimation() {
		if (isLandscapeLayout()) {
			return AndroidUtils.isLayoutRtl(app)
					? R.anim.slide_out_right : R.anim.slide_out_left;
		} else {
			return R.anim.slide_out_bottom;
		}
	}

	protected Drawable getIconOrig(int iconId) {
		UiUtilities iconsCache = app.getUIUtilities();
		return iconsCache.getIcon(iconId, 0);

	}

	protected Drawable getIcon(int iconId) {
		return getIcon(iconId, ColorUtilities.getDefaultIconColorId(!isLight()));
	}

	protected Drawable getIcon(int iconId, int colorId) {
		UiUtilities iconsCache = app.getUIUtilities();
		return iconsCache.getIcon(iconId, colorId);
	}

	@NonNull
	protected String getString(@StringRes int resId, Object... formatArgs) {
		return app.getString(resId, formatArgs);
	}

    @NonNull
    public OsmandApplication getApplication() {
        return app;
    }
}
