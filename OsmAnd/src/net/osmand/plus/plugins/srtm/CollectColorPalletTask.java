package net.osmand.plus.plugins.srtm;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.shared.ColorPalette;
import net.osmand.shared.palette.data.PaletteRepository;
import net.osmand.shared.palette.data.PaletteUtils;
import net.osmand.shared.palette.domain.PaletteItem;
import net.osmand.shared.palette.domain.category.PaletteCategory;
import net.osmand.shared.palette.domain.filetype.PaletteFileType;
import net.osmand.shared.palette.domain.filetype.PaletteFileTypeRegistry;

public class CollectColorPalletTask extends AsyncTask<Void, Void, ColorPalette> {

	private final OsmandApplication app;
	private final String colorPaletteFileName;
	private final CollectColorPalletListener listener;

	public static void execute(@NonNull OsmandApplication app, @NonNull String fileName,
	                           @NonNull CollectColorPalletListener listener) {
		OsmAndTaskManager.executeTask(new CollectColorPalletTask(app, fileName, listener));
	}

	private CollectColorPalletTask(@NonNull OsmandApplication app, @NonNull String fileName,
	                               @NonNull CollectColorPalletListener listener) {
		this.app = app;
		this.colorPaletteFileName = fileName;
		this.listener = listener;
	}

	@Override
	protected void onPreExecute() {
		listener.collectingPalletStarted();
	}

	@Override
	protected ColorPalette doInBackground(Void... params) {
		PaletteFileType fileType = PaletteFileTypeRegistry.INSTANCE.fromFileName(colorPaletteFileName);
		PaletteCategory category = fileType != null ? fileType.getCategory() : null;
		String paletteName = PaletteUtils.INSTANCE.extractPaletteName(colorPaletteFileName);

		PaletteRepository repository = app.getPaletteRepository();
		if (category != null && paletteName != null) {
			PaletteItem item = repository.findPaletteItem(category.getId(), paletteName);
			if (item instanceof PaletteItem.Gradient gradient) {
				return gradient.getColorPalette();
			}
		}
		return null;
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
