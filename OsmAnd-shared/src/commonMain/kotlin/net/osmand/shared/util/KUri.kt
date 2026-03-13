package net.osmand.shared.util

data class KUri(
    val original: String,
    val scheme: String?,
    val host: String?,
    val path: String?,
    val query: String?,
    val rawQuery: String?,
    val fragment: String?,
    val isOpaque: Boolean,
    val schemeSpecificPart: String?,
    val rawSchemeSpecificPart: String
) {
    companion object {
        fun create(uriString: String): KUri? {
            val schemeMatch = Regex("^([A-Za-z][A-Za-z0-9+\\-.]*):(.*)$").find(uriString) ?: return null
            val scheme = schemeMatch.groupValues[1]
            val afterScheme = schemeMatch.groupValues[2]

            val rawSchemeSpecificPart = afterScheme
            val schemeSpecificPart: String

            val isOpaque = !afterScheme.startsWith("//")
            if (isOpaque) {
                schemeSpecificPart = decodePlusOutsideQuery(rawSchemeSpecificPart)
                return KUri(
                    original = uriString,
                    scheme = scheme,
                    host = null,
                    path = null,
                    query = null,
                    rawQuery = null,
                    fragment = null,
                    isOpaque = true,
                    schemeSpecificPart = schemeSpecificPart,
                    rawSchemeSpecificPart = rawSchemeSpecificPart
                )
            }

            var rest = afterScheme.substring(2)
            var fragment: String? = null
            val hashIndex = rest.indexOf('#')
            if (hashIndex >= 0) {
                fragment = rest.substring(hashIndex + 1)
                rest = rest.substring(0, hashIndex)
            }

            var rawQuery: String? = null
            val qIndex = rest.indexOf('?')
            if (qIndex >= 0) {
                rawQuery = rest.substring(qIndex + 1)
                rest = rest.substring(0, qIndex)
            }

            val slashIndex = rest.indexOf('/')
            val authority: String
            val path: String
            if (slashIndex >= 0) {
                authority = rest.substring(0, slashIndex)
                path = rest.substring(slashIndex)
            } else {
                authority = rest
                path = ""
            }

            val host = authority.substringAfterLast("@", authority).substringBefore(":", authority.substringAfterLast("@", authority))
            schemeSpecificPart = buildString {
                append("//")
                append(authority)
                append(path)
                if (rawQuery != null) append("?").append(rawQuery)
                if (fragment != null) append("#").append(fragment)
            }

            return KUri(
                original = uriString,
                scheme = scheme,
                host = if (host.isEmpty()) null else host,
                path = decodePlusOutsideQuery(path),
                query = rawQuery?.let { decodeQueryPreservingSeparators(it) },
                rawQuery = rawQuery,
                fragment = fragment?.let { decodePlusOutsideQuery(it) },
                isOpaque = false,
                schemeSpecificPart = decodePlusOutsideQuery(schemeSpecificPart),
                rawSchemeSpecificPart = rawSchemeSpecificPart
            )
        }

        fun urlDecode(value: String): String {
            if (value.isEmpty()) return value

            val bytes = mutableListOf<Byte>()
            val out = StringBuilder()
            var i = 0

            fun flushBytes() {
                if (bytes.isNotEmpty()) {
                    out.append(bytes.toByteArray().decodeToString())
                    bytes.clear()
                }
            }

            while (i < value.length) {
                val c = value[i]
                when {
                    c == '+' -> {
                        flushBytes()
                        out.append(' ')
                        i++
                    }
                    c == '%' && i + 2 < value.length -> {
                        val hex = value.substring(i + 1, i + 3)
                        val byte = hex.toIntOrNull(16)
                        if (byte != null) {
                            bytes.add(byte.toByte())
                            i += 3
                        } else {
                            flushBytes()
                            out.append(c)
                            i++
                        }
                    }
                    else -> {
                        flushBytes()
                        out.append(c)
                        i++
                    }
                }
            }

            flushBytes()
            return out.toString()
        }

        private fun decodePlusOutsideQuery(value: String): String {
            return value
        }

        private fun decodeQueryPreservingSeparators(value: String): String {
            return value
        }
    }
}