package net.osmand.plus.wikivoyage;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import net.osmand.plus.wikivoyage.data.WikivoyageDbHelper.SearchResult;

import java.util.ArrayList;
import java.util.List;

public class WikivoyageExploreDialogFragment extends BaseOsmAndDialogFragment {

	public static final String TAG = "WikivoyageExploreDialogFragment";

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final View mainView = inflater.inflate(R.layout.fragment_wikivoyage_explore_dialog, container);

		final EditText searchEt = (EditText) mainView.findViewById(R.id.search_edit_text);

		final SearchListAdapter adapter = new SearchListAdapter();
		RecyclerView rv = (RecyclerView) mainView.findViewById(R.id.recycler_view);
		rv.setLayoutManager(new LinearLayoutManager(getContext()));
		rv.setAdapter(adapter);

		ImageButton backBtn = (ImageButton) mainView.findViewById(R.id.back_button);
		backBtn.setImageDrawable(getContentIcon(R.drawable.ic_arrow_back));
		backBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		ImageButton searchBtn = (ImageButton) mainView.findViewById(R.id.search_button);
		searchBtn.setImageDrawable(getContentIcon(R.drawable.ic_action_search_dark));
		searchBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				adapter.setItems(getMyApplication().getWikivoyageDbHelper().search((searchEt).getText().toString()));
			}
		});

		searchEt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEARCH) {
					adapter.setItems(getMyApplication().getWikivoyageDbHelper().search((searchEt).getText().toString()));
					return true;
				}
				return false;
			}
		});

		return mainView;
	}

	public static boolean showInstance(FragmentManager fm) {
		try {
			WikivoyageExploreDialogFragment fragment = new WikivoyageExploreDialogFragment();
			fragment.show(fm, TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}

	private static class SearchListAdapter extends RecyclerView.Adapter<SearchListAdapter.ViewHolder> {

		private List<SearchResult> items = new ArrayList<>();

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
			View itemView = LayoutInflater.from(viewGroup.getContext())
					.inflate(R.layout.wikivoyage_search_list_item, viewGroup, false);
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
}
