package net.osmand.plus.measurementtool;

import static net.osmand.IndexConstants.GPX_FILE_EXT;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.shared.gpx.GpxUtilities;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxUtilities.Metadata;
import net.osmand.shared.gpx.GpxUtilities.Track;
import net.osmand.shared.gpx.GpxUtilities.TrkSegment;
import net.osmand.shared.gpx.GpxUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FileUtils;
import net.osmand.util.Algorithms;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

class SaveGpxRouteAsyncTask extends AsyncTask<Void, Void, Exception> {

    private final WeakReference<MeasurementToolFragment> fragmentRef;
    private ProgressDialog progressDialog;

    private final File outFile;
    private File backupFile;
    private final GpxFile gpxFile;
    private GpxFile savedGpxFile;
    private final boolean simplified;
    private final boolean addToTrack;
    private final boolean showOnMap;

    private final SaveGpxRouteListener saveGpxRouteListener;

    public SaveGpxRouteAsyncTask(MeasurementToolFragment fragment, File outFile, GpxFile gpxFile,
                                 boolean simplified, boolean addToTrack, boolean showOnMap,
                                 SaveGpxRouteListener saveGpxRouteListener) {
        fragmentRef = new WeakReference<>(fragment);
        this.outFile = outFile;
        this.showOnMap = showOnMap;
        this.gpxFile = gpxFile;
        this.simplified = simplified;
        this.addToTrack = addToTrack;
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
        MeasurementEditingContext editingContext = fragment.getEditingCtx();
        Exception res = null;

        if (gpxFile == null) {
            String fileName = outFile.getName();
            String trackName = fileName.substring(0, fileName.length() - GPX_FILE_EXT.length());
            GpxFile gpx = generateGpxFile(editingContext, trackName, new GpxFile(Version.getFullVersion(app)));
            res = GpxUtilities.writeGpxFile(outFile, gpx);
            gpx.path = outFile.getAbsolutePath();
            savedGpxFile = gpx;
            if (showOnMap) {
                MeasurementToolFragment.showGpxOnMap(app, gpx, true);
            }
        } else {
            backupFile = FileUtils.backupFile(app, outFile);
            String trackName = Algorithms.getFileNameWithoutExtension(outFile);
            GpxFile gpx = generateGpxFile(editingContext, trackName, gpxFile);
            gpx.metadata = new Metadata(gpxFile.metadata);
            if (!gpx.showCurrentTrack) {
                res = GpxUtilities.writeGpxFile(outFile, gpx);
            }
            savedGpxFile = gpx;
            if (showOnMap) {
                MeasurementToolFragment.showGpxOnMap(app, gpx, false);
            }
        }
        if (res == null) {
            savedGpxFile.addGeneralTrack();
        }

        return res;
    }

    private GpxFile generateGpxFile(@NonNull MeasurementEditingContext editingCtx,
                                    @NonNull String trackName,
                                    @NonNull GpxFile gpx) {
        return generateGpxFile(editingCtx, trackName, gpx, simplified, addToTrack);
    }

    public static GpxFile generateGpxFile(@NonNull MeasurementEditingContext editingCtx,
                                          @NonNull String trackName,
                                          @NonNull GpxFile gpx,
                                          boolean simplified,
                                          boolean addToTrack) {
        List<TrkSegment> before = editingCtx.getBeforeTrkSegmentLine();
        List<TrkSegment> after = editingCtx.getAfterTrkSegmentLine();

        if (simplified) {
            Track track = new Track();
            track.name = trackName;
            gpx.tracks.add(track);

            GpxData gpxData = editingCtx.getGpxData();
            if (gpxData != null) {
                List<WptPt> points = gpxData.getGpxFile().getPoints();
                gpx.addPoints(points);
            }

            for (TrkSegment s : before) {
                TrkSegment segment = new TrkSegment();
                segment.points.addAll(s.points);
                track.segments.add(segment);
            }
            for (TrkSegment s : after) {
                TrkSegment segment = new TrkSegment();
                segment.points.addAll(s.points);
                track.segments.add(segment);
            }
        } else {
            GpxFile newGpx = editingCtx.exportGpx(trackName);
            if (newGpx != null) {
                if (addToTrack) {
                    newGpx.tracks.addAll(gpx.tracks);
                    newGpx.routes.addAll(gpx.routes);
                    newGpx.addPoints(gpx.getPoints());
                }
                gpx = newGpx;
            }
        }
        return gpx;
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

        void gpxSavingFinished(Exception warning, GpxFile savedGpxFile, File backupFile);
    }
}