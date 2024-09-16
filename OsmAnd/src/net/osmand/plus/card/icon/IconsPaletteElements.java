package net.osmand.plus.card.icon;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.UiUtilities;

public abstract class IconsPaletteElements<IconData> {

	protected final OsmandApplication app;
	protected final LayoutInflater themedInflater;
	protected final boolean nightMode;

	public IconsPaletteElements(@NonNull Context context, boolean nightMode) {
		this.nightMode = nightMode;
		app = (OsmandApplication) context.getApplicationContext();
		themedInflater = UiUtilities.getInflater(context, nightMode);
	}

	@NonNull
	public View createView(@NonNull ViewGroup parent) {
		return themedInflater.inflate(getLayoutId(), parent, false);
	}

	@LayoutRes
	protected abstract int getLayoutId();

	public abstract void bindView(@NonNull View itemView, @NonNull IconData icon,
	                              @ColorInt int controlsColor, boolean isSelected);

	protected Drawable getPaintedIcon(@DrawableRes int id, @ColorInt int color) {
		return app.getUIUtilities().getPaintedIcon(id, color);
	}

	protected Drawable getIcon(@DrawableRes int id, @ColorRes int colorId) {
		return app.getUIUtilities().getIcon(id, colorId);
	}

	protected Drawable getContentIcon(@DrawableRes int id) {
		return app.getUIUtilities().getThemedIcon(id);
	}

	public boolean isNightMode() {
		return nightMode;
	}
}
