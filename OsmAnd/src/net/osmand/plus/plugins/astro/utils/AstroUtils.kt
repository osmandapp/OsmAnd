package net.osmand.plus.plugins.astro.utils

import android.content.Context
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.defineStar
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.horizon
import io.github.cosinekitty.astronomy.searchAltitude
import io.github.cosinekitty.astronomy.searchRiseSet
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.plugins.astro.Constellation
import net.osmand.plus.plugins.astro.SkyObject
import net.osmand.plus.plugins.astro.StarWatcherSettings
import net.osmand.plus.plugins.astro.StarWatcherSettings.StarMapConfig
import net.osmand.plus.plugins.astro.views.StarView
import net.osmand.plus.settings.enums.ThemeUsageContext
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object AstroUtils {

	fun bodyName(ctx: Context, b: Body) = when (b) {
		Body.Sun -> ctx.getString(R.string.astro_name_sun)
		Body.Moon -> ctx.getString(R.string.astro_name_moon)
		Body.Mercury -> ctx.getString(R.string.astro_name_mercury)
		Body.Venus -> ctx.getString(R.string.astro_name_venus)
		Body.Mars -> ctx.getString(R.string.astro_name_mars)
		Body.Jupiter -> ctx.getString(R.string.astro_name_jupiter)
		Body.Saturn -> ctx.getString(R.string.astro_name_saturn)
		Body.Uranus -> ctx.getString(R.string.astro_name_uranus)
		Body.Neptune -> ctx.getString(R.string.astro_name_neptune)
		Body.Pluto -> ctx.getString(R.string.astro_name_pluto)
		else -> b.toString()
	}

	@ColorInt
	fun bodyColor(b: Body): Int = when (b) {
		Body.Sun -> "#FFD54F".toColorInt()
		Body.Moon -> "#BDBDBD".toColorInt()
		Body.Mercury -> "#F9A825".toColorInt()
		Body.Venus -> "#66BB6A".toColorInt()
		Body.Mars -> "#EF5350".toColorInt()
		Body.Jupiter -> "#8D6E63".toColorInt()
		Body.Saturn -> "#D4A373".toColorInt()
		else -> "#FFFFFF".toColorInt()
	}

	@DrawableRes
	fun getObjectTypeIcon(type: SkyObject.Type): Int {
		return when (type) {
			SkyObject.Type.SUN -> R.drawable.ic_action_sun
			SkyObject.Type.MOON -> R.drawable.ic_action_moon
			SkyObject.Type.PLANET -> R.drawable.ic_action_ufo
			SkyObject.Type.STAR -> R.drawable.ic_action_favorite
			SkyObject.Type.GALAXY, SkyObject.Type.GALAXY_CLUSTER -> R.drawable.ic_world_globe_dark
			SkyObject.Type.NEBULA -> R.drawable.ic_action_clouds
			SkyObject.Type.BLACK_HOLE -> R.drawable.ic_action_circle
			SkyObject.Type.CONSTELLATION -> R.drawable.ic_action_celestial_path
			else -> R.drawable.ic_action_favorite
		}
	}

	fun getObjectTypeName(ctx: Context, type: SkyObject.Type): String {
		return when (type) {
			SkyObject.Type.SUN -> ctx.getString(R.string.astro_name_sun)
			SkyObject.Type.MOON -> ctx.getString(R.string.astro_name_moon)
			SkyObject.Type.PLANET -> ctx.getString(R.string.astro_planets)
			SkyObject.Type.STAR -> ctx.getString(R.string.astro_stars)
			SkyObject.Type.GALAXY -> ctx.getString(R.string.astro_galaxies)
			SkyObject.Type.NEBULA -> ctx.getString(R.string.astro_nebulae)
			SkyObject.Type.BLACK_HOLE -> ctx.getString(R.string.astro_black_holes)
			SkyObject.Type.OPEN_CLUSTER -> ctx.getString(R.string.astro_open_clusters)
			SkyObject.Type.GLOBULAR_CLUSTER -> ctx.getString(R.string.astro_globular_clusters)
			SkyObject.Type.GALAXY_CLUSTER -> ctx.getString(R.string.astro_galaxy_clusters)
			SkyObject.Type.CONSTELLATION -> ctx.getString(R.string.astro_constellations)
			else -> type.name
		}
	}

	data class Twilight(
		val sunrise: ZonedDateTime?, val sunset: ZonedDateTime?,
		val civilDawn: ZonedDateTime?, val civilDusk: ZonedDateTime?,
		val nauticalDawn: ZonedDateTime?, val nauticalDusk: ZonedDateTime?,
		val astroDawn: ZonedDateTime?, val astroDusk: ZonedDateTime?
	)

	fun formatLocalTime(astronomyTime: Time): String {
		val calendar = Calendar.getInstance(TimeZone.getDefault())
		calendar.timeInMillis = astronomyTime.toMillisecondsSince1970()
		return String.format(
			Locale.getDefault(), "%02d:%02d",
			calendar.get(Calendar.HOUR_OF_DAY),
			calendar.get(Calendar.MINUTE))
	}

	fun showStarMapOptionsDialog(
		context: Context,
		starView: StarView,
		swSettings: StarWatcherSettings,
		onApply: ((StarMapConfig) -> Unit)? = null
	) {
		val config = swSettings.getStarMapConfig()

		val app = (context.applicationContext as OsmandApplication)
		val nightMode = app.daynightHelper.isNightMode(ThemeUsageContext.APP)

		var tempAzimuthal = config.showAzimuthalGrid
		var tempEquatorial = config.showEquatorialGrid
		var tempEcliptic = config.showEclipticLine
		var tempMeridian = config.showMeridianLine
		var tempEquator = config.showEquatorLine
		var tempFavorites = config.showFavorites
		var tempSun = config.showSun
		var tempMoon = config.showMoon
		var tempPlanets = config.showPlanets
		var tempConstellations = config.showConstellations
		var tempStars = config.showStars
		var tempGalaxies = config.showGalaxies
		var tempNebulae = config.showNebulae
		var tempOpenClusters = config.showOpenClusters
		var tempGlobularClusters = config.showGlobularClusters
		var tempGalaxyClusters = config.showGalaxyClusters
		var tempBlackHoles = config.showBlackHoles
		var tempShowMagnitudeFilter = config.showMagnitudeFilter

		val scrollView = ScrollView(context)
		val ll = LinearLayout(context)
		ll.orientation = LinearLayout.VERTICAL

		val listTextSize = context.resources.getDimensionPixelSize(R.dimen.default_list_text_size).toFloat()
		val dialogContentPadding = context.resources.getDimensionPixelSize(R.dimen.dialog_content_margin)
		val contentPadding = context.resources.getDimensionPixelSize(R.dimen.content_padding)
		val textPadding = context.resources.getDimensionPixelSize(R.dimen.content_padding_small)
		ll.setPadding(dialogContentPadding, contentPadding, dialogContentPadding, contentPadding)
		scrollView.addView(ll)

		val activeColorRes = ColorUtilities.getActiveIconColorId(nightMode)
		val secondaryColorRes = ColorUtilities.getSecondaryIconColorId(nightMode)
		val checkedColorStateList =
			AndroidUtils.createCheckedColorStateList(app, secondaryColorRes, activeColorRes)

		fun addSection(title: String) {
			val tv = TextView(context)
			tv.text = title.uppercase()
			tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, listTextSize)
			tv.setTextColor(ContextCompat.getColor(context, R.color.color_favorite_gray))
			tv.setPadding(0, contentPadding, 0, contentPadding / 2)
			ll.addView(tv)
		}

		fun addCheckBox(title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
			val cb = AppCompatCheckBox(context).apply {
				text = title
				setTextSize(TypedValue.COMPLEX_UNIT_PX, listTextSize)
				setTextColor(ColorUtilities.getPrimaryTextColor(context, nightMode))
				isChecked = checked
				buttonTintList = checkedColorStateList
				setPadding(textPadding, 0, contentPadding, 0)
				setOnCheckedChangeListener { _, isChecked -> onChecked(isChecked) }
			}

			val params = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				topMargin = contentPadding / 4
				bottomMargin = contentPadding / 4
			}

			ll.addView(cb, params)
		}

		// MAP
		addSection(context.getString(R.string.shared_string_map))
		addCheckBox(context.getString(R.string.azimuthal_grid), tempAzimuthal) { tempAzimuthal = it }
		addCheckBox(context.getString(R.string.equatorial_grid), tempEquatorial) { tempEquatorial = it }
		addCheckBox(context.getString(R.string.ecliptic_line), tempEcliptic) { tempEcliptic = it }
		addCheckBox(context.getString(R.string.meridian_line), tempMeridian) { tempMeridian = it }
		addCheckBox(context.getString(R.string.equator_line), tempEquator) { tempEquator = it }
		addCheckBox(context.getString(R.string.favorites_item), tempFavorites) { tempFavorites = it }
		addCheckBox(context.getString(R.string.magnitude_filter), tempShowMagnitudeFilter) { tempShowMagnitudeFilter = it }

		// PLANETS
		addSection(context.getString(R.string.astro_planets))
		addCheckBox(context.getString(R.string.astro_name_sun), tempSun) { tempSun = it }
		addCheckBox(context.getString(R.string.astro_name_moon), tempMoon) { tempMoon = it }
		addCheckBox(context.getString(R.string.astro_planets), tempPlanets) { tempPlanets = it }

		// OTHER
		addSection(context.getString(R.string.shared_string_other))
		addCheckBox(context.getString(R.string.astro_constellations), tempConstellations) { tempConstellations = it }
		addCheckBox(context.getString(R.string.astro_stars), tempStars) { tempStars = it }
		addCheckBox(context.getString(R.string.astro_galaxies), tempGalaxies) { tempGalaxies = it }
		addCheckBox(context.getString(R.string.astro_nebulae), tempNebulae) { tempNebulae = it }
		addCheckBox(context.getString(R.string.astro_open_clusters), tempOpenClusters) { tempOpenClusters = it }
		addCheckBox(context.getString(R.string.astro_globular_clusters), tempGlobularClusters) { tempGlobularClusters = it }
		addCheckBox(context.getString(R.string.astro_galaxy_clusters), tempGalaxyClusters) { tempGalaxyClusters = it }
		addCheckBox(context.getString(R.string.astro_black_holes), tempBlackHoles) { tempBlackHoles = it }

		val dialog = AlertDialog.Builder(context)
			.setTitle(R.string.visible_layers_and_objects)
			.setView(scrollView)
			.setPositiveButton(R.string.shared_string_apply) { _, _ ->
				starView.showAzimuthalGrid = tempAzimuthal
				starView.showEquatorialGrid = tempEquatorial
				starView.showEclipticLine = tempEcliptic
				starView.showMeridianLine = tempMeridian
				starView.showEquatorLine = tempEquator
				starView.showFavorites = tempFavorites

				starView.showSun = tempSun
				starView.showMoon = tempMoon
				starView.showPlanets = tempPlanets

				starView.showConstellations = tempConstellations

				starView.showStars = tempStars
				starView.showGalaxies = tempGalaxies
				starView.showNebulae = tempNebulae
				starView.showOpenClusters = tempOpenClusters
				starView.showGlobularClusters = tempGlobularClusters
				starView.showGalaxyClusters = tempGalaxyClusters
				starView.showBlackHoles = tempBlackHoles

				starView.updateVisibility()

				val newConfig = config.copy(
					showAzimuthalGrid = tempAzimuthal,
					showEquatorialGrid = tempEquatorial,
					showEclipticLine = tempEcliptic,
					showMeridianLine = tempMeridian,
					showEquatorLine = tempEquator,
					showFavorites = tempFavorites,
					showSun = tempSun,
					showMoon = tempMoon,
					showPlanets = tempPlanets,
					showConstellations = tempConstellations,
					showStars = tempStars,
					showGalaxies = tempGalaxies,
					showNebulae = tempNebulae,
					showOpenClusters = tempOpenClusters,
					showGlobularClusters = tempGlobularClusters,
					showGalaxyClusters = tempGalaxyClusters,
					showBlackHoles = tempBlackHoles,
					showMagnitudeFilter = tempShowMagnitudeFilter
				)
				swSettings.setStarMapConfig(newConfig)
				onApply?.invoke(newConfig)
			}
			.setNegativeButton(R.string.shared_string_cancel, null)
			.create()

		dialog.setCancelable(true)
		dialog.setCanceledOnTouchOutside(true)
		dialog.show()
	}

	// ---------- Extensions for Type Conversions ----------

	fun Time.toZoned(zoneId: ZoneId): ZonedDateTime =
		Instant.ofEpochMilli(this.toMillisecondsSince1970()).atZone(zoneId)

	fun ZonedDateTime.toAstroTime(): Time =
		Time.Companion.fromMillisecondsSince1970(this.toInstant().toEpochMilli())

	// ---------- Physics / Math Helpers ----------

	fun <T> withCustomStar(ra: Double, dec: Double, block: (Body) -> T): T {
		synchronized(this) {
			defineStar(Body.Star1, ra, dec, 1000.0)
			return block(Body.Star1)
		}
	}

	fun altitude(body: Body, tLocal: ZonedDateTime, obs: Observer): Double {
		val tUtc = tLocal.toAstroTime()
		val eq = equator(body, tUtc, obs, EquatorEpoch.OfDate, Aberration.Corrected)
		val hor = horizon(tUtc, obs, eq.ra, eq.dec, Refraction.Normal)
		return hor.altitude
	}

	fun altitude(obj: SkyObject, tLocal: ZonedDateTime, obs: Observer): Double {
		val body = obj.body
		return if (body != null) {
			altitude(body, tLocal, obs)
		} else {
			withCustomStar(obj.ra, obj.dec) { star ->
				altitude(star, tLocal, obs)
			}
		}
	}

	fun nextRiseSet(
		body: Body,
		startSearch: ZonedDateTime,
		obs: Observer,
		windowStart: ZonedDateTime? = null,
		windowEnd: ZonedDateTime? = null
	): Pair<ZonedDateTime?, ZonedDateTime?> {
		val searchStartUtc = startSearch.toAstroTime()
		val zone = startSearch.zone
		val limitDays = 2.0

		val nextRise = searchRiseSet(body, obs, Direction.Rise, searchStartUtc, limitDays)
		val nextSet  = searchRiseSet(body, obs, Direction.Set, searchStartUtc, limitDays)

		val r = nextRise?.toZoned(zone)
		val s = nextSet?.toZoned(zone)

		val rFiltered = if (windowStart != null && windowEnd != null) {
			r?.takeIf { !it.isBefore(windowStart) && !it.isAfter(windowEnd) }
		} else r

		val sFiltered = if (windowStart != null && windowEnd != null) {
			s?.takeIf { !it.isBefore(windowStart) && !it.isAfter(windowEnd) }
		} else s

		return rFiltered to sFiltered
	}

	fun nextRiseSet(
		obj: SkyObject,
		startSearch: ZonedDateTime,
		obs: Observer,
		windowStart: ZonedDateTime? = null,
		windowEnd: ZonedDateTime? = null
	): Pair<ZonedDateTime?, ZonedDateTime?> {
		val body = obj.body
		return if (body != null) {
			nextRiseSet(body, startSearch, obs, windowStart, windowEnd)
		} else {
			withCustomStar(obj.ra, obj.dec) { star ->
				nextRiseSet(star, startSearch, obs, windowStart, windowEnd)
			}
		}
	}

	fun computeTwilight(startLocal: ZonedDateTime, endLocal: ZonedDateTime, obs: Observer, zoneId: ZoneId): Twilight {
		fun findAlt(direction: Direction, deg: Double): ZonedDateTime? {
			val t0 = startLocal.toAstroTime()
			val t = searchAltitude(Body.Sun, obs, direction, t0, 2.0, deg)
			return t?.toZoned(zoneId)
		}
		val searchStart = startLocal.toAstroTime()
		val sr = searchRiseSet(Body.Sun, obs, Direction.Rise, searchStart, 2.0)
		val ss = searchRiseSet(Body.Sun, obs, Direction.Set, searchStart, 2.0)
		return Twilight(
			sr?.toZoned(zoneId), ss?.toZoned(zoneId),
			findAlt(Direction.Rise, -6.0), findAlt(Direction.Set, -6.0),
			findAlt(Direction.Rise, -12.0), findAlt(Direction.Set, -12.0),
			findAlt(Direction.Rise, -18.0), findAlt(Direction.Set, -18.0)
		)
	}

	fun calculateConstellationCenter(c: Constellation, skyObjectMap: Map<Int, SkyObject>): Pair<Double, Double>? {
		var sumX = 0.0
		var sumY = 0.0
		var sumZ = 0.0
		var count = 0

		val uniqueStars = mutableSetOf<Int>()
		c.lines.forEach { (id1, id2) -> uniqueStars.add(id1); uniqueStars.add(id2) }

		uniqueStars.forEach { id ->
			val star = skyObjectMap[id]
			if (star != null) {
				val raRad = Math.toRadians(star.ra * 15.0)
				val decRad = Math.toRadians(star.dec)
				sumX += cos(decRad) * cos(raRad)
				sumY += cos(decRad) * sin(raRad)
				sumZ += sin(decRad)
				count++
			}
		}

		if (count > 0) {
			val avgX = sumX / count
			val avgY = sumY / count
			val avgZ = sumZ / count
			val hyp = sqrt(avgX * avgX + avgY * avgY)
			val decRad = atan2(avgZ, hyp)
			var raRad = atan2(avgY, avgX)
			if (raRad < 0) raRad += 2 * PI
			return (Math.toDegrees(raRad) / 15.0) to Math.toDegrees(decRad)
		}
		return null
	}
}
