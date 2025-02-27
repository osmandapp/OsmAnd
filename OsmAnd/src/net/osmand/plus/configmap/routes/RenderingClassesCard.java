package net.osmand.plus.configmap.routes;

import static net.osmand.plus.dashboard.DashboardType.RENDERING_CLASS;
import static net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem.INVALID_ID;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.ConfigureMapUtils;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.ctxmenu.ViewCreator;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.render.RenderingClass;
import net.osmand.util.Algorithms;

import java.util.List;

public class RenderingClassesCard extends MapBaseCard {

	private final RouteLayersHelper routeLayersHelper;
	private final RenderingClass renderingClass;
	private final List<RenderingClass> subclasses;

	private ViewCreator viewCreator;
	private ViewGroup container;

	@Override
	public int getCardLayoutId() {
		return R.layout.rendering_classes_card;
	}

	public RenderingClassesCard(@NonNull MapActivity activity,
			@Nullable RenderingClass renderingClass,
			@Nullable List<RenderingClass> subclasses) {
		super(activity, true);
		this.renderingClass = renderingClass;
		this.subclasses = subclasses;

		routeLayersHelper = app.getRouteLayersHelper();

		viewCreator = new ViewCreator(activity, nightMode);
		viewCreator.setDefaultLayoutId(R.layout.list_item_icon_and_menu);
		viewCreator.setCustomControlsColor(settings.getApplicationMode().getProfileColor(nightMode));
	}

	@Override
	protected void updateContent() {
		container = view.findViewById(R.id.container);
		container.removeAllViews();

		boolean showRows = true;
		if (renderingClass != null) {
			container.addView(createHeaderRow(renderingClass));
			showRows = settings.getBooleanRenderClassProperty(renderingClass).get();
		}

		if (!Algorithms.isEmpty(subclasses) && showRows) {
			container.addView(themedInflater.inflate(R.layout.simple_divider_item, container, false));

			for (int i = 0; i < subclasses.size(); i++) {
				RenderingClass renderingClass = subclasses.get(i);
				boolean lastItem = i == subclasses.size() - 1;
				List<RenderingClass> children = ConfigureMapUtils.getChildrenRenderingClasses(app, renderingClass);
				container.addView(createItemRow(renderingClass, !Algorithms.isEmpty(children), !lastItem));
			}
		}
	}

	@NonNull
	private View createHeaderRow(@NonNull RenderingClass renderingClass) {
		View view = createItemRow(renderingClass, false, false);

		TextView title = view.findViewById(R.id.title);
		title.setTypeface(FontCache.getMediumFont(title.getTypeface()));

		return view;
	}

	@NonNull
	private View createItemRow(@NonNull RenderingClass renderingClass, boolean showSubscreen,
			boolean showDivider) {
		CommonPreference<Boolean> pref = settings.getBooleanRenderClassProperty(renderingClass);
		boolean enabled = pref.get();

		ContextMenuItem item = new ContextMenuItem(renderingClass.getName())
				.setSelected(enabled)
				.setHideDivider(!showDivider)
				.setTitle(renderingClass.getTitle())
				.setColor(enabled ? settings.getApplicationMode().getProfileColor(nightMode) : null)
				.setSecondaryIcon(showSubscreen ? R.drawable.ic_action_additional_option : INVALID_ID);

		View view = viewCreator.getView(item, null);

		CompoundButton compoundButton = view.findViewById(R.id.toggle_item);
		compoundButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				pref.set(isChecked);
				notifyCardPressed();
			}
		});
		view.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (showSubscreen) {
					routeLayersHelper.setSelectedRenderingClass(renderingClass);
					mapActivity.getDashboard().setDashboardVisibility(true,
							RENDERING_CLASS, AndroidUtils.getCenterViewCoordinates(view));
				} else {
					compoundButton.toggle();
				}
			}
		});
		int profileColor = settings.getApplicationMode().getProfileColor(nightMode);
		AndroidUtils.setBackground(view, UiUtilities.getColoredSelectableDrawable(app, profileColor, 0.3f));

		return view;
	}
}