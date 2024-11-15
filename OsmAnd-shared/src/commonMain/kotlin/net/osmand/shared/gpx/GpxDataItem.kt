package net.osmand.shared.gpx

import net.osmand.shared.io.KFile
import net.osmand.shared.routing.ColoringType
import net.osmand.shared.util.PlatformUtil

class GpxDataItem(
	file: KFile
) : DataItem(file) {

	private var analysis: GpxTrackAnalysis? = null

	companion object {
		fun isRegularTrack(file: KFile) = file.path().startsWith(PlatformUtil.getOsmAndContext().getGpxDir().path())
	}

	fun isRegularTrack() = Companion.isRegularTrack(file)

	fun getAnalysis(): GpxTrackAnalysis? {
		return analysis
	}

	fun setAnalysis(analysis: GpxTrackAnalysis?) {
		this.analysis = analysis
		updateAnalysisParameters()
	}

	override fun isValidValue(parameter: GpxParameter, value: Any?): Boolean {
		return (value == null && parameter.isNullSupported())
				|| (value != null && parameter.typeClass == value::class)
	}

	fun copyData(item: GpxDataItem) {
		for (entry in item.map.entries) {
			val parameter = entry.key
			if (parameter !in listOf(
					GpxParameter.FILE_NAME,
					GpxParameter.FILE_DIR,
					GpxParameter.FILE_LAST_MODIFIED_TIME
				)
			) {
				map[parameter] = entry.value
			}
		}
		setAnalysis(item.analysis)
	}

	private fun updateAnalysisParameters() {
		val hasAnalysis = analysis != null
		for (gpxParameter in GpxParameter.entries) {
			if (gpxParameter.analysisParameter) {
				map[gpxParameter] =
					if (hasAnalysis) analysis?.getGpxParameter(gpxParameter) else null
			}
		}
	}

	fun readGpxParams(gpxFile: GpxFile) {
		setParameter(GpxParameter.FILE_NAME, file.name())
		setParameter(GpxParameter.FILE_DIR, GpxDbUtils.getGpxFileDir(file))
		setParameter(GpxParameter.FILE_LAST_MODIFIED_TIME, file.lastModified())

		for (parameter in GpxParameter.getAppearanceParameters()) {
			readGpxAppearanceParameter(gpxFile, parameter)
		}

		/* TODO: Implement GpsFilterHelper
		val extensions = gpxFile.getExtensionsToRead()
		setParameter(
			GpxParameter.SMOOTHING_THRESHOLD,
			GpsFilterHelper.SmoothingFilter.getSmoothingThreshold(extensions)
		)
		setParameter(
			GpxParameter.MIN_FILTER_SPEED,
			GpsFilterHelper.SpeedFilter.getMinFilterSpeed(extensions)
		)
		setParameter(
			GpxParameter.MAX_FILTER_SPEED,
			GpsFilterHelper.SpeedFilter.getMaxFilterSpeed(extensions)
		)
		setParameter(
			GpxParameter.MIN_FILTER_ALTITUDE,
			GpsFilterHelper.AltitudeFilter.getMinFilterAltitude(extensions)
		)
		setParameter(
			GpxParameter.MAX_FILTER_ALTITUDE,
			GpsFilterHelper.AltitudeFilter.getMaxFilterAltitude(extensions)
		)
		setParameter(
			GpxParameter.MAX_FILTER_HDOP,
			GpsFilterHelper.HdopFilter.getMaxFilterHdop(extensions)
		)
		 */
		setParameter(GpxParameter.FILE_CREATION_TIME, gpxFile.metadata.time)
	}

	fun readGpxAppearanceParameter(gpxFile: GpxFile, parameter: GpxParameter) {
		when (parameter) {
			GpxParameter.COLOR -> setParameter(GpxParameter.COLOR, gpxFile.getColor(0))
			GpxParameter.WIDTH -> setParameter(GpxParameter.WIDTH, gpxFile.getWidth(null))
			GpxParameter.SHOW_ARROWS ->
				setParameter(
					GpxParameter.SHOW_ARROWS,
					if (gpxFile.isShowArrowsSet()) gpxFile.isShowArrows() else null)

			GpxParameter.SHOW_START_FINISH -> {
				setParameter(
					GpxParameter.SHOW_START_FINISH,
					if (gpxFile.isShowStartFinishSet()) gpxFile.isShowStartFinish() else null);

			}

			GpxParameter.SPLIT_TYPE -> {
				if (!gpxFile.getSplitType().isNullOrEmpty() && gpxFile.getSplitInterval() > 0) {
					val splitType = GpxSplitType.getSplitTypeByName(gpxFile.getSplitType())
					setParameter(GpxParameter.SPLIT_TYPE, splitType.type)
				}
			}

			GpxParameter.SPLIT_INTERVAL -> {
				if (!gpxFile.getSplitType().isNullOrEmpty() && gpxFile.getSplitInterval() > 0) {
					setParameter(GpxParameter.SPLIT_INTERVAL, gpxFile.getSplitInterval())
				}
			}

			GpxParameter.COLORING_TYPE -> {
				if (!gpxFile.getColoringType().isNullOrEmpty()) {
					setParameter(GpxParameter.COLORING_TYPE, gpxFile.getColoringType())
				} else if (!gpxFile.getGradientScaleType().isNullOrEmpty()) {
					val scaleType =
						GradientScaleType.getGradientTypeByName(gpxFile.getGradientScaleType())
					val coloringType = ColoringType.valueOf(scaleType)
					setParameter(GpxParameter.COLORING_TYPE, coloringType?.getName(null))
				}
			}

			GpxParameter.COLOR_PALETTE ->
				if (gpxFile.getGradientColorPalette()?.isNotEmpty() == true) {
					setParameter(GpxParameter.COLOR_PALETTE, gpxFile.getGradientColorPalette())
				}

				GpxParameter.TRACK_VISUALIZATION_TYPE -> setParameter(
				GpxParameter.TRACK_VISUALIZATION_TYPE,
				gpxFile.get3DVisualizationType()
			)

			GpxParameter.TRACK_3D_LINE_POSITION_TYPE -> setParameter(
				GpxParameter.TRACK_3D_LINE_POSITION_TYPE,
				gpxFile.get3DLinePositionType()
			)

			GpxParameter.TRACK_3D_WALL_COLORING_TYPE -> setParameter(
				GpxParameter.TRACK_3D_WALL_COLORING_TYPE,
				gpxFile.get3DWallColoringType()
			)

			else -> {}
		}
	}

	inline fun <reified T: Any> getAppearanceParameter(parameter: GpxParameter): T? {
		var value: Any? = getAppearanceParameter<Any>(this, parameter)
		if (value == null) {
			value = parameter.defaultValue
		}
		return castGpxParameter<T>(parameter, value)
	}

	inline fun <reified T: Any> getAppearanceParameter(file: KFile, parameter: GpxParameter): T? {
		val item = GpxDbHelper.getItem(file)
		if (item != null) {
			return getAppearanceParameter<T>(item, parameter)
		}
		return null
	}

	inline fun <reified T: Any> getAppearanceParameter(item: GpxDataItem, parameter: GpxParameter): T? {
		var value: T? = item.getParameter(parameter)
		if (value != null) {
			return castGpxParameter<T>(parameter, value)
		}
		val dir = item.file.getParentFile()
		if (dir != null) {
			val dirItem = GpxDbHelper.getGpxDirItem(dir)
			value = dirItem.getParameter(parameter)
			if (value != null) {
				return castGpxParameter<T>(parameter, value)
			}
		}
		return null
	}

	inline fun <reified T : Any> castGpxParameter(parameter: GpxParameter, value: Any?): T? {
		return if (parameter.typeClass.isInstance(value)) value as? T else null
	}

	override fun getParameters(): Map<GpxParameter, Any?> =
		GpxParameter.entries.associateWith { parameter ->
			if (parameter.analysisParameter) analysis?.getGpxParameter(parameter)
			else getParameter(parameter)
		}
}
