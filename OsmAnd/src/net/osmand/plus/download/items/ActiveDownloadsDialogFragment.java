package net.osmand.plus.download.items;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.base.BasicProgressAsyncTask;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadEntry;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ActiveDownloadsDialogFragment extends DialogFragment {
	private final static Log LOG = PlatformUtil.getLog(ActiveDownloadsDialogFragment.class);

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.downloads).setNegativeButton(R.string.shared_string_cancel, null);
		Collection<List<DownloadEntry>> vs =
				DownloadActivity.downloadListIndexThread.getEntriesToDownload().values();
		ArrayList<DownloadEntry> downloadEntries = new ArrayList<>();
		for (List<DownloadEntry> list : vs) {
			downloadEntries.addAll(list);
		}
		final DownloadEntryAdapter adapter = new DownloadEntryAdapter(
				(DownloadActivity) getActivity(), downloadEntries);
		builder.setAdapter(adapter, null);
		((DownloadActivity) getActivity()).registerUpdateListener(adapter);
		return builder.create();
	}

	public static class DownloadEntryAdapter extends ArrayAdapter<DownloadEntry> {
		private final Drawable deleteDrawable;
		private final DownloadActivity context;
		private int itemInProgressPosition = -1;
		private int progress = -1;
		private final Set<Integer> downloadedItems = new HashSet<>();
		private boolean isFinished;

		public DownloadEntryAdapter(DownloadActivity context, List<DownloadEntry> objects) {
			super(context, R.layout.two_line_with_images_list_item, objects);
			this.context = context;
			deleteDrawable = context.getMyApplication().getIconsCache()
					.getPaintedContentIcon(R.drawable.ic_action_remove_dark,
							context.getResources().getColor(R.color.dash_search_icon_dark));
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = LayoutInflater.from(parent.getContext())
						.inflate(R.layout.two_line_with_images_list_item, parent, false);
				DownloadEntryViewHolder viewHolder =
						new DownloadEntryViewHolder(convertView, deleteDrawable, context, this);
				convertView.setTag(viewHolder);
			}
			DownloadEntryViewHolder viewHolder = (DownloadEntryViewHolder) convertView.getTag();
			int localProgress = itemInProgressPosition == position ? progress : -1;
			viewHolder.bindDownloadEntry(getItem(position), localProgress,
					isFinished || downloadedItems.contains(position));
			return convertView;
		}

		public void setProgress(BasicProgressAsyncTask<?, ?, ?> task, Object tag) {
			isFinished = task == null
					|| task.getStatus() == AsyncTask.Status.FINISHED;
			itemInProgressPosition = -1;
			progress = -1;
			if (isFinished) return;
			if (tag instanceof DownloadEntry) {
				progress = task.getProgressPercentage();
				for (int i = 0; i < getCount(); i++) {
					if (getItem(i).equals(tag)) {
						itemInProgressPosition = i;
						downloadedItems.add(i);
					}
				}
			}
			notifyDataSetChanged();
		}
	}

	private static class DownloadEntryViewHolder {
		public final View.OnClickListener activeDownloadOnClickListener;
		private final TextView nameTextView;
		private final TextView descrTextView;
		private final ImageView leftImageView;
		private final ImageView rightImageButton;
		private final Button rightButton;
		private final ProgressBar progressBar;
		private final TextView mapDateTextView;
		private final Drawable deleteDrawable;
		private final DownloadActivity context;
		private final DownloadEntryAdapter adapter;

		private DownloadEntryViewHolder(View convertView, Drawable deleteDrawable,
										final DownloadActivity context,
										DownloadEntryAdapter adapter) {
			this.deleteDrawable = deleteDrawable;
			this.context = context;
			this.adapter = adapter;
			nameTextView = (TextView) convertView.findViewById(R.id.name);
			descrTextView = (TextView) convertView.findViewById(R.id.description);
			leftImageView = (ImageView) convertView.findViewById(R.id.leftImageView);
			rightImageButton = (ImageView) convertView.findViewById(R.id.rightImageButton);
			rightButton = (Button) convertView.findViewById(R.id.rightButton);
			progressBar = (ProgressBar) convertView.findViewById(R.id.progressBar);
			mapDateTextView = (TextView) convertView.findViewById(R.id.mapDateTextView);
			progressBar.setVisibility(View.VISIBLE);
			rightImageButton.setImageDrawable(deleteDrawable);

			activeDownloadOnClickListener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					context.makeSureUserCancelDownload();
				}
			};
		}

		public void bindDownloadEntry(final DownloadEntry downloadEntry, final int progress,
									  boolean isDownloaded) {
			nameTextView.setText(downloadEntry.baseName);
			rightImageButton.setVisibility(View.VISIBLE);

			int localProgress = progress;
			boolean isIndeterminate = true;
			View.OnClickListener onClickListener = null;
			if (progress != -1) {
				// downloading
				isIndeterminate = false;
				onClickListener = activeDownloadOnClickListener;
				double downloaded = downloadEntry.sizeMB * progress / 100;
				descrTextView.setText(String.format("%.1f from %.1f MB", downloaded,
						downloadEntry.sizeMB));
			} else if (isDownloaded) {
				// Downloaded
				isIndeterminate = false;
				localProgress = progressBar.getMax();
				descrTextView.setText(String.format("%.1f MB", downloadEntry.sizeMB));

			} else {
				// pending
				onClickListener = new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						context.getEntriesToDownload().remove(downloadEntry.item);
						adapter.remove(downloadEntry);
					}
				};
				descrTextView.setText(String.format("%.1f MB", downloadEntry.sizeMB));
			}
			rightImageButton.setOnClickListener(onClickListener);
			progressBar.setIndeterminate(isIndeterminate);
			progressBar.setProgress(localProgress);

		}

	}
}