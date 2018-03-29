package net.osmand.plus.wikivoyage;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import net.osmand.plus.R;
import net.osmand.plus.wikivoyage.search.WikivoyageSearchDialogFragment;

public class WikivoyageExploreDialogFragment extends WikivoyageBaseDialogFragment {

	public static final String TAG = "WikivoyageExploreDialogFragment";

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final View mainView = inflate(R.layout.fragment_wikivoyage_explore_dialog, container);

		setupToolbar((Toolbar) mainView.findViewById(R.id.toolbar));

		((ImageView) mainView.findViewById(R.id.search_icon))
				.setImageDrawable(getContentIcon(R.drawable.ic_action_search_dark));

		mainView.findViewById(R.id.search_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				WikivoyageSearchDialogFragment.showInstance(getFragmentManager());
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
}
