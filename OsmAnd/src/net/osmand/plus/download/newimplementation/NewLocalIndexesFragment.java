package net.osmand.plus.download.newimplementation;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.StatFs;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmAndListFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.IndexItem;

import org.apache.commons.logging.Log;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;


public class NewLocalIndexesFragment extends OsmAndListFragment {
	private static final Log LOG = PlatformUtil.getLog(NewLocalIndexesFragment.class);
	private static final MessageFormat formatGb = new MessageFormat("{0, number,<b>#.##</b>} GB", Locale.US);

	public static final int RELOAD_ID = 0;
	private CategoriesAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.local_index_fragment, container, false);

		ListView listView = (ListView) view.findViewById(android.R.id.list);
		mAdapter = new CategoriesAdapter(getActivity(), getMyApplication());
		listView.setAdapter(mAdapter);

		View header = inflater.inflate(R.layout.local_index_fragment_header, listView, false);
		initMemoryConsumedCard(header);
		DownloadsUiInitHelper.initFreeVersionBanner(header, getMyApplication().getSettings(),
				getResources());
		listView.addHeaderView(header);

		getDownloadActivity().setSupportProgressBarIndeterminateVisibility(false);
		return view;
	}

	private void initMemoryConsumedCard(View header) {
		ProgressBar sizeProgress = (ProgressBar) header.findViewById(R.id.memory_progress);
		File dir = getMyApplication().getAppPath("").getParentFile();
		String size = formatGb.format(new Object[]{0});
		int percent = 0;
		if (dir.canRead()) {
			StatFs statFs = new StatFs(dir.getAbsolutePath());
			//noinspection deprecation
			size = formatGb.format(new Object[]{(float) (statFs.getAvailableBlocks()) * statFs.getBlockSize() / (1 << 30)});
			//noinspection deprecation
			percent = statFs.getAvailableBlocks() * 100 / statFs.getBlockCount();
		}
		sizeProgress.setProgress(percent);
		String text = getString(R.string.free, size);

		TextView descriptionText = (TextView) header.findViewById(R.id.memory_size);
		descriptionText.setText(Html.fromHtml(text));
		descriptionText.setMovementMethod(LinkMovementMethod.getInstance());
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		MenuItem item = menu.add(0, RELOAD_ID, 0, R.string.shared_string_refresh);
		item.setIcon(R.drawable.ic_action_refresh_dark);
		MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == RELOAD_ID) {
			// re-create the thread
			DownloadActivity.downloadListIndexThread.runReloadIndexFiles();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
		fragmentTransaction.addToBackStack(null);
		MapsInCategoryFragment.createInstance(mAdapter.getItem(position-1))
				.show(fragmentTransaction, MapsInCategoryFragment.TAG);
	}

	private DownloadActivity getDownloadActivity() {
		return (DownloadActivity) getActivity();
	}

	public void onCategorizationFinished(List<IndexItem> filtered, List<IndexItemCategoryWithSubcat> cats) {
		mAdapter.clear();
		mAdapter.addAll(cats);
	}

	private static class CategoriesAdapter extends ArrayAdapter<IndexItemCategoryWithSubcat> {
		private final OsmandApplication osmandApplication;

		public CategoriesAdapter(Context context, OsmandApplication osmandApplication) {
			super(context, R.layout.simple_list_menu_item);
			this.osmandApplication = osmandApplication;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder viewHolder;
			if (convertView == null) {
				convertView = LayoutInflater.from(parent.getContext())
						.inflate(R.layout.simple_list_menu_item, parent, false);
				viewHolder = new ViewHolder();
				viewHolder.textView = (TextView) convertView.findViewById(R.id.title);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) convertView.getTag();
			}
			Drawable iconLeft = osmandApplication.getIconsCache()
					.getContentIcon(R.drawable.ic_world_globe_dark);
			viewHolder.textView.setCompoundDrawablesWithIntrinsicBounds(iconLeft, null, null, null);
			viewHolder.textView.setText(getItem(position).categoryStaticData.getName());
			return convertView;
		}

		private static class ViewHolder {
			TextView textView;
		}
	}
}
