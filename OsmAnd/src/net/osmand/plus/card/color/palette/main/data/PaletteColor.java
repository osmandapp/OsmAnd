package net.osmand.plus.card.color.palette.main.data;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import net.osmand.ColorPalette.ColorValue;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public class PaletteColor {

	private ColorValue colorValue;
	private final String id;
	private final long creationTime;

	public PaletteColor(@ColorInt int color, long creationTime) {
		this(new ColorValue(color), creationTime);
	}

	public PaletteColor(@NonNull ColorValue colorValue, long creationTime) {
		this.id = generateId(creationTime);
		this.colorValue = colorValue;
		this.creationTime = creationTime;
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

	@NonNull
	public String getId() {
		return id;
	}

	public long getCreationTime() {
		return creationTime;
	}

	@NonNull
	public PaletteColor duplicate() {
		long now = System.currentTimeMillis();
		return new PaletteColor(colorValue, now);
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

	@NonNull
	public static String generateId(long creationTime) {
		return "palette_color_" + creationTime;
	}
}
