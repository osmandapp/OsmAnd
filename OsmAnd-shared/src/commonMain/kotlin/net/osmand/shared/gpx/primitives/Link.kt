package net.osmand.shared.gpx.primitives

class Link : GpxExtensions {
    var href: String? = null
    var text: String? = null

    constructor()

    constructor(href: String?) {
        this.href = href;
    }

    constructor(link: Link) {
        href = link.href
        text = link.text
        copyExtensions(link)
    }
}
