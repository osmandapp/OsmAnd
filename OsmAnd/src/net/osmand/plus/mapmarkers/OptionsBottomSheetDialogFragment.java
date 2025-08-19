package net.osmand.plus.mapmarkers;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;

public class OptionsBottomSheetDialogFragment extends BottomSheetDialogFragment {

	public static final String TAG = OptionsBottomSheetDialogFragment.class.getSimpleName();
	private static final String GROUPS_MARKERS_MENU = "groups_markers_menu";
	private static final String HISTORY_MARKERS_MENU = "history_markers_menu";

	private MarkerOptionsFragmentListener listener;
	private boolean disableSortBy;
	private boolean disableSaveAsTrack;
	private boolean disableMoveAllToHistory;

	public void setListener(@NonNull MarkerOptionsFragmentListener listener) {
		this.listener = listener;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		boolean groupsMenu = args != null && args.getBoolean(GROUPS_MARKERS_MENU, false);
		boolean historyMenu = args != null && args.getBoolean(HISTORY_MARKERS_MENU, false);
		disableSortBy = disableSaveAsTrack = groupsMenu || historyMenu;
		disableMoveAllToHistory = historyMenu;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.fragment_marker_options_bottom_sheet_dialog);
		view.setOnClickListener(v -> dismiss());

		View mainView = view.findViewById(R.id.main_view);
		if (!AndroidUiHelper.isOrientationPortrait(requireActivity())) {
			mainView.getLayoutParams().width = getDimensionPixelSize(R.dimen.landscape_bottom_sheet_dialog_fragment_width);
		}

		((ImageView) mainView.findViewById(R.id.sort_by_icon)).setImageDrawable(getContentIcon(R.drawable.ic_sort_waypoint_dark));
		int displayedCount = settings.DISPLAYED_MARKERS_WIDGETS_COUNT.get();
		ImageView showDirectionIcon = mainView.findViewById(R.id.show_direction_icon);
		int imageResId = displayedCount == 1
				? R.drawable.ic_action_device_topbar
				: R.drawable.ic_action_device_topbar_two;
		showDirectionIcon.setBackground(getContentIcon(R.drawable.ic_action_device_top));
		showDirectionIcon.setImageDrawable(getIcon(imageResId, R.color.active_color_primary_light));
		((ImageView) mainView.findViewById(R.id.coordinate_input_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_coordinates_longitude));
		((ImageView) mainView.findViewById(R.id.build_route_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_gdirections_dark));
		((ImageView) mainView.findViewById(R.id.save_as_new_track_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_polygom_dark));
		((ImageView) mainView.findViewById(R.id.move_all_to_history_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_history2));

		View sortByRow = mainView.findViewById(R.id.sort_by_row);
		if (disableSortBy) {
			disableView(sortByRow);
		} else {
			sortByRow.setOnClickListener(v -> {
				if (listener != null) {
					listener.sortByOnClick();
				}
				dismiss();
			});
		}
		mainView.findViewById(R.id.show_direction_row).setOnClickListener(v -> {
			if (listener != null) {
				listener.showDirectionOnClick();
			}
			dismiss();
		});
		mainView.findViewById(R.id.coordinate_input_row).setOnClickListener(v -> {
			if (listener != null) {
				listener.coordinateInputOnClick();
			}
			dismiss();
		});
		mainView.findViewById(R.id.build_route_row).setOnClickListener(v -> {
			if (listener != null) {
				listener.buildRouteOnClick();
			}
			dismiss();
		});
		View saveAsTrackRow = mainView.findViewById(R.id.save_as_new_track_row);
		if (disableSaveAsTrack) {
			disableView(saveAsTrackRow);
		} else {
			saveAsTrackRow.setOnClickListener(v -> {
				if (listener != null) {
					listener.saveAsNewTrackOnClick();
				}
				dismiss();
			});
		}
		View moveAllToHistoryRow = mainView.findViewById(R.id.move_all_to_history_row);
		if (disableMoveAllToHistory) {
			disableView(moveAllToHistoryRow);
		} else {
			moveAllToHistoryRow.setOnClickListener(v -> {
				if (listener != null) {
					listener.moveAllToHistoryOnClick();
				}
				dismiss();
			});
		}

		mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				Activity activity = getActivity();
				if (activity == null) return;

				int allowedHeight = getAllowedHeight();
				if (AndroidUiHelper.isOrientationPortrait(activity)) {
					if (allowedHeight - mainView.getHeight() >= getDimension(R.dimen.bottom_sheet_content_padding_small)) {
						AndroidUtils.setBackground(activity, mainView, nightMode, R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);
					}
				} else {
					if (allowedHeight - mainView.getHeight() >= getDimension(R.dimen.bottom_sheet_content_padding_small)) {
						AndroidUtils.setBackground(activity, mainView, nightMode,
								R.drawable.bg_bottom_sheet_topsides_landscape_light, R.drawable.bg_bottom_sheet_topsides_landscape_dark);
					} else {
						AndroidUtils.setBackground(activity, mainView, nightMode,
								R.drawable.bg_bottom_sheet_sides_landscape_light, R.drawable.bg_bottom_sheet_sides_landscape_dark);
					}
				}

				ViewTreeObserver obs = mainView.getViewTreeObserver();
				obs.removeOnGlobalLayoutListener(this);
			}
		});

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		((MapMarkersDialogFragment) getParentFragment()).setupBlurStatusBar();
	}

	@Override
	public void onPause() {
		super.onPause();
		((MapMarkersDialogFragment) getParentFragment()).restoreStatusBarColor();
	}

	@Override
	public void dismiss() {
		if (listener != null) {
			listener.dismiss();
		}
		super.dismiss();
	}

	private void disableView(View view) {
		view.setEnabled(false);
		view.setAlpha(.5f);
	}

	private int getAllowedHeight() {
		Activity activity = requireActivity();
		int scrH = AndroidUtils.getScreenHeight(activity);
		int stBarH = AndroidUtils.getStatusBarHeight(activity);
		int nBarH = AndroidUtils.getNavBarHeight(activity);
		// 56dp below is height of the bottom navigation view
		return scrH - stBarH - nBarH - dpToPx(56);
	}

	public static void showInstance(@NonNull FragmentManager fm, boolean group, boolean history,
	                                @NonNull MarkerOptionsFragmentListener listener) {
		if (AndroidUtils.isFragmentCanBeAdded(fm, TAG)) {
			Bundle args = new Bundle();
			args.putBoolean(GROUPS_MARKERS_MENU, group);
			args.putBoolean(HISTORY_MARKERS_MENU, history);

			OptionsBottomSheetDialogFragment fragment = new OptionsBottomSheetDialogFragment();
			fragment.setArguments(args);
			fragment.setListener(listener);
			fm.beginTransaction()
					.add(R.id.menu_container, fragment, OptionsBottomSheetDialogFragment.TAG)
					.commitAllowingStateLoss();
		}
	}
}
