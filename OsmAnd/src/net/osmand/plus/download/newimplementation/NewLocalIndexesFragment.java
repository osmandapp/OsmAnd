package net.osmand.plus.download.newimplementation;

import android.content.Context;
import android.os.Bundle;
import android.os.StatFs;
import android.support.v4.view.MenuItemCompat;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmAndListFragment;
import net.osmand.plus.download.BaseDownloadActivity;
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

		getDownloadActivity().setSupportProgressBarIndeterminateVisibility(false);

		ProgressBar sizeProgress = (ProgressBar) view.findViewById(R.id.memory_progress);
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

		TextView descriptionText = (TextView) view.findViewById(R.id.memory_size);
		descriptionText.setText(Html.fromHtml(text));
		descriptionText.setMovementMethod(LinkMovementMethod.getInstance());

		TextView downloadsLeftTextView = (TextView) view.findViewById(R.id.downloadsLeftTextView);
		downloadsLeftTextView.setText(getString(R.string.downloads_left_template,
				BaseDownloadActivity.MAXIMUM_AVAILABLE_FREE_DOWNLOADS
						- getMyApplication().getSettings().NUMBER_OF_FREE_DOWNLOADS.get()));
		TextView freeVersionDescriptionTextView =
				(TextView) view.findViewById(R.id.freeVersionDescriptionTextView);
		freeVersionDescriptionTextView.setText(getString(R.string.free_version_message,
				BaseDownloadActivity.MAXIMUM_AVAILABLE_FREE_DOWNLOADS));


		ListView listView = (ListView) view.findViewById(android.R.id.list);
		mAdapter = new CategoriesAdapter(getActivity());
		listView.setAdapter(mAdapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				LOG.debug("onItemClick()");

			}
		});
//		listAdapter = new LocalIndexesAdapter(getActivity());
//		listView.setAdapter(listAdapter);
//		setListView(listView);
		return view;
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
			DownloadActivity.downloadListIndexThread.runReloadIndexFiles(true);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		MapsInCategoryFragment.createInstance(mAdapter.getItem(position))
				.show(getChildFragmentManager(), MapsInCategoryFragment.TAG);
		LOG.debug("onListItemClick()");
		super.onListItemClick(l, v, position, id);
	}

	private DownloadActivity getDownloadActivity() {
		return (DownloadActivity) getActivity();
	}

	public void onCategorizationFinished(List<IndexItem> filtered, List<IndexItemCategoryWithSubcat> cats) {
		LOG.debug("cats=" + cats);
		mAdapter.clear();
		mAdapter.addAll(cats);
	}

	private static class CategoriesAdapter extends ArrayAdapter<IndexItemCategoryWithSubcat> {
		public CategoriesAdapter(Context context) {
			super(context, R.layout.simple_list_menu_item);
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
			viewHolder.textView.setText(getItem(position).categoryStaticData.getName());
			return convertView;
		}

		private static class ViewHolder {
			TextView textView;
		}
	}
}
