package net.osmand.plus.mapmarkers;

import android.os.Bundle;
import android.view.View;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.util.Algorithms;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HistoryMarkerMenuBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = "HistoryMarkerMenuBottomSheetDialogFragment";

	public static final String MARKER_POSITION = "marker_position";
	public static final String MARKER_NAME = "marker_name";
	public static final String MARKER_COLOR_INDEX = "marker_color_index";
	public static final String MARKER_VISITED_DATE = "marker_visited_date";

	private HistoryMarkerMenuFragmentListener listener;

	public void setListener(HistoryMarkerMenuFragmentListener listener) {
		this.listener = listener;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Bundle arguments = getArguments();

		if (arguments != null) {
			int pos = arguments.getInt(MARKER_POSITION);
			String markerName = arguments.getString(MARKER_NAME);
			int markerColorIndex = arguments.getInt(MARKER_COLOR_INDEX);
			long markerVisitedDate = arguments.getLong(MARKER_VISITED_DATE);

			Date date = new Date(markerVisitedDate);
			String month = new SimpleDateFormat("MMM", Locale.getDefault()).format(date);
			month = Algorithms.capitalizeFirstLetter(month);
			month = month.replaceAll("\\.", "");
			String day = new SimpleDateFormat("d", Locale.getDefault()).format(date);

			BaseBottomSheetItem markerItem = new BottomSheetItemWithDescription.Builder()
					.setDescription(getString(R.string.passed, month + " " + day))
					.setIcon(getIcon(R.drawable.ic_action_flag, MapMarker.getColorId(markerColorIndex)))
					.setTitle(markerName)
					.setLayoutId(R.layout.bottom_sheet_item_with_descr_56dp)
					.create();
			items.add(markerItem);

			items.add(new DividerItem(getContext()));

			BaseBottomSheetItem makeActiveItem = new SimpleBottomSheetItem.Builder()
					.setIcon(getContentIcon(R.drawable.ic_action_reset_to_default_dark))
					.setTitle(getString(R.string.make_active))
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (listener != null) {
								listener.onMakeMarkerActive(pos);
							}
							dismiss();
						}
					})
					.create();
			items.add(makeActiveItem);

			BaseBottomSheetItem deleteItem = new SimpleBottomSheetItem.Builder()
					.setIcon(getContentIcon(R.drawable.ic_action_delete_dark))
					.setTitle(getString(R.string.shared_string_delete))
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (listener != null) {
								listener.onDeleteMarker(pos);
							}
							dismiss();
						}
					})
					.create();
			items.add(deleteItem);
		}
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	interface HistoryMarkerMenuFragmentListener {

		void onMakeMarkerActive(int pos);

		void onDeleteMarker(int pos);
	}
}
