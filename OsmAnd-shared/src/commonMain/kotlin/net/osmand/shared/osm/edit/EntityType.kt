package net.osmand.shared.osm.edit

/**
 * Based on `Entity.EntityType` from OsmAnd-java. The enclosing `Entity` class and its `valueOf`
 * helpers are not implemented here.
 */
enum class EntityType {
	NODE,
	WAY,
	RELATION,
	WAY_BOUNDARY
}
