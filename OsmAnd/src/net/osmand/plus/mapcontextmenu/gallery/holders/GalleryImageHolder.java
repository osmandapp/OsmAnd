package net.osmand.plus.mapcontextmenu.gallery.holders;

import static net.osmand.plus.mapcontextmenu.gallery.GalleryGridItemDecorator.GRID_SCREEN_ITEM_SPACE_DP;
import static net.osmand.plus.mapcontextmenu.gallery.holders.GalleryImageHolder.ImageHolderType.*;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.mapcontextmenu.gallery.GalleryContextHelper;
import net.osmand.plus.mapcontextmenu.gallery.GalleryGridAdapter.ImageCardListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

public class GalleryImageHolder extends RecyclerView.ViewHolder {
	private static final int MAIN_PHOTO_SIZE_DP = 156;

	private static final int STANDARD_PHOTO_SIZE_DP = 72;

	private final ImageView imageView;
	private final ImageView sourceTypeView;
	private final View itemView;
	private final GalleryContextHelper galleryContextHelper;
	private ImageHolderType type;

	public GalleryImageHolder(@NonNull View itemView, @NonNull GalleryContextHelper galleryContextHelper) {
		super(itemView);
		this.itemView = itemView;
		this.galleryContextHelper = galleryContextHelper;
		imageView = itemView.findViewById(R.id.image);
		sourceTypeView = itemView.findViewById(R.id.source_type);
	}

	public void bindView(@NonNull MapActivity mapActivity, @NonNull ImageCardListener listener, @NonNull ImageCard imageCard,
	                     @NonNull ImageHolderType type, Integer viewWidth, boolean nightMode) {
		this.type = type;
		OsmandApplication app = mapActivity.getMyApplication();
		setupView(mapActivity, viewWidth, nightMode);

		Bitmap bitmap = galleryContextHelper.getBitmap(imageCard.getImageUrl(), listener);
		if (bitmap != null) {
			setImage(bitmap);
		} else {
			setEmptyImage(app, nightMode);
		}
		imageView.setOnClickListener(v -> listener.onImageClicked(imageCard));

		if (type == MAIN) {
			int topIconId = imageCard.getTopIconId();
			if (topIconId != 0) {
				Drawable icon = app.getUIUtilities().getIcon(imageCard.getTopIconId());
				setSourceTypeIcon(icon);
			} else {
				setSourceTypeIcon(null);
			}
		} else {
			setSourceTypeIcon(null);
		}
	}

	public void updateImage(@Nullable Bitmap bitmap) {
		if (bitmap != null) {
			setImage(bitmap);
		}
	}

	private void setSourceTypeIcon(@Nullable Drawable icon) {
		AndroidUiHelper.updateVisibility(sourceTypeView, icon != null);
		sourceTypeView.setImageDrawable(icon);
	}

	private void setupView(@NonNull MapActivity mapActivity, Integer viewWidth, boolean nightMode) {
		OsmandApplication app = mapActivity.getMyApplication();
		int sizeInPx;
		if (type == SPAN_RESIZABLE) {
			int spanCount = GalleryContextHelper.getSettingsSpanCount(mapActivity);
			int recyclerViewPadding = AndroidUtils.dpToPx(app, 13);
			int itemSpace = AndroidUtils.dpToPx(app, GRID_SCREEN_ITEM_SPACE_DP * 2);
			int screenWidth;
			if (viewWidth != null) {
				screenWidth = viewWidth;
			} else {
				screenWidth = AndroidUiHelper.isOrientationPortrait(mapActivity) ? AndroidUtils.getScreenWidth(mapActivity) : AndroidUtils.getScreenHeight(mapActivity);
			}
			sizeInPx = calculateItemSize(spanCount, recyclerViewPadding, itemSpace, screenWidth);
		} else {
			int imageSize = type == MAIN ? MAIN_PHOTO_SIZE_DP : STANDARD_PHOTO_SIZE_DP;
			sizeInPx = AndroidUtils.dpToPx(app, imageSize);
		}
		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(sizeInPx, sizeInPx);
		itemView.setLayoutParams(layoutParams);
		itemView.setBackgroundColor(ColorUtilities.getActivityBgColor(app, nightMode));
	}

	private int calculateItemSize(int spanCount, int recyclerViewPadding, int itemSpace, int screenWidth) {
		int spaceForItems = screenWidth - ((recyclerViewPadding * 2) + (spanCount * itemSpace));
		return spaceForItems / spanCount;
	}

	private void setImage(Bitmap bitmap) {
		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
		layoutParams.gravity = Gravity.CENTER;
		imageView.setLayoutParams(layoutParams);
		imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
		imageView.setImageBitmap(bitmap);
	}

	private void setEmptyImage(@NonNull OsmandApplication app, boolean nightMode) {
		int sizeInDp = AndroidUtils.dpToPx(app, 24);
		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(sizeInDp, sizeInDp);
		layoutParams.gravity = Gravity.CENTER;
		imageView.setLayoutParams(layoutParams);
		imageView.setImageBitmap(null);
		Drawable emptyIcon = app.getUIUtilities().getPaintedIcon(R.drawable.mm_tourism_museum, ColorUtilities.getDefaultIconColor(app, nightMode));
		imageView.setImageDrawable(emptyIcon);
		imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
	}

	public ImageHolderType getHolderType() {
		return type;
	}

	public enum ImageHolderType {MAIN, STANDARD, SPAN_RESIZABLE}
}

