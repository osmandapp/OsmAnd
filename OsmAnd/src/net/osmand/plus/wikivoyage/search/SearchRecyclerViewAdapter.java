package net.osmand.plus.wikivoyage.search;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.wikivoyage.data.WikivoyageSearchResult;

import java.util.ArrayList;
import java.util.List;

public class SearchRecyclerViewAdapter extends RecyclerView.Adapter<SearchRecyclerViewAdapter.ViewHolder> {

	private IconsCache iconsCache;

	private List<WikivoyageSearchResult> items = new ArrayList<>();

	private View.OnClickListener onItemClickListener;

	public void setOnItemClickListener(View.OnClickListener onItemClickListener) {
		this.onItemClickListener = onItemClickListener;
	}

	SearchRecyclerViewAdapter(OsmandApplication app) {
		this.iconsCache = app.getIconsCache();
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
		View itemView = LayoutInflater.from(viewGroup.getContext())
				.inflate(R.layout.wikivoyage_search_list_item, viewGroup, false);
		itemView.setOnClickListener(onItemClickListener);
		return new ViewHolder(itemView);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int pos) {
		boolean lastItem = pos == getItemCount() - 1;

		WikivoyageSearchResult item = items.get(pos);
		holder.icon.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_placeholder_city, R.color.icon_color));
		holder.title.setText(item.getArticleTitle().toString());
		holder.description.setText(item.getLang().toString());
		holder.divider.setVisibility(lastItem ? View.GONE : View.VISIBLE);
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	public WikivoyageSearchResult getItem(int pos) {
		return items.get(pos);
	}

	public void setItems(@Nullable List<WikivoyageSearchResult> items) {
		if (items == null) {
			this.items.clear();
		} else {
			this.items = items;
		}
		notifyDataSetChanged();
	}

	static class ViewHolder extends RecyclerView.ViewHolder {

		final ImageView icon;
		final TextView title;
		final TextView description;
		final View divider;

		public ViewHolder(View itemView) {
			super(itemView);
			icon = (ImageView) itemView.findViewById(R.id.icon);
			title = (TextView) itemView.findViewById(R.id.title);
			description = (TextView) itemView.findViewById(R.id.description);
			divider = itemView.findViewById(R.id.divider);
		}
	}
}
