package net.osmand.plus.wikivoyage.explore;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.wikivoyage.data.WikivoyageArticle;
import net.osmand.plus.wikivoyage.data.WikivoyageLocalDataHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SavedArticlesTabFragment extends BaseOsmAndFragment implements WikivoyageLocalDataHelper.Listener {

	private WikivoyageLocalDataHelper dataHelper;

	private SavedArticlesRvAdapter adapter;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		dataHelper = WikivoyageLocalDataHelper.getInstance(getMyApplication());
		adapter = new SavedArticlesRvAdapter(getMyApplication());

		final View mainView = inflater.inflate(R.layout.fragment_saved_articles_tab, container, false);

		final RecyclerView rv = (RecyclerView) mainView.findViewById(R.id.recycler_view);
		rv.setLayoutManager(new LinearLayoutManager(getContext()));
		rv.setAdapter(adapter);

		return mainView;
	}

	@Override
	public void onResume() {
		super.onResume();
		adapter.setItems(getItems());
		adapter.notifyDataSetChanged();
		dataHelper.setListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		dataHelper.setListener(null);
	}

	@Override
	public void savedArticlesUpdated() {
		List<Object> newItems = getItems();
		SavedArticlesDiffCallback diffCallback = new SavedArticlesDiffCallback(adapter.getItems(), newItems);
		DiffUtil.DiffResult diffRes = DiffUtil.calculateDiff(diffCallback);
		adapter.setItems(newItems);
		diffRes.dispatchUpdatesTo(adapter);
	}

	private List<Object> getItems() {
		List<Object> items = new ArrayList<>();
		List<WikivoyageArticle> savedArticles = dataHelper.getSavedArticles();
		if (!savedArticles.isEmpty()) {
			Collections.reverse(savedArticles);
			items.add(getString(R.string.saved_articles));
			items.addAll(savedArticles);
		}
		return items;
	}

	private static class SavedArticlesDiffCallback extends DiffUtil.Callback {

		private List<Object> oldItems;
		private List<Object> newItems;

		SavedArticlesDiffCallback(List<Object> oldItems, List<Object> newItems) {
			this.oldItems = oldItems;
			this.newItems = newItems;
		}

		@Override
		public int getOldListSize() {
			return oldItems.size();
		}

		@Override
		public int getNewListSize() {
			return newItems.size();
		}

		@Override
		public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
			Object oldItem = oldItems.get(oldItemPosition);
			Object newItem = newItems.get(newItemPosition);
			return (oldItem instanceof String && newItem instanceof String) || oldItem == newItem;
		}

		@Override
		public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
			Object oldItem = oldItems.get(oldItemPosition);
			Object newItem = newItems.get(newItemPosition);
			if (oldItem instanceof String && newItem instanceof String) {
				return false;
			} else if (oldItem instanceof WikivoyageArticle && newItem instanceof WikivoyageArticle) {
				WikivoyageArticle oldArticle = (WikivoyageArticle) oldItem;
				WikivoyageArticle newArticle = (WikivoyageArticle) newItem;
				return oldArticle.getCityId() == newArticle.getCityId()
						&& oldArticle.getLang().equals(newArticle.getLang());
			}
			return false;
		}
	}
}
