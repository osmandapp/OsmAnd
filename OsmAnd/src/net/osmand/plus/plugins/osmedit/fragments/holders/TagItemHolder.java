package net.osmand.plus.plugins.osmedit.fragments.holders;

import static net.osmand.plus.plugins.osmedit.dialogs.EditPoiDialogFragment.AMENITY_TEXT_LENGTH;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.osmedit.data.EditPoiData;
import net.osmand.plus.plugins.osmedit.fragments.EditPoiContentAdapter.EditPoiAdapterListener;
import net.osmand.plus.plugins.osmedit.fragments.EditPoiContentAdapter.EditPoiListener;
import net.osmand.plus.plugins.osmedit.fragments.AdvancedEditPoiFragment.OsmTagsArrayAdapter;
import net.osmand.plus.plugins.osmedit.fragments.AdvancedEditPoiFragment.TagItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.OsmandTextFieldBoxes;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.util.Algorithms;

import studio.carbonylgroup.textfieldboxes.ExtendedEditText;

public class TagItemHolder extends RecyclerView.ViewHolder {

	private final OsmandTextFieldBoxes tagFB;
	private final OsmandTextFieldBoxes valueFB;
	private final ExtendedEditText tagEditText;
	private final AutoCompleteTextView valueEditText;
	private final View deleteButton;
	private final OsmTagsArrayAdapter tagAdapter;
	private final Activity activity;
	private final ArrayAdapter<String> valueAdapter;

	@Nullable
	private TagItem tagItem;
	@Nullable
	private EditPoiData data;
	@Nullable
	private EditPoiListener editPoiListener;
	@Nullable
	private EditPoiAdapterListener editPoiAdapterListener;

	private final TextWatcher tagWatcher = new SimpleTextWatcher() {
		@Override
		public void afterTextChanged(Editable s) {
			if (tagItem != null) {
				tagItem.setTag(s.toString());
			}
		}
	};

	private final TextWatcher valueWatcher = new SimpleTextWatcher() {
		@Override
		public void afterTextChanged(Editable s) {
			if (tagItem != null) {
				tagItem.setValue(s.toString());
				applyValue();
			}
		}
	};

	public TagItemHolder(@NonNull View itemView, @NonNull OsmandApplication app, @NonNull ArrayAdapter<String> valueAdapter,
	                     @NonNull OsmTagsArrayAdapter tagAdapter, @NonNull Activity activity, boolean nightMode) {
		super(itemView);
		this.tagAdapter = tagAdapter;
		this.valueAdapter = valueAdapter;
		this.activity = activity;

		tagFB = itemView.findViewById(R.id.tag_fb);
		valueFB = itemView.findViewById(R.id.value_fb);
		tagEditText = itemView.findViewById(R.id.tagEditText);
		valueEditText = itemView.findViewById(R.id.valueEditText);
		deleteButton = itemView.findViewById(R.id.delete_button);

		Drawable deleteDrawable = app.getUIUtilities().getIcon(R.drawable.ic_action_remove_dark, !nightMode);
		tagFB.setClearButton(deleteDrawable);
		valueFB.setClearButton(deleteDrawable);

		tagEditText.setAdapter(tagAdapter);
		tagEditText.setThreshold(1);

		valueEditText.setAdapter(valueAdapter);
		valueEditText.setThreshold(3);
		valueEditText.setFilters(new InputFilter[] {new InputFilter.LengthFilter(AMENITY_TEXT_LENGTH)});

		setupListeners();
	}

	private void setupListeners() {
		tagEditText.setOnFocusChangeListener((v, hasFocus) -> {
			updateCurrentTagEditText(hasFocus);
			tagFB.setHasClearButton(hasFocus);
			if (hasFocus) {
				tagAdapter.getFilter().filter(tagEditText.getText());
			} else {
				applyTag();
			}
		});

		valueEditText.setOnFocusChangeListener((v, hasFocus) -> {
			valueFB.setHasClearButton(hasFocus);
			if (hasFocus) {
				valueAdapter.getFilter().filter(valueEditText.getText());
			}
		});

		deleteButton.setOnClickListener(v -> deleteItem());
	}

	private void showKeyboard(@NonNull View view) {
		view.requestFocus();
		if (activity != null) {
			AndroidUtils.showSoftKeyboard(activity, view);
		}
	}

	public void focusOnTagEdit() {
		showKeyboard(tagEditText);
	}

	public void bindView(@NonNull TagItem tagItem, @NonNull EditPoiData data,
	                     @NonNull EditPoiListener editPoiListener,
	                     @NonNull EditPoiAdapterListener editPoiAdapterListener) {
		this.tagItem = tagItem;
		this.data = data;
		this.editPoiListener = editPoiListener;
		this.editPoiAdapterListener = editPoiAdapterListener;

		tagFB.setHasClearButton(false);
		valueFB.setHasClearButton(false);

		tagEditText.removeTextChangedListener(tagWatcher);
		tagEditText.setText(tagItem.getTag(), false);
		tagEditText.addTextChangedListener(tagWatcher);

		valueEditText.removeTextChangedListener(valueWatcher);
		valueEditText.setText(tagItem.getValue(), false);
		valueEditText.addTextChangedListener(valueWatcher);
	}

	private void applyTag() {
		if (tagItem == null || data == null || data.isInEdit()) {
			return;
		}
		String tag = tagItem.getTag();
		if (Algorithms.isEmpty(tag)) {
			return;
		}
		String appliedTag = tagItem.getAppliedTag();
		if (!Algorithms.stringsEqual(appliedTag, tag)) {
			if (!Algorithms.isEmpty(appliedTag)) {
				data.removeTag(appliedTag);
			}
			data.putTag(tag, tagItem.getValue());
			tagItem.setAppliedTag(tag);
		}
	}

	private void applyValue() {
		if (tagItem == null || data == null || data.isInEdit()) {
			return;
		}
		String tag = tagItem.getTag();
		if (!Algorithms.isEmpty(tag)) {
			data.putTag(tag, tagItem.getValue());
		}
	}

	private void deleteItem() {
		int position = getBindingAdapterPosition();
		TagItem deletedItem = tagItem;
		if (deletedItem == null || position == RecyclerView.NO_POSITION) {
			return;
		}
		tagItem = null;

		if (editPoiListener != null) {
			editPoiListener.onDeleteItem(position);
		}
		if (editPoiAdapterListener != null) {
			editPoiAdapterListener.removeItem(position);
		}
		String appliedTag = deletedItem.getAppliedTag();
		if (data != null && !data.isInEdit() && !Algorithms.isEmpty(appliedTag)) {
			data.removeTag(appliedTag);
		}
	}

	private void updateCurrentTagEditText(boolean hasFocus) {
		if (editPoiAdapterListener == null) {
			return;
		}
		if (hasFocus) {
			editPoiAdapterListener.setCurrentTagEditText(tagEditText);
		} else if (tagEditText.equals(editPoiAdapterListener.getCurrentTagEditText())) {
			editPoiAdapterListener.setCurrentTagEditText(null);
		}
	}
}
