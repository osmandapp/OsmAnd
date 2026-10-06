package net.osmand.shared.util

/**
 * Canonical decomposition, every non-spacing mark dropped, canonical composition.
 *
 * Call [KUnicode.stripDiacritics] rather than this: it answers most strings without a platform
 * call at all.
 */
internal expect fun platformStripDiacritics(value: String): String

/** Canonical composition (NFC). Call [KUnicode.normalizeNFC], which skips ascii. */
internal expect fun platformNormalizeNFC(value: String): String
