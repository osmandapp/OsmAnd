package net.osmand.plus.card.color.palette.main.data;

import static net.osmand.IndexConstants.COLOR_PALETTE_DIR;
import static net.osmand.plus.helpers.ColorsPaletteUtils.DEFAULT_USER_PALETTE_FILE;

import androidx.annotation.NonNull;

import net.osmand.ColorPalette;
import net.osmand.ColorPalette.ColorValue;
import net.osmand.plus.OsmandApplication;
import net.osmand.shared.palette.domain.DefaultPaletteColors;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileColorsCollection extends ColorsCollection {

	private static boolean fileRecreated = false;

	private final File file;

	public FileColorsCollection(@NonNull OsmandApplication app) {
		this(getSourceFile(app));
	}

	private FileColorsCollection(@NonNull File file) {
		this.file = file;
		if (fileRecreated) {
			addAllUniqueColors(DefaultPaletteColors.values());
		} else {
			loadColors();
		}
	}

	@Override
	protected void loadColorsInLastUsedOrder() throws IOException {
		ColorPalette palette = readFile();
		for (ColorValue color : palette.getColors()) {
			lastUsedOrder.add(new PaletteColor(color));
		}
	}

	@NonNull
	private ColorPalette readFile() throws IOException {
		return ColorPalette.parseColorPalette(new FileReader(file), false);
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
		StringBuilder content = new StringBuilder("# Index,R,G,B,A\n");
		content.append(ColorPalette.writeColorPalette(colorValues));
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			writer.write(content.toString());
			writer.flush();
			writer.close();
		} catch (IOException e) {
			LOG.error("Error when trying to write to the file: " + e.getMessage());
		}
	}

	@NonNull
	private static File getSourceFile(@NonNull OsmandApplication app) {
		File dir = app.getAppPath(COLOR_PALETTE_DIR);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		File file = new File(dir, DEFAULT_USER_PALETTE_FILE);
		try {
			if (!file.isFile()) {
				file.createNewFile();
				fileRecreated = true;
			}
		} catch (IOException e) {
			LOG.debug("Can't create a color palette file: " + e.getMessage());
		}
		return file;
	}
}
