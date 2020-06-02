package net.osmand.plus.dialogs;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemTwoChoicesButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.ShortDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.OsmandSettings.CommonPreference;
import net.osmand.plus.settings.bottomsheets.BasePreferenceBottomSheet;
import net.osmand.render.RenderingRule;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRuleStorageProperties;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static net.osmand.plus.transport.TransportLinesMenu.RENDERING_CATEGORY_TRANSPORT;
import static net.osmand.render.RenderingRuleStorageProperties.UI_CATEGORY_DETAILS;

public class DetailsBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = DetailsBottomSheet.class.getName();
	private List<RenderingRuleProperty> properties;
	private List<CommonPreference<Boolean>> preferences;
	private ArrayAdapter<?> arrayAdapter;
	private ContextMenuAdapter adapter;
	private int position;

	public static void showInstance(@NonNull FragmentManager fm,
									List<RenderingRuleProperty> properties,
									List<CommonPreference<Boolean>> preferences,
									ArrayAdapter<?> arrayAdapter,
									ContextMenuAdapter adapter,
									int position) {
		DetailsBottomSheet bottomSheet = new DetailsBottomSheet();
		bottomSheet.setProperties(properties);
		bottomSheet.setPreferences(preferences);
		bottomSheet.setAdapter(adapter);
		bottomSheet.setPosition(position);
		bottomSheet.setArrayAdapter(arrayAdapter);
		bottomSheet.show(fm, TAG);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (properties == null || preferences == null) {
			properties = new ArrayList<>();
			preferences = new ArrayList<>();
			List<RenderingRuleProperty> customRules = ConfigureMapMenu.getCustomRules(requiredMyApplication(),
					RenderingRuleStorageProperties.UI_CATEGORY_HIDDEN, RENDERING_CATEGORY_TRANSPORT);
			for (RenderingRuleProperty pr : customRules) {
				if (UI_CATEGORY_DETAILS.equals(pr.getCategory()) && pr.isBoolean()) {
					properties.add(pr);
					final OsmandSettings.CommonPreference<Boolean> pref = requiredMyApplication().getSettings()
							.getCustomRenderBooleanProperty(pr.getAttrName());
					preferences.add(pref);
				}
			}
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		int selectedProfileColorRes = requiredMyApplication().getSettings().APPLICATION_MODE.get().getIconColorInfo().getColor(nightMode);
		TitleItem titleItem = new TitleItem(getString(R.string.rendering_category_details));
		items.add(titleItem);
		ShortDescriptionItem descriptionItem = new ShortDescriptionItem(getString(R.string.details_dialog_decr));
		items.add(descriptionItem);
		if (preferences != null && properties != null) {
			for (int i = 0; i < properties.size(); i++) {
				RenderingRuleProperty property = properties.get(i);
				RenderingRuleProperty nextProperty = i + 1 < properties.size() - 1 ? properties.get(i + 1) : null;
				final CommonPreference<Boolean> pref = preferences.get(i);
				final CommonPreference<Boolean> nextPref = i + 1 < preferences.size() - 1 ? preferences.get(i + 1) : null;
				final int tag = i;
				String attrName = property.getAttrName();
				boolean showDivider = "moreDetailed".equals(attrName) || "showSurfaceGrade".equals(attrName) || "coloredBuildings".equals(attrName) || "streetLighting".equals(attrName);
				if ("streetLighting".equals(property.getAttrName())
						&& nextProperty != null
						&& "streetLightingNight".equals(nextProperty.getAttrName())
						&& nextPref != null) {
					BaseBottomSheetItem item = new BottomSheetItemTwoChoicesButton.Builder()
							.setLeftBtnSelected(!nextPref.get())
							.setLeftBtnTitleRes(R.string.shared_string_all_time)
							.setRightBtnTitleRes(R.string.shared_string_night_map)
							.setOnBottomBtnClickListener(new BottomSheetItemTwoChoicesButton.OnBottomBtnClickListener() {
								@Override
								public void onBottomBtnClick(boolean onLeftClick) {
									nextPref.set(!onLeftClick);
								}
							})
							.setCompoundButtonColorId(selectedProfileColorRes)
							.setChecked(pref.get())
							.setTitle(property.getName())
							.setIconHidden(true)
							.setShowDivider(showDivider)
							.setLayoutId(R.layout.bottom_sheet_item_with_switch)
							.setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View view) {
									boolean checked = !pref.get();
									pref.set(checked);
									nextPref.set(false);
									updateItem(tag);
								}
							})
							.setTag(tag)
							.create();
					items.add(item);
				} else if (!"streetLightingNight".equals(property.getAttrName())) {
					BaseBottomSheetItem item = new BottomSheetItemWithCompoundButton.Builder()
							.setCompoundButtonColorId(selectedProfileColorRes)
							.setChecked(pref.get())
							.setTitle(property.getName())
							.setIconHidden(true)
							.setShowDivider(showDivider)
							.setLayoutId(R.layout.bottom_sheet_item_with_switch)
							.setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View view) {
									boolean checked = !pref.get();
									pref.set(checked);
									updateItem(tag);
								}
							})
							.setTag(tag)
							.create();
					items.add(item);
				}
			}
		}
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		boolean checked = false;
		int selected = 0;
		for (int i = 0; i < preferences.size(); i++) {
			boolean active = preferences.get(i).get();
			checked |= active;
			if (active) {
				selected++;
			}
		}
		if (adapter != null) {
			adapter.getItem(position).setSelected(checked);
			adapter.getItem(position).setColorRes(checked ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
			adapter.getItem(position).setDescription(getString(
					R.string.ltr_or_rtl_combine_via_slash,
					String.valueOf(selected),
					String.valueOf(preferences.size())));
		}
		if (arrayAdapter != null) {
			arrayAdapter.notifyDataSetInvalidated();
		}
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			MapActivity a = (MapActivity) activity;
			a.getMyApplication().getResourceManager().getRenderer().clearCache();
			a.updateMapSettings();
			a.getMapView().refreshMap(true);
			a.getMapLayers().updateLayers(a.getMapView());
		}
		super.onDismiss(dialog);
	}

	private void updateItem(int tag) {
		for (BaseBottomSheetItem item : items) {
			Object itemTag = item.getTag();
			if (itemTag instanceof Integer && ((Integer) itemTag) == tag) {
				((BottomSheetItemWithCompoundButton) item).setChecked(preferences.get(tag).get());
			}
		}
	}

	public void setProperties(List<RenderingRuleProperty> properties) {
		this.properties = properties;
	}

	public void setPreferences(List<CommonPreference<Boolean>> preferences) {
		this.preferences = preferences;
	}

	public void setAdapter(ContextMenuAdapter adapter) {
		this.adapter = adapter;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public void setArrayAdapter(ArrayAdapter<?> arrayAdapter) {
		this.arrayAdapter = arrayAdapter;
	}
}
