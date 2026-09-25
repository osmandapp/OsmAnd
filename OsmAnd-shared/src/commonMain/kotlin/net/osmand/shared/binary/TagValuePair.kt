package net.osmand.shared.binary

import kotlin.jvm.JvmField

/**
 * A tag and its value as an obf file stores them in an encoding table, plus the attribute id the
 * table gave the pair. HH network points carry the tags the vehicle profile filters on as a list
 * of these.
 *
 * A copy of `BinaryMapIndexReader.TagValuePair` in OsmAnd-java, which stays there for android
 * and tools; this copy is for iOS.
 */
class TagValuePair(
	@JvmField var tag: String?,
	@JvmField var value: String?,
	@JvmField var additionalAttribute: Int
) {

	fun isAdditional(): Boolean = additionalAttribute % 2 == 1

	override fun hashCode(): Int {
		val prime = 31
		var result = 1
		result = prime * result + additionalAttribute
		result = prime * result + (tag?.hashCode() ?: 0)
		result = prime * result + (value?.hashCode() ?: 0)
		return result
	}

	fun toSimpleString(): String {
		if (value == null) {
			return tag ?: "null"
		}
		return "$tag-$value"
	}

	override fun toString(): String = "TagValuePair : $tag - $value"

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other == null) return false
		if (other !is TagValuePair) return false
		if (additionalAttribute != other.additionalAttribute) return false
		if (tag != other.tag) return false
		if (value != other.value) return false
		return true
	}
}
