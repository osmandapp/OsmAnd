package net.osmand.plus.help;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.ContextMenuListAdapter;
import net.osmand.plus.widgets.ctxmenu.ViewCreator;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class HelpArticlesFragment extends BaseOsmAndFragment implements OnItemClickListener {

	private static final String TAG = HelpArticlesFragment.class.getSimpleName();

	private HelpArticle article;
	private ContextMenuListAdapter adapter;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.help_articles_fragment, container, false);

		ContextMenuAdapter menuAdapter = new ContextMenuAdapter(app);
		List<ContextMenuItem> items = createItems();
		for (ContextMenuItem item : items) {
			menuAdapter.addItem(item);
		}

		FragmentActivity activity = requireActivity();
		ViewCreator viewCreator = new ViewCreator(activity, nightMode);
		viewCreator.setDefaultLayoutId(R.layout.help_list_item);
		adapter = menuAdapter.toListAdapter(activity, viewCreator);

		ListView listView = view.findViewById(R.id.list_view);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(this);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();

		HelpActivity activity = (HelpActivity) requireActivity();
		ActionBar actionBar = activity.getSupportActionBar();
		if (actionBar != null && article != null) {
			actionBar.setTitle(HelpArticleUtils.getArticleName(app, article));
		}
	}

	@NonNull
	public List<ContextMenuItem> createItems() {
		List<ContextMenuItem> items = new ArrayList<>();
		HelpActivity activity = (HelpActivity) requireActivity();

		if (article != null) {
			for (HelpArticle article : article.articles.values()) {
				items.add(HelpArticleUtils.createArticleItem(activity, article));
			}
		}
		if (!Algorithms.isEmpty(items)) {
			items.add(new ContextMenuItem(null).setLayout(R.layout.simple_divider_item));
		}
		return items;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, @NonNull MenuInflater inflater) {
		menu.clear();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == android.R.id.home) {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ContextMenuItem item = adapter.getItem(position);
		ItemClickListener listener = item.getItemClickListener();
		if (listener != null) {
			listener.onContextMenuClick(adapter, view, item, false);
		}
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull HelpArticle article) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			HelpArticlesFragment fragment = new HelpArticlesFragment();
			fragment.setRetainInstance(true);
			fragment.article = article;

			manager.beginTransaction()
					.addToBackStack(null)
					.replace(R.id.fragmentContainer, fragment, TAG)
					.commitAllowingStateLoss();
		}
	}
}