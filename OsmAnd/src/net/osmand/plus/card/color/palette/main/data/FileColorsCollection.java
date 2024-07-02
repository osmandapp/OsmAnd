package net.osmand.plus.card.color.palette.main.data;

import androidx.annotation.NonNull;

import net.osmand.ColorPalette;
import net.osmand.ColorPalette.ColorValue;
import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileColorsCollection extends ColorsCollection {

	private static final String DEFAULT_USER_PALETTE_FILE = "user_palette_default.txt";

	private final File file;
	private final List<String> commentsFromFile = new ArrayList<>();

	public FileColorsCollection(@NonNull OsmandApplication app) {
		this(getSourceFile(app));
	}

	public FileColorsCollection(@NonNull File file) {
		this.file = file;
		loadColors();
	}

	@Override
	protected void loadColorsInLastUsedOrder() throws IOException {
		long now = System.currentTimeMillis();
		ColorPalette palette = readFile();
		for (ColorValue color : palette.getColors()) {
			lastUsedOrder.add(new PaletteColor(color, now++));
		}
	}

	private ColorPalette readFile() throws IOException {
		commentsFromFile.clear();
		return ColorPalette.parseColorPalette(new FileReader(file), commentsFromFile, false);
	}

	@Override
	protected void saveColors() {
		// Update indexes
		for (PaletteColor paletteColor : originalOrder) {
			int index = originalOrder.indexOf(paletteColor);
			paletteColor.setIndex(index + 1);
		}
		// Use order of last used colors
		List<ColorValue> colorValues = new ArrayList<>();
		for (PaletteColor paletteColor : lastUsedOrder) {
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
