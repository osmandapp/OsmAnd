package net.osmand.shared.osm

/**
 * The languages a tag can be suffixed with, as `MapRenderingTypes` in OsmAnd-java lists them.
 *
 * Only the list is copied: the rest of `MapRenderingTypes` reads rendering_types.xml and belongs
 * to the map creator. The order matters, because poi_types.xml expands a `lang="true"` type into
 * one type per language in this order, and the obf files number them by that position.
 */
object MapRenderingTypes {

	val langs = arrayOf(
		"af", "als", "ar", "az", "be", "bg", "bn", "bpy", "br", "bs", "ca", "ceb", "ckb", "crh",
		"cs", "cy", "da", "de", "el", "eo", "es", "et", "eu", "fa", "fi", "fr", "fy", "ga", "gl",
		"he", "hi", "hsb", "hr", "ht", "hu", "hy", "id", "is", "it", "ja", "ka", "kk", "kn", "ko",
		"ku", "la", "lb", "lo", "lt", "lv", "mi", "mk", "ml", "mr", "ms", "nds", "new", "nl", "nn",
		"no", "nv", "oc", "os", "pl", "pms", "pt", "ro", "ru", "sat", "sc", "sh", "sk", "sl", "sq",
		"sr", "sr-latn", "sv", "sw", "ta", "te", "th", "tl", "tr", "uk", "vi", "vo", "zh",
		"zh-hans", "zh-hant"
	)

	/** Java keeps this in a `TreeSet`; common Kotlin has none, so it is sorted by hand. */
	val langsSet: Set<String> = LinkedHashSet(langs.sorted())
}
