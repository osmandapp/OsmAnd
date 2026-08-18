package net.osmand.plus.wikivoyage.explore.travelcards;

import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;

public class StartEditingTravelCard extends BaseTravelCard {

	public static final int TYPE = 1;

	private final FragmentActivity activity;

	public StartEditingTravelCard(@NonNull FragmentActivity activity, boolean nightMode) {
		super((OsmandApplication) activity.getApplication(), nightMode);
		this.activity = activity;
	}

	@Override
	public void bindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
		if (viewHolder instanceof StartEditingTravelVH) {
			StartEditingTravelVH holder = (StartEditingTravelVH) viewHolder;
			holder.title.setText(R.string.start_editing_card_image_text);
			holder.description.setText(R.string.start_editing_card_description);
			holder.backgroundImage.setImageResource(R.drawable.img_help_wikivoyage_contribute);
			holder.button.setText(R.string.start_editing);
			holder.button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					AndroidUtils.openUrl(activity,
							Uri.parse("https://" + app.getLanguage().toLowerCase() + ".m.wikivoyage.org"), nightMode);
				}
			});
		}
	}

	public static class StartEditingTravelVH extends RecyclerView.ViewHolder {

		final TextView title;
		final TextView description;
		final TextView button;
		final ImageView backgroundImage;

		public StartEditingTravelVH(View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			description = itemView.findViewById(R.id.description);
			button = itemView.findViewById(R.id.bottom_button_text);
			backgroundImage = itemView.findViewById(R.id.background_image);
		}
	}

	@Override
	public int getCardType() {
		return TYPE;
	}
}
