package net.osmand.plus.measurementtool;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import net.osmand.AndroidUtils;
import net.osmand.FileUtils;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.Track;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.measurementtool.GpxData.ActionType;
import net.osmand.plus.measurementtool.MeasurementToolFragment.SaveType;
import net.osmand.util.Algorithms;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static net.osmand.IndexConstants.GPX_FILE_EXT;

class SaveGpxRouteAsyncTask extends AsyncTask<Void, Void, Exception> {

    private WeakReference<MeasurementToolFragment> fragmentRef;
    private ProgressDialog progressDialog;

    private SaveType saveType;
    private ActionType actionType;

    private File outFile;
    private File backupFile;
    private GPXFile gpxFile;
    private GPXFile savedGpxFile;
    private boolean showOnMap;

    private SaveGpxRouteListener saveGpxRouteListener;


    public SaveGpxRouteAsyncTask(MeasurementToolFragment fragment, File outFile, GPXFile gpxFile,
                                 ActionType actionType, SaveType saveType, boolean showOnMap, SaveGpxRouteListener saveGpxRouteListener) {
        fragmentRef = new WeakReference<>(fragment);
        this.outFile = outFile;
        this.showOnMap = showOnMap;
        this.gpxFile = gpxFile;
        this.actionType = actionType;
        this.saveType = saveType;
        this.saveGpxRouteListener = saveGpxRouteListener;
    }

    @Override
    protected void onPreExecute() {
        MeasurementToolFragment fragment = fragmentRef.get();
        if (fragment != null && fragment.getContext() != null) {
            fragment.cancelModes();

            Context ctx = fragment.getContext();
            progressDialog = new ProgressDialog(ctx);
            progressDialog.setMessage(ctx.getString(R.string.saving_gpx_tracks));
            progressDialog.show();
        }
    }

    @Override
    protected Exception doInBackground(Void... voids) {
        MeasurementToolFragment fragment = fragmentRef.get();
        if (fragment == null || fragment.getActivity() == null) {
            return null;
        }
        MapActivity mapActivity = (MapActivity) fragment.getActivity();
        OsmandApplication app = mapActivity.getMyApplication();
        MeasurementToolLayer measurementLayer = mapActivity.getMapLayers().getMeasurementToolLayer();
        MeasurementEditingContext editingCtx = fragment.getEditingCtx();

        List<WptPt> points = editingCtx.getPoints();
        TrkSegment before = editingCtx.getBeforeTrkSegmentLine();
        TrkSegment after = editingCtx.getAfterTrkSegmentLine();
        if (gpxFile == null) {
            String fileName = outFile.getName();
            String trackName = fileName.substring(0, fileName.length() - GPX_FILE_EXT.length());
            GPXFile gpx = new GPXFile(Version.getFullVersion(app));
            if (measurementLayer != null) {
                if (saveType == MeasurementToolFragment.SaveType.LINE) {
                    TrkSegment segment = new TrkSegment();
                    if (editingCtx.hasRoute()) {
                        segment.points.addAll(editingCtx.getRoutePoints());
                    } else {
                        segment.points.addAll(before.points);
                        segment.points.addAll(after.points);
                    }
                    Track track = new Track();
                    track.name = trackName;
                    track.segments.add(segment);
                    gpx.tracks.add(track);
                } else if (saveType == MeasurementToolFragment.SaveType.ROUTE_POINT) {
                    if (editingCtx.hasRoute()) {
                        GPXFile newGpx = editingCtx.exportRouteAsGpx(trackName);
                        if (newGpx != null) {
                            gpx = newGpx;
                        }
                    }
                    gpx.addRoutePoints(points);
                }
            }
            Exception res = GPXUtilities.writeGpxFile(outFile, gpx);
            gpx.path = outFile.getAbsolutePath();
            savedGpxFile = gpx;
            if (showOnMap) {
                MeasurementToolFragment.showGpxOnMap(app, gpx, actionType, true);
            }
            return res;
        } else {
            GPXFile gpx = gpxFile;
            backupFile = FileUtils.backupFile(app, outFile);
            String trackName = Algorithms.getFileNameWithoutExtension(outFile);
            if (measurementLayer != null) {
                if (fragment.isPlanRouteMode()) {
                    if (saveType == MeasurementToolFragment.SaveType.LINE) {
                        TrkSegment segment = new TrkSegment();
                        if (editingCtx.hasRoute()) {
                            segment.points.addAll(editingCtx.getRoutePoints());
                        } else {
                            segment.points.addAll(before.points);
                            segment.points.addAll(after.points);
                        }
                        Track track = new Track();
                        track.name = trackName;
                        track.segments.add(segment);
                        gpx.tracks.add(track);
                    } else if (saveType == MeasurementToolFragment.SaveType.ROUTE_POINT) {
                        if (editingCtx.hasRoute()) {
                            GPXFile newGpx = editingCtx.exportRouteAsGpx(trackName);
                            if (newGpx != null) {
                                gpx = newGpx;
                            }
                        }
                        gpx.addRoutePoints(points);
                    }
                } else if (actionType != null) {
                    GpxData gpxData = editingCtx.getGpxData();
                    switch (actionType) {
                        case ADD_SEGMENT: {
                            List<WptPt> snappedPoints = new ArrayList<>();
                            snappedPoints.addAll(before.points);
                            snappedPoints.addAll(after.points);
                            gpx.addTrkSegment(snappedPoints);
                            break;
                        }
                        case ADD_ROUTE_POINTS: {
                            gpx.replaceRoutePoints(points);
                            break;
                        }
                        case EDIT_SEGMENT: {
                            if (gpxData != null) {
                                TrkSegment segment = new TrkSegment();
                                segment.points.addAll(points);
                                gpx.replaceSegment(gpxData.getTrkSegment(), segment);
                            }
                            break;
                        }
                        case OVERWRITE_SEGMENT: {
                            if (gpxData != null) {
                                List<WptPt> snappedPoints = new ArrayList<>();
                                snappedPoints.addAll(before.points);
                                snappedPoints.addAll(after.points);
                                TrkSegment segment = new TrkSegment();
                                segment.points.addAll(snappedPoints);
                                gpx.replaceSegment(gpxData.getTrkSegment(), segment);
                            }
                            break;
                        }
                    }
                } else {
                    gpx.addRoutePoints(points);
                }
            }
            Exception res = null;
            if (!gpx.showCurrentTrack) {
                res = GPXUtilities.writeGpxFile(outFile, gpx);
            }
            savedGpxFile = gpx;
            if (showOnMap) {
                MeasurementToolFragment.showGpxOnMap(app, gpx, actionType, false);
            }
            return res;
        }
    }

    @Override
    protected void onPostExecute(Exception warning) {
        MeasurementToolFragment fragment = fragmentRef.get();
        if (fragment != null && progressDialog != null && AndroidUtils.isActivityNotDestroyed(fragment.getActivity())) {
            progressDialog.dismiss();
        }
        if (saveGpxRouteListener != null) {
            saveGpxRouteListener.gpxSavingFinished(warning, savedGpxFile, backupFile);
        }
    }

    public interface SaveGpxRouteListener {

        void gpxSavingFinished(Exception warning, GPXFile savedGpxFile, File backupFile);
    }
}