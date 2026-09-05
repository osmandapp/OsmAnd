package net.osmand.plus.plugins.rastermaps;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.rastermaps.DownloadTilesHelper.DownloadType;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.CompoundButtonType;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

class SelectTilesDownloadTypeAlertDialog {

	private final OsmandApplication app;
	private final LayoutInflater layoutInflater;
	private final boolean nightMode;
	private final int selectedItem;

	private AlertDialog dialog;
	private final DownloadTypeSelectionListener listener;

	private List<Pair<String, String>> items;

	public SelectTilesDownloadTypeAlertDialog(@NonNull OsmandApplication app,
	                                          boolean nightMode,
	                                          boolean allSelected,
	                                          @NonNull DownloadTypeSelectionListener listener) {
		this.app = app;
		this.layoutInflater = UiUtilities.getInflater(app, nightMode);
		this.nightMode = nightMode;
		this.selectedItem = allSelected ? 0 : 1;
		this.listener = listener;
	}

	public void show(@NonNull Activity activity) {
		Context themedContext = UiUtilities.getThemedContext(activity, nightMode);
		dialog = new AlertDialog.Builder(themedContext)
				.setTitle(R.string.download_tiles)
				.setAdapter(new Adapter(), null)
				.show();
	}

	public void updateData(long totalTilesCount, long missingTilesCount, boolean approximate) {
		items = new ArrayList<>();
		items.add(Pair.create(app.getString(R.string.shared_string_all), formatNumber(totalTilesCount)));
		String missingTiles;
		if (missingTilesCount == -1) {
			missingTiles = "—";
		} else {
			String formattedMissing = formatNumber(missingTilesCount);
			missingTiles = approximate
					? MessageFormat.format("~{0}", formattedMissing)
					: formattedMissing;
		}
		items.add(Pair.create(app.getString(R.string.shared_string_only_missing), missingTiles));
	}

	@NonNull
	private String formatNumber(long number) {
		return OsmAndFormatter.formatValue(number, "", false, 0, app).value;
	}

	private class Adapter extends BaseAdapter {

		@Override
		public int getCount() {
			return items.size();
		}

		@Override
		public Object getItem(int position) {
			return items.get(position);
		}

		@Override
		public long getItemId(int position) {
			return items.hashCode();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView == null
					? layoutInflater.inflate(R.layout.download_type_item, parent, false)
					: convertView;

			CompoundButton compoundButton = view.findViewById(R.id.compound_button);
			TextView tvTitle = view.findViewById(R.id.title);
			TextView tvDesc = view.findViewById(R.id.desc);

			UiUtilities.setupCompoundButton(compoundButton, nightMode, CompoundButtonType.GLOBAL);
			compoundButton.setChecked(position == selectedItem);
			tvTitle.setText(items.get(position).first);
			tvDesc.setText(items.get(position).second);

			DownloadType downloadType = position == 0 ? DownloadType.FORCE_ALL : DownloadType.ONLY_MISSING;
			view.setOnClickListener(v -> {
				listener.onDownloadTypeSelected(downloadType);
				dialog.dismiss();
			});

			return view;
		}
	}

	interface DownloadTypeSelectionListener {

		void onDownloadTypeSelected(@NonNull DownloadType downloadType);
	}
}