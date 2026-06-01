package net.osmand.plus.gallery.ui.holders;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.gallery.contract.IGalleryActionListener;
import net.osmand.plus.gallery.model.GalleryAction;
import net.osmand.plus.gallery.model.GalleryItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

public class NoMediaHolder extends RecyclerView.ViewHolder {

	private final ImageView imageView;
	private final TextView titleView;
	private final TextView descriptionView;
	private final DialogButton actionButton;

	public NoMediaHolder(@NonNull View itemView) {
		super(itemView);
		this.imageView = itemView.findViewById(R.id.icon);
		this.titleView = itemView.findViewById(R.id.title);
		this.descriptionView = itemView.findViewById(R.id.description);
		this.actionButton = itemView.findViewById(R.id.no_media_action_button);
	}

	public void bindView(@NonNull GalleryItem.NoMedia item,
	                     @Nullable IGalleryActionListener listener) {
		imageView.setImageResource(R.drawable.ic_action_photo_album);
		titleView.setText(R.string.no_media);
		descriptionView.setText(R.string.no_media_descr);

		GalleryAction action = item.getAction();
		AndroidUiHelper.updateVisibility(actionButton, action != null);

		if (action != null) {
			actionButton.setOnClickListener(v -> {
				if (listener != null) {
					listener.handleGalleryAction(action);
				}
			});
		} else {
			actionButton.setOnClickListener(null);
		}

		// TODO: customize
	}
}
