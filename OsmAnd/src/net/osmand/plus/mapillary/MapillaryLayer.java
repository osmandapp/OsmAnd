package net.osmand.plus.mapillary;

import net.osmand.data.LatLon;

interface MapillaryLayer {

	void setSelectedImageLocation(LatLon selectedImageLocation);

	void setSelectedImageCameraAngle(Float selectedImageCameraAngle);
}
