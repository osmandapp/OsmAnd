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

	private OsmandApplication app;

	ExploreRvAdapter(OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
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
}
