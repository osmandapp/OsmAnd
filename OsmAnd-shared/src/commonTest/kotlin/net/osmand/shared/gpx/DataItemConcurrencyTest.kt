package net.osmand.shared.gpx

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.osmand.shared.io.KFile
import kotlin.test.Test

class DataItemConcurrencyTest {

	@Test
	fun analysisCalculationParametersSurviveConcurrentWrites() = runBlocking {
		val item = GpxDataItem.fromDatabase(KFile("/gpx/concurrent-item.gpx"))
		item.setAnalysis(GpxTrackAnalysis())
		val gpxFileWithoutSplit = GpxFile(null)

		val writer = launch(Dispatchers.Default) {
			repeat(WRITES) {
				item.setParameter(GpxParameter.SPLIT_TYPE, 1)
				item.readGpxAppearanceParameter(gpxFileWithoutSplit, GpxParameter.SPLIT_TYPE)
			}
		}
		while (writer.isActive) {
			item.getAnalysisCalculationParameters()
		}
	}

	private companion object {
		const val WRITES = 100_000
	}
}
