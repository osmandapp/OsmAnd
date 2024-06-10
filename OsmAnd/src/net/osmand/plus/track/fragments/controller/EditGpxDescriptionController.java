package net.osmand.plus.track.fragments.controller;

import android.view.View;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.shared.gpx.GpxFile;
import net.osmand.PlatformUtil;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.track.fragments.EditDescriptionFragment.OnDescriptionSavedCallback;
import net.osmand.plus.track.fragments.ReadGpxDescriptionFragment;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.save.SaveGpxHelper;
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
		GpxFile gpxFile = getGpxFile();
		if (gpxFile != null) {
			webView.setWebViewClient(new ArticleWebViewClient(fragment, activity, gpxFile, view, true));
		}
	}

	@Override
	public void saveEditedDescription(@NonNull String editedText, @NonNull OnDescriptionSavedCallback callback) {
		TrackMenuFragment trackMenuFragment = activity.getFragmentsHelper().getTrackMenuFragment();
		if (trackMenuFragment == null) {
			return;
		}

		GpxFile gpx = trackMenuFragment.getGpx();
		gpx.getMetadata().setDesc(editedText);

		File file = trackMenuFragment.getDisplayHelper().getFile();
		SaveGpxHelper.saveGpx(file, gpx, errorMessage -> {
			if (errorMessage != null) {
				log.error(errorMessage);
			}
			if (activity.getFragmentsHelper().getTrackMenuFragment() != null) {
				activity.getFragmentsHelper().getTrackMenuFragment().updateContent();
			}
			callback.onDescriptionSaved();
		});
	}

	@Nullable
	private GpxFile getGpxFile() {
		TrackMenuFragment trackMenuFragment = activity.getFragmentsHelper().getTrackMenuFragment();
		if (trackMenuFragment != null) {
			TrackDisplayHelper displayHelper = trackMenuFragment.getDisplayHelper();
			if (displayHelper != null) {
				return displayHelper.getGpx();
			}
		}
		return null;
	}

}
