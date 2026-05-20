package net.osmand.plus.plugins.astronomy.views.contextmenu

import android.net.Uri
import androidx.core.net.toUri
import net.osmand.plus.plugins.astronomy.AstroArticle
import net.osmand.plus.plugins.astronomy.SkyObject
import net.osmand.plus.wikipedia.WikiAlgorithms

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
					showAllTitle = skyObject.niceName(),
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
		val hasOfflineArticle = astroArticle?.hasOfflineContent() == true
		val wikipediaUri = astroArticle?.getOnlineArticleUrl()?.let(Uri::parse)
		val hasWikipediaArticle = hasOfflineArticle || wikipediaUri != null
		val wikidataUri = obj.wid
			.takeIf { it.isNotBlank() && shouldOpenWikidata(obj, hasWikipediaArticle) }
			?.let(::buildWikidataUri)
		val readMoreUri = wikipediaUri ?: wikidataUri
		val linkType = when {
			hasWikipediaArticle -> AstroDescriptionLinkType.WIKIPEDIA
			wikidataUri != null -> AstroDescriptionLinkType.WIKIDATA
			else -> null
		}
		if (description.isBlank() && readMoreUri == null && !hasOfflineArticle) {
			return null
		}
		return AstroDescriptionCardItem(
			description = description,
			readMoreUri = readMoreUri,
			linkType = linkType,
			hasOfflineArticle = hasOfflineArticle
		)
	}

	private fun buildWikidataUri(wikidataId: String): Uri {
		return (WikiAlgorithms.WIKI_DATA_BASE_URL + Uri.encode(wikidataId)).toUri()
	}

	private fun shouldOpenWikidata(obj: SkyObject, hasWikipediaArticle: Boolean): Boolean {
		if (hasWikipediaArticle) {
			return false
		}
		return obj.hasMissingPrimaryName()
	}
}
