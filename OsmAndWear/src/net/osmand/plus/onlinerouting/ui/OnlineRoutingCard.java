package net.osmand.plus.onlinerouting.ui;

import android.text.Editable;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.CompoundButtonType;
import net.osmand.plus.widgets.OsmandTextFieldBoxes;
import net.osmand.plus.widgets.chips.ChipItem;
import net.osmand.plus.widgets.chips.HorizontalChipsView;

import java.util.List;

public class OnlineRoutingCard extends MapBaseCard {

	private View headerContainer;
	private TextView tvHeaderTitle;
	private TextView tvHeaderSubtitle;
	private HorizontalChipsView chipsView;
	private TextView tvDescription;
	private View checkBoxContainer;
	private CheckBox checkBox;
	private TextView tvCheckBoxDescription;
	private View fieldBoxContainer;
	private OsmandTextFieldBoxes textFieldBoxes;
	private EditText editText;
	private TextView tvHelperText;
	private TextView tvErrorText;
	private View bottomDivider;
	private DialogButton button;
	private OnTextChangedListener onTextChangedListener;
	private boolean fieldBoxHelperTextShowed;

	private final ApplicationMode appMode;

	public OnlineRoutingCard(@NonNull MapActivity mapActivity, boolean nightMode, ApplicationMode appMode) {
		super(mapActivity);
		this.nightMode = nightMode;
		this.appMode = appMode;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.online_routing_preference_segment;
	}

	@Override
	protected void updateContent() {
		headerContainer = view.findViewById(R.id.header);
		tvHeaderTitle = view.findViewById(R.id.title);
		tvHeaderSubtitle = view.findViewById(R.id.subtitle);
		chipsView = view.findViewById(R.id.selection_menu);
		tvDescription = view.findViewById(R.id.description);
		checkBoxContainer = view.findViewById(R.id.checkbox_container);
		checkBox = view.findViewById(R.id.checkbox);
		tvCheckBoxDescription = view.findViewById(R.id.checkbox_description);
		fieldBoxContainer = view.findViewById(R.id.field_box_container);
		textFieldBoxes = view.findViewById(R.id.field_box);
		editText = view.findViewById(R.id.edit_text);
		tvHelperText = view.findViewById(R.id.helper_text);
		tvErrorText = view.findViewById(R.id.error_text);
		bottomDivider = view.findViewById(R.id.bottom_divider);
		button = view.findViewById(R.id.button);

		int activeColor = appMode.getProfileColor(nightMode);
		textFieldBoxes.setPrimaryColor(activeColor);
		textFieldBoxes.setGravityFloatingLabel(Gravity.START);

		editText.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				if (onTextChangedListener != null) {
					boolean editedByUser = editText.getTag() == null;
					String text = editText.getText().toString().trim();
					onTextChangedListener.onTextChanged(editedByUser, text);
				}
			}
		});

		editText.setOnFocusChangeListener((v, hasFocus) -> {
			if (hasFocus) {
				editText.setSelection(editText.getText().length());
				AndroidUtils.showSoftKeyboard(getMapActivity(), editText);
			}
		});
	}

	public void setHeaderTitle(@NonNull String title) {
		showElements(headerContainer, tvHeaderTitle);
		tvHeaderTitle.setText(title);
	}

	public void setHeaderSubtitle(@NonNull String subtitle) {
		showElements(headerContainer, tvHeaderSubtitle);
		tvHeaderSubtitle.setText(subtitle);
	}

	public void setSelectionMenu(@NonNull List<ChipItem> items,
								 @NonNull String selectedId,
								 @NonNull CallbackWithObject<ChipItem> callback) {
		showElements(chipsView);
		chipsView.setItems(items);
		ChipItem selected = chipsView.getChipById(selectedId);
		chipsView.setSelected(selected);
		chipsView.setOnSelectChipListener(chip -> {
			chipsView.smoothScrollTo(chip);
			callback.processResult(chip);
			return true;
		});
		chipsView.notifyDataSetChanged();
	}

	private void updateBottomMarginSelectionMenu() {
		ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) chipsView.getLayoutParams();
		int contentPadding = getDimen(R.dimen.content_padding);
		params.bottomMargin = isVisibleViewsBelowSelectionMenu() ? contentPadding : 0;
	}

	public void setDescription(@NonNull String description) {
		showElements(tvDescription);
		tvDescription.setText(description);
	}

	public void onClickCheckBox(@NonNull String title, @NonNull CallbackWithObject<Boolean> callback) {
		showElements(checkBoxContainer);
		tvCheckBoxDescription.setText(title);
		UiUtilities.setupCompoundButton(checkBox, nightMode, CompoundButtonType.GLOBAL);
		checkBoxContainer.setOnClickListener(v -> {
			callback.processResult(checkBox.isChecked());
		});
	}

	public void setCheckBox(boolean checked) {
		checkBox.setChecked(checked);
	}

	public void setCheckBox(@NonNull String title, boolean checked,
	                        @NonNull CallbackWithObject<Boolean> callback) {
		showElements(checkBoxContainer);
		tvCheckBoxDescription.setText(title);
		checkBoxContainer.setOnClickListener(v -> {
			checkBox.setChecked(!checkBox.isChecked());
			callback.processResult(checkBox.isChecked());
		});
		UiUtilities.setupCompoundButton(checkBox, nightMode, CompoundButtonType.GLOBAL);
		checkBox.setChecked(checked);
	}

	public void setFieldBoxLabelText(@NonNull String labelText) {
		showElements(fieldBoxContainer);
		textFieldBoxes.setLabelText(labelText);
	}

	public void hideFieldBoxLabel() {
		textFieldBoxes.makeCompactPadding();
	}

	public void setFieldBoxHelperText(@NonNull String helperText) {
		showElements(fieldBoxContainer, tvHelperText);
		fieldBoxHelperTextShowed = true;
		tvHelperText.setText(helperText);
	}

	public void showFieldBoxError(@NonNull String errorText) {
		showElements(fieldBoxContainer, tvErrorText);
		hideElements(tvHelperText);
		tvErrorText.setText(errorText);
	}

	public void hideFieldBoxError() {
		hideElements(tvErrorText);
		if (fieldBoxHelperTextShowed) {
			showElements(tvHelperText);
		}
	}

	public void setEditedText(@NonNull String text) {
		setEditedText(text, true);
	}

	public void setEditedText(@NonNull String text, boolean enabled) {
		showElements(fieldBoxContainer);
		editText.setTag("");    // indicate that the text was edited programmatically
		editText.setText(text);
		editText.setTag(null);
		editText.setEnabled(enabled);
	}

	@NonNull
	public String getEditedText() {
		return editText.getText().toString();
	}

	public void showDivider() {
		showElements(bottomDivider);
	}

	public void setButton(@NonNull String title,
						  @NonNull OnClickListener listener) {
		showElements(button);
		button.setOnClickListener(listener);
		button.setTitle(title);
	}

	public void show() {
		showElements(view);
	}

	public void hide() {
		hideElements(view);
	}

	public void showFieldBox() {
		showElements(fieldBoxContainer);
	}

	public void hideFieldBox() {
		hideElements(fieldBoxContainer);
	}

	private void showElements(View... views) {
		AndroidUiHelper.setVisibility(View.VISIBLE, views);
		updateBottomMarginSelectionMenu();
	}

	private void hideElements(View... views) {
		AndroidUiHelper.setVisibility(View.GONE, views);
		updateBottomMarginSelectionMenu();
	}

	private boolean isVisibleViewsBelowSelectionMenu() {
		return isVisible(tvDescription) || isVisible(fieldBoxContainer) || isVisible(button);
	}

	public boolean isVisible(View view) {
		return view.getVisibility() == View.VISIBLE;
	}

	public void setOnTextChangedListener(@Nullable OnTextChangedListener onTextChangedListener) {
		this.onTextChangedListener = onTextChangedListener;
	}

	public interface OnTextChangedListener {
		void onTextChanged(boolean editedByUser, @NonNull String text);
	}
}
