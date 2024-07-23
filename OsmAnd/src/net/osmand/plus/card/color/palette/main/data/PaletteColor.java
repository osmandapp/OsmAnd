package net.osmand.plus.card.color.palette.main.data;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import net.osmand.ColorPalette.ColorValue;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public class PaletteColor {

	private final long id;
	private ColorValue colorValue;

	public PaletteColor(@ColorInt int color) {
		this(new ColorValue(color));
	}

	public PaletteColor(@NonNull ColorValue colorValue) {
		this.id = generateUniqueId();
		this.colorValue = colorValue;
	}

	@NonNull
	public ColorValue getColorValue() {
		return colorValue;
	}

	@ColorInt
	public int getColor() {
		return colorValue.clr;
	}

	public void setColor(@ColorInt int color) {
		this.colorValue = new ColorValue(colorValue.val, color);
	}

	public void setIndex(int index) {
		colorValue.setValue(index);
	}

	public int getIndex() {
		return (int) colorValue.val;
	}

	@NonNull
	public Long getId() {
		return id;
	}

	@NonNull
	public PaletteColor duplicate() {
		return new PaletteColor(colorValue);
	}

	@NonNull
	public String toHumanString(@NonNull OsmandApplication app) {
		return app.getString(R.string.shared_string_custom);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof PaletteColor)) return false;

		PaletteColor that = (PaletteColor) o;

		return getId().equals(that.getId());
	}

	@Override
	public int hashCode() {
		return getId().hashCode();
	}

	private static long lastGeneratedId = 0;

	private static long generateUniqueId() {
		return lastGeneratedId == 0 ? lastGeneratedId = System.currentTimeMillis() : ++lastGeneratedId;
	}
}
