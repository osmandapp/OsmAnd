package net.osmand.plus.plugins.srtm.building

import androidx.annotation.StringRes
import net.osmand.plus.R

/** Where the light of the hillshade and of the sun shadows comes from. */
enum class SunLightMode(@StringRes val titleId: Int) {

	/** The real sun above the centre of the map, now. */
	LIVE(R.string.sun_light_live),

	/** The real sun above the centre of the map at a chosen date and time. */
	DATE(R.string.shared_string_date),

	/** Azimuth and altitude set by hand. */
	MANUAL(R.string.shared_string_manual)
}
