package net.osmand.plus.measurementtool;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.FileUtils;
import net.osmand.GPXUtilities;
import net.osmand.data.QuadRect;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import static net.osmand.IndexConstants.GPX_FILE_EXT;

class SaveGPX extends AsyncTask<Void, Void, Exception> {

    private final MeasurementToolFragment measurementToolFragment;
    private final GPXUtilities.GPXFile gpxFile;
    private final File dir;
    private final String fileName;
    private final MeasurementToolFragment.SaveType saveType;
    private final boolean showOnMap;
    private final GpxData.ActionType actionType;
    private final MeasurementToolFragment.FinalSaveAction finalSaveAction;
    private final OsmandApplication app;
    @SuppressLint("StaticFieldLeak")
    private final MapActivity mapActivity;
    private MeasurementToolLayer measurementToolLayer;
    private boolean nightMode;
    private ProgressDialog progressDialog;
    private File backupFile;
    private File outFile;
    private GPXUtilities.GPXFile savedGpxFile;
    public MeasurementEditingContext editingCtx = new MeasurementEditingContext();
    private static final int UNDO_MODE = 0x8;

    public SaveGPX(MeasurementToolFragment measurementToolFragment, GPXUtilities.GPXFile gpxFile, File dir, String fileName, MeasurementToolFragment.SaveType saveType, boolean showOnMap, GpxData.ActionType actionType, MeasurementToolFragment.FinalSaveAction finalSaveAction, OsmandApplication app, MapActivity mapActivity, MeasurementToolLayer measurementToolLayer, boolean nightMode) {
        this.measurementToolFragment = measurementToolFragment;
        this.gpxFile = gpxFile;
        this.dir = dir;
        this.fileName = fileName;
        this.saveType = saveType;
        this.showOnMap = showOnMap;
        this.actionType = actionType;
        this.finalSaveAction = finalSaveAction;
        this.app = app;
        this.mapActivity = mapActivity;
        this.measurementToolLayer = measurementToolLayer;
        this.nightMode = nightMode;
    }

    @Override
    protected void onPreExecute() {
        measurementToolFragment.cancelModes();
        if (mapActivity != null) {
            progressDialog = new ProgressDialog(mapActivity);
            progressDialog.setMessage(measurementToolFragment.getString(R.string.saving_gpx_tracks));
            progressDialog.show();
        }
    }

    @Override
    protected Exception doInBackground(Void... voids) {
        if (app == null) {
            return null;
        }
        List<GPXUtilities.WptPt> points = editingCtx.getPoints();
        GPXUtilities.TrkSegment before = editingCtx.getBeforeTrkSegmentLine();
        GPXUtilities.TrkSegment after = editingCtx.getAfterTrkSegmentLine();
        if (gpxFile == null) {
            outFile = new File(dir, fileName);
            String trackName = fileName.substring(0, fileName.length() - GPX_FILE_EXT.length());
            GPXUtilities.GPXFile gpx = new GPXUtilities.GPXFile(Version.getFullVersion(app));
            if (measurementToolLayer != null) {
                if (saveType == MeasurementToolFragment.SaveType.LINE) {
                    GPXUtilities.TrkSegment segment = new GPXUtilities.TrkSegment();
                    if (editingCtx.hasRoute()) {
                        segment.points.addAll(editingCtx.getRoutePoints());
                    } else {
                        segment.points.addAll(before.points);
                        segment.points.addAll(after.points);
                    }
                    GPXUtilities.Track track = new GPXUtilities.Track();
                    track.name = trackName;
                    track.segments.add(segment);
                    gpx.tracks.add(track);
                } else if (saveType == MeasurementToolFragment.SaveType.ROUTE_POINT) {
                    if (editingCtx.hasRoute()) {
                        GPXUtilities.GPXFile newGpx = editingCtx.exportRouteAsGpx(trackName);
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
                showGpxOnMap(app, gpx, true);
            }
            return res;
        } else {
            GPXUtilities.GPXFile gpx = gpxFile;
            outFile = new File(gpx.path);
            backupFile = FileUtils.backupFile(app, outFile);
            String trackName = Algorithms.getFileNameWithoutExtension(outFile);
            if (measurementToolLayer != null) {
                if (measurementToolFragment.isPlanRouteMode()) {
                    if (saveType == MeasurementToolFragment.SaveType.LINE) {
                        GPXUtilities.TrkSegment segment = new GPXUtilities.TrkSegment();
                        if (editingCtx.hasRoute()) {
                            segment.points.addAll(editingCtx.getRoutePoints());
                        } else {
                            segment.points.addAll(before.points);
                            segment.points.addAll(after.points);
                        }
                        GPXUtilities.Track track = new GPXUtilities.Track();
                        track.name = trackName;
                        track.segments.add(segment);
                        gpx.tracks.add(track);
                    } else if (saveType == MeasurementToolFragment.SaveType.ROUTE_POINT) {
                        if (editingCtx.hasRoute()) {
                            GPXUtilities.GPXFile newGpx = editingCtx.exportRouteAsGpx(trackName);
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
                            List<GPXUtilities.WptPt> snappedPoints = new ArrayList<>();
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
                                GPXUtilities.TrkSegment segment = new GPXUtilities.TrkSegment();
                                segment.points.addAll(points);
                                gpx.replaceSegment(gpxData.getTrkSegment(), segment);
                            }
                            break;
                        }
                        case OVERWRITE_SEGMENT: {
                            if (gpxData != null) {
                                List<GPXUtilities.WptPt> snappedPoints = new ArrayList<>();
                                snappedPoints.addAll(before.points);
                                snappedPoints.addAll(after.points);
                                GPXUtilities.TrkSegment segment = new GPXUtilities.TrkSegment();
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
                showGpxOnMap(app, gpx, false);
            }
            return res;
        }
    }

    private void showGpxOnMap(OsmandApplication app, GPXUtilities.GPXFile gpx, boolean isNewGpx) {
        GpxSelectionHelper.SelectedGpxFile sf = app.getSelectedGpxHelper().selectGpxFile(gpx, true, false);
        if (sf != null && !isNewGpx) {
            if (actionType == GpxData.ActionType.ADD_SEGMENT || actionType == GpxData.ActionType.EDIT_SEGMENT) {
                sf.processPoints(app);
            }
        }
    }

    @Override
    protected void onPostExecute(Exception warning) {
        onGpxSaved(warning);
    }

    private void onGpxSaved(Exception warning) {
        if (mapActivity == null) {
            return;
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        mapActivity.refreshMap();
        if (warning == null) {
            if (editingCtx.isNewData() && savedGpxFile != null) {
                QuadRect rect = savedGpxFile.getRect();
                GPXUtilities.TrkSegment segment = savedGpxFile.getNonEmptyTrkSegment();
                GpxData gpxData = new GpxData(savedGpxFile, rect, GpxData.ActionType.EDIT_SEGMENT, segment);
                editingCtx.setGpxData(gpxData);
                measurementToolFragment.updateToolbar();
            }
            if (measurementToolFragment.isInEditMode()) {
                editingCtx.setChangesSaved();
                measurementToolFragment.dismiss(mapActivity);
            } else {
                switch (finalSaveAction) {
                    case SHOW_SNACK_BAR_AND_CLOSE:
                        final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
                        Snackbar snackbar = Snackbar.make(mapActivity.getLayout(),
                                MessageFormat.format(measurementToolFragment.getString(R.string.gpx_saved_sucessfully), outFile.getName()),
                                Snackbar.LENGTH_LONG)
                                .setAction(R.string.shared_string_undo, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        MapActivity mapActivity = mapActivityRef.get();
                                        if (mapActivity != null) {
                                            if (outFile != null) {
                                                OsmandApplication app = mapActivity.getMyApplication();
                                                FileUtils.removeGpxFile(app, outFile);
                                                if (backupFile != null) {
                                                    FileUtils.renameGpxFile(app, backupFile, outFile);
                                                    GPXUtilities.GPXFile gpx = GPXUtilities.loadGPXFile(outFile);
                                                    measurementToolFragment.setupGpxData(gpx);
                                                    if (showOnMap) {
                                                        showGpxOnMap(app, gpx, false);
                                                    }
                                                } else {
                                                    measurementToolFragment.setupGpxData(null);
                                                }
                                            }
                                            measurementToolFragment.setMode(UNDO_MODE, true);
                                            MeasurementToolFragment.showInstance(mapActivity.getSupportFragmentManager()
                                            );
                                        }
                                    }
                                })
                                .addCallback(new Snackbar.Callback() {
                                    @Override
                                    public void onDismissed(Snackbar transientBottomBar, int event) {
                                        if (event != DISMISS_EVENT_ACTION) {
                                            editingCtx.setChangesSaved();
                                        }
                                        super.onDismissed(transientBottomBar, event);
                                    }
                                });
                        snackbar.getView().<TextView>findViewById(com.google.android.material.R.id.snackbar_action)
                                .setAllCaps(false);
                        UiUtilities.setupSnackbar(snackbar, nightMode);
                        snackbar.show();
                        measurementToolFragment.dismiss(mapActivity);
                        break;
                    case SHOW_IS_SAVED_FRAGMENT:
                        editingCtx.setChangesSaved();
                        SavedTrackBottomSheetDialogFragment.showInstance(mapActivity.getSupportFragmentManager(),
                                outFile.getAbsolutePath());
                        measurementToolFragment.dismiss(mapActivity);
                        break;
                    case SHOW_TOAST:
                        editingCtx.setChangesSaved();
                        if (!savedGpxFile.showCurrentTrack) {
                            Toast.makeText(mapActivity,
                                    MessageFormat.format(measurementToolFragment.getString(R.string.gpx_saved_sucessfully), outFile.getAbsolutePath()),
                                    Toast.LENGTH_LONG).show();
                        }
                }
            }
        } else {
            Toast.makeText(mapActivity, warning.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
