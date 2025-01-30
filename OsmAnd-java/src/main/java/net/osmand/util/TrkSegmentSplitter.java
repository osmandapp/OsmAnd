package net.osmand.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import net.osmand.data.LatLon;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXUtilities;
import net.osmand.gpx.GPXUtilities.Track;
import net.osmand.gpx.GPXUtilities.TrkSegment;
import net.osmand.gpx.GPXUtilities.WptPt;

// Reuse or delete once web is ready
public class TrkSegmentSplitter {

	public static void main(String[] args) {
//		int STEP = 30;
//		TIntArrayList altIncs = new TIntArrayList(new int[] {0, 10, -1, 5, -3, 2, 3, -16, -1});
//		
//		String str = encodeIntHeightArrayGraph(STEP, altIncs, 3);
//		TIntArrayList decodedSteps = decodeIntHeightArrayGraph(str, 3);
//		GPXFile gpx = GPXUtilities.loadGPXFile(new File("/Users/victorshcherb/osmand/maps/tracks/rec/2015-01-19_02-43_Mon.gpx"));
//		TrkSegment sgm = gpx.tracks.get(0).segments.get(0);
//		
//		// Algorithm begin
//		double startEle = 130;
//		augmentTrkSegmentWithAltitudes(sgm, decodedSteps, startEle);
		
		File dir = new File("/Users/victorshcherb/osmand/gpx/downhill");
		File[] fs = dir.listFiles();
		long time = System.currentTimeMillis();
		
		LatLon start = new LatLon(48.34764, 24.41092), end = new LatLon(48.35351, 24.41589); // 1 all variations
//		LatLon start = new LatLon(48.37805, 24.40994), end = new LatLon(48.36245, 24.40626); // 14
		
//		LatLon start = new LatLon(50.43796, 30.29913), end = new LatLon(50.43768, 30.29901); // Chaikia
//		LatLon start = new LatLon(48.37097, 24.38033), end = new LatLon(48.37248, 24.39530); // 11
//		LatLon start = new LatLon(48.36787, 24.36888), end = new LatLon(48.36758, 24.38158); // 12
//		LatLon start = new LatLon(52.832935, -1.375711), end = new LatLon(52.83286015, -1.37650815); 
		
		
		
		List<TrkSegment> res = new ArrayList<>();
		int files = 0;
		for (File f : fs) {
			if (f.getName().endsWith(".gpx")) {
				files++;
				GPXFile g = GPXUtilities.loadGPXFile(f, null, false);
//				System.out.println(f.getName());
				for (Track t : g.tracks) {
					for (TrkSegment s : t.segments) {
						process(f, s, start, end, res);
					}
				}

			}
		}
		time = System.currentTimeMillis() - time;
		System.out.printf("Processed %d files in %.2f seconds, found %d segments:\n", files, time / 1000.0, res.size());
		
		Collections.sort(res, new Comparator<>() {

			@Override
			public int compare(TrkSegment o1, TrkSegment o2) {
				return Long.compare(o1.points.get(0).time, o2.points.get(0).time);
			}
		});
		
		for (int k = 0; k < res.size(); k++) {
			TrkSegment s = res.get(k);
			GPXFile g = new GPXFile("");
			g.tracks.add(new Track());
			g.tracks.get(0).segments.add(s);
			long tm = s.points.get(0).time;
			GPXTrackAnalysis an = g.getAnalysis(tm);
			System.out.printf("%d. %s\n %s, distance %.3f km, duration %.1f s, points %d\n"
					+ "    Up %.0f m, down %.0f m, \n"
					+ "    Avg speed: %.1f kmh, max speed %.1f kmh\n", 
					k + 1, s.name, new Date(tm), an.getTotalDistance() / 1000, an.getDurationInMs() / 1000.0d, s.points.size(), 
					an.getDiffElevationUp(), an.getDiffElevationDown(),
					
					an.getAvgSpeed() * 3.6, an.getMaxSpeed() * 3.6
					);
		}
		
	}
	
	
	private static final double DIST_THRESHOLD = 20;
	
	private static double lat(TrkSegment s, int ind) {
		return s.points.get(ind).lat;
	}
	
	private static double lon(TrkSegment s, int ind) {
		return s.points.get(ind).lon;
	}
	
	private static long time(TrkSegment s, int ind) {
		return s.points.get(ind).time;
	}

	private static void process(File f, TrkSegment s, LatLon start, LatLon end, List<TrkSegment> res) {
		int startInd = -1;
//		System.out.println(f.getName() + "  " + s.points.size());
		for (int i = 1; i < s.points.size();) {
			LatLon pnt = startInd == -1 ? start : end;
			double dist = MapUtils.getOrthogonalDistance(pnt.getLatitude(), pnt.getLongitude(), s.points.get(i - 1).lat,
					s.points.get(i - 1).lon, s.points.get(i).lat, s.points.get(i).lon);
			if (dist < DIST_THRESHOLD) {
				int ind = i;
				for (int j = ind + 1; j < s.points.size() && j < ind + 10; j++) {
					double d2 = MapUtils.getOrthogonalDistance(pnt.getLatitude(), pnt.getLongitude(),
							s.points.get(j - 1).lat, s.points.get(j - 1).lon, s.points.get(j).lat, s.points.get(j).lon);
					if (d2 < dist) {
						dist = d2;
						i = j;
					}
				}
				if (startInd == -1) {
					startInd = i;
//					System.out.println("S " + startInd);
				} else {
					int finalInd = i;
					TrkSegment r = new TrkSegment();
					LatLon startProj = MapUtils.getProjection(start.getLatitude(), start.getLongitude(), 
							lat(s, startInd - 1), lon(s, startInd - 1), lat(s, startInd), lon(s, startInd));
					double stPercent = MapUtils.getDistance(startProj, lat(s, startInd), lon(s, startInd)) / 
							MapUtils.getDistance(lat(s, startInd - 1), lon(s, startInd - 1), lat(s, startInd), lon(s, startInd));
					WptPt st = new WptPt(startProj.getLatitude(), startProj.getLongitude());
					st.time = time(s, startInd - 1) + (long) (stPercent * (time(s, startInd) - time(s, startInd - 1)));
					st.ele = s.points.get(startInd).ele;
					
					LatLon endProj = MapUtils.getProjection(end.getLatitude(), end.getLongitude(),
							lat(s, finalInd - 1), lon(s, finalInd - 1), lat(s, finalInd), lon(s, finalInd));
					double enPercent = MapUtils.getDistance(endProj, lat(s, finalInd), lon(s, finalInd)) / 
							MapUtils.getDistance(lat(s, finalInd - 1), lon(s, finalInd - 1), lat(s, finalInd), lon(s, finalInd));
					WptPt en = new WptPt(endProj.getLatitude(), endProj.getLongitude());
					en.time = time(s, finalInd - 1) + (long) (enPercent * (time(s, finalInd) - time(s, finalInd - 1)));
					en.ele = s.points.get(finalInd).ele;
					r.points.add(st);
					for (int k = startInd + 1; k <= finalInd; k++) {
						r.points.add(s.points.get(k));
					}
					r.points.add(en);
					r.name = f.getName() + " " + startInd + " threshold=" + String.format("%.2f m",
							MapUtils.getDistance(startProj, start) + MapUtils.getDistance(endProj, end));
					res.add(r);
					startInd = -1;
				}
			}
			i++;
		}
	}	
}