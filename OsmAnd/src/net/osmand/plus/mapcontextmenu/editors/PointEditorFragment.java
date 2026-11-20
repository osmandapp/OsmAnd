package net.osmand.plus.mapcontextmenu.editors;

import static net.osmand.plus.myplaces.favorites.FavoriteGroup.PERSONAL_CATEGORY;
import static net.osmand.plus.myplaces.favorites.FavoriteGroup.isPersonalCategoryDisplayName;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputLayout;

import net.osmand.data.LatLon;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.utils.InsetTarget;
import net.osmand.plus.utils.InsetTarget.Type;
import net.osmand.plus.utils.InsetTargetsCollection;
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

import java.text.DateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public abstract class PointEditorFragment extends EditorFragment {

	private static final String SELECTED_GROUP_KEY = "editor_selected_group";
	private static final String CREATION_TIMESTAMP_KEY = "editor_creation_date";
	private static final String CREATION_TIMESTAMP_EDITED_KEY = "editor_creation_date_edited";

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
	private TextView creationTimestampView;

	private long editedCreationTimestamp;
	private boolean isCreationTimestampDirtied;

	protected PointsGroup selectedGroup;
	protected PointBitmapDrawer pointDrawer;
	protected boolean skipConfirmationDialog;

	@Override
	protected int getLayoutId() {
		return R.layout.point_editor_fragment;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		if (selectedGroup != null) {
			outState.putString(SELECTED_GROUP_KEY, selectedGroup.getName());
		}
		outState.putLong(CREATION_TIMESTAMP_KEY, editedCreationTimestamp);
		outState.putBoolean(CREATION_TIMESTAMP_EDITED_KEY, isCreationTimestampDirtied);
	}

	@Override
	protected void restoreEditorState(@NonNull Bundle savedInstanceState) {
		super.restoreEditorState(savedInstanceState);
		String groupName = savedInstanceState.getString(SELECTED_GROUP_KEY);
		if (groupName != null) {
			for (PointsGroup group : getPointsGroups().values()) {
				if (groupName.equals(group.getName())) {
					selectedGroup = group;
					break;
				}
			}
		}
		if (savedInstanceState.containsKey(CREATION_TIMESTAMP_KEY)) {
			editedCreationTimestamp = savedInstanceState.getLong(CREATION_TIMESTAMP_KEY);
			isCreationTimestampDirtied = savedInstanceState.getBoolean(CREATION_TIMESTAMP_EDITED_KEY);
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		Context context = requireContext();

		view = super.onCreateView(inflater, container, savedInstanceState);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);

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
		setupCreationTimestamp(activeColor);

		if (app.accessibilityEnabled()) {
			nameCaption.setFocusable(true);
			nameEdit.setHint(R.string.access_hint_enter_name);
		}

		View deleteButton = view.findViewById(R.id.button_delete_container);
		deleteButton.setOnClickListener(v -> deletePressed());

		PointEditor editor = getEditor();
		if (editor == null) {
			return view;
		}
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

		descriptionEdit.setOnTouchListener((v, event) -> {
			descriptionEdit.getParent().requestDisallowInterceptTouchEvent(true);
			return false;
		});
		drawPointImage();

		return view;
	}

	@Override
	public InsetTargetsCollection getInsetTargets() {
		InsetTargetsCollection collection = super.getInsetTargets();

		if (getActionBarActivity() instanceof MyPlacesActivity) {
			collection.removeType(Type.ROOT_INSET);
		} else {
			collection = super.getInsetTargets();
			collection.add(InsetTarget.createScrollable(R.id.editor_scroll_view).build());
		}
		return collection;
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
	protected void onScrollViewTouched(@NonNull View scrollView, @NonNull MotionEvent event) {
		if (descriptionEdit != null) {
			descriptionEdit.getParent().requestDisallowInterceptTouchEvent(false);
		}
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
		CharSequence text = descriptionEdit.getText();
		if (!Algorithms.isEmpty(text) || descriptionEdit.hasFocus()) {
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

	private void setupCreationTimestamp(int activeColor) {
		View creationTimestampRow = view.findViewById(R.id.creation_date_row);
		creationTimestampView = view.findViewById(R.id.creation_date);
		PointEditor editor = getEditor();
		long timestamp = getCreationTimestamp();
		boolean visible = timestamp != 0 && (editor == null || !editor.isNew()) && isCreationTimestampEditable();
		AndroidUiHelper.updateVisibility(creationTimestampRow, visible);
		if (visible) {
			if (!isCreationTimestampDirtied) {
				editedCreationTimestamp = timestamp;
			}
			updateCreationTimestampText();

			View.OnClickListener listener = v -> showCreationTimestampDialog();
			creationTimestampRow.setOnClickListener(listener);
			creationTimestampView.setOnClickListener(listener);

			TextInputLayout creationTimestampCaption = view.findViewById(R.id.creation_date_caption);
			creationTimestampCaption.setEndIconDrawable(getPaintedIcon(R.drawable.ic_action_edit_outlined, activeColor));
			creationTimestampCaption.setEndIconOnClickListener(listener);
			creationTimestampCaption.setHelperText(getString(R.string.showing_in_timezone, getLocalTimeZoneName()));
		}
	}

	private void updateCreationTimestampText() {
		if (creationTimestampView != null) {
			DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(creationTimestampView.getContext());
			DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(creationTimestampView.getContext());
			Date date = new Date(editedCreationTimestamp);
			creationTimestampView.setText(dateFormat.format(date) + " — " + timeFormat.format(date));
		}
	}

	private void showCreationTimestampDialog() {
		Context context = requireContext();
		Context themedContext = UiUtilities.getThemedContext(context, nightMode);
		View dialogView = UiUtilities.getInflater(context, nightMode)
				.inflate(R.layout.dialog_date_time_picker, null);
		DatePicker datePicker = dialogView.findViewById(R.id.date_picker);
		TimePicker timePicker = dialogView.findViewById(R.id.time_picker);
		TextView timezoneSubtext = dialogView.findViewById(R.id.timezone_subtext);
		timezoneSubtext.setText(getString(R.string.showing_in_timezone, getLocalTimeZoneName()));

		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(editedCreationTimestamp);
		timePicker.setIs24HourView(android.text.format.DateFormat.is24HourFormat(context));
		timePicker.setHour(calendar.get(Calendar.HOUR_OF_DAY));
		timePicker.setMinute(calendar.get(Calendar.MINUTE));

		View occurrenceContainer = dialogView.findViewById(R.id.dst_occurrence_container);
		RadioGroup occurrenceGroup = dialogView.findViewById(R.id.dst_occurrence_group);
		RadioButton firstOccurrence = dialogView.findViewById(R.id.dst_occurrence_first);
		RadioButton secondOccurrence = dialogView.findViewById(R.id.dst_occurrence_second);
		TextView gapMessage = dialogView.findViewById(R.id.dst_gap_message);

		// The offset the user picked for the ambiguous hour, kept while the time stays ambiguous
		ZoneOffset[] selectedOffset = new ZoneOffset[1];

		AlertDialog dialog = new AlertDialog.Builder(themedContext)
				.setTitle(R.string.created_on)
				.setView(dialogView)
				.setNegativeButton(R.string.shared_string_cancel, null)
				.setPositiveButton(R.string.shared_string_ok, null)
				.create();

		Runnable updateState = () -> {
			LocalDateTime localTime = getSelectedLocalDateTime(datePicker, timePicker);
			List<ZoneOffset> offsets = ZoneId.systemDefault().getRules().getValidOffsets(localTime);

			boolean ambiguous = offsets.size() > 1;
			boolean nonexistent = offsets.isEmpty();
			AndroidUiHelper.updateVisibility(occurrenceContainer, ambiguous);
			AndroidUiHelper.updateVisibility(gapMessage, nonexistent);

			if (ambiguous) {
				// Keep the previous choice if it is still one of the valid offsets
				if (selectedOffset[0] == null || !offsets.contains(selectedOffset[0])) {
					selectedOffset[0] = offsets.get(0);
				}
				firstOccurrence.setText(getString(R.string.dst_time_occurs_twice_first,
						formatOffset(offsets.get(0))));
				secondOccurrence.setText(getString(R.string.dst_time_occurs_twice_second,
						formatOffset(offsets.get(1))));
				int checkedId = selectedOffset[0].equals(offsets.get(0))
						? R.id.dst_occurrence_first : R.id.dst_occurrence_second;
				if (occurrenceGroup.getCheckedRadioButtonId() != checkedId) {
					occurrenceGroup.check(checkedId);
				}
			} else {
				selectedOffset[0] = offsets.isEmpty() ? null : offsets.get(0);
			}

			Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
			if (okButton != null) {
				okButton.setEnabled(!nonexistent);
			}
		};

		DatePicker.OnDateChangedListener dateChangedListener = (picker, year, month, day) -> updateState.run();
		timePicker.setOnTimeChangedListener((picker, hour, minute) -> updateState.run());
		occurrenceGroup.setOnCheckedChangeListener((group, checkedId) -> {
			LocalDateTime localTime = getSelectedLocalDateTime(datePicker, timePicker);
			List<ZoneOffset> offsets = ZoneId.systemDefault().getRules().getValidOffsets(localTime);
			if (offsets.size() > 1) {
				selectedOffset[0] = checkedId == R.id.dst_occurrence_first ? offsets.get(0) : offsets.get(1);
			}
		});

		dialog.setOnShowListener(d -> {
			updateState.run();
			dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
				LocalDateTime localTime = getSelectedLocalDateTime(datePicker, timePicker);
				List<ZoneOffset> offsets = ZoneId.systemDefault().getRules().getValidOffsets(localTime);
				if (offsets.isEmpty()) {
					return;
				}
				ZoneOffset offset = selectedOffset[0] != null && offsets.contains(selectedOffset[0])
						? selectedOffset[0] : offsets.get(0);
				editedCreationTimestamp = localTime.atOffset(offset).toInstant().toEpochMilli();
				isCreationTimestampDirtied = editedCreationTimestamp != getCreationTimestamp();
				updateCreationTimestampText();
				dialog.dismiss();
			});
		});

		// Registering the date listener also refreshes the state for the initial values
		datePicker.init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
				calendar.get(Calendar.DAY_OF_MONTH), dateChangedListener);
		dialog.show();
		updateState.run();
	}

	@NonNull
	private static LocalDateTime getSelectedLocalDateTime(@NonNull DatePicker datePicker, @NonNull TimePicker timePicker) {
		return LocalDateTime.of(datePicker.getYear(), datePicker.getMonth() + 1, datePicker.getDayOfMonth(),
				timePicker.getHour(), timePicker.getMinute());
	}

	/**
	 * @return offset formatted as "UTC+2" or "UTC+5:30"
	 */
	@NonNull
	private static String formatOffset(@NonNull ZoneOffset offset) {
		int totalMinutes = offset.getTotalSeconds() / 60;
		int absMinutes = Math.abs(totalMinutes);
		int hours = absMinutes / 60;
		int minutes = absMinutes % 60;
		StringBuilder sb = new StringBuilder("UTC");
		sb.append(totalMinutes < 0 ? '-' : '+').append(hours);
		if (minutes != 0) {
			sb.append(':').append(minutes < 10 ? "0" : "").append(minutes);
		}
		return sb.toString();
	}

	/**
	 * @return ID of the device time zone with its current offset, e.g. "Africa/Casablanca (UTC+1)"
	 */
	@NonNull
	private String getLocalTimeZoneName() {
		// The ID is always available, unlike the display name which describes the standard
		// offset only and can be misleading for zones with a DST rule (e.g. Africa/Casablanca)
		TimeZone timeZone = TimeZone.getDefault();
		return timeZone.getID() + " (" + formatTimeZoneOffset(timeZone) + ")";
	}

	/**
	 * Formats the current offset of the given time zone, e.g. "UTC+1", "UTC+5:30" or "UTC-3:30".
	 * The current offset is used rather than the raw offset, so it accounts for daylight saving time.
	 */
	@NonNull
	private static String formatTimeZoneOffset(@NonNull TimeZone timeZone) {
		int offsetMinutes = timeZone.getOffset(System.currentTimeMillis()) / 60000;
		int absMinutes = Math.abs(offsetMinutes);
		int hours = absMinutes / 60;
		int minutes = absMinutes % 60;
		StringBuilder sb = new StringBuilder("UTC");
		sb.append(offsetMinutes < 0 ? '-' : '+').append(hours);
		if (minutes != 0) {
			sb.append(':').append(minutes < 10 ? "0" : "").append(minutes);
		}
		return sb.toString();
	}

	/**
	 * @return true if the user changed the creation timestamp in the editor
	 */
	protected boolean isCreationTimestampEdited() {
		return isCreationTimestampDirtied;
	}

	/**
	 * @return the creation timestamp currently selected in the editor
	 */
	protected long getEditedCreationTimestamp() {
		return isCreationTimestampDirtied ? editedCreationTimestamp : getCreationTimestamp();
	}

	/**
	 * @return creation timestamp of the edited point in milliseconds, or 0 if it is unknown
	 */
	protected long getCreationTimestamp() {
		return 0;
	}

	/**
	 * @return true if the creation timestamp of the edited point can be changed by the user.
	 * For a currently recording track we don't expose editing, because the attributes are
	 * persisted in a database rather than in an XML structure until the recording is finished,
	 * and handling that second serialization format is not worth it for this uncommon use case.
	 */
	protected boolean isCreationTimestampEditable() {
		return true;
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
			setColorSelected(false);
			selectColorInPalette();
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
			if (isPersonalCategoryDisplayName(app, selectedGroup.getName())) {
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
				setupGroupName(holder.groupName, group);
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

	protected void setupGroupName(@NonNull TextView groupName, @NonNull PointsGroup group) {
		groupName.setTag(null);
		groupName.setSingleLine(true);
		groupName.setMaxLines(1);
		groupName.setEllipsize(TextUtils.TruncateAt.END);
		groupName.setText(group.getName());
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