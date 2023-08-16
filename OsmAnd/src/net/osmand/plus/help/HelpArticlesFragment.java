package net.osmand.plus.help;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

	private HelpArticleNode articleNode;
	private ContextMenuListAdapter adapter;

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

	@NonNull
	public List<ContextMenuItem> createItems() {
		List<ContextMenuItem> items = new ArrayList<>();

		if (articleNode != null) {
			for (HelpArticleNode node : articleNode.articles.values()) {
				String title = HelpArticleUtils.getArticleName(app, node.url);

				if (Algorithms.isEmpty(node.articles)) {
					items.add(createArticleItem(title, node.url));
				} else {
					items.add(createGroupItem(title, node));
				}
			}
		}
		return items;
	}

	@NonNull
	private ContextMenuItem createGroupItem(@NonNull String title, @NonNull HelpArticleNode node) {
		return new ContextMenuItem(null)
				.setTitle(title)
				.setIcon(R.drawable.ic_action_book_info)
				.setListener((uiAdapter, view, item, isChecked) -> {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						FragmentManager manager = activity.getSupportFragmentManager();
						HelpArticlesFragment.showInstance(manager, node);
					}
					return false;
				});
	}

	@NonNull
	private ContextMenuItem createArticleItem(@NonNull String title, @NonNull String url) {
		return new ContextMenuItem(null)
				.setTitle(title)
				.setIcon(R.drawable.ic_action_book_info)
				.setListener((uiAdapter, view, item, isChecked) -> {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						FragmentManager manager = activity.getSupportFragmentManager();
						HelpArticleDialogFragment.showInstance(manager, url, title);
					}
					return false;
				});
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ContextMenuItem item = adapter.getItem(position);
		ItemClickListener listener = item.getItemClickListener();
		if (listener != null) {
			listener.onContextMenuClick(adapter, view, item, false);
		}
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull HelpArticleNode articleNode) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			HelpArticlesFragment fragment = new HelpArticlesFragment();
			fragment.setRetainInstance(true);
			fragment.articleNode = articleNode;

			manager.beginTransaction()
					.addToBackStack(null)
					.add(R.id.fragmentContainer, fragment, TAG)
					.commitAllowingStateLoss();
		}
	}
}