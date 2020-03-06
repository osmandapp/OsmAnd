package net.osmand.plus.wikivoyage.explore.travelcards;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.chooseplan.ChoosePlanDialogFragment;

public class OpenBetaTravelCard extends BaseTravelCard {

	public static final int TYPE = 0;

	private FragmentManager fragmentManager;

	public OpenBetaTravelCard(OsmandApplication app, boolean nightMode, FragmentManager fragmentManager) {
		super(app, nightMode);
		this.fragmentManager = fragmentManager;
	}

	@Override
	public void bindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
		if (viewHolder instanceof OpenBetaTravelVH) {
			final OpenBetaTravelVH holder = (OpenBetaTravelVH) viewHolder;
			holder.title.setText(R.string.welcome_to_open_beta);
			holder.description.setText(R.string.welcome_to_open_beta_description);
			holder.backgroundImage.setImageResource(R.drawable.img_help_wikivoyage_articles);
			holder.button.setText(R.string.get_unlimited_access);
			holder.button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					ChoosePlanDialogFragment.showWikivoyageInstance(fragmentManager);
				}
			});
		}
	}

	public static class OpenBetaTravelVH extends RecyclerView.ViewHolder {

		final TextView title;
		final TextView description;
		final TextView button;
		final ImageView backgroundImage;

		public OpenBetaTravelVH(final View itemView) {
			super(itemView);
			title = (TextView) itemView.findViewById(R.id.title);
			description = (TextView) itemView.findViewById(R.id.description);
			button = (TextView) itemView.findViewById(R.id.bottom_button_text);
			backgroundImage = (ImageView) itemView.findViewById(R.id.background_image);
		}
	}

	@Override
	public int getCardType() {
		return TYPE;
	}
}
