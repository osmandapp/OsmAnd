package net.osmand.plus.mapcontextmenu.builders.cards;

import static net.osmand.plus.mapcontextmenu.gallery.GalleryGridAdapter.IMAGE_TYPE;
import static net.osmand.plus.mapcontextmenu.gallery.GalleryGridAdapter.NO_INTERNET_TYPE;

import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.gallery.GalleryController;
import net.osmand.plus.mapcontextmenu.gallery.GalleryGridAdapter;
import net.osmand.plus.mapcontextmenu.gallery.GalleryGridAdapter.ImageCardListener;
import net.osmand.plus.mapcontextmenu.gallery.GalleryGridFragment;
import net.osmand.plus.mapcontextmenu.gallery.GalleryGridItemDecorator;
import net.osmand.plus.mapcontextmenu.gallery.GalleryPhotoPagerFragment;
import net.osmand.plus.plugins.mapillary.MapillaryImageDialog;
import net.osmand.plus.plugins.mapillary.MapillaryPlugin;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class CardsRowBuilder {
	private final OsmandApplication app;
	private final MapActivity mapActivity;

	private final MenuBuilder menuBuilder;
	private final List<AbstractCard> cards = new ArrayList<>();
	private View galleryView;
	private GalleryGridAdapter galleryGridAdapter;

	public CardsRowBuilder(MenuBuilder menuBuilder) {
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

	public void setCards(AbstractCard... cards) {
		setCards(Arrays.asList(cards));
	}

	public void setCards(@NonNull Collection<? extends AbstractCard> cards) {
		this.cards.clear();
		this.cards.addAll(cards);

		if (!menuBuilder.isHidden()) {
			List<Object> list = new ArrayList<>(cards);
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
		LayoutInflater themedInflater = UiUtilities.getInflater(app, nightMode);
		galleryView = themedInflater.inflate(R.layout.gallery_card, null);
		RecyclerView recyclerView = galleryView.findViewById(R.id.recycler_view);

		List<Object> items = new ArrayList<>();
		ImageCardListener listener = getImageCardListener(controller, onlinePhotos);
		galleryGridAdapter = new GalleryGridAdapter(mapActivity, listener, null, onlinePhotos, nightMode);

		if (!app.getSettings().isInternetConnectionAvailable()) {
			items.add(NO_INTERNET_TYPE);
		} else {
			items.addAll(cards);
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
	private ImageCardListener getImageCardListener(@NonNull GalleryController controller, boolean onlinePhotos) {
		return new ImageCardListener() {
			@Override
			public void onImageClicked(@NonNull ImageCard imageCard) {
				if (onlinePhotos) {
					GalleryPhotoPagerFragment.showInstance(mapActivity, controller.getImageCardFromUrl(imageCard.imageUrl));
				} else {
					mapActivity.getContextMenu().close();
					MapillaryImageDialog.show(mapActivity, imageCard.getKey(), imageCard.getImageHiresUrl(), imageCard.getUrl(), imageCard.getLocation(),
							imageCard.getCa(), app.getString(R.string.mapillary), null, true);
				}
			}

			@Override
			public void onReloadImages() {
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
		if (Algorithms.isEmpty(cards)) {
			return false;
		}
		for (AbstractCard card : cards) {
			if (card instanceof ImageCard) {
				return true;
			}
		}
		return false;
	}

	private int itemsCount() {
		return cards.size();
	}
}
