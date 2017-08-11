package net.osmand.plus.measurementtool;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;

public class OptionsBottomSheetDialogFragment extends BottomSheetDialogFragment {

	public final static String TAG = "OptionsBottomSheetDialogFragment";

	private OptionsOnClickListener listener;
	private boolean addLineMode;

	public void setOptionsOnClickListener(OptionsOnClickListener listener) {
		this.listener = listener;
	}

	public void setAddLineMode(boolean addLineMode) {
		this.addLineMode = addLineMode;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final boolean nightMode = getMyApplication().getDaynightHelper().isNightModeForMapControls();
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_options_bottom_sheet_dialog, null);

		((ImageView) mainView.findViewById(R.id.snap_to_road_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_snap_to_road));
		((ImageView) mainView.findViewById(R.id.clear_all_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_reset_to_default_dark));
		if (!addLineMode) {
			((ImageView) mainView.findViewById(R.id.save_as_new_track_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_polygom_dark));
			((ImageView) mainView.findViewById(R.id.add_to_the_track_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_split_interval));
		} else {
			mainView.findViewById(R.id.save_as_new_track_row).setVisibility(View.GONE);
			mainView.findViewById(R.id.add_to_the_track_row).setVisibility(View.GONE);
			mainView.findViewById(R.id.save_as_new_segment_row).setVisibility(View.VISIBLE);
			((ImageView) mainView.findViewById(R.id.save_as_new_segment_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_polygom_dark));
		}

		mainView.findViewById(R.id.snap_to_road_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.snapToRoadOnCLick();
				}
				dismiss();
			}
		});
		mainView.findViewById(R.id.clear_all_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.clearAllOnClick();
				}
				dismiss();
			}
		});
		if (!addLineMode) {
			mainView.findViewById(R.id.save_as_new_track_row).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (listener != null) {
						listener.saveAsNewTrackOnClick();
					}
					dismiss();
				}
			});
			mainView.findViewById(R.id.add_to_the_track_row).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (listener != null) {
						listener.addToTheTrackOnClick();
					}
					dismiss();
				}
			});
		} else {
			mainView.findViewById(R.id.save_as_new_segment_row).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (listener != null) {
						listener.saveAsNewSegmentOnClick();
					}
					dismiss();
				}
			});
		}
		mainView.findViewById(R.id.cancel_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		return mainView;
	}

	interface OptionsOnClickListener {

		void snapToRoadOnCLick();

		void saveAsNewSegmentOnClick();

		void saveAsNewTrackOnClick();

		void addToTheTrackOnClick();

		void clearAllOnClick();
	}
}
