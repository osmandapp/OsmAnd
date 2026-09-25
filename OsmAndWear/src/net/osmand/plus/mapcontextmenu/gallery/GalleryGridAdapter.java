package net.osmand.plus.mapcontextmenu.gallery;

import static net.osmand.plus.mapcontextmenu.gallery.holders.GalleryImageHolder.ImageHolderType.MAIN;
import static net.osmand.plus.mapcontextmenu.gallery.holders.GalleryImageHolder.ImageHolderType.SPAN_RESIZABLE;
import static net.osmand.plus.mapcontextmenu.gallery.holders.GalleryImageHolder.ImageHolderType.STANDARD;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.mapcontextmenu.builders.cards.NoImagesCard;
import net.osmand.plus.mapcontextmenu.builders.cards.ProgressCard;
import net.osmand.plus.mapcontextmenu.gallery.holders.GalleryImageHolder;
import net.osmand.plus.mapcontextmenu.gallery.holders.GalleryImageHolder.ImageHolderType;
import net.osmand.plus.mapcontextmenu.gallery.holders.ImagesCountHolder;
import net.osmand.plus.mapcontextmenu.gallery.holders.MapillaryContributeHolder;
import net.osmand.plus.mapcontextmenu.gallery.holders.NoImagesHolder;
import net.osmand.plus.mapcontextmenu.gallery.holders.NoInternetHolder;
import net.osmand.plus.plugins.mapillary.MapillaryContributeCard;
import net.osmand.plus.plugins.mapillary.MapillaryImageCard;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class GalleryGridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	protected static final int MAIN_IMAGE_TYPE = 0;
	public static final int IMAGE_TYPE = 1;
	private static final int PROGRESS_TYPE = 2;
	private static final int MAPILLARY_CONTRIBUTE_TYPE = 3;
	private static final int NO_IMAGES_TYPE = 4;
	public static final int NO_INTERNET_TYPE = 5;
	protected static final int IMAGES_COUNT_TYPE = 6;

	protected static final int UPDATE_PROGRESS_BAR_PAYLOAD_TYPE = 1;
	public static final int UPDATE_IMAGE_VIEW_TYPE = 2;

	private final List<Object> items = new ArrayList<>();

	private final ImageCardListener listener;
	private final LayoutInflater themedInflater;
	private final boolean nightMode;
	private final MapActivity mapActivity;
	private final OsmandApplication app;

	private final boolean isOnlinePhotos;
	private boolean resizeBySpanCount = false;
	private boolean loadingImages = false;
	private final Integer viewWidth;

	public GalleryGridAdapter(@NonNull MapActivity mapActivity, @NonNull ImageCardListener listener,
	                          @Nullable Integer viewWidth, boolean isOnlinePhotos, boolean nightMode) {
		this.listener = listener;
		this.nightMode = nightMode;
		this.isOnlinePhotos = isOnlinePhotos;
		this.mapActivity = mapActivity;
		this.app = mapActivity.getMyApplication();
		this.viewWidth = viewWidth;
		themedInflater = UiUtilities.getInflater(mapActivity, nightMode);
	}

	public void setItems(@NonNull List<Object> items) {
		this.items.clear();
		if (isOnlinePhotos) {
			this.items.addAll(items);
		} else {
			List<Object> limitedItems = new ArrayList<>();
			int addedMapillaryCards = 0;
			for (Object object : items) {
				if (object instanceof MapillaryImageCard mapillaryImageCard) {
					if (addedMapillaryCards < 5) {
						limitedItems.add(mapillaryImageCard);
						addedMapillaryCards++;
					}
				} else {
					limitedItems.add(object);
				}
			}
			this.items.addAll(limitedItems);
		}

		notifyDataSetChanged();
	}

	public List<Object> getItems() {
		return items;
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View itemView;
		return switch (viewType) {
			case MAIN_IMAGE_TYPE, IMAGE_TYPE -> {
				itemView = themedInflater.inflate(R.layout.gallery_card_item, parent, false);
				yield new GalleryImageHolder(app, itemView);
			}
			case MAPILLARY_CONTRIBUTE_TYPE -> {
				itemView = themedInflater.inflate(R.layout.context_menu_card_add_mapillary_images, parent, false);
				yield new MapillaryContributeHolder(itemView);
			}
			case NO_IMAGES_TYPE -> {
				itemView = themedInflater.inflate(R.layout.no_image_card, parent, false);
				yield new NoImagesHolder(itemView, app);
			}
			case NO_INTERNET_TYPE -> {
				itemView = themedInflater.inflate(R.layout.no_internet_card, parent, false);
				yield new NoInternetHolder(itemView, app);
			}
			case IMAGES_COUNT_TYPE -> {
				itemView = themedInflater.inflate(R.layout.images_count_item, parent, false);
				yield new ImagesCountHolder(itemView, app);
			}
			default -> throw new IllegalArgumentException("Unsupported view type");
		};
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		if (holder instanceof GalleryImageHolder viewHolder) {
			Object item = items.get(position);
			if (item instanceof ImageCard imageCard) {
				ImageHolderType type = resizeBySpanCount ? SPAN_RESIZABLE : position == 0 ? MAIN : STANDARD;
				viewHolder.bindView(mapActivity, listener, imageCard, type, viewWidth, nightMode);
			}
		} else if (holder instanceof MapillaryContributeHolder viewHolder) {
			viewHolder.bindView(nightMode, mapActivity);
		} else if (holder instanceof NoImagesHolder noImagesHolder) {
			noImagesHolder.bindView(nightMode, mapActivity, isOnlinePhotos);
		} else if (holder instanceof NoInternetHolder noInternetHolder) {
			noInternetHolder.bindView(nightMode, listener, loadingImages);
		} else if (holder instanceof ImagesCountHolder imagesCountHolder) {
			imagesCountHolder.bindView(items.size() - 1, nightMode);
		}
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
		if (!Algorithms.isEmpty(payloads) && payloads.get(0) instanceof Integer payLoadInteger) {
			if (holder instanceof NoInternetHolder noInternetHolder && payLoadInteger == UPDATE_PROGRESS_BAR_PAYLOAD_TYPE) {
				noInternetHolder.updateProgressBar(loadingImages);
			}
		} else {
			super.onBindViewHolder(holder, position, payloads);
		}
	}

	public void onLoadingImages(boolean loadingImages) {
		this.loadingImages = loadingImages;
		for (int i = 0; i < items.size(); i++) {
			Object object = items.get(i);
			if (object instanceof Integer integer && integer == NO_INTERNET_TYPE) {
				notifyItemChanged(i, UPDATE_PROGRESS_BAR_PAYLOAD_TYPE);
			}
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	@Override
	public int getItemViewType(int position) {
		Object object = items.get(position);
		if (object instanceof MapillaryContributeCard) {
			return MAPILLARY_CONTRIBUTE_TYPE;
		} else if (object instanceof ImageCard && position == 0) {
			return MAIN_IMAGE_TYPE;
		} else if (object instanceof ImageCard) {
			return IMAGE_TYPE;
		} else if (object instanceof ProgressCard) {
			return PROGRESS_TYPE;
		} else if (object instanceof NoImagesCard) {
			return NO_IMAGES_TYPE;
		} else if (object instanceof Integer integer) {
			if (integer == NO_INTERNET_TYPE) {
				return NO_INTERNET_TYPE;
			} else if (integer == IMAGES_COUNT_TYPE) {
				return IMAGES_COUNT_TYPE;
			}
		}
		throw new IllegalArgumentException("Unsupported view type");
	}

	public RecyclerView.ItemAnimator getAnimator() {
		return new DefaultItemAnimator() {
			@Override
			public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
				return true;
			}
		};
	}

	public void setResizeBySpanCount(boolean resizeBySpanCount) {
		this.resizeBySpanCount = resizeBySpanCount;
	}

	public interface ImageCardListener {

		void onImageClicked(@NonNull ImageCard imageCard);

		default void onReloadImages() {
		}
	}
}
