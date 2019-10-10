package net.osmand.plus.monitoring;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import java.io.File;
import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.myplaces.AvailableGPXFragment;
import net.osmand.plus.myplaces.AvailableGPXFragment.GpxInfo;

public class OnSaveCurrentTrackFragment extends BottomSheetDialogFragment {

	public static final String TAG = "OnSaveCurrentTrackBottomSheetFragment";
	public static final String SAVED_TRACK_KEY = "saved_track_filename";
	
	
	
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final OsmandApplication app = requiredMyApplication();
		Bundle args = getArguments();
		String savedGpxName = "";
		if (args != null && args.containsKey(SAVED_TRACK_KEY)) {
			savedGpxName = args.getString(SAVED_TRACK_KEY, "");
		} else {
			dismiss();
		}

		final File f = new File (app.getAppCustomization().getTracksDir(), savedGpxName + ".gpx");
		final boolean nightMode = !app.getSettings().isLightContent();
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		View mainView = View
			.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_on_save_current_track, container);
		TextView tv = mainView.findViewById(R.id.saved_track_name_string);
		Button openTrackBtn = mainView.findViewById(R.id.open_track_button);
		Button showOnMapBtn = mainView.findViewById(R.id.show_on_map_button);

		tv.setText(AndroidUtils.getStyledString(app.getString(R.string.shared_string_track_is_saved), savedGpxName, Typeface.BOLD));

		openTrackBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				AvailableGPXFragment.openTrack(getActivity(), f);
				dismiss();
			}
		});
		
		showOnMapBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				GpxInfo gpxInfo = new GpxInfo();
				gpxInfo.setGpx(GPXUtilities.loadGPXFile(f));
				if (gpxInfo.gpx != null) {
					OsmandSettings settings = app.getSettings();
					WptPt loc = gpxInfo.gpx.findPointToShow();
					if (loc != null) {
						settings.setMapLocationToShow(loc.lat, loc.lon, settings.getLastKnownMapZoom());
						app.getSelectedGpxHelper().setGpxFileToDisplay(gpxInfo.gpx);
					}
				}
				dismiss();
			}
		});
		return mainView;
	}

	public static void showInstance(FragmentManager fragmentManager, String filename) {
		OnSaveCurrentTrackFragment f = new OnSaveCurrentTrackFragment();
		Bundle b = new Bundle();
		b.putString(SAVED_TRACK_KEY, filename);
		f.setArguments(b);
		f.show(fragmentManager, TAG);
	}
}
