package net.osmand.shared.gpx.primitives

class Author : GpxExtensions {
	var name: String? = null
	var email: String? = null
	var link: String? = null
	var linkText: String? = null

	constructor()

	constructor(author: Author) {
		name = author.name
		email = author.email
		link = author.link
		linkText = author.linkText
		copyExtensions(author)
	}
}
