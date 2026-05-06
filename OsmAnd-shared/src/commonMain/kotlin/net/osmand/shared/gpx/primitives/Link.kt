package net.osmand.shared.gpx.primitives

class Link : GpxExtensions {
    var href: String? = null
    var text: String? = null
    var type: String? = null

    constructor()

    constructor(href: String?) {
        this.href = href
    }

    constructor(href: String?, text: String?, type: String? = null) {
        this.href = href;
        this.text = text;
        this.type = type
    }

    constructor(link: Link) {
        href = link.href
        text = link.text
        type = link.type
        copyExtensions(link)
    }
}
