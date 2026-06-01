package net.osmand.plus.mapcontextmenu.gallery;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.gallery.model.GalleryAction;
import net.osmand.plus.gallery.model.GalleryItem;
import net.osmand.plus.gallery.model.GalleryItem.NoInternet;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.gallery.controller.GalleryController;
import net.osmand.plus.gallery.ui.GalleryGridAdapter;
import net.osmand.plus.gallery.ui.GalleryGridConfig;
import net.osmand.plus.gallery.ui.GalleryListener;
import net.osmand.plus.gallery.ui.GalleryGridFragment;
import net.osmand.plus.gallery.ui.GalleryGridItemDecorator;
import net.osmand.plus.gallery.ui.GalleryPhotoPagerFragment;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.shared.media.domain.MediaItem;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class GalleryRowBuilder {
	private final OsmandApplication app;
	private final MapActivity mapActivity;

	private final MenuBuilder menuBuilder;
	private final List<GalleryItem> galleryItems = new ArrayList<>();
	private View galleryView;
	private GalleryGridAdapter galleryGridAdapter;
	@Nullable
	private View.OnClickListener addButtonClickListener;
	@Nullable
	private View.OnClickListener showAllClickListener;
	@Nullable
	private MediaItemClickListener mediaItemClickListener;
	private boolean requireInternet = true;

	public GalleryRowBuilder(MenuBuilder menuBuilder) {
		this.menuBuilder = menuBuilder;
		this.mapActivity = menuBuilder.getMapActivity();
		this.app = menuBuilder.getApplication();
	}

	public MenuBuilder getMenuBuilder() {
		return menuBuilder;
	}

	public View getGalleryView() {
		return galleryView;
	}

	public void setAddButtonClickListener(@Nullable View.OnClickListener addButtonClickListener) {
		this.addButtonClickListener = addButtonClickListener;
	}

	public void setShowAllClickListener(@Nullable View.OnClickListener showAllClickListener) {
		this.showAllClickListener = showAllClickListener;
	}

	public void setMediaItemClickListener(@Nullable MediaItemClickListener mediaItemClickListener) {
		this.mediaItemClickListener = mediaItemClickListener;
	}

	public void setRequireInternet(boolean requireInternet) {
		this.requireInternet = requireInternet;
	}

	public void setItems(GalleryItem... items) {
		setItems(Arrays.asList(items));
	}

	public void setItems(@NonNull Collection<? extends GalleryItem> items) {
		this.galleryItems.clear();
		this.galleryItems.addAll(items);

		if (!menuBuilder.isHidden()) {
			List<GalleryItem> list = new ArrayList<>(items);
			galleryGridAdapter.setItems(list);

			MapContextMenu mapContextMenu = menuBuilder.getMapContextMenu();
			if (itemsCount() > 0 && mapContextMenu != null) {
				mapContextMenu.updateLayout();
			}
		}
		updateShowAll();
		updateAddButton();
	}

	private void updateShowAll() {
		if (galleryView != null) {
			View viewAllButton = galleryView.findViewById(R.id.view_all);
			AndroidUiHelper.updateVisibility(viewAllButton, shouldShowViewAll());
			updateButtonsContainer();
		}
	}

	public void onLoadingImage(boolean loading) {
		galleryGridAdapter.onLoadingImages(loading);
	}

	public void build(@NonNull GalleryController controller, @NonNull GalleryGridConfig config, boolean nightMode) {
		galleryView = UiUtilities.inflate(mapActivity, nightMode, R.layout.gallery_card);
		RecyclerView recyclerView = galleryView.findViewById(R.id.recycler_view);

		List<GalleryItem> items = new ArrayList<>();
		GalleryListener listener = getGalleryListener(controller);
		galleryGridAdapter = new GalleryGridAdapter(mapActivity, listener, controller, null, config, nightMode);

		if (requireInternet && !app.getSettings().isInternetConnectionAvailable()) {
			items.add(NoInternet.INSTANCE);
		} else {
			items.addAll(galleryItems);
		}
		galleryGridAdapter.setItems(items);

		recyclerView.setLayoutManager(getGridLayoutManager());
		GalleryGridItemDecorator galleryGridItemDecorator = new GalleryGridItemDecorator(app);
		recyclerView.addItemDecoration(galleryGridItemDecorator);
		recyclerView.setAdapter(galleryGridAdapter);

		setupViewALlButton(config);
		setupAddButton();
	}

	private GridLayoutManager getGridLayoutManager() {
		GridLayoutManager gridLayoutManager = new GridLayoutManager(app, 2, GridLayoutManager.HORIZONTAL, false);
		gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
			@Override
			public int getSpanSize(int position) {
				return galleryGridAdapter.isRegularMediaItemOnPosition(position) ? 1 : 2;
			}
		});
		return gridLayoutManager;
	}

	private void setupViewALlButton(@NonNull GalleryGridConfig config) {
		DialogButton viewAllButton = galleryView.findViewById(R.id.view_all);
		viewAllButton.setTitleId(config.getShowAllButtonTitleResId());
		viewAllButton.setOnClickListener(v -> onShowAllButtonClicked(config));
		updateShowAll();
	}

	private void setupAddButton() {
		DialogButton addButton = galleryView.findViewById(R.id.add_media);
		addButton.setOnClickListener(addButtonClickListener);
		updateAddButton();
	}

	private void updateAddButton() {
		if (galleryView != null) {
			View addButton = galleryView.findViewById(R.id.add_media);
			AndroidUiHelper.updateVisibility(addButton, addButtonClickListener != null);
			updateButtonsContainer();
		}
	}

	private void updateButtonsContainer() {
		View buttonsContainer = galleryView.findViewById(R.id.gallery_buttons_container);
		View addButton = galleryView.findViewById(R.id.add_media);
		View viewAllButton = galleryView.findViewById(R.id.view_all);
		boolean addVisible = addButton.getVisibility() == View.VISIBLE;
		boolean viewAllVisible = viewAllButton.getVisibility() == View.VISIBLE;
		updateViewAllButtonMargin(viewAllButton, addVisible && viewAllVisible);
		boolean visible = addVisible || viewAllVisible;
		AndroidUiHelper.updateVisibility(buttonsContainer, visible);
	}

	private void updateViewAllButtonMargin(@NonNull View viewAllButton, boolean addStartMargin) {
		ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) viewAllButton.getLayoutParams();
		int marginStart = addStartMargin ? app.getResources().getDimensionPixelSize(R.dimen.content_padding) : 0;
		if (params.getMarginStart() != marginStart) {
			params.setMarginStart(marginStart);
			viewAllButton.setLayoutParams(params);
		}
	}

	@NonNull
	private GalleryListener getGalleryListener(@NonNull GalleryController controller) {
		return new GalleryListener() {
			@Override
			public void onMediaItemClicked(@NonNull MediaItem mediaItem) {
				if (mediaItemClickListener != null) {
					mediaItemClickListener.onMediaItemClicked(mediaItem);
					return;
				}
				if (!PluginsHelper.handleGalleryMediaItemClick(mapActivity, mediaItem)) {
					int position = controller.getPhotoItemIndexById(mediaItem.getId());
					GalleryPhotoPagerFragment.showInstance(mapActivity, position);
				}
			}

			@Override
			public void onReloadMediaItems() {
				if (!app.getSettings().isInternetConnectionAvailable()) {
					app.showShortToastMessage(R.string.shared_string_no_internet_connection);
				} else {
					menuBuilder.startLoadingImages();
				}
			}
		};
	}

	private void onShowAllButtonClicked(@NonNull GalleryGridConfig config) {
		if (showAllClickListener != null) {
			showAllClickListener.onClick(galleryView.findViewById(R.id.view_all));
			return;
		}
		GalleryAction action = config.getShowAllButtonAction();
		if (action != null) {
			PluginsHelper.handleGalleryAction(action);
		} else {
			GalleryGridFragment.showInstance(mapActivity);
		}
	}

	private boolean shouldShowViewAll() {
		if (Algorithms.isEmpty(galleryItems)) {
			return false;
		}
		for (GalleryItem item : galleryItems) {
			if (item instanceof GalleryItem.Media) {
				return true;
			}
		}
		return false;
	}

	private int itemsCount() {
		return galleryItems.size();
	}

	public interface MediaItemClickListener {
		void onMediaItemClicked(@NonNull MediaItem mediaItem);
	}
}
