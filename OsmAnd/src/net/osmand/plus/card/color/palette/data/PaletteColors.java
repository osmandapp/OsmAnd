package net.osmand.plus.card.color.palette.data;

import androidx.annotation.NonNull;

import net.osmand.plus.R;

public interface PaletteColors {

	PaletteColor DARK_YELLOW = new DefaultPaletteColor("darkyellow", 0xffeecc22, R.string.rendering_value_darkyellow_name);
	PaletteColor RED = new DefaultPaletteColor("red", 0xffd00d0d, R.string.rendering_value_red_name);
	PaletteColor ORANGE = new DefaultPaletteColor("orange", 0xffff5020, R.string.rendering_value_orange_name);
	PaletteColor YELLOW = new DefaultPaletteColor("yellow", 0xffeeee10, R.string.rendering_value_yellow_name);
	PaletteColor LIGHT_GREEN = new DefaultPaletteColor("lightgreen", 0xff88e030, R.string.rendering_value_lightgreen_name);
	PaletteColor GREEN = new DefaultPaletteColor("green", 0xff00842b, R.string.rendering_value_green_name);
	PaletteColor LIGHT_BLUE = new DefaultPaletteColor("lightblue", 0xff10c0f0, R.string.rendering_value_lightblue_name);
	PaletteColor BLUE = new DefaultPaletteColor("blue", 0xff1010a0, R.string.rendering_value_blue_name);
	PaletteColor PURPLE = new DefaultPaletteColor("purple", 0xffa71de1, R.string.rendering_value_purple_name);
	PaletteColor PINK = new DefaultPaletteColor("pink", 0xffe044bb, R.string.rendering_value_pink_name);
	PaletteColor BROWN = new DefaultPaletteColor("brown", 0xff8e2512, R.string.rendering_value_brown_name);
	PaletteColor BLACK = new DefaultPaletteColor("black", 0xff000001, R.string.rendering_value_black_name);

	@NonNull
	static PaletteColor[] values() {
		return new PaletteColor[]{
				DARK_YELLOW, RED, ORANGE, YELLOW, LIGHT_GREEN, GREEN,
				LIGHT_BLUE, BLUE, PURPLE, PINK, BROWN, BLACK
		};
	}

}
