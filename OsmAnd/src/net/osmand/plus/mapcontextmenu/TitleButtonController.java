package net.osmand.plus.mapcontextmenu;

import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.ColorUtilities;

public abstract class TitleButtonController {

	private final MenuController controller;

	public String caption = "";
	public int startIconId;
	public int endIconId;
	public boolean needRightText;
	public String rightTextCaption = "";
	public boolean visible = true;
	public boolean tintIcon = true;
	public Drawable startIcon;
	public Drawable endIcon;
	public boolean enabled = true;

	public TitleButtonController(@NonNull MenuController controller) {
		this.controller = controller;
	}

	@Nullable
	public Drawable getStartIcon() {
		return getIconDrawable(true);
	}

	@Nullable
	public Drawable getEndIcon() {
		return getIconDrawable(false);
	}

	@Nullable
	private Drawable getIconDrawable(boolean start) {
		Drawable drawable = start ? startIcon : endIcon;
		if (drawable != null) {
			return drawable;
		}
		int resId = start ? startIconId : endIconId;
		if (resId != 0) {
			if (tintIcon) {
				return enabled ? getNormalIcon(resId) : getDisabledIcon(resId);
			}
			MapActivity mapActivity = controller.getMapActivity();
			return mapActivity != null ? AppCompatResources.getDrawable(mapActivity, resId) : null;
		}
		return null;
	}

	public void clearIcon(boolean left) {
		if (left) {
			startIcon = null;
			startIconId = 0;
		} else {
			endIcon = null;
			endIconId = 0;
		}
	}

	private Drawable getDisabledIcon(@DrawableRes int iconResId) {
		return controller.getIcon(iconResId, ColorUtilities.getDefaultIconColorId(!controller.isLight()));
	}

	private Drawable getNormalIcon(@DrawableRes int iconResId) {
		return controller.getIcon(iconResId, ColorUtilities.getActiveColorId(!controller.isLight()));
	}

	public abstract void buttonPressed();
}
