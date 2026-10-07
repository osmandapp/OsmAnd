package net.osmand.data;

import com.vividsolutions.jts.geom.Geometry;

import java.util.List;

public class GeometryTile {

	private final List<Geometry> data;
	private final SourceFingerprint sourceFingerprint;

	public GeometryTile(List<Geometry> data) {
		this(data, SourceFingerprint.EMPTY);
	}

	public GeometryTile(List<Geometry> data, SourceFingerprint sourceFingerprint) {
		this.data = data;
		this.sourceFingerprint = sourceFingerprint;
	}

	public List<Geometry> getData() {
		return data;
	}

	public SourceFingerprint getSourceFingerprint() {
		return sourceFingerprint;
	}
}
