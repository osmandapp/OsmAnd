package net.osmand.plus.auto;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.model.Toggle;

import net.osmand.plus.R;

/**
 * Settings screen demo.
 */
public final class SettingsScreen extends Screen {

	@NonNull
	final SharedPreferences mSharedPref;

	SettingsScreen(@NonNull CarContext carContext) {
		super(carContext);
		mSharedPref = carContext.getSharedPreferences("SETTINGS", Context.MODE_PRIVATE);
	}

	@NonNull
	@Override
	public Template onGetTemplate() {
		ListTemplate.Builder templateBuilder = new ListTemplate.Builder();

		// Create 2 sections with three settings each.
        /*
        ItemList.Builder sectionABuilder = new ItemList.Builder();
        sectionABuilder.addItem(buildRow(R.string.settings_one_label, R.string.settings_one_pref));
        sectionABuilder.addItem(buildRow(R.string.settings_two_label, R.string.settings_two_pref));
        sectionABuilder.addItem(
                buildRow(R.string.settings_three_label, R.string.settings_three_pref));

        templateBuilder.addSectionedList(
                SectionedItemList.create(
                        sectionABuilder.build(),
                        getCarContext().getString(R.string.settings_section_a_label)));

        ItemList.Builder sectionBBuilder = new ItemList.Builder();
        sectionBBuilder.addItem(
                buildRow(R.string.settings_four_label, R.string.settings_four_pref));
        sectionBBuilder.addItem(
                buildRow(R.string.settings_five_label, R.string.settings_five_pref));
        sectionBBuilder.addItem(buildRow(R.string.settings_six_label, R.string.settings_six_pref));

        templateBuilder.addSectionedList(
                SectionedItemList.create(
                        sectionBBuilder.build(),
                        getCarContext().getString(R.string.settings_section_b_label)));

         */
		return templateBuilder
				.setHeaderAction(Action.BACK)
				.setTitle(getCarContext().getString(R.string.shared_string_settings))
				.build();
	}

	@NonNull
	private Row buildRow(int labelResourcee, int prefKeyResource) {
		return new Row.Builder()
				.setTitle(getCarContext().getString(labelResourcee))
				.setToggle(
						new Toggle.Builder(
								(value) -> {
									writeSharedPref(prefKeyResource, value);
								})
								.setChecked(readSharedPref(prefKeyResource, false))
								.build())
				.build();
	}

	private boolean readSharedPref(int keyResource, boolean defaultValue) {
		return mSharedPref.getBoolean(getCarContext().getString(keyResource), defaultValue);
	}

	private void writeSharedPref(int keyResource, boolean value) {
		SharedPreferences.Editor editor = mSharedPref.edit();
		editor.putBoolean(getCarContext().getString(keyResource), value);
		editor.commit();
	}
}
