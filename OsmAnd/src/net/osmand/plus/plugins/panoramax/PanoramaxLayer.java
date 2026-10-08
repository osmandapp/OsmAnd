package net.osmand.plus.plugins.panoramax;

import net.osmand.data.LatLon;

interface PanoramaxLayer {

	void setSelectedImageLocation(LatLon selectedImageLocation);

	void setSelectedImageCameraAngle(Float selectedImageCameraAngle);
}
