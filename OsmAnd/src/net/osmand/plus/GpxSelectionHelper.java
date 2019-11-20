package net.osmand.plus;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import java.util.Map;
import java.util.Map.Entry;
import net.osmand.GPXUtilities;
import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.GPXUtilities.Route;
import net.osmand.GPXUtilities.Track;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.MapMarkersHelper.MapMarkersGroup;
import net.osmand.plus.OsmandSettings.MetricsConstants;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetAxisType;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetType;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GpxSelectionHelper {

	private static final String CURRENT_TRACK = "currentTrack";
	private static final String FILE = "file";
	private static final String BACKUP = "backup";
	private static final String BACKUPMODIFIEDTIME = "backupTime";
	private static final String COLOR = "color";
	private static final String SELECTED_BY_USER = "selected_by_user";
	private OsmandApplication app;
	@NonNull
	private List<SelectedGpxFile> selectedGPXFiles = new java.util.ArrayList<>();
	private Map<GPXFile, Long> selectedGpxFilesBackUp = new java.util.HashMap<>();
	private SavingTrackHelper savingTrackHelper;
	private final static Log LOG = PlatformUtil.getLog(GpxSelectionHelper.class);


	public GpxSelectionHelper(OsmandApplication osmandApplication, SavingTrackHelper trackHelper) {
		this.app = osmandApplication;
		savingTrackHelper = trackHelper;
	}

	public void clearAllGpxFilesToShow(boolean backupSelection) {
		selectedGpxFilesBackUp.clear();
		if (backupSelection) {
			for(SelectedGpxFile s : selectedGPXFiles) {
				selectedGpxFilesBackUp.put(s.gpxFile, s.modifiedTime);
			}
		}
		selectedGPXFiles = new ArrayList<>();
		saveCurrentSelections();
	}

	public void restoreSelectedGpxFiles() {
		for (Entry<GPXFile, Long> gpxEntry : selectedGpxFilesBackUp.entrySet()) {
			if (!Algorithms.isEmpty(gpxEntry.getKey().path)) {
				final File file = new File(gpxEntry.getKey().path);
				if (file.exists() && !file.isDirectory()) {
					if (file.lastModified() > gpxEntry.getValue()) {
						new GpxFileLoaderTask(file, app).execute();
					} else {
						selectGpxFile(gpxEntry.getKey(), true, false);
					}
				}
			}
			saveCurrentSelections();
		}
	}

	private static class GpxFileLoaderTask extends AsyncTask<Void, Void, GPXFile> {

		File fileToLoad;
		GpxSelectionHelper helper;

		GpxFileLoaderTask(File fileToLoad, OsmandApplication app) {
			this.fileToLoad = fileToLoad;
			this.helper = app.getSelectedGpxHelper();
		}

		@Override
		protected GPXFile doInBackground(Void... voids) {
			return GPXUtilities.loadGPXFile(fileToLoad);
		}

		@Override
		protected void onPostExecute(GPXFile gpxFile) {
			if (gpxFile != null) {
				helper.selectGpxFile(gpxFile, true, false);
			}
		}
	}

	public boolean isShowingAnyGpxFiles() {
		return !selectedGPXFiles.isEmpty();
	}

	@NonNull
	public List<SelectedGpxFile> getSelectedGPXFiles() {
		return selectedGPXFiles;
	}

	public Map<GPXFile, Long> getSelectedGpxFilesBackUp() {
		return selectedGpxFilesBackUp;
	}


	@SuppressLint({"StringFormatInvalid", "StringFormatMatches"})
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
			if (g.getGpxFile().containsPoint(point)) {
				return g;
			}
		}
		return null;
	}

	public static boolean processSplit(OsmandApplication app) {
		if (app == null || app.isApplicationInitializing()) {
			return false;
		}
		List<GpxDataItem> items = app.getGpxDbHelper().getSplitItems();
		for (GpxDataItem dataItem : items) {
			SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(dataItem.getFile().getAbsolutePath());
			if (selectedGpxFile != null && selectedGpxFile.getGpxFile() != null) {
				GPXFile gpxFile = selectedGpxFile.getGpxFile();
				List<GpxDisplayGroup> groups = app.getSelectedGpxHelper().collectDisplayGroups(gpxFile);
				if (dataItem.getSplitType() == GPXDatabase.GPX_SPLIT_TYPE_NO_SPLIT) {
					for (GpxDisplayGroup model : groups) {
						model.noSplit(app);
					}
					selectedGpxFile.setDisplayGroups(groups, app);
				} else if (dataItem.getSplitType() == GPXDatabase.GPX_SPLIT_TYPE_DISTANCE) {
					for (GpxDisplayGroup model : groups) {
						model.splitByDistance(app, dataItem.getSplitInterval());
					}
					selectedGpxFile.setDisplayGroups(groups, app);
				} else if (dataItem.getSplitType() == GPXDatabase.GPX_SPLIT_TYPE_TIME) {
					for (GpxDisplayGroup model : groups) {
						model.splitByTime(app, (int) dataItem.getSplitInterval());
					}
					selectedGpxFile.setDisplayGroups(groups, app);
				}
			}
		}
		return true;
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
		group.setGeneralTrack(true);
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
		group.setGeneralTrack(t.generalTrack);
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
				if (group.getModifiableList().size() > 0) {
					dg.add(group);
				}
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

		if (!g.isPointsEmpty()) {
			GpxDisplayGroup group = new GpxDisplayGroup(g);
			group.gpxName = name;
			group.setType(GpxDisplayItemType.TRACK_POINTS);
			group.setDescription(getString(R.string.gpx_selection_number_of_points, g.getPointsSize()));
			group.setName(getString(R.string.gpx_selection_points, name));
			dg.add(group);
			List<GpxDisplayItem> list = group.getModifiableList();
			int k = 0;
			for (WptPt r : g.getPoints()) {
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

	private static void processGroupTrack(@NonNull OsmandApplication app, @NonNull GpxDisplayGroup group) {
		if (group.track == null) {
			return;
		}
		List<GpxDisplayItem> list = group.getModifiableList();
		String timeSpanClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_time_span_color));
		String speedClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_speed));
		String ascClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_altitude_asc));
		String descClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_altitude_desc));
		String distanceClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_distance_color));
		final float eleThreshold = 3;
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
		List<SelectedGpxFile> newList = new ArrayList<>(selectedGPXFiles);
		for (SelectedGpxFile s : newList) {
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

	@Nullable
	public WptPt getVisibleWayPointByLatLon(@NonNull LatLon latLon) {
		for (SelectedGpxFile selectedGpx : selectedGPXFiles) {
			GPXFile gpx;
			if (selectedGpx != null && (gpx = selectedGpx.getGpxFile()) != null) {
				for (WptPt pt : gpx.getPoints()) {
					if (latLon.equals(new LatLon(pt.getLatitude(), pt.getLongitude()))) {
						return pt;
					}
				}
			}
		}
		return null;
	}

	public void setGpxFileToDisplay(GPXFile... gpxs) {
		// special case for gpx current route
		for (GPXFile gpx : gpxs) {
			selectGpxFile(gpx, true, false);
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
					boolean selectedByUser = obj.optBoolean(SELECTED_BY_USER, true);
					if (obj.has(FILE)) {
						File fl = new File(obj.getString(FILE));
						if (p != null) {
							p.startTask(getString(R.string.loading_smth, fl.getName()), -1);
						}
						GPXFile gpx = GPXUtilities.loadGPXFile(fl);
						if (obj.has(COLOR)) {
							int clr = Algorithms.parseColor(obj.getString(COLOR));
							gpx.setColor(clr);
						}
						if (gpx.error != null) {
							save = true;
						} else if(obj.has(BACKUP)) {
							selectedGpxFilesBackUp.put(gpx, gpx.modifiedTime);
						} else {
							selectGpxFile(gpx, true, false, true, selectedByUser, false);
						}
						gpx.addGeneralTrack();
					} else if (obj.has(CURRENT_TRACK)) {
						SelectedGpxFile file = savingTrackHelper.getCurrentTrack();
						file.selectedByUser = selectedByUser;
						List<SelectedGpxFile> newSelectedGPXFiles = new ArrayList<>(selectedGPXFiles);
						newSelectedGPXFiles.add(file);
						selectedGPXFiles = newSelectedGPXFiles;
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
					obj.put(SELECTED_BY_USER, s.selectedByUser);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				ar.put(obj);
			}
		}
		for (Map.Entry<GPXFile, Long> s : selectedGpxFilesBackUp.entrySet()) {
			if (s != null) {
				try {
					JSONObject obj = new JSONObject();
					if(Algorithms.isEmpty(s.getKey().path)) {
						obj.put(CURRENT_TRACK, true);
					} else {
						obj.put(FILE, s.getKey().path);
					}
					obj.put(SELECTED_BY_USER, true);
					obj.put(BACKUP, true);
					obj.put(BACKUPMODIFIEDTIME, s.getValue());
					ar.put(obj);
				} catch (JSONException e) {
					e.printStackTrace();
				}

			}
		}
		app.getSettings().SELECTED_GPX.set(ar.toString());
	}

	private SelectedGpxFile selectGpxFileImpl(GPXFile gpx, GpxDataItem dataItem, boolean show, boolean notShowNavigationDialog, boolean syncGroup, boolean selectedByUser) {
		boolean displayed;
		SelectedGpxFile sf;
		if (gpx != null && gpx.showCurrentTrack) {
			sf = savingTrackHelper.getCurrentTrack();
			sf.notShowNavigationDialog = notShowNavigationDialog;
			displayed = selectedGPXFiles.contains(sf);
			if (!displayed && show) {
				sf.selectedByUser = selectedByUser;
			}
		} else {
			assert gpx != null;
			sf = getSelectedFileByPath(gpx.path);
			displayed = sf != null;
			if (show && sf == null) {
				sf = new SelectedGpxFile();
				if (dataItem != null && dataItem.getColor() != 0) {
					gpx.setColor(dataItem.getColor());
				}
				sf.setGpxFile(gpx, app);
				sf.notShowNavigationDialog = notShowNavigationDialog;
				sf.selectedByUser = selectedByUser;
			}
		}
		if (displayed != show) {
			List<SelectedGpxFile> newSelectedGPXFiles = new ArrayList<>(selectedGPXFiles);
			if (show) {
				newSelectedGPXFiles.add(sf);
			} else {
				newSelectedGPXFiles.remove(sf);
			}
			selectedGPXFiles = newSelectedGPXFiles;
		}
		if (syncGroup) {
			syncGpxWithMarkers(gpx);
		}
		if (sf != null) {
			sf.splitProcessed = false;
		}
		return sf;
	}

	public SelectedGpxFile selectGpxFile(GPXFile gpx, boolean show, boolean notShowNavigationDialog) {
		return selectGpxFile(gpx, show, notShowNavigationDialog, true, true, true);
	}

	public SelectedGpxFile selectGpxFile(GPXFile gpx, GpxDataItem dataItem, boolean show, boolean notShowNavigationDialog, boolean syncGroup, boolean selectedByUser) {
		SelectedGpxFile sf = selectGpxFileImpl(gpx, dataItem, show, notShowNavigationDialog, syncGroup, selectedByUser);
		saveCurrentSelections();
		return sf;
	}

	public SelectedGpxFile selectGpxFile(GPXFile gpx, boolean show, boolean notShowNavigationDialog, boolean syncGroup, boolean selectedByUser, boolean canAddToMarkers) {
		GpxDataItem dataItem = app.getGpxDbHelper().getItem(new File(gpx.path));
		if (canAddToMarkers && show && dataItem != null && dataItem.isShowAsMarkers()) {
			app.getMapMarkersHelper().addOrEnableGroup(gpx);
		}
		return selectGpxFile(gpx, dataItem, show, notShowNavigationDialog, syncGroup, selectedByUser);
	}

	public void clearPoints(GPXFile gpxFile) {
		gpxFile.clearPoints();
		syncGpxWithMarkers(gpxFile);
	}

	public void addPoint(WptPt point, GPXFile gpxFile) {
		gpxFile.addPoint(point);
		syncGpxWithMarkers(gpxFile);
	}

	public void addPoints(Collection<? extends WptPt> collection, GPXFile gpxFile) {
		gpxFile.addPoints(collection);
		syncGpxWithMarkers(gpxFile);
	}

	public boolean removePoint(WptPt point, GPXFile gpxFile) {
		boolean res = gpxFile.deleteWptPt(point);
		syncGpxWithMarkers(gpxFile);
		return res;
	}

	private void syncGpxWithMarkers(GPXFile gpxFile) {
		File gpx = new File(gpxFile.path);
		MapMarkersHelper mapMarkersHelper = app.getMapMarkersHelper();
		MapMarkersGroup group = mapMarkersHelper.getMarkersGroup(gpxFile);
		if (group != null) {
			mapMarkersHelper.runSynchronization(group);
		}
	}


	public static class SelectedGpxFile {
		public boolean notShowNavigationDialog = false;
		public boolean selectedByUser = true;

		private boolean showCurrentTrack;
		private GPXFile gpxFile;
		private int color;
		private GPXTrackAnalysis trackAnalysis;
		private long modifiedTime = -1;
		private boolean splitProcessed = false;
		private List<TrkSegment> processedPointsToDisplay = new ArrayList<>();
		private boolean routePoints;

		private List<GpxDisplayGroup> displayGroups;

		public void setGpxFile(GPXFile gpxFile, OsmandApplication app) {
			this.gpxFile = gpxFile;
			if (gpxFile.tracks.size() > 0) {
				this.color = gpxFile.tracks.get(0).getColor(0);
			}
			processPoints(app);
		}

		public GPXTrackAnalysis getTrackAnalysis(OsmandApplication app) {
			if (modifiedTime != gpxFile.modifiedTime) {
				update(app);
			}
			return trackAnalysis;
		}

		private void update(OsmandApplication app) {
			modifiedTime = gpxFile.modifiedTime;
			trackAnalysis = gpxFile.getAnalysis(
					Algorithms.isEmpty(gpxFile.path) ? System.currentTimeMillis() :
							new File(gpxFile.path).lastModified());
			displayGroups = null;
			splitProcessed = GpxSelectionHelper.processSplit(app);
		}

		public void processPoints(OsmandApplication app) {
			update(app);
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

		public List<GpxDisplayGroup> getDisplayGroups(OsmandApplication app) {
			if (modifiedTime != gpxFile.modifiedTime || !splitProcessed) {
				update(app);
			}
			return displayGroups;
		}

		public void setDisplayGroups(List<GpxDisplayGroup> displayGroups, OsmandApplication app) {
			if (modifiedTime != gpxFile.modifiedTime) {
				update(app);
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
		private boolean generalTrack;

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

		public void setColor(int color) {
			this.color = color;
		}

		public boolean isGeneralTrack() {
			return generalTrack;
		}

		public void setGeneralTrack(boolean generalTrack) {
			this.generalTrack = generalTrack;
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

		public boolean isGeneralTrack() {
			return group != null && group.isGeneralTrack();
		}
	}
}
