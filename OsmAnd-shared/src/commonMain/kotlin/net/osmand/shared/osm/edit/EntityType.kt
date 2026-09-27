package net.osmand.shared.osm.edit

/**
 * A copy of `Entity.EntityType` in OsmAnd-java, which stays there for android and tools; this copy
 * is for iOS. Java nests it in `Entity`, which is not copied, and so are its `valueOf` helpers.
 */
enum class EntityType {
	NODE,
	WAY,
	RELATION,
	WAY_BOUNDARY
}
