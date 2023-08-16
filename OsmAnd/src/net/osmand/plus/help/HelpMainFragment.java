package net.osmand.plus.help;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
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
import net.osmand.plus.widgets.ctxmenu.callback.ItemLongClickListener;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import java.util.List;

public class HelpMainFragment extends BaseOsmAndFragment implements OnItemClickListener, OnItemLongClickListener {

	public static final String TAG = HelpMainFragment.class.getSimpleName();

	private HelpArticlesHelper articlesHelper;
	private ContextMenuListAdapter adapter;
	private ListView listView;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		HelpActivity activity = (HelpActivity) requireActivity();
		articlesHelper = activity.getArticlesHelper();
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


	public void updateContent() {
		ContextMenuAdapter menuAdapter = new ContextMenuAdapter(app);
		List<ContextMenuItem> items = articlesHelper.createItems();
		for (ContextMenuItem item : items) {
			menuAdapter.addItem(item);
		}

		FragmentActivity activity = requireActivity();
		ViewCreator viewCreator = new ViewCreator(activity, nightMode);
		viewCreator.setDefaultLayoutId(R.layout.help_list_item);
		adapter = menuAdapter.toListAdapter(activity, viewCreator);

		listView.setAdapter(adapter);
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
			fragment.setRetainInstance(true);
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.commitAllowingStateLoss();
		}
	}
}