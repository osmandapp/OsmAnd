package net.osmand.plus.plugins.srtm;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.ColorPalette;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class CollectColorPalletsTask extends AsyncTask<Void, Void, ColorPalette> {

	private final OsmandApplication app;
	private final String modeKey;
	private final CollectColorPalletListener listener;

	public CollectColorPalletsTask(@NonNull OsmandApplication app, @NonNull String modeKey, @NonNull CollectColorPalletListener listener) {
		this.app = app;
		this.modeKey = modeKey;
		this.listener = listener;
	}

	@Override
	protected void onPreExecute() {
		listener.collectingPalletStarted();
	}

	@Override
	protected ColorPalette doInBackground(Void... params) {
		if (!TerrainMode.isModeExist(modeKey)) {
			PlatformUtil.getLog(CollectColorPalletsTask.class).error("Provided terrain mode doesn't exist");
			return null;
		}
		TerrainMode mode = TerrainMode.getByKey(modeKey);
		File heightmapDir = app.getAppPath(IndexConstants.CLR_PALETTE_DIR);
		File mainColorFile = new File(heightmapDir, mode.getMainFile());

		ColorPalette colorPalette = null;
		try {
			if (mainColorFile.exists()) {
				colorPalette = ColorPalette.parseColorPalette(new FileReader(mainColorFile));
			}
		} catch (IOException e) {
			PlatformUtil.getLog(CollectColorPalletsTask.class).error("Error reading color file ", e);
		}

		return colorPalette;
	}

	@Override
	protected void onPostExecute(ColorPalette colorPalette) {
		listener.collectingPalletFinished(colorPalette);
	}

	public interface CollectColorPalletListener {

		void collectingPalletStarted();

		void collectingPalletFinished(@Nullable ColorPalette colorPalette);
	}
}
