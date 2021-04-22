package net.osmand.plus.mapcontextmenu.editors;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.PointDescription;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class MapMarkerEditorFragment extends PointEditorFragment {

	private MapMarkerEditor editor;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MapActivity mapActivity = getMapActivity();
		editor = mapActivity != null ? mapActivity.getContextMenu().getMapMarkerEditor() : null;
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View mainView = super.onCreateView(inflater, container, savedInstanceState);
		if (mainView != null) {
			mainView.findViewById(R.id.category_row).setVisibility(View.GONE);
			mainView.findViewById(R.id.description_info_view).setVisibility(View.GONE);
		}
		return mainView;
	}

	@Override
	protected boolean wasSaved() {
		return true;
	}

	@Override
	protected void save(boolean needDismiss) {
		EditText nameEt = getNameEdit();
		String name = nameEt.getText().toString().trim();
		if (name.replaceAll("\\s", "").length() > 0) {
			MapMarker marker = editor.getMarker();
			marker.setOriginalPointDescription(new PointDescription(PointDescription.POINT_TYPE_MAP_MARKER, name));
			OsmandApplication app = getMyApplication();
			if (app != null) {
				app.getMapMarkersHelper().updateMapMarker(marker, true);
			}
			if (needDismiss) {
				dismiss(true);
			}
		} else {
			nameEt.setError(getString(R.string.wrong_input));
		}
	}

	@Override
	protected void delete(final boolean needDismiss) {
		Context ctx = getContext();
		final MapMarker marker = editor.getMarker();
		new AlertDialog.Builder(ctx)
				.setMessage(getString(R.string.markers_remove_dialog_msg, marker.getName(ctx)))
				.setNegativeButton(R.string.shared_string_no, null)
				.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						OsmandApplication app = getMyApplication();
						if (app != null) {
							app.getMapMarkersHelper().removeMarker(marker);
						}
						if (needDismiss) {
							dismiss(true);
						}
					}
				})
				.create()
				.show();
	}

	@Override
	public PointEditor getEditor() {
		return editor;
	}

	@Override
	public String getToolbarTitle() {
		return getString(R.string.edit_map_marker);
	}

	@Override
	public String getHeaderCaption() {
		return "";
	}

	@Override
	public String getNameInitValue() {
		return editor.getMarker().getName(getContext());
	}

	@Override
	public String getCategoryInitValue() {
		return "";
	}

	@Override
	public String getDescriptionInitValue() {
		return "";
	}

	@Override
	public Drawable getNameIcon() {
		return requireMyApplication().getUIUtilities().getIcon(R.drawable.ic_action_flag, getPointColor());
	}

	@Override
	public Drawable getCategoryIcon() {
		return null;
	}

	@Override
	public int getPointColor() {
		return MapMarker.getColorId(editor.getMarker().colorIndex);
	}

	public static void showInstance(MapActivity mapActivity) {
		MapMarkerEditor editor = mapActivity.getContextMenu().getMapMarkerEditor();
		if (editor != null) {
			MapMarkerEditorFragment fragment = new MapMarkerEditorFragment();
			mapActivity.getSupportFragmentManager().beginTransaction()
					.add(R.id.fragmentContainer, fragment, editor.getFragmentTag())
					.addToBackStack(null)
					.commitAllowingStateLoss();
		}
	}
}
