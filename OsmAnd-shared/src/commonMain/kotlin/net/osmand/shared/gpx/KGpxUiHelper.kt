package net.osmand.shared.gpx

import net.osmand.shared.IndexConstants.GPX_FILE_EXT
import net.osmand.shared.IndexConstants.GPX_IMPORT_DIR
import net.osmand.shared.IndexConstants.GPX_INDEX_DIR
import net.osmand.shared.IndexConstants.GPX_RECORDED_INDEX_DIR
import net.osmand.shared.io.KFile
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.PlatformUtil

object KGpxUiHelper {

	fun getGpxTitle(name: String?): String {
		return KAlgorithms.getFileNameWithoutExtension(name) ?: ""
	}

	fun getGpxDirTitle(name: String?): String? {
		return if (KAlgorithms.isEmpty(name)) {
			""
		} else KAlgorithms.capitalizeFirstLetter(
			KAlgorithms.getFileNameWithoutExtension(
				name))
	}


	fun getFolderName(
		dir: KFile,
		includeParentDir: Boolean): String {
		var name: String = dir.name()
		if (GPX_INDEX_DIR == name + PlatformUtil.getFileSeparator()) {
			return PlatformUtil.getStringResource("shared_string_tracks")
		}
		val dirPath: String = dir.path() + PlatformUtil.getFileSeparator()
		if (dirPath.endsWith(GPX_IMPORT_DIR) || dirPath.endsWith(GPX_RECORDED_INDEX_DIR)) {
			return KAlgorithms.capitalizeFirstLetter(name) ?: ""
		}
		if (includeParentDir) {
			val parent = dir.getParentFile()
			val parentName = parent?.name() ?: ""
			if (!KAlgorithms.isEmpty(parentName) && GPX_INDEX_DIR != parentName + PlatformUtil.getFileSeparator()) {
				name = parentName + PlatformUtil.getFileSeparator() + name
			}
			return name
		}
		return name
	}

	fun isGpxFile(file: KFile): Boolean {
		return file.name().lowercase()
			.endsWith(GPX_FILE_EXT)
	}

}