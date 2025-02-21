package net.osmand.plus.nearbyplaces

import android.os.AsyncTask
import com.google.gson.Gson
import net.osmand.PlatformUtil
import net.osmand.data.NearbyPlacePoint
import net.osmand.plus.OsmandApplication
import net.osmand.plus.utils.FileUtils
import org.apache.commons.logging.Log
import java.io.File
import java.io.FileWriter

class SaveNearbyPlacesTask(val app:OsmandApplication, val collection: List<NearbyPlacePoint>) : AsyncTask<Void, Void, Boolean>() {
	private val LOG: Log = PlatformUtil.getLog(
		NearbyPlacesLoadSavedTask::class.java.name)

	override fun doInBackground(vararg params: Void?): Boolean {
		val file = File(FileUtils.getTempDir(app), "nearby_places")
		val gson = Gson()
		return try {
			FileWriter(file).use { writer ->
				val jsonString = gson.toJson(collection)
				writer.write(jsonString)
			}
			true // Success
		} catch (e: Exception) {
			false // Failure
		}
	}

	override fun onPostExecute(success: Boolean) {
		if (success) {
			LOG.debug("Nearby places saved successfully.")
		} else {
			LOG.error("Failed to save nearby places.")
		}
	}
}