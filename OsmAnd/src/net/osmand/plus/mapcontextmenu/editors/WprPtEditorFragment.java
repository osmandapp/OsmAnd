package net.osmand.plus.mapcontextmenu.editors;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import net.osmand.data.LatLon;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.util.Algorithms;

public class WprPtEditorFragment extends PointEditorFragment {
	private WptPtEditor editor;
	private WptPt wpt;
	private SavingTrackHelper helper;

	private boolean saved;
	private int defaultColor;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		helper = getMapActivity().getMyApplication().getSavingTrackHelper();
		editor = getMapActivity().getContextMenu().getWptPtPointEditor();
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		defaultColor = getResources().getColor(R.color.gpx_color_point);

		wpt = editor.getWptPt();
	}

	@Override
	public PointEditor getEditor() {
		return editor;
	}

	@Override
	public String getToolbarTitle() {
		if (editor.isNew()) {
			return getMapActivity().getResources().getString(R.string.context_menu_item_add_waypoint);
		} else {
			return getMapActivity().getResources().getString(R.string.shared_string_edit);
		}
	}

	public static void showInstance(final MapActivity mapActivity) {
		WptPtEditor editor = mapActivity.getContextMenu().getWptPtPointEditor();
		//int slideInAnim = editor.getSlideInAnimation();
		//int slideOutAnim = editor.getSlideOutAnimation();

		WprPtEditorFragment fragment = new WprPtEditorFragment();
		mapActivity.getSupportFragmentManager().beginTransaction()
				//.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
				.add(R.id.fragmentContainer, fragment, editor.getFragmentTag())
				.addToBackStack(null).commit();
	}

	@Override
	protected boolean wasSaved() {
		return saved;
	}

	@Override
	protected void save(final boolean needDismiss) {
		String name = Algorithms.isEmpty(getNameTextValue()) ? null : getNameTextValue();
		String category = Algorithms.isEmpty(getCategoryTextValue()) ? null : getCategoryTextValue();
		String description = Algorithms.isEmpty(getDescriptionTextValue()) ? null : getDescriptionTextValue();
		if (editor.isNew()) {
			doAddWpt(name, category, description);
		} else {
			//todo save wpt
		}
		getMapActivity().getMapView().refreshMap(true);
		if (needDismiss) {
			dismiss(false);
		}

		MapContextMenu menu = getMapActivity().getContextMenu();
		LatLon latLon = new LatLon(wpt.getLatitude(), wpt.getLongitude());
		if (menu.getLatLon().equals(latLon)) {
			menu.update(latLon, wpt.getPointDescription(getMapActivity()), wpt);
		}

		saved = true;
	}

	private void doAddWpt(String name, String category, String description) {
		wpt.name = name;
		wpt.category = category;
		wpt.desc = description;
		helper.insertPointData(wpt.getLatitude(), wpt.getLongitude(), System.currentTimeMillis(), description, name, category);
	}

	@Override
	protected void delete(final boolean needDismiss) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(getString(R.string.favourites_remove_dialog_msg, wpt.name));
		builder.setNegativeButton(R.string.shared_string_no, null);
		builder.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//helper.deleteFavourite(wpt); todo delete wpt
				if (needDismiss) {
					dismiss(true);
				}
				getMapActivity().getMapView().refreshMap(true);
			}
		});
		builder.create().show();
	}

	@Override
	public String getHeaderCaption() {
		return getMapActivity().getResources().getString(R.string.gpx_wpt);
	}

	@Override
	public String getNameInitValue() {
		return wpt.name;
	}

	@Override
	public String getCategoryInitValue() {
		return Algorithms.isEmpty(wpt.category) ? "" : wpt.category;
	}

	@Override
	public String getDescriptionInitValue() {
		return wpt.desc;
	}

	@Override
	public Drawable getNameIcon() {
		int color = wpt.getColor(defaultColor);
		return FavoriteImageDrawable.getOrCreate(getMapActivity(), color, false);
	}

	@Override
	public Drawable getCategoryIcon() {
		int color = wpt.getColor(defaultColor);
		return getIcon(R.drawable.ic_action_folder_stroke, color);
	}

	public Drawable getIcon(int resId, int color) {
		OsmandApplication app = getMyApplication();
		Drawable d = app.getResources().getDrawable(resId).mutate();
		d.clearColorFilter();
		d.setColorFilter(color, PorterDuff.Mode.SRC_IN);
		return d;
	}
}
