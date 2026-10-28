package net.osmand.plus.mapcontextmenu.gallery.holders;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.gallery.GalleryGridAdapter.ImageCardListener;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

public class NoInternetHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;
	private final ImageView imageView;
	private final DialogButton tryAgainButton;
	private final ProgressBar progressBar;

	public NoInternetHolder(@NonNull View itemView, @NonNull OsmandApplication app) {
		super(itemView);
		this.imageView = itemView.findViewById(R.id.icon);
		this.app = app;
		this.tryAgainButton = itemView.findViewById(R.id.try_again_button);
		this.progressBar = itemView.findViewById(R.id.progress);
	}

	public void bindView(boolean nightMode, @NonNull ImageCardListener listener, boolean loadingImages) {
		Drawable icon = app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_wifi_off, ColorUtilities.getDefaultIconColor(app, nightMode));
		imageView.setImageDrawable(icon);

		updateProgressBar(loadingImages);
		tryAgainButton.setOnClickListener(v -> listener.onReloadImages());
	}

	public void updateProgressBar(boolean loadingImages) {
		View text = tryAgainButton.findViewById(R.id.button_text);
		if (loadingImages) {
			text.setVisibility(View.INVISIBLE);
			AndroidUiHelper.updateVisibility(progressBar, true);
		} else {
			text.setVisibility(View.VISIBLE);
			AndroidUiHelper.updateVisibility(progressBar, false);
		}
	}
}
