package net.osmand.plus.importfiles;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.IndexConstants.OSMAND_SETTINGS_FILE_EXT;
import static net.osmand.IndexConstants.RENDERER_INDEX_EXT;
import static net.osmand.IndexConstants.ROUTING_FILE_EXT;
import static net.osmand.plus.importfiles.ImportHelper.KML_SUFFIX;
import static net.osmand.plus.importfiles.ImportHelper.KMZ_SUFFIX;

import androidx.annotation.NonNull;

public enum ImportType {

	SETTINGS(OSMAND_SETTINGS_FILE_EXT),
	ROUTING(ROUTING_FILE_EXT),
	RENDERING(RENDERER_INDEX_EXT),
	GPX(GPX_FILE_EXT),
	KML(KML_SUFFIX),
	KMZ(KMZ_SUFFIX);

	private final String extension;

	ImportType(@NonNull String extension) {
		this.extension = extension;
	}

	@NonNull
	public String getExtension() {
		return extension;
	}
}