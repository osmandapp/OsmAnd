package net.osmand.plus.card.color.palette.main.data;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

public interface DefaultPaletteColors {

	int SUNNY_YELLOW = 0xFFEECC22;
	int BRIGHT_RED = 0xFFD00D0D;
	int ORANGE_RED = 0xFFFF5020;
	int LEMON_YELLOW = 0xFFEEEE10;
	int SPRING_GREEN = 0xFF88E030;
	int FOREST_GREEN = 0xFF00842B;
	int SKY_BLUE = 0xFF10C0F0;
	int ROYAL_BLUE = 0xFF1010A0;
	int PURPLE = 0xFFA71DE1;
	int PINK = 0xFFE044BB;
	int BRICK_RED = 0xFF8E2512;
	int ALMOST_BLACK = 0xFF000001;
	int LAVENDER = 0xFFDC5FFF;
	int PURE_BLUE = 0xFF0000FF;
	int CYAN = 0xFF00FFFF;

	@NonNull
	static List<Integer> valuesList() {
		return Arrays.asList(values());
	}

	@NonNull
	static Integer[] values() {
		return new Integer[] {
				SUNNY_YELLOW, BRIGHT_RED, ORANGE_RED, LEMON_YELLOW,
				SPRING_GREEN, FOREST_GREEN, SKY_BLUE, ROYAL_BLUE,
				PURPLE, PINK, BRICK_RED, ALMOST_BLACK, LAVENDER,
				PURE_BLUE, CYAN
		};
	}
}
