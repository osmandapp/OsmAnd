package net.osmand.plus.download.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseAlertDialogFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.utils.AndroidUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ActiveDownloadsDialogFragment extends BaseAlertDialogFragment implements DownloadEvents {

	private static final String TAG = ActiveDownloadsDialogFragment.class.getSimpleName();

	private IndexItemAdapter adapter;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		updateNightMode();
		AlertDialog.Builder builder = createDialogBuilder();
		builder.setTitle(R.string.downloads).setNegativeButton(R.string.shared_string_close, null);
		adapter = new IndexItemAdapter(this, (DownloadActivity) requireActivity());
		builder.setAdapter(adapter, null);
		return builder.create();
	}

	public void onUpdatedIndexesList() {
		adapter.refreshAllData();
	}

	@Override
	public void downloadHasFinished() {
		adapter.refreshAllData();
	}

	public void downloadInProgress() {
		adapter.notifyDataSetChanged();
	}

	public static void showInstance(@NonNull FragmentActivity activity) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			new ActiveDownloadsDialogFragment().show(fragmentManager, TAG);
		}
	}

	private class IndexItemAdapter extends ArrayAdapter<IndexItem> {
		private final DownloadActivity context;
		private final DialogFragment dlgFragment;

		public IndexItemAdapter(@NonNull DialogFragment dlgFragment, @NonNull DownloadActivity context) {
			super(context, R.layout.two_line_with_images_list_item, new ArrayList<>());
			this.dlgFragment = dlgFragment;
			this.context = context;
			refreshAllData();
		}

		public void refreshAllData() {
			clear();
			List<IndexItem> items = context.getDownloadThread().getCurrentDownloadingItems();
			if (items.isEmpty()) {
				dlgFragment.dismissAllowingStateLoss();
			}
			for (IndexItem item : context.getDownloadThread().getCurrentDownloadingItems()) {
				add(item);
			}
		}

		@Override
		@NonNull
		public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
			if (convertView == null) {
				convertView = inflate(R.layout.two_line_with_images_list_item, parent, false);
				ItemViewHolder viewHolder = new ItemViewHolder(convertView, context);
				viewHolder.setSilentCancelDownload(true);
				viewHolder.setShowTypeInDesc(true);
				viewHolder.setShowProgressInDescr(true);
				viewHolder.setShowParentRegionName(true);
				viewHolder.setUseShortName(false);
				convertView.setTag(viewHolder);
			}
			ItemViewHolder viewHolder = (ItemViewHolder) convertView.getTag();
			IndexItem item = getItem(position);
			viewHolder.bindDownloadItem(Objects.requireNonNull(item));
			return convertView;
		}
	}
}
