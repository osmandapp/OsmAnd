package net.osmand.plus.wikivoyage.search;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.wikivoyage.data.SearchResult;

import java.util.ArrayList;
import java.util.List;

public class SearchRecyclerViewAdapter extends RecyclerView.Adapter<SearchRecyclerViewAdapter.ViewHolder> {

	private List<SearchResult> items = new ArrayList<>();

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
		View itemView = LayoutInflater.from(viewGroup.getContext())
				.inflate(R.layout.wikivoyage_search_list_item, viewGroup, false);
		itemView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

			}
		});
		return new ViewHolder(itemView);
	}

	@Override
	public void onBindViewHolder(ViewHolder viewHolder, int i) {
		SearchResult item = items.get(i);
		viewHolder.searchTerm.setText(item.getSearchTerm());
		viewHolder.cityId.setText(String.valueOf(item.getCityId()));
		viewHolder.articleTitle.setText(item.getArticleTitle());
		viewHolder.lang.setText(item.getLang());
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	public void setItems(List<SearchResult> items) {
		this.items = items;
		notifyDataSetChanged();
	}

	static class ViewHolder extends RecyclerView.ViewHolder {

		final TextView searchTerm;
		final TextView cityId;
		final TextView articleTitle;
		final TextView lang;

		public ViewHolder(View itemView) {
			super(itemView);
			searchTerm = (TextView) itemView.findViewById(R.id.search_term);
			cityId = (TextView) itemView.findViewById(R.id.city_id);
			articleTitle = (TextView) itemView.findViewById(R.id.article_title);
			lang = (TextView) itemView.findViewById(R.id.lang);
		}
	}
}
