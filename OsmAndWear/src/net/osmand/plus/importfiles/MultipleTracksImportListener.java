package net.osmand.plus.importfiles;

import net.osmand.shared.gpx.GpxFile;

public abstract class MultipleTracksImportListener implements GpxImportListener {

	private final int filesSize;
	private int importCounter;

	public MultipleTracksImportListener(int filesSize) {
		this.filesSize = filesSize;
	}

	@Override
	public void onImportComplete(boolean success) {
		if (!success) {
			importCounter++;
		}
		checkImportFinished();
	}

	@Override
	public void onSaveComplete(boolean success, GpxFile gpxFile) {
		importCounter++;
		checkImportFinished();
	}

	private void checkImportFinished() {
		if (importCounter == filesSize) {
			onImportFinished();
		}
	}
}