package net.osmand.plus.wikivoyage.explore;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.wikivoyage.explore.travelcards.ArticleTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.ArticleTravelCard.ArticleTravelVH;
import net.osmand.plus.wikivoyage.explore.travelcards.OpenBetaTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.OpenBetaTravelCard.OpenBetaTravelVH;
import net.osmand.plus.wikivoyage.explore.travelcards.StartEditingTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.StartEditingTravelCard.StartEditingTravelVH;

import java.util.ArrayList;
import java.util.List;

public class ExploreRvAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private static final int HEADER_TYPE = 3;

	private final List<Object> items = new ArrayList<>();

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		switch (viewType) {
			case OpenBetaTravelCard.TYPE:
				return new OpenBetaTravelVH(inflate(parent, R.layout.wikivoyage_open_beta_card));

			case StartEditingTravelCard.TYPE:
				return new StartEditingTravelVH(inflate(parent, R.layout.wikivoyage_start_editing_card));

			case ArticleTravelCard.TYPE:
				int layoutId = ArticleTravelCard.USE_ALTERNATIVE_CARD
						? R.layout.wikivoyage_article_card_alternative
						: R.layout.wikivoyage_article_card;
				return new ArticleTravelVH(inflate(parent, layoutId));

			case HEADER_TYPE:
				return new HeaderVH(inflate(parent, R.layout.wikivoyage_list_header));

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
		Object item = getItem(position);
		if (viewHolder instanceof HeaderVH && item instanceof String) {
			final HeaderVH holder = (HeaderVH) viewHolder;
			holder.title.setText((String) item);
			holder.description.setText(String.valueOf(getArticleItemCount()));
		} else if (viewHolder instanceof ArticleTravelVH && item instanceof ArticleTravelCard) {
			ArticleTravelCard articleTravelCard = (ArticleTravelCard) item;
			articleTravelCard.setLastItem(position == getItemCount() - 1);
			articleTravelCard.bindViewHolder(viewHolder);
		} else if (viewHolder instanceof OpenBetaTravelVH && item instanceof OpenBetaTravelCard) {
			OpenBetaTravelCard openBetaTravelCard = (OpenBetaTravelCard) item;
			openBetaTravelCard.bindViewHolder(viewHolder);
		} else if (viewHolder instanceof StartEditingTravelVH && item instanceof StartEditingTravelCard) {
			StartEditingTravelCard startEditingTravelCard = (StartEditingTravelCard) item;
			startEditingTravelCard.bindViewHolder(viewHolder);
		}
	}

	@Override
	public int getItemViewType(int position) {
		Object object = getItem(position);
		if (object instanceof String) {
			return HEADER_TYPE;
		} else if (object instanceof OpenBetaTravelCard) {
			return ((OpenBetaTravelCard) object).getCardType();
		} else if (object instanceof StartEditingTravelCard) {
			return ((StartEditingTravelCard) object).getCardType();
		} else if (object instanceof ArticleTravelCard) {
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
			if (o instanceof ArticleTravelCard) {
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
