package net.osmand.plus.wikivoyage.explore;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.wikivoyage.article.WikivoyageArticleDialogFragment;
import net.osmand.plus.wikivoyage.data.WikivoyageArticle;
import net.osmand.plus.wikivoyage.data.WikivoyageLocalDataHelper;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SavedArticlesTabFragment extends BaseOsmAndFragment implements WikivoyageLocalDataHelper.Listener {

	protected static final Log LOG = PlatformUtil.getLog(SavedArticlesTabFragment.class);

	private WikivoyageLocalDataHelper dataHelper;

	private SavedArticlesRvAdapter adapter;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final OsmandApplication app = getMyApplication();
		dataHelper = app.getTravelDbHelper().getLocalDataHelper();

		final View mainView = inflater.inflate(R.layout.fragment_saved_articles_tab, container, false);

		adapter = new SavedArticlesRvAdapter(app);
		adapter.setListener(new SavedArticlesRvAdapter.Listener() {
			@Override
			public void openArticle(WikivoyageArticle article) {
				FragmentManager fm = getFragmentManager();
				if (fm != null) {
					WikivoyageArticleDialogFragment.showInstance(app, fm, article.getCityId(), article.getLang());
				}
			}
		});

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
				if (newItemPosition == newItems.size() - 1 && lastItemChanged()) {
					return false;
				}
				WikivoyageArticle oldArticle = (WikivoyageArticle) oldItem;
				WikivoyageArticle newArticle = (WikivoyageArticle) newItem;
				return oldArticle.getCityId() == newArticle.getCityId()
						&& oldArticle.getLang().equals(newArticle.getLang());
			}
			return false;
		}

		private boolean lastItemChanged() {
			return newItems.get(newItems.size() - 1) != oldItems.get(oldItems.size() - 1);
		}
	}
}
