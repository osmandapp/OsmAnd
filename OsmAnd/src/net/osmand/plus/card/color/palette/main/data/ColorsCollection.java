package net.osmand.plus.card.color.palette.main.data;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.ColorPalette;
import net.osmand.ColorPalette.ColorValue;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;

import org.apache.commons.logging.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ColorsCollection {

	private static final Log LOG = PlatformUtil.getLog(ColorsCollection.class);

	private static final String DEFAULT_USER_PALETTE_FILE = "user_palette_default.txt";

	private final File file;
	private final List<String> commentsFromFile = new ArrayList<>();
	private final List<PaletteColor> originalColors = new ArrayList<>();
	private final List<PaletteColor> lastUsedColors = new LinkedList<>();

	public ColorsCollection(@NonNull OsmandApplication app) {
		this(getSourceFile(app));
	}

	public ColorsCollection(@NonNull File file) {
		this.file = file;
		loadColors();
	}

	@Nullable
	public PaletteColor findPaletteColor(@ColorInt int colorInt) {
		for (PaletteColor paletteColor : originalColors) {
			if (paletteColor.getColor() == colorInt) {
				return paletteColor;
			}
		}
		return null;
	}

	@NonNull
	public List<PaletteColor> getColors(@NonNull PaletteSortingMode sortingMode) {
		return new ArrayList<>(sortingMode == PaletteSortingMode.ORIGINAL ? originalColors : lastUsedColors);
	}

	public void setColors(@NonNull List<PaletteColor> originalColors,
	                      @NonNull List<PaletteColor> lastUsedColors) {
		this.originalColors.clear();
		this.lastUsedColors.clear();
		this.originalColors.addAll(originalColors);
		this.lastUsedColors.addAll(lastUsedColors);
		syncFile();
	}

	@NonNull
	public PaletteColor duplicateColor(@NonNull PaletteColor paletteColor) {
		PaletteColor duplicate = paletteColor.duplicate();
		addColorDuplicate(originalColors, paletteColor, duplicate);
		addColorDuplicate(lastUsedColors, paletteColor, duplicate);
		syncFile();
		return duplicate;
	}

	private void addColorDuplicate(@NonNull List<PaletteColor> list,
	                               @NonNull PaletteColor original,
	                               @NonNull PaletteColor duplicate) {
		int index = list.indexOf(original);
		if (index >= 0 && index < list.size()) {
			list.add(index + 1, duplicate);
		} else {
			list.add(duplicate);
		}
	}

	public boolean askRemoveColor(@NonNull PaletteColor paletteColor) {
		if (originalColors.remove(paletteColor)) {
			lastUsedColors.remove(paletteColor);
			syncFile();
			return true;
		}
		return false;
	}

	@Nullable
	public PaletteColor addOrUpdateColor(@Nullable PaletteColor oldColor,
	                                     @ColorInt int newColor) {
		return oldColor == null ? addNewColor(newColor) : updateColor(oldColor, newColor);
	}

	@NonNull
	private PaletteColor addNewColor(@ColorInt int newColor) {
		long now = System.currentTimeMillis();
		PaletteColor paletteColor = new PaletteColor(newColor, now);
		originalColors.add(paletteColor);
		lastUsedColors.add(0, paletteColor);
		syncFile();
		return paletteColor;
	}

	@NonNull
	private PaletteColor updateColor(@NonNull PaletteColor paletteColor, @ColorInt int newColor) {
		paletteColor.setColor(newColor);
		syncFile();
		return paletteColor;
	}

	public void askRenewLastUsedTime(@Nullable PaletteColor paletteColor) {
		if (paletteColor != null) {
			lastUsedColors.remove(paletteColor);
			lastUsedColors.add(0, paletteColor);
		}
	}

	private void loadColors() {
		long now = System.currentTimeMillis();
		try {
			ColorPalette palette = readFile();
			originalColors.clear();
			lastUsedColors.clear();
			for (ColorValue color : palette.getColors()) {
				lastUsedColors.add(new PaletteColor(color, now++));
			}
			originalColors.addAll(lastUsedColors);
			originalColors.sort((a, b) -> Double.compare(a.getIndex(), b.getIndex()));
		} catch (IOException e) {
			LOG.error("Error when trying to read file: " + e.getMessage());
		}
	}

	private ColorPalette readFile() throws IOException {
		commentsFromFile.clear();
		return ColorPalette.parseColorPalette(new FileReader(file), commentsFromFile, false);
	}

	private void syncFile() {
		// Update indexes
		for (PaletteColor paletteColor : originalColors) {
			int index = originalColors.indexOf(paletteColor);
			paletteColor.setIndex(index + 1);
		}
		// Use order of last used colors
		List<ColorValue> colorValues = new ArrayList<>();
		for (PaletteColor paletteColor : lastUsedColors) {
			colorValues.add(paletteColor.getColorValue());
		}
		StringBuilder content = new StringBuilder();
		for (String comment : commentsFromFile) {
			content.append(comment).append("\n");
		}
		content.append(ColorPalette.writeColorPalette(colorValues));

		try {
			FileWriter writer = new FileWriter(file);
			BufferedWriter w = new BufferedWriter(writer);
			w.write(content.toString());
			w.flush();
			w.close();
		} catch (IOException e) {
			LOG.error("Error when trying to write to the file: " + e.getMessage());
		}
	}

	@NonNull
	private static File getSourceFile(@NonNull OsmandApplication app) {
		File dir = app.getAppPath(IndexConstants.COLOR_PALETTE_DIR);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		File file = new File(dir, DEFAULT_USER_PALETTE_FILE);
		try {
			file.createNewFile();
		} catch (IOException e) {
			LOG.debug("Can't create a color palette file: " + e.getMessage());
		}
		return file;
	}
}
