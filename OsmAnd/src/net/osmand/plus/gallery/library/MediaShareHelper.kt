package net.osmand.plus.gallery.library

import android.content.ClipData
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.utils.AndroidUtils
import net.osmand.shared.media.MediaUriResolver
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaType

object MediaShareHelper {
	@JvmStatic
	fun share(activity: FragmentActivity, items: List<MediaItem>) {
		if (items.isEmpty()) return
		val app = activity.application as OsmandApplication
		val resolver = app.galleryHelper.mediaSourceResolver
		val files = items.mapNotNull { item -> resolver.getShareableUri(item)?.let { item to it } }
		val uris = files.map { it.second }
		// Remote items without a shareable file are sent as links; local items whose file is gone are skipped
		val links = items.filter { item -> item is MediaItem.Remote && files.none { it.first === item } }
			.mapNotNull { MediaUriResolver.getShareUri(it)?.takeIf(String::isNotBlank) }
		if (uris.isEmpty() && links.isEmpty()) return
		val intent = Intent()
		if (uris.isNotEmpty()) {
			intent.action = if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE
			intent.type = files.map { mimeType(it.first) }.distinct().singleOrNull() ?: "*/*"
			if (uris.size == 1) intent.putExtra(Intent.EXTRA_STREAM, uris.first())
			else intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
			intent.clipData = ClipData.newRawUri(files.first().first.title, uris.first()).apply {
				uris.drop(1).forEach { addItem(ClipData.Item(it)) }
			}
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
			if (links.isNotEmpty()) intent.putExtra(Intent.EXTRA_TEXT, links.joinToString("\n"))
		} else {
			intent.action = Intent.ACTION_SEND
			intent.type = "text/plain"
			intent.putExtra(Intent.EXTRA_TEXT, links.joinToString("\n"))
		}
		AndroidUtils.startActivityIfSafe(activity, Intent.createChooser(intent, activity.getString(R.string.shared_string_share))
			.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
	}

	private fun mimeType(item: MediaItem) = when (item.type) {
		MediaType.PHOTO -> "image/*"
		MediaType.VIDEO -> "video/*"
		MediaType.AUDIO -> "audio/*"
		else -> "*/*"
	}
}
