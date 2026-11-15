package net.osmand.plus.auto.screens;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.model.Action;
import androidx.car.app.model.Pane;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;

import net.osmand.plus.R;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.search.core.SearchResult;

public class RoutePreviewPromptScreen extends BaseAndroidAutoScreen {

	@NonNull
	private final Action settingsAction;
	@NonNull
	private final SearchResult searchResult;

	public RoutePreviewPromptScreen(@NonNull CarContext carContext, @NonNull Action settingsAction,
	                                @NonNull SearchResult searchResult) {
		super(carContext);
		this.settingsAction = settingsAction;
		this.searchResult = searchResult;
	}

	@NonNull
	@Override
	public Template getTemplate() {
		return new PaneTemplate.Builder(new Pane.Builder()
				.addRow(new Row.Builder()
						.setTitle(getTitle())
						.build())
				.addAction(new Action.Builder()
						.setTitle(getApp().getString(R.string.shared_string_continue))
						.setOnClickListener(this::openRoutePreview)
						.build())
				.build())
				.setTitle(getApp().getString(R.string.current_route))
				.setHeaderAction(Action.BACK)
				.build();
	}

	private String getTitle() {
		String name = QuickSearchListItem.getName(getApp(), searchResult);
		if (name == null || name.length() == 0) {
			name = QuickSearchListItem.getTypeName(getApp(), searchResult);
		}
		return name;
	}

	private void openRoutePreview() {
		getScreenManager().pushForResult(
				new RoutePreviewScreen(getCarContext(), settingsAction, searchResult, true),
				result -> {
					if (result != null) {
						getScreenManager().popToRoot();
						getApp().getOsmandMap().getMapActions().startNavigation();
						if (getApp().getCarNavigationSession() != null
								&& getApp().getCarNavigationSession().hasStarted()) {
							getApp().getCarNavigationSession().startNavigationScreen();
						}
						finish();
					}
				});
	}
}
