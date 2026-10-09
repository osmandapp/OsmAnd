package net.osmand.plus.wikivoyage.search;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.ResultMatcher;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.plus.wikivoyage.WikiBaseDialogFragment;
import net.osmand.plus.wikivoyage.article.WikivoyageArticleDialogFragment;
import net.osmand.plus.wikivoyage.data.TravelLocalDataHelper;
import net.osmand.plus.wikivoyage.data.WikivoyageSearchHistoryItem;
import net.osmand.plus.wikivoyage.data.WikivoyageSearchResult;

import java.util.ArrayList;
import java.util.List;

public class WikivoyageSearchDialogFragment extends WikiBaseDialogFragment {

	public static final String TAG = "WikivoyageSearchDialogFragment";

	private WikivoyageSearchHelper searchHelper;
	private String searchQuery = "";

	private boolean paused;
	private boolean cancelled;

	private SearchRecyclerViewAdapter adapter;

	private EditText searchEt;
	private ImageButton clearIb;
	private ProgressBar progressBar;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		searchHelper = new WikivoyageSearchHelper(app);

		View mainView = inflate(R.layout.fragment_wikivoyage_search_dialog, container);

		Toolbar toolbar = mainView.findViewById(R.id.toolbar);
		setupToolbar(toolbar);
		toolbar.setContentInsetStartWithNavigation(
				getResources().getDimensionPixelOffset(R.dimen.wikivoyage_search_divider_margin_start)
		);

		searchEt = toolbar.findViewById(R.id.searchEditText);
		searchEt.setHint(R.string.shared_string_search);
		searchEt.setOnEditorActionListener((v, actionId, event) -> {
			if (actionId == EditorInfo.IME_ACTION_SEARCH) {
				String newQueryText = searchQuery + " ";
				searchEt.setText(newQueryText);
				searchEt.setSelection(newQueryText.length());
				AndroidUtils.hideSoftKeyboard(getActivity(), searchEt);
				return true;
			}
			return false;
		});
		searchEt.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				String newQuery = s.toString();
				if (!searchQuery.equalsIgnoreCase(newQuery)) {
					searchQuery = newQuery;
					if (searchQuery.isEmpty()) {
						cancelSearch();
						setAdapterItems(null);
					} else {
						runSearch();
					}
				}
			}
		});

		progressBar = toolbar.findViewById(R.id.searchProgressBar);

		clearIb = toolbar.findViewById(R.id.clearButton);
		clearIb.setImageDrawable(getContentIcon(R.drawable.ic_action_remove_dark));
		clearIb.setOnClickListener(v -> searchEt.setText(""));

		adapter = new SearchRecyclerViewAdapter(app);
		RecyclerView rv = mainView.findViewById(R.id.recycler_view);
		rv.setLayoutManager(new LinearLayoutManager(getContext()));
		rv.setAdapter(adapter);
		adapter.setOnItemClickListener(v -> {
			int pos = rv.getChildAdapterPosition(v);
			FragmentManager fm = getFragmentManager();
			if (pos != RecyclerView.NO_POSITION && fm != null) {
				Object item = adapter.getItem(pos);
				if (item instanceof WikivoyageSearchResult) {
					WikivoyageSearchResult res = (WikivoyageSearchResult) item;
					WikivoyageArticleDialogFragment.showInstance(fm, res.getArticleId(), new ArrayList<>(res.getLangs()));
				} else if (item instanceof WikivoyageSearchHistoryItem) {
					WikivoyageSearchHistoryItem historyItem = (WikivoyageSearchHistoryItem) item;
					WikivoyageArticleDialogFragment
							.showInstanceByTitle(app, fm, historyItem.getArticleTitle(), historyItem.getLang());
				}
			}
		});

		return mainView;
	}

	@Override
	public void onResume() {
		super.onResume();
		paused = false;
		searchEt.requestFocus();
		if (TextUtils.isEmpty(searchQuery)) {
			setAdapterItems(null);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		paused = true;
	}

	private void cancelSearch() {
		cancelled = true;
		if (!paused) {
			switchProgressBarVisibility(false);
		}
	}

	private void setAdapterItems(@Nullable List<WikivoyageSearchResult> items) {
		if (items == null || items.isEmpty()) {
			TravelLocalDataHelper ldh = app.getTravelHelper().getBookmarksHelper();
			adapter.setHistoryItems(ldh.getAllHistory());
		} else {
			adapter.setItems(items);
		}
	}

	private void runSearch() {
		switchProgressBarVisibility(true);
		cancelled = false;
		searchHelper.search(searchQuery, new ResultMatcher<List<WikivoyageSearchResult>>() {
			@Override
			public boolean publish(List<WikivoyageSearchResult> results) {
				app.runInUIThread(() -> {
					if (!isCancelled()) {
						setAdapterItems(results);
						switchProgressBarVisibility(false);
					}
				});
				return true;
			}

			@Override
			public boolean isCancelled() {
				return paused || cancelled;
			}
		});
	}

	private void switchProgressBarVisibility(boolean show) {
		AndroidUiHelper.updateVisibility(clearIb, show);
		AndroidUiHelper.updateVisibility(progressBar, show);
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			WikivoyageSearchDialogFragment fragment = new WikivoyageSearchDialogFragment();
			fragment.show(manager, TAG);
		}
	}
}
