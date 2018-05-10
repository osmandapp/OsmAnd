package net.osmand.plus.dialogs;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.widget.CompoundButtonCompat;
import android.support.v4.widget.NestedScrollView;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemTitleWithDescrAndButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SubtitleDividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.openseamapsplugin.NauticalMapsPlugin;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.render.RenderingRulesStorage;

import java.util.ArrayList;
import java.util.List;

public class SelectMapStyleBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = SelectMapStyleBottomSheetDialogFragment.class.getSimpleName();

	private static final String SELECTED_STYLE_KEY = "selected_style";
	private static final int COLLAPSED_DESCRIPTION_LINES = 3;

	private boolean descriptionExpanded;

	private LinearLayout stylesContainer;
	private BottomSheetItemTitleWithDescrAndButton descrItem;
	private View.OnClickListener onStyleClickListener;
	private ColorStateList rbColorList;

	private List<String> styles;
	private String selectedStyle;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final Context context = getContext();
		if (context == null) {
			return;
		}

		RendererRegistry rendererRegistry = getMyApplication().getRendererRegistry();
		styles = new ArrayList<>(rendererRegistry.getRendererNames());
		if (OsmandPlugin.getEnabledPlugin(NauticalMapsPlugin.class) == null) {
			styles.remove(RendererRegistry.NAUTICAL_RENDER);
		}
		if (savedInstanceState == null) {
			selectedStyle = rendererRegistry.getCurrentSelectedRenderer().getName();
		} else {
			selectedStyle = savedInstanceState.getString(SELECTED_STYLE_KEY);
		}

		rbColorList = AndroidUtils.createCheckedColorStateList(context, R.color.icon_color, getActiveColorId());

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
		for (int i = 0; i < styles.size(); i++) {
			LayoutInflater.from(new ContextThemeWrapper(context, themeRes))
					.inflate(R.layout.bottom_sheet_item_with_radio_btn_left, stylesContainer, true);
		}
		nestedScrollView.addView(stylesContainer);
		items.add(new BaseBottomSheetItem.Builder().setCustomView(nestedScrollView).create());

		populateStylesList();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
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
			ConfigureMapMenu.refreshMapComplete(mapActivity);
			mapActivity.getDashboard().refreshContent(true);
		} else {
			Toast.makeText(mapActivity, R.string.renderer_load_exception, Toast.LENGTH_SHORT).show();
		}
		dismiss();
	}

	@Override
	protected boolean useScrollableItemsContainer() {
		return false;
	}

	@Nullable
	private MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity != null && activity instanceof MapActivity) {
			return (MapActivity) activity;
		}
		return null;
	}

	@SuppressWarnings("RedundantCast")
	private void populateStylesList() {
		Context context = getContext();
		if (context == null) {
			return;
		}
		for (int i = 0; i < styles.size(); i++) {
			String style = styles.get(i);
			boolean selected = style.equals(selectedStyle);
			String title = RendererRegistry.getTranslatedRendererName(context, style);
			if (title == null) {
				title = style.replace('_', ' ').replace('-', ' ');
			}

			View view = stylesContainer.getChildAt(i);
			view.setTag(style);
			view.setOnClickListener(getOnStyleClickListener());

			TextView titleTv = (TextView) view.findViewById(R.id.title);
			titleTv.setText(title);
			titleTv.setTextColor(getStyleTitleColor(selected));

			RadioButton rb = (RadioButton) view.findViewById(R.id.compound_button);
			rb.setChecked(selected);
			CompoundButtonCompat.setButtonTintList(rb, rbColorList);
		}
	}

	@ColorInt
	private int getStyleTitleColor(boolean selected) {
		int colorId = selected
				? getActiveColorId()
				: nightMode ? R.color.primary_text_dark : R.color.primary_text_light;
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
