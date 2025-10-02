package net.osmand.plus.dialogs;

import static net.osmand.osm.OsmRouteType.SKI;
import static net.osmand.plus.utils.UiUtilities.CompoundButtonType.PROFILE_DEPENDENT;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.FragmentManager;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemTitleWithDescrAndButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SubtitleDividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SelectMapStyleBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = SelectMapStyleBottomSheetDialogFragment.class.getSimpleName();

	private static final String SELECTED_STYLE_KEY = "selected_style";
	private static final int COLLAPSED_DESCRIPTION_LINES = 2;

	private boolean descriptionExpanded;

	private LinearLayout stylesContainer;
	private BottomSheetItemTitleWithDescrAndButton descrItem;
	private View.OnClickListener onStyleClickListener;

	private TreeMap<String, String> stylesMap;
	private String selectedStyle;
	@Nullable
	private SelectStyleListener selectStyleListener;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context themedContext = getThemedContext();
		stylesMap = generateStylesMap(themedContext);
		if (savedInstanceState == null) {
			RenderingRulesStorage current = app.getRendererRegistry().getCurrentSelectedRenderer();
			if (current != null) {
				selectedStyle = current.getName();
			}
		} else {
			selectedStyle = savedInstanceState.getString(SELECTED_STYLE_KEY);
		}
		if (selectedStyle == null) {
			selectedStyle = RendererRegistry.DEFAULT_RENDER;
		}

		items.add(new TitleItem(getString(R.string.map_widget_renderer)));

		descrItem = (BottomSheetItemTitleWithDescrAndButton) new BottomSheetItemTitleWithDescrAndButton.Builder()
				.setButtonTitle(getString(R.string.show_full_description))
				.setOnButtonClickListener(v -> {
					descriptionExpanded = !descriptionExpanded;
					descrItem.setButtonText(getString(descriptionExpanded
							? R.string.hide_full_description : R.string.show_full_description));
					descrItem.setDescriptionMaxLines(descriptionExpanded
							? Integer.MAX_VALUE : COLLAPSED_DESCRIPTION_LINES);
					setupHeightAndBackground(getView());
				})
				.setDescription(RendererRegistry.getRendererDescription(themedContext, selectedStyle))
				.setDescriptionMaxLines(COLLAPSED_DESCRIPTION_LINES)
				.setLayoutId(R.layout.bottom_sheet_item_with_expandable_descr)
				.create();
		items.add(descrItem);

		items.add(new SubtitleDividerItem(themedContext));

		NestedScrollView nestedScrollView = new NestedScrollView(themedContext);
		stylesContainer = new LinearLayout(themedContext);
		stylesContainer.setLayoutParams((new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)));
		stylesContainer.setOrientation(LinearLayout.VERTICAL);
		stylesContainer.setPadding(0, getDimensionPixelSize(R.dimen.bottom_sheet_content_padding_small), 0, 0);
		for (int i = 0; i < stylesMap.size(); i++) {
			inflate(R.layout.bottom_sheet_item_with_radio_btn_left, stylesContainer, true);
		}
		nestedScrollView.addView(stylesContainer);
		items.add(new BaseBottomSheetItem.Builder().setCustomView(nestedScrollView).create());

		populateStylesList();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(SELECTED_STYLE_KEY, selectedStyle);
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	protected void onRightBottomButtonClick() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) return;

		setStyle(mapActivity, selectedStyle);

		if (selectStyleListener != null) {
			selectStyleListener.onMapStyleSelected();
		}
		dismiss();
	}

	public static void setStyle(@NonNull MapActivity mapActivity, @NonNull String selectedStyle) {
		OsmandApplication app = mapActivity.getApp();
		RenderingRulesStorage loaded = app.getRendererRegistry().getRenderer(selectedStyle);
		if (loaded != null) {
			OsmandMapTileView view = mapActivity.getMapView();
			OsmandSettings settings = view.getSettings();
			settings.RENDERER.set(selectedStyle);
			CommonPreference<Boolean> pisteRoutesPref = settings.getCustomRenderBooleanProperty(SKI.getRenderingPropertyAttr());
			if (pisteRoutesPref.get()) {
				pisteRoutesPref.set(settings.RENDERER.get().equals(RendererRegistry.WINTER_SKI_RENDER));
			}
			app.getRendererRegistry().setCurrentSelectedRender(loaded);
			mapActivity.refreshMapComplete();
			DashboardOnMap dashboard = mapActivity.getDashboard();
			dashboard.refreshContent(false);
		} else {
			app.showShortToastMessage(R.string.renderer_load_exception);
		}
	}

	@Override
	protected boolean useScrollableItemsContainer() {
		return true;
	}

	@NonNull
	private TreeMap<String, String> generateStylesMap(Context context) {
		Collator collator = OsmAndCollator.primaryCollator();
		TreeMap<String, String> res = new TreeMap<>((string1, string2) -> {
			if (string1.equals(RendererRegistry.DEFAULT_RENDER)) {
				return -1;
			}
			if (string2.equals(RendererRegistry.DEFAULT_RENDER)) {
				return 1;
			}
			return collator.compare(string1, string2);
		});
		Map<String, String> renderers = app.getRendererRegistry().getRenderers(false);
		List<String> disabledRendererNames = PluginsHelper.getDisabledRendererNames();

		if (!Algorithms.isEmpty(disabledRendererNames)) {
			Iterator<Map.Entry<String, String>> iterator = renderers.entrySet().iterator();
			while (iterator.hasNext()) {
				String rendererVal = iterator.next().getValue();
				String rendererFileName = Algorithms.getFileWithoutDirs(rendererVal);
				if (disabledRendererNames.contains(rendererFileName)) {
					iterator.remove();
				}
			}
		}

		List<String> names = new ArrayList<>(renderers.keySet());
		for (String name : names) {
			String translation = RendererRegistry.getRendererName(context, name);
			res.put(translation, name);
		}

		return res;
	}

	@SuppressWarnings("RedundantCast")
	private void populateStylesList() {
		int counter = 0;
		for (Map.Entry<String, String> entry : stylesMap.entrySet()) {
			String name = entry.getValue();
			boolean selected = name.equals(selectedStyle);

			View view = stylesContainer.getChildAt(counter);
			view.setTag(name);
			view.setOnClickListener(getOnStyleClickListener());

			TextView titleTv = (TextView) view.findViewById(R.id.title);
			titleTv.setText(entry.getKey());
			titleTv.setTextColor(getStyleTitleColor(selected));

			RadioButton rb = (RadioButton) view.findViewById(R.id.compound_button);
			rb.setChecked(selected);
			UiUtilities.setupCompoundButton(rb, nightMode, PROFILE_DEPENDENT);

			counter++;
		}
	}

	@ColorInt
	private int getStyleTitleColor(boolean selected) {
		int colorId = selected
				? getActiveColorId()
				: ColorUtilities.getPrimaryTextColorId(nightMode);
		return getColor(colorId);
	}

	@NonNull
	private View.OnClickListener getOnStyleClickListener() {
		if (onStyleClickListener == null) {
			onStyleClickListener = v -> {
				selectedStyle = (String) v.getTag();
				descrItem.setDescription(RendererRegistry.getRendererDescription(app, selectedStyle));
				populateStylesList();
			};
		}
		return onStyleClickListener;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		showInstance(fragmentManager, null);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @Nullable SelectStyleListener selectStyleListener) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			SelectMapStyleBottomSheetDialogFragment fragment = new SelectMapStyleBottomSheetDialogFragment();
			fragment.selectStyleListener = selectStyleListener;
			fragment.show(fragmentManager, TAG);
		}
	}

	public interface SelectStyleListener {
		void onMapStyleSelected();
	}
}
