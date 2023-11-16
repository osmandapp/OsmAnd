package net.osmand.plus.track.helpers;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.track.GpxSplitType;
import net.osmand.plus.track.GradientScaleType;
import net.osmand.plus.track.helpers.GpsFilterHelper.AltitudeFilter;
import net.osmand.plus.track.helpers.GpsFilterHelper.HdopFilter;
import net.osmand.plus.track.helpers.GpsFilterHelper.SmoothingFilter;
import net.osmand.plus.track.helpers.GpsFilterHelper.SpeedFilter;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.Map;

public class GpxDataItem {

	@NonNull
	private File file;
	@Nullable
	private GPXTrackAnalysis analysis;

	@ColorInt
	private int color;
	private String width;
	private String coloringType;
	private String nearestCityName;
	private String containingFolder;

	private int splitType;
	private double splitInterval;

	private long fileCreationTime = -1;
	private long fileLastModifiedTime;
	private long fileLastUploadedTime;

	private boolean showArrows;
	private boolean showStartFinish = true;
	private boolean joinSegments;
	private boolean showAsMarkers;
	private boolean importedByApi;

	private double maxFilterHdop = Double.NaN;
	private double minFilterSpeed = Double.NaN;
	private double maxFilterSpeed = Double.NaN;
	private double minFilterAltitude = Double.NaN;
	private double maxFilterAltitude = Double.NaN;
	private double smoothingThreshold = Double.NaN;


	public GpxDataItem(@NonNull File file) {
		this.file = file;
	}

	@NonNull
	public File getFile() {
		return file;
	}

	@Nullable
	public GPXTrackAnalysis getAnalysis() {
		return analysis;
	}

	public void setAnalysis(@Nullable GPXTrackAnalysis analysis) {
		this.analysis = analysis;
	}

	@ColorInt
	public int getColor() {
		return color;
	}

	public void setColor(@ColorInt int color) {
		this.color = color;
	}

	@Nullable
	public String getWidth() {
		return width;
	}

	public void setWidth(@Nullable String width) {
		this.width = width;
	}

	@Nullable
	public String getColoringType() {
		return coloringType;
	}

	public void setColoringType(@Nullable String coloringType) {
		this.coloringType = coloringType;
	}

	@Nullable
	public String getNearestCityName() {
		return nearestCityName;
	}

	public void setNearestCityName(@Nullable String nearestCityName) {
		this.nearestCityName = nearestCityName;
	}

	@Nullable
	public String getContainingFolder() {
		return containingFolder;
	}

	public void setContainingFolder(@Nullable String containingFolder) {
		this.containingFolder = containingFolder;
	}

	public int getSplitType() {
		return splitType;
	}

	public void setSplitType(int splitType) {
		this.splitType = splitType;
	}

	public double getSplitInterval() {
		return splitInterval;
	}

	public void setSplitInterval(double splitInterval) {
		this.splitInterval = splitInterval;
	}

	public long getFileCreationTime() {
		return fileCreationTime;
	}

	public void setFileCreationTime(long fileCreationTime) {
		this.fileCreationTime = fileCreationTime;
	}

	public long getFileLastModifiedTime() {
		return fileLastModifiedTime;
	}

	public void setFileLastModifiedTime(long fileLastModifiedTime) {
		this.fileLastModifiedTime = fileLastModifiedTime;
	}

	public long getFileLastUploadedTime() {
		return fileLastUploadedTime;
	}

	public void setFileLastUploadedTime(long fileLastUploadedTime) {
		this.fileLastUploadedTime = fileLastUploadedTime;
	}

	public boolean isShowArrows() {
		return showArrows;
	}

	public void setShowArrows(boolean showArrows) {
		this.showArrows = showArrows;
	}

	public boolean isShowStartFinish() {
		return showStartFinish;
	}

	public void setShowStartFinish(boolean showStartFinish) {
		this.showStartFinish = showStartFinish;
	}

	public boolean isJoinSegments() {
		return joinSegments;
	}

	public void setJoinSegments(boolean joinSegments) {
		this.joinSegments = joinSegments;
	}

	public boolean isShowAsMarkers() {
		return showAsMarkers;
	}

	public void setShowAsMarkers(boolean showAsMarkers) {
		this.showAsMarkers = showAsMarkers;
	}

	public boolean isImportedByApi() {
		return importedByApi;
	}

	public void setImportedByApi(boolean importedByApi) {
		this.importedByApi = importedByApi;
	}

	public double getMaxFilterHdop() {
		return maxFilterHdop;
	}

	public void setMaxFilterHdop(double maxFilterHdop) {
		this.maxFilterHdop = maxFilterHdop;
	}

	public double getMinFilterSpeed() {
		return minFilterSpeed;
	}

	public void setMinFilterSpeed(double minFilterSpeed) {
		this.minFilterSpeed = minFilterSpeed;
	}

	public double getMaxFilterSpeed() {
		return maxFilterSpeed;
	}

	public void setMaxFilterSpeed(double maxFilterSpeed) {
		this.maxFilterSpeed = maxFilterSpeed;
	}

	public double getMinFilterAltitude() {
		return minFilterAltitude;
	}

	public void setMinFilterAltitude(double minFilterAltitude) {
		this.minFilterAltitude = minFilterAltitude;
	}

	public double getMaxFilterAltitude() {
		return maxFilterAltitude;
	}

	public void setMaxFilterAltitude(double maxFilterAltitude) {
		this.maxFilterAltitude = maxFilterAltitude;
	}

	public double getSmoothingThreshold() {
		return smoothingThreshold;
	}

	public void setSmoothingThreshold(double smoothingThreshold) {
		this.smoothingThreshold = smoothingThreshold;
	}

	public void readGpxParams(@NonNull GPXFile gpxFile) {
		color = gpxFile.getColor(0);
		width = gpxFile.getWidth(null);
		showArrows = gpxFile.isShowArrows();
		showStartFinish = gpxFile.isShowStartFinish();

		if (!Algorithms.isEmpty(gpxFile.getSplitType()) && gpxFile.getSplitInterval() > 0) {
			GpxSplitType gpxSplitType = GpxSplitType.getSplitTypeByName(gpxFile.getSplitType());
			splitType = gpxSplitType.getType();
			splitInterval = gpxFile.getSplitInterval();
		}

		if (!Algorithms.isEmpty(gpxFile.getColoringType())) {
			coloringType = gpxFile.getColoringType();
		} else if (!Algorithms.isEmpty(gpxFile.getGradientScaleType())) {
			GradientScaleType scaleType = GradientScaleType.getGradientTypeByName(gpxFile.getGradientScaleType());
			ColoringType type = ColoringType.fromGradientScaleType(scaleType);
			coloringType = type == null ? null : type.getName(null);
		}

		Map<String, String> extensions = gpxFile.getExtensionsToRead();
		smoothingThreshold = SmoothingFilter.getSmoothingThreshold(extensions);
		minFilterSpeed = SpeedFilter.getMinFilterSpeed(extensions);
		maxFilterSpeed = SpeedFilter.getMaxFilterSpeed(extensions);
		minFilterAltitude = AltitudeFilter.getMinFilterAltitude(extensions);
		maxFilterAltitude = AltitudeFilter.getMaxFilterAltitude(extensions);
		maxFilterHdop = HdopFilter.getMaxFilterHdop(extensions);
		fileCreationTime = gpxFile.metadata.time;
	}

	@Override
	public int hashCode() {
		return file.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof GpxDataItem)) {
			return false;
		}
		GpxDataItem other = (GpxDataItem) obj;
		return file.equals(other.file);
	}
}
