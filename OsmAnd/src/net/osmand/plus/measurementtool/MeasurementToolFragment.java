package net.osmand.plus.measurementtool;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.IndexConstants;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.Route;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.IconsCache;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarController;
import net.osmand.plus.widgets.IconPopupMenu;

import java.io.File;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

import static net.osmand.plus.GPXUtilities.GPXFile;

public class MeasurementToolFragment extends Fragment {

	public static final String TAG = "MeasurementToolFragment";
	private static final String EXT = ".gpx";

	private MeasurementToolBarController toolBarController;
	private TextView distanceTv;
	private TextView pointsTv;
	private String pointsSt;

	private boolean wasCollapseButtonVisible;
	private boolean pointsDetailsOpened;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final MapActivity mapActivity = (MapActivity) getActivity();
		final MeasurementToolLayer measurementLayer = mapActivity.getMapLayers().getMeasurementToolLayer();
		final IconsCache iconsCache = mapActivity.getMyApplication().getIconsCache();
		final boolean nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		final int backgroundColor = ContextCompat.getColor(getActivity(),
				nightMode ? R.color.ctx_menu_info_view_bg_dark : R.color.ctx_menu_info_view_bg_light);
		boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);

		pointsSt = mapActivity.getString(R.string.points).toLowerCase();

		View view = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_measurement_tool, null);

		final View mainView = view.findViewById(R.id.main_view);
		AndroidUtils.setBackground(mapActivity, mainView, nightMode, R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);
		view.findViewById(R.id.points_list_container).setBackgroundColor(backgroundColor);

		distanceTv = (TextView) mainView.findViewById(R.id.measurement_distance_text_view);
		pointsTv = (TextView) mainView.findViewById(R.id.measurement_points_text_view);

		((ImageView) mainView.findViewById(R.id.ruler_icon))
				.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_ruler, R.color.color_myloc_distance));

		final ImageButton upDownBtn = ((ImageButton) mainView.findViewById(R.id.up_down_button));
		upDownBtn.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_arrow_up));
		upDownBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (!pointsDetailsOpened) {
					upBtnOnClick(mainView, iconsCache.getThemedIcon(R.drawable.ic_action_arrow_down));
				} else {
					downBtnOnClick(mainView, iconsCache.getThemedIcon(R.drawable.ic_action_arrow_up));
				}
			}
		});

		final ImageButton undoBtn = ((ImageButton) mainView.findViewById(R.id.undo_point_button));
		final ImageButton redoBtn = ((ImageButton) mainView.findViewById(R.id.redo_point_button));

		undoBtn.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_undo_dark));
		undoBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (measurementLayer.undoPointOnClick()) {
					enable(undoBtn);
				} else {
					disable(undoBtn);
				}
				enable(redoBtn);
				updateText();
			}
		});

		redoBtn.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_redo_dark));
		redoBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (measurementLayer.redoPointOnClick()) {
					enable(redoBtn);
				} else {
					disable(redoBtn);
				}
				enable(undoBtn);
				updateText();
			}
		});

		mainView.findViewById(R.id.add_point_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				measurementLayer.addPointOnClick();
				enable(undoBtn, upDownBtn);
				disable(redoBtn);
				updateText();
			}
		});

		disable(undoBtn, redoBtn, upDownBtn);

		enterMeasurementMode();

		if (portrait) {
			toolBarController = new MeasurementToolBarController();
			toolBarController.setTitle(mapActivity.getString(R.string.measurement_tool_action_bar));
			toolBarController.setOnBackButtonClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mapActivity.onBackPressed();
				}
			});
			toolBarController.setOnCloseButtonClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					IconPopupMenu popup = new IconPopupMenu(mapActivity, mapActivity.findViewById(R.id.widget_top_bar_close_button));
					popup.getMenuInflater().inflate(R.menu.measurement_tool_menu, popup.getMenu());
					final Menu menu = popup.getMenu();
					IconsCache ic = mapActivity.getMyApplication().getIconsCache();
					menu.findItem(R.id.action_save_as_gpx).setIcon(ic.getThemedIcon(R.drawable.ic_action_polygom_dark));
					menu.findItem(R.id.action_clear_all).setIcon(ic.getThemedIcon(R.drawable.ic_action_reset_to_default_dark));
					popup.setOnMenuItemClickListener(new IconPopupMenu.OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem menuItem) {
							switch (menuItem.getItemId()) {
								case R.id.action_save_as_gpx:
									if (measurementLayer.getPointsCount() > 0) {
										saveAsGpxOnClick(mapActivity);
									} else {
										Toast.makeText(mapActivity, mapActivity.getString(R.string.none_point_error), Toast.LENGTH_SHORT).show();
									}
									return true;
								case R.id.action_clear_all:
									measurementLayer.clearPoints();
									disable(undoBtn, redoBtn, upDownBtn);
									updateText();
									return true;
							}
							return false;
						}
					});
					popup.show();
				}
			});
			mapActivity.showTopToolbar(toolBarController);
		}

		return view;
	}

	private void upBtnOnClick(View view, Drawable icon) {
		pointsDetailsOpened = true;
		view.findViewById(R.id.points_list_container).setVisibility(View.VISIBLE);
		((ImageButton) view.findViewById(R.id.up_down_button)).setImageDrawable(icon);
	}

	private void downBtnOnClick(View view, Drawable icon) {
		pointsDetailsOpened = false;
		view.findViewById(R.id.points_list_container).setVisibility(View.GONE);
		((ImageButton) view.findViewById(R.id.up_down_button)).setImageDrawable(icon);
	}

	private void saveAsGpxOnClick(MapActivity mapActivity) {
		final File dir = mapActivity.getMyApplication().getAppPath(IndexConstants.GPX_INDEX_DIR);
		final LayoutInflater inflater = getLayoutInflater();
		final View view = inflater.inflate(R.layout.save_gpx_dialog, null);
		final EditText nameEt = (EditText) view.findViewById(R.id.gpx_name_et);
		final TextView fileExistsTv = (TextView) view.findViewById(R.id.file_exists_text_view);
		final SwitchCompat showOnMapToggle = (SwitchCompat) view.findViewById(R.id.toggle_show_on_map);

		final String suggestedName = new SimpleDateFormat("yyyy-M-dd hh-mm E", Locale.US).format(new Date());
		String displayedName = String.copyValueOf(suggestedName.toCharArray());
		File fout = new File(dir, suggestedName + EXT);
		int ind = 1;
		while (fout.exists()) {
			displayedName = suggestedName + " " + (++ind);
			fout = new File(dir, displayedName + EXT);
		}
		nameEt.setText(displayedName);
		nameEt.setSelection(displayedName.length());

		final boolean[] textChanged = new boolean[1];
		nameEt.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				if (new File(dir, editable.toString() + EXT).exists()) {
					fileExistsTv.setVisibility(View.VISIBLE);
				} else {
					fileExistsTv.setVisibility(View.INVISIBLE);
				}
				textChanged[0] = true;
			}
		});

		new AlertDialog.Builder(mapActivity)
				.setTitle(R.string.enter_gpx_name)
				.setView(view)
				.setPositiveButton(R.string.shared_string_save, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						final String name = nameEt.getText().toString();
						String fileName = String.copyValueOf(name.toCharArray()) + EXT;
						if (textChanged[0]) {
							File fout = new File(dir, fileName);
							int ind = 1;
							while (fout.exists()) {
								fileName = name + " " + (++ind) + EXT;
								fout = new File(dir, fileName);
							}
						}
						saveGpx(dir, fileName, showOnMapToggle.isChecked());
					}
				})
				.setNegativeButton(R.string.shared_string_cancel, null)
				.show();
	}

	private void saveGpx(final File dir, final String fileName, final boolean showOnMap) {
		new AsyncTask<Void, Void, String>() {

			private ProgressDialog progressDialog;
			private File toSave;

			@Override
			protected void onPreExecute() {
				MapActivity activity = getMapActivity();
				if (activity != null) {
					progressDialog = new ProgressDialog(activity);
					progressDialog.setMessage(activity.getString(R.string.saving_gpx_tracks));
					progressDialog.show();
				}
			}

			@Override
			protected String doInBackground(Void... voids) {
				toSave = new File(dir, fileName);
				GPXFile gpx = new GPXFile();
				MeasurementToolLayer measurementLayer = getMeasurementLayer();
				if (measurementLayer != null) {
					LinkedList<WptPt> points = measurementLayer.getMeasurementPoints();
					if (points.size() == 1) {
						gpx.points.add(points.getFirst());
					} else if (points.size() > 1) {
						Route rt = new Route();
						gpx.routes.add(rt);
						rt.points.addAll(points);
					}
				}
				MapActivity activity = getMapActivity();
				if (activity != null) {
					// todo
					String res = GPXUtilities.writeGpxFile(toSave, gpx, activity.getMyApplication());
					activity.getMyApplication().getSelectedGpxHelper().selectGpxFile(gpx, showOnMap, false);
					return res;
				}
				return null;
			}

			@Override
			protected void onPostExecute(String warning) {
				MapActivity activity = getMapActivity();
				if (activity != null) {
					if (warning == null) {
						Toast.makeText(activity,
								MessageFormat.format(activity.getString(R.string.gpx_saved_sucessfully), toSave.getAbsolutePath()),
								Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(activity, warning, Toast.LENGTH_LONG).show();
					}
				}
				if (progressDialog != null && progressDialog.isShowing()) {
					progressDialog.dismiss();
				}
			}
		}.execute();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		exitMeasurementMode();
	}

	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	private MeasurementToolLayer getMeasurementLayer() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return mapActivity.getMapLayers().getMeasurementToolLayer();
		}
		return null;
	}

	private void enable(View... views) {
		for (View view : views) {
			view.setEnabled(true);
			view.setAlpha(1);
		}
	}

	private void disable(View... views) {
		for (View view : views) {
			view.setEnabled(false);
			view.setAlpha(.5f);
		}
	}

	private void updateText() {
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (measurementLayer != null) {
			distanceTv.setText(measurementLayer.getDistanceSt() + ",");
			pointsTv.setText(pointsSt + ": " + measurementLayer.getPointsCount());
		}
	}

	private void enterMeasurementMode() {
		MapActivity mapActivity = getMapActivity();
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (mapActivity != null && measurementLayer != null) {
			measurementLayer.setInMeasurementMode(true);
			mapActivity.refreshMap();
			mapActivity.disableDrawer();
			mark(View.INVISIBLE, R.id.map_left_widgets_panel, R.id.map_right_widgets_panel, R.id.map_center_info);
			mark(View.GONE, R.id.map_route_info_button, R.id.map_menu_button, R.id.map_compass_button, R.id.map_layers_button,
					R.id.map_search_button, R.id.map_quick_actions_button);

			View collapseButton = mapActivity.findViewById(R.id.map_collapse_button);
			if (collapseButton != null && collapseButton.getVisibility() == View.VISIBLE) {
				wasCollapseButtonVisible = true;
				collapseButton.setVisibility(View.INVISIBLE);
			} else {
				wasCollapseButtonVisible = false;
			}

			updateText();
		}
	}

	private void exitMeasurementMode() {
		MapActivity mapActivity = getMapActivity();
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (mapActivity != null && measurementLayer != null) {
			if (toolBarController != null) {
				mapActivity.hideTopToolbar(toolBarController);
			}
			measurementLayer.setInMeasurementMode(false);
			mapActivity.refreshMap();
			mapActivity.enableDrawer();
			mark(View.VISIBLE, R.id.map_left_widgets_panel, R.id.map_right_widgets_panel, R.id.map_center_info,
					R.id.map_route_info_button, R.id.map_menu_button, R.id.map_compass_button, R.id.map_layers_button,
					R.id.map_search_button, R.id.map_quick_actions_button);

			View collapseButton = mapActivity.findViewById(R.id.map_collapse_button);
			if (collapseButton != null && wasCollapseButtonVisible) {
				collapseButton.setVisibility(View.VISIBLE);
			}

			measurementLayer.clearPoints();
		}
	}

	private void mark(int status, int... widgets) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			for (int widget : widgets) {
				View v = mapActivity.findViewById(widget);
				if (v != null) {
					v.setVisibility(status);
				}
			}
		}
	}

	private static class MeasurementToolBarController extends TopToolbarController {

		MeasurementToolBarController() {
			super(MapInfoWidgetsFactory.TopToolbarControllerType.MEASUREMENT_TOOL);
			setBackBtnIconClrIds(0, 0);
			setCloseBtnIconClrIds(0, 0);
			setTitleTextClrIds(R.color.primary_text_dark, R.color.primary_text_dark);
			setDescrTextClrIds(R.color.primary_text_dark, R.color.primary_text_dark);
			setBgIds(R.drawable.gradient_toolbar, R.drawable.gradient_toolbar,
					R.drawable.gradient_toolbar, R.drawable.gradient_toolbar);
			setCloseBtnIconIds(R.drawable.ic_overflow_menu_white, R.drawable.ic_overflow_menu_white);
			setBackBtnIconIds(R.drawable.ic_action_remove_dark, R.drawable.ic_action_remove_dark);
			setSingleLineTitle(false);
		}

		@Override
		public void updateToolbar(MapInfoWidgetsFactory.TopToolbarView view) {
			super.updateToolbar(view);
			view.getShadowView().setVisibility(View.GONE);
		}
	}
}
