package net.osmand.plus;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GPXFile extends GPXExtensions {

	private final static NumberFormat latLonFormat = new DecimalFormat("0.00#####", new DecimalFormatSymbols(new Locale("EN", "US")));

	public String author;
	public List<Track> tracks = new ArrayList<Track>();
	public List<WptPt> points = new ArrayList<WptPt>();
	public List<Route> routes = new ArrayList<Route>();

	public String warning = null;
	public String path = "";
	public boolean showCurrentTrack;
	public long modifiedTime = 0;

	public boolean isCloudmadeRouteFile() {
		return "cloudmade".equalsIgnoreCase(author);
	}


	public GPXTrackAnalysis getAnalysis(long fileTimestamp) {
		GPXTrackAnalysis g = new GPXTrackAnalysis();
		g.wptPoints = points.size();
		List<SplitSegment> splitSegments = new ArrayList<SplitSegment>();
		for(int i = 0; i< tracks.size() ; i++){
			Track subtrack = tracks.get(i);
			for(TrkSegment segment : subtrack.segments){
				g.totalTracks ++;
				if(segment.points.size() > 1) {
					splitSegments.add(new SplitSegment(segment));
				}
			}
		}
		g.prepareInformation(fileTimestamp, splitSegments.toArray(new SplitSegment[splitSegments.size()]));
		return g ;
	}


	public boolean hasRtePt() {
		for(Route r : routes) {
			if(r.points.size() > 0) {
				return true;
			}
		}
		return false;
	}

	public boolean hasWptPt() {
		return points.size() > 0;
	}

	public boolean hasTrkpt() {
		for(Track t  : tracks) {
			for (TrkSegment ts : t.segments) {
				if (ts.points.size() > 0) {
					return true;
				}
			}
		}
		return false;
	}

	public WptPt addWptPt(double lat, double lon, long time, String description, String name, String category, int color) {
		double latAdjusted = Double.parseDouble(latLonFormat.format(lat));
		double lonAdjusted = Double.parseDouble(latLonFormat.format(lon));
		final WptPt pt = new WptPt(latAdjusted, lonAdjusted, time, Double.NaN, 0, Double.NaN);
		pt.name = name;
		pt.category = category;
		pt.desc = description;
		if (color != 0) {
			pt.setColor(color);
		}

		points.add(pt);
		modifiedTime = System.currentTimeMillis();

		return pt;
	}

	public void updateWptPt(WptPt pt, double lat, double lon, long time, String description, String name, String category, int color) {
		int index = points.indexOf(pt);

		double latAdjusted = Double.parseDouble(latLonFormat.format(lat));
		double lonAdjusted = Double.parseDouble(latLonFormat.format(lon));

		pt.lat = latAdjusted;
		pt.lon = lonAdjusted;
		pt.time = time;
		pt.desc = description;
		pt.name = name;
		pt.category = category;
		if (color != 0) {
			pt.setColor(color);
		}

		if (index != -1) {
			points.set(index, pt);
		}
		modifiedTime = System.currentTimeMillis();
	}

	public boolean deleteWptPt(WptPt pt) {
		modifiedTime = System.currentTimeMillis();
		return points.remove(pt);
	}

	public List<TrkSegment> processRoutePoints() {
		List<TrkSegment> tpoints = new ArrayList<TrkSegment>();
		if (routes.size() > 0) {
			for (Route r : routes) {
				TrkSegment sgmt = new TrkSegment();
				tpoints.add(sgmt);
				sgmt.points.addAll(r.points);
			}
		}
		return tpoints;
	}

	public List<TrkSegment> proccessPoints() {
		List<TrkSegment> tpoints = new ArrayList<TrkSegment>();
		for (Track t : tracks) {
			int trackColor = t.getColor(getColor(0));
			float trackZoom = t.getGpxZoom(getGpxZoom(1.0f));
			for (TrkSegment ts : t.segments) {
				if (ts.points.size() > 0) {
					TrkSegment sgmt = new TrkSegment();
					sgmt.setCustomZoom(ts.hasCustomZoom()|t.hasCustomZoom());
					tpoints.add(sgmt);
					sgmt.points.addAll(ts.points);
					sgmt.setColor(ts.getColor(trackColor));
					sgmt.setGpxZoom(ts.getGpxZoom(trackZoom));
				}
			}
		}
		return tpoints;
	}

	public WptPt getLastPoint() {
		if (tracks.size() > 0) {
			Track tk = tracks.get(tracks.size() - 1);
			if (tk.segments.size() > 0) {
				TrkSegment ts = tk.segments.get(tk.segments.size() - 1);
				if (ts.points.size() > 0) {
					return ts.points.get(ts.points.size() - 1);
				}
			}
		}
		return null;
	}

	public WptPt findPointToShow() {
		for (Track t : tracks) {
			for (TrkSegment s : t.segments) {
				if (s.points.size() > 0) {
					return s.points.get(0);
				}
			}
		}
		for (Route s : routes) {
			if (s.points.size() > 0) {
				return s.points.get(0);
			}
		}
		if (points.size() > 0) {
			return points.get(0);
		}
		return null;
	}

	public boolean isEmpty() {
		for (Track t : tracks) {
			if (t.segments != null) {
				for (TrkSegment s : t.segments) {
					boolean tracksEmpty = s.points.isEmpty();
					if (!tracksEmpty) {
						return false;
					}
				}
			}
		}
		return points.isEmpty() && routes.isEmpty();
	}


}
