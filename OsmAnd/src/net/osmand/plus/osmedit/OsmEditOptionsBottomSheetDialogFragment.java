package net.osmand.plus.osmedit;

import android.os.Bundle;
import android.view.View;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerHalfItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.util.Algorithms;

public class OsmEditOptionsBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = "OsmEditOptionsBottomSheetDialogFragment";

	public static final String OSM_POINT = "osm_point";

	private OsmEditOptionsFragmentListener listener;

	public void setListener(OsmEditOptionsFragmentListener listener) {
		this.listener = listener;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Bundle args = getArguments();
		if (args != null) {
			final OsmPoint osmPoint = (OsmPoint) args.getSerializable(OSM_POINT);
			String name = OsmEditingPlugin.getName(osmPoint);
			if (Algorithms.isEmpty(name)) {
				name = OsmEditingPlugin.getCategory(osmPoint, getContext());
			}
			items.add(new TitleItem(name + ":"));

			BaseBottomSheetItem uploadItem = new SimpleBottomSheetItem.Builder()
					.setIcon(getContentIcon(R.drawable.ic_action_export))
					.setTitle(getString(R.string.local_openstreetmap_upload))
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (listener != null) {
								listener.onUploadClick(osmPoint);
							}
							dismiss();
						}
					})
					.create();
			items.add(uploadItem);

			BaseBottomSheetItem showOnMapItem = new SimpleBottomSheetItem.Builder()
					.setIcon(getContentIcon(R.drawable.ic_show_on_map))
					.setTitle(getString(R.string.shared_string_show_on_map))
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (listener != null) {
								listener.onShowOnMapClick(osmPoint);
							}
							dismiss();
						}
					})
					.create();
			items.add(showOnMapItem);

			items.add(new DividerHalfItem(getContext()));

			if (osmPoint instanceof OpenstreetmapPoint && osmPoint.getAction() != OsmPoint.Action.DELETE) {
				BaseBottomSheetItem modifyOsmChangeItem = new SimpleBottomSheetItem.Builder()
						.setIcon(getContentIcon(R.drawable.ic_action_edit_dark))
						.setTitle(getString(R.string.poi_context_menu_modify_osm_change))
						.setLayoutId(R.layout.bottom_sheet_item_simple)
						.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								if (listener != null) {
									listener.onModifyOsmChangeClick(osmPoint);
								}
								dismiss();
							}
						})
						.create();
				items.add(modifyOsmChangeItem);
			}

			if (osmPoint instanceof OsmNotesPoint) {
				BaseBottomSheetItem modifyOsmNoteItem = new SimpleBottomSheetItem.Builder()
						.setIcon(getContentIcon(R.drawable.ic_action_edit_dark))
						.setTitle(getString(R.string.context_menu_item_modify_note))
						.setLayoutId(R.layout.bottom_sheet_item_simple)
						.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								if (listener != null) {
									listener.onModifyOsmNoteClick(osmPoint);
								}
								dismiss();
							}
						})
						.create();
				items.add(modifyOsmNoteItem);
			}

			BaseBottomSheetItem deleteItem = new SimpleBottomSheetItem.Builder()
					.setIcon(getContentIcon(R.drawable.ic_action_delete_dark))
					.setTitle(getString(R.string.shared_string_delete))
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (listener != null) {
								listener.onDeleteClick(osmPoint);
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

	public interface OsmEditOptionsFragmentListener {

		void onUploadClick(OsmPoint osmPoint);

		void onShowOnMapClick(OsmPoint osmPoint);

		void onModifyOsmChangeClick(OsmPoint osmPoint);

		void onModifyOsmNoteClick(OsmPoint osmPoint);

		void onDeleteClick(OsmPoint osmPoint);
	}
}
