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
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.data.TravelLocalDataHelper;
import net.osmand.plus.wikivoyage.explore.travelcards.ArticleTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.OpenBetaTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.StartEditingTravelCard;

import java.util.ArrayList;
import java.util.List;

public class ExploreRvAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private static final int HEADER_TYPE = 3;
	private static final int ITEM_TYPE = 4;

	private final OsmandSettings settings;

	private final List<Object> items = new ArrayList<>();

	private ExploreRvAdapter.Listener listener;
	OsmandApplication app;
	private final Drawable readIcon;
	private final Drawable deleteIcon;

	public void setListener(ExploreRvAdapter.Listener listener) {
		this.listener = listener;
	}

	ExploreRvAdapter(OsmandApplication app) {
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
		RecyclerView.ViewHolder holder = null;
		View itemView = null;
		int layoutId = 0;
		if (viewType == OpenBetaTravelCard.TYPE) {
			layoutId = R.layout.wikivoyage_open_beta_card;
			itemView = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
			return new OpenBetaTravelCard.OpenBetaTravelVH(itemView);
		}
		if (viewType == StartEditingTravelCard.TYPE) {
			layoutId = R.layout.wikivoyage_start_editing_card;
			itemView = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
			return new StartEditingTravelCard.StartEditingTravelVH(itemView);

		}
		if (viewType == ArticleTravelCard.TYPE) {
			layoutId = ArticleTravelCard.USE_ALTERNATIVE_CARD ? R.layout.wikivoyage_article_card_alternative : R.layout.wikivoyage_article_card;
			itemView = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
			return new ArticleTravelCard.ArticleTravelVH(itemView);
		}
		if (viewType == HEADER_TYPE) {
			layoutId = R.layout.wikivoyage_list_header;
			itemView = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
			return new HeaderVH(itemView);
		}
		return null;
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
		if (viewHolder instanceof HeaderVH) {
			final HeaderVH holder = (HeaderVH) viewHolder;
			holder.title.setText((String) getItem(position));
			holder.description.setText(String.valueOf(items.size() - 3));
		} else if (viewHolder instanceof ArticleTravelCard.ArticleTravelVH) {
			if (getItem(position) instanceof ArticleTravelCard) {
				ArticleTravelCard articleTravelCard = (ArticleTravelCard) getItem(position);
				articleTravelCard.bindViewHolder(viewHolder);
			}
		} else if (viewHolder instanceof OpenBetaTravelCard.OpenBetaTravelVH) {
			if (getItem(position) instanceof OpenBetaTravelCard) {
				OpenBetaTravelCard openBetaTravelCard = (OpenBetaTravelCard) getItem(position);
				openBetaTravelCard.bindViewHolder(viewHolder);
			}
		} else if (viewHolder instanceof StartEditingTravelCard.StartEditingTravelVH) {
			if (getItem(position) instanceof StartEditingTravelCard) {
				StartEditingTravelCard startEditingTravelCard = (StartEditingTravelCard) getItem(position);
				startEditingTravelCard.bindViewHolder(viewHolder);
			}
		}
	}

	@Override
	public int getItemViewType(int position) {
		if (getItem(position) instanceof String) {
			return HEADER_TYPE;
		}
		if (getItem(position) instanceof OpenBetaTravelCard) {
			return ((OpenBetaTravelCard) getItem(position)).getCardType();
		}
		if (getItem(position) instanceof StartEditingTravelCard) {
			return ((StartEditingTravelCard) getItem(position)).getCardType();
		}
		if (getItem(position) instanceof ArticleTravelCard) {
			return ((ArticleTravelCard) getItem(position)).getCardType();
		}
		return -1;
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
