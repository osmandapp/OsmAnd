package net.osmand.plus.mapcontextmenu.builders.cards;

import static net.osmand.plus.mapcontextmenu.gallery.GalleryGridAdapter.IMAGE_TYPE;

import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.gallery.GalleryItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.gallery.GalleryController;
import net.osmand.plus.mapcontextmenu.gallery.GalleryGridAdapter;
import net.osmand.plus.mapcontextmenu.gallery.GalleryListener;
import net.osmand.plus.mapcontextmenu.gallery.GalleryGridFragment;
import net.osmand.plus.mapcontextmenu.gallery.GalleryGridItemDecorator;
import net.osmand.plus.mapcontextmenu.gallery.GalleryPhotoPagerFragment;
import net.osmand.plus.plugins.mapillary.MapillaryImageDialog;
import net.osmand.plus.plugins.mapillary.MapillaryPlugin;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.shared.media.domain.MediaItem;
import net.osmand.shared.media.domain.MediaOrigin;
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
	}

	private void updateShowAll() {
		View viewAllButton = galleryView.findViewById(R.id.view_all);
		AndroidUiHelper.updateVisibility(viewAllButton, shouldShowViewAll());
	}

	public void onLoadingImage(boolean loading) {
		galleryGridAdapter.onLoadingImages(loading);
	}

	public void build(@NonNull GalleryController controller, boolean onlinePhotos, boolean nightMode) {
		galleryView = UiUtilities.inflate(mapActivity, nightMode, R.layout.gallery_card);
		RecyclerView recyclerView = galleryView.findViewById(R.id.recycler_view);

		List<GalleryItem> items = new ArrayList<>();
		GalleryListener listener = getGalleryListener(controller, onlinePhotos);
		galleryGridAdapter = new GalleryGridAdapter(mapActivity, listener, null, onlinePhotos, nightMode);

		if (!app.getSettings().isInternetConnectionAvailable()) {
			items.add(new GalleryItem.NoInternet());
		} else {
			items.addAll(galleryItems);
		}
		galleryGridAdapter.setItems(items);

		recyclerView.setLayoutManager(getGridLayoutManager());
		GalleryGridItemDecorator galleryGridItemDecorator = new GalleryGridItemDecorator(app);
		recyclerView.addItemDecoration(galleryGridItemDecorator);
		recyclerView.setAdapter(galleryGridAdapter);

		setupViewALlButton(onlinePhotos);
	}

	private GridLayoutManager getGridLayoutManager() {
		GridLayoutManager gridLayoutManager = new GridLayoutManager(app, 2, GridLayoutManager.HORIZONTAL, false);
		gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
			@Override
			public int getSpanSize(int position) {
				return galleryGridAdapter.getItemViewType(position) == IMAGE_TYPE ? 1 : 2;
			}
		});
		return gridLayoutManager;
	}

	private void setupViewALlButton(boolean onlinePhotos) {
		DialogButton viewAllButton = galleryView.findViewById(R.id.view_all);
		viewAllButton.setTitleId(onlinePhotos ? R.string.shared_string_show_all : R.string.shared_string_explore);
		viewAllButton.setOnClickListener(v -> showAll(onlinePhotos));
		updateShowAll();
	}

	@NonNull
	private GalleryListener getGalleryListener(@NonNull GalleryController controller, boolean onlinePhotos) {
		return new GalleryListener() {
			@Override
			public void onMediaItemClicked(@NonNull MediaItem mediaItem) {
				if (onlinePhotos) {
					GalleryPhotoPagerFragment.showInstance(mapActivity, controller.getItemIndexBySourceUri(mediaItem.getSourceUri()));
				} else if (mediaItem.getOrigin() == MediaOrigin.MAPILLARY && mediaItem instanceof MediaItem.Remote remote) {
					mapActivity.getContextMenu().close();
					var metadata = remote.getMetadata();
					var resource = mediaItem.getResource();
					var details = mediaItem.getDetails();

					if (Algorithms.isEmpty(metadata.getKey())) {
						return;
					}

					LatLon location = null;
					if (metadata.getLatitude() != null && metadata.getLongitude() != null) {
						location = new LatLon(metadata.getLatitude(), metadata.getLongitude());
					}

					MapillaryImageDialog.show(
							mapActivity, metadata.getKey(),
							resource.getFullUri(), details.getViewUrl(),
							location, metadata.getCameraAngle(),
							app.getString(R.string.mapillary), null, true
					);
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

	private void showAll(boolean onlinePhotos) {
		if (onlinePhotos) {
			GalleryGridFragment.showInstance(mapActivity);
		} else {
			MapillaryPlugin.openMapillary(mapActivity, null);
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
}
