package net.osmand.plus.search.history

import net.osmand.data.City.CityType
import net.osmand.search.core.ObjectType
import net.osmand.util.Algorithms

data class HistoryItemMetadata(
	val objectType: ObjectType? = null,
	val cityType: CityType? = null,
	val displayName: String? = null,
	val poiCategoryKey: String? = null,
	val poiSubtypeKey: String? = null,
	val typeName: String? = null,
	val address: String? = null,
	val relatedObjectName: String? = null,
	val openingHours: String? = null,
	val alternateName: String? = null,
	val photoUrl: String? = null,
	val osmId: Long? = null
) {

	fun hasMetadata(): Boolean {
		return objectType != null || cityType != null || !Algorithms.isEmpty(displayName)
				|| !Algorithms.isEmpty(typeName) || !Algorithms.isEmpty(address)
				|| !Algorithms.isEmpty(relatedObjectName) || !Algorithms.isEmpty(openingHours)
				|| !Algorithms.isEmpty(alternateName) || !Algorithms.isEmpty(photoUrl)
				|| osmId != null || !Algorithms.isEmpty(poiCategoryKey)
				|| !Algorithms.isEmpty(poiSubtypeKey)
	}

	fun fillMissingFrom(metadata: HistoryItemMetadata): HistoryItemMetadata {
		return HistoryItemMetadata(
			objectType = objectType ?: metadata.objectType,
			cityType = cityType ?: metadata.cityType,
			displayName = if (!Algorithms.isEmpty(displayName)) displayName else metadata.displayName,
			poiCategoryKey = if (!Algorithms.isEmpty(poiCategoryKey)) poiCategoryKey else metadata.poiCategoryKey,
			poiSubtypeKey = if (!Algorithms.isEmpty(poiSubtypeKey)) poiSubtypeKey else metadata.poiSubtypeKey,
			typeName = if (!Algorithms.isEmpty(typeName)) typeName else metadata.typeName,
			address = if (!Algorithms.isEmpty(address)) address else metadata.address,
			relatedObjectName = if (!Algorithms.isEmpty(relatedObjectName)) relatedObjectName else metadata.relatedObjectName,
			openingHours = if (!Algorithms.isEmpty(openingHours)) openingHours else metadata.openingHours,
			alternateName = if (!Algorithms.isEmpty(alternateName)) alternateName else metadata.alternateName,
			photoUrl = if (!Algorithms.isEmpty(photoUrl)) photoUrl else metadata.photoUrl,
			osmId = osmId ?: metadata.osmId
		)
	}

	fun withObjectType(objectType: ObjectType?): HistoryItemMetadata = copy(objectType = objectType)

	fun withCityType(cityType: CityType?): HistoryItemMetadata = copy(cityType = cityType)

	fun withDisplayName(displayName: String?): HistoryItemMetadata = copy(displayName = displayName)

	fun withPoiCategoryKey(poiCategoryKey: String?): HistoryItemMetadata = copy(poiCategoryKey = poiCategoryKey)

	fun withPoiSubtypeKey(poiSubtypeKey: String?): HistoryItemMetadata = copy(poiSubtypeKey = poiSubtypeKey)

	fun withTypeName(typeName: String?): HistoryItemMetadata = copy(typeName = typeName)

	fun withAddress(address: String?): HistoryItemMetadata = copy(address = address)

	fun withRelatedObjectName(relatedObjectName: String?): HistoryItemMetadata = copy(relatedObjectName = relatedObjectName)

	fun withOpeningHours(openingHours: String?): HistoryItemMetadata = copy(openingHours = openingHours)

	fun withAlternateName(alternateName: String?): HistoryItemMetadata = copy(alternateName = alternateName)

	fun withPhotoUrl(photoUrl: String?): HistoryItemMetadata = copy(photoUrl = photoUrl)

	fun withOsmId(osmId: Long?): HistoryItemMetadata = copy(osmId = osmId)
}
