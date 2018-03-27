package net.osmand.plus.wikivoyage.search;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;

public class WikivoyageSearchDialogFragment extends BaseOsmAndDialogFragment {

	public static final String TAG = "WikivoyageSearchDialogFragment";

	private SearchRecyclerViewAdapter adapter;

	private EditText searchEt;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final View mainView = inflater.inflate(R.layout.fragment_wikivoyage_search_dialog, container);

		Toolbar toolbar = (Toolbar) mainView.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getContentIcon(R.drawable.ic_arrow_back));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		searchEt = (EditText) toolbar.findViewById(R.id.search_edit_text);
		searchEt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEARCH) {
					runSearch();
					return true;
				}
				return false;
			}
		});

		ImageButton searchBtn = (ImageButton) mainView.findViewById(R.id.search_button);
		searchBtn.setImageDrawable(getContentIcon(R.drawable.ic_action_search_dark));
		searchBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				runSearch();
			}
		});

		adapter = new SearchRecyclerViewAdapter();
		RecyclerView rv = (RecyclerView) mainView.findViewById(R.id.recycler_view);
		rv.setLayoutManager(new LinearLayoutManager(getContext()));
		rv.setAdapter(adapter);

		return mainView;
	}

	private void runSearch() {
		adapter.setItems(getMyApplication().getWikivoyageDbHelper().search((searchEt).getText().toString()));
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
