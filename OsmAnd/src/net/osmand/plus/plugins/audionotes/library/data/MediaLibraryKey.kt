package net.osmand.plus.plugins.audionotes.library.data

import net.osmand.shared.gpx.primitives.Link
import net.osmand.shared.media.LinkMediaFactory
import net.osmand.shared.media.MediaProvider
import java.net.URI

object MediaLibraryKey {
	fun of(link: Link, appBasePath: String): String? {
		val href = link.href?.trim()?.takeIf { it.isNotEmpty() } ?: return null
		LinkMediaFactory.getInternalMediaFileName(LinkMediaFactory.getInternalPath(href))?.let { return "internal:$it" }
		val uri = runCatching { URI(href).normalize() }.getOrNull()
		if (uri?.scheme.equals("file", ignoreCase = true)) {
			val path = uri?.path.orEmpty()
			val base = appBasePath.trimEnd('/') + "/"
			if (path.startsWith(base)) {
				MediaProvider.getInternalMediaAliasFileName(path.removePrefix(base))?.let { return "internal:$it" }
			}
		}
		return LinkMediaFactory.getMediaId(link)
	}
}
