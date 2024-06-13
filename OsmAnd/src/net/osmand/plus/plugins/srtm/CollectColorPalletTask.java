package net.osmand.plus.plugins.srtm;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.ColorPalette;
import net.osmand.plus.OsmandApplication;

import java.io.File;

public class CollectColorPalletTask extends AsyncTask<Void, Void, ColorPalette> {

	private final OsmandApplication app;
	private final File colorPaletteFile;
	private final CollectColorPalletListener listener;

	public CollectColorPalletTask(@NonNull OsmandApplication app, @NonNull File colorPaletteFile, @NonNull CollectColorPalletListener listener) {
		this.app = app;
		this.colorPaletteFile = colorPaletteFile;
		this.listener = listener;
	}

	@Override
	protected void onPreExecute() {
		listener.collectingPalletStarted();
	}

	@Override
	protected ColorPalette doInBackground(Void... params) {
		return app.getColorPaletteHelper().getGradientColorPalette(colorPaletteFile);
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
