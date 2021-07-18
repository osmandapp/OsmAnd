package net.osmand.plus.dialogs;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemTitleWithDescrAndButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SubtitleDividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static net.osmand.plus.UiUtilities.CompoundButtonType.PROFILE_DEPENDENT;

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

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final Context context = getContext();
		if (context == null) {
			return;
		}

		stylesMap = generateStylesMap(context);
		if (savedInstanceState == null) {
			RenderingRulesStorage current = getMyApplication().getRendererRegistry().getCurrentSelectedRenderer();
			if (current != null) {
				selectedStyle = current.getName();
			}
		} else {
			selectedStyle = savedInstanceState.getString(SELECTED_STYLE_KEY);
		}
		if(selectedStyle == null) {
			selectedStyle = RendererRegistry.DEFAULT_RENDER;
		}

		items.add(new TitleItem(getString(R.string.map_widget_renderer)));

		descrItem = (BottomSheetItemTitleWithDescrAndButton) new BottomSheetItemTitleWithDescrAndButton.Builder()
				.setButtonTitle(getString(R.string.show_full_description))
				.setOnButtonClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						descriptionExpanded = !descriptionExpanded;
						descrItem.setButtonText(getString(descriptionExpanded
								? R.string.hide_full_description : R.string.show_full_description));
						descrItem.setDescriptionMaxLines(descriptionExpanded
								? Integer.MAX_VALUE : COLLAPSED_DESCRIPTION_LINES);
						setupHeightAndBackground(getView());
					}
				})
				.setDescription(RendererRegistry.getRendererDescription(context, selectedStyle))
				.setDescriptionMaxLines(COLLAPSED_DESCRIPTION_LINES)
				.setLayoutId(R.layout.bottom_sheet_item_with_expandable_descr)
				.create();
		items.add(descrItem);

		items.add(new SubtitleDividerItem(context));

		NestedScrollView nestedScrollView = new NestedScrollView(context);
		stylesContainer = new LinearLayout(context);
		stylesContainer.setLayoutParams((new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)));
		stylesContainer.setOrientation(LinearLayout.VERTICAL);
		stylesContainer.setPadding(0, getResources().getDimensionPixelSize(R.dimen.bottom_sheet_content_padding_small), 0, 0);
		for (int i = 0; i < stylesMap.size(); i++) {
			LayoutInflater.from(new ContextThemeWrapper(context, themeRes))
					.inflate(R.layout.bottom_sheet_item_with_radio_btn_left, stylesContainer, true);
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
		if (mapActivity == null) {
			return;
		}
		OsmandApplication app = getMyApplication();
		RenderingRulesStorage loaded = app.getRendererRegistry().getRenderer(selectedStyle);
		if (loaded != null) {
			OsmandMapTileView view = mapActivity.getMapView();
			view.getSettings().RENDERER.set(selectedStyle);
			app.getRendererRegistry().setCurrentSelectedRender(loaded);
			mapActivity.refreshMapComplete();
			mapActivity.getDashboard().refreshContent(true);
		} else {
			Toast.makeText(mapActivity, R.string.renderer_load_exception, Toast.LENGTH_SHORT).show();
		}
		dismiss();
	}

	@Override
	protected boolean useScrollableItemsContainer() {
		return true;
	}

	@Nullable
	private MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity != null && activity instanceof MapActivity) {
			return (MapActivity) activity;
		}
		return null;
	}

	@NonNull
	private TreeMap<String, String> generateStylesMap(Context context) {
		final Collator collator = OsmAndCollator.primaryCollator();
		TreeMap<String, String> res = new TreeMap<>(new Comparator<String>() {
			@Override
			public int compare(String string1, String string2) {
				if (string1.equals(RendererRegistry.DEFAULT_RENDER)) {
					return -1;
				}
				if (string2.equals(RendererRegistry.DEFAULT_RENDER)) {
					return 1;
				}
				return collator.compare(string1, string2);
			}
		});
		Map<String, String> renderers = getMyApplication().getRendererRegistry().getRenderers();
		List<String> disabledRendererNames = OsmandPlugin.getDisabledRendererNames();

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
		Context context = getContext();
		if (context == null) {
			return;
		}
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
				: nightMode ? R.color.text_color_primary_dark : R.color.text_color_primary_light;
		return getResolvedColor(colorId);
	}

	private View.OnClickListener getOnStyleClickListener() {
		if (onStyleClickListener == null) {
			onStyleClickListener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Context context = getContext();
					if (context == null) {
						return;
					}
					selectedStyle = (String) v.getTag();
					descrItem.setDescription(RendererRegistry.getRendererDescription(context, selectedStyle));
					populateStylesList();
				}
			};
		}
		return onStyleClickListener;
	}
}
