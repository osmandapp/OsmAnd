package net.osmand.plus;

import android.content.Context;

import net.osmand.data.LocationPoint;
import net.osmand.data.PointDescription;
import net.osmand.util.Algorithms;

public class WptPt extends GPXExtensions implements LocationPoint {

	public double lat;
	public double lon;
	public String name = null;
	public String link = null;
	// previous undocumented feature 'category' ,now 'type'
	public String category = null;
	public String desc = null;
	// by default
	public long time = 0;
	public double ele = Double.NaN;
	public double speed = 0;
	public double hdop = Double.NaN;
	public boolean deleted = false;
	public double cumDist = 0.0;				// cumulative distance, if in a track

	public void setCumulativeDistance(double dist) {
		cumDist = dist;
	}
	public double getCumulativeDistance(){
		return cumDist;
	}

	public WptPt() {
	}

	@Override
	public int getColor() {
		return getColor(0);
	}

	@Override
	public double getLatitude() {
		return lat;
	}

	@Override
	public double getLongitude() {
		return lon;
	}


	@Override
	public PointDescription getPointDescription(Context ctx) {
		return new PointDescription(PointDescription.POINT_TYPE_WPT, name);
	}

	public WptPt(double lat, double lon, long time, double ele, double speed, double hdop) {
		this.lat = lat;
		this.lon = lon;
		this.time = time;
		this.ele = ele;
		this.speed = speed;
		this.hdop = hdop;
	}

	@Override
	public boolean isVisible() {
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((category == null) ? 0 : category.hashCode());
		result = prime * result + ((desc == null) ? 0 : desc.hashCode());
		result = prime * result + ((lat == 0) ? 0 : Double.valueOf(lat).hashCode());
		result = prime * result + ((lon == 0) ? 0 : Double.valueOf(lon).hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		WptPt other = (WptPt) obj;
		return Algorithms.objectEquals(other.name, name)
				&& Algorithms.objectEquals(other.category, category)
				&& Algorithms.objectEquals(other.lat, lat)
				&& Algorithms.objectEquals(other.lon, lon)
				&& Algorithms.objectEquals(other.desc, desc);
	}
}
