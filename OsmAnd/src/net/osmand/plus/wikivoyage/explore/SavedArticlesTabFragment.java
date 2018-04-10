package net.osmand.plus.wikivoyage.explore;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;

public class SavedArticlesTabFragment extends BaseOsmAndFragment {

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final View mainView = inflater.inflate(R.layout.fragment_saved_articles_tab, container, false);

		final RecyclerView rv = (RecyclerView) mainView.findViewById(R.id.recycler_view);
		rv.setLayoutManager(new LinearLayoutManager(getContext()));
		rv.setAdapter(new SavedArticlesRvAdapter(getMyApplication()));

		return mainView;
	}
}
