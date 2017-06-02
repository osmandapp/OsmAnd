package net.osmand.plus;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import net.osmand.IProgress;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.GPXUtilities.Route;
import net.osmand.plus.GPXUtilities.Track;
import net.osmand.plus.GPXUtilities.TrkSegment;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.OsmandSettings.MetricsConstants;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetAxisType;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetType;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GpxSelectionHelper {

	private static final String CURRENT_TRACK = "currentTrack";
	private static final String FILE = "file";
	private static final String COLOR = "color";
	private OsmandApplication app;
	@NonNull
	private List<SelectedGpxFile> selectedGPXFiles = new java.util.ArrayList<>();
	private SavingTrackHelper savingTrackHelper;

	public GpxSelectionHelper(OsmandApplication osmandApplication, SavingTrackHelper trackHelper) {
		this.app = osmandApplication;
		savingTrackHelper = trackHelper;
	}

	public void clearAllGpxFileToShow() {
		selectedGPXFiles.clear();
		saveCurrentSelections();
	}

	public boolean isShowingAnyGpxFiles() {
		return !selectedGPXFiles.isEmpty();
	}

	@NonNull
	public List<SelectedGpxFile> getSelectedGPXFiles() {
		return selectedGPXFiles;
	}

	public String getGpxDescription() {
		if (selectedGPXFiles.size() == 1) {
			GPXFile currentGPX = app.getSavingTrackHelper().getCurrentGpx();
			if (selectedGPXFiles.get(0).getGpxFile() == currentGPX) {
				return app.getResources().getString(R.string.current_track);
			}

			File file = new File(selectedGPXFiles.get(0).getGpxFile().path);
			return Algorithms.getFileNameWithoutExtension(file).replace('_', ' ');
		} else if (selectedGPXFiles.size() == 0) {
			return null;
		} else {
			return app.getResources().getString(R.string.number_of_gpx_files_selected_pattern,
					selectedGPXFiles.size());
		}
	}

	public SelectedGpxFile getSelectedGPXFile(WptPt point) {
		for (SelectedGpxFile g : selectedGPXFiles) {
			if (g.getGpxFile().points.contains(point)) {
				return g;
			}
		}
		return null;
	}

	private String getString(int resId, Object... formatArgs) {
		return app.getString(resId, formatArgs);
	}

	public GpxDisplayGroup buildGeneralGpxDisplayGroup(GPXFile g, Track t) {
		GpxDisplayGroup group = new GpxDisplayGroup(g);
		String name = getGroupName(g);
		group.gpxName = name;
		group.color = t.getColor(g.getColor(0));
		group.setType(GpxDisplayItemType.TRACK_SEGMENT);
		group.setTrack(t);
		group.setName(getString(R.string.gpx_selection_track, name, ""));
		String d = "";
		if (t.name != null && t.name.length() > 0) {
			d = t.name + " " + d;
		}
		group.setDescription(d);
		processGroupTrack(app, group);
		return group;
	}

	public GpxDisplayGroup buildGpxDisplayGroup(GPXFile g, int trackIndex, String name) {
		Track t = g.tracks.get(trackIndex);
		GpxDisplayGroup group = new GpxDisplayGroup(g);
		group.gpxName = name;
		group.color = t.getColor(g.getColor(0));
		group.setType(GpxDisplayItemType.TRACK_SEGMENT);
		group.setTrack(t);
		String ks = (trackIndex + 1) + "";
		group.setName(getString(R.string.gpx_selection_track, name, g.tracks.size() == 1 ? "" : ks));
		String d = "";
		if (t.name != null && t.name.length() > 0) {
			d = t.name + " " + d;
		}
		group.setDescription(d);
		processGroupTrack(app, group);
		return group;
	}

	private String getGroupName(GPXFile g) {
		String name = g.path;
		if (g.showCurrentTrack) {
			name = getString(R.string.shared_string_currently_recording_track);
		} else {
			int i = name.lastIndexOf('/');
			if (i >= 0) {
				name = name.substring(i + 1);
			}
			i = name.lastIndexOf('\\');
			if (i >= 0) {
				name = name.substring(i + 1);
			}
			if (name.toLowerCase().endsWith(".gpx")) {
				name = name.substring(0, name.length() - 4);
			}
			name = name.replace('_', ' ');
		}
		return name;
	}

	public List<GpxDisplayGroup> collectDisplayGroups(GPXFile g) {
		List<GpxDisplayGroup> dg = new ArrayList<>();
		String name = getGroupName(g);
		if (g.tracks.size() > 0) {
			for (int i = 0; i < g.tracks.size(); i++) {
				GpxDisplayGroup group = buildGpxDisplayGroup(g, i, name);
				dg.add(group);
			}
		}
		if (g.routes.size() > 0) {
			int k = 0;
			for (Route route : g.routes) {
				GpxDisplayGroup group = new GpxDisplayGroup(g);
				group.gpxName = name;
				group.setType(GpxDisplayItemType.TRACK_ROUTE_POINTS);
				String d = getString(R.string.gpx_selection_number_of_points, name, route.points.size());
				if (route.name != null && route.name.length() > 0) {
					d = route.name + " " + d;
				}
				group.setDescription(d);
				String ks = (k++) + "";
				group.setName(getString(R.string.gpx_selection_route_points, name, g.routes.size() == 1 ? "" : ks));
				dg.add(group);
				List<GpxDisplayItem> list = group.getModifiableList();
				int t = 0;
				for (WptPt r : route.points) {
					GpxDisplayItem item = new GpxDisplayItem();
					item.group = group;
					item.description = r.desc;
					item.expanded = true;
					item.name = r.name;
					t++;
					if (Algorithms.isEmpty(item.name)) {
						item.name = getString(R.string.gpx_selection_point, t + "");
					}
					item.locationStart = r;
					item.locationEnd = r;
					list.add(item);
				}
			}
		}

		if (g.points.size() > 0) {
			GpxDisplayGroup group = new GpxDisplayGroup(g);
			group.gpxName = name;
			group.setType(GpxDisplayItemType.TRACK_POINTS);
			group.setDescription(getString(R.string.gpx_selection_number_of_points, g.points.size()));
			group.setName(getString(R.string.gpx_selection_points, name));
			dg.add(group);
			List<GpxDisplayItem> list = group.getModifiableList();
			int k = 0;
			for (WptPt r : g.points) {
				GpxDisplayItem item = new GpxDisplayItem();
				item.group = group;
				item.description = r.desc;
				item.name = r.name;
				k++;
				if (Algorithms.isEmpty(item.name)) {
					item.name = getString(R.string.gpx_selection_point, k + "");
				}
				item.expanded = true;
				item.locationStart = r;
				item.locationEnd = r;
				list.add(item);
			}
		}
		return dg;
	}

	private static void processGroupTrack(OsmandApplication app, GpxDisplayGroup group) {
		List<GpxDisplayItem> list = group.getModifiableList();
		String timeSpanClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_time_span_color));
		String speedClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_speed));
		String ascClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_altitude_asc));
		String descClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_altitude_desc));
		String distanceClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_distance_color));
		final float eleThreshold = 3;
//		int t = 1;
		for (TrkSegment r : group.track.segments) {
			if (r.points.size() == 0) {
				continue;
			}
			GPXTrackAnalysis[] as;
			boolean split = true;
			if (group.splitDistance > 0) {
				List<GPXTrackAnalysis> trackSegments = r.splitByDistance(group.splitDistance);
				as = trackSegments.toArray(new GPXTrackAnalysis[trackSegments.size()]);
			} else if (group.splitTime > 0) {
				List<GPXTrackAnalysis> trackSegments = r.splitByTime(group.splitTime);
				as = trackSegments.toArray(new GPXTrackAnalysis[trackSegments.size()]);
			} else {
				split = false;
				as = new GPXTrackAnalysis[]{GPXTrackAnalysis.segment(0, r)};
			}
			for (GPXTrackAnalysis analysis : as) {
				GpxDisplayItem item = new GpxDisplayItem();
				item.group = group;
				if (split) {
					item.splitMetric = analysis.metricEnd;
					item.secondarySplitMetric = analysis.secondaryMetricEnd;
					item.splitName = formatSplitName(analysis.metricEnd, group, app);
					item.splitName += " (" + formatSecondarySplitName(analysis.secondaryMetricEnd, group, app) + ") ";
				}

				item.description = GpxUiHelper.getDescription(app, analysis, true);
				item.analysis = analysis;
				String name = "";
//				if(group.track.segments.size() > 1) {
//					name += t++ + ". ";
//				}
				if (!group.isSplitDistance()) {
					name += GpxUiHelper.getColorValue(distanceClr, OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));
				}
				if ((analysis.timeSpan > 0 || analysis.timeMoving > 0) && !group.isSplitTime()) {
					long tm = analysis.timeMoving;
					if (tm == 0) {
						tm = analysis.timeSpan;
					}
					if (name.length() != 0)
						name += ", ";
					name += GpxUiHelper.getColorValue(timeSpanClr, Algorithms.formatDuration((int) (tm / 1000), app.accessibilityEnabled()));
				}
				if (analysis.isSpeedSpecified()) {
					if (name.length() != 0)
						name += ", ";
					name += GpxUiHelper.getColorValue(speedClr, OsmAndFormatter.getFormattedSpeed(analysis.avgSpeed, app));
				}
// add min/max elevation data to split track analysis to facilitate easier track/segment identification
				if (analysis.isElevationSpecified()) {
					if (name.length() != 0)
						name += ", ";
					name += GpxUiHelper.getColorValue(descClr, OsmAndFormatter.getFormattedAlt(analysis.minElevation, app));
					name += " - ";
					name += GpxUiHelper.getColorValue(ascClr, OsmAndFormatter.getFormattedAlt(analysis.maxElevation, app));
				}
				if (analysis.isElevationSpecified() && (analysis.diffElevationUp > eleThreshold ||
						analysis.diffElevationDown > eleThreshold)) {
					if (name.length() != 0)
						name += ", ";
					if (analysis.diffElevationDown > eleThreshold) {
						name += GpxUiHelper.getColorValue(descClr, " \u2193 " +
								OsmAndFormatter.getFormattedAlt(analysis.diffElevationDown, app));
					}
					if (analysis.diffElevationUp > eleThreshold) {
						name += GpxUiHelper.getColorValue(ascClr, " \u2191 " +
								OsmAndFormatter.getFormattedAlt(analysis.diffElevationUp, app));
					}
				}
				item.name = name;
				item.locationStart = analysis.locationStart;
				item.locationEnd = analysis.locationEnd;
				list.add(item);
			}
		}
	}

	private static String formatSecondarySplitName(double metricEnd, GpxDisplayGroup group, OsmandApplication app) {
		if (group.isSplitDistance()) {
			return Algorithms.formatDuration((int) metricEnd, app.accessibilityEnabled());
		} else {
			return OsmAndFormatter.getFormattedDistance((float) metricEnd, app);
		}
	}

	private static String formatSplitName(double metricEnd, GpxDisplayGroup group, OsmandApplication app) {
		if (group.isSplitDistance()) {
			MetricsConstants mc = app.getSettings().METRIC_SYSTEM.get();
			if (mc == MetricsConstants.KILOMETERS_AND_METERS) {
				final double sd = group.getSplitDistance();
				int digits = sd < 100 ? 2 : (sd < 1000 ? 1 : 0);
				int rem1000 = (int) (metricEnd + 0.5) % 1000;
				if (rem1000 > 1 && digits < 1) {
					digits = 1;
				}
				int rem100 = (int) (metricEnd + 0.5) % 100;
				if (rem100 > 1 && digits < 2) {
					digits = 2;
				}
				return OsmAndFormatter.getFormattedRoundDistanceKm((float) metricEnd, digits, app);
			} else {
				return OsmAndFormatter.getFormattedDistance((float) metricEnd, app);
			}
		} else {
			return Algorithms.formatDuration((int) metricEnd, app.accessibilityEnabled());
		}
	}

	public SelectedGpxFile getSelectedFileByPath(String path) {
		for (SelectedGpxFile s : selectedGPXFiles) {
			if (s.getGpxFile().path.equals(path)) {
				return s;
			}
		}
		return null;
	}

	public SelectedGpxFile getSelectedFileByName(String path) {
		for (SelectedGpxFile s : selectedGPXFiles) {
			if (s.getGpxFile().path.endsWith("/" + path)) {
				return s;
			}
		}
		return null;
	}

	public SelectedGpxFile getSelectedCurrentRecordingTrack() {
		for (SelectedGpxFile s : selectedGPXFiles) {
			if (s.isShowCurrentTrack()) {
				return s;
			}
		}
		return null;
	}

	public void setGpxFileToDisplay(GPXFile... gpxs) {
		// special case for gpx current route
		for (GPXFile gpx : gpxs) {
			selectGpxFileImpl(gpx, true, false);
		}
		saveCurrentSelections();
	}

	public void loadGPXTracks(IProgress p) {
		String load = app.getSettings().SELECTED_GPX.get();
		if (!Algorithms.isEmpty(load)) {
			try {
				JSONArray ar = new JSONArray(load);
				boolean save = false;
				for (int i = 0; i < ar.length(); i++) {
					JSONObject obj = ar.getJSONObject(i);
					if (obj.has(FILE)) {
						File fl = new File(obj.getString(FILE));
						if (p != null) {
							p.startTask(getString(R.string.loading_smth, fl.getName()), -1);
						}
						GPXFile gpx = GPXUtilities.loadGPXFile(app, fl);
						if (obj.has(COLOR)) {
							int clr = Algorithms.parseColor(obj.getString(COLOR));
							gpx.setColor(clr);
						}
						if (gpx.warning != null) {
							save = true;
						} else {
							selectGpxFile(gpx, true, false);
						}
					} else if (obj.has(CURRENT_TRACK)) {
						selectedGPXFiles.add(savingTrackHelper.getCurrentTrack());
					}
				}
				if (save) {
					saveCurrentSelections();
				}
			} catch (Exception e) {
				app.getSettings().SELECTED_GPX.set("");
				e.printStackTrace();
			}
		}
	}

	private void saveCurrentSelections() {
		JSONArray ar = new JSONArray();
		for (SelectedGpxFile s : selectedGPXFiles) {
			if (s.gpxFile != null && !s.notShowNavigationDialog) {
				JSONObject obj = new JSONObject();
				try {
					if (s.isShowCurrentTrack()) {
						obj.put(CURRENT_TRACK, true);
					} else if (!Algorithms.isEmpty(s.gpxFile.path)) {
						obj.put(FILE, s.gpxFile.path);
						if (s.gpxFile.getColor(0) != 0) {
							obj.put(COLOR, Algorithms.colorToString(s.gpxFile.getColor(0)));
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
				ar.put(obj);
			}
		}
		app.getSettings().SELECTED_GPX.set(ar.toString());
	}

	private SelectedGpxFile selectGpxFileImpl(GPXFile gpx, boolean show, boolean notShowNavigationDialog) {
		boolean displayed;
		SelectedGpxFile sf;
		if (gpx != null && gpx.showCurrentTrack) {
			sf = savingTrackHelper.getCurrentTrack();
			sf.notShowNavigationDialog = notShowNavigationDialog;
			displayed = selectedGPXFiles.contains(sf);
		} else {
			assert gpx != null;
			sf = getSelectedFileByPath(gpx.path);
			displayed = sf != null;
			if (show && sf == null) {
				sf = new SelectedGpxFile();
				sf.setGpxFile(gpx);
				sf.notShowNavigationDialog = notShowNavigationDialog;
			}
		}
		if (displayed != show) {
			if (show) {
				selectedGPXFiles.add(sf);
			} else {
				selectedGPXFiles.remove(sf);
			}
		}
		return sf;
	}

	public SelectedGpxFile selectGpxFile(GPXFile gpx, boolean show, boolean notShowNavigationDialog) {
		SelectedGpxFile sf = selectGpxFileImpl(gpx, show, notShowNavigationDialog);
		saveCurrentSelections();
		return sf;
	}


	public static class SelectedGpxFile {
		public boolean notShowNavigationDialog = false;

		private boolean showCurrentTrack;
		private GPXFile gpxFile;
		private int color;
		private GPXTrackAnalysis trackAnalysis;
		private long modifiedTime = -1;
		private List<TrkSegment> processedPointsToDisplay = new ArrayList<>();
		private boolean routePoints;

		private List<GpxDisplayGroup> displayGroups;

		public void setGpxFile(GPXFile gpxFile) {
			this.gpxFile = gpxFile;
			if (gpxFile.tracks.size() > 0) {
				this.color = gpxFile.tracks.get(0).getColor(0);
			}
			processPoints();
		}

		public GPXTrackAnalysis getTrackAnalysis() {
			if (modifiedTime != gpxFile.modifiedTime) {
				update();
			}
			return trackAnalysis;
		}

		private void update() {
			modifiedTime = gpxFile.modifiedTime;
			trackAnalysis = gpxFile.getAnalysis(
					Algorithms.isEmpty(gpxFile.path) ? System.currentTimeMillis() :
							new File(gpxFile.path).lastModified());
			displayGroups = null;
		}

		public void processPoints() {
			update();
			this.processedPointsToDisplay = gpxFile.proccessPoints();
			if (this.processedPointsToDisplay.isEmpty()) {
				this.processedPointsToDisplay = gpxFile.processRoutePoints();
				routePoints = !this.processedPointsToDisplay.isEmpty();
			}
		}

		public boolean isRoutePoints() {
			return routePoints;
		}

		public List<TrkSegment> getPointsToDisplay() {
			return processedPointsToDisplay;
		}

		public List<TrkSegment> getModifiablePointsToDisplay() {
			return processedPointsToDisplay;
		}

		public GPXFile getGpxFile() {
			return gpxFile;
		}

		public GPXFile getModifiableGpxFile() {
			// call process points after
			return gpxFile;
		}

		public boolean isShowCurrentTrack() {
			return showCurrentTrack;
		}

		public void setShowCurrentTrack(boolean showCurrentTrack) {
			this.showCurrentTrack = showCurrentTrack;
		}

		public int getColor() {
			return color;
		}

		public List<GpxDisplayGroup> getDisplayGroups() {
			if (modifiedTime != gpxFile.modifiedTime) {
				update();
			}
			return displayGroups;
		}

		public void setDisplayGroups(List<GpxDisplayGroup> displayGroups) {
			if (modifiedTime != gpxFile.modifiedTime) {
				update();
			}
			this.displayGroups = displayGroups;
		}


	}

	public enum GpxDisplayItemType {
		TRACK_SEGMENT,
		TRACK_POINTS,
		TRACK_ROUTE_POINTS
	}

	public static class GpxDisplayGroup {

		private GpxDisplayItemType type = GpxDisplayItemType.TRACK_SEGMENT;
		private List<GpxDisplayItem> list = new ArrayList<>();
		private GPXFile gpx;
		private String gpxName;
		private String name;
		private String description;
		private Track track;
		private double splitDistance = -1;
		private int splitTime = -1;
		private int color;

		public GpxDisplayGroup(GPXFile gpx) {
			this.gpx = gpx;
		}

		public void setTrack(Track track) {
			this.track = track;
		}

		public GPXFile getGpx() {
			return gpx;
		}

		public Track getTrack() {
			return track;
		}

		public GpxDisplayGroup cloneInstance() {
			GpxDisplayGroup group = new GpxDisplayGroup(gpx);
			group.type = type;
			group.name = name;
			group.description = description;
			group.track = track;
			group.list = new ArrayList<>(list);
			return group;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getGpxName() {
			return gpxName;
		}

		public String getName() {
			return name;
		}

		public List<GpxDisplayItem> getModifiableList() {
			return list;
		}

		public GpxDisplayItemType getType() {
			return type;
		}

		public void setType(GpxDisplayItemType type) {
			this.type = type;
		}

		public boolean isSplitDistance() {
			return splitDistance > 0;
		}

		public double getSplitDistance() {
			return splitDistance;
		}

		public boolean isSplitTime() {
			return splitTime > 0;
		}

		public int getSplitTime() {
			return splitTime;
		}

		public String getGroupName() {
			return name;
		}

		public void noSplit(OsmandApplication app) {
			list.clear();
			splitDistance = -1;
			splitTime = -1;
			processGroupTrack(app, this);
		}

		public void splitByDistance(OsmandApplication app, double meters) {
			list.clear();
			splitDistance = meters;
			splitTime = -1;
			processGroupTrack(app, this);
		}

		public void splitByTime(OsmandApplication app, int seconds) {
			list.clear();
			splitDistance = -1;
			splitTime = seconds;
			processGroupTrack(app, this);
		}

		public int getColor() {
			return color;
		}
	}

	public static class GpxDisplayItem {

		public GPXTrackAnalysis analysis;
		public GpxDisplayGroup group;
		public WptPt locationStart;
		public WptPt locationEnd;
		public double splitMetric = -1;
		public double secondarySplitMetric = -1;
		public String splitName;
		public String name;
		public String description;
		public String url;
		public Bitmap image;
		public boolean expanded;
		public boolean route;
		public boolean wasHidden = true;

		public WptPt locationOnMap;
		public GPXDataSetType[] chartTypes;
		public GPXDataSetAxisType chartAxisType = GPXDataSetAxisType.DISTANCE;

		public Matrix chartMatrix;
		public float chartHighlightPos = -1f;
	}
}
