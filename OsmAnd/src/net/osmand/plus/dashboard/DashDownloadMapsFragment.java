package net.osmand.plus.dashboard;

import java.io.File;
import java.text.MessageFormat;
import java.util.Locale;

import net.osmand.IndexConstants;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.helpers.FontCache;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by dummy on 02.12.14.
 */
public class DashDownloadMapsFragment extends DashBaseFragment {

	public static final String TAG = "DASH_DOWNLOAD_MAPS_FRAGMENT";
	MessageFormat formatMb = new MessageFormat("{0, number,##.#} MB", Locale.US);
	MessageFormat formatGb = new MessageFormat("{0, number,#.##} GB", Locale.US);

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = getActivity().getLayoutInflater().inflate(R.layout.dash_download_maps_fragment, container,
				false);
		Typeface typeface = FontCache.getRobotoMedium(getActivity());
		final TextView message = ((TextView) view.findViewById(R.id.message));
		message.setTypeface(typeface);

		Button local = ((Button) view.findViewById(R.id.local_downloads));
		local.setTypeface(typeface);
		local.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final Intent intent = new Intent(view.getContext(), getMyApplication().getAppCustomization()
						.getDownloadIndexActivity());
				intent.putExtra(DownloadActivity.TAB_TO_OPEN, DownloadActivity.LOCAL_TAB);
				// intent.putExtra(DownloadActivity.SINGLE_TAB, true);
				getActivity().startActivity(intent);
			}
		});

		Button cancelBtn = ((Button) view.findViewById(R.id.download_new_map));
		cancelBtn.setTypeface(typeface);
		cancelBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final Intent intent = new Intent(view.getContext(), getMyApplication().getAppCustomization()
						.getDownloadIndexActivity());
				intent.putExtra(DownloadActivity.TAB_TO_OPEN, DownloadActivity.DOWNLOAD_TAB);
				// intent.putExtra(DownloadActivity.SINGLE_TAB, true);
				getActivity().startActivity(intent);
			}
		});
		return view;
	}

	@Override
	public void onOpenUpdate() {
		refreshData();
	}

	public void refreshData() {
		if (getView() == null) {
			return;
		}

		final TextView message = ((TextView) getView().findViewById(R.id.message));
		final Button local = ((Button) getView().findViewById(R.id.local_downloads));
		new AsyncTask<Void, Void, Void>() {
			int countMaps = 0;
			long size = 0;

			@Override
			protected Void doInBackground(Void... params) {
				updateCount(IndexConstants.MAPS_PATH);
				updateCount(IndexConstants.SRTM_INDEX_DIR);
				return null;
			}

			protected void updateCount(String s) {
				if (!DashDownloadMapsFragment.this.isAdded() || getMyApplication() == null) {
					return;
				}
				File ms = getMyApplication().getAppPath(s);
				if (ms.exists()) {
					File[] lf = ms.listFiles();
					if (lf != null) {
						for (File f : ms.listFiles()) {
							if (f.getName().endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)) {
								size += f.length();
								countMaps++;
							}
						}
					}
				}
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				if (!DashDownloadMapsFragment.this.isAdded() || getMyApplication() == null) {
					return;
				}
				if (countMaps > 0) {
					long mb = 1 << 20;
					long gb = 1 << 30;
					String sz = size > gb ? formatGb.format(new Object[] { (float) size / (gb) }) : formatMb
							.format(new Object[] { (float) size / mb });
					message.setText(getString(R.string.dash_download_msg, countMaps + "") + " (" + sz + ")");
					local.setVisibility(View.VISIBLE);
				} else {
					message.setText(getString(R.string.dash_download_msg_none));
					local.setVisibility(View.GONE);
				}

			}
		}.execute((Void) null);
	}
}
