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
import net.osmand.plus.wikivoyage.explore.travelcards.TravelDownloadUpdateCard;
import net.osmand.plus.wikivoyage.explore.travelcards.TravelDownloadUpdateCard.DownloadUpdateVH;

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

			case TravelDownloadUpdateCard.TYPE:
				return new DownloadUpdateVH(inflate(parent, R.layout.travel_download_update_card));

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
			((ArticleTravelCard) item).bindViewHolder(viewHolder);
			ArticleTravelCard articleTravelCard = (ArticleTravelCard) item;
			articleTravelCard.setLastItem(position == getLastArticleItemIndex());
			articleTravelCard.bindViewHolder(viewHolder);
			((ArticleTravelCard) item).bindViewHolder(viewHolder);
		} else if (viewHolder instanceof OpenBetaTravelVH && item instanceof OpenBetaTravelCard) {
			((OpenBetaTravelCard) item).bindViewHolder(viewHolder);
		} else if (viewHolder instanceof StartEditingTravelVH && item instanceof StartEditingTravelCard) {
			((StartEditingTravelCard) item).bindViewHolder(viewHolder);
		} else if (viewHolder instanceof DownloadUpdateVH && item instanceof TravelDownloadUpdateCard) {
			((TravelDownloadUpdateCard) item).bindViewHolder(viewHolder);
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
		} else if (object instanceof TravelDownloadUpdateCard) {
			return ((TravelDownloadUpdateCard) object).getCardType();
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

	private int getLastArticleItemIndex() {
		for (int i = items.size() - 1; i > 0; i--) {
			Object o = items.get(i);
			if (o instanceof ArticleTravelCard) {
				return i;
			}
		}
		return 0;
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

	public boolean addItem(int position, Object item) {
		if (position >= 0 && position <= items.size()) {
			items.add(position, item);
			return true;
		}
		return false;
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
