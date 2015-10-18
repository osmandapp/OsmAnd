package net.osmand.plus.download.items;

import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 */
public class TwoLineWithImagesViewHolder {
	protected final TextView nameTextView;
	protected final TextView descrTextView;
	protected final ImageView leftImageView;
	protected final ImageView rightImageButton;
	protected final Button rightButton;
	protected final ProgressBar progressBar;
	protected final TextView mapDateTextView;
	protected final DownloadActivity context;

	public TwoLineWithImagesViewHolder(View view, DownloadActivity context) {
		this.context = context;
		progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
		mapDateTextView = (TextView) view.findViewById(R.id.mapDateTextView);
		rightButton = (Button) view.findViewById(R.id.rightButton);
		leftImageView = (ImageView) view.findViewById(R.id.leftImageView);
		descrTextView = (TextView) view.findViewById(R.id.description);
		rightImageButton = (ImageView) view.findViewById(R.id.rightImageButton);
		nameTextView = (TextView) view.findViewById(R.id.name);
	}
}
