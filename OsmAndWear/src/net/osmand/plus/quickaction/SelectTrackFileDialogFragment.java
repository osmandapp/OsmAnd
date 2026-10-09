package net.osmand.plus.quickaction;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.track.GpxTrackAdapter;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.io.File;
import java.util.List;

public class SelectTrackFileDialogFragment extends BaseOsmAndDialogFragment {

	public static final String TAG = SelectTrackFileDialogFragment.class.getSimpleName();

	private CallbackWithObject<String> fileSelectCallback;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.select_track_file_dialog_fragment, container, false);
		setupToolbar(view);
		setupRecyclerView(view);
		return view;
	}

	private void setupToolbar(@NonNull View root) {
		Toolbar toolbar = root.findViewById(R.id.toolbar);
		int closeIconColor = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);
		Drawable closeIcon = getIcon(R.drawable.ic_action_close, closeIconColor);
		toolbar.setNavigationIcon(closeIcon);
		toolbar.setNavigationOnClickListener(v -> dismiss());
	}

	private void setupRecyclerView(@NonNull View root) {
		Context context = root.getContext();

		File gpxRootDir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		List<GPXInfo> gpxInfoList = GpxUiHelper.getSortedGPXFilesInfo(gpxRootDir, null, false);
		boolean showCurrentGpx = PluginsHelper.isActive(OsmandMonitoringPlugin.class);
		if (showCurrentGpx) {
			gpxInfoList.add(0, new GPXInfo(getString(R.string.shared_string_currently_recording_track), null));
		}
		GpxTrackAdapter adapter = new GpxTrackAdapter(context, gpxInfoList);
		adapter.setShowCurrentGpx(showCurrentGpx);
		adapter.setShowFolderName(true);
		adapter.setAdapterListener(position -> {
			Fragment target = getTargetFragment();
			CallbackWithObject<String> listener = target instanceof CallbackWithObject<?>
					? ((CallbackWithObject<String>) target)
					: null;
			if (listener != null) {
				processResult(gpxRootDir, gpxInfoList, showCurrentGpx, position, listener);
			} else if (fileSelectCallback != null) {
				processResult(gpxRootDir, gpxInfoList, showCurrentGpx, position, fileSelectCallback);
			}
			dismiss();
		});

		RecyclerView recyclerView = root.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(context));
		recyclerView.setAdapter(adapter);
	}

	private void processResult(File gpxRootDir, List<GPXInfo> gpxInfoList, boolean showCurrentGpx, int position, CallbackWithObject<String> listener) {
		boolean currentTrack = position == 0 && showCurrentGpx;
		if (currentTrack) {
			listener.processResult("");
		} else {
			GPXInfo selectedGpxInfo = gpxInfoList.get(position);
			String fileName = selectedGpxInfo.getFileName();
			String gpxFilePath = gpxRootDir.getAbsolutePath() + "/" + fileName;
			listener.processResult(gpxFilePath);
		}
	}

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager,
	                                @Nullable Fragment target) {
		showInstance(fragmentManager, target, null);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager,
	                                @Nullable Fragment target,
	                                @Nullable CallbackWithObject<String> fileSelectCallback) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			SelectTrackFileDialogFragment fragment = new SelectTrackFileDialogFragment();
			fragment.fileSelectCallback = fileSelectCallback;
			fragment.setRetainInstance(true);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}
}