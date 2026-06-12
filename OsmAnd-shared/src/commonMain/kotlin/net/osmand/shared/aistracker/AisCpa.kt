package net.osmand.shared.aistracker

class AisCpa {
    var hasCpa: Boolean = false
    var cpa: Float = AisObjectConstants.INVALID_CPA
    var tcpa: Double = AisObjectConstants.INVALID_TCPA
    var tcpaUpdateTimestamp: Long = 0

    // Additional fields ported from AisTrackerHelper
    var cpaPos1: AisLatLon? = null
    var cpaPos2: AisLatLon? = null
    var t1: Double = 0.0
    var t2: Double = 0.0
    var valid: Boolean = false

    fun reset() {
        hasCpa = false
        cpa = AisObjectConstants.INVALID_CPA
        tcpa = AisObjectConstants.INVALID_TCPA
        tcpaUpdateTimestamp = 0
        cpaPos1 = null
        cpaPos2 = null
        t1 = 0.0
        t2 = 0.0
        valid = false
    }
}
