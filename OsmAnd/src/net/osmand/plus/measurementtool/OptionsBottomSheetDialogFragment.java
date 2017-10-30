package net.osmand.plus.measurementtool;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.widgets.TextViewEx;

public class OptionsBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public final static String TAG = "OptionsBottomSheetDialogFragment";

	private OptionsFragmentListener listener;
	private boolean addLineMode;
	private boolean snapToRoadEnabled;

	public void setListener(OptionsFragmentListener listener) {
		this.listener = listener;
	}

	public void setAddLineMode(boolean addLineMode) {
		this.addLineMode = addLineMode;
	}

	public void setSnapToRoadEnabled(boolean snapToRoadEnabled) {
		this.snapToRoadEnabled = snapToRoadEnabled;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_options_bottom_sheet_dialog, null);

		if (nightMode) {
			((TextViewEx) mainView.findViewById(R.id.options_title)).setTextColor(getResources().getColor(R.color.ctx_menu_info_text_dark));
		}
		if (snapToRoadEnabled) {
			mainView.findViewById(R.id.snap_to_road_enabled_text_view).setVisibility(View.VISIBLE);
			((SwitchCompat) mainView.findViewById(R.id.snap_to_road_switch)).setChecked(true);
		}
		((ImageView) mainView.findViewById(R.id.snap_to_road_icon)).setImageDrawable(snapToRoadEnabled
				? getActiveIcon(R.drawable.ic_action_snap_to_road)
				: getContentIcon(R.drawable.ic_action_snap_to_road));
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
						listener.addToGpxOnClick();
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

		setupHeightAndBackground(mainView, R.id.measure_options_scroll_view);

		return mainView;
	}

	interface OptionsFragmentListener {

		void snapToRoadOnCLick();

		void addToGpxOnClick();

		void saveAsNewTrackOnClick();

		void addToTheTrackOnClick();

		void clearAllOnClick();
	}
}
