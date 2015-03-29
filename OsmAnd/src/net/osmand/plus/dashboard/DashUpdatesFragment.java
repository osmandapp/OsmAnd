package net.osmand.plus.dashboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.osmand.plus.R;
import net.osmand.plus.base.BasicProgressAsyncTask;
import net.osmand.plus.download.BaseDownloadActivity;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.DatabaseHelper;
import net.osmand.plus.helpers.FontCache;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by Denis on 21.11.2014.
 */
public class DashUpdatesFragment extends DashBaseFragment {

	public static final String TAG = "DASH_UPDATES_FRAGMENT";

	private ProgressBar currentProgress;
	private List<ProgressBar> progressBars = new ArrayList<ProgressBar>();
	private List<String> baseNames = new ArrayList<String>();
	private List<ImageButton> downloadButtons = new ArrayList<ImageButton>();
	private List<IndexItem> downloadQueue = new ArrayList<IndexItem>();
	private ImageButton cancelButton;

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_updates_fragment, container, false);
		Typeface typeface = FontCache.getRobotoMedium(getActivity());
		((TextView) view.findViewById(R.id.header)).setTypeface(typeface);
		Button showAll = (Button) view.findViewById(R.id.show_all);
		showAll.setTypeface(typeface);
		showAll.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final Intent intent = new Intent(view.getContext(), getMyApplication().getAppCustomization()
						.getDownloadIndexActivity());
				intent.putExtra(DownloadActivity.TAB_TO_OPEN, DownloadActivity.UPDATES_TAB);
				// intent.putExtra(DownloadActivity.SINGLE_TAB, true);
				getActivity().startActivity(intent);
			}
		});
		return view;
	}

	@Override
	public void onOpenDash() {
		downloadQueue.clear();
		if (BaseDownloadActivity.downloadListIndexThread != null) {
			currentProgress = null;
			cancelButton = null;
		}
		updatedDownloadsList(BaseDownloadActivity.downloadListIndexThread.getItemsToUpdate());

	}

	public void updatedDownloadsList(List<IndexItem> list) {
		List<IndexItem> itemList = new ArrayList<IndexItem>(list);
		Collections.sort(itemList, new Comparator<IndexItem>() {
			@Override
			public int compare(IndexItem indexItem, IndexItem t1) {
				DatabaseHelper helper = BaseDownloadActivity.downloadListIndexThread.getDbHelper();
				return (int) (helper.getCount(t1.getBasename(), DatabaseHelper.DOWNLOAD_ENTRY) - helper.getCount(
						indexItem.getBasename(), DatabaseHelper.DOWNLOAD_ENTRY));
			}
		});
		View mainView = getView();
		// it may be null because download index thread is async
		if (mainView == null) {
			return;
		}
		progressBars.clear();
		baseNames.clear();
		downloadButtons.clear();
		((TextView) mainView.findViewById(R.id.header)).setText(getString(R.string.map_update,
				String.valueOf(list.size())));

		LinearLayout updates = (LinearLayout) mainView.findViewById(R.id.updates_items);
		updates.removeAllViews();

		if (itemList.size() < 1) {
			mainView.findViewById(R.id.maps).setVisibility(View.GONE);
			return;
		} else {
			mainView.findViewById(R.id.maps).setVisibility(View.VISIBLE);
		}

		for (int i = 0; i < itemList.size(); i++) {
			final IndexItem item = itemList.get(i);
			if (i > 2) {
				break;
			}
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View view = inflater.inflate(R.layout.dash_updates_item, null, false);
			String name = item.getVisibleName(getMyApplication(), getMyApplication().getResourceManager()
					.getOsmandRegions());
			String d = item.getDate(getMyApplication().getResourceManager().getDateFormat()) + ", "
					+ item.getSizeDescription(getMyApplication());
			String eName = name.replace("\n", " ");
			((TextView) view.findViewById(R.id.map_name)).setText(eName);
			((TextView) view.findViewById(R.id.map_descr)).setText(d);
			final ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.ProgressBar);
			View downloadButton = (view.findViewById(R.id.btn_download));
			downloadButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (getDownloadActivity().isInQueue(item)) {
						getDownloadActivity().removeFromQueue(item);
						((ImageView) view).setImageDrawable(
								getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_action_import));
					} else if (!getDownloadActivity().startDownload(item)) {
						((ImageView) view).setImageDrawable(
								getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_action_remove_dark));
					}
				}
			});
			downloadButtons.add((ImageButton) downloadButton);
			baseNames.add(item.getBasename());
			progressBars.add(progressBar);
			updates.addView(view);
		}
		updateProgress(BaseDownloadActivity.downloadListIndexThread.getCurrentRunningTask(), false);
	}

	private BaseDownloadActivity getDownloadActivity() {
		return (BaseDownloadActivity) getActivity();
	}

	public void updateProgress(BasicProgressAsyncTask<?, ?, ?> basicProgressAsyncTask, boolean updateOnlyProgress) {
		if (basicProgressAsyncTask == null) {
			return;
		}
		// needed when rotation is performed and progress can be null
		if (!updateOnlyProgress) {
			getProgressIfPossible(basicProgressAsyncTask.getDescription());
		}

		if (currentProgress == null) {
			return;
		}

		if (updateOnlyProgress) {
			if (!basicProgressAsyncTask.isIndeterminate()) {
				currentProgress.setProgress(basicProgressAsyncTask.getProgressPercentage());
			}
		} else {
			boolean visible = basicProgressAsyncTask.getStatus() != AsyncTask.Status.FINISHED;
			if (!visible) {
				return;
			}
			cancelButton.setImageDrawable(getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_action_remove_dark));
			View view = (View) cancelButton.getParent();
			if (view != null && view.findViewById(R.id.map_descr) != null) {
				view.findViewById(R.id.map_descr).setVisibility(View.GONE);
			}
			cancelButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					getDownloadActivity().cancelDownload();
				}
			});
			boolean intermediate = basicProgressAsyncTask.isIndeterminate();
			currentProgress.setVisibility(intermediate ? View.GONE : View.VISIBLE);
			if (!intermediate) {
				currentProgress.setProgress(basicProgressAsyncTask.getProgressPercentage());
			}
		}
	}

	private void getProgressIfPossible(String message) {
		if (getActivity() == null) {
			return;
		}
		for (int i = 0; i < baseNames.size(); i++) {
			if (message.equals(getActivity().getString(R.string.shared_string_downloading) + " " + baseNames.get(i))) {
				currentProgress = progressBars.get(i);
				cancelButton = downloadButtons.get(i);
				currentProgress.setVisibility(View.VISIBLE);
				return;
			}
		}
	}
}
