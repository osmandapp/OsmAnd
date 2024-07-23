package net.osmand.plus.helpers;

import static net.osmand.IndexConstants.TXT_EXT;

import android.os.AsyncTask;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.ColorPalette;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.card.color.palette.gradient.DuplicateGradientTask;
import net.osmand.plus.card.color.palette.gradient.DuplicateGradientTask.DuplicateGradientListener;
import net.osmand.plus.plugins.srtm.CollectColorPalletTask;
import net.osmand.plus.plugins.srtm.CollectColorPalletTask.CollectColorPalletListener;
import net.osmand.plus.plugins.srtm.TerrainMode;
import net.osmand.plus.plugins.srtm.TerrainMode.TerrainType;
import net.osmand.router.RouteColorize;
import net.osmand.router.RouteColorize.ColorizationType;
import net.osmand.util.Algorithms;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ColorPaletteHelper {

	public static final String ROUTE_PREFIX = "route_";
	public static final String GRADIENT_ID_SPLITTER = "_";

	private final OsmandApplication app;
	private final ConcurrentHashMap<String, ColorPalette> cachedColorPalette = new ConcurrentHashMap<>();


	public ColorPaletteHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public Map<String, Pair<ColorPalette, Long>> getPalletsForType(@NonNull Object gradientType) {
		Map<String, Pair<ColorPalette, Long>> colorPalettes = new HashMap<>();
		if (gradientType instanceof ColorizationType) {
			colorPalettes = getColorizationTypePallets((ColorizationType) gradientType);
		} else if (gradientType instanceof TerrainType) {
			colorPalettes = getTerrainModePallets((TerrainType) gradientType);
		}
		return colorPalettes;
	}

	private Map<String, Pair<ColorPalette, Long>> getColorizationTypePallets(@NonNull ColorizationType type){
		Map<String, Pair<ColorPalette, Long>> colorPalettes = new HashMap<>();
		String colorTypePrefix = ROUTE_PREFIX + type.name().toLowerCase() + GRADIENT_ID_SPLITTER;

		File colorPalletsDir = getColorPaletteDir();
		File[] colorFiles = colorPalletsDir.listFiles();
		if (colorFiles != null) {
			for (File file : colorFiles) {
				String fileName = file.getName();
				if (fileName.startsWith(colorTypePrefix) && fileName.endsWith(TXT_EXT)) {
					String colorPalletName = fileName.replace(colorTypePrefix, "").replace(TXT_EXT, "");
					ColorPalette colorPalette = getGradientColorPalette(fileName);
					colorPalettes.put(colorPalletName, new Pair<>(colorPalette, file.lastModified()));
				}
			}
		}
		return colorPalettes;
	}

	private Map<String, Pair<ColorPalette, Long>> getTerrainModePallets(@NonNull TerrainType type) {
		Map<String, Pair<ColorPalette, Long>> colorPalettes = new HashMap<>();
		for (TerrainMode mode : TerrainMode.values(app)) {
			if (mode.getType() == type) {
				String fileName = mode.getMainFile();
				File file = new File(getColorPaletteDir(), fileName);
				ColorPalette colorPalette = getGradientColorPalette(fileName);
				if (colorPalette != null && file.exists()) {
					colorPalettes.put(mode.getKeyName(), new Pair<>(colorPalette, file.lastModified()));
				}
			}
		}
		return colorPalettes;
	}

	private boolean isValidPalette(ColorPalette palette) {
		return palette != null && palette.getColors().size() >= 2;
	}

	private File getColorPaletteDir() {
		return app.getAppPath(IndexConstants.CLR_PALETTE_DIR);
	}

	@NonNull
	public ColorPalette requireGradientColorPaletteSync(@NonNull ColorizationType colorizationType, @NonNull String gradientPaletteName) {
		ColorPalette colorPalette = getGradientColorPaletteSync(colorizationType, gradientPaletteName);
		return isValidPalette(colorPalette) ? colorPalette : RouteColorize.getDefaultPalette(colorizationType);
	}

	@Nullable
	public ColorPalette getGradientColorPaletteSync(@NonNull ColorizationType colorizationType, @NonNull String gradientPaletteName) {
		String colorPaletteFileName = ROUTE_PREFIX + colorizationType.name().toLowerCase() + GRADIENT_ID_SPLITTER + gradientPaletteName + TXT_EXT;
		return getGradientColorPalette(colorPaletteFileName);
	}

	@Nullable
	public ColorPalette getGradientColorPaletteSync(@NonNull String modeKey) {
		return getGradientColorPalette(modeKey);
	}

	@Nullable
	public ColorPalette getGradientColorPalette(@NonNull String colorPaletteFileName) {
		ColorPalette colorPalette = cachedColorPalette.get(colorPaletteFileName);

		if (colorPalette == null) {
			File colorPaletteFile = new File(getColorPaletteDir(), colorPaletteFileName);
			try {
				if (colorPaletteFile.exists()) {
					colorPalette = ColorPalette.parseColorPalette(new FileReader(colorPaletteFile));
					cachedColorPalette.put(colorPaletteFileName, colorPalette);
				}
			} catch (IOException e) {
				PlatformUtil.getLog(ColorPaletteHelper.class).error("Error reading color file ", e);
			}
		}
		return colorPalette;
	}

	public void getColorPaletteAsync(@NonNull String modeKey, @NonNull CollectColorPalletListener listener) {
		TerrainMode mode = TerrainMode.getByKey(modeKey);
		String colorPaletteFileName = mode.getMainFile();
		getGradientColorPaletteAsync(colorPaletteFileName, listener);
	}

	public void getColorPaletteAsync(@NonNull ColorizationType colorizationType, @NonNull String gradientPaletteName, @NonNull CollectColorPalletListener listener) {
		String colorPaletteFileName = ROUTE_PREFIX + colorizationType.name().toLowerCase() + GRADIENT_ID_SPLITTER + gradientPaletteName + TXT_EXT;
		getGradientColorPaletteAsync(colorPaletteFileName, listener);
	}

	private void getGradientColorPaletteAsync(@NonNull String colorPaletteFileName, @NonNull CollectColorPalletListener listener) {
		ColorPalette colorPalette = cachedColorPalette.get(colorPaletteFileName);

		if (colorPalette != null) {
			listener.collectingPalletFinished(colorPalette);
		} else {
			CollectColorPalletTask collectColorPalletTask = new CollectColorPalletTask(app, colorPaletteFileName, new CollectColorPalletListener() {
				@Override
				public void collectingPalletStarted() {
					listener.collectingPalletStarted();
				}

				@Override
				public void collectingPalletFinished(@Nullable ColorPalette colorPalette) {
					if (colorPalette != null) {
						cachedColorPalette.put(colorPaletteFileName, colorPalette);
					}
					listener.collectingPalletFinished(colorPalette);
				}
			});
			collectColorPalletTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}

	public void duplicateGradient(@NonNull String colorPaletteFileName, @NonNull DuplicateGradientListener duplicateGradientListener) {
		DuplicateGradientTask duplicateGradientTask = new DuplicateGradientTask(app, colorPaletteFileName, duplicateGradientListener);
		duplicateGradientTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void deleteGradient(@NonNull String colorPaletteFileName, @NonNull DeleteGradientListener deleteGradientListener) {
		File gradientToDelete = new File(getColorPaletteDir(), colorPaletteFileName);

		boolean deleted = Algorithms.removeAllFiles(gradientToDelete);
		if (deleted) {
			cachedColorPalette.remove(colorPaletteFileName);
		}
		deleteGradientListener.onGradientDeleted(deleted);
	}

	public interface DeleteGradientListener {
		void onGradientDeleted(boolean deleted);
	}
}