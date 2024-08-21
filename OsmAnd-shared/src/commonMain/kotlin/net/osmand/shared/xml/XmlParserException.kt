package net.osmand.shared.xml

class XmlParserException : Exception {
	constructor(message: String?) : super(message)
	constructor(message: String?, e: Exception) : super(message, e)
}