package net.osmand.plus.plugins.osmedit.dialogs;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.ShortDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.plugins.osmedit.fragments.OsmEditsFragment;
import net.osmand.plus.plugins.osmedit.fragments.OsmEditsFragment.ExportTypesDef;
import net.osmand.plus.utils.AndroidUtils;

public class ExportOptionsBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = "ExportOptionsBottomSheetDialogFragment";
	public static final String POI_COUNT_KEY = "poi_count";
	public static final String NOTES_COUNT_KEY = "notes_count";

	private ExportOptionsFragmentListener listener;

	private int poiCount;
	private int osmNotesCount;

	public void setListener(ExportOptionsFragmentListener listener) {
		this.listener = listener;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Bundle args = getArguments();
		if (args != null) {
			poiCount = args.getInt(POI_COUNT_KEY);
			osmNotesCount = args.getInt(NOTES_COUNT_KEY);
		}

		items.add(new TitleItem(getString(R.string.shared_string_export)));

		items.add(new ShortDescriptionItem(getString(R.string.osm_edits_export_desc)));

		BaseBottomSheetItem poiItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(String.valueOf(poiCount))
				.setIcon(getContentIcon(R.drawable.ic_action_info_dark))
				.setTitle(getString(R.string.poi))
				.setLayoutId(R.layout.bottom_sheet_item_with_right_descr)
				.setDisabled(!(poiCount > 0))
				.setOnClickListener(v -> {
					if (listener != null) {
						listener.onClick(OsmEditsFragment.EXPORT_TYPE_POI);
					}
					dismiss();
				})
				.create();
		items.add(poiItem);

		BaseBottomSheetItem osmNotesItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(String.valueOf(osmNotesCount))
				.setIcon(getContentIcon(R.drawable.ic_action_osm_note))
				.setTitle(getString(R.string.osm_notes))
				.setLayoutId(R.layout.bottom_sheet_item_with_right_descr)
				.setDisabled(!(osmNotesCount > 0))
				.setOnClickListener(v -> {
					if (listener != null) {
						listener.onClick(OsmEditsFragment.EXPORT_TYPE_NOTES);
					}
					dismiss();
				})
				.create();
		items.add(osmNotesItem);

		BaseBottomSheetItem allDataItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(String.valueOf(poiCount + osmNotesCount))
				.setIcon(getContentIcon(R.drawable.ic_action_folder))
				.setTitle(getString(R.string.all_data))
				.setLayoutId(R.layout.bottom_sheet_item_with_right_descr)
				.setDisabled(!(poiCount + osmNotesCount > 0))
				.setOnClickListener(v -> {
					if (listener != null) {
						listener.onClick(OsmEditsFragment.EXPORT_TYPE_ALL);
					}
					dismiss();
				})
				.create();
		items.add(allDataItem);
	}

	public static void showInstance(@NonNull FragmentManager childFragmentManager,
	                                @NonNull ExportOptionsFragmentListener listener,
	                                int poiCount, int notesCount) {
		if (AndroidUtils.isFragmentCanBeAdded(childFragmentManager, TAG)) {
			Bundle args = new Bundle();
			args.putInt(POI_COUNT_KEY, poiCount);
			args.putInt(NOTES_COUNT_KEY, notesCount);
			ExportOptionsBottomSheetDialogFragment fragment = new ExportOptionsBottomSheetDialogFragment();
			fragment.setArguments(args);
			fragment.setUsedOnMap(false);
			fragment.setListener(listener);
			fragment.show(childFragmentManager, TAG);
		}
	}

	public interface ExportOptionsFragmentListener {

		void onClick(@ExportTypesDef int type);
	}
}
