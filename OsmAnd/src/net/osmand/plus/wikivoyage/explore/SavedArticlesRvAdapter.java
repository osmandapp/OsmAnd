package net.osmand.plus.wikivoyage.explore;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import net.osmand.PicassoUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.widgets.tools.CropCircleTransformation;
import net.osmand.plus.wikivoyage.WikivoyageUtils;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.data.TravelLocalDataHelper;

import java.util.ArrayList;
import java.util.List;

public class SavedArticlesRvAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private static final int HEADER_TYPE = 0;
	private static final int ITEM_TYPE = 1;

	private final OsmandApplication app;
	private final OsmandSettings settings;

	private final List<Object> items = new ArrayList<>();

	private Listener listener;

	private final Drawable readIcon;
	private final Drawable deleteIcon;
	private PicassoUtils picasso;

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	SavedArticlesRvAdapter(OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		picasso = PicassoUtils.getPicasso(app);

		int colorId = settings.isLightContent()
				? R.color.wikivoyage_active_light : R.color.wikivoyage_active_dark;
		UiUtilities ic = app.getUIUtilities();
		readIcon = ic.getIcon(R.drawable.ic_action_read_article, colorId);
		deleteIcon = ic.getIcon(R.drawable.ic_action_read_later_fill, colorId);
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		boolean header = viewType == HEADER_TYPE;
		int layoutId = header ? R.layout.wikivoyage_list_header : R.layout.wikivoyage_article_card;
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
			TravelArticle article = (TravelArticle) getItem(position);
			final String url = TravelArticle.getImageUrl(article.getImageTitle(), false);
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
			holder.content.setText(article.getContent());
			holder.partOf.setText(article.getGeoDescription());
			holder.leftButton.setText(app.getString(R.string.shared_string_read));
			holder.leftButton.setCompoundDrawablesWithIntrinsicBounds(readIcon, null, null, null);
			holder.rightButton.setText(app.getString(R.string.shared_string_remove));
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
					if (item != null && item instanceof TravelArticle) {
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
					if (item != null && item instanceof TravelArticle) {
						final TravelArticle article = (TravelArticle) item;
						final TravelLocalDataHelper ldh = app.getTravelHelper().getBookmarksHelper();
						ldh.removeArticleFromSaved(article);
						Snackbar snackbar = Snackbar.make(itemView, R.string.article_removed, Snackbar.LENGTH_LONG)
								.setAction(R.string.shared_string_undo, new View.OnClickListener() {
									@Override
									public void onClick(View view) {
										ldh.restoreSavedArticle(article);
									}
								});
						boolean nightMode = !settings.isLightContent();
						UiUtilities.setupSnackbar(snackbar, nightMode);
						int wikivoyageActiveColorResId = nightMode ? R.color.wikivoyage_active_dark : R.color.wikivoyage_active_light;
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
