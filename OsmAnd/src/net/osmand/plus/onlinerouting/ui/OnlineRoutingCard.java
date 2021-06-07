package net.osmand.plus.onlinerouting.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter.HorizontalSelectionAdapterListener;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter.HorizontalSelectionItem;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.widgets.OsmandTextFieldBoxes;

import java.util.List;

public class OnlineRoutingCard extends MapBaseCard {

	private View headerContainer;
	private TextView tvHeaderTitle;
	private TextView tvHeaderSubtitle;
	private RecyclerView rvSelectionMenu;
	private HorizontalSelectionAdapter adapter;
	private TextView tvDescription;
	private View fieldBoxContainer;
	private OsmandTextFieldBoxes textFieldBoxes;
	private EditText editText;
	private TextView tvHelperText;
	private TextView tvErrorText;
	private View bottomDivider;
	private View button;
	private OnTextChangedListener onTextChangedListener;
	private boolean fieldBoxHelperTextShowed;

	private ApplicationMode appMode;

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
		rvSelectionMenu = view.findViewById(R.id.selection_menu);
		tvDescription = view.findViewById(R.id.description);
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

		editText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				if (onTextChangedListener != null) {
					boolean editedByUser = editText.getTag() == null;
					String text = editText.getText().toString().trim();
					onTextChangedListener.onTextChanged(editedByUser, text);
				}
			}
		});

		editText.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					editText.setSelection(editText.getText().length());
					AndroidUtils.showSoftKeyboard(getMapActivity(), editText);
				}
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

	public void setSelectionMenu(@NonNull List<HorizontalSelectionItem> items,
								 @NonNull String selectedItemTitle,
								 @NonNull final CallbackWithObject<HorizontalSelectionItem> callback) {
		showElements(rvSelectionMenu);
		rvSelectionMenu.setLayoutManager(
				new LinearLayoutManager(app, RecyclerView.HORIZONTAL, false));
		adapter = new HorizontalSelectionAdapter(app, nightMode);
		adapter.setItems(items);
		adapter.setSelectedItemByTitle(selectedItemTitle);
		adapter.setListener(new HorizontalSelectionAdapterListener() {
			@Override
			public void onItemSelected(HorizontalSelectionItem item) {
				if (callback.processResult(item)) {
					adapter.setSelectedItem(item);
				}
			}
		});
		rvSelectionMenu.setAdapter(adapter);
	}

	private void updateBottomMarginSelectionMenu() {
		ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) rvSelectionMenu.getLayoutParams();
		int contentPadding = app.getResources().getDimensionPixelSize(R.dimen.content_padding);
		params.bottomMargin = isVisibleViewsBelowSelectionMenu() ? contentPadding : 0;
	}

	public void setDescription(@NonNull String description) {
		showElements(tvDescription);
		tvDescription.setText(description);
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
		showElements(fieldBoxContainer);
		editText.setTag("");    // indicate that the text was edited programmatically
		editText.setText(text);
		editText.setTag(null);
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
		UiUtilities.setupDialogButton(nightMode, button, DialogButtonType.PRIMARY, title);
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
