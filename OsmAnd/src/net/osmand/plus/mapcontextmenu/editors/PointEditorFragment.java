package net.osmand.plus.mapcontextmenu.editors;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

public abstract class PointEditorFragment extends BaseOsmAndFragment {

	private View view;
	private EditText nameEdit;
	private boolean cancelled;

	@SuppressLint("ClickableViewAccessibility")
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		Context context = requireContext();
		boolean nightMode = !requireSettings().isLightContent();
		view = UiUtilities.getInflater(getContext(), nightMode).inflate(R.layout.point_editor_fragment, container, false);

		PointEditor editor = getEditor();
		if (editor == null) {
			return view;
		}

		editor.updateLandscapePortrait(requireActivity());
		editor.updateNightMode();

		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setBackgroundColor(ColorUtilities.getAppBarColor(context, !editor.isLight()));
		toolbar.setTitle(getToolbarTitle());

		Drawable icBack = getIcon(AndroidUtils.getNavigationIconResId(context),
				ColorUtilities.getActiveButtonsAndLinksTextColorId(!editor.isLight()));
		toolbar.setNavigationIcon(icBack);
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setTitleTextColor(ColorUtilities.getActiveTabTextColor(context, nightMode));
		toolbar.setNavigationOnClickListener(v -> dismiss());

		int activeColor = ColorUtilities.getActiveColor(context, !editor.isLight());

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
		deleteButton.setOnClickListener(v -> deletePressed());
		AndroidUiHelper.updateVisibility(deleteButton, !editor.isNew());

		int activityBgColorId = ColorUtilities.getActivityBgColorId(!editor.isLight());
		int listBgColorId = ColorUtilities.getListBgColorId(!editor.isLight());
		view.findViewById(R.id.background_layout).setBackgroundResource(activityBgColorId);
		view.findViewById(R.id.buttons_layout).setBackgroundResource(activityBgColorId);
		view.findViewById(R.id.title_view).setBackgroundResource(listBgColorId);

		TextView nameCaption = view.findViewById(R.id.name_caption);
		AndroidUtils.setTextSecondaryColor(context, nameCaption, !editor.isLight());
		nameCaption.setText(getNameCaption());

		nameEdit = view.findViewById(R.id.name_edit);
		AndroidUtils.setTextPrimaryColor(context, nameEdit, !editor.isLight());
		AndroidUtils.setHintTextSecondaryColor(context, nameEdit, !editor.isLight());
		nameEdit.setText(getNameInitValue());

		ImageView nameImage = view.findViewById(R.id.name_image);
		nameImage.setImageDrawable(getNameIcon());

		if (requireMyApplication().accessibilityEnabled()) {
			nameCaption.setFocusable(true);
			nameEdit.setHint(R.string.access_hint_enter_name);
		}
		return view;
	}

	protected EditText getNameEdit() {
		return nameEdit;
	}

	public Drawable getRowIcon(int iconId) {
		PointEditor editor = getEditor();
		boolean light = editor == null || editor.isLight();
		return getIcon(iconId, ColorUtilities.getDefaultIconColorId(!light));
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
		PointEditor editor = getEditor();
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
		PointEditor editor = getEditor();
		if (!wasSaved() && editor != null && !editor.isNew() && !cancelled) {
			save(false);
		}
		super.onDestroyView();
	}

	@Override
	public int getStatusBarColorId() {
		return R.color.status_bar_color_light;
	}

	@Override
	protected boolean isFullScreenAllowed() {
		return false;
	}

	protected void savePressed() {
		save(true);
	}

	protected void deletePressed() {
		delete(true);
	}

	protected abstract boolean wasSaved();

	protected abstract void save(boolean needDismiss);

	protected abstract void delete(boolean needDismiss);

	@Nullable
	public abstract PointEditor getEditor();

	public abstract String getToolbarTitle();

	@Nullable
	protected MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	@Nullable
	@Override
	protected OsmandApplication getMyApplication() {
		if (getActivity() == null) {
			return null;
		}
		return (OsmandApplication) getActivity().getApplication();
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

	public abstract String getHeaderCaption();

	public String getNameCaption() {
		return getString(R.string.shared_string_name);
	}

	public abstract String getNameInitValue();

	public abstract String getDescriptionInitValue();

	public abstract Drawable getNameIcon();

	public abstract int getPointColor();

	public String getNameTextValue() {
		EditText nameEdit = view.findViewById(R.id.name_edit);
		return nameEdit.getText().toString().trim();
	}

	protected Drawable getPaintedIcon(int iconId, int color) {
		return getPaintedContentIcon(iconId, color);
	}
}