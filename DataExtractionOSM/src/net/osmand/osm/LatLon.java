package net.osmand.osm;

public class LatLon {
		private final double longitude;
		private final double latitude;

		public LatLon(double latitude, double longitude){
			this.latitude = latitude;
			this.longitude = longitude;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			long temp;
			temp = Double.doubleToLongBits(latitude);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(longitude);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			LatLon other = (LatLon) obj;
			if (Double.doubleToLongBits(latitude) != Double
					.doubleToLongBits(other.latitude))
				return false;
			if (Double.doubleToLongBits(longitude) != Double
					.doubleToLongBits(other.longitude))
				return false;
			return true;
		}
		@Override
		public String toString() {
			return "Lat " + latitude +" Lon "+ longitude; //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		public double getLatitude() {
			return latitude;
		}
		
		public double getLongitude() {
			return longitude;
		}
		
	}