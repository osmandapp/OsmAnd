package net.osmand.plus.plugins.osmedit.fragments.holders;

import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.osm.edit.OSMSettings;
import net.osmand.plus.R;
import net.osmand.plus.plugins.osmedit.data.EditPoiData;
import net.osmand.plus.plugins.osmedit.fragments.EditPoiContentAdapter.EditPoiListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;

import java.util.Map;

public class BasicInfoHolder extends RecyclerView.ViewHolder {

	private final EditText streetEditText;
	private final EditText houseNumberEditText;
	private final EditText phoneEditText;
	private final EditText webSiteEditText;
	private final EditText descriptionEditText;

	public BasicInfoHolder(@NonNull View itemView) {
		super(itemView);
		streetEditText = itemView.findViewById(R.id.streetEditText);
		houseNumberEditText = itemView.findViewById(R.id.houseNumberEditText);
		phoneEditText = itemView.findViewById(R.id.phoneEditText);
		webSiteEditText = itemView.findViewById(R.id.webSiteEditText);
		descriptionEditText = itemView.findViewById(R.id.descriptionEditText);
	}

	protected void addTextWatcher(String tag, EditText e, EditPoiData data, @NonNull EditPoiListener editPoiListener) {
		e.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				if (data != null && !data.isInEdit()) {
					if (!TextUtils.isEmpty(s)) {
						data.putTag(tag, s.toString());
					} else if (editPoiListener.isBasicTagsInitialized() && editPoiListener.isFragmentResumed()) {
						data.removeTag(tag);
					}
				}
			}
		});
	}

	public void bindView(EditPoiData data, @NonNull EditPoiListener editPoiListener) {
		addTextWatcher(OSMSettings.OSMTagKey.ADDR_STREET.getValue(), streetEditText, data, editPoiListener);
		addTextWatcher(OSMSettings.OSMTagKey.WEBSITE.getValue(), webSiteEditText, data, editPoiListener);
		addTextWatcher(OSMSettings.OSMTagKey.PHONE.getValue(), phoneEditText, data, editPoiListener);
		addTextWatcher(OSMSettings.OSMTagKey.ADDR_HOUSE_NUMBER.getValue(), houseNumberEditText, data, editPoiListener);
		addTextWatcher(OSMSettings.OSMTagKey.DESCRIPTION.getValue(), descriptionEditText, data, editPoiListener);
		InputFilter[] lengthLimit = editPoiListener.getLengthLimit();
		streetEditText.setFilters(lengthLimit);
		houseNumberEditText.setFilters(lengthLimit);
		phoneEditText.setFilters(lengthLimit);
		webSiteEditText.setFilters(lengthLimit);
		descriptionEditText.setFilters(lengthLimit);

		AndroidUtils.setTextHorizontalGravity(streetEditText, Gravity.START);
		AndroidUtils.setTextHorizontalGravity(houseNumberEditText, Gravity.START);
		AndroidUtils.setTextHorizontalGravity(phoneEditText, Gravity.START);
		AndroidUtils.setTextHorizontalGravity(webSiteEditText, Gravity.START);
		AndroidUtils.setTextHorizontalGravity(descriptionEditText, Gravity.START);

		if (data == null) {
			return;
		}
		Map<String, String> tagValues = data.getTagValues();
		streetEditText.setText(tagValues.get(OSMSettings.OSMTagKey.ADDR_STREET.getValue()));
		houseNumberEditText.setText(tagValues.get(OSMSettings.OSMTagKey.ADDR_HOUSE_NUMBER.getValue()));
		phoneEditText.setText(tagValues.get(OSMSettings.OSMTagKey.PHONE.getValue()));
		webSiteEditText.setText(tagValues.get(OSMSettings.OSMTagKey.WEBSITE.getValue()));
		descriptionEditText.setText(tagValues.get(OSMSettings.OSMTagKey.DESCRIPTION.getValue()));
	}
}