package net.osmand.plus.card.color.palette.gradient;

import static net.osmand.plus.helpers.ColorPaletteHelper.GRADIENT_ID_SPLITTER;

import androidx.annotation.NonNull;

import net.osmand.ColorPalette;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;

public class PaletteGradientColor extends PaletteColor {

	public static String DEFAULT_NAME = "default";

	private final String stringId;
	private final ColorPalette colorPalette;
	private int index;

	public PaletteGradientColor(@NonNull String id, @NonNull ColorPalette colorPalette,
	                            long creationTime, int initialIndex) {
		super(null, creationTime);
		this.stringId = id;
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
		return getTypeName(getStringId());
	}

	@NonNull
	public String getPaletteName() {
		return getPaletteName(getStringId());
	}

	@NonNull
	public static String getTypeName(@NonNull String stringId){
		String[] splitId = stringId.split(GRADIENT_ID_SPLITTER);
		return splitId[0].toLowerCase();
	}

	@NonNull
	public static String getPaletteName(@NonNull String stringId) {
		String[] splitId = stringId.split(GRADIENT_ID_SPLITTER);
		return splitId[1];
	}

	@NonNull
	public ColorPalette getColorPalette() {
		return colorPalette;
	}
}
