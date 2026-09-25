package net.osmand.plus.wikivoyage.explore.travelcards;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.shared.gpx.GpxFile;
import net.osmand.osm.OsmRouteType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.wikivoyage.data.TravelGpx;
import net.osmand.plus.wikivoyage.data.TravelHelper;
import net.osmand.util.Algorithms;

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
			TravelGpxVH holder = (TravelGpxVH) viewHolder;
			if (!Algorithms.isEmpty(article.getDescription())) {
				holder.title.setText(article.getDescription());
			} else {
				holder.title.setText(article.getTitle());
			}
			holder.userIcon.setImageDrawable(getActiveIcon(R.drawable.ic_action_user_account_16));
			holder.user.setText(article.user);
			String activityTypeKey = article.activityType;
			if (!Algorithms.isEmpty(activityTypeKey)) {
				OsmRouteType activityType = OsmRouteType.getOrCreateTypeFromName(activityTypeKey);
				int activityTypeIcon = AndroidUtils.getActivityTypeIcon(app, activityType);
				holder.activityTypeIcon.setImageDrawable(getActiveIcon(activityTypeIcon));
				holder.activityType.setText(AndroidUtils.getActivityTypeTitle(app, activityType));
				holder.activityTypeLabel.setVisibility(View.VISIBLE);
			}
			holder.distance.setText(OsmAndFormatter.getFormattedDistance(article.totalDistance, app));
			holder.diffElevationUp.setText(OsmAndFormatter.getFormattedAlt(article.diffElevationUp, app));
			holder.diffElevationDown.setText(OsmAndFormatter.getFormattedAlt(article.diffElevationDown, app));
			holder.leftButton.setText(app.getString(R.string.shared_string_view));
			View.OnClickListener readClickListener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (activity != null) {
						app.getTravelHelper().getArticleById(article.generateIdentifier(), null, true,
								new TravelHelper.GpxReadCallback() {
									@Override
									public void onGpxFileReading() {
									}

									@Override
									public void onGpxFileRead(@Nullable GpxFile gpxFile) {
										File file = app.getTravelHelper().createGpxFile(article);
										TrackMenuFragment.openTrack(activity, file, null);
									}
								});
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

	private void updateSaveButton(TravelGpxVH holder) {
		if (article != null) {
			TravelHelper helper = app.getTravelHelper();
			boolean saved = helper.getBookmarksHelper().isArticleSaved(article);
			Drawable icon = getActiveIcon(saved ? R.drawable.ic_action_read_later_fill : R.drawable.ic_action_read_later);
			holder.rightButton.setText(saved ? R.string.shared_string_remove : R.string.shared_string_save);
			holder.rightButton.setCompoundDrawablesWithIntrinsicBounds(null, null, icon, null);
			holder.rightButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					helper.saveOrRemoveArticle(article, !saved);
					updateSaveButton(holder);
				}
			});
		}
	}

	public static class TravelGpxVH extends RecyclerView.ViewHolder {

		public final TextView title;
		public final TextView user;
		public final ImageView userIcon;
		public final TextView activityType;
		public final ImageView activityTypeIcon;
		public final View activityTypeLabel;
		public final TextView distance;
		public final TextView diffElevationUp;
		public final TextView diffElevationDown;
		public final TextView leftButton;
		public final TextView rightButton;
		public final View divider;
		public final View shadow;

		public TravelGpxVH(View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			user = itemView.findViewById(R.id.user_name);
			userIcon = itemView.findViewById(R.id.user_icon);
			activityType = itemView.findViewById(R.id.activity_type);
			activityTypeIcon = itemView.findViewById(R.id.activity_type_icon);
			activityTypeLabel = itemView.findViewById(R.id.activity_type_label);
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
