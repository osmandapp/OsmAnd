package net.osmand.shared.data

import net.osmand.shared.osm.PoiCategory
import net.osmand.shared.osm.PoiType
import net.osmand.shared.util.KAlgorithms
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

class AmenityTagEntry private constructor(builder: Builder) {

	enum class CollapsableEntryType {
		NONE, PLAIN, POI_TYPE_GROUP, ELEVATION_PILLS, OPENING_HOURS
	}

	@JvmField val key: String = builder.getKey()
	@JvmField val value: String? = builder.getValue()
	@JvmField val resolvedType: AdditionalInfoBundle.ResolvedPoiType? = builder.resolvedType
	@JvmField val iconId: Int = builder.getIconId()
	@JvmField val iconNameCandidates: List<String> = builder.iconNameCandidates
	@JvmField val fallbackIconId: Int = builder.fallbackIconId
	@JvmField val textPrefix: String? = builder.getTextPrefix()
	@JvmField val text: String? = builder.getText()
	@JvmField val hiddenUrl: String? = builder.getHiddenUrl()
	@JvmField val collapsableEntries: List<AmenityTagEntry>? = builder.collapsableEntries
	@JvmField val collapsableEntryType: CollapsableEntryType = builder.getCollapsableEntryType()
	@JvmField val collapsablePoiTypes: List<PoiType>? = builder.collapsablePoiTypes
	@JvmField val collapsableCategory: PoiCategory? = builder.collapsableCategory
	@JvmField val poiAdditional: Boolean = builder.poiAdditional
	@JvmField val collapsable: Boolean = builder.getCollapsableEntryType() != CollapsableEntryType.NONE
	@JvmField val textColor: Int = builder.textColor
	@JvmField val isWiki: Boolean = builder.isWiki()
	@JvmField val isText: Boolean = builder.isText()
	@JvmField val isDescription: Boolean = builder.isDescription()
	@JvmField val needLinks: Boolean = builder.needLinks
	@JvmField val isPhoneNumber: Boolean = builder.isPhoneNumber
	@JvmField val isUrl: Boolean = builder.isUrl
	@JvmField val order: Int = builder.order
	@JvmField val name: String? = builder.name
	@JvmField val matchWidthDivider: Boolean = builder.matchWidthDivider
	@JvmField val textLinesLimit: Int = builder.textLinesLimit

	class Builder(private val key: String) {
		private var value: String? = null
		internal var resolvedType: AdditionalInfoBundle.ResolvedPoiType? = null
		private var iconId: Int = 0
		internal var iconNameCandidates: List<String> = emptyList()
		internal var fallbackIconId: Int = 0
		private var textPrefix: String? = ""
		private var text: String? = null
		private var hiddenUrl: String? = null
		internal var collapsableEntries: List<AmenityTagEntry>? = null
		private var collapsableEntryType: CollapsableEntryType = CollapsableEntryType.NONE
		internal var collapsablePoiTypes: List<PoiType>? = null
		internal var collapsableCategory: PoiCategory? = null
		internal var poiAdditional: Boolean = false
		internal var textColor: Int = 0
		private var isWiki: Boolean = false
		private var isText: Boolean = false
		private var isDescription: Boolean = false
		internal var needLinks: Boolean = false
		internal var isPhoneNumber: Boolean = false
		internal var isUrl: Boolean = false
		internal var order: Int = 0
		internal var name: String? = null
		internal var matchWidthDivider: Boolean = false
		internal var textLinesLimit: Int = 0

		fun setValue(value: String?): Builder = apply { this.value = value }

		fun setResolvedType(resolvedType: AdditionalInfoBundle.ResolvedPoiType?): Builder =
			apply { this.resolvedType = resolvedType }

		fun setIconId(iconId: Int): Builder = apply { this.iconId = iconId }

		fun setIconNameCandidates(iconNameCandidates: List<String>): Builder =
			apply { this.iconNameCandidates = iconNameCandidates }

		fun setFallbackIconId(fallbackIconId: Int): Builder = apply { this.fallbackIconId = fallbackIconId }

		fun setTextPrefix(textPrefix: String?): Builder = apply { this.textPrefix = textPrefix }

		fun setText(text: String?): Builder = apply { this.text = text }

		fun setHiddenUrl(hiddenUrl: String?): Builder = apply { this.hiddenUrl = hiddenUrl }

		fun setCollapsableEntries(collapsableEntries: List<AmenityTagEntry>?): Builder = apply {
			this.collapsableEntries = collapsableEntries
			if (collapsableEntryType == CollapsableEntryType.NONE && collapsableEntries != null) {
				collapsableEntryType = CollapsableEntryType.PLAIN
			}
		}

		fun setCollapsableEntryType(collapsableEntryType: CollapsableEntryType): Builder =
			apply { this.collapsableEntryType = collapsableEntryType }

		fun setCollapsablePoiTypes(collapsablePoiTypes: List<PoiType>?): Builder =
			apply { this.collapsablePoiTypes = collapsablePoiTypes }

		fun setCollapsableCategory(collapsableCategory: PoiCategory?): Builder =
			apply { this.collapsableCategory = collapsableCategory }

		fun setPoiAdditional(poiAdditional: Boolean): Builder = apply { this.poiAdditional = poiAdditional }

		fun setTextColor(color: Int): Builder = apply { this.textColor = color }

		fun setIsWiki(wiki: Boolean): Builder = apply { this.isWiki = wiki }

		fun setIsText(textFlag: Boolean): Builder = apply { this.isText = textFlag }

		fun setIsDescription(isDescription: Boolean): Builder = apply { this.isDescription = isDescription }

		fun setNeedLinks(needLinks: Boolean): Builder = apply { this.needLinks = needLinks }

		fun setIsPhoneNumber(isPhoneNumber: Boolean): Builder = apply { this.isPhoneNumber = isPhoneNumber }

		fun setIsUrl(isUrl: Boolean): Builder = apply { this.isUrl = isUrl }

		fun setOrder(order: Int): Builder = apply { this.order = order }

		fun setName(name: String?): Builder = apply { this.name = name }

		fun setMatchWidthDivider(match: Boolean): Builder = apply { this.matchWidthDivider = match }

		fun setTextLinesLimit(limit: Int): Builder = apply { this.textLinesLimit = limit }

		fun setTextIfNotPresent(text: String?): Builder = apply { if (!hasText()) setText(text) }

		fun setTextPrefixIfNotPresent(textPrefix: String?): Builder =
			apply { if (!hasTextPrefix()) setTextPrefix(textPrefix) }

		fun getKey(): String = key

		fun getValue(): String? = value

		fun getIconId(): Int = iconId

		fun hasIcon(): Boolean = iconId != 0

		fun getTextPrefix(): String? = textPrefix

		fun hasTextPrefix(): Boolean = !KAlgorithms.isEmpty(textPrefix)

		fun getText(): String? = text

		fun hasText(): Boolean = !KAlgorithms.isEmpty(text)

		fun getHiddenUrl(): String? = hiddenUrl

		fun hasHiddenUrl(): Boolean = !KAlgorithms.isEmpty(hiddenUrl)

		fun getCollapsableEntryType(): CollapsableEntryType = collapsableEntryType

		fun isWiki(): Boolean = isWiki

		fun isText(): Boolean = isText

		fun isDescription(): Boolean = isDescription

		fun isNeedLinks(): Boolean = needLinks && collapsableEntryType == CollapsableEntryType.NONE

		fun build(): AmenityTagEntry = AmenityTagEntry(this)

		companion object {
			@JvmStatic
			fun from(entry: AmenityTagEntry): Builder = Builder(entry.key)
				.setValue(entry.value)
				.setResolvedType(entry.resolvedType)
				.setIconId(entry.iconId)
				.setIconNameCandidates(entry.iconNameCandidates)
				.setFallbackIconId(entry.fallbackIconId)
				.setTextPrefix(entry.textPrefix)
				.setText(entry.text)
				.setHiddenUrl(entry.hiddenUrl)
				.setCollapsableEntries(entry.collapsableEntries)
				.setCollapsableEntryType(entry.collapsableEntryType)
				.setCollapsablePoiTypes(entry.collapsablePoiTypes)
				.setCollapsableCategory(entry.collapsableCategory)
				.setPoiAdditional(entry.poiAdditional)
				.setTextColor(entry.textColor)
				.setIsWiki(entry.isWiki)
				.setIsText(entry.isText)
				.setIsDescription(entry.isDescription)
				.setNeedLinks(entry.needLinks)
				.setIsPhoneNumber(entry.isPhoneNumber)
				.setIsUrl(entry.isUrl)
				.setOrder(entry.order)
				.setName(entry.name)
				.setMatchWidthDivider(entry.matchWidthDivider)
				.setTextLinesLimit(entry.textLinesLimit)
		}
	}
}
