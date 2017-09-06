package net.osmand.plus.mapmarkers;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;

public class MarkerOptionsBottomSheetDialogFragment extends BottomSheetDialogFragment {

	public final static String TAG = "MarkerOptionsBottomSheetDialogFragment";

	private MarkerOptionsFragmentListener listener;
	private boolean portrait;

	public void setListener(MarkerOptionsFragmentListener listener) {
		this.listener = listener;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		portrait = AndroidUiHelper.isOrientationPortrait(getActivity());

		final View mainView = inflater.inflate(R.layout.fragment_marker_options_bottom_sheet_dialog, container);
		if (portrait) {
			AndroidUtils.setBackground(getActivity(), mainView, false, R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);
		}

		((ImageView) mainView.findViewById(R.id.sort_by_icon))
				.setImageDrawable(getIcon(R.drawable.ic_sort_waypoint_dark, R.color.on_map_icon_color));
		((ImageView) mainView.findViewById(R.id.show_direction_icon))
				.setImageDrawable(getIcon(R.drawable.ic_sort_waypoint_dark, R.color.on_map_icon_color));
		((ImageView) mainView.findViewById(R.id.build_route_icon))
				.setImageDrawable(getIcon(R.drawable.map_directions, R.color.on_map_icon_color));
		((ImageView) mainView.findViewById(R.id.save_as_new_track_icon))
				.setImageDrawable(getIcon(R.drawable.ic_action_polygom_dark, R.color.on_map_icon_color));
		((ImageView) mainView.findViewById(R.id.move_all_to_history_icon))
				.setImageDrawable(getIcon(R.drawable.ic_action_history2, R.color.on_map_icon_color));

		((TextView) mainView.findViewById(R.id.show_direction_text_view)).setText("Top bar");

		mainView.findViewById(R.id.sort_by_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.sortByOnClick();
				}
				dismiss();
			}
		});
		mainView.findViewById(R.id.show_direction_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.showDirectionOnClick();
				}
				dismiss();
			}
		});
		mainView.findViewById(R.id.build_route_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.buildRouteOnClick();
				}
				dismiss();
			}
		});
		mainView.findViewById(R.id.save_as_new_track_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.saveAsNewTrackOnClick();
				}
				dismiss();
			}
		});
		mainView.findViewById(R.id.move_all_to_history_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.moveAllToHistoryOnClick();
				}
				dismiss();
			}
		});
		mainView.findViewById(R.id.cancel_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		final int screenHeight = AndroidUtils.getScreenHeight(getActivity());
		final int statusBarHeight = AndroidUtils.getStatusBarHeight(getActivity());
		final int navBarHeight = AndroidUtils.getNavBarHeight(getActivity());

		mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				final View scrollView = mainView.findViewById(R.id.marker_options_scroll_view);
				int scrollViewHeight = scrollView.getHeight();
				int dividerHeight = AndroidUtils.dpToPx(getContext(), 1);
				int cancelButtonHeight = getContext().getResources().getDimensionPixelSize(R.dimen.measure_distance_bottom_sheet_cancel_button_height);
				int spaceForScrollView = screenHeight - statusBarHeight - navBarHeight - dividerHeight - cancelButtonHeight;
				if (scrollViewHeight > spaceForScrollView) {
					scrollView.getLayoutParams().height = spaceForScrollView;
					scrollView.requestLayout();
				}

				if (!portrait) {
					if (screenHeight - statusBarHeight - mainView.getHeight()
							>= AndroidUtils.dpToPx(getActivity(), 8)) {
						AndroidUtils.setBackground(getActivity(), mainView, false,
								R.drawable.bg_bottom_sheet_topsides_landscape_light, R.drawable.bg_bottom_sheet_topsides_landscape_dark);
					} else {
						AndroidUtils.setBackground(getActivity(), mainView, false,
								R.drawable.bg_bottom_sheet_sides_landscape_light, R.drawable.bg_bottom_sheet_sides_landscape_dark);
					}
				}

				ViewTreeObserver obs = mainView.getViewTreeObserver();
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					obs.removeOnGlobalLayoutListener(this);
				} else {
					obs.removeGlobalOnLayoutListener(this);
				}
			}
		});

		return mainView;
	}

	@Override
	public void onStart() {
		super.onStart();
		if (!portrait) {
			final Window window = getDialog().getWindow();
			WindowManager.LayoutParams params = window.getAttributes();
			params.width = getActivity().getResources().getDimensionPixelSize(R.dimen.landscape_bottom_sheet_dialog_fragment_width);
			window.setAttributes(params);
		}
	}

	interface MarkerOptionsFragmentListener {

		void sortByOnClick();

		void showDirectionOnClick();

		void buildRouteOnClick();

		void saveAsNewTrackOnClick();

		void moveAllToHistoryOnClick();
	}
}
