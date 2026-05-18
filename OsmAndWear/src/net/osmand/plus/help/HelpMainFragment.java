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
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.feedback.FeedbackHelper;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.ContextMenuListAdapter;
import net.osmand.plus.widgets.ctxmenu.ViewCreator;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.callback.ItemLongClickListener;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HelpMainFragment extends BaseOsmAndFragment implements OnItemClickListener, OnItemLongClickListener {

	public static final String TAG = HelpMainFragment.class.getSimpleName();

	private ContextMenuListAdapter adapter;
	private ListView listView;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.help_main_screen, container, false);

		listView = view.findViewById(R.id.list_view);
		listView.setOnItemClickListener(this);
		listView.setOnItemLongClickListener(this);

		updateContent();

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();

		HelpActivity activity = (HelpActivity) requireActivity();
		ActionBar actionBar = activity.getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle(R.string.shared_string_help);
		}
	}

	public void updateContent() {
		HelpActivity activity = (HelpActivity) requireActivity();
		HelpArticlesHelper articlesHelper = activity.getArticlesHelper();

		List<ContextMenuItem> items = articlesHelper.createItems();
		if (!Algorithms.isEmpty(items)) {
			items.get(items.size() - 1).setHideDivider(true);
			items.add(new ContextMenuItem(null).setLayout(R.layout.card_bottom_divider));
		}
		ContextMenuAdapter menuAdapter = new ContextMenuAdapter(app);
		for (ContextMenuItem item : items) {
			menuAdapter.addItem(item);
		}

		ViewCreator viewCreator = new ViewCreator(activity, nightMode);
		viewCreator.setDefaultLayoutId(R.layout.help_list_item);
		adapter = menuAdapter.toListAdapter(activity, viewCreator);

		listView.setAdapter(adapter);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, @NonNull MenuInflater inflater) {
		menu.clear();
		inflater.inflate(R.menu.help_menu, menu);
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
		} else if (id == R.id.action_menu) {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				showOptionsMenu(activity.findViewById(R.id.action_menu));
			}
		}
		return super.onOptionsItemSelected(item);
	}

	private void showOptionsMenu(@NonNull View view) {
		List<PopUpMenuItem> items = new ArrayList<>();

		HelpActivity activity = (HelpActivity) requireActivity();

		File exceptionLog = app.getAppPath(FeedbackHelper.EXCEPTION_PATH);
		if (exceptionLog.exists()) {
			items.add(new PopUpMenuItem.Builder(activity)
					.setTitleId(R.string.send_crash_log)
					.setIcon(getContentIcon(R.drawable.ic_action_bug_outlined_send))
					.setOnClickListener(v -> app.getFeedbackHelper().sendCrashLog()).create());
		}
		items.add(new PopUpMenuItem.Builder(activity)
				.setTitleId(R.string.send_logcat_log)
				.setIcon(getContentIcon(R.drawable.ic_action_file_report_outlined_send))
				.setOnClickListener(v -> activity.readAndSaveLogs()).create());

		items.add(new PopUpMenuItem.Builder(activity)
				.setTitleId(R.string.copy_build_version)
				.showTopDivider(true)
				.setIcon(getContentIcon(R.drawable.ic_action_osmand_logo))
				.setOnClickListener(v -> ShareMenu.copyToClipboardWithToast(activity,
						Version.getFullVersionWithReleaseDate(app), Toast.LENGTH_SHORT)).create());

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		displayData.layoutId = R.layout.simple_popup_menu_item;
		PopUpMenu.show(displayData);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ContextMenuItem item = adapter.getItem(position);
		ItemClickListener listener = item.getItemClickListener();
		if (listener != null) {
			listener.onContextMenuClick(adapter, view, item, false);
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		ItemLongClickListener listener = adapter.getItem(position).getItemLongClickListener();
		if (listener != null) {
			listener.onContextMenuLongClick(adapter, position, position, false, null);
			return true;
		}
		return false;
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			HelpMainFragment fragment = new HelpMainFragment();
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.commitAllowingStateLoss();
		}
	}
}