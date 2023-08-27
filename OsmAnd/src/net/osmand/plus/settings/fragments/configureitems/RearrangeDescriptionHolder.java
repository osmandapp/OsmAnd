package net.osmand.plus.settings.fragments.configureitems;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.R;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.UnmovableItem;

class RearrangeDescriptionHolder extends ViewHolder implements UnmovableItem {

	protected final ImageView image;
	protected final ImageView deviceImage;
	protected final TextView description;
	protected final FrameLayout imageContainer;

	RearrangeDescriptionHolder(@NonNull View itemView) {
		super(itemView);
		image = itemView.findViewById(R.id.image);
		deviceImage = itemView.findViewById(R.id.device_image);
		description = itemView.findViewById(R.id.description);
		imageContainer = itemView.findViewById(R.id.image_container);
	}

	@Override
	public boolean isMovingDisabled() {
		return true;
	}
}
