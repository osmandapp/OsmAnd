package net.osmand.plus.download.local.dialogs;

import static net.osmand.plus.download.DownloadActivity.LOCAL_TAB_NUMBER;
import static net.osmand.plus.download.local.OperationType.DELETE_OPERATION;
import static net.osmand.plus.importfiles.ImportHelper.IMPORT_FILE_REQUEST;
import static net.osmand.plus.utils.ColorUtilities.getAppBarColorId;
import static net.osmand.plus.utils.ColorUtilities.getToolbarActiveColorId;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.base.dialog.interfaces.dialog.IAskRefreshDialogCompletely;
import net.osmand.plus.base.dialog.interfaces.dialog.IDialogNightModeInfoProvider;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.local.*;
import net.osmand.plus.download.local.dialogs.LocalItemsAdapter.LocalItemListener;
import net.osmand.plus.download.local.dialogs.SortMapsBottomSheet.MapsSortModeListener;
import net.osmand.plus.download.local.dialogs.controllers.LocalItemsController;
import net.osmand.plus.download.local.dialogs.menu.FolderMenuProvider;
import net.osmand.plus.download.local.dialogs.menu.GroupMenuProvider;
import net.osmand.plus.download.local.dialogs.menu.ItemMenuProvider;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.settings.enums.LocalSortMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LocalItemsFragment extends LocalBaseFragment implements LocalItemListener,
		MapsSortModeListener, LocalSizeCalculationListener, IAskRefreshDialogCompletely,
		IDialogNightModeInfoProvider {

	public static final String TAG = LocalItemsFragment.class.getSimpleName();
	private static final String ITEM_TYPE_KEY = "item_type_key";

	private LocalItemType type;
	private ImportHelper importHelper;
	private LocalItemsAdapter adapter;
	private LocalItemsController controller;

	// Menu Providers
	private ItemMenuProvider itemMenuProvider;
	private GroupMenuProvider groupMenuProvider;
	private FolderMenuProvider folderMenuProvider;

	@Override
	@ColorRes
	public int getStatusBarColorId() {
		if (controller != null && controller.isSelectionMode()) {
			return ColorUtilities.getStatusBarActiveColorId(nightMode);
		} else {
			return ColorUtilities.getStatusBarColorId(nightMode);
		}
	}

	public boolean isSelectionMode() {
		return controller != null && controller.isSelectionMode();
	}

	public void setSelectionMode(boolean selectionMode) {
		if (controller != null) {
			controller.setSelectionMode(selectionMode);
		}
	}

	@NonNull
	public ItemsSelectionHelper<BaseLocalItem> getSelectionHelper() {
		return controller.getSelectionHelper();
	}

	@Nullable
	public LocalGroup getGroup() {
		LocalCategory category = getCategory();
		return category != null ? category.getGroups().get(type) : null;
	}

	@Nullable
	public LocalCategory getCategory() {
		Map<CategoryType, LocalCategory> categories = getCategories();
		return categories != null ? categories.get(type.getCategoryType()) : null;
	}

	@Nullable
	@Override
	public Map<CategoryType, LocalCategory> getCategories() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof LocalCategoriesFragment) {
			return ((LocalCategoriesFragment) fragment).getCategories();
		}
		return null;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle args = getArguments();
		if (args != null) {
			type = AndroidUtils.getSerializable(args, ITEM_TYPE_KEY, LocalItemType.class);
		}

		controller = LocalItemsController.getExistedInstance(app);
		if (controller != null) {
			controller.registerDialog(this);
		} else {
			dismiss();
			return;
		}

		DownloadActivity activity = requireDownloadActivity();
		importHelper = app.getImportHelper();

		int iconColorId = ColorUtilities.getDefaultIconColorId(nightMode);
		groupMenuProvider = new GroupMenuProvider(activity, this);
		itemMenuProvider = new ItemMenuProvider(activity, this);
		itemMenuProvider.setIconColorId(iconColorId);
		folderMenuProvider = new FolderMenuProvider(activity, this);
		folderMenuProvider.setIconColorId(iconColorId);

		activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				if (!controller.handleBackPress()) {
					dismiss();
				}
			}
		});
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.recyclerview_fragment, container, false);
		requireDownloadActivity().getAccessibilityAssistant().registerPage(view, LOCAL_TAB_NUMBER);
		setupRecyclerView(view);
		updateContent();
		return view;
	}

	private void setupRecyclerView(@NonNull View view) {
		adapter = new LocalItemsAdapter(view.getContext(), this, nightMode);

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
		recyclerView.setAdapter(adapter);
	}

	@Override
	public void onResume() {
		super.onResume();
		updateToolbar();
		LocalSizeController.addCalculationListener(app, this);
	}

	@Override
	public void onPause() {
		super.onPause();
		updateToolbar();
		LocalSizeController.removeCalculationListener(app, this);

		DownloadActivity activity = getDownloadActivity();
		if (activity != null) {
			activity.updateToolbar();
		}
	}

	private void dismiss() {
		callActivity(activity -> {
			FragmentManager manager = activity.getSupportFragmentManager();
			if (!manager.isStateSaved()) {
				manager.popBackStack(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			}
		});
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (controller != null) {
			controller.finishProcessIfNeeded(getActivity());
		}
	}

	@Override
	public void onAskRefreshDialogCompletely(@NonNull String processId) {
		updateContent();
	}

	public void updateContent() {
		if (controller != null) {
			updateAdapter();
			updateToolbar();
		}
	}

	private void updateAdapter() {
		List<Object> items = new ArrayList<>(controller.getDisplayItems(getGroup(), getCategory()));
		adapter.setSelectionMode(controller.isSelectionMode());
		adapter.setCountryMode(!controller.isRootFolder());
		adapter.setItems(items);
	}

	private void updateToolbar() {
		DownloadActivity activity = getDownloadActivity();
		ActionBar actionBar = activity != null ? activity.getSupportActionBar() : null;
		if (actionBar != null) {
			updateStatusBar(activity);
			activity.invalidateOptionsMenu();

			actionBar.setElevation(5.0f);
			LocalGroup group = getGroup();
			if (group != null) {
				actionBar.setTitle(controller.getToolbarTitle(group));
			}

			boolean selectionMode = controller.isSelectionMode();
			int colorId = selectionMode ? ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode) : 0;
			actionBar.setHomeAsUpIndicator(getIcon(selectionMode ? R.drawable.ic_action_close : AndroidUtils.getNavigationIconResId(app), colorId));

			int backgroundColorId = selectionMode ? getToolbarActiveColorId(nightMode) : getAppBarColorId(nightMode);
			actionBar.setBackgroundDrawable(new ColorDrawable(ColorUtilities.getColor(app, backgroundColorId)));
		}
	}

	@Nullable
	public List<BaseLocalItem> getCurrentFolderItems() {
		return controller != null ? controller.getCurrentFolderItems() : null;
	}

	@Override
	public boolean isItemSelected(@NonNull BaseLocalItem item) {
		return controller.isItemSelected(item);
	}

	@Override
	public boolean itemUpdateAvailable(@NonNull LocalItem item) {
		return getItemsToUpdate().containsKey(item.getFile().getName());
	}

	@Override
	public void onItemSelected(@NonNull BaseLocalItem item) {
		controller.onItemClick(item, requireActivity());
	}

	@Override
	public void onItemOptionsSelected(@NonNull BaseLocalItem item, @NonNull View view) {
		if (item instanceof MultipleLocalItem multipleLocalItem) {
			folderMenuProvider.setItem(multipleLocalItem);
			folderMenuProvider.showMenu(view);
		} else {
			itemMenuProvider.setItem(item);
			itemMenuProvider.showMenu(view);
		}
	}

	@Override
	public void setMapsSortMode(@NonNull LocalSortMode sortMode) {
		LocalItemUtils.getSortModePref(app, type).set(sortMode);
		updateAdapter();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == IMPORT_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
			if (intent != null) {
				importHelper.handleImport(intent);
			}
		} else {
			super.onActivityResult(requestCode, resultCode, intent);
		}
	}

	@Override
	public void onOperationStarted() {
		updateProgressVisibility(true);
	}

	@Override
	public void onOperationProgress(@NonNull OperationType type, @NonNull BaseLocalItem... values) {
		LocalGroup group = getGroup();
		if (type == DELETE_OPERATION && group != null) {
			for (BaseLocalItem item : values) {
				group.removeItem(app, item);
			}
		}
		if (isAdded()) {
			updateAdapter();
		}
	}

	@Override
	public void onSizeCalculationEvent(@NonNull LocalItem localItem) {
		updateContent();
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		groupMenuProvider.onCreateMenu(menu, inflater);
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull LocalItemType type, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle args = new Bundle();
			args.putSerializable(ITEM_TYPE_KEY, type);

			LocalItemsFragment fragment = new LocalItemsFragment();
			fragment.setArguments(args);
			fragment.setTargetFragment(target, 0);

			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}