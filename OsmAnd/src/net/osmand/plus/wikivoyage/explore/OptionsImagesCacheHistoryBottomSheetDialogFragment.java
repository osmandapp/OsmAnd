package net.osmand.plus.wikivoyage.explore;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;

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

public class OptionsImagesCacheHistoryBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public final static String TAG = "OptionsImagesCasheBottomSheetDialogFragment";

	@Override
	public void createMenuItems(Bundle savedInstanceState) {

		items.add(new TitleItem(getString(R.string.shared_string_options)));

		boolean showImages = getMyApplication().getSettings().SHOW_IMAGES.get();

		BaseBottomSheetItem showWebviewImagesItem = new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(showImages)
				.setIcon(getContentIcon(R.drawable.ic_type_img))
				.setTitle(getString(R.string.show_images))
				.setLayoutId(R.layout.bottom_sheet_item_with_switch)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						OsmandSettings settings = getMyApplication().getSettings();
						settings.SHOW_IMAGES.set(!settings.SHOW_IMAGES.get());
						dismiss();
					}
				})
				.create();
		items.add(showWebviewImagesItem);

		BaseBottomSheetItem clearCacheItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.shared_string_clear))
				.setTitle(getString(R.string.images_cache))
				.setLayoutId(R.layout.bottom_sheet_item_with_right_descr)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						new WebView(getContext()).clearCache(true);
						dismiss();
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
						WikivoyageLocalDataHelper.getInstance(getMyApplication()).clearHistory();
						dismiss();
					}
				})
				.create();
		items.add(clearHistoryItem);
	}
}
