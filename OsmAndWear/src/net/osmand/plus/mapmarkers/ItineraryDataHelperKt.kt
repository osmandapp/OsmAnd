package net.osmand.plus.mapmarkers

import net.osmand.plus.mapmarkers.ItineraryDataHelper.ItineraryGroupInfo
import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.GpxUtilities.GpxExtensionsReader
import net.osmand.shared.gpx.GpxUtilities.GpxExtensionsWriter
import net.osmand.shared.gpx.GpxUtilities.readText
import net.osmand.shared.gpx.GpxUtilities.writeNotNullText
import net.osmand.shared.xml.XmlParserException
import net.osmand.shared.xml.XmlPullParser
import net.osmand.shared.xml.XmlSerializer
import java.io.IOException

object ItineraryDataHelperKt {

	@JvmStatic
	fun assignExtensionWriter(gpxFile: GpxFile, groups: Collection<ItineraryGroupInfo>) {
		if (gpxFile.getExtensionsWriter("itinerary") == null) {
			gpxFile.setExtensionsWriter("itinerary", object : GpxExtensionsWriter {
				override fun writeExtensions(serializer: XmlSerializer) {
					for (group in groups) {
						try {
							serializer.startTag(
								null, "osmand:" + ItineraryDataHelper.ITINERARY_GROUP
							)
							writeNotNullText(serializer, "osmand:name", group.name)
							writeNotNullText(serializer, "osmand:type", group.type)
							writeNotNullText(serializer, "osmand:path", group.path)
							writeNotNullText(serializer, "osmand:alias", group.alias)
							writeNotNullText(serializer, "osmand:categories", group.categories)
							serializer.endTag(null, "osmand:" + ItineraryDataHelper.ITINERARY_GROUP)
						} catch (e: IOException) {
							ItineraryDataHelper.LOG.error(e)
						}
					}
				}
			})
		}
	}

	@JvmStatic
	fun getGpxExtensionsReader(groupInfos: MutableList<ItineraryGroupInfo?>): GpxExtensionsReader {
		return object : GpxExtensionsReader {
			@Throws(IOException::class, XmlParserException::class)
			override fun readExtensions(res: GpxFile, parser: XmlPullParser): Boolean {
				if (ItineraryDataHelper.ITINERARY_GROUP.equals(
						parser.getName(), ignoreCase = true
					)
				) {
					val groupInfo = ItineraryGroupInfo()
					var tok: Int
					while (parser.next().also { tok = it } != XmlPullParser.END_DOCUMENT) {
						if (tok == XmlPullParser.START_TAG) {
							val tagName = parser.getName()!!.toLowerCase()
							if ("name" == tagName) {
								groupInfo.name = readText(parser, tagName)
							} else if ("type" == tagName) {
								groupInfo.type = readText(parser, tagName)
							} else if ("path" == tagName) {
								groupInfo.path = readText(parser, tagName)
							} else if ("alias" == tagName) {
								groupInfo.alias = readText(parser, tagName)
							} else if ("categories" == tagName) {
								groupInfo.categories = readText(parser, tagName)
							}
						} else if (tok == XmlPullParser.END_TAG) {
							if (ItineraryDataHelper.ITINERARY_GROUP.equals(
									parser.getName(), ignoreCase = true
								)
							) {
								groupInfos.add(groupInfo)
								return true
							}
						}
					}
				}
				return false
			}
		}
	}
}