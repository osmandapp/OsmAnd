package net.osmand.plus.osmedit;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.IconsCache;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.osmedit.EditPoiFragment.Tag;

public class NormalDataFragment extends Fragment {
	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		IconsCache iconsCache = ((MapActivity) getActivity()).getMyApplication().getIconsCache();
		View view = inflater.inflate(R.layout.fragment_edit_poi_normal, container, false);

		ImageView streetImageView = (ImageView) view.findViewById(R.id.streetImageView);
		streetImageView.setImageDrawable(iconsCache.getContentIcon(R.drawable.ic_action_street_name));
		ImageView houseNumberImageView = (ImageView) view.findViewById(R.id.houseNumberImageView);
		houseNumberImageView.setImageDrawable(iconsCache.getContentIcon(R.drawable.ic_action_building_number));
		ImageView phoneImageView = (ImageView) view.findViewById(R.id.phoneImageView);
		phoneImageView.setImageDrawable(iconsCache.getContentIcon(R.drawable.ic_action_call_dark));
		ImageView webSiteImageView = (ImageView) view.findViewById(R.id.webSiteImageView);
		webSiteImageView.setImageDrawable(iconsCache.getContentIcon(R.drawable.ic_world_globe_dark));
		ImageView descriptionImageView = (ImageView) view.findViewById(R.id.descriptionImageView);
		descriptionImageView.setImageDrawable(iconsCache.getContentIcon(R.drawable.ic_action_description));

		// TODO replace with constants
		final TextView streetEditText = (TextView) view.findViewById(R.id.streetEditText);
		streetEditText.setOnFocusChangeListener(new MyOnFocusChangeListener(getData(), "addr:street"));
		final TextView houseNumberEditText = (TextView) view.findViewById(R.id.houseNumberEditText);
		houseNumberEditText.setOnFocusChangeListener(new MyOnFocusChangeListener(getData(), "addr:housenumber"));
		final TextView phoneEditText = (TextView) view.findViewById(R.id.phoneEditText);
		phoneEditText.setOnFocusChangeListener(new MyOnFocusChangeListener(getData(), "phone"));
		final TextView webSiteEditText = (TextView) view.findViewById(R.id.webSiteEditText);
		webSiteEditText.setOnFocusChangeListener(new MyOnFocusChangeListener(getData(), "website"));
		final TextView descriptionEditText = (TextView) view.findViewById(R.id.descriptionEditText);
		descriptionEditText.setOnFocusChangeListener(new MyOnFocusChangeListener(getData(), "description"));

		Button saveButton = (Button) view.findViewById(R.id.saveButton);
		int saveButtonTextId = //getData().isLocalEdit ? R.string.shared_string_save :
				R.string.default_buttons_commit;
		saveButton.setText(saveButtonTextId);
		saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getEditPoiFragment().send();
			}
		});
		Button cancelButton = (Button) view.findViewById(R.id.cancelButton);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
				Fragment editPoiFragment = getParentFragment();
				fragmentManager.beginTransaction().remove(editPoiFragment).commit();
				fragmentManager.popBackStack();
			}
		});
		return view;
	}

	private EditPoiFragment getEditPoiFragment() {
		return (EditPoiFragment) getParentFragment();
	}

	private EditPoiFragment.EditPoiData getData() {
		return getEditPoiFragment().getEditPoiData();
	}

	private static class MyOnFocusChangeListener implements View.OnFocusChangeListener {
		private EditPoiFragment.EditPoiData data;
		private String tagName;

		public MyOnFocusChangeListener(EditPoiFragment.EditPoiData data, String tagName) {
			this.data = data;
			this.tagName = tagName;
		}

		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			String string = ((EditText) v).getText().toString();
			if (!TextUtils.isEmpty(string)) {
				Tag tag = new Tag(tagName, string);
				data.tags.remove(tag);
				data.tags.add(tag);
			}
		}
	}
}
