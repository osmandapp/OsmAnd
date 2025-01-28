package net.osmand.plus.configmap.routes;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.render.RenderingClass;

import java.util.List;

public class RenderingClassesCard extends MapBaseCard {

	private final RenderingClass mainClass;
	private final List<RenderingClass> list;

	private ViewGroup container;

	@Override
	public int getCardLayoutId() {
		return R.layout.rendering_classes_card;
	}

	public RenderingClassesCard(@NonNull MapActivity activity, @NonNull RenderingClass mainClass,
			@NonNull List<RenderingClass> list) {
		super(activity, true);
		this.mainClass = mainClass;
		this.list = list;
	}

	@Override
	protected void updateContent() {
		setupHeader();
		setupRenderingClasses();
	}

	private void setupHeader() {
		TextView header = view.findViewById(R.id.header);
		header.setText(mainClass.getTitle());
	}

	private void setupRenderingClasses() {
		container = view.findViewById(R.id.container);
		container.removeAllViews();

		for (int i = 0; i < list.size(); i++) {
			RenderingClass renderingClass = list.get(i);
			container.addView(createRadioButton(renderingClass, i == list.size() - 1));
		}
	}

	@NonNull
	private View createRadioButton(@NonNull RenderingClass renderingClass, boolean lastItem) {
		CommonPreference<Boolean> pref = settings.getСustomBooleanRenderClassProperty(renderingClass.getName(), renderingClass.isEnabledByDefault());
		View view = themedInflater.inflate(R.layout.bottom_sheet_item_with_switch_56dp, container, false);

		ImageView icon = view.findViewById(R.id.icon);
		AndroidUiHelper.updateVisibility(icon, false);

		TextView title = view.findViewById(R.id.title);
		title.setText(renderingClass.getTitle());

		CompoundButton button = view.findViewById(R.id.compound_button);
		UiUtilities.setupCompoundButton(nightMode, ColorUtilities.getActiveColor(app, nightMode), button);
		button.setChecked(pref.get());

		view.setOnClickListener(v -> {
			boolean checked = !pref.get();
			pref.set(checked);
			button.setChecked(checked);
			notifyCardPressed();
		});

		int profileColor = settings.getApplicationMode().getProfileColor(nightMode);
		Drawable background = UiUtilities.getColoredSelectableDrawable(app, profileColor, 0.3f);
		AndroidUtils.setBackground(view, background);

		return view;
	}
}