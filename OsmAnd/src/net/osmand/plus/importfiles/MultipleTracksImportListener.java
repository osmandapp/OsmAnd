package net.osmand.plus.importfiles;

import net.osmand.shared.gpx.GpxFile;

public abstract class MultipleTracksImportListener implements GpxImportListener {

	private int filesCount;
	private int importCounter;

	public MultipleTracksImportListener(int filesCount) {
		this.filesCount = filesCount;
	}

	public void setFilesCount(int filesCount) {
		this.filesCount = filesCount;
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
		if (importCounter == filesCount) {
			onImportFinished();
		}
	}
}