package net.osmand.shared.gpx.primitives

interface Linkable {
	var links: List<Link>?
	fun addLink(link: Link)
	fun removeLink(link: Link)
}
