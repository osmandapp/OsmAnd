package net.osmand.plus.settings.fragments.search;

import static androidx.test.internal.util.Checks.checkNotNull;

import android.app.Activity;

import androidx.test.core.app.ActivityScenario;

import org.junit.rules.ExternalResource;

import java.util.function.Supplier;

// adapted from androidx.test.ext.junit.rules.ActivityScenarioRule
class NonClosingActivityScenarioRule<A extends Activity> extends ExternalResource {

	private final Supplier<ActivityScenario<A>> scenarioSupplier;

	public NonClosingActivityScenarioRule(final Class<A> activityClass) {
		scenarioSupplier = () -> ActivityScenario.launch(checkNotNull(activityClass));
	}

	@Override
	protected void before() {
		scenarioSupplier.get();
	}

	@Override
	protected void after() {
		// fixing: Activity never becomes requested state "[DESTROYED]" (last lifecycle transition = "PAUSED")
		// scenario.close();
	}
}
