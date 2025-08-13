package net.osmand.plus.card.color.palette.gradient;

import static net.osmand.plus.helpers.ColorPaletteHelper.GRADIENT_ID_SPLITTER;

import androidx.annotation.NonNull;

import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.shared.ColorPalette;

import java.util.Objects;

public class PaletteGradientColor extends PaletteColor {

	public static String DEFAULT_NAME = "default";

	private final String stringId;
	private final String typeName;
	private final String paletteName;
	private final ColorPalette colorPalette;
	private int index;

	public PaletteGradientColor(@NonNull String typeName, @NonNull String paletteName,
	                            @NonNull ColorPalette colorPalette, int initialIndex) {
		super(null);
		this.stringId = typeName + GRADIENT_ID_SPLITTER + paletteName;
		this.typeName = typeName;
		this.paletteName = paletteName;
		this.colorPalette = colorPalette;
		this.index = initialIndex;
	}

	public String getStringId() {
		return stringId;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public void setIndex(int index) {
		this.index = index;
	}

	@NonNull
	public String getTypeName() {
		return typeName;
	}

	@NonNull
	public String getPaletteName() {
		return paletteName;
	}

	@NonNull
	public ColorPalette getColorPalette() {
		return colorPalette;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof PaletteGradientColor)) return false;
		PaletteGradientColor that = (PaletteGradientColor) o;
		return Objects.equals(getStringId(), that.getStringId());
	}

	@Override
	public int hashCode() {
		return getStringId() != null ? getStringId().hashCode() : 0;
	}
}
