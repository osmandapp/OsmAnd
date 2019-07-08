package net.osmand.plus.monitoring;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import java.io.File;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.myplaces.AvailableGPXFragment;

public class OnSaveCurrentTrackFragment extends BottomSheetDialogFragment {

	public static final String TAG = "OnSaveCurrentTrackBottomSheetFragment";
	public static final String SAVED_TRACK_KEY = "saved_track_filename";
	
	
	
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final OsmandApplication app = getMyApplication();
		Bundle args = getArguments();
		String savedGpxName = "";
		if (args != null && args.containsKey(SAVED_TRACK_KEY)) {
			savedGpxName = args.getString(SAVED_TRACK_KEY, "");
		} else {
			dismiss();
		}
		
		final File f = new File (app.getAppCustomization().getTracksDir() +"/"+ savedGpxName + ".gpx");
		final boolean nightMode = !app.getSettings().isLightContent();
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		View mainView = View
			.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_on_save_current_track, container);
		TextView tv = mainView.findViewById(R.id.saved_track_name_string);
		Button openTrackBtn = mainView.findViewById(R.id.open_track_button);
		Button showOnMapBtn = mainView.findViewById(R.id.show_on_map_button);
		
		SpannableStringBuilder ssb = new SpannableStringBuilder();
		ssb.append(app.getResources().getString(R.string.shared_string_gpx_track)).append(" ");
		int startIndex = ssb.length();
		ssb.append(savedGpxName).append(" ");
		ssb.setSpan(new StyleSpan(Typeface.BOLD), startIndex, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		startIndex = ssb.length();
		ssb.append(app.getResources().getString(R.string.shared_string_saved));
		ssb.setSpan(new StyleSpan(Typeface.NORMAL), startIndex, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		tv.setText(ssb);
		
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
				//show track on MapActivity
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
