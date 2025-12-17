package net.osmand.plus.plugins.odb

import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import net.osmand.plus.OsmandApplication
import net.osmand.plus.charts.ChartUtils
import net.osmand.plus.charts.GPXDataSetAxisType
import net.osmand.plus.charts.GPXDataSetType
import net.osmand.plus.charts.GpxDataSetTypeGroup
import net.osmand.plus.charts.OrderedLineDataSet
import net.osmand.plus.settings.enums.ThemeUsageContext
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.shared.gpx.GpxTrackAnalysis
import net.osmand.shared.gpx.PointAttributes
import net.osmand.shared.obd.OBDDataComputer
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget.Companion.findByGpxTag
import net.osmand.util.Algorithms
import java.text.MessageFormat

class VehicleMetricAttributesUtils {
    companion object {
        fun getAvailableGPXDataSetTypes(
            analysis: GpxTrackAnalysis,
            out: MutableList<Array<GPXDataSetType?>>
        ) {
            for (type in GPXDataSetType.entries) {
                if (type.typeGroup != GpxDataSetTypeGroup.VEHICLE_METRICS) {
                    continue
                }

                if (analysis.hasData(type.dataKey)) {
                    out.add(arrayOf(type))
                }
            }
        }

        fun createVehicleMetricsDataSet(
            app: OsmandApplication,
            plugin: VehicleMetricsPlugin,
            chart: LineChart,
            analysis: GpxTrackAnalysis,
            graphType: GPXDataSetType,
            axisType: GPXDataSetAxisType,
            useRightAxis: Boolean,
            drawFilled: Boolean,
            calcWithoutGaps: Boolean
        ): OrderedLineDataSet {
            val nightMode = app.daynightHelper.isNightMode(ThemeUsageContext.APP)
            val widgetType: OBDDataComputer.OBDTypeWidget? =
                OBDDataComputer.OBDTypeWidget.findByGpxTag(graphType.dataKey)

            val divX = ChartUtils.getDivX(app, chart, analysis, axisType, calcWithoutGaps)

            val pair = ChartUtils.getScalingY(app, graphType)
            val mulY = if (pair != null) pair.first else 1f
            val divY = if (pair != null) pair.second else Float.NaN

            val speedInTrack = analysis.hasSpeedInTrack()
            val textColor = ColorUtilities.getColor(app, graphType.getTextColorId(!speedInTrack))
            val yAxis = ChartUtils.getYAxis(chart, textColor, useRightAxis)
            yAxis.axisMinimum = 0f

            val values = getPointAttributeValues(
                plugin,
                graphType.dataKey,
                widgetType,
                analysis.pointAttributes,
                axisType,
                divX,
                mulY,
                divY,
                calcWithoutGaps
            )
            val dataSet = OrderedLineDataSet(values, "", graphType, axisType, !useRightAxis)

            var format: String? = null
            if (dataSet.yMax < 3) {
                format = "{0,number,0.#} "
            }
            val formatY = format
            val mainUnitY = widgetType?.let { plugin.getWidgetUnit(it) } ?: ""

            yAxis.valueFormatter = IAxisValueFormatter { value: Float, axis: AxisBase? ->
                if (!Algorithms.isEmpty(formatY)) {
                    return@IAxisValueFormatter MessageFormat.format(formatY + mainUnitY, value)
                } else {
                    return@IAxisValueFormatter OsmAndFormatter.formatInteger(
                        (value + 0.5).toInt(),
                        mainUnitY,
                        app
                    )
                }
            }

            dataSet.divX = divX
            dataSet.units = mainUnitY

            val color = ColorUtilities.getColor(app, graphType.getFillColorId(!speedInTrack))
            ChartUtils.setupDataSet(
                app,
                dataSet,
                color,
                color,
                drawFilled,
                false,
                useRightAxis,
                nightMode
            )

            return dataSet
        }

        private fun getPointAttributeValues(
            plugin: VehicleMetricsPlugin,
            key: String,
            widgetType: OBDDataComputer.OBDTypeWidget?,
            pointAttributes: List<PointAttributes>,
            axisType: GPXDataSetAxisType,
            divX: Float, mulY: Float, divY: Float,
            calcWithoutGaps: Boolean
        ): List<Entry> {
            val values: MutableList<Entry> = ArrayList()
            var currentX = 0f

            for (i in pointAttributes.indices) {
                val attribute = pointAttributes[i]

                val stepX =
                    if (axisType == GPXDataSetAxisType.TIME || axisType == GPXDataSetAxisType.TIME_OF_DAY) attribute.timeDiff else attribute.distance

                if (i == 0 || stepX > 0) {
                    if (!(calcWithoutGaps && attribute.firstPoint)) {
                        currentX += stepX / divX
                    }
                    if (attribute.hasValidValue(key)) {
                        var value = attribute.getAttributeValue(key)!!
                        val formattedValue =
                            widgetType?.let { plugin.getWidgetConvertedValue(it, value) }

                        if (formattedValue is Number) {
                            value = formattedValue as Float
                        }

                        var currentY =
                            if (java.lang.Float.isNaN(divY)) value * mulY else divY / value
                        if (currentY < 0 || java.lang.Float.isInfinite(currentY)) {
                            currentY = 0f
                        }
                        if (attribute.firstPoint && currentY != 0f) {
                            values.add(Entry(currentX, 0f))
                        }
                        values.add(Entry(currentX, currentY))
                        if (attribute.lastPoint && currentY != 0f) {
                            values.add(Entry(currentX, 0f))
                        }
                    }
                }
            }
            return values
        }
    }
}