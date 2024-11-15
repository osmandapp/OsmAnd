package net.osmand.plus.mapcontextmenu.editors;

import static net.osmand.plus.myplaces.favorites.FavoriteGroup.PERSONAL_CATEGORY;
import static net.osmand.plus.myplaces.favorites.FavoriteGroup.isPersonalCategoryDisplayName;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputLayout;

import net.osmand.data.LatLon;
import net.osmand.shared.gpx.GpxUtilities.PointsGroup;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.myplaces.tracks.MapBitmapDrawerListener;
import net.osmand.plus.myplaces.tracks.MapDrawParams;
import net.osmand.plus.myplaces.tracks.PointBitmapDrawer;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class PointEditorFragment extends EditorFragment {

	private TextView addDelDescription;
	private TextView addAddressBtn;
	private TextView addToHiddenGroupInfo;
	private ImageView deleteAddressIcon;
	private ImageView image;
	private ImageView nameIcon;
	private GroupAdapter groupListAdapter;
	private RecyclerView groupRecyclerView;
	private View descriptionCaption;
	private View addressCaption;
	private EditText descriptionEdit;
	private EditText addressEdit;

	protected PointsGroup selectedGroup;
	protected PointBitmapDrawer pointDrawer;
	protected boolean skipConfirmationDialog;

	@Override
	protected int getLayoutId() {
		return R.layout.point_editor_fragment;
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		Context context = requireContext();
		PointEditor editor = getEditor();
		if (editor == null) {
			view = themedInflater.inflate(getLayoutId(), container, false);
			AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);
			return view;
		}

		view = super.onCreateView(inflater, container, savedInstanceState);

		int activeColor = ColorUtilities.getActiveColor(context, nightMode);
		ImageView toolbarAction = view.findViewById(R.id.toolbar_action);
		ImageView replaceIcon = view.findViewById(R.id.replace_action_icon);
		replaceIcon.setImageDrawable(getPaintedIcon(R.drawable.ic_action_replace, activeColor));
		ImageView deleteIcon = view.findViewById(R.id.delete_action_icon);
		deleteIcon.setImageDrawable(getPaintedIcon(R.drawable.ic_action_delete_dark, activeColor));
		ImageView groupListIcon = view.findViewById(R.id.group_list_button_icon);
		groupListIcon.setImageDrawable(getPaintedIcon(R.drawable.ic_action_group_select_all, activeColor));
		addToHiddenGroupInfo = view.findViewById(R.id.add_hidden_group_info);
		addToHiddenGroupInfo.setText(getString(R.string.add_hidden_group_info, getString(R.string.shared_string_my_places)));
		View groupList = view.findViewById(R.id.group_list_button);
		groupList.setOnClickListener(v -> showSelectCategoryDialog());

		TextInputLayout nameCaption = view.findViewById(R.id.name_caption);
		nameCaption.setHint(getString(R.string.shared_string_name));

		image = view.findViewById(R.id.image);
		nameIcon = view.findViewById(R.id.name_icon);
		addressEdit = view.findViewById(R.id.address_edit);
		descriptionEdit = view.findViewById(R.id.description_edit);

		AndroidUtils.setTextPrimaryColor(view.getContext(), descriptionEdit, nightMode);
		AndroidUtils.setTextPrimaryColor(view.getContext(), addressEdit, nightMode);
		AndroidUtils.setHintTextSecondaryColor(view.getContext(), descriptionEdit, nightMode);
		AndroidUtils.setHintTextSecondaryColor(view.getContext(), addressEdit, nightMode);
		if (getDescriptionInitValue() != null) {
			descriptionEdit.setText(getDescriptionInitValue());
		}

		descriptionCaption = view.findViewById(R.id.description);
		addressCaption = view.findViewById(R.id.address);
		addDelDescription = view.findViewById(R.id.description_button);
		addAddressBtn = view.findViewById(R.id.address_button);
		deleteAddressIcon = view.findViewById(R.id.delete_address_icon);
		deleteAddressIcon.setImageDrawable(getPaintedIcon(R.drawable.ic_action_trash_basket_16, activeColor));

		addDelDescription.setTextColor(activeColor);
		addAddressBtn.setTextColor(activeColor);
		Drawable addressIcon = getPaintedIcon(R.drawable.ic_action_location_16, activeColor);
		addAddressBtn.setCompoundDrawablesWithIntrinsicBounds(addressIcon, null, null, null);
		addDelDescription.setOnClickListener(v -> {
			if (descriptionCaption.getVisibility() != View.VISIBLE) {
				descriptionCaption.setVisibility(View.VISIBLE);
				addDelDescription.setText(view.getResources().getString(R.string.delete_description));
				View descriptionEdit = view.findViewById(R.id.description_edit);
				descriptionEdit.requestFocus();
				AndroidUtils.softKeyboardDelayed(getActivity(), descriptionEdit);
			} else {
				descriptionCaption.setVisibility(View.GONE);
				addDelDescription.setText(view.getResources().getString(R.string.add_description));
				AndroidUtils.hideSoftKeyboard(requireActivity(), descriptionEdit);
				descriptionEdit.clearFocus();
			}
			updateDescriptionIcon();
		});
		AndroidUiHelper.updateVisibility(addressCaption, false);

		String addressInitValue = getAddressInitValue();
		if (!Algorithms.isEmpty(addressInitValue)) {
			addressEdit.setText(addressInitValue);
			addAddressBtn.setText(addressInitValue);
			addressEdit.setSelection(addressInitValue.length());
			AndroidUiHelper.updateVisibility(deleteAddressIcon, true);
		} else {
			addAddressBtn.setText(getString(R.string.add_address));
			AndroidUiHelper.updateVisibility(deleteAddressIcon, false);
		}

		deleteAddressIcon.setOnClickListener(v -> {
			addressEdit.setText("");
			addAddressBtn.setText(view.getResources().getString(R.string.add_address));
			AndroidUiHelper.updateVisibility(addressCaption, false);
			AndroidUiHelper.updateVisibility(deleteAddressIcon, false);
		});

		View addressRow = view.findViewById(R.id.address_row);
		addAddressBtn.setOnClickListener(v -> {
			if (addressCaption.getVisibility() != View.VISIBLE) {
				addressCaption.setVisibility(View.VISIBLE);
				addressEdit.requestFocus();
				addressEdit.setSelection(addressEdit.getText().length());
				AndroidUtils.softKeyboardDelayed(requireActivity(), addressEdit);
				AndroidUiHelper.updateVisibility(addressRow, false);
			} else {
				addressCaption.setVisibility(View.GONE);
				addAddressBtn.setText(getAddressTextValue());
				AndroidUtils.hideSoftKeyboard(requireActivity(), addressEdit);
				addressEdit.clearFocus();
			}
		});
		updateNameIcon();

		if (app.accessibilityEnabled()) {
			nameCaption.setFocusable(true);
			nameEdit.setHint(R.string.access_hint_enter_name);
		}

		View deleteButton = view.findViewById(R.id.button_delete_container);
		deleteButton.setOnClickListener(v -> deletePressed());

		if (editor.isProcessingTemplate()) {
			View replaceButton = view.findViewById(R.id.button_replace_container);
			AndroidUiHelper.setVisibility(View.GONE, toolbarAction, replaceButton, deleteButton);
		}
		if (editor.isNew()) {
			toolbarAction.setImageDrawable(getPaintedIcon(R.drawable.ic_action_replace, activeColor));
			deleteButton.setVisibility(View.GONE);
			descriptionCaption.setVisibility(View.GONE);
			deleteIcon.setVisibility(View.GONE);
			nameEdit.selectAll();
			nameEdit.requestFocus();

			if (savedInstanceState == null) {
				showKeyboard();
			}
		} else {
			toolbarAction.setImageDrawable(getPaintedIcon(R.drawable.ic_action_delete_dark, activeColor));
			deleteButton.setVisibility(View.VISIBLE);
			deleteIcon.setVisibility(View.VISIBLE);
		}

		toolbarAction.setOnClickListener(view -> {
			if (!editor.isNew) {
				deletePressed();
			}
		});
		createGroupSelector();

		view.findViewById(R.id.editor_scroll_view).setOnTouchListener((v, event) -> {
			descriptionEdit.getParent().requestDisallowInterceptTouchEvent(false);
			return false;
		});

		descriptionEdit.setOnTouchListener((v, event) -> {
			descriptionEdit.getParent().requestDisallowInterceptTouchEvent(true);
			return false;
		});
		drawPointImage();

		return view;
	}

	private void updateDescriptionIcon() {
		int iconId = descriptionCaption.getVisibility() == View.VISIBLE
				? R.drawable.ic_action_trash_basket_16
				: R.drawable.ic_action_description_16;
		int activeColor = ColorUtilities.getActiveColorId(nightMode);
		Drawable icon = getIcon(iconId, activeColor);
		addDelDescription.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
	}

	@Override
	protected void onMainScrollChanged() {
		super.onMainScrollChanged();
		descriptionEdit.clearFocus();
		addressEdit.clearFocus();
	}

	@Override
	protected void setupNameChangeListener() {
		boolean emptyNameAllowed = getEditor() != null && getEditor().isProcessingTemplate();
		if (!emptyNameAllowed) {
			super.setupNameChangeListener();
		}
	}

	@Override
	protected void setupButtons() {
		super.setupButtons();
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.buttons_divider), true);
		DialogButton cancelButton = view.findViewById(R.id.dismiss_button);
		cancelButton.setOnClickListener(v -> showExitDialog());
		cancelButton.setButtonType(DialogButtonType.SECONDARY);
		cancelButton.setTitleId(R.string.shared_string_cancel);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (skipConfirmationDialog) {
			save(true);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!descriptionEdit.getText().toString().isEmpty() || descriptionEdit.hasFocus()) {
			descriptionCaption.setVisibility(View.VISIBLE);
			addDelDescription.setText(app.getString(R.string.delete_description));
		} else {
			descriptionCaption.setVisibility(View.GONE);
			addDelDescription.setText(app.getString(R.string.add_description));
		}
		updateDescriptionIcon();
	}

	private void createGroupSelector() {
		groupListAdapter = new GroupAdapter();
		groupRecyclerView = view.findViewById(R.id.group_recycler_view);
		groupRecyclerView.setAdapter(groupListAdapter);
		groupRecyclerView.setLayoutManager(new LinearLayoutManager(app, RecyclerView.HORIZONTAL, false));

		if (selectedGroup != null) {
			setSelectedItemWithScroll(selectedGroup);
		}
	}

	@Override
	public void onCardPressed(@NonNull BaseCard card) {
		super.onCardPressed(card);
		if (card instanceof ShapesCard) {
			updateNameIcon();
		}
	}

	@Override
	protected void updateContent() {
		super.updateContent();
		updateNameIcon();
	}

	private void updateNameIcon() {
		if (nameIcon != null) {
			nameIcon.setImageDrawable(getNameIcon());
		}
	}

	@NonNull
	public String getSelectedCategory() {
		return selectedGroup != null ? selectedGroup.getName() : "";
	}

	@Override
	public void onDestroyView() {
		PointEditor editor = getEditor();
		if (!wasSaved() && editor != null && !editor.isNew() && !cancelled) {
			save(false);
		}
		if (pointDrawer != null) {
			pointDrawer.setDrawingAllowed(false);
		}
		super.onDestroyView();
	}

	@Override
	protected void showKeyboard() {
		if (!skipConfirmationDialog) {
			super.showKeyboard();
		}
	}

	private void deletePressed() {
		delete(true);
	}

	public void setPointsGroup(@NonNull PointsGroup group) {
		setPointsGroup(group, shouldUpdateAppearance());
		setSelectedItemWithScroll(group);
	}

	public void setPointsGroup(@NonNull PointsGroup group, boolean updateAppearance) {
		this.selectedGroup = group;
		if (updateAppearance) {
			setColor(group.getColor());
			setIconName(group.getIconName());
			setBackgroundType(group.getBackgroundType());
			updateContent();
		}
		AndroidUiHelper.updateVisibility(addToHiddenGroupInfo, !isCategoryVisible(group.getName()));
	}

	private boolean shouldUpdateAppearance() {
		PointEditor editor = getEditor();
		return editor != null && editor.isNew();
	}

	@SuppressLint("NotifyDataSetChanged")
	private void setSelectedItemWithScroll(@NonNull PointsGroup group) {
		groupListAdapter.fillGroups();
		groupListAdapter.notifyDataSetChanged();
		int position = 0;
		PointEditor editor = getEditor();
		if (editor != null) {
			int itemPosition = groupListAdapter.getItemPosition(group);
			position = groupListAdapter.items.size() == itemPosition + 1 ? itemPosition + 1 : itemPosition;
		}
		groupRecyclerView.scrollToPosition(position);
	}

	protected void drawPointImage() {
		LatLon latLon = getPointCoordinates();
		MapDrawParams params = getMapDrawParams();
		pointDrawer = new PointBitmapDrawer(app, params, latLon);
		pointDrawer.addListener(new MapBitmapDrawerListener() {
			@Override
			public void onBitmapDrawn(boolean success) {
				if (image != null) {
					image.setClipToOutline(true);
					image.setImageBitmap(pointDrawer.getMapBitmap());
				}
			}
		});
		pointDrawer.initAndDraw();
	}

	@NonNull
	private MapDrawParams getMapDrawParams() {
		DisplayMetrics metrics = new DisplayMetrics();
		AndroidUtils.getDisplay(requireContext()).getMetrics(metrics);

		int width = metrics.widthPixels - AndroidUtils.dpToPx(app, 32);
		int height = getResources().getDimensionPixelSize(R.dimen.point_image_height);

		return new MapDrawParams(metrics.density, width, height);
	}

	protected String getLastUsedGroup() {
		return "";
	}

	protected String getDefaultCategoryName() {
		return getString(R.string.shared_string_none);
	}

	public void dismiss() {
		dismiss(false);
	}

	public void dismiss(boolean includingMenu) {
		super.dismiss();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			MapContextMenu mapContextMenu = mapActivity.getContextMenu();
			if (includingMenu) {
				mapContextMenu.close();
			} else {
				if (!mapContextMenu.isVisible() && mapContextMenu.isActive()) {
					mapContextMenu.show();
				}
			}
		}
	}

	protected abstract void delete(boolean needDismiss);

	@NonNull
	protected abstract Map<String, PointsGroup> getPointsGroups();

	@NonNull
	protected abstract LatLon getPointCoordinates();

	protected abstract String getAddressInitValue();

	protected abstract String getDescriptionInitValue();

	protected abstract Drawable getNameIcon();

	protected abstract void showSelectCategoryDialog();

	protected abstract void showAddNewCategoryFragment();

	protected abstract boolean isCategoryVisible(String name);

	protected String getCategoryTextValue() {
		RecyclerView recyclerView = view.findViewById(R.id.group_recycler_view);
		if (recyclerView.getAdapter() != null && selectedGroup != null) {
			if (isPersonalCategoryDisplayName(requireContext(), selectedGroup.getName())) {
				return PERSONAL_CATEGORY;
			}
			if (Algorithms.stringsEqual(selectedGroup.getName(), getDefaultCategoryName())) {
				return "";
			}
			return selectedGroup.getName();
		}
		return "";
	}

	protected String getDescriptionTextValue() {
		EditText descriptionEdit = view.findViewById(R.id.description_edit);
		String res = descriptionEdit.getText().toString().trim();
		return Algorithms.isEmpty(res) ? null : res;
	}

	protected String getAddressTextValue() {
		EditText addressEdit = view.findViewById(R.id.address_edit);
		String res = addressEdit.getText().toString().trim();
		return Algorithms.isEmpty(res) ? null : res;
	}

	class GroupAdapter extends RecyclerView.Adapter<GroupsViewHolder> {

		private static final int VIEW_TYPE_CELL = 0;
		private static final int VIEW_TYPE_FOOTER = 1;

		private final List<PointsGroup> items = new ArrayList<>();

		GroupAdapter() {
			fillGroups();
		}

		private void fillGroups() {
			items.clear();
			items.addAll(getPointsGroups().values());
		}

		@NonNull
		@Override
		public GroupsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			Context context = parent.getContext();
			View view = LayoutInflater.from(context)
					.inflate(R.layout.point_editor_group_select_item, parent, false);
			int activeColor = ColorUtilities.getActiveColor(context, nightMode);
			if (viewType != VIEW_TYPE_CELL) {
				Drawable iconAdd = getPaintedIcon(R.drawable.ic_action_add, activeColor);
				((ImageView) view.findViewById(R.id.groupIcon)).setImageDrawable(iconAdd);
				((TextView) view.findViewById(R.id.groupName)).setText(R.string.add_group);
				GradientDrawable rectContourDrawable = (GradientDrawable) AppCompatResources.getDrawable(app,
						R.drawable.bg_select_group_button_outline);
				if (rectContourDrawable != null) {
					int strokeColor = ColorUtilities.getStrokedButtonsOutlineColor(context, nightMode);
					rectContourDrawable.setStroke(AndroidUtils.dpToPx(app, 1), strokeColor);
					((ImageView) view.findViewById(R.id.outlineRect)).setImageDrawable(rectContourDrawable);
				}
			}
			((TextView) view.findViewById(R.id.groupName)).setTextColor(activeColor);
			return new GroupsViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull GroupsViewHolder holder, int position) {
			if (position == items.size()) {
				holder.groupButton.setOnClickListener(view -> showAddNewCategoryFragment());
			} else {
				holder.groupButton.setOnClickListener(view -> {
					int previousSelectedPosition = getItemPosition(selectedGroup);
					setPointsGroup(items.get(holder.getAdapterPosition()), shouldUpdateAppearance());

					notifyItemChanged(holder.getAdapterPosition());
					notifyItemChanged(previousSelectedPosition);
				});
				PointsGroup group = items.get(position);
				holder.groupName.setText(group.getName());
				holder.pointsCounter.setText(String.valueOf(group.getPoints().size()));
				int strokeColor;
				int strokeWidth;
				if (Algorithms.objectEquals(selectedGroup, items.get(position))) {
					strokeColor = ColorUtilities.getActiveColor(app, nightMode);
					strokeWidth = 2;
				} else {
					strokeColor = ColorUtilities.getStrokedButtonsOutlineColor(app, nightMode);
					strokeWidth = 1;
				}
				GradientDrawable rectContourDrawable = (GradientDrawable) AppCompatResources.getDrawable(app,
						R.drawable.bg_select_group_button_outline);
				if (rectContourDrawable != null) {
					rectContourDrawable.setStroke(AndroidUtils.dpToPx(app, strokeWidth), strokeColor);
					holder.groupButton.setImageDrawable(rectContourDrawable);
				}
				int color;
				int iconID;
				if (isCategoryVisible(group.getName())) {
					color = group.getColor() == 0 ? getDefaultColor() : group.getColor();
					iconID = R.drawable.ic_action_folder;
					holder.groupName.setTypeface(null, Typeface.NORMAL);
				} else {
					color = ContextCompat.getColor(app, R.color.text_color_secondary_light);
					iconID = R.drawable.ic_action_hide;
					holder.groupName.setTypeface(null, Typeface.ITALIC);
				}
				holder.groupIcon.setImageDrawable(UiUtilities.tintDrawable(
						AppCompatResources.getDrawable(app, iconID), color));
			}
			AndroidUtils.setBackground(app, holder.groupButton, nightMode, R.drawable.ripple_solid_light_6dp,
					R.drawable.ripple_solid_dark_6dp);
		}

		@Override
		public int getItemViewType(int position) {
			return (position == items.size()) ? VIEW_TYPE_FOOTER : VIEW_TYPE_CELL;
		}

		@Override
		public int getItemCount() {
			return items.size() + 1;
		}

		int getItemPosition(PointsGroup group) {
			return items.indexOf(group);
		}
	}

	static class GroupsViewHolder extends RecyclerView.ViewHolder {

		final TextView pointsCounter;
		final TextView groupName;
		final ImageView groupIcon;
		final ImageView groupButton;

		GroupsViewHolder(View itemView) {
			super(itemView);
			pointsCounter = itemView.findViewById(R.id.counter);
			groupName = itemView.findViewById(R.id.groupName);
			groupIcon = itemView.findViewById(R.id.groupIcon);
			groupButton = itemView.findViewById(R.id.outlineRect);
		}
	}
}