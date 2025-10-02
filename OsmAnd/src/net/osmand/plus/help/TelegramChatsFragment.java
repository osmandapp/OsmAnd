package net.osmand.plus.help;

import android.content.Intent;
import android.net.Uri;
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
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.ContextMenuListAdapter;
import net.osmand.plus.widgets.ctxmenu.ViewCreator;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.util.Algorithms;

import java.util.LinkedHashMap;
import java.util.Map;

public class TelegramChatsFragment extends BaseFullScreenFragment implements OnItemClickListener {

	private static final String TAG = TelegramChatsFragment.class.getSimpleName();

	private static final String TELEGRAM_CHATS = "telegram_chats";

	private Map<String, String> telegramChats;
	private ContextMenuListAdapter adapter;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

		Bundle arg = getArguments();
		if (arg != null && arg.containsKey(TELEGRAM_CHATS)) {
			telegramChats = (Map<String, String>) AndroidUtils.getSerializable(arg, TELEGRAM_CHATS, LinkedHashMap.class);
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.help_articles_fragment, container, false);

		ContextMenuAdapter menuAdapter = new ContextMenuAdapter(app);

		menuAdapter.addItem(new ContextMenuItem(null)
				.setTitle(getString(R.string.telegram_chats_descr))
				.setLayout(R.layout.description_article_item));

		if (!Algorithms.isEmpty(telegramChats)) {
			for (Map.Entry<String, String> entry : telegramChats.entrySet()) {
				String key = entry.getKey();
				String url = entry.getValue();

				ContextMenuItem item = createSocialItem(key, url);
				menuAdapter.addItem(item);
			}
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
	private ContextMenuItem createSocialItem(@NonNull String key, @NonNull String url) {
		return new ContextMenuItem(null)
				.setTitle(HelpArticleUtils.getTelegramChatName(app, key))
				.setDescription(url)
				.setIcon(R.drawable.ic_action_social_telegram)
				.setListener((uiAdapter, view, item, isChecked) -> {
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
					AndroidUtils.startActivityIfSafe(requireActivity(), intent);
					return false;
				});
	}

	@Override
	public void onResume() {
		super.onResume();

		HelpActivity activity = (HelpActivity) requireActivity();
		ActionBar actionBar = activity.getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle(getString(R.string.telegram_chats));
		}
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
	public void onCreateOptionsMenu(Menu menu, @NonNull MenuInflater inflater) {
		menu.clear();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ContextMenuItem item = adapter.getItem(position);
		ItemClickListener listener = item.getItemClickListener();
		if (listener != null) {
			listener.onContextMenuClick(adapter, view, item, false);
		}
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull Map<String, String> telegramChats) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle bundle = new Bundle();
			bundle.putSerializable(TELEGRAM_CHATS, new LinkedHashMap<>(telegramChats));

			TelegramChatsFragment fragment = new TelegramChatsFragment();
			fragment.setArguments(bundle);
			manager.beginTransaction()
					.addToBackStack(null)
					.replace(R.id.fragmentContainer, fragment, TAG)
					.commitAllowingStateLoss();
		}
	}
}
