package net.osmand.shared.gpx.primitives

class Author : GpxExtensions {
	var name: String? = null
	var email: String? = null
	var link: Link? = null

	constructor()

	constructor(author: Author) {
		name = author.name
		email = author.email
		link = author.link?.let { Link(it) }
		copyExtensions(author)
	}
}
