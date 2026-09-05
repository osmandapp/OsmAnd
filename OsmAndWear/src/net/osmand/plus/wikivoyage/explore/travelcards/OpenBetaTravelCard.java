package net.osmand.plus.wikivoyage.explore.travelcards;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.wikivoyage.explore.WikivoyageExploreActivity;

public class OpenBetaTravelCard extends BaseTravelCard {

	public static final int TYPE = 0;

	private static boolean closed;

	private final FragmentActivity activity;

	public OpenBetaTravelCard(@NonNull FragmentActivity activity, boolean nightMode) {
		super((OsmandApplication) activity.getApplication(), nightMode);
		this.activity = activity;
	}

	public static boolean isClosed() {
		return closed;
	}

	@Override
	public void bindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
		if (viewHolder instanceof OpenBetaTravelVH) {
			OpenBetaTravelVH holder = (OpenBetaTravelVH) viewHolder;
			holder.title.setText(R.string.welcome_to_open_beta);
			holder.description.setText(R.string.welcome_to_open_beta_description);
			holder.backgroundImage.setImageResource(R.drawable.img_help_wikivoyage_articles);
			//holder.button.setText(R.string.get_unlimited_access);
			holder.button.setText(R.string.shared_string_close);
			holder.button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					closed = true;
					((WikivoyageExploreActivity) activity).updateFragments();
					//ChoosePlanDialogFragment.showWikivoyageInstance(activity.getSupportFragmentManager());
				}
			});
		}
	}

	public static class OpenBetaTravelVH extends RecyclerView.ViewHolder {

		final TextView title;
		final TextView description;
		final TextView button;
		final ImageView backgroundImage;

		public OpenBetaTravelVH(View itemView) {
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
