package net.osmand.shared.gpx.primitives

interface Linkable {
	val links: List<Link>?
	fun addLink(link: Link)
	fun removeLink(link: Link)
}
