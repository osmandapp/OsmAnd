package net.osmand.plus.backup

import java.util.ArrayList
import java.util.Collections

/**
 * Result of parsing a Cloud `/userdata/list-files` response.
 */
class CloudFileListParseResult internal constructor(
	val status: Int?,
	val message: String?,
	val maximumAccountSize: Long?,
	remoteFiles: List<RemoteFile>,
	val runtimeException: RuntimeException? = null
) {

	val remoteFiles: List<RemoteFile> =
		Collections.unmodifiableList(ArrayList(remoteFiles))
}
