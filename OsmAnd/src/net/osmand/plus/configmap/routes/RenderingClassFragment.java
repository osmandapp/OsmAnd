package net.osmand.plus.configmap.routes;

import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.configmap.ConfigureMapUtils;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.render.RenderingClass;

import java.util.List;

public class RenderingClassFragment extends MapRoutesFragment {

	public static final String TAG = RenderingClassFragment.class.getSimpleName();

	private RenderingClass renderingClass;
	private CommonPreference<Boolean> preference;

	@Override
	protected boolean isEnabled() {
		return preference.get();
	}

	@Override
	protected void toggleMainPreference(@NonNull View view) {
		preference.set(!isEnabled());
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		renderingClass = app.getRouteLayersHelper().getSelectedRenderingClass();
		preference = settings.getBooleanRenderClassProperty(renderingClass);
	}

	protected void setupHeader(@NonNull View view) {
		super.setupHeader(view);

		boolean enabled = isEnabled();
		View container = view.findViewById(R.id.preference_container);

		TextView title = container.findViewById(R.id.title);
		title.setText(renderingClass.getTitle());

		AndroidUiHelper.updateVisibility(container.findViewById(R.id.icon), false);
		AndroidUiHelper.updateVisibility(container.findViewById(R.id.description), false);
	}

	@Override
	protected void createCards(@NonNull View view) {
		super.createCards(view);

		Pair<RenderingClass, List<RenderingClass>> pair = ConfigureMapUtils.getRenderingClassWithChildren(app, renderingClass.getName());
		if (pair != null) {
			addCard(new RenderingClassesCard(getMapActivity(), null, pair.second));
		}
	}

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @NonNull RenderingClass renderingClass) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			RenderingClassFragment fragment = new RenderingClassFragment();
			fragment.renderingClass = renderingClass;
			manager.beginTransaction()
					.replace(R.id.content, fragment, fragment.getTag())
					.commitAllowingStateLoss();
		}
	}
}