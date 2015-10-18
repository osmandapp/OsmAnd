package net.osmand.plus.download.items;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import net.osmand.plus.R;
import net.osmand.plus.download.BaseDownloadActivity;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.IndexItem;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

public class ActiveDownloadsDialogFragment extends DialogFragment {

	private IndexItemAdapter adapter;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.downloads).setNegativeButton(R.string.shared_string_dismiss, null);
		adapter = new IndexItemAdapter(getDownloadActivity());
		builder.setAdapter(adapter, null);
		getDownloadActivity().setActiveDownloads(this);
		return builder.create();
	}
	
	public void refresh() {
		adapter.updateData();
	}
	
	public void onDetach() {
		super.onDetach();
		getDownloadActivity().setActiveDownloads(null);
	};
	
	
	DownloadActivity getDownloadActivity() {
		return (DownloadActivity) getActivity();
	}

	public static class IndexItemAdapter extends ArrayAdapter<IndexItem> {
		private final Drawable deleteDrawable;
		private final DownloadActivity context;
		private int itemInProgressPosition = -1;
		private int progress = -1;
		private final Set<Integer> downloadedItems = new HashSet<>();
		private boolean isFinished;

		public IndexItemAdapter(DownloadActivity context) {
			super(context, R.layout.two_line_with_images_list_item, new ArrayList<IndexItem>());
			this.context = context;
			deleteDrawable = context.getMyApplication().getIconsCache()
					.getPaintedContentIcon(R.drawable.ic_action_remove_dark,
							context.getResources().getColor(R.color.dash_search_icon_dark));
			updateData();
		}

		public void updateData() {
			clear();
			addAll(BaseDownloadActivity.downloadListIndexThread.getCurrentDownloadingItems());
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = LayoutInflater.from(parent.getContext())
						.inflate(R.layout.two_line_with_images_list_item, parent, false);
				DownloadEntryViewHolder viewHolder =
						new DownloadEntryViewHolder(convertView, context, deleteDrawable, this);
				convertView.setTag(viewHolder);
			}
			DownloadEntryViewHolder viewHolder = (DownloadEntryViewHolder) convertView.getTag();
			int localProgress = itemInProgressPosition == position ? progress : -1;
			viewHolder.bindDownloadEntry(getItem(position), localProgress,
					isFinished || downloadedItems.contains(position));
			return convertView;
		}
		
	}

	private static class DownloadEntryViewHolder extends TwoLineWithImagesViewHolder {
		private final Drawable deleteDrawable;
		private final IndexItemAdapter adapter;

		private DownloadEntryViewHolder(View convertView, final DownloadActivity context,
										Drawable deleteDrawable, IndexItemAdapter adapter) {
			super(convertView, context);
			this.deleteDrawable = deleteDrawable;
			this.adapter = adapter;
			progressBar.setVisibility(View.VISIBLE);
			rightImageButton.setImageDrawable(deleteDrawable);
		}

		public void bindDownloadEntry(final IndexItem item, final int progress,
									  boolean isDownloaded) {
			nameTextView.setText(item.getVisibleName(context,
					context.getMyApplication().getRegions()));
			rightImageButton.setVisibility(View.VISIBLE);

			int localProgress = progress;
			boolean isIndeterminate = true;
			if (progress != -1) {
				isIndeterminate = false;
				double downloaded = item.getContentSizeMB()  * progress / 100;
				descrTextView.setText(context.getString(R.string.value_downloaded_from_max, downloaded,
						item.getContentSizeMB()));
			} else if (isDownloaded) {
				isIndeterminate = false;
				localProgress = progressBar.getMax();
				descrTextView.setText(context.getString(R.string.file_size_in_mb,
						item.getContentSizeMB()));
				rightImageButton.setVisibility(View.GONE);
			} else {
				descrTextView.setText(context.getString(R.string.file_size_in_mb,
						item.getContentSizeMB()));
			}
			rightImageButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					context.cancelDownload(item);
					adapter.updateData();
				}
			});
			progressBar.setIndeterminate(isIndeterminate);
			progressBar.setProgress(localProgress);

		}

	}
}