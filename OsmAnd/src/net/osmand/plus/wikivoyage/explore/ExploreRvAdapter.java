package net.osmand.plus.wikivoyage.explore;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.explore.travelcards.ArticleTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.OpenBetaTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.StartEditingTravelCard;

import java.util.ArrayList;
import java.util.List;

public class ExploreRvAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private static final int HEADER_TYPE = 3;

	private final OsmandSettings settings;

	private final List<Object> items = new ArrayList<>();

	private OsmandApplication app;

	ExploreRvAdapter(OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		if (viewType == OpenBetaTravelCard.TYPE) {
			View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.wikivoyage_open_beta_card, parent, false);
			return new OpenBetaTravelCard.OpenBetaTravelVH(itemView);
		}
		if (viewType == StartEditingTravelCard.TYPE) {
			View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.wikivoyage_start_editing_card, parent, false);
			return new StartEditingTravelCard.StartEditingTravelVH(itemView);

		}
		if (viewType == ArticleTravelCard.TYPE) {
			int layoutId = ArticleTravelCard.USE_ALTERNATIVE_CARD ? R.layout.wikivoyage_article_card_alternative : R.layout.wikivoyage_article_card;
			View itemView = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
			return new ArticleTravelCard.ArticleTravelVH(itemView);
		}
		if (viewType == HEADER_TYPE) {
			View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.wikivoyage_list_header, parent, false);
			return new HeaderVH(itemView);
		}
		return null;
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
		Object object = getItem(position);
		if (viewHolder instanceof HeaderVH) {
			final HeaderVH holder = (HeaderVH) viewHolder;
			holder.title.setText((String) object);
			holder.description.setText(String.valueOf(getArticleItemCount()));
		} else if (viewHolder instanceof ArticleTravelCard.ArticleTravelVH) {
			if (object instanceof ArticleTravelCard) {
				ArticleTravelCard articleTravelCard = (ArticleTravelCard) object;
				articleTravelCard.bindViewHolder(viewHolder);
			}
		} else if (viewHolder instanceof OpenBetaTravelCard.OpenBetaTravelVH) {
			if (object instanceof OpenBetaTravelCard) {
				OpenBetaTravelCard openBetaTravelCard = (OpenBetaTravelCard) object;
				openBetaTravelCard.bindViewHolder(viewHolder);
			}
		} else if (viewHolder instanceof StartEditingTravelCard.StartEditingTravelVH) {
			if (object instanceof StartEditingTravelCard) {
				StartEditingTravelCard startEditingTravelCard = (StartEditingTravelCard) object;
				startEditingTravelCard.bindViewHolder(viewHolder);
			}
		}
	}

	@Override
	public int getItemViewType(int position) {
		Object object = getItem(position);
		if (object instanceof String) {
			return HEADER_TYPE;
		}
		if (object instanceof OpenBetaTravelCard) {
			return ((OpenBetaTravelCard) object).getCardType();
		}
		if (object instanceof StartEditingTravelCard) {
			return ((StartEditingTravelCard) object).getCardType();
		}
		if (object instanceof ArticleTravelCard) {
			return ((ArticleTravelCard) object).getCardType();
		}
		return -1;
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	public int getArticleItemCount() {
		int count = 0;
		for (Object o : items) {
			if (o instanceof TravelArticle) {
				count++;
			}
		}
		return count;
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
}
