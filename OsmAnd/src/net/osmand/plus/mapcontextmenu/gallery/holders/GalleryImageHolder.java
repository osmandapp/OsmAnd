package net.osmand.plus.mapcontextmenu.gallery.holders;

import static net.osmand.plus.mapcontextmenu.gallery.GalleryGridItemDecorator.GRID_SCREEN_ITEM_SPACE_DP;
import static net.osmand.plus.mapcontextmenu.gallery.holders.GalleryImageHolder.ImageHolderType.*;

import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.mapcontextmenu.gallery.GalleryController;
import net.osmand.plus.mapcontextmenu.gallery.GalleryGridAdapter.ImageCardListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

public class GalleryImageHolder extends RecyclerView.ViewHolder {
	private final int MAIN_PHOTO_SIZE_DP;

	private final int STANDARD_PHOTO_SIZE_DP;

	private final ImageView imageView;
	private final ImageView sourceTypeView;
	private final View itemView;
	private ImageHolderType type;

	public GalleryImageHolder(OsmandApplication app, @NonNull View itemView) {
		super(itemView);
		this.itemView = itemView;
		imageView = itemView.findViewById(R.id.image);
		sourceTypeView = itemView.findViewById(R.id.source_type);
		MAIN_PHOTO_SIZE_DP = app.getResources().getDimensionPixelSize(R.dimen.gallery_big_icon_size);
		STANDARD_PHOTO_SIZE_DP = app.getResources().getDimensionPixelSize(R.dimen.gallery_standard_icon_size);
	}

	public void bindView(@NonNull MapActivity mapActivity, @NonNull ImageCardListener listener, @NonNull ImageCard imageCard,
	                     @NonNull ImageHolderType type, Integer viewWidth, boolean nightMode) {
		this.type = type;
		OsmandApplication app = mapActivity.getMyApplication();
		setupView(mapActivity, viewWidth, nightMode);

		Picasso.get().load(imageCard.getImageUrl()).into(imageView, new Callback() {
			@Override
			public void onSuccess() {
				FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
				layoutParams.gravity = Gravity.CENTER;
				imageView.setLayoutParams(layoutParams);
				imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
			}

			@Override
			public void onError(Exception e) {

			}
		});
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

	private void setSourceTypeIcon(@Nullable Drawable icon) {
		AndroidUiHelper.updateVisibility(sourceTypeView, icon != null);
		sourceTypeView.setImageDrawable(icon);
	}

	private void setupView(@NonNull MapActivity mapActivity, Integer viewWidth, boolean nightMode) {
		OsmandApplication app = mapActivity.getMyApplication();
		int sizeInPx;
		if (type == SPAN_RESIZABLE) {
			int spanCount = GalleryController.getSettingsSpanCount(mapActivity);
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

