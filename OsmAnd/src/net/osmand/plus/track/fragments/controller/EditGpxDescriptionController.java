package net.osmand.plus.track.fragments.controller;

import android.os.AsyncTask;
import android.view.View;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXFile;
import net.osmand.PlatformUtil;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.track.SaveGpxAsyncTask;
import net.osmand.plus.track.SaveGpxAsyncTask.SaveGpxListener;
import net.osmand.plus.track.fragments.EditDescriptionFragment.OnDescriptionSavedCallback;
import net.osmand.plus.track.fragments.ReadGpxDescriptionFragment;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.TrackDisplayHelper;
import net.osmand.plus.wikivoyage.ArticleWebViewClient;

import org.apache.commons.logging.Log;

import java.io.File;

public class EditGpxDescriptionController extends EditDescriptionController {

	private static final Log log = PlatformUtil.getLog(EditGpxDescriptionController.class);

	public EditGpxDescriptionController(@NonNull MapActivity mapActivity) {
		super(mapActivity);
	}

	public void setupWebViewController(@NonNull WebView webView, @NonNull View view, @NonNull ReadGpxDescriptionFragment fragment) {
		GPXFile gpxFile = getGpxFile();
		if (gpxFile != null) {
			webView.setWebViewClient(new ArticleWebViewClient(fragment, activity, gpxFile, view, true));
		}
	}

	@Override
	public void saveEditedDescription(@NonNull String editedText, @NonNull OnDescriptionSavedCallback callback) {
		TrackMenuFragment trackMenuFragment = activity.getTrackMenuFragment();
		if (trackMenuFragment == null) {
			return;
		}

		GPXFile gpx = trackMenuFragment.getGpx();
		gpx.metadata.getExtensionsToWrite().put("desc", editedText);

		File file = trackMenuFragment.getDisplayHelper().getFile();
		new SaveGpxAsyncTask(file, gpx, new SaveGpxListener() {
			@Override
			public void gpxSavingStarted() {
			}

			@Override
			public void gpxSavingFinished(Exception errorMessage) {
				if (errorMessage != null) {
					log.error(errorMessage);
				}
				if (activity.getTrackMenuFragment() != null) {
					TrackMenuFragment trackMenuFragment = activity.getTrackMenuFragment();
					trackMenuFragment.updateContent();
				}
				callback.onDescriptionSaved();
			}
		}).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Nullable
	private GPXFile getGpxFile() {
		TrackMenuFragment trackMenuFragment = activity.getTrackMenuFragment();
		if (trackMenuFragment != null) {
			TrackDisplayHelper displayHelper = trackMenuFragment.getDisplayHelper();
			if (displayHelper != null) {
				return displayHelper.getGpx();
			}
		}
		return null;
	}

}
