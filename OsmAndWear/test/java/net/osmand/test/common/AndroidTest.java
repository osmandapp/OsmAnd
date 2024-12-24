package net.osmand.test.common;

import static java.util.concurrent.TimeUnit.MINUTES;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingPolicies;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.SimulationMode;

import org.junit.After;
import org.junit.Before;

public abstract class AndroidTest {

	protected OsmandApplication app;
	protected OsmandSettings settings;
	protected AppInitIdlingResource appInitIdlingResource;

	@Before
	public void setup() {
		Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
		app = ((OsmandApplication) context.getApplicationContext());
		settings = app.getSettings();

		EspressoUtils.grantPermissions(app);
		IdlingPolicies.setIdlingResourceTimeout(5, MINUTES);

		appInitIdlingResource = new AppInitIdlingResource(app);
		registerIdlingResources(appInitIdlingResource);

		Espresso.onIdle();
	}

	@After
	public void cleanUp() {
		unregisterIdlingResources(appInitIdlingResource);
	}

	protected void registerIdlingResources(@NonNull IdlingResource... idlingResources) {
		IdlingRegistry.getInstance().register(idlingResources);
	}

	protected void unregisterIdlingResources(@NonNull IdlingResource... idlingResources) {
		IdlingRegistry.getInstance().unregister(idlingResources);
	}

	protected void enableSimulation(float kmPerHour) {
		settings.simulateNavigation = true;
		settings.simulateNavigationMode = SimulationMode.CONSTANT.getKey();
		settings.simulateNavigationSpeed = kmPerHour / 3.6f;
	}
}