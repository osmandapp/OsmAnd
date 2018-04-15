package net.osmand.plus.wikivoyage.explore;

import java.io.File;
import java.util.List;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AlertDialogLayout;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;
import net.osmand.PicassoUtils;
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
import net.osmand.plus.wikivoyage.data.WikivoyageDbHelper;
import net.osmand.plus.wikivoyage.data.WikivoyageLocalDataHelper;

public class WikivoyageOptionsBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public final static String TAG = "WikivoyageOptionsBottomSheetDialogFragment";

	protected void selectTravelBookDialog() {
		final WikivoyageDbHelper dbHelper = getMyApplication().getWikivoyageDbHelper();
		final List<File> list = dbHelper.getExistingTravelBooks();
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		String[] ls = new String[list.size()];
		for (int i = 0; i < ls.length; i++) {
			ls[i] = dbHelper.formatTravelBookName(list.get(i));
		}
		builder.setTitle(R.string.select_travel_book);
		builder.setItems(ls, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dbHelper.selectTravelBook(list.get(which));
			}
		});
		builder.setNegativeButton(R.string.shared_string_dismiss, null);
		builder.show();

	}
	
	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final OsmandApplication app = getMyApplication();
		final OsmandSettings.CommonPreference<Boolean> showImagesPref = app.getSettings().WIKIVOYAGE_SHOW_IMAGES;
		final WikivoyageDbHelper dbHelper = app.getWikivoyageDbHelper();
		items.add(new TitleItem(getString(R.string.shared_string_options)));
		
		if(dbHelper.getExistingTravelBooks().size() > 1) {
			BaseBottomSheetItem selectTravelBook = new BottomSheetItemWithDescription.Builder()
			.setDescription(dbHelper.formatTravelBookName(dbHelper.getSelectedTravelBook()))
			.setDescriptionColorId(nightMode ? R.color.wikivoyage_active_dark : R.color.wikivoyage_active_light)
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
			
		}

		BaseBottomSheetItem showImagesItem = new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(showImagesPref.get())
				.setIcon(getContentIcon(R.drawable.ic_type_img))
				.setTitle(getString(R.string.download_images))
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
				.setDescriptionColorId(nightMode ? R.color.wikivoyage_active_dark : R.color.wikivoyage_active_light)
				.setTitle(getString(R.string.images_cache) + ": ???") // TODO : show images cache size
				.setLayoutId(R.layout.bottom_sheet_item_with_right_descr)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						new WebView(getContext()).clearCache(true);
						PicassoUtils.clearAllPicassoCache();
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
						WikivoyageLocalDataHelper ldh = getMyApplication().getWikivoyageDbHelper().getLocalDataHelper();
						ldh.clearHistory();
						dismiss();
					}
				})
				.create();
		items.add(clearHistoryItem);
	}

	
}
