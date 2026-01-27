package net.osmand.plus.myplaces.favorites.dialogs;

import static net.osmand.plus.myplaces.MyPlacesActivity.FAV_TAB;
import static net.osmand.plus.myplaces.MyPlacesActivity.TAB_ID;
import static net.osmand.plus.myplaces.favorites.dialogs.FavoritesTreeFragment.IMPORT_FAVOURITES_REQUEST;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.mapcontextmenu.other.ShareMenu.NativeShareDialogBuilder;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.myplaces.favorites.ShareFavoritesAsyncTask;
import net.osmand.plus.myplaces.favorites.ShareFavoritesAsyncTask.ShareFavoritesListener;
import net.osmand.plus.myplaces.favorites.dialogs.FavoriteFoldersAdapter.FavoriteAdapterListener;
import net.osmand.plus.myplaces.favorites.dialogs.SortFavoriteViewHolder.SortFavoriteListener;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.InsetTarget;
import net.osmand.plus.utils.InsetTarget.Type;
import net.osmand.plus.utils.InsetTargetsCollection;

import java.io.File;

public abstract class BaseFavoriteListFragment extends BaseFullScreenFragment
		implements SortFavoriteListener, FragmentStateHolder, IFavoriteListListener, ShareFavoritesListener {

	protected static final String SELECTED_GROUP_KEY = "selected_group_key";

	protected FavouritesHelper helper;
	protected ImportHelper importHelper;
	protected boolean selectionMode = false;

	protected FavoriteFoldersAdapter adapter;
	protected FavoriteGroup selectedGroup;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		importHelper = app.getImportHelper();
		setHasOptionsMenu(true);
		helper = app.getFavoritesHelper();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(getLayoutId(), container, false);
		adapter = new FavoriteFoldersAdapter(requireMyActivity(), nightMode, getFavoriteFolderListener());
		adapter.setSortFavoriteListener(this);

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(app));
		recyclerView.setAdapter(adapter);

		updateContent();

		return view;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		OnBackPressedCallback backCallback = new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				if (handleBackInsideFragment()) {
					return;
				}

				setEnabled(false);
				requireActivity().onBackPressed();
				setEnabled(true);
			}
		};

		requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backCallback);
	}

	private boolean handleBackInsideFragment() {
		if (selectionMode) {
			exitSelectionMode();
			return true;
		}
		return false;
	}

	@Override
	public InsetTargetsCollection getInsetTargets() {
		InsetTargetsCollection collection = super.getInsetTargets();
		collection.replace(InsetTarget.createScrollable(R.id.recycler_view));
		collection.removeType(Type.ROOT_INSET);
		return collection;
	}

	protected abstract FavoriteAdapterListener getFavoriteFolderListener();

	protected abstract int getLayoutId();

	protected abstract void updateContent();

	protected abstract ItemsSelectionHelper<?> getSelectionHelper();

	protected abstract void updateSelectionToolbar();

	@Override
	public Bundle storeState() {
		Bundle bundle = new Bundle();
		bundle.putInt(TAB_ID, FAV_TAB);
		if (selectedGroup != null) {
			bundle.putString(SELECTED_GROUP_KEY, selectedGroup.getName());
		}
		return bundle;
	}

	@Override
	public void restoreState(Bundle bundle) {
		if (bundle != null && bundle.getInt(TAB_ID) == FAV_TAB) {
			String selectedGroupName = bundle.getString(SELECTED_GROUP_KEY);
			if (selectedGroupName != null) {
				selectedGroup = helper.getGroup(selectedGroupName);
			}
			bundle.remove(SELECTED_GROUP_KEY);
		}
	}

	@Override
	public void reloadData() {
		updateContent();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, @NonNull MenuInflater inflater) {
		menu.clear();

		if (isInSelectionMode()) {
			inflater.inflate(R.menu.menu_selection_mode, menu);
		} else {
			inflater.inflate(R.menu.myplaces_tracks_menu, menu);
		}

		requireMyPlacesActivity().setToolbarVisibility(false);
	}

	@NonNull
	private MyPlacesActivity requireMyPlacesActivity() {
		return (MyPlacesActivity) requireMyActivity();
	}

	protected void setupHomeButton() {
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			int iconId = AndroidUtils.getNavigationIconResId(app);
			int colorId = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);

			actionBar.setHomeButtonEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setHomeAsUpIndicator(app.getUIUtilities().getIcon(iconId, colorId));
		}
	}

	protected void changeTitle(@NonNull String title) {
		ActionBar actionBar = getSupportActionBar();
		if (actionBar == null) return;
		actionBar.setTitle(title);
	}

	protected void exitSelectionMode() {
		if (!selectionMode) return;
		setSelectionMode(false);
		updateSelectionToolbar();
	}

	protected void setSelectionMode(boolean mode) {
		selectionMode = mode;
		if (!selectionMode) {
			getSelectionHelper().clearSelectedItems();
		}
		adapter.setSelectionMode(selectionMode);
		updateSelectionToolbar();
	}

	protected boolean isInSelectionMode() {
		return selectionMode;
	}

	@Override
	public void onPause() {
		super.onPause();
		exitSelectionMode();
	}

	@Nullable
	private ActionBar getSupportActionBar() {
		MyPlacesActivity activity = (MyPlacesActivity) requireMyActivity();
		return activity.getSupportActionBar();
	}

	@Override
	public void shareFavorites(@Nullable FavoriteGroup group) {
		ShareFavoritesAsyncTask shareFavoritesTask = new ShareFavoritesAsyncTask(app, group, this);
		OsmAndTaskManager.executeTask(shareFavoritesTask);
	}

	@Override
	public void shareFavoritesStarted() {
		updateProgressVisibility(true);
	}

	@Override
	public void shareFavoritesFinished(@NonNull File destFile, @NonNull Spanned pointsDescription) {
		updateProgressVisibility(false);
		if (destFile.exists()) {
			OsmandActionBarActivity activity = requireMyActivity();
			String type = "text/plain";
			String extraText = String.valueOf(pointsDescription);
			String extraSubject = app.getString(R.string.share_fav_subject);

			OsmandApplication app = (OsmandApplication) activity.getApplication();
			new NativeShareDialogBuilder()
					.addFileWithSaveAction(destFile, app, activity, true)
					.setChooserTitle(extraSubject)
					.setExtraSubject(extraSubject)
					.setExtraText(extraText)
					.setExtraStream(AndroidUtils.getUriForFile(app, destFile))
					.setType(type)
					.build(app);
		}
	}

	private void updateProgressVisibility(boolean visible) {
		OsmandActionBarActivity activity = getActionBarActivity();
		if (activity != null) {
			activity.setSupportProgressBarIndeterminateVisibility(visible);
		}
	}

	protected void importFavourites() {
		Intent intent = ImportHelper.getImportFileIntent();
		AndroidUtils.startActivityForResultIfSafe(this, intent, IMPORT_FAVOURITES_REQUEST);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if (requestCode == IMPORT_FAVOURITES_REQUEST && resultCode == Activity.RESULT_OK) {
			if (data != null && data.getData() != null) {
				importHelper.handleFavouritesImport(data.getData());
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}
}
