package net.osmand.shared.util

import net.sf.junidecode.Junidecode

internal actual fun toLatin(text: String): String = Junidecode.unidecode(text)
