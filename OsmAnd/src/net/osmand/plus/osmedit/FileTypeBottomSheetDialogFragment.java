package net.osmand.plus.osmedit;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.osmedit.OsmEditsFragment.FileTypesDef;
import net.osmand.plus.widgets.TextViewEx;

public class FileTypeBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = "FileTypeBottomSheetDialogFragment";

	private FileTypeFragmentListener listener;

	public void setListener(FileTypeFragmentListener listener) {
		this.listener = listener;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes),
				R.layout.fragment_osm_file_type_bottom_sheet_dialog, container);

		if (nightMode) {
			((TextViewEx) mainView.findViewById(R.id.title_text_view)).setTextColor(getResources().getColor(R.color.ctx_menu_info_text_dark));
		}

		Drawable fileIcon = getContentIcon(R.drawable.ic_type_file);
		((ImageView) mainView.findViewById(R.id.osc_file_icon)).setImageDrawable(fileIcon);
		((ImageView) mainView.findViewById(R.id.gpx_file_icon)).setImageDrawable(fileIcon);

		mainView.findViewById(R.id.osc_file_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (listener != null) {
					listener.onClick(OsmEditsFragment.FILE_TYPE_OSC);
				}
				dismiss();
			}
		});

		mainView.findViewById(R.id.gpx_file_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (listener != null) {
					listener.onClick(OsmEditsFragment.FILE_TYPE_GPX);
				}
				dismiss();
			}
		});

		mainView.findViewById(R.id.cancel_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		setupHeightAndBackground(mainView, R.id.scroll_view);

		return mainView;
	}

	public interface FileTypeFragmentListener {

		void onClick(@FileTypesDef int type);
	}
}
