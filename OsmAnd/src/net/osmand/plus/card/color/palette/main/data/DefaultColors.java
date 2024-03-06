package net.osmand.plus.card.color.palette.main.data;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import net.osmand.plus.R;

import java.util.Objects;

public interface DefaultColors {

	PaletteColor DARK_YELLOW = new PredefinedPaletteColor("darkyellow", 0xffeecc22, R.string.rendering_value_darkyellow_name);
	PaletteColor RED = new PredefinedPaletteColor("red", 0xffd00d0d, R.string.rendering_value_red_name);
	PaletteColor ORANGE = new PredefinedPaletteColor("orange", 0xffff5020, R.string.rendering_value_orange_name);
	PaletteColor YELLOW = new PredefinedPaletteColor("yellow", 0xffeeee10, R.string.rendering_value_yellow_name);
	PaletteColor LIGHT_GREEN = new PredefinedPaletteColor("lightgreen", 0xff88e030, R.string.rendering_value_lightgreen_name);
	PaletteColor GREEN = new PredefinedPaletteColor("green", 0xff00842b, R.string.rendering_value_green_name);
	PaletteColor LIGHT_BLUE = new PredefinedPaletteColor("lightblue", 0xff10c0f0, R.string.rendering_value_lightblue_name);
	PaletteColor BLUE = new PredefinedPaletteColor("blue", 0xff1010a0, R.string.rendering_value_blue_name);
	PaletteColor PURPLE = new PredefinedPaletteColor("purple", 0xffa71de1, R.string.rendering_value_purple_name);
	PaletteColor PINK = new PredefinedPaletteColor("pink", 0xffe044bb, R.string.rendering_value_pink_name);
	PaletteColor BROWN = new PredefinedPaletteColor("brown", 0xff8e2512, R.string.rendering_value_brown_name);
	PaletteColor BLACK = new PredefinedPaletteColor("black", 0xff000001, R.string.rendering_value_black_name);

	@ColorInt
	static int valueOf(@NonNull String id) {
		for (PaletteColor paletteColor : values()) {
			if (Objects.equals(paletteColor.getId(), id)) {
				return paletteColor.getColor();
			}
		}
		return 0;
	}

	@NonNull
	static PaletteColor[] values() {
		return new PaletteColor[]{
				DARK_YELLOW, RED, ORANGE, YELLOW, LIGHT_GREEN, GREEN,
				LIGHT_BLUE, BLUE, PURPLE, PINK, BROWN, BLACK
		};
	}

}
