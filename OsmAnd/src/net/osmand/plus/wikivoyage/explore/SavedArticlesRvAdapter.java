package net.osmand.plus.wikivoyage.explore;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.PicassoUtils;
import net.osmand.osm.OsmRouteType;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.widgets.tools.CropCircleTransformation;
import net.osmand.plus.wikipedia.WikiArticleHelper;
import net.osmand.plus.wikivoyage.WikivoyageUtils;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.data.TravelGpx;
import net.osmand.plus.wikivoyage.data.TravelHelper;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.wikivoyage.explore.travelcards.TravelGpxCard.TravelGpxVH;

public class SavedArticlesRvAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private static final int HEADER_TYPE = 0;
	private static final int ARTICLE_TYPE = 1;
	private static final int GPX_TYPE = 2;

	private final OsmandApplication app;
	private final OsmandSettings settings;

	private final List<Object> items = new ArrayList<>();

	private Listener listener;

	private final Drawable readIcon;
	private final Drawable deleteIcon;
	private final PicassoUtils picasso;
	boolean nightMode;

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	SavedArticlesRvAdapter(OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		picasso = PicassoUtils.getPicasso(app);
		nightMode = !app.getSettings().isLightContent();
		readIcon = getActiveIcon(R.drawable.ic_action_read_article);
		deleteIcon = getActiveIcon(R.drawable.ic_action_read_later_fill);
	}

	private Drawable getActiveIcon(@DrawableRes int iconId) {
		int colorId = nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
		return app.getUIUtilities().getIcon(iconId, colorId);
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		switch (viewType) {
			case HEADER_TYPE:
				return new HeaderVH(inflate(parent, R.layout.wikivoyage_list_header));
			case ARTICLE_TYPE:
				return new ItemVH(inflate(parent, R.layout.wikivoyage_article_card));
			case GPX_TYPE:
				return new TravelGpxVH(inflate(parent, R.layout.wikivoyage_travel_gpx_card));
			default:
				throw new RuntimeException("Unsupported view type: " + viewType);
		}
	}

	@NonNull
	private View inflate(@NonNull ViewGroup parent, @LayoutRes int layoutId) {
		return LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
		if (viewHolder instanceof HeaderVH) {
			HeaderVH holder = (HeaderVH) viewHolder;
			holder.title.setText((String) getItem(position));
			holder.description.setText(String.valueOf(items.size() - 1));
		} else if (viewHolder instanceof ItemVH) {
			ItemVH holder = (ItemVH) viewHolder;
			TravelArticle article = (TravelArticle) getItem(position);
			String url = TravelArticle.getImageUrl(article.getImageTitle(), false);
			Boolean loaded = picasso.isURLLoaded(url);
			boolean lastItem = position == getItemCount() - 1;

			RequestCreator rc = Picasso.get().load(url);
			WikivoyageUtils.setupNetworkPolicy(settings, rc);
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
			holder.leftButton.setCompoundDrawablesWithIntrinsicBounds(readIcon, null, null, null);
			holder.rightButton.setText(app.getString(R.string.shared_string_remove));
			holder.rightButton.setCompoundDrawablesWithIntrinsicBounds(null, null, deleteIcon, null);
			holder.divider.setVisibility(lastItem ? View.GONE : View.VISIBLE);
			holder.shadow.setVisibility(lastItem ? View.VISIBLE : View.GONE);
		} else if (viewHolder instanceof TravelGpxVH) {
			TravelGpx article = (TravelGpx) getItem(position);
			TravelGpxVH holder = (TravelGpxVH) viewHolder;
			holder.title.setText(article.getTitle());
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
					if (listener != null) {
						listener.openArticle(article);
					}
				}
			};
			holder.leftButton.setOnClickListener(readClickListener);
			holder.itemView.setOnClickListener(readClickListener);
			holder.leftButton.setCompoundDrawablesWithIntrinsicBounds(readIcon, null, null, null);
			updateSaveButton(holder, article);
		}
	}

	private void updateSaveButton(TravelGpxVH holder, TravelGpx article) {
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
					updateSaveButton(holder, article);
				}
			});
		}
	}

	@Override
	public int getItemViewType(int position) {
		if (getItem(position) instanceof String) {
			return HEADER_TYPE;
		} else if (getItem(position) instanceof TravelGpx) {
			return GPX_TYPE;
		}
		return ARTICLE_TYPE;
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	private Object getItem(int position) {
		return items.get(position);
	}

	@NonNull
	public List<Object> getItems() {
		return items;
	}

	public void setItems(List<Object> items) {
		this.items.clear();
		this.items.addAll(items);
	}

	static class HeaderVH extends RecyclerView.ViewHolder {

		final TextView title;
		final TextView description;

		HeaderVH(View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			description = itemView.findViewById(R.id.description);
		}
	}

	class ItemVH extends RecyclerView.ViewHolder {

		final TextView title;
		final TextView content;
		final TextView partOf;
		final ImageView icon;
		final TextView leftButton;
		final TextView rightButton;
		final View divider;
		final View shadow;

		ItemVH(View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			content = itemView.findViewById(R.id.content);
			partOf = itemView.findViewById(R.id.part_of);
			icon = itemView.findViewById(R.id.icon);
			leftButton = itemView.findViewById(R.id.left_button);
			rightButton = itemView.findViewById(R.id.right_button);
			divider = itemView.findViewById(R.id.divider);
			shadow = itemView.findViewById(R.id.shadow);

			View.OnClickListener readClickListener = new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					Object item = getItemByPosition();
					if (item instanceof TravelArticle) {
						if (listener != null) {
							listener.openArticle((TravelArticle) item);
						}
					}
				}
			};

			itemView.setOnClickListener(readClickListener);
			leftButton.setOnClickListener(readClickListener);

			rightButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					Object item = getItemByPosition();
					if (item instanceof TravelArticle) {
						TravelArticle article = (TravelArticle) item;
						TravelHelper helper = app.getTravelHelper();
						helper.saveOrRemoveArticle(article, false);
						Snackbar snackbar = Snackbar.make(itemView, R.string.article_removed, Snackbar.LENGTH_LONG)
								.setAction(R.string.shared_string_undo, new View.OnClickListener() {
									@Override
									public void onClick(View view) {
										helper.saveOrRemoveArticle(article, true);
									}
								});
						boolean nightMode = !settings.isLightContent();
						UiUtilities.setupSnackbar(snackbar, nightMode);
						int wikivoyageActiveColorResId = nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
						UiUtilities.setupSnackbar(snackbar, nightMode, null, null, wikivoyageActiveColorResId, null);
						snackbar.show();
					}
				}
			});
		}

		@Nullable
		private Object getItemByPosition() {
			int pos = getAdapterPosition();
			if (pos != RecyclerView.NO_POSITION) {
				return getItem(pos);
			}
			return null;
		}
	}

	interface Listener {
		void openArticle(TravelArticle article);
	}
}
