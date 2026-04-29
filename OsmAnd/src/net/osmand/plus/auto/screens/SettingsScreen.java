package net.osmand.plus.auto.screens;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.SectionedItemList;
import androidx.car.app.model.Template;
import androidx.car.app.model.Toggle;
import androidx.core.graphics.drawable.IconCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.OsmandSettings;

/**
 * Settings screen demo.
 */
public final class SettingsScreen extends BaseAndroidAutoScreen {

	@NonNull
	final OsmandSettings osmandSettings;

	public SettingsScreen(@NonNull CarContext carContext) {
		super(carContext);
		osmandSettings = ((OsmandApplication) carContext.getApplicationContext()).getSettings();
	}

	@NonNull
	@Override
	public Template getTemplate() {
		ListTemplate.Builder templateBuilder = new ListTemplate.Builder();

		// Create 2 sections with three settings each.
        ItemList.Builder sectionABuilder = new ItemList.Builder();
        sectionABuilder.addItem(new Row.Builder()
				.setTitle(getCarContext().getString(R.string.voice_announcements))
				.setToggle(
						new Toggle.Builder(
								(value) -> osmandSettings.VOICE_MUTE.set(!value))
								.setChecked(!osmandSettings.VOICE_MUTE.get())
								.build())
				.build()
        );
		sectionABuilder.addItem(new Row.Builder()
				.setTitle(getCarContext().getString(R.string.display_distance_to_first_intermediate))
				.addText(getCarContext().getString(R.string.display_distance_to_first_intermediate_summary))
				.setToggle(
						new Toggle.Builder(osmandSettings.USE_LEFT_DISTANCE_TO_INTERMEDIATE::set)
								.setChecked(osmandSettings.USE_LEFT_DISTANCE_TO_INTERMEDIATE.get())
								.build())
				.build()
		);

		templateBuilder.addSectionedList(
				SectionedItemList.create(
						sectionABuilder.build(),
						getCarContext().getString(R.string.shared_string_navigation)));

		CarIcon icon = new CarIcon.Builder(IconCompat.createWithResource(getApp(), R.drawable.ic_action_map_magnifier)).build();
		Row.Builder magnifierRowBuilder = new Row.Builder()
				.setTitle(getCarContext().getString(R.string.map_magnifier))
				.setImage(icon)
				.setBrowsable(true)
				.setOnClickListener(() -> {
					getScreenManager().push(new MapMagnifierScreen(getCarContext()));
				});

		ItemList.Builder configureMapSectionBuilder = new ItemList.Builder();
		configureMapSectionBuilder.addItem(magnifierRowBuilder.build());

		templateBuilder.addSectionedList(
				SectionedItemList.create(
						configureMapSectionBuilder.build(),
						getCarContext().getString(R.string.configure_map)));

        /*
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
}
