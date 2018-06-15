package net.osmand.data;

import com.vividsolutions.jts.geom.Geometry;

import java.util.List;

public class GeometryTile {

	private List<Geometry> data;

	public GeometryTile(List<Geometry> data) {
		this.data = data;
	}

	public List<Geometry> getData() {
		return data;
	}
}
