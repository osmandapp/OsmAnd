package net.osmand.plus.wikivoyage.explore;

import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;

import androidx.appcompat.widget.PopupMenu;

import net.osmand.plus.utils.PicassoUtils;
import net.osmand.plus.OnDialogFragmentResultListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerHalfItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.wikipedia.WikiArticleShowImages;
import net.osmand.plus.wikivoyage.data.TravelLocalDataHelper;

public class WikivoyageOptionsBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = WikivoyageOptionsBottomSheetDialogFragment.class.getSimpleName();

	public static final int DOWNLOAD_IMAGES_CHANGED = 1;
	public static final int CACHE_CLEARED = 2;
	public static final int TRAVEL_BOOK_CHANGED = 3;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}
		items.add(new TitleItem(getString(R.string.shared_string_options)));

		CommonPreference<WikiArticleShowImages> showImagesPref = app.getSettings().WIKI_ARTICLE_SHOW_IMAGES;
		BaseBottomSheetItem showImagesItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(showImagesPref.get().name))
				.setDescriptionColorId(nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light)
				.setIcon(getContentIcon(R.drawable.ic_type_img))
				.setTitle(getString(R.string.download_images))
				.setLayoutId(R.layout.bottom_sheet_item_with_right_descr)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						PopupMenu popup = new PopupMenu(v.getContext(), v, Gravity.END);
						for (WikiArticleShowImages showImages : WikiArticleShowImages.values()) {
							MenuItem item = popup.getMenu().add(getString(showImages.name));
							item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
								@Override
								public boolean onMenuItemClick(MenuItem item) {
									showImagesPref.set(showImages);
									sendResult(DOWNLOAD_IMAGES_CHANGED);
									dismiss();
									return true;
								}
							});
						}
						popup.show();
					}
				})
				.create();
		items.add(showImagesItem);

		BaseBottomSheetItem clearCacheItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.shared_string_clear))
				.setDescriptionColorId(nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light)
				.setTitle(getString(R.string.images_cache))
				.setLayoutId(R.layout.bottom_sheet_item_with_right_descr)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						new WebView(getContext()).clearCache(true);
						PicassoUtils.getPicasso(getMyApplication()).clearAllPicassoCache();
						sendResult(CACHE_CLEARED);
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
						OsmandApplication app = getMyApplication();
						if (app != null) {
							TravelLocalDataHelper ldh = app.getTravelHelper().getBookmarksHelper();
							ldh.clearHistory();
						}
						dismiss();
					}
				})
				.create();
		items.add(clearHistoryItem);
	}

	private void sendResult(int resultCode) {
		OnDialogFragmentResultListener resultListener = getResultListener();
		if (resultListener != null) {
			resultListener.onDialogFragmentResult(TAG, resultCode, null);
		}
	}


}
