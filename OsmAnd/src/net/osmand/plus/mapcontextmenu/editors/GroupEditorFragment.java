package net.osmand.plus.mapcontextmenu.editors;

import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.utils.InsetsUtils.InsetSide;
import net.osmand.shared.gpx.GpxUtilities.PointsGroup;
import net.osmand.data.BackgroundType;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.editors.SelectPointsCategoryBottomSheet.CategorySelectionListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;

import java.util.List;
import java.util.Set;

public abstract class GroupEditorFragment extends EditorFragment {

	public static final String TAG = GroupEditorFragment.class.getName();

	protected String groupName;
	protected PointsGroup pointsGroup;
	protected CategorySelectionListener listener;

	protected boolean saved;

	@Override
	protected int getLayoutId() {
		return R.layout.category_editor_fragment;
	}

	@Override
	@DrawableRes
	protected int getToolbarNavigationIconId() {
		return R.drawable.ic_action_close;
	}

	@NonNull
	@Override
	protected String getToolbarTitle() {
		return getString(pointsGroup != null ? R.string.edit_category : R.string.favorite_category_add_new_title);
	}

	@Nullable
	@Override
	public String getNameInitValue() {
		return groupName;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (pointsGroup != null) {
			groupName = pointsGroup.getName();
			setColor(pointsGroup.getColor());
			setIconName(pointsGroup.getIconName());
			setBackgroundType(BackgroundType.getByTypeName(pointsGroup.getBackgroundType(), DEFAULT_BACKGROUND_TYPE));
		} else {
			setColor(getDefaultColor());
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		setupCategoryNameTextBox();
		setupCategoryNameEditText();

		return view;
	}


	@Nullable
	@Override
	public List<Integer> getBottomContainersIds() {
		return null;
	}

	@Nullable
	@Override
	public Set<InsetSide> getRootInsetSides() {
		return null;
	}

	@Nullable
	@Override
	public List<Integer> getScrollableViewIds() {
		return null;
	}

	private void setupCategoryNameTextBox() {
		nameCaption.setHint(getString(R.string.favorite_category_name));
		nameCaption.setStartIconTintList(ColorStateList.valueOf(getColor()));
		nameCaption.setDefaultHintTextColor(ColorStateList.valueOf(ColorUtilities.getSecondaryTextColor(app, nightMode)));
	}

	private void setupCategoryNameEditText() {
		if (Algorithms.isEmpty(groupName) || isCategoryExists(groupName)) {
			nameEdit.requestFocus();
			AndroidUtils.softKeyboardDelayed(requireActivity(), nameEdit);
		}
	}

	protected abstract void addNewGroup();

	protected abstract void editPointsGroup(boolean updatePoints);

	protected abstract boolean isCategoryExists(@NonNull String name);

	@Override
	protected void setupButtons() {
		super.setupButtons();
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.dismiss_button), false);
	}

	@Override
	protected void checkEnteredName(@NonNull String name, @NonNull View saveButton) {
		String trimmedName = name.trim();
		if (pointsGroup == null && isCategoryExists(trimmedName)) {
			nameCaption.setError(getString(R.string.favorite_category_dublicate_message));
			saveButton.setEnabled(false);
		} else {
			nameCaption.setError(null);
			saveButton.setEnabled(true);
		}
		groupName = trimmedName;
	}

	@Override
	protected void updateContent() {
		super.updateContent();
		nameCaption.setStartIconTintList(ColorStateList.valueOf(getColor()));
	}

	@Override
	protected void savePressed() {
		if (pointsGroup != null) {
			PointEditor editor = getEditor();
			FragmentActivity activity = getActivity();
			if (editor != null && activity != null) {
				String tag = editor.getFragmentTag();
				FragmentManager manager = activity.getSupportFragmentManager();
				SaveGroupConfirmationBottomSheet.showInstance(manager, this, tag, pointsGroup.getPoints().size());
			}
		} else {
			save(true);
		}
	}

	@Override
	protected void save(boolean needDismiss) {
		if (pointsGroup == null) {
			addNewGroup();
		}
		PointEditor editor = getEditor();
		if (editor != null) {
			editor.setPointsGroup(pointsGroup);
		}
		if (listener != null) {
			listener.onCategorySelected(pointsGroup);
		}
		if (needDismiss) {
			dismiss();
		}
		saved = true;
	}

	@Override
	protected boolean wasSaved() {
		return saved;
	}
}