package net.osmand.plus.wikivoyage.explore;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerHalfItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.wikivoyage.data.WikivoyageLocalDataHelper;

public class WikivoyageOptionsBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public final static String TAG = "WikivoyageOptionsBottomSheetDialogFragment";

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final OsmandApplication app = getMyApplication();
		final OsmandSettings.CommonPreference<Boolean> showImagesPref = app.getSettings().WIKIVOYAGE_SHOW_IMAGES;

		items.add(new TitleItem(getString(R.string.shared_string_options)));

		BaseBottomSheetItem showImagesItem = new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(showImagesPref.get())
				.setIcon(getContentIcon(R.drawable.ic_type_img))
				.setTitle(getString(R.string.show_images))
				.setLayoutId(R.layout.bottom_sheet_item_with_switch)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						showImagesPref.set(!showImagesPref.get());
						dismiss();
					}
				})
				.create();
		items.add(showImagesItem);

		BaseBottomSheetItem clearCacheItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.shared_string_clear))
				.setTitle(getString(R.string.images_cache) + ": ???") // TODO : show images cache size
				.setLayoutId(R.layout.bottom_sheet_item_with_right_descr)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						// TODO : implement clearing of cache
						Toast.makeText(getContext(), "Currently in development", Toast.LENGTH_SHORT).show();
					}
				})
				.create();
		items.add(clearCacheItem);

		items.add(new DividerHalfItem(getContext()));

		BaseBottomSheetItem clearHistoryItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_history))
				.setTitle(getString(R.string.delete_search_history))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						WikivoyageLocalDataHelper.getInstance(app).clearHistory();
						dismiss();
					}
				})
				.create();
		items.add(clearHistoryItem);
	}
}
