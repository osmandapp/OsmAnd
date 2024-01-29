package net.osmand.test.common;

import android.content.Context;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.SimulationMode;

import org.junit.After;
import org.junit.Before;

import androidx.annotation.NonNull;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.platform.app.InstrumentationRegistry;

public abstract class AndroidTest {

	protected OsmandApplication app;
	protected OsmandSettings settings;
	protected AppInitIdlingResource appInitIdlingResource;

	@Before
	public void setup() {
		Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
		app = ((OsmandApplication) context.getApplicationContext());

		EspressoUtils.grantPermissions(app);

		appInitIdlingResource = new AppInitIdlingResource(app);
		registerIdlingResources(appInitIdlingResource);
		Espresso.onIdle();

		settings = app.getSettings();
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