package net.osmand.plus.settings.enums;

import static net.osmand.plus.settings.enums.ScreenLayoutMode.PORTRAIT;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum PanelsLayoutMode {

	WIDE(R.string.panels_layout_wide,
			R.drawable.ic_action_panels_layout_portrait_wide,
			R.drawable.ic_action_panels_layout_landscape_wide,
			R.drawable.img_panels_layout_portrait_wide_day,
			R.drawable.img_panels_layout_portrait_wide_night,
			R.drawable.img_panels_layout_landscape_wide_day,
			R.drawable.img_panels_layout_landscape_wide_night),

	COMPACT(R.string.panels_layout_compact,
			R.drawable.ic_action_panels_layout_portrait_compact,
			R.drawable.ic_action_panels_layout_landscape_compact,
			R.drawable.img_panels_layout_portrait_compact_day,
			R.drawable.img_panels_layout_portrait_compact_night,
			R.drawable.img_panels_layout_landscape_compact_day,
			R.drawable.img_panels_layout_landscape_compact_night);

	@StringRes
	private final int nameId;
	@DrawableRes
	private final int iconPortrait;
	@DrawableRes
	private final int iconLandscape;
	@DrawableRes
	private final int imagePortraitDay;
	@DrawableRes
	private final int imagePortraitNight;
	@DrawableRes
	private final int imageLandscapeDay;
	@DrawableRes
	private final int imageLandscapeNight;

	PanelsLayoutMode(@StringRes int nameId,
			@DrawableRes int iconPortrait,
			@DrawableRes int iconLandscape,
			@DrawableRes int imagePortraitDay,
			@DrawableRes int imagePortraitNight,
			@DrawableRes int imageLandscapeDay,
			@DrawableRes int imageLandscapeNight) {
		this.nameId = nameId;
		this.iconPortrait = iconPortrait;
		this.iconLandscape = iconLandscape;
		this.imagePortraitDay = imagePortraitDay;
		this.imagePortraitNight = imagePortraitNight;
		this.imageLandscapeDay = imageLandscapeDay;
		this.imageLandscapeNight = imageLandscapeNight;
	}

	@NonNull
	public String toHumanString(@NonNull Context ctx) {
		return ctx.getString(nameId);
	}

	@DrawableRes
	public int getIcon(@Nullable ScreenLayoutMode mode) {
		return mode == null || mode == PORTRAIT ? iconPortrait : iconLandscape;
	}

	@DrawableRes
	public int getImage(boolean portrait, boolean nightMode) {
		if (portrait) {
			return nightMode ? imagePortraitNight : imagePortraitDay;
		} else {
			return nightMode ? imageLandscapeNight : imageLandscapeDay;
		}
	}

	@NonNull
	public static PanelsLayoutMode getDefault() {
		return WIDE;
	}
}