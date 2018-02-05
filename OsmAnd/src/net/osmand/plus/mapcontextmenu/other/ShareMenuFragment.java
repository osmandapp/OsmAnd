package net.osmand.plus.mapcontextmenu.other;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.mapcontextmenu.other.ShareMenu.ShareItem;
import net.osmand.plus.widgets.TextViewEx;

import java.util.List;


public class ShareMenuFragment extends MenuBottomSheetDialogFragment implements OnItemClickListener {
	public static final String TAG = "ShareMenuFragment";

	private ArrayAdapter<ShareItem> listAdapter;
	private ShareMenu menu;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null && getActivity() instanceof MapActivity) {
			menu = ShareMenu.restoreMenu(savedInstanceState, (MapActivity) getActivity());
		}
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		final View view = View.inflate(new ContextThemeWrapper(getContext(), themeRes),
				R.layout.share_menu_fragment, container);

		if (nightMode) {
			((TextViewEx) view.findViewById(R.id.title_text_view)).setTextColor(getResources().getColor(R.color.ctx_menu_info_text_dark));
		}

		ListView listView = (ListView) view.findViewById(R.id.list);
		listAdapter = createAdapter();
		listView.setAdapter(listAdapter);
		listView.setOnItemClickListener(this);

		view.findViewById(R.id.cancel_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		setupHeightAndBackground(view, R.id.scroll_view);

		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		menu.saveMenu(outState);
	}

	public static void showInstance(ShareMenu menu) {
		ShareMenuFragment fragment = new ShareMenuFragment();
		fragment.menu = menu;
		fragment.setUsedOnMap(true);
		fragment.show(menu.getMapActivity().getSupportFragmentManager(), ShareMenuFragment.TAG);
	}

	private ArrayAdapter<ShareItem> createAdapter() {
		final List<ShareItem> items = menu.getItems();
		return new ArrayAdapter<ShareItem>(menu.getMapActivity(), R.layout.share_list_item, items) {

			@SuppressLint("InflateParams")
			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				View v = convertView;
				if (v == null) {
					v = menu.getMapActivity().getLayoutInflater().inflate(R.layout.share_list_item, null);
				}
				AndroidUtils.setBackground(v.getContext(), v, nightMode, R.drawable.expandable_list_item_background_light, R.drawable.expandable_list_item_background_dark);
				final ShareItem item = getItem(position);
				ImageView icon = (ImageView) v.findViewById(R.id.icon);
				icon.setImageDrawable(menu.getMapActivity().getMyApplication()
						.getIconsCache().getIcon(item.getIconResourceId(), !nightMode));
				TextView name = (TextView) v.findViewById(R.id.name);
				AndroidUtils.setTextPrimaryColor(v.getContext(), name, nightMode);
				name.setText(getContext().getText(item.getTitleResourceId()));
				return v;
			}
		};
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		dismiss();
		menu.share(listAdapter.getItem(position));
	}

	public void dismissMenu() {
		dismiss();
		menu.getMapActivity().getContextMenu().close();
	}
}
