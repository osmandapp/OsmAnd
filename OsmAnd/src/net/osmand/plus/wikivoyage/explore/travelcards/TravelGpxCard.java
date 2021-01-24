package net.osmand.plus.wikivoyage.explore.travelcards;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.wikipedia.WikiArticleHelper;
import net.osmand.plus.wikivoyage.article.WikivoyageArticleDialogFragment;
import net.osmand.plus.wikivoyage.data.TravelGpx;
import net.osmand.plus.wikivoyage.data.TravelLocalDataHelper;

public class TravelGpxCard extends BaseTravelCard {

	public static final int TYPE = 3;

	private final TravelGpx article;
	private final Drawable readIcon;
	private final FragmentManager fragmentManager;
	private boolean isLastItem;

	public TravelGpxCard(@NonNull OsmandApplication app, boolean nightMode, @NonNull TravelGpx article,
	                     @NonNull FragmentManager fragmentManager) {
		super(app, nightMode);
		this.article = article;
		readIcon = getActiveIcon(R.drawable.ic_action_read_article);
		this.fragmentManager = fragmentManager;
	}

	@Override
	public void bindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
		if (viewHolder instanceof TravelGpxVH) {
			final TravelGpxVH holder = (TravelGpxVH) viewHolder;
			holder.title.setText(article.getTitle());
			holder.content.setText(WikiArticleHelper.getPartialContent(article.getContent()));
			holder.distance.setText(OsmAndFormatter.getFormattedDistance(article.totalDistance,app));
			holder.diffElevationUp.setText(OsmAndFormatter.getFormattedAlt(article.diffElevationUp,app));
			holder.diffElevationDown.setText(OsmAndFormatter.getFormattedAlt(article.diffElevationDown,app));
			holder.leftButton.setText(app.getString(R.string.shared_string_view));
			View.OnClickListener readClickListener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (fragmentManager != null) {
						WikivoyageArticleDialogFragment.showInstance(app, fragmentManager,
								article.generateIdentifier(), article.getLang());
					}
				}
			};
			holder.leftButton.setOnClickListener(readClickListener);
			holder.itemView.setOnClickListener(readClickListener);
			holder.leftButton.setCompoundDrawablesWithIntrinsicBounds(readIcon, null, null, null);
			updateSaveButton(holder);
			holder.divider.setVisibility(isLastItem ? View.GONE : View.VISIBLE);
			holder.shadow.setVisibility(isLastItem ? View.VISIBLE : View.GONE);
		}
	}

	private void updateSaveButton(final TravelGpxVH holder) {
		if (article != null) {
			final TravelLocalDataHelper helper = app.getTravelHelper().getBookmarksHelper();
			final boolean saved = helper.isArticleSaved(article);
			Drawable icon = getActiveIcon(saved ? R.drawable.ic_action_read_later_fill : R.drawable.ic_action_read_later);
			holder.rightButton.setText(saved ? R.string.shared_string_remove : R.string.shared_string_save);
			holder.rightButton.setCompoundDrawablesWithIntrinsicBounds(null, null, icon, null);
			holder.rightButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (saved) {
						helper.removeArticleFromSaved(article);
					} else {
						app.getTravelHelper().createGpxFile(article);
						helper.addArticleToSaved(article);
					}
					updateSaveButton(holder);
				}
			});
		}
	}

	public static class TravelGpxVH extends RecyclerView.ViewHolder {

		final TextView title;
		final TextView content;
		final TextView distance;
		final TextView diffElevationUp;
		final TextView diffElevationDown;
		final TextView leftButton;
		final TextView rightButton;
		final View divider;
		final View shadow;

		public TravelGpxVH(final View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			content = itemView.findViewById(R.id.content);
			distance = itemView.findViewById(R.id.distance);
			diffElevationUp = itemView.findViewById(R.id.diff_ele_up);
			diffElevationDown = itemView.findViewById(R.id.diff_ele_down);
			leftButton = itemView.findViewById(R.id.left_button);
			rightButton = itemView.findViewById(R.id.right_button);
			divider = itemView.findViewById(R.id.divider);
			shadow = itemView.findViewById(R.id.shadow);
		}
	}

	public void setLastItem(boolean lastItem) {
		isLastItem = lastItem;
	}

	@Override
	public int getCardType() {
		return TYPE;
	}
}
