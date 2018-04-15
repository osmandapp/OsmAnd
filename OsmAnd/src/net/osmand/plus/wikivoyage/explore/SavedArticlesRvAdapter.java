package net.osmand.plus.wikivoyage.explore;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import net.osmand.AndroidUtils;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.widgets.tools.CropCircleTransformation;
import net.osmand.plus.widgets.tools.CropRectTransformation;
import net.osmand.plus.wikivoyage.WikivoyageUtils;
import net.osmand.plus.wikivoyage.data.WikivoyageArticle;
import net.osmand.plus.wikivoyage.data.WikivoyageLocalDataHelper;

import java.util.ArrayList;
import java.util.List;

public class SavedArticlesRvAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private static final int HEADER_TYPE = 0;
	private static final int ITEM_TYPE = 1;

	private static final boolean USE_ALTERNATIVE_CARD = false;

	private final OsmandApplication app;
	private final OsmandSettings settings;

	private final List<Object> items = new ArrayList<>();

	private Listener listener;

	private final Drawable readIcon;
	private final Drawable deleteIcon;

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	SavedArticlesRvAdapter(OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();

		int colorId = settings.isLightContent()
				? R.color.wikivoyage_active_light : R.color.wikivoyage_active_dark;
		IconsCache ic = app.getIconsCache();
		readIcon = ic.getIcon(R.drawable.ic_action_read_article, colorId);
		deleteIcon = ic.getIcon(R.drawable.ic_action_read_later_fill, colorId);
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		boolean header = viewType == HEADER_TYPE;
		int layoutId = header
				? R.layout.wikivoyage_list_header
				: USE_ALTERNATIVE_CARD ? R.layout.wikivoyage_article_card_alternative : R.layout.wikivoyage_article_card;
		View itemView = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
		return header ? new HeaderVH(itemView) : new ItemVH(itemView);
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
		if (viewHolder instanceof HeaderVH) {
			final HeaderVH holder = (HeaderVH) viewHolder;
			holder.title.setText((String) getItem(position));
			holder.description.setText(String.valueOf(items.size() - 1));
		} else {
			final ItemVH holder = (ItemVH) viewHolder;
			WikivoyageArticle article = (WikivoyageArticle) getItem(position);
			boolean lastItem = position == getItemCount() - 1;
			RequestCreator rc = Picasso.get()
					.load(WikivoyageArticle.getImageUrl(article.getImageTitle(), false));
			WikivoyageUtils.setupNetworkPolicy(settings, rc);
			rc.transform(USE_ALTERNATIVE_CARD ? new CropRectTransformation() : new CropCircleTransformation())
					.into(holder.icon, new Callback() {
						@Override
						public void onSuccess() {
							holder.icon.setVisibility(View.VISIBLE);
						}

						@Override
						public void onError(Exception e) {
							holder.icon.setVisibility(View.GONE);
						}
					});
			holder.title.setText(article.getTitle());
			holder.content.setText(article.getContent());
			holder.partOf.setText(article.getGeoDescription());
			holder.leftButton.setText(app.getString(R.string.shared_string_read));
			holder.leftButton.setCompoundDrawablesWithIntrinsicBounds(readIcon, null, null, null);
			holder.rightButton.setText(app.getString(R.string.shared_string_delete));
			holder.rightButton.setCompoundDrawablesWithIntrinsicBounds(null, null, deleteIcon, null);
			holder.divider.setVisibility(lastItem ? View.GONE : View.VISIBLE);
			holder.shadow.setVisibility(lastItem ? View.VISIBLE : View.GONE);
		}
	}

	@Override
	public int getItemViewType(int position) {
		if (getItem(position) instanceof String) {
			return HEADER_TYPE;
		}
		return ITEM_TYPE;
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
			title = (TextView) itemView.findViewById(R.id.title);
			description = (TextView) itemView.findViewById(R.id.description);
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

		ItemVH(final View itemView) {
			super(itemView);
			title = (TextView) itemView.findViewById(R.id.title);
			content = (TextView) itemView.findViewById(R.id.content);
			partOf = (TextView) itemView.findViewById(R.id.part_of);
			icon = (ImageView) itemView.findViewById(R.id.icon);
			leftButton = (TextView) itemView.findViewById(R.id.left_button);
			rightButton = (TextView) itemView.findViewById(R.id.right_button);
			divider = itemView.findViewById(R.id.divider);
			shadow = itemView.findViewById(R.id.shadow);

			View.OnClickListener readClickListener = new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					Object item = getItemByPosition();
					if (item != null && item instanceof WikivoyageArticle) {
						if (listener != null) {
							listener.openArticle((WikivoyageArticle) item);
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
					if (item != null && item instanceof WikivoyageArticle) {
						final WikivoyageArticle article = (WikivoyageArticle) item;
						final WikivoyageLocalDataHelper ldh = app.getTravelDbHelper().getLocalDataHelper();;
						ldh.removeArticleFromSaved(article);
						Snackbar snackbar = Snackbar.make(itemView, R.string.article_removed, Snackbar.LENGTH_LONG)
								.setAction(R.string.shared_string_undo, new View.OnClickListener() {
									@Override
									public void onClick(View view) {
										ldh.restoreSavedArticle(article);
									}
								});
						AndroidUtils.setSnackbarTextColor(snackbar, R.color.wikivoyage_active_dark);
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
		void openArticle(WikivoyageArticle article);
	}
}
