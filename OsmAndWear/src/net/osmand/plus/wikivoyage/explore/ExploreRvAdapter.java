package net.osmand.plus.wikivoyage.explore;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.wikivoyage.explore.travelcards.ArticleTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.ArticleTravelCard.ArticleTravelVH;
import net.osmand.plus.wikivoyage.explore.travelcards.BaseTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.HeaderTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.HeaderTravelCard.HeaderTravelVH;
import net.osmand.plus.wikivoyage.explore.travelcards.OpenBetaTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.OpenBetaTravelCard.OpenBetaTravelVH;
import net.osmand.plus.wikivoyage.explore.travelcards.StartEditingTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.StartEditingTravelCard.StartEditingTravelVH;
import net.osmand.plus.wikivoyage.explore.travelcards.TravelButtonCard;
import net.osmand.plus.wikivoyage.explore.travelcards.TravelButtonCard.TravelButtonVH;
import net.osmand.plus.wikivoyage.explore.travelcards.TravelDownloadUpdateCard;
import net.osmand.plus.wikivoyage.explore.travelcards.TravelGpxCard;
import net.osmand.plus.wikivoyage.explore.travelcards.TravelGpxCard.TravelGpxVH;
import net.osmand.plus.wikivoyage.explore.travelcards.TravelNeededMapsCard;
import net.osmand.plus.wikivoyage.explore.travelcards.TravelNeededMapsCard.NeededMapsVH;

import java.util.ArrayList;
import java.util.List;

public class ExploreRvAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private static final int FIRST_POSITION = 0;
	private static final int SECOND_POSITION = 1;

	private final List<BaseTravelCard> items = new ArrayList<>();
	private TravelDownloadUpdateCard downloadCard;
	private TravelNeededMapsCard neededMapsCard;

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		switch (viewType) {
			case OpenBetaTravelCard.TYPE:
				return new OpenBetaTravelVH(inflate(parent, R.layout.wikivoyage_open_beta_card));

			case StartEditingTravelCard.TYPE:
				return new StartEditingTravelVH(inflate(parent, R.layout.wikivoyage_start_editing_card));

			case ArticleTravelCard.TYPE:
				return new ArticleTravelVH(inflate(parent, R.layout.wikivoyage_article_card));

			case TravelGpxCard.TYPE:
				return new TravelGpxVH(inflate(parent, R.layout.wikivoyage_travel_gpx_card));

			case HeaderTravelCard.TYPE:
				return new HeaderTravelVH(inflate(parent, R.layout.wikivoyage_list_header));

			case TravelButtonCard.TYPE:
				return new TravelButtonVH(inflate(parent, R.layout.wikivoyage_button_card));

			case TravelDownloadUpdateCard.TYPE:
			case TravelNeededMapsCard.TYPE:
				return new NeededMapsVH(inflate(parent, R.layout.travel_needed_maps_card));

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
		BaseTravelCard item = getItem(position);
		if (viewHolder instanceof HeaderTravelVH && item instanceof HeaderTravelCard) {
			HeaderTravelCard headerTravelCard = (HeaderTravelCard) item;
			headerTravelCard.setArticleItemCount(getArticleItemCount());
			headerTravelCard.bindViewHolder(viewHolder);
		} else if (viewHolder instanceof ArticleTravelVH && item instanceof TravelGpxCard) {
			TravelGpxCard travelGpxCard = (TravelGpxCard) item;
			travelGpxCard.setLastItem(position == getLastArticleItemIndex());
			travelGpxCard.bindViewHolder(viewHolder);
		} else if (viewHolder instanceof ArticleTravelVH && item instanceof ArticleTravelCard) {
			ArticleTravelCard articleTravelCard = (ArticleTravelCard) item;
			articleTravelCard.setLastItem(position == getLastArticleItemIndex());
			articleTravelCard.bindViewHolder(viewHolder);
		} else {
			item.bindViewHolder(viewHolder);
		}
	}

	@Override
	public int getItemViewType(int position) {
		return getItem(position).getCardType();
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	public int getArticleItemCount() {
		int count = 0;
		for (BaseTravelCard o : items) {
			if (o instanceof ArticleTravelCard || o instanceof TravelGpxCard) {
				count++;
			}
		}
		return count;
	}

	private int getLastArticleItemIndex() {
		for (int i = items.size() - 1; i > 0; i--) {
			BaseTravelCard o = items.get(i);
			if (o instanceof TravelButtonCard) {
				return 0;
			}
			if (o instanceof ArticleTravelCard || o instanceof TravelGpxCard) {
				return i;
			}
		}
		return 0;
	}

	private BaseTravelCard getItem(int position) {
		return items.get(position);
	}

	@NonNull
	public List<BaseTravelCard> getItems() {
		return items;
	}

	public void setItems(List<BaseTravelCard> items) {
		this.items.clear();
		this.items.addAll(items);
		notifyDataSetChanged();
	}

	private void removeItem(int position) {
		items.remove(position);
	}

	public boolean addItem(int position, BaseTravelCard item) {
		if (position >= 0 && position <= items.size()) {
			items.add(position, item);
			return true;
		}
		return false;
	}

	public void addNeededMapsCard(TravelNeededMapsCard card) {
		this.neededMapsCard = card;
		if (isCardExists(getNeededMapsCardPosition(), TravelNeededMapsCard.TYPE)) {
			updateNeededMapsCard(false);
		} else if (addItem(getNeededMapsCardPosition(), card)) {
			notifyItemInserted(getNeededMapsCardPosition());
		}
	}

	public void updateNeededMapsCard(boolean onlyProgress) {
		if (onlyProgress) {
			TravelNeededMapsCard nd = this.neededMapsCard;
			if (nd != null) {
				nd.updateView();
			}
			return;
		}
		int pos = getNeededMapsCardPosition();
		if (isCardExists(pos, TravelNeededMapsCard.TYPE)) {
			notifyItemChanged(pos);
		}
	}

	public void removeNeededMapsCard() {
		this.neededMapsCard = null;
		int pos = getNeededMapsCardPosition();
		if (isCardExists(pos, TravelNeededMapsCard.TYPE)) {
			removeItem(pos);
			notifyItemRemoved(pos);
		}
	}

	private int getNeededMapsCardPosition() {
		if (isCardExists(FIRST_POSITION, TravelDownloadUpdateCard.TYPE)) {
			return SECOND_POSITION;
		}
		return FIRST_POSITION;
	}

	public void addDownloadUpdateCard(TravelDownloadUpdateCard card) {
		this.downloadCard = card;
		int pos = getDownloadUpdateCardPosition();
		if (isCardExists(pos, TravelDownloadUpdateCard.TYPE)) {
			updateDownloadUpdateCard(false);
		} else if (addItem(pos, card)) {
			notifyDataSetChanged();
		}
	}

	public void updateDownloadUpdateCard(boolean onlyProgress) {
		if (onlyProgress) {
			TravelDownloadUpdateCard dc = this.downloadCard;
			if (dc != null) {
				dc.updateView();
			}
			return;
		}
		int pos = getDownloadUpdateCardPosition();
		if (isCardExists(pos, TravelDownloadUpdateCard.TYPE)) {
			notifyItemChanged(pos);
		}
	}

	public void removeDownloadUpdateCard() {
		this.downloadCard = null;
		int pos = getDownloadUpdateCardPosition();
		if (isCardExists(pos, TravelDownloadUpdateCard.TYPE)) {
			removeItem(pos);
			notifyItemRemoved(pos);
		}
	}

	private int getDownloadUpdateCardPosition() {
		return FIRST_POSITION;
	}

	private boolean isCardExists(int position, int cardType) {
		return items.size() > position && items.get(position).getCardType() == cardType;
	}
}
