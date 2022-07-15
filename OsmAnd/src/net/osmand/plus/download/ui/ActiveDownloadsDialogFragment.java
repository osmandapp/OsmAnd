package net.osmand.plus.download.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.IndexItem;

import java.util.ArrayList;
import java.util.List;

public class ActiveDownloadsDialogFragment extends DialogFragment implements DownloadEvents {

	private IndexItemAdapter adapter;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.downloads).setNegativeButton(R.string.shared_string_close, null);
		adapter = new IndexItemAdapter(this, getDownloadActivity());
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

	DownloadActivity getDownloadActivity() {
		return (DownloadActivity) getActivity();
	}

	public static class IndexItemAdapter extends ArrayAdapter<IndexItem> {
		private final DownloadActivity context;
		private final DialogFragment dlgFragment;

		public IndexItemAdapter(DialogFragment dlgFragment, DownloadActivity context) {
			super(context, R.layout.two_line_with_images_list_item, new ArrayList<IndexItem>());
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
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = LayoutInflater.from(parent.getContext())
						.inflate(R.layout.two_line_with_images_list_item, parent, false);
				ItemViewHolder viewHolder =
						new ItemViewHolder(convertView, context);
				viewHolder.setSilentCancelDownload(true);
				viewHolder.setShowTypeInDesc(true);
				viewHolder.setShowProgressInDescr(true);
				convertView.setTag(viewHolder);
			}
			ItemViewHolder viewHolder = (ItemViewHolder) convertView.getTag();
			IndexItem item = getItem(position);
			viewHolder.bindDownloadItem(item);
			return convertView;
		}
	}
}
