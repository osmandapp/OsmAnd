package net.osmand.plus.wikivoyage.explore;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.wikivoyage.article.WikivoyageArticleDialogFragment;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.data.TravelGpx;
import net.osmand.plus.wikivoyage.data.TravelLocalDataHelper;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SavedArticlesTabFragment extends BaseOsmAndFragment implements TravelLocalDataHelper.Listener {

	protected static final Log LOG = PlatformUtil.getLog(SavedArticlesTabFragment.class);

	@Nullable
	private TravelLocalDataHelper dataHelper;
	@Nullable
	private SavedArticlesRvAdapter adapter;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		dataHelper = app.getTravelHelper().getBookmarksHelper();
		View mainView = themedInflater.inflate(R.layout.fragment_saved_articles_tab, container, false);

		adapter = new SavedArticlesRvAdapter(app);
		adapter.setListener(article -> {
			if (article instanceof TravelGpx) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					File file = app.getTravelHelper().createGpxFile(article);
					TrackMenuFragment.openTrack(getActivity(), file, null);
				}
			} else {
				FragmentManager fm = getFragmentManager();
				if (fm != null) {
					WikivoyageArticleDialogFragment.showInstance(app, fm, article.generateIdentifier(), article.getLang());
				}
			}
		});

		RecyclerView rv = mainView.findViewById(R.id.recycler_view);
		rv.setLayoutManager(new LinearLayoutManager(getContext()));
		rv.setAdapter(adapter);

		return mainView;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (dataHelper != null) {
			dataHelper.addListener(this);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (dataHelper != null) {
			dataHelper.removeListener(this);
		}
	}

	@Override
	public void savedArticlesUpdated() {
		if (adapter != null && isAdded()) {
			List<Object> newItems = getItems();
			SavedArticlesDiffCallback diffCallback = new SavedArticlesDiffCallback(adapter.getItems(), newItems);
			DiffUtil.DiffResult diffRes = DiffUtil.calculateDiff(diffCallback);
			adapter.setItems(newItems);
			diffRes.dispatchUpdatesTo(adapter);
		}
	}

	public void invalidateAdapter() {
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	private List<Object> getItems() {
		List<Object> items = new ArrayList<>();
		if (dataHelper != null) {
			List<TravelArticle> savedArticles = dataHelper.getSavedArticles();
			if (!savedArticles.isEmpty()) {
				Collections.reverse(savedArticles);
				items.add(getString(R.string.saved_articles));
				items.addAll(savedArticles);
			}
		}
		return items;
	}

	private static class SavedArticlesDiffCallback extends DiffUtil.Callback {

		private final List<Object> oldItems;
		private final List<Object> newItems;

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
			} else if (oldItem instanceof TravelArticle && newItem instanceof TravelArticle) {
				if (newItemPosition == newItems.size() - 1 && lastItemChanged()) {
					return false;
				}
				TravelArticle oldArticle = (TravelArticle) oldItem;
				TravelArticle newArticle = (TravelArticle) newItem;
				return oldArticle.getRouteId() != null && oldArticle.getLang() != null &&
						oldArticle.getRouteId().equals(newArticle.getRouteId())
						&& oldArticle.getLang().equals(newArticle.getLang());
			}
			return false;
		}

		private boolean lastItemChanged() {
			return newItems.get(newItems.size() - 1) != oldItems.get(oldItems.size() - 1);
		}
	}
}
