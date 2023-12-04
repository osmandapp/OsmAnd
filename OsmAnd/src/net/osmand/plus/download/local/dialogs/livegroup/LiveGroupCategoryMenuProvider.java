package net.osmand.plus.download.local.dialogs.livegroup;

import static net.osmand.plus.importfiles.ImportHelper.IMPORT_FILE_REQUEST;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.local.LocalGroup;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.download.local.dialogs.LocalSearchFragment;
import net.osmand.plus.download.local.dialogs.SortMapsBottomSheet;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.settings.enums.MapsSortMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import java.util.ArrayList;
import java.util.List;

public class LiveGroupCategoryMenuProvider implements MenuProvider {

	private final OsmandApplication app;
	private final UiUtilities uiUtilities;
	private final DownloadActivity activity;
	private final LiveGroupItemsFragment fragment;
	private final boolean nightMode;

	public LiveGroupCategoryMenuProvider(@NonNull DownloadActivity activity, @NonNull LiveGroupItemsFragment fragment) {
		this.activity = activity;
		this.fragment = fragment;
		this.nightMode = fragment.isNightMode();
		app = activity.getMyApplication();
		uiUtilities = app.getUIUtilities();
	}

	@Override
	public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		menu.clear();

		LocalGroup group = fragment.getGroup();
		if (group == null) {
			return;
		}
		int colorId = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);

			MenuItem searchItem = menu.add(0, R.string.shared_string_search, 0, R.string.shared_string_search);
			searchItem.setIcon(getIcon(R.drawable.ic_action_search_dark, colorId));
			searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			searchItem.setOnMenuItemClickListener(item -> {
				FragmentManager manager = activity.getSupportFragmentManager();
				LocalSearchFragment.showInstance(manager, group.getType(), null, fragment);
				return true;
			});
		LocalItemType type = group.getType();
		if (type.isMapsSortingSupported()) {
			MapsSortMode sortMode = app.getSettings().LOCAL_MAPS_SORT_MODE.get();
			MenuItem sortItem = menu.add(0, R.string.shared_string_sort, 0, R.string.shared_string_sort);
			sortItem.setIcon(getIcon(sortMode.getIconId(), colorId));
			sortItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			sortItem.setOnMenuItemClickListener(item -> {
				FragmentManager manager = activity.getSupportFragmentManager();
				SortMapsBottomSheet.showInstance(manager, fragment);
				return true;
			});
		}
		MenuItem actionsItem = menu.add(0, R.string.shared_string_more, 0, R.string.shared_string_more);
		actionsItem.setIcon(getIcon(R.drawable.ic_overflow_menu_white, colorId));
		actionsItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		actionsItem.setOnMenuItemClickListener(item -> {
			showAdditionalActions(activity.findViewById(item.getItemId()));
			return true;
		});
	}

	private void showAdditionalActions(@NonNull View view) {
		List<PopUpMenuItem> items = new ArrayList<>();
		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_import)
				.setIcon(getContentIcon(R.drawable.ic_action_import))
				.setOnClickListener(v -> {
					Intent intent = ImportHelper.getImportFileIntent();
					AndroidUtils.startActivityForResultIfSafe(fragment, intent, IMPORT_FILE_REQUEST);
				})
				.showTopDivider(true)
				.create());

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		PopUpMenu.show(displayData);
	}

	@Override
	public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
		return false;
	}

	@Nullable
	private Drawable getIcon(@DrawableRes int id, @ColorRes int colorId) {
		return uiUtilities.getIcon(id, colorId);
	}

	@Nullable
	private Drawable getContentIcon(@DrawableRes int id) {
		return uiUtilities.getThemedIcon(id);
	}
}