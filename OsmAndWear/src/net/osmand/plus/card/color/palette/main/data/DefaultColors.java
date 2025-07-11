package net.osmand.plus.card.color.palette.main.data;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.card.color.palette.migration.data.PaletteColorV1;
import net.osmand.plus.card.color.palette.migration.data.PredefinedPaletteColor;

import java.util.Objects;

public interface DefaultColors {

	PaletteColorV1 DARK_YELLOW = new PredefinedPaletteColor("darkyellow", 0xffeecc22, R.string.rendering_value_darkyellow_name);
	PaletteColorV1 RED = new PredefinedPaletteColor("red", 0xffd00d0d, R.string.rendering_value_red_name);
	PaletteColorV1 ORANGE = new PredefinedPaletteColor("orange", 0xffff5020, R.string.rendering_value_orange_name);
	PaletteColorV1 YELLOW = new PredefinedPaletteColor("yellow", 0xffeeee10, R.string.rendering_value_yellow_name);
	PaletteColorV1 LIGHT_GREEN = new PredefinedPaletteColor("lightgreen", 0xff88e030, R.string.rendering_value_lightgreen_name);
	PaletteColorV1 GREEN = new PredefinedPaletteColor("green", 0xff00842b, R.string.rendering_value_green_name);
	PaletteColorV1 LIGHT_BLUE = new PredefinedPaletteColor("lightblue", 0xff10c0f0, R.string.rendering_value_lightblue_name);
	PaletteColorV1 BLUE = new PredefinedPaletteColor("blue", 0xff1010a0, R.string.rendering_value_blue_name);
	PaletteColorV1 PURPLE = new PredefinedPaletteColor("purple", 0xffa71de1, R.string.rendering_value_purple_name);
	PaletteColorV1 PINK = new PredefinedPaletteColor("pink", 0xffe044bb, R.string.rendering_value_pink_name);
	PaletteColorV1 BROWN = new PredefinedPaletteColor("brown", 0xff8e2512, R.string.rendering_value_brown_name);
	PaletteColorV1 BLACK = new PredefinedPaletteColor("black", 0xff000001, R.string.rendering_value_black_name);

	@ColorInt
	static int valueOf(@NonNull String id) {
		for (PaletteColorV1 paletteColor : values()) {
			if (Objects.equals(paletteColor.getId(), id)) {
				return paletteColor.getColor();
			}
		}
		return 0;
	}

	@NonNull
	static PaletteColorV1[] values() {
		return new PaletteColorV1[]{
				DARK_YELLOW, RED, ORANGE, YELLOW, LIGHT_GREEN, GREEN,
				LIGHT_BLUE, BLUE, PURPLE, PINK, BROWN, BLACK
		};
	}

}
