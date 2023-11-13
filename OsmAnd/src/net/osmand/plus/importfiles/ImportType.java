package net.osmand.plus.importfiles;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.IndexConstants.OSMAND_SETTINGS_FILE_EXT;
import static net.osmand.IndexConstants.RENDERER_INDEX_EXT;
import static net.osmand.IndexConstants.ROUTING_FILE_EXT;

public enum ImportType {

	SETTINGS(OSMAND_SETTINGS_FILE_EXT),
	ROUTING(ROUTING_FILE_EXT),
	RENDERING(RENDERER_INDEX_EXT),
	GPX(GPX_FILE_EXT),
	KML(ImportHelper.KML_SUFFIX),
	KMZ(ImportHelper.KMZ_SUFFIX);

	private final String extension;

	ImportType(String extension) {
		this.extension = extension;
	}

	public String getExtension() {
		return extension;
	}

}