package net.osmand.plus.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.srtm.CollectColorPalletTask;
import net.osmand.plus.plugins.srtm.CollectColorPalletTask.CollectColorPalletListener;
import net.osmand.plus.plugins.srtm.TerrainMode;
import net.osmand.shared.ColorPalette;
import net.osmand.shared.io.KFile;

import java.util.concurrent.ConcurrentHashMap;

@Deprecated
public class ColorPaletteHelper {

	private final OsmandApplication app;
	private final ConcurrentHashMap<String, ColorPalette> cachedColorPalette = new ConcurrentHashMap<>();


	public ColorPaletteHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	private KFile getColorPaletteDir() {
		return app.getAppPathKt(IndexConstants.CLR_PALETTE_DIR);
	}

	@Nullable
	public ColorPalette getGradientColorPalette(@NonNull String colorPaletteFileName) {
		ColorPalette colorPalette = cachedColorPalette.get(colorPaletteFileName);

		if (colorPalette == null) {
			KFile colorPaletteFile = new KFile(getColorPaletteDir(), colorPaletteFileName);
			try {
				if (colorPaletteFile.exists()) {
					colorPalette = ColorPalette.Companion.parseColorPalette(colorPaletteFile);
					cachedColorPalette.put(colorPaletteFileName, colorPalette);
				}
			} catch (Exception e) {
				PlatformUtil.getLog(ColorPaletteHelper.class).error("Error reading color file ", e);
			}
		}
		return colorPalette;
	}

	public void getColorPaletteAsync(@NonNull String modeKey, @NonNull CollectColorPalletListener listener) {
		TerrainMode mode = TerrainMode.getByKey(modeKey);
		String colorPaletteFileName = mode.getMainFileName();
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
			OsmAndTaskManager.executeTask(collectColorPalletTask);
		}
	}
}