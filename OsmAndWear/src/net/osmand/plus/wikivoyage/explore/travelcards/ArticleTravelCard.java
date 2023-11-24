package net.osmand.plus.wikivoyage.explore.travelcards;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import net.osmand.plus.utils.PicassoUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.widgets.tools.CropCircleTransformation;
import net.osmand.plus.wikipedia.WikiArticleHelper;
import net.osmand.plus.wikivoyage.WikivoyageUtils;
import net.osmand.plus.wikivoyage.article.WikivoyageArticleDialogFragment;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.data.TravelLocalDataHelper;

public class ArticleTravelCard extends BaseTravelCard {

	public static final int TYPE = 2;

	private final TravelArticle article;
	private final Drawable readIcon;
	private final FragmentManager fragmentManager;
	private boolean isLastItem;

	private final PicassoUtils picasso;

	public ArticleTravelCard(OsmandApplication app, boolean nightMode, TravelArticle article, FragmentManager fragmentManager) {
		super(app, nightMode);
		this.article = article;
		readIcon = getActiveIcon(R.drawable.ic_action_read_article);
		this.fragmentManager = fragmentManager;
		picasso = PicassoUtils.getPicasso(app);
	}

	@Override
	public void bindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
		if (viewHolder instanceof ArticleTravelVH) {
			ArticleTravelVH holder = (ArticleTravelVH) viewHolder;
			String url = TravelArticle.getImageUrl(article.getImageTitle(), false);
			Boolean loaded = picasso.isURLLoaded(url);

			RequestCreator rc = Picasso.get()
					.load(url);
			WikivoyageUtils.setupNetworkPolicy(app.getSettings(), rc);
			rc.transform(new CropCircleTransformation())
					.into(holder.icon, new Callback() {
						@Override
						public void onSuccess() {
							holder.icon.setVisibility(View.VISIBLE);
							picasso.setResultLoaded(url, true);
						}

						@Override
						public void onError(Exception e) {
							holder.icon.setVisibility(View.GONE);
							picasso.setResultLoaded(url, false);
						}
					});

			holder.icon.setVisibility(loaded == null || loaded.booleanValue() ? View.VISIBLE : View.GONE);
			holder.title.setText(article.getTitle());
			holder.content.setText(WikiArticleHelper.getPartialContent(article.getContent()));
			holder.partOf.setText(article.getGeoDescription());
			holder.leftButton.setText(app.getString(R.string.shared_string_read));
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

	private void updateSaveButton(ArticleTravelVH holder) {
		if (article != null) {
			TravelLocalDataHelper helper = app.getTravelHelper().getBookmarksHelper();
			boolean saved = helper.isArticleSaved(article);
			Drawable icon = getActiveIcon(saved ? R.drawable.ic_action_read_later_fill : R.drawable.ic_action_read_later);
			holder.rightButton.setText(saved ? R.string.shared_string_remove : R.string.shared_string_bookmark);
			holder.rightButton.setCompoundDrawablesWithIntrinsicBounds(null, null, icon, null);
			holder.rightButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					app.getTravelHelper().saveOrRemoveArticle(article, !saved);
					updateSaveButton(holder);
				}
			});
		}
	}

	public static class ArticleTravelVH extends RecyclerView.ViewHolder {

		final TextView title;
		final TextView content;
		final TextView partOf;
		final ImageView icon;
		final TextView leftButton;
		final TextView rightButton;
		final View divider;
		final View shadow;

		public ArticleTravelVH(View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			content = itemView.findViewById(R.id.content);
			partOf = itemView.findViewById(R.id.part_of);
			icon = itemView.findViewById(R.id.icon);
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
