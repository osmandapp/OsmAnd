package net.osmand.shared.media.library

import net.osmand.shared.media.domain.MediaType

interface SortableMedia {
	val id: String
	val title: String
	val type: MediaType
	val dateMs: Long?
	val lastModifiedMs: Long?
	val sizeBytes: Long?
	val durationMs: Long?
	val lat: Double?
	val lon: Double?
}
