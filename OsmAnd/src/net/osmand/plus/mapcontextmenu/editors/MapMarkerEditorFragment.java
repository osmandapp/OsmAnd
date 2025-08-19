package net.osmand.plus.mapcontextmenu.editors;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;

public class MapMarkerEditorFragment extends BaseFullScreenFragment {

	@Nullable
	private MapMarkerEditor editor;
	private boolean cancelled;

	private EditText nameEdit;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		editor = ((MapActivity) requireActivity()).getContextMenu().getMapMarkerEditor();
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		Context context = requireContext();
		View view = inflate(R.layout.map_marker_editor_fragment, container, false);

		MapMarkerEditor editor = this.editor;
		if (editor == null) {
			return view;
		}

		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setBackgroundColor(ColorUtilities.getAppBarColor(context, nightMode));
		toolbar.setTitle(R.string.edit_map_marker);

		Drawable icBack = getIcon(AndroidUtils.getNavigationIconResId(context),
				ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode));
		toolbar.setNavigationIcon(icBack);
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setTitleTextColor(ColorUtilities.getActiveTabTextColor(context, nightMode));
		toolbar.setNavigationOnClickListener(v -> dismiss());

		int activeColor = ColorUtilities.getActiveColor(context, nightMode);

		Button saveButton = view.findViewById(R.id.save_button);
		saveButton.setTextColor(activeColor);
		saveButton.setOnClickListener(v -> savePressed());

		Button cancelButton = view.findViewById(R.id.cancel_button);
		cancelButton.setTextColor(activeColor);
		cancelButton.setOnClickListener(v -> {
			cancelled = true;
			dismiss();
		});

		Button deleteButton = view.findViewById(R.id.delete_button);
		deleteButton.setTextColor(activeColor);
		deleteButton.setOnClickListener(v -> delete());
		AndroidUiHelper.updateVisibility(deleteButton, !editor.isNew());

		int activityBgColorId = ColorUtilities.getActivityBgColorId(nightMode);
		int listBgColorId = ColorUtilities.getListBgColorId(nightMode);
		view.findViewById(R.id.background_layout).setBackgroundResource(activityBgColorId);
		view.findViewById(R.id.buttons_layout).setBackgroundResource(activityBgColorId);
		view.findViewById(R.id.title_view).setBackgroundResource(listBgColorId);

		TextView nameCaption = view.findViewById(R.id.name_caption);
		AndroidUtils.setTextSecondaryColor(context, nameCaption, nightMode);
		nameCaption.setText(R.string.shared_string_name);

		nameEdit = view.findViewById(R.id.name_edit);
		AndroidUtils.setTextPrimaryColor(context, nameEdit, nightMode);
		AndroidUtils.setHintTextSecondaryColor(context, nameEdit, nightMode);
		nameEdit.setText(editor.getMarker().getName(context));

		ImageView nameImage = view.findViewById(R.id.name_image);
		int markerColorId = MapMarker.getColorId(editor.getMarker().colorIndex);
		nameImage.setImageDrawable(getIcon(R.drawable.ic_action_flag, markerColorId));

		if (app.accessibilityEnabled()) {
			nameCaption.setFocusable(true);
			nameEdit.setHint(R.string.access_hint_enter_name);
		}
		return view;
	}

	private void savePressed() {
		save(true);
	}

	private void delete() {
		Context context = getContext();
		if (editor != null && context != null) {
			MapMarker marker = editor.getMarker();
			new AlertDialog.Builder(context)
					.setMessage(getString(R.string.markers_remove_dialog_msg, marker.getName(context)))
					.setNegativeButton(R.string.shared_string_no, null)
					.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> {
						app.getMapMarkersHelper().removeMarker(marker);
						dismiss(true);
					})
					.create()
					.show();
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getContextMenu().setBaseFragmentVisibility(false);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (editor != null && editor.isNew()) {
			nameEdit.selectAll();
			nameEdit.requestFocus();
			AndroidUtils.softKeyboardDelayed(getActivity(), nameEdit);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			AndroidUtils.hideSoftKeyboard(mapActivity, mapActivity.getCurrentFocus());
			mapActivity.getContextMenu().setBaseFragmentVisibility(true);
		}
	}

	@Override
	public void onDestroyView() {
		if (editor != null && !editor.isNew() && !cancelled) {
			save(false);
		}
		super.onDestroyView();
	}

	private void save(boolean needDismiss) {
		String name = nameEdit.getText().toString().trim();
		if (Algorithms.isBlank(name)) {
			nameEdit.setError(getString(R.string.wrong_input));
		} else {
			if (editor != null) {
				MapMarker marker = editor.getMarker();
				marker.setOriginalPointDescription(new PointDescription(PointDescription.POINT_TYPE_MAP_MARKER, name));
				app.getMapMarkersHelper().updateMapMarker(marker, true);
			}
			if (needDismiss) {
				dismiss(true);
			}
		}
	}

	public void dismiss() {
		dismiss(false);
	}

	public void dismiss(boolean includingMenu) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (includingMenu) {
				mapActivity.getSupportFragmentManager().popBackStack();
				mapActivity.getContextMenu().close();
			} else {
				mapActivity.getSupportFragmentManager().popBackStack();
			}
		}
	}

	@Override
	public int getStatusBarColorId() {
		return R.color.status_bar_main_light;
	}

	@Override
	protected boolean isFullScreenAllowed() {
		return false;
	}

	public static void showInstance(@NonNull MapActivity mapActivity) {
		MapMarkerEditor editor = mapActivity.getContextMenu().getMapMarkerEditor();
		if (editor != null) {
			FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
			String tag = editor.getFragmentTag();
			if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, tag)) {
				MapMarkerEditorFragment fragment = new MapMarkerEditorFragment();
				fragmentManager.beginTransaction()
						.add(R.id.fragmentContainer, fragment, tag)
						.addToBackStack(null)
						.commitAllowingStateLoss();
			}
		}
	}
}