package net.osmand.plus.measurementtool;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.ShortDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;

public class SaveAsNewTrackBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public final static String TAG = "SaveAsNewTrackBottomSheetDialogFragment";

	private SaveAsNewTrackFragmentListener listener;

	public void setListener(SaveAsNewTrackFragmentListener listener) {
		this.listener = listener;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}

		items.add(new TitleItem(getString(R.string.shared_string_save_as_gpx)));

		items.add(new ShortDescriptionItem(getString(R.string.measurement_tool_save_as_new_track_descr)));

		if (Build.VERSION.SDK_INT >= 18) {
			final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
			View imagesRow = View.inflate(new ContextThemeWrapper(getContext(), themeRes),
					R.layout.fragment_save_as_new_track_images_row, null);

			final ImageView routePointImage = (ImageView) imagesRow.findViewById(R.id.route_point_image);
			final ImageView lineImage = (ImageView) imagesRow.findViewById(R.id.line_image);
			Drawable routePointDrawable = ContextCompat.getDrawable(app, nightMode
					? R.drawable.img_help_trip_route_points_night
					: R.drawable.img_help_trip_route_points_day);
			Drawable lineDrawable = ContextCompat.getDrawable(app, nightMode
					? R.drawable.img_help_trip_track_night
					: R.drawable.img_help_trip_track_day);
			if (routePointDrawable != null && lineDrawable != null) {
				routePointImage.setImageDrawable(AndroidUtils.getDrawableForDirection(app, routePointDrawable));
				lineImage.setImageDrawable(AndroidUtils.getDrawableForDirection(app, lineDrawable));
			}
			routePointImage.setOnClickListener(saveAsRoutePointOnClickListener);
			lineImage.setOnClickListener(saveAsLineOnClickListener);

			View.OnTouchListener textOnTouchListener = new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					return false;
				}
			};
			imagesRow.findViewById(R.id.line_text).setOnTouchListener(textOnTouchListener);
			imagesRow.findViewById(R.id.route_point_text).setOnTouchListener(textOnTouchListener);

			items.add(new BaseBottomSheetItem.Builder().setCustomView(imagesRow).create());
		}

		BaseBottomSheetItem saveAsRoutePointsItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_route_points))
				.setTitle(getString(R.string.save_as_route_point))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(saveAsRoutePointOnClickListener)
				.create();
		items.add(saveAsRoutePointsItem);

		BaseBottomSheetItem saveAsLineItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_split_interval))
				.setTitle(getString(R.string.save_as_line))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(saveAsLineOnClickListener)
				.create();
		items.add(saveAsLineItem);
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
