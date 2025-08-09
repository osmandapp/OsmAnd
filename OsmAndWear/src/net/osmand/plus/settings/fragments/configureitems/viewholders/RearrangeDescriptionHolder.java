package net.osmand.plus.settings.fragments.configureitems.viewholders;

import android.content.res.Resources;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.fragments.configureitems.ScreenType;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.UnmovableItem;

public class RearrangeDescriptionHolder extends ViewHolder implements UnmovableItem {

	private final OsmandApplication app;

	private final ImageView image;
	private final ImageView deviceImage;
	private final TextView description;
	private final FrameLayout imageContainer;

	public RearrangeDescriptionHolder(@NonNull View itemView) {
		super(itemView);
		app = (OsmandApplication) itemView.getContext().getApplicationContext();

		image = itemView.findViewById(R.id.image);
		deviceImage = itemView.findViewById(R.id.device_image);
		description = itemView.findViewById(R.id.description);
		imageContainer = itemView.findViewById(R.id.image_container);
	}

	@Override
	public boolean isMovingDisabled() {
		return true;
	}

	public void bindView(@NonNull ScreenType screenType, boolean nightMode) {
		deviceImage.setImageResource(getImageId(screenType, nightMode));
		image.setImageResource(nightMode ? screenType.imageNightId : screenType.imageDayId);
		description.setText(app.getString(R.string.reorder_or_hide_from, app.getString(screenType.titleId)));

		setupImagePadding(screenType);
	}

	@DrawableRes
	private int getImageId(@NonNull ScreenType screenType, boolean nightMode) {
		if (screenType == ScreenType.CONTEXT_MENU_ACTIONS) {
			return nightMode ? R.drawable.img_settings_device_bottom_dark : R.drawable.img_settings_device_bottom_light;
		} else {
			return nightMode ? R.drawable.img_settings_device_top_dark : R.drawable.img_settings_device_top_light;
		}
	}

	private void setupImagePadding(@NonNull ScreenType screenType) {
		Resources resources = app.getResources();
		int paddingHorizontal = (int) resources.getDimension(R.dimen.dashboard_map_toolbar);
		int paddingVertical = (int) resources.getDimension(R.dimen.content_padding);

		if (screenType == ScreenType.CONTEXT_MENU_ACTIONS) {
			imageContainer.setPadding(paddingHorizontal, 0, paddingHorizontal, paddingVertical);
		} else {
			imageContainer.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, 0);
		}
	}
}
