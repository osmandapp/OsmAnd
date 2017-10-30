package net.osmand.plus.measurementtool;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;

public class SaveAsNewTrackBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public final static String TAG = "SaveAsNewTrackBottomSheetDialogFragment";

	private SaveAsNewTrackFragmentListener listener;

	public void setListener(SaveAsNewTrackFragmentListener listener) {
		this.listener = listener;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_save_as_new_track_bottom_sheet_dialog, container);

		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
			mainView.findViewById(R.id.images_row).setVisibility(View.GONE);
		} else {
			final ImageView routePointImage = (ImageView) mainView.findViewById(R.id.route_point_image);
			ImageView lineImage = (ImageView) mainView.findViewById(R.id.line_image);
			if (nightMode) {
				routePointImage.setImageResource(R.drawable.img_help_trip_route_points_night);
				lineImage.setImageResource(R.drawable.img_help_trip_track_night);
			} else {
				routePointImage.setImageResource(R.drawable.img_help_trip_route_points_day);
				lineImage.setImageResource(R.drawable.img_help_trip_track_day);
			}

			mainView.findViewById(R.id.line_text).setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View view, MotionEvent motionEvent) {
					return false;
				}
			});
			lineImage.setOnClickListener(saveAsLineOnClickListener);

			mainView.findViewById(R.id.route_point_text).setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View view, MotionEvent motionEvent) {
					return false;
				}
			});
			routePointImage.setOnClickListener(saveAsRoutePointOnClickListener);
		}

		if (nightMode) {
			((TextView) mainView.findViewById(R.id.save_as_new_track_title)).setTextColor(getResources().getColor(R.color.ctx_menu_info_text_dark));
		}

		((ImageView) mainView.findViewById(R.id.route_point_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_route_points));
		((ImageView) mainView.findViewById(R.id.line_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_split_interval));

		mainView.findViewById(R.id.save_as_line_row).setOnClickListener(saveAsLineOnClickListener);
		mainView.findViewById(R.id.save_as_route_point_row).setOnClickListener(saveAsRoutePointOnClickListener);

		mainView.findViewById(R.id.cancel_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		setupHeightAndBackground(mainView, R.id.save_as_new_track_scroll_view);

		return mainView;
	}

	private View.OnClickListener saveAsLineOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			if (listener != null) {
				listener.saveAsLineOnClick();
			}
			dismiss();
		}
	};

	private View.OnClickListener saveAsRoutePointOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			if (listener != null) {
				listener.saveAsRoutePointOnClick();
			}
			dismiss();
		}
	};

	interface SaveAsNewTrackFragmentListener {

		void saveAsRoutePointOnClick();

		void saveAsLineOnClick();
	}
}
