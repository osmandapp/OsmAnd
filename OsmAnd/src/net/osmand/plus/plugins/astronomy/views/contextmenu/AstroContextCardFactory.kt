package net.osmand.plus.plugins.astronomy.views.contextmenu

import android.net.Uri
import net.osmand.plus.plugins.astronomy.AstroArticle
import net.osmand.plus.plugins.astronomy.SkyObject
import net.osmand.plus.wikipedia.WikiAlgorithms
import org.json.JSONObject
import androidx.core.net.toUri

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
			buildDescriptionCardItem(skyObject, article)?.let(::add)
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

	private fun buildDescriptionCardItem(
		obj: SkyObject,
		astroArticle: AstroArticle?
	): AstroDescriptionCardItem? {
		val description = astroArticle?.description?.trim().orEmpty()
		val wikipediaUri = astroArticle?.summaryJson
			?.let(::extractWikiPageUrl)
			?.let(Uri::parse)
		val wikidataUri = obj.wid
			.takeIf { it.isNotBlank() && shouldOpenWikidata(obj, wikipediaUri) }
			?.let(::buildWikidataUri)
		val readMoreUri = wikipediaUri ?: wikidataUri
		val linkType = when {
			wikipediaUri != null -> AstroDescriptionLinkType.WIKIPEDIA
			wikidataUri != null -> AstroDescriptionLinkType.WIKIDATA
			else -> null
		}
		if (description.isBlank() && readMoreUri == null) {
			return null
		}
		return AstroDescriptionCardItem(
			description = description,
			readMoreUri = readMoreUri,
			linkType = linkType
		)
	}

	private fun buildWikidataUri(wikidataId: String): Uri {
		return (WikiAlgorithms.WIKI_DATA_BASE_URL + Uri.encode(wikidataId)).toUri()
	}

	private fun shouldOpenWikidata(obj: SkyObject, wikipediaUri: Uri?): Boolean {
		if (wikipediaUri != null) {
			return false
		}
		val displayName = obj.localizedName?.trim()
			?.takeIf { it.isNotBlank() }
			?: obj.name.trim()
		return displayName.isBlank() || displayName.equals(obj.wid, ignoreCase = true)
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
