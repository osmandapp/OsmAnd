package net.osmand.shared.wiki

object WikiMetadata {

    private const val UNKNOWN = "Unknown"

    class Metadata {
        var date: String? = null
        var author: String? = null
        var license: String? = null
        var description: String? = null
    }

    fun updateMetadata(metadataMap: Map<String, Map<String, String>>, metadata: Metadata) {
        metadataMap.values.forEach { details ->
            details["date"]?.takeIf { it.isNotEmpty() && (metadata.date.isNullOrEmpty() || metadata.date == UNKNOWN) }?.let {
                metadata.date = it
            }
            details["license"]?.takeIf { it.isNotEmpty() && (metadata.license.isNullOrEmpty() || metadata.license == UNKNOWN) }?.let {
                metadata.license = it
            }
            details["author"]?.takeIf { it.isNotEmpty() && (metadata.author.isNullOrEmpty() || metadata.author == UNKNOWN) }?.let {
                metadata.author = it
            }
            details["description"]?.takeIf { it.isNotEmpty() && (metadata.description.isNullOrEmpty() || metadata.description == UNKNOWN) }?.let {
                metadata.description = it
            }
        }
    }


}