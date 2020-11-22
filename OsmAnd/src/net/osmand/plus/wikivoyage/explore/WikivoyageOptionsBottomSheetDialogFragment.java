package net.osmand.plus.wikivoyage.explore;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;

import net.osmand.PicassoUtils;
import net.osmand.plus.OnDialogFragmentResultListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.CommonPreference;
import net.osmand.plus.wikipedia.WikiArticleShowImages;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerHalfItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.wikivoyage.data.TravelDbHelper;
import net.osmand.plus.wikivoyage.data.TravelLocalDataHelper;

import java.io.File;
import java.util.List;

public class WikivoyageOptionsBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public final static String TAG = WikivoyageOptionsBottomSheetDialogFragment.class.getSimpleName();

	public static final int DOWNLOAD_IMAGES_CHANGED = 1;
	public static final int CACHE_CLEARED = 2;
	public static final int TRAVEL_BOOK_CHANGED = 3;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}
		final CommonPreference<WikiArticleShowImages> showImagesPref = app.getSettings().WIKI_ARTICLE_SHOW_IMAGES;
		final TravelDbHelper dbHelper = app.getTravelDbHelper();

		items.add(new TitleItem(getString(R.string.shared_string_options)));

		if (dbHelper.getExistingTravelBooks().size() > 1) {
			BaseBottomSheetItem selectTravelBook = new BottomSheetItemWithDescription.Builder()
					.setDescription(dbHelper.formatTravelBookName(dbHelper.getSelectedTravelBook()))
					.setDescriptionColorId(nightMode ? R.color.wikivoyage_active_dark : R.color.wikivoyage_active_light)
					.setIcon(getContentIcon(R.drawable.ic_action_travel))
					.setTitle(getString(R.string.shared_string_travel_book))
					.setLayoutId(R.layout.bottom_sheet_item_with_right_descr)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							selectTravelBookDialog();
							dismiss();
						}
					})
					.create();
			items.add(selectTravelBook);

			items.add(new DividerHalfItem(getContext()));
		}

		BaseBottomSheetItem showImagesItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(showImagesPref.get().name))
				.setDescriptionColorId(nightMode ? R.color.wikivoyage_active_dark : R.color.wikivoyage_active_light)
				.setIcon(getContentIcon(R.drawable.ic_type_img))
				.setTitle(getString(R.string.download_images))
				.setLayoutId(R.layout.bottom_sheet_item_with_right_descr)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						final PopupMenu popup = new PopupMenu(v.getContext(), v, Gravity.END);
						for (final WikiArticleShowImages showImages : WikiArticleShowImages.values()) {
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
				.setDescriptionColorId(nightMode ? R.color.wikivoyage_active_dark : R.color.wikivoyage_active_light)
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
							TravelLocalDataHelper ldh = app.getTravelDbHelper().getLocalDataHelper();
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

	private void selectTravelBookDialog() {
		Context ctx = getContext();
		OsmandApplication app = getMyApplication();
		if (ctx == null || app == null) {
			return;
		}

		final TravelDbHelper dbHelper = app.getTravelDbHelper();
		final List<File> list = dbHelper.getExistingTravelBooks();
		String[] ls = new String[list.size()];
		for (int i = 0; i < ls.length; i++) {
			ls[i] = dbHelper.formatTravelBookName(list.get(i));
		}

		new AlertDialog.Builder(ctx)
				.setTitle(R.string.select_travel_book)
				.setItems(ls, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dbHelper.selectTravelBook(list.get(which));
						sendResult(TRAVEL_BOOK_CHANGED);
					}
				})
				.setNegativeButton(R.string.shared_string_dismiss, null)
				.show();
	}
}
