package net.osmand.shared.wiki

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import net.osmand.shared.data.KQuadRect
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.PlatformUtil
import net.osmand.shared.util.UrlEncoder
import net.osmand.shared.wiki.WikiHelper.addFile

object WikiCoreHelper {

	private val LOG = LoggerFactory.getLogger("WikiCoreHelper")

	const val USE_OSMAND_WIKI_API = true

	private const val WIKIMEDIA_API_ENDPOINT = "https://commons.wikimedia.org/w/api.php"
	private const val WIKIDATA_API_ENDPOINT = "https://www.wikidata.org/w/api.php"
	private const val WIKIDATA_ACTION = "?action=wbgetclaims&property=P18&entity="
	private const val WIKIMEDIA_ACTION = "?action=query&list=categorymembers&cmtitle="
	private const val CM_LIMIT = "&cmlimit=100"
	private const val FORMAT_JSON = "&format=json"

	const val WIKIMEDIA_FILE = "File:"
	const val WIKIMEDIA_CATEGORY = "Category:"
	private const val THUMB_SIZE = 480
	private const val ICON_SIZE = 64

	const val OSMAND_API_ENDPOINT = "https://osmand.net/api/"
	const val OSMAND_SEARCH_ENDPOINT = "https://osmand.net/search/"
	private const val WIKI_PLACE_ACTION = "wiki_place?"
	private const val GET_WIKI_DATA_ACTION = "get-wiki-data?"
	private const val DEPT_CAT_LIMIT = 1

	private val IMAGE_EXTENSIONS = listOf(".jpeg", ".jpg", ".png", ".gif")

	@OptIn(ExperimentalSerializationApi::class)
	private val jsonParser = Json {
		ignoreUnknownKeys = true
		coerceInputValues = true
		explicitNulls = false
		isLenient = true
	}

	fun getExploreImageList(
		mapRect: KQuadRect,
		zoom: Int,
		langs: String
	): List<OsmandApiFeatureData> {
		val wikiImages = mutableListOf<OsmandApiFeatureData>()
		val baseApiActionUrl = "$OSMAND_SEARCH_ENDPOINT$GET_WIKI_DATA_ACTION"

		val nw = "${mapRect.top.toString()},${mapRect.left.toString()}"
		val se = "${mapRect.bottom.toString()},${mapRect.right.toString()}"

		val url = StringBuilder(baseApiActionUrl).apply {
			append("northWest=").append(UrlEncoder.encode(nw))
			append("&southEast=").append(UrlEncoder.encode(se))
			append("&zoom=").append(zoom)
			append("&lang=").append(langs)
			append("&filters=")
		}.toString()

		LOG.debug("Download images $url")
		getNearbyImagesOsmAndAPIRequest(url, wikiImages)
		return wikiImages
	}

	fun getWikiImageList(
		tags: Map<String, String>,
		listener: NetworkResponseListener? = null
	): List<WikiImage> {
		val wikiTagData = WikiHelper.extractWikiTagData(tags)
		val wikidataId = wikiTagData.wikidataId
		val wikiCategory = wikiTagData.wikiCategory
		val wikiTitle = wikiTagData.wikiTitle

		val wikiImages = wikiTagData.wikiImages
		if (USE_OSMAND_WIKI_API) {
			val params = mutableListOf<String>()

			wikidataId?.takeIf { it.isNotEmpty() }?.let {
				params.add("article=${UrlEncoder.encode(it)}")
			}
			wikiCategory?.takeIf { it.isNotEmpty() }?.let {
				params.add("category=${UrlEncoder.encode(it)}")
			}
			wikiTitle?.takeIf { it.isNotEmpty() }?.let {
				params.add("wiki=${UrlEncoder.encode(it)}")
			}
			if (params.isNotEmpty()) {
				val urlParams = params.joinToString("&")
				val finalUrl = "$OSMAND_API_ENDPOINT$WIKI_PLACE_ACTION$urlParams&addMetaData=true"
				getImagesOsmAndAPIRequestV2(finalUrl, wikiImages, listener)
			}
		} else {
			wikidataId?.takeIf { it.isNotEmpty() }?.let { getWikidataImageWikidata(it, wikiImages) }
			wikiCategory?.takeIf { it.isNotEmpty() }
				?.let { getWikimediaImageCategory(it, wikiImages, 0) }
		}
		return wikiImages
	}

	fun getImagesFromJson(json: String, wikiImages: MutableList<WikiImage>): List<WikiImage> {
		return try {
			val response = jsonParser.decodeFromString(OsmandAPIResponseV2.serializer(), json)
			createWikiImages(response, wikiImages)
		} catch (e: Exception) {
			LOG.error("Failed to parse JSON from string", e)
			wikiImages
		}
	}

	private fun getNearbyImagesOsmAndAPIRequest(
		url: String,
		wikiImages: MutableList<OsmandApiFeatureData>
	) {
		val response = sendWikipediaApiRequest<OsmandAPIFeaturesResponse>(url, useGzip = true)
		response?.features?.let { wikiImages.addAll(it) }
	}

	private fun getImagesOsmAndAPIRequestV2(
		url: String,
		wikiImages: MutableList<WikiImage>,
		listener: NetworkResponseListener?
	): List<WikiImage> {
		val response =
			sendWikipediaApiRequest<OsmandAPIResponseV2>(url, useGzip = false, listener = listener)
		return createWikiImages(response, wikiImages)
	}

	private fun createWikiImages(
		response: OsmandAPIResponseV2?,
		wikiImages: MutableList<WikiImage>
	): List<WikiImage> {
		response?.images?.forEach { obj ->
			if (obj is JsonObject) {
				val wikiImage = parseImageDataWithMetaData(obj)
				if (wikiImage != null && isUrlFileImage(wikiImage)) {
					wikiImages.add(wikiImage)
				}
			}
		}
		return wikiImages
	}

	private fun getWikimediaImageCategory(
		categoryName: String,
		wikiImages: MutableList<WikiImage>,
		depth: Int
	): List<WikiImage> {
		val name = if (categoryName.startsWith(WIKIMEDIA_CATEGORY)) categoryName else WIKIMEDIA_CATEGORY + categoryName
		val url = "$WIKIMEDIA_API_ENDPOINT$WIKIMEDIA_ACTION$name$CM_LIMIT$FORMAT_JSON"

		val response = sendWikipediaApiRequest<WikimediaResponse>(url, useGzip = false)
		val members = response?.query?.categorymembers
		if (!members.isNullOrEmpty()) {
			val subCategories = mutableListOf<String>()
			for (cm in members) {
				val memberTitle = cm.title ?: continue
				when {
					memberTitle.startsWith(WIKIMEDIA_CATEGORY) -> subCategories.add(memberTitle)
					memberTitle.startsWith(WIKIMEDIA_FILE) -> addFile(wikiImages, memberTitle)
				}
			}
			if (depth < DEPT_CAT_LIMIT) {
				for (subCategory in subCategories) {
					getWikimediaImageCategory(subCategory, wikiImages, depth + 1)
				}
			}
		}
		return wikiImages
	}

	fun getWikidataImageWikidata(
		wikidataId: String,
		wikiImages: MutableList<WikiImage>
	): List<WikiImage> {
		val url = "$WIKIDATA_API_ENDPOINT$WIKIDATA_ACTION$wikidataId$FORMAT_JSON"
		val response = sendWikipediaApiRequest<WikidataResponse>(url, useGzip = false)

		response?.claims?.p18?.forEach { p18 ->
			p18.mainsnak?.datavalue?.value?.let { imageFileName ->
				val wikiImage = WikiHelper.getImageData(imageFileName)
				wikiImages.add(wikiImage)
			}
		}
		return wikiImages
	}

	private fun parseImageDataWithMetaData(image: JsonObject): WikiImage? {
		val imageUrl = image.getString("image") ?: return null
		val wikiImage = parseImageDataFromFile(imageUrl) ?: return null

		val metadata = wikiImage.metadata
		image.getString("date")?.let { metadata.date = it }
		image.getString("author")?.let { metadata.author = it }
		image.getString("license")?.let { metadata.license = it }

		val mediaIdLong = image.getString("mediaId")?.toLongOrNull() ?: -1L
		wikiImage.setMediaId(mediaIdLong)

		return wikiImage
	}

	private fun parseImageDataFromFile(imageUrl: String): WikiImage? {
		val imageHiResUrl = imageUrl.replace("%20", " ").replace(" ", "_")
		val decodedUrl = UrlEncoder.decode(imageUrl)

		val imageFileName = KAlgorithms.getFileWithoutDirs(decodedUrl)
		val imageName = KAlgorithms.getFileNameWithoutExtension(decodedUrl)

		val imageStubUrl = "$imageHiResUrl?width=$THUMB_SIZE"
		val imageIconUrl = "$imageHiResUrl?width=$ICON_SIZE"

		return WikiImage(imageFileName, imageName, imageStubUrl, imageHiResUrl, imageIconUrl)
	}

	private fun isUrlFileImage(wikiImage: WikiImage): Boolean {
		val path = wikiImage.imageHiResUrl
		val lastDot = path.lastIndexOf('.')
		if (lastDot != -1) {
			val ext = path.substring(lastDot).lowercase()
			return IMAGE_EXTENSIONS.contains(ext)
		}
		return false
	}

	private inline fun <reified T> sendWikipediaApiRequest(
		url: String,
		useGzip: Boolean,
		listener: NetworkResponseListener? = null
	): T? {
		return try {
			val response = PlatformUtil.getNetworkAPI().sendGetRequest(url, null, useGzip)
			if (response.error == null && response.response != null) {
				listener?.onGetRawResponse(response.response)
				jsonParser.decodeFromString<T>(response.response)
			} else {
				LOG.error("Request failed: ${response.error}")
				null
			}
		} catch (e: Exception) {
			LOG.error(e.message ?: "Request/parse failed", e)
			null
		}
	}

	private fun JsonObject.getString(key: String): String? {
		val value = this[key] ?: return null
		return when (value) {
			is JsonNull -> null
			is JsonPrimitive -> value.contentOrNull
			else -> value.toString()
		}
	}

	@Serializable
	data class OsmandAPIFeaturesResponse(val features: List<OsmandApiFeatureData>? = null)

	@Serializable
	data class OsmandAPIResponseV2(@SerialName("features-v2") val images: JsonArray? = null)

	@Serializable
	data class OsmandApiFeatureData(
		val properties: WikiDataProperties? = null,
		val geometry: WikiDataGeometry? = null
	)

	@Serializable
	data class WikiDataGeometry(val coordinates: DoubleArray? = null)

	@Serializable
	data class WikiDataProperties(
		val id: String? = null,
		val photoId: String? = null,
		val photoTitle: String? = null,
		val wikiTitle: String? = null,
		val poitype: String? = null,
		val poisubtype: String? = null,
		val catId: String? = null,
		val catTitle: String? = null,
		val depId: String? = null,
		val depTitle: String? = null,
		val wikiLang: String? = null,
		val wikiDesc: String? = null,
		val wikiLangs: String? = null,
		val wikiLangViews: String? = null,
		val osmid: Long? = null,
		val elo: Double? = null,
		val osmtype: Int = 0
	)

	@Serializable
	data class WikidataResponse(val claims: Claims? = null)

	@Serializable
	data class Claims(@SerialName("P18") val p18: List<P18>? = null)

	@Serializable
	data class P18(val mainsnak: Mainsnak? = null)

	@Serializable
	data class Mainsnak(val datavalue: Datavalue? = null, val datatype: String? = null)

	@Serializable
	data class Datavalue(val value: String? = null, val type: String? = null)

	@Serializable
	data class WikimediaResponse(val query: Query? = null)

	@Serializable
	data class Query(val categorymembers: List<Categorymember>? = null)

	@Serializable
	data class Categorymember(val title: String? = null)

	interface NetworkResponseListener {
		fun onGetRawResponse(response: String)
	}
}
