package net.osmand.plus.profiles.onlinerouting;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.CallbackWithObject;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter.HorizontalSelectionAdapterListener;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter.HorizontalSelectionItem;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.widgets.OsmandTextFieldBoxes;

import java.util.List;

public class OnlineRoutingSegmentCard extends BaseCard {

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
	private View bottomDivider;
	private View button;
	private View resultContainer;
	private OnTextChangedListener onTextChangedListener;

	public OnlineRoutingSegmentCard(@NonNull MapActivity mapActivity, boolean nightMode) {
		super(mapActivity);
		this.nightMode = nightMode;
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
		bottomDivider = view.findViewById(R.id.bottom_divider);
		button = view.findViewById(R.id.button);
		resultContainer = view.findViewById(R.id.result_container);

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
					String text = editText.getText().toString();
					onTextChangedListener.onTextChanged(editedByUser, text);
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

	public void setSelectionMenu(List<HorizontalSelectionItem> items,
	                             String selectedItemTitle,
	                             final CallbackWithObject<HorizontalSelectionItem> callback) {
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

	public void setDescription(@NonNull String description) {
		showElements(tvDescription);
		tvDescription.setText(description);
	}

	public void setFieldBoxLabelText(@NonNull String labelText) {
		showElements(fieldBoxContainer);
		textFieldBoxes.setLabelText(labelText);
	}

	public void setFieldBoxHelperText(@NonNull String helperText) {
		showElements(fieldBoxContainer, tvHelperText);
		tvHelperText.setText(helperText);
	}

	public void setEditedText(@NonNull String text) {
		editText.setTag("");    // needed to indicate that the text was edited programmatically
		editText.setText(text);
		editText.setTag(null);
	}

	public void showDivider() {
		showElements(bottomDivider);
	}

	public void setButton(OnClickListener listener) {
		showElements(button);
		button.setOnClickListener(listener);
		UiUtilities.setupDialogButton(nightMode, button,
				DialogButtonType.PRIMARY, R.string.test_route_calculation);
	}

	public void showFieldBox() {
		showElements(fieldBoxContainer);
	}

	public void hideFieldBox() {
		hideElements(fieldBoxContainer);
	}

	private void showElements(View... views) {
		AndroidUiHelper.setVisibility(View.VISIBLE, views);
	}

	private void hideElements(View... views) {
		AndroidUiHelper.setVisibility(View.GONE, views);
	}

	public void setOnTextChangedListener(OnTextChangedListener onTextChangedListener) {
		this.onTextChangedListener = onTextChangedListener;
	}

	public interface OnTextChangedListener {
		void onTextChanged(boolean editedByUser, String text);
	}
}
