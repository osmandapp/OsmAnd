package net.osmand.plus.osmedit;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.osmedit.OsmEditsFragment.ExportTypesDef;
import net.osmand.plus.widgets.TextViewEx;

public class ExportOptionsBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = "ExportOptionsBottomSheetDialogFragment";
	public static final String POI_COUNT_KEY = "poi_count";
	public static final String NOTES_COUNT_KEY = "notes_count";

	private ExportOptionsFragmentListener listener;

	private int poiCount;
	private int osmNotesCount;

	public void setListener(ExportOptionsFragmentListener listener) {
		this.listener = listener;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes),
				R.layout.fragment_osm_export_options_bottom_sheet_dialog, container);

		Bundle args = getArguments();
		if (args != null) {
			poiCount = args.getInt(POI_COUNT_KEY);
			osmNotesCount = args.getInt(NOTES_COUNT_KEY);
		}

		if (nightMode) {
			((TextViewEx) mainView.findViewById(R.id.title_text_view)).setTextColor(getResources().getColor(R.color.ctx_menu_info_text_dark));
		}

		((ImageView) mainView.findViewById(R.id.poi_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_info_dark));
		((ImageView) mainView.findViewById(R.id.osm_notes_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_bug_dark));
		((ImageView) mainView.findViewById(R.id.all_data_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_folder));

		((TextView) mainView.findViewById(R.id.poi_count_text_view)).setText(String.valueOf(poiCount));
		((TextView) mainView.findViewById(R.id.osm_notes_count_text_view)).setText(String.valueOf(osmNotesCount));
		((TextView) mainView.findViewById(R.id.all_data_count_text_view)).setText(String.valueOf(poiCount + osmNotesCount));

		View poiRow = mainView.findViewById(R.id.poi_row);
		if (poiCount > 0) {
			poiRow.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (listener != null) {
						listener.onClick(OsmEditsFragment.EXPORT_TYPE_POI);
					}
					dismiss();
				}
			});
		} else {
			disable(poiRow);
		}

		View osmNotesRow = mainView.findViewById(R.id.osm_notes_row);
		if (osmNotesCount > 0) {
			osmNotesRow.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (listener != null) {
						listener.onClick(OsmEditsFragment.EXPORT_TYPE_NOTES);
					}
					dismiss();
				}
			});
		} else {
			disable(osmNotesRow);
		}

		View allDataRow = mainView.findViewById(R.id.all_data_row);
		if ((poiCount + osmNotesCount) > 0) {
			allDataRow.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (listener != null) {
						listener.onClick(OsmEditsFragment.EXPORT_TYPE_ALL);
					}
					dismiss();
				}
			});
		} else {
			disable(allDataRow);
		}

		mainView.findViewById(R.id.cancel_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		setupHeightAndBackground(mainView, R.id.scroll_view);

		return mainView;
	}

	private void disable(View view) {
		view.setEnabled(false);
		view.setAlpha(.5f);
	}

	public interface ExportOptionsFragmentListener {

		void onClick(@ExportTypesDef int type);
	}
}
