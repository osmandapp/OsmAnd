package net.osmand.plus.track;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.track.SaveGpxAsyncTask.SaveGpxListener;
import net.osmand.plus.widgets.EditTextEx;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;

public class GpxEditDescriptionDialogFragment extends BaseOsmAndDialogFragment {

	public static final String TAG = GpxEditDescriptionDialogFragment.class.getSimpleName();
	private static final Log log = PlatformUtil.getLog(GpxEditDescriptionDialogFragment.class);

	private static final String CONTENT_KEY = "content_key";

	private EditTextEx editableHtml;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		OsmandApplication app = getMyApplication();
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		LayoutInflater themedInflater = UiUtilities.getInflater(requireContext(), nightMode);
		View view = themedInflater.inflate(R.layout.dialog_edit_gpx_description, container, false);

		editableHtml = view.findViewById(R.id.description);

		view.findViewById(R.id.btn_close).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		view.findViewById(R.id.btn_save).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Editable editable = editableHtml.getText();
				if (!Algorithms.isEmpty(editable) && !saveGpx(editable.toString())) {
					dismiss();
				}
			}
		});

		Bundle args = getArguments();
		if (args != null) {
			String html = args.getString(CONTENT_KEY);
			if (html != null) {
				editableHtml.setText(html);
			}
		}

		return view;
	}

	private boolean saveGpx(final String html) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null || mapActivity.getTrackMenuFragment() == null) {
			return false;
		}
		TrackMenuFragment trackMenuFragment = mapActivity.getTrackMenuFragment();

		GPXFile gpx = trackMenuFragment.getGpx();
		gpx.metadata.getExtensionsToWrite().put("desc", html);

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
				MapActivity mapActivity = getMapActivity();
				if (mapActivity == null || mapActivity.getTrackMenuFragment() == null) {
					TrackMenuFragment trackMenuFragment = mapActivity.getTrackMenuFragment();
					trackMenuFragment.updateContent();
				}
				Fragment target = getTargetFragment();
				if (target instanceof GpxReadDescriptionDialogFragment) {
					((GpxReadDescriptionDialogFragment) target).updateContent(html);
				}
				dismiss();
			}
		}).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		return true;
	}

	@Nullable
	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	public static void showInstance(@NonNull FragmentActivity activity, @NonNull String description, @Nullable Fragment target) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (!fragmentManager.isStateSaved()) {
			Bundle args = new Bundle();
			args.putString(CONTENT_KEY, description);

			GpxEditDescriptionDialogFragment fragment = new GpxEditDescriptionDialogFragment();
			fragment.setArguments(args);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, GpxEditDescriptionDialogFragment.TAG);
		}
	}
}
