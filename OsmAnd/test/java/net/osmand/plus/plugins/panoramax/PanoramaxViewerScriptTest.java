package net.osmand.plus.plugins.panoramax;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.lang.reflect.Field;

@RunWith(AndroidJUnit4.class)
public class PanoramaxViewerScriptTest {

	// Stands in for the page and the Panoramax viewer, and records what the script posts to the app.
	private static final String PAGE = """
			var posted = [], listeners = {}, timers = [], meta = null, clock = 0;
			var viewer = {
				psv: {getPictureMetadata: function() { return meta; }},
				addEventListener: function(name, listener) { listeners[name] = listener; }
			};
			var document = {
				getElementById: function(id) { return id === 'viewer' ? viewer : {appendChild: function() {}}; },
				createElement: function() { return {}; },
				createTextNode: function() { return {}; }
			};
			var window = {
				addEventListener: function(name, listener) { listeners[name] = listener; },
				removeEventListener: function() {}
			};
			var performance = {now: function() { return clock += 1000; }};
			var customElements = {get: function() { return {}; }};
			function setTimeout(callback) { return timers.push(callback); }
			function clearTimeout(id) { timers[id - 1] = function() {}; }
			function pnxFail() {}
			function load(properties, x) {
				meta = properties ? {properties: properties} : null;
				listeners['psv:picture-loaded']({detail: {picId: 'p', lat: 48.8, lon: 2.3, x: x}});
			}
			function rotate(x) {
				listeners['psv:view-rotated']({detail: {x: x}});
				timers.splice(0).forEach(function(callback) { callback(); });
			}
			function messages() {
				return JSON.stringify(posted.map(function(m) { return m.type + ':' + m.heading; }));
			}
			""";

	private static final String HANDSHAKE = "listeners.message({data: 'nonce', ports: [{postMessage: "
			+ "function(m) { posted.push(JSON.parse(m)); }}]});";

	private Context context;
	private Scriptable scope;

	@Before
	public void setUp() throws Exception {
		Field field = PanoramaxImageDialog.class.getDeclaredField("VIEWER_SCRIPT");
		field.setAccessible(true);
		context = Context.enter();
		// Android cannot load the bytecode Rhino generates, so the script is interpreted as in CommandPlayer.
		context.setOptimizationLevel(-1);
		scope = context.initSafeStandardObjects();
		run(PAGE + "(" + field.get(null) + ")('nonce');" + HANDSHAKE);
	}

	@After
	public void tearDown() {
		Context.exit();
	}

	@Test
	public void sendsNoHeadingForAPictureWithoutAValidAzimuth() {
		run("load(null, 0); load({}, 0); load({'view:azimuth': null}, 0);"
				+ "load({'view:azimuth': NaN}, 0); load({'view:azimuth': '90'}, 0);");
		assertEquals("[\"picture:null\",\"picture:null\",\"picture:null\",\"picture:null\",\"picture:null\"]",
				run("messages()"));
	}

	@Test
	public void keepsANorthAzimuth() {
		run("load({'view:azimuth': 0}, 0); rotate(90);");
		assertEquals("[\"picture:0\",\"heading:90\"]", run("messages()"));
	}

	@Test
	public void sendsTheHeadingOfEachPictureWhenSwitching() {
		run("load({'view:azimuth': 321}, 321); load({}, 0); load({'view:azimuth': 0}, 0);");
		assertEquals("[\"picture:321\",\"picture:null\",\"picture:0\"]", run("messages()"));
	}

	@Test
	public void ignoresRotationsOfAPictureWithoutAzimuth() {
		run("load({}, 0); rotate(40); load({'view:azimuth': 10}, 10); rotate(80);");
		assertEquals("[\"picture:null\",\"picture:10\",\"heading:80\"]", run("messages()"));
	}

	private String run(String script) {
		return Context.toString(context.evaluateString(scope, script, "test", 1, null));
	}
}
