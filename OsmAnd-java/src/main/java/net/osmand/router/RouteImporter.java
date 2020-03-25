package net.osmand.router;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXExtensionsReader;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.RouteDataBundle;
import net.osmand.binary.RouteDataObject;
import net.osmand.binary.StringBundle;
import net.osmand.binary.StringBundleReader;
import net.osmand.binary.StringBundleXmlReader;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static net.osmand.binary.RouteDataObject.HEIGHT_UNDEFINED;

public class RouteImporter {

	public final static Log log = PlatformUtil.getLog(RouteImporter.class);

	private File file;
	private GPXFile gpxFile;

	public RouteImporter(File file) {
		this.file = file;
	}

	public RouteImporter(GPXFile gpxFile) {
		this.gpxFile = gpxFile;
	}

	public List<RouteSegmentResult> importRoute() {

		final List<RouteSegmentResult> route = new ArrayList<>();
		final RouteRegion region = new RouteRegion();
		final RouteDataResources resources = new RouteDataResources();

		GPXExtensionsReader extensionsReader = new GPXExtensionsReader() {
			@Override
			public boolean readExtensions(GPXFile res, XmlPullParser parser) throws Exception {
				if (!resources.hasLocations()) {
					List<Location> locations = resources.getLocations();
					double lastElevation = HEIGHT_UNDEFINED;
					if (res.tracks.size() > 0 && res.tracks.get(0).segments.size() > 0 && res.tracks.get(0).segments.get(0).points.size() > 0) {
						for (WptPt point : res.tracks.get(0).segments.get(0).points) {
							Location loc = new Location("", point.getLatitude(), point.getLongitude());
							if (!Double.isNaN(point.ele)) {
								loc.setAltitude(point.ele);
								lastElevation = point.ele;
							} else if (lastElevation != HEIGHT_UNDEFINED) {
								loc.setAltitude(lastElevation);
							}
							locations.add(loc);
						}
					}
				}
				String tag = parser.getName();
				if ("route".equals(tag)) {
					int tok;
					while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
						if (tok == XmlPullParser.START_TAG) {
							tag = parser.getName();
							if ("segment".equals(tag)) {
								StringBundleReader bundleReader = new StringBundleXmlReader(parser);
								RouteDataObject object = new RouteDataObject(region);
								RouteSegmentResult segment = new RouteSegmentResult(object);
								bundleReader.readBundle();
								segment.readFromBundle(new RouteDataBundle(resources, bundleReader.getBundle()));
								route.add(segment);
							}
						} else if (tok == XmlPullParser.END_TAG) {
							tag = parser.getName();
							if ("route".equals(tag)) {
								return true;
							}
						}
					}
				} else if ("types".equals(tag)) {
					int tok;
					int i = 0;
					while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
						if (tok == XmlPullParser.START_TAG) {
							tag = parser.getName();
							if ("type".equals(tag)) {
								StringBundleReader bundleReader = new StringBundleXmlReader(parser);
								bundleReader.readBundle();
								StringBundle bundle = bundleReader.getBundle();
								String t = bundle.getString("t", null);
								String v = bundle.getString("v", null);
								region.initRouteEncodingRule(i++, t, v);
							}
						} else if (tok == XmlPullParser.END_TAG) {
							tag = parser.getName();
							if ("types".equals(tag)) {
								return true;
							}
						}
					}
				}
				return false;
			}
		};

		if (gpxFile != null) {
			GPXUtilities.loadGPXFile(null, gpxFile, extensionsReader);
			for (RouteSegmentResult segment : route) {
				segment.fillNames(resources);
			}
		} else if (file != null) {
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(file);
				GPXFile gpxFile = GPXUtilities.loadGPXFile(fis, null, extensionsReader);
				for (RouteSegmentResult segment : route) {
					segment.fillNames(resources);
				}
				gpxFile.path = file.getAbsolutePath();
				gpxFile.modifiedTime = file.lastModified();
			} catch (IOException e) {
				log.error("Error importing route " + file.getAbsolutePath(), e);
				return null;
			} finally {
				try {
					if (fis != null) {
						fis.close();
					}
				} catch (IOException ignore) {
					// ignore
				}
			}
		}

		return route;
	}
}
