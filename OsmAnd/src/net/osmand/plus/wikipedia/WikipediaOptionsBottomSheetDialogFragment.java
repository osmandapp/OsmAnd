package net.osmand.plus.wikipedia;

import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.utils.AndroidUtils;


public class WikipediaOptionsBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = WikipediaOptionsBottomSheetDialogFragment.class.getSimpleName();

	public static final int REQUEST_CODE = 0;
	public static final int SHOW_PICTURES_CHANGED_REQUEST_CODE = 1;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		CommonPreference<WikiArticleShowImages> showImagesPref = settings.WIKI_ARTICLE_SHOW_IMAGES;

		items.add(new TitleItem(getString(R.string.shared_string_options)));

		BaseBottomSheetItem showImagesItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(showImagesPref.get().name))
				.setDescriptionColorId(nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light)
				.setIcon(getContentIcon(R.drawable.ic_type_img))
				.setTitle(getString(R.string.download_images))
				.setLayoutId(R.layout.bottom_sheet_item_with_right_descr)
				.setOnClickListener(v -> {
					PopupMenu popup = new PopupMenu(v.getContext(), v, Gravity.END);
					for (WikiArticleShowImages showImages : WikiArticleShowImages.values()) {
						MenuItem menuItem = popup.getMenu().add(getString(showImages.name));
						menuItem.setOnMenuItemClickListener(item -> {
							showImagesPref.set(showImages);
							sendResult();
							dismiss();
							return true;
						});
					}
					popup.show();
				})
				.create();
		items.add(showImagesItem);
	}

	private void sendResult() {
		Fragment fragment = getTargetFragment();
		if (fragment != null) {
			fragment.onActivityResult(getTargetRequestCode(), SHOW_PICTURES_CHANGED_REQUEST_CODE, null);
		}
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			WikipediaOptionsBottomSheetDialogFragment fragment = new WikipediaOptionsBottomSheetDialogFragment();
			fragment.setUsedOnMap(false);
			fragment.setTargetFragment(target, REQUEST_CODE);
			fragment.show(manager, TAG);
		}
	}
}

