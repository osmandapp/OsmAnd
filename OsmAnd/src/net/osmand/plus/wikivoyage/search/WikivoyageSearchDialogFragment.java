package net.osmand.plus.wikivoyage.search;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import net.osmand.ResultMatcher;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.wikivoyage.WikivoyageArticleDialogFragment;
import net.osmand.plus.wikivoyage.data.WikivoyageSearchResult;
import net.osmand.plus.wikivoyage.search.WikivoyageSearchHelper.SearchListener;

import java.util.List;

public class WikivoyageSearchDialogFragment extends BaseOsmAndDialogFragment implements SearchListener {

	public static final String TAG = "WikivoyageSearchDialogFragment";

	private WikivoyageSearchHelper searchHelper;
	private String searchQuery = "";

	private boolean paused;
	private boolean cancelPrev;

	private SearchRecyclerViewAdapter adapter;

	private EditText searchEt;
	private ImageButton clearIb;
	private ProgressBar progressBar;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final OsmandApplication app = getMyApplication();
		searchHelper = new WikivoyageSearchHelper(app);
		final boolean nightMode = !app.getSettings().isLightContent();
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		final View mainView = LayoutInflater.from(new ContextThemeWrapper(app, themeRes))
				.inflate(R.layout.fragment_wikivoyage_search_dialog, container, false);

		Toolbar toolbar = (Toolbar) mainView.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getContentIcon(R.drawable.ic_arrow_back));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		searchEt = (EditText) toolbar.findViewById(R.id.searchEditText);
		searchEt.setHint(R.string.shared_string_search);
		searchEt.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				String newQuery = s.toString();
				if (!searchQuery.equalsIgnoreCase(newQuery)) {
					searchQuery = newQuery;
					if (searchQuery.isEmpty()) {
						cancelSearch();
						adapter.setItems(null);
					} else {
						runSearch();
					}
				}
			}
		});

		progressBar = (ProgressBar) toolbar.findViewById(R.id.searchProgressBar);

		clearIb = (ImageButton) toolbar.findViewById(R.id.clearButton);
		clearIb.setImageDrawable(getContentIcon(R.drawable.ic_action_remove_dark));
		clearIb.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				searchEt.setText("");
			}
		});

		adapter = new SearchRecyclerViewAdapter();
		final RecyclerView rv = (RecyclerView) mainView.findViewById(R.id.recycler_view);
		rv.setLayoutManager(new LinearLayoutManager(getContext()));
		rv.setAdapter(adapter);
		adapter.setOnItemClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int pos = rv.getChildAdapterPosition(v);
				if (pos != RecyclerView.NO_POSITION) {
					WikivoyageArticleDialogFragment.showInstance(getFragmentManager(), adapter.getItem(pos));
				}
			}
		});

		return mainView;
	}

	@Override
	public void onResume() {
		super.onResume();
		paused = false;
		searchHelper.registerListener(this);
		searchEt.requestFocus();
	}

	@Override
	public void onPause() {
		super.onPause();
		paused = true;
		searchHelper.unregisterListener();
	}

	@Override
	public void onSearchStarted() {
		getMyApplication().runInUIThread(new Runnable() {
			public void run() {
				switchProgressBarVisibility(true);
			}
		});
	}

	@Override
	public void onSearchFinished(@Nullable final List<WikivoyageSearchResult> results) {
		getMyApplication().runInUIThread(new Runnable() {
			public void run() {
				adapter.setItems(results);
				switchProgressBarVisibility(false);
			}
		});
	}

	private void cancelSearch() {
		cancelPrev = true;
		if (!paused) {
			switchProgressBarVisibility(false);
		}
	}

	private void runSearch() {
		cancelPrev = true;
		searchHelper.search(searchQuery, new ResultMatcher<WikivoyageSearchResult>() {
			@Override
			public boolean publish(WikivoyageSearchResult object) {
				cancelPrev = false;
				return true;
			}

			@Override
			public boolean isCancelled() {
				return paused || cancelPrev;
			}
		});
	}

	private void switchProgressBarVisibility(boolean show) {
		progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
		clearIb.setVisibility(show ? View.GONE : View.VISIBLE);
	}

	public static boolean showInstance(FragmentManager fm) {
		try {
			WikivoyageSearchDialogFragment fragment = new WikivoyageSearchDialogFragment();
			fragment.show(fm, TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}
}
