package net.osmand.plus.plugins.externalsensors.devices.sensors

import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.utils.OsmAndFormatter

class BikeCadenceDataField(nameId: Int, unitNameId: Int, cadenceValue: Number) :
    SensorWidgetDataField(
        SensorWidgetDataFieldType.BIKE_CADENCE,
        nameId,
        unitNameId,
        cadenceValue) {
    override fun getFormattedValue(app: OsmandApplication): OsmAndFormatter.FormattedValue? {
        val cadence = numberValue.toFloat()
        return if (cadence > 0) OsmAndFormatter.FormattedValue(
            cadence,
            cadence.toString(),
            app.getString(R.string.revolutions_per_minute_unit)) else null
    }
}