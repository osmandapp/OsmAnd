package net.osmand.plus.mapcontextmenu.other;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import net.osmand.plus.mapcontextmenu.other.ShareMenu.ShareItem;

import java.util.List;


public class ShareMenuFragment extends Fragment implements OnItemClickListener {
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
		View view = inflater.inflate(R.layout.share_menu_fragment, container, false);

		View mainView = view.findViewById(R.id.main_view);
		if (menu.isLandscapeLayout()) {
			AndroidUtils.setBackground(view.getContext(), mainView, !menu.isLight(),
					R.drawable.bg_left_menu_light, R.drawable.bg_left_menu_dark);
		} else {
			AndroidUtils.setBackground(view.getContext(), mainView, !menu.isLight(),
					R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);
		}

		TextView headerCaption = (TextView) view.findViewById(R.id.header_caption);
		AndroidUtils.setTextSecondaryColor(view.getContext(), headerCaption, !menu.isLight());

		ListView listView = (ListView) view.findViewById(R.id.list);
		listAdapter = createAdapter();
		listView.setAdapter(listAdapter);
		listView.setOnItemClickListener(this);

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
		menu.getMapActivity().getContextMenu().setBaseFragmentVisibility(false);
	}

	@Override
	public void onStop() {
		super.onStop();
		menu.getMapActivity().getContextMenu().setBaseFragmentVisibility(true);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		menu.saveMenu(outState);
	}

	public static void showInstance(ShareMenu menu) {
		int slideInAnim = menu.getSlideInAnimation();
		int slideOutAnim = menu.getSlideOutAnimation();

		ShareMenuFragment fragment = new ShareMenuFragment();
		fragment.menu = menu;
		menu.getMapActivity().getSupportFragmentManager().beginTransaction()
				.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
				.add(R.id.fragmentContainer, fragment, TAG)
				.addToBackStack(TAG).commit();
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
				AndroidUtils.setBackground(v.getContext(), v, !menu.isLight(), R.drawable.expandable_list_item_background_light, R.drawable.expandable_list_item_background_dark);
				final ShareItem item = getItem(position);
				ImageView icon = (ImageView) v.findViewById(R.id.icon);
				icon.setImageDrawable(menu.getMapActivity().getMyApplication()
						.getIconsCache().getContentIcon(item.getIconResourceId(), menu.isLight()));
				TextView name = (TextView) v.findViewById(R.id.name);
				AndroidUtils.setTextPrimaryColor(v.getContext(), name, !menu.isLight());
				name.setText(getContext().getText(item.getTitleResourceId()));
				return v;
			}
		};
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		menu.share(listAdapter.getItem(position));
		dismissMenu();
	}

	public void dismissMenu() {
		if (menu.getMapActivity().getContextMenu().isVisible()) {
			menu.getMapActivity().getContextMenu().hide();
		} else {
			menu.getMapActivity().getSupportFragmentManager().popBackStack();
		}
	}
}
