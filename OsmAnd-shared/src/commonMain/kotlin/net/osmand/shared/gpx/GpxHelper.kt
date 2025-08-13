package net.osmand.shared.gpx

import net.osmand.shared.IndexConstants.GPX_FILE_EXT
import net.osmand.shared.io.KFile
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.Localization
import net.osmand.shared.util.PlatformUtil
import okio.Path

object GpxHelper {

	fun getGpxTitle(name: String?) = KFile.getFileNameWithoutExtension(name) ?: ""

	fun getGpxDirTitle(name: String?): String? =
		if (KAlgorithms.isEmpty(name)) {
			""
		} else {
			KAlgorithms.capitalizeFirstLetter(KFile.getFileNameWithoutExtension(name))
		}

	fun getFolderName(
		dir: KFile,
		includeParentDir: Boolean): String {
		var name: String = dir.name()
		val osmandCtx = PlatformUtil.getOsmAndContext()
		if (osmandCtx.getGpxDir().name() == name + Path.DIRECTORY_SEPARATOR) {
			return Localization.getString("shared_string_tracks")
		}
		val dirPath: String = dir.path() + Path.DIRECTORY_SEPARATOR
		if (dirPath.endsWith(osmandCtx.getGpxImportDir().name()) || dirPath.endsWith(osmandCtx.getGpxRecordedDir().name())) {
			return KAlgorithms.capitalizeFirstLetter(name) ?: ""
		}
		if (includeParentDir) {
			val parent = dir.getParentFile()
			val parentName = parent?.name() ?: ""
			if (!KAlgorithms.isEmpty(parentName) && osmandCtx.getGpxDir().name() != parentName + Path.DIRECTORY_SEPARATOR) {
				name = parentName + Path.DIRECTORY_SEPARATOR + name
			}
			return name
		}
		return name
	}

	fun isGpxFile(file: KFile) = file.name().lowercase().endsWith(GPX_FILE_EXT)
}