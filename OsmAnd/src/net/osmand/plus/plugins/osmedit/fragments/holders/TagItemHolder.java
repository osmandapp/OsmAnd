package net.osmand.plus.plugins.osmedit.fragments.holders;

import static net.osmand.plus.plugins.osmedit.dialogs.EditPoiDialogFragment.AMENITY_TEXT_LENGTH;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.InputFilter;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
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

	public void bindView(@NonNull RecyclerView.ViewHolder holder, @NonNull TagItem tagItem, EditPoiData data, @NonNull EditPoiListener editPoiListener, @NonNull EditPoiAdapterListener editPoiAdapterListener) {
		String tag = tagItem.tag();
		String value = tagItem.value();

		tagFB.setHasClearButton(false);
		valueFB.setHasClearButton(false);

		tagEditText.setText(tag);

		String[] previousTag = {tag};
		tagEditText.setOnFocusChangeListener((v, hasFocus) -> {
			updateCurrentTagEditText(hasFocus, editPoiAdapterListener);
			tagFB.setHasClearButton(hasFocus);
			if (Algorithms.isEmpty(tagEditText.getText().toString())) {
				return;
			}
			if (!hasFocus) {
				if (!data.isInEdit()) {
					String s = tagEditText.getText().toString();
					if (!previousTag[0].equals(s)) {
						data.removeTag(previousTag[0]);
						data.putTag(s, valueEditText.getText().toString());
						previousTag[0] = s;
					}
				}
			} else {
				tagAdapter.getFilter().filter(tagEditText.getText());
			}
		});

		valueEditText.setText(value);

		valueEditText.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				if (!data.isInEdit()) {
					data.putTag(tagEditText.getText().toString(), s.toString());
				}
			}
		});

		valueEditText.setOnFocusChangeListener((v, hasFocus) -> {
			if (hasFocus) {
				valueFB.setHasClearButton(true);
				valueAdapter.getFilter().filter(valueEditText.getText());
			} else {
				valueFB.setHasClearButton(false);
			}
		});

		deleteButton.setOnClickListener(v -> {
			int itemPosition = holder.getAdapterPosition();
			editPoiListener.onDeleteItem(itemPosition);
			editPoiAdapterListener.removeItem(itemPosition);
			data.removeTag(tagEditText.getText().toString());
		});
	}

	private void updateCurrentTagEditText(boolean hasFocus, @NonNull EditPoiAdapterListener listener) {
		if (!hasFocus) {
			if (tagEditText.equals(listener.getCurrentTagEditText())) {
				listener.setCurrentTagEditText(null);
			}
		} else {
			listener.setCurrentTagEditText(tagEditText);
		}
	}
}



