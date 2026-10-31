package net.osmand.shared.binary

/**
 * What a reader of an obf file is for: each gets a reader of its own, see [BinaryMapReaderResource].
 * A copy of `ResourceManager.BinaryMapReaderResourceType` in the android app.
 */
enum class BinaryMapReaderResourceType {
	POI,
	REVERSE_GEOCODING,
	STREET_LOOKUP,
	TRANSPORT,
	ADDRESS,
	QUICK_SEARCH,
	ROUTING,
	TRANSPORT_ROUTING
}
