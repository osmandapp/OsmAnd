package net.osmand.plus.auto.screens;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.OnClickListener;
import androidx.car.app.model.ParkedOnlyOnClickListener;
import androidx.car.app.model.Template;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;

public class RequestPurchaseScreen extends BaseAndroidAutoScreen {

	public RequestPurchaseScreen(@NonNull CarContext carContext) {
		super(carContext);
	}

	@NonNull
	@Override
	public Template onGetTemplate() {

		String message = getCarContext().getString(R.string.android_auto_purchase_request_title);

		OnClickListener listener = ParkedOnlyOnClickListener.create(() -> {
			OsmandApplication app = (OsmandApplication) getCarContext().getApplicationContext();
			Bundle params = new Bundle();
			params.putBoolean(ChoosePlanFragment.OPEN_CHOOSE_PLAN, true);
			params.putString(ChoosePlanFragment.CHOOSE_PLAN_FEATURE, OsmAndFeature.ANDROID_AUTO.name());
			MapActivity.launchMapActivityMoveToTop(app, null, null, params);
		});
		
		Action action = new Action.Builder()
				.setTitle(getCarContext().getString(R.string.continue_on_phone))
				.setBackgroundColor(CarColor.BLUE)
				.setOnClickListener(listener)
				.build();

		return new MessageTemplate.Builder(message)
				.setHeaderAction(Action.APP_ICON)
				.addAction(action)
				.build();
	}
}
