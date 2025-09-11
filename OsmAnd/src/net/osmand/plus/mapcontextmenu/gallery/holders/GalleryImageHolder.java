package net.osmand.plus.mapcontextmenu.gallery.holders;

import static net.osmand.plus.mapcontextmenu.gallery.GalleryGridItemDecorator.GRID_SCREEN_ITEM_SPACE_DP;
import static net.osmand.plus.mapcontextmenu.gallery.holders.GalleryImageHolder.ImageHolderType.*;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.mapcontextmenu.builders.cards.UrlImageCard;
import net.osmand.plus.mapcontextmenu.gallery.GalleryController;
import net.osmand.plus.mapcontextmenu.gallery.GalleryGridAdapter.ImageCardListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.shared.util.ImageLoadSource;
import net.osmand.shared.util.ImageLoaderCallback;
import net.osmand.shared.util.LoadingImage;
import net.osmand.shared.util.NetworkImageLoader;

public class GalleryImageHolder extends RecyclerView.ViewHolder {
	private final int MAIN_PHOTO_SIZE_DP;

	private final int STANDARD_PHOTO_SIZE_DP;

	private final OsmandApplication app;
	private final ImageView ivImage;
	private final ImageView ivSourceType;
	private final ImageView loadSourceType;
	private final TextView tvUrl;
	private final ProgressBar progressBar;
	private final View border;
	private final View itemView;
	private ImageHolderType type;
	private LoadingImage loadingImage;

	public GalleryImageHolder(@NonNull OsmandApplication app, @NonNull View itemView) {
		super(itemView);
		this.itemView = itemView;
		this.app = app;
		ivImage = itemView.findViewById(R.id.image);
		ivSourceType = itemView.findViewById(R.id.source_type);
		loadSourceType = itemView.findViewById(R.id.load_source_type);
		tvUrl = itemView.findViewById(R.id.url);
		border = itemView.findViewById(R.id.card_outline);
		progressBar = itemView.findViewById(R.id.progress);
		MAIN_PHOTO_SIZE_DP = app.getResources().getDimensionPixelSize(R.dimen.gallery_big_icon_size);
		STANDARD_PHOTO_SIZE_DP = app.getResources().getDimensionPixelSize(R.dimen.gallery_standard_icon_size);
	}

	public void bindView(@NonNull MapActivity mapActivity, @NonNull ImageCardListener listener, @NonNull ImageCard imageCard,
	                     @NonNull ImageHolderType type, Integer viewWidth, @NonNull NetworkImageLoader imageLoader, boolean nightMode) {
		this.type = type;
		OsmandApplication app = mapActivity.getApp();
		UiUtilities uiUtilities = app.getUIUtilities();
		setupView(mapActivity, viewWidth, nightMode);

		int topIconId = imageCard.getTopIconId();
		if (type == MAIN && topIconId != 0) {
			setSourceTypeIcon(uiUtilities.getIcon(topIconId));
		} else {
			setSourceTypeIcon(null);
		}

		int bg = nightMode ? R.drawable.context_menu_card_dark : R.drawable.context_menu_card_light;
		AndroidUtils.setBackground(mapActivity, border, bg);

		progressBar.setVisibility(imageCard instanceof UrlImageCard ? View.VISIBLE : View.GONE);

		ivImage.setImageDrawable(null);

		if (imageCard.isImageDownloadFailed()) {
			bindUrl(mapActivity, imageCard, nightMode);
		} else {
			tryLoadImage(mapActivity, listener, imageCard, imageLoader, nightMode);
		}
	}

	private void tryLoadImage(@NonNull MapActivity mapActivity, @NonNull ImageCardListener listener,
	                          @NonNull ImageCard imageCard, @NonNull NetworkImageLoader imageLoader, boolean nightMode) {
		if (loadingImage != null) {
			loadingImage.cancel();
		}

		String imageUrl = imageCard.getImageUrl();

		if (imageUrl != null) {
			loadingImage = imageLoader.loadImage(imageUrl, new ImageLoaderCallback() {
				@Override
				public void onStart(@Nullable Bitmap bitmap) {

				}

				@Override
				public void onSuccess(@NonNull Bitmap bitmap) {
					bindImage(listener, imageCard);
					Drawable next = new BitmapDrawable(ivImage.getResources(), bitmap);

					ivImage.setImageDrawable(next);
				}

				@Override
				public void onError() {
					if (!app.getSettings().isInternetConnectionAvailable()) {
						tryLoadCacheHiResImage(mapActivity, listener, imageCard, imageLoader, nightMode);
					} else {
						imageCard.markImageDownloadFailed(true);
						bindUrl(mapActivity, imageCard, nightMode);
					}
				}
			}, this::updateLoadSource, false);
		}
	}

	private void tryLoadCacheHiResImage(@NonNull MapActivity mapActivity, @NonNull ImageCardListener listener,
	                                    @NonNull ImageCard imageCard, @NonNull NetworkImageLoader imageLoader, boolean nightMode) {
		String hiResUrl = imageCard.getGalleryFullSizeUrl();
		if (hiResUrl != null) {
			loadingImage = imageLoader.loadImage(hiResUrl, new ImageLoaderCallback() {
				@Override
				public void onStart(@Nullable Bitmap bitmap) {

				}

				@Override
				public void onSuccess(@NonNull Bitmap bitmap) {
					bindImage(listener, imageCard);
					Drawable next = new BitmapDrawable(ivImage.getResources(), bitmap);

					ivImage.setImageDrawable(next);
				}

				@Override
				public void onError() {
					imageCard.markImageDownloadFailed(true);
					bindUrl(mapActivity, imageCard, nightMode);
				}
			}, this::updateLoadSource, false);
		}
	}

	private void updateLoadSource(@Nullable ImageLoadSource source) {
		if (!app.getSettings().isInternetConnectionAvailable() && ImageLoadSource.NETWORK != source) {
			loadSourceType.setVisibility(View.VISIBLE);
		} else {
			loadSourceType.setVisibility(View.GONE);
		}
	}

	private void bindImage(@NonNull ImageCardListener listener, @NonNull ImageCard imageCard) {
		LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layoutParams.gravity = Gravity.CENTER;
		ivImage.setVisibility(View.VISIBLE);
		ivImage.setLayoutParams(layoutParams);
		ivImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
		ivImage.setOnClickListener(v -> listener.onImageClicked(imageCard));
		tvUrl.setVisibility(View.GONE);
		border.setVisibility(View.GONE);
		progressBar.setVisibility(View.GONE);
	}

	private void bindUrl(@NonNull MapActivity mapActivity, @NonNull ImageCard imageCard, boolean nightMode) {
		ivImage.setVisibility(View.GONE);
		tvUrl.setVisibility(View.VISIBLE);
		tvUrl.setText(imageCard.getUrl());
		tvUrl.setOnClickListener(v -> AndroidUtils.openUrl(mapActivity, imageCard.getUrl(), nightMode));
		border.setVisibility(View.VISIBLE);
		progressBar.setVisibility(View.GONE);
		updateLoadSource(null);
		setSourceTypeIcon(null);
	}

	private void setSourceTypeIcon(@Nullable Drawable icon) {
		AndroidUiHelper.updateVisibility(ivSourceType, icon != null);
		ivSourceType.setImageDrawable(icon);
	}

	private void setupView(@NonNull MapActivity mapActivity, Integer viewWidth, boolean nightMode) {
		OsmandApplication app = mapActivity.getApp();
		int sizeInPx;
		if (type == SPAN_RESIZABLE) {
			int spanCount = GalleryController.getSettingsSpanCount(mapActivity);
			int recyclerViewPadding = AndroidUtils.dpToPx(app, 13);
			int itemSpace = AndroidUtils.dpToPx(app, GRID_SCREEN_ITEM_SPACE_DP * 2);
			int screenWidth;
			if (viewWidth != null) {
				screenWidth = viewWidth;
			} else {
				screenWidth = AndroidUiHelper.isOrientationPortrait(mapActivity)
						? AndroidUtils.getScreenWidth(mapActivity)
						: AndroidUtils.getScreenHeight(mapActivity);
			}
			sizeInPx = calculateItemSize(spanCount, recyclerViewPadding, itemSpace, screenWidth);
		} else {
			sizeInPx = type == MAIN ? MAIN_PHOTO_SIZE_DP : STANDARD_PHOTO_SIZE_DP;
		}
		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(sizeInPx, sizeInPx);
		itemView.setLayoutParams(layoutParams);
		itemView.setBackgroundColor(ColorUtilities.getActivityBgColor(app, nightMode));
	}

	private int calculateItemSize(int spanCount, int recyclerViewPadding, int itemSpace, int screenWidth) {
		int spaceForItems = screenWidth - ((recyclerViewPadding * 2) + (spanCount * itemSpace));
		return spaceForItems / spanCount;
	}

	public ImageHolderType getHolderType() {
		return type;
	}

	public enum ImageHolderType {MAIN, STANDARD, SPAN_RESIZABLE}
}

