package net.osmand.plus.plugins.astronomy.views.contextmenu

import android.net.Uri
import androidx.core.net.toUri
import net.osmand.plus.OsmandApplication
import net.osmand.plus.plugins.astronomy.AstroArticle
import net.osmand.plus.plugins.astronomy.SkyObject
import org.json.JSONObject

class AstroDescriptionCardModel(
	app: OsmandApplication,
	var skyObject: SkyObject,
	var astroArticle: AstroArticle?
) : AstroContextCard(app) {

	fun updateCard(skyObject: SkyObject, astroArticle: AstroArticle?) {
		this.skyObject = skyObject
		this.astroArticle = astroArticle
	}

	fun getWikiUri(): Uri {
		var url: Uri? = null
		astroArticle?.apply {
			url = summaryJson?.getWikiPageUrl()?.toUri()
		}

		if (url == null) {
			url =
				"https://www.wikidata.org/wiki/${skyObject.wid}".toUri()
		}
		return url
	}

	fun String.getWikiPageUrl(): String? = runCatching {
		val json = JSONObject(this)

		val content = json.optJSONObject("content_urls") ?: return null

		content.optJSONObject("mobile")?.optString("page")
			?.takeIf { it.isNotBlank() }
			?: content.optJSONObject("desktop")?.optString("page")
				?.takeIf { it.isNotBlank() }

	}.getOrNull()

}