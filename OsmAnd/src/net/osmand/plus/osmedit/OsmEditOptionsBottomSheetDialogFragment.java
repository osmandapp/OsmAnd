package net.osmand.plus.osmedit;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.widgets.TextViewEx;

public class OsmEditOptionsBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public final static String TAG = "OsmEditOptionsBottomSheetDialogFragment";

	public static final String OSM_POINT = "osm_point";

	private OsmEditOptionsFragmentListener listener;

	public void setListener(OsmEditOptionsFragmentListener listener) {
		this.listener = listener;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_osm_edit_options_bottom_sheet_dialog, container);

		Bundle args = getArguments();
		if (args != null) {
			final OsmPoint osmPoint = (OsmPoint) args.getSerializable(OSM_POINT);

			((TextViewEx) mainView.findViewById(R.id.osm_edit_name)).setText(OsmEditingPlugin.getName(osmPoint) + ":");

			((ImageView) mainView.findViewById(R.id.upload_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_export));
			mainView.findViewById(R.id.upload_row).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (listener != null) {
						listener.onUploadClick(osmPoint);
					}
					dismiss();
				}
			});

			((ImageView) mainView.findViewById(R.id.show_on_map_icon)).setImageDrawable(getContentIcon(R.drawable.ic_show_on_map));
			mainView.findViewById(R.id.show_on_map_row).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (listener != null) {
						listener.onShowOnMapClick(osmPoint);
					}
					dismiss();
				}
			});

			if (osmPoint instanceof OpenstreetmapPoint && osmPoint.getAction() != OsmPoint.Action.DELETE) {
				mainView.findViewById(R.id.modify_osm_change_row).setVisibility(View.VISIBLE);
				((ImageView) mainView.findViewById(R.id.modify_osm_change_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_edit_dark));
				mainView.findViewById(R.id.modify_osm_change_row).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						if (listener != null) {
							listener.onModifyOsmChangeClick(osmPoint);
						}
						dismiss();
					}
				});
			}

			if (osmPoint instanceof OsmNotesPoint) {
				mainView.findViewById(R.id.modify_osm_note_row).setVisibility(View.VISIBLE);
				((ImageView) mainView.findViewById(R.id.modify_osm_note_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_edit_dark));
				mainView.findViewById(R.id.modify_osm_note_row).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						if (listener != null) {
							listener.onModifyOsmNoteClick(osmPoint);
						}
						dismiss();
					}
				});
			}

			((ImageView) mainView.findViewById(R.id.delete_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_delete_dark));
			mainView.findViewById(R.id.delete_row).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (listener != null) {
						listener.onDeleteClick(osmPoint);
					}
					dismiss();
				}
			});
		}

		if (nightMode) {
			((TextViewEx) mainView.findViewById(R.id.osm_edit_name)).setTextColor(getResources().getColor(R.color.ctx_menu_info_text_dark));
		}

		mainView.findViewById(R.id.cancel_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		setupHeightAndBackground(mainView, R.id.osm_edit_options_scroll_view);

		return mainView;
	}

	public interface OsmEditOptionsFragmentListener {

		void onUploadClick(OsmPoint osmPoint);

		void onShowOnMapClick(OsmPoint osmPoint);

		void onModifyOsmChangeClick(OsmPoint osmPoint);

		void onModifyOsmNoteClick(OsmPoint osmPoint);

		void onDeleteClick(OsmPoint osmPoint);
	}
}
