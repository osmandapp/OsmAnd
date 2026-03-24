package net.osmand.plus.plugins.astronomy.views.contextmenu

import android.net.Uri
import net.osmand.plus.plugins.astronomy.AstroArticle
import net.osmand.plus.plugins.astronomy.SkyObject
import org.json.JSONObject
import java.util.Locale

class AstroContextCardFactory {

	fun buildCards(
		skyObject: SkyObject?,
		article: AstroArticle?,
		uiState: AstroContextUiState,
		knowledgeItem: AstroKnowledgeCardItem?,
		visibilityItem: AstroVisibilityCardItem?,
		scheduleItem: AstroScheduleCardItem?
	): List<AstroContextMenuItem> {
		if (skyObject == null) {
			return emptyList()
		}
		return buildList(6) {
			knowledgeItem?.let(::add)
			add(
				AstroDescriptionCardItem(
					description = article?.description.orEmpty(),
					wikiUri = buildWikiUri(skyObject, article)
				)
			)
			skyObject.catalogs.takeIf { it.isNotEmpty() }?.let { catalogs ->
				add(
					AstroCatalogsCardItem(
						catalogs = catalogs,
						expanded = uiState.catalogsExpanded
					)
				)
			}
			add(
				AstroGalleryCardItem(
					wid = skyObject.wid,
					showAllTitle = skyObject.localizedName ?: skyObject.name,
					state = uiState.galleryState
				)
			)
			visibilityItem?.let(::add)
			scheduleItem?.let(::add)
		}
	}

	private fun buildWikiUri(obj: SkyObject, astroArticle: AstroArticle?): Uri {
		val pageUrl = astroArticle?.summaryJson?.let(::extractWikiPageUrl)
		return (pageUrl ?: buildWikipediaArticleUrl(obj, astroArticle)).let(Uri::parse)
	}

	private fun buildWikipediaArticleUrl(obj: SkyObject, astroArticle: AstroArticle?): String {
		val language = astroArticle?.lang?.takeIf { it.isNotBlank() } ?: "en"
		val title = astroArticle?.title?.takeIf { it.isNotBlank() } ?: obj.name
		val normalizedTitle = title.replace(' ', '_')
		return "https://${language.lowercase(Locale.US)}.wikipedia.org/wiki/${Uri.encode(normalizedTitle)}"
	}

	private fun extractWikiPageUrl(summaryJson: String): String? = runCatching {
		val json = JSONObject(summaryJson)
		val content = json.optJSONObject("content_urls") ?: return null
		content.optJSONObject("mobile")?.optString("page")
			?.takeIf { it.isNotBlank() }
			?: content.optJSONObject("desktop")?.optString("page")
				?.takeIf { it.isNotBlank() }
	}.getOrNull()
}
