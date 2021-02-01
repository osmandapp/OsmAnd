package net.osmand.plus.wikivoyage.explore.travelcards;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.track.TrackMenuFragment;
import net.osmand.plus.wikivoyage.data.TravelGpx;
import net.osmand.plus.wikivoyage.data.TravelLocalDataHelper;

import java.io.File;

public class TravelGpxCard extends BaseTravelCard {

	public static final int TYPE = 3;

	private final TravelGpx article;
	private final Drawable readIcon;
	private final FragmentActivity activity;
	private boolean isLastItem;

	public TravelGpxCard(@NonNull OsmandApplication app, boolean nightMode, @NonNull TravelGpx article,
	                     @NonNull FragmentActivity activity) {
		super(app, nightMode);
		this.article = article;
		readIcon = getActiveIcon(R.drawable.ic_action_read_article);
		this.activity = activity;
	}

	@Override
	public void bindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
		if (viewHolder instanceof TravelGpxVH) {
			final TravelGpxVH holder = (TravelGpxVH) viewHolder;
			holder.title.setText(article.getTitle());
			Drawable icon = getActiveIcon(R.drawable.ic_action_user_account_16);
			holder.user.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
			holder.user.setText(article.user);
			AndroidUtils.setBackground(app, holder.user, nightMode, R.drawable.btn_border_bg_light, R.drawable.btn_border_bg_dark);
			holder.distance.setText(OsmAndFormatter.getFormattedDistance(article.totalDistance, app));
			holder.diffElevationUp.setText(OsmAndFormatter.getFormattedAlt(article.diffElevationUp, app));
			holder.diffElevationDown.setText(OsmAndFormatter.getFormattedAlt(article.diffElevationDown, app));
			holder.leftButton.setText(app.getString(R.string.shared_string_view));
			View.OnClickListener readClickListener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (activity != null) {
						File file = app.getTravelHelper().createGpxFile(article);
						TrackMenuFragment.openTrack(activity, file, null);
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

		public final TextView title;
		public final TextView user;
		public final TextView distance;
		public final TextView diffElevationUp;
		public final TextView diffElevationDown;
		public final TextView leftButton;
		public final TextView rightButton;
		public final View divider;
		public final View shadow;

		public TravelGpxVH(final View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			user = itemView.findViewById(R.id.user_name);
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
