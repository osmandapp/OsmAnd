"""Test the actual renderer rotation gate and destruction methods in isolation.

Run with Python 3 and JDK 17+ on PATH, or set JAVA_HOME. An optional first
argument selects a different repository checkout (for regression controls).
Platform/native dependencies use minimal doubles; this does not compile the app
or validate Android lifecycle ordering, real EGL drivers, or rendering output.
"""
import os
from pathlib import Path
import subprocess
import sys
import tempfile

ROOT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[3]


def block(source, signature):
    start = source.index(signature)
    opening = source.index('{', start)
    depth = 1
    end = opening + 1
    while depth:
        depth += (source[end] == '{') - (source[end] == '}')
        end += 1
    return source[start:end]



activity = (ROOT / 'OsmAnd/src/net/osmand/plus/activities/MapActivity.java').read_text()
view = (ROOT / 'OsmAnd/src/net/osmand/plus/views/MapViewWithLayers.java').read_text()

android_test = r'''
class RotationPolicyTest {
    static class ActivityInfo {
        static final int CONFIG_ORIENTATION=128, CONFIG_SCREEN_SIZE=1024, CONFIG_SCREEN_LAYOUT=256;
    }
    static class Display {
        int id, rotation;
        int getDisplayId() { return id; }
        int getRotation() { return rotation; }
    }
    static class WindowManager {
        Display display = new Display();
        Display getDefaultDisplay() { return display; }
    }
    static class DisplayManager {
        Display display = new Display();
        Display getDisplay(int id) { return display; }
    }
    WindowManager manager = new WindowManager();
    App app = new App();
    int changes, initialDisplayId, initialDisplayRotation;
    boolean changing, finishing;
    WindowManager getWindowManager() { return manager; }
    int getChangingConfigurations() { return changes; }
    boolean isChangingConfigurations() { return changing; }
    boolean isFinishing() { return finishing; }
    __POLICY__
    __DISPLAY__

    static class Renderer {
        boolean available = true;
        int preparations, destroys;
        boolean prepareRendererForReuse() { preparations++; return available; }
        void handleOnDestroy() { destroys++; }
    }
    static class MapRendererContext {
        Renderer renderer;
        Renderer getMapRendererView() { return renderer; }
    }
    static class NativeCoreContext {
        static MapRendererContext context;
        static MapRendererContext getMapRendererContext() { return context; }
    }
    static class NavigationSession {
        boolean started;
        boolean hasStarted() { return started; }
    }
    static class App {
        DisplayManager displayManager = new DisplayManager();
        <T> T getSystemService(Class<T> type) { return type.cast(displayManager); }
        NavigationSession car;
        int removals;
        NavigationSession getCarNavigationSession() { return car; }
        App getOsmandMap() { return this; }
        void removeRenderingViewSetupListener(Object listener) { removals++; }
    }
    static class MapView {
        int detaches, cleared;
        void setMapRenderer(Object renderer, boolean disable) { detaches++; }
        void clearTouchDetectors() { cleared++; }
    }
    static class Layers {
        Renderer atlasMapRendererView = new Renderer();
        App app = new App();
        MapView mapView = new MapView();
        int resets;
        void resetMapRendererView() { resets++; }
        Object getRenderingViewSetupListener() { return this; }
        __DESTROY__
    }
    static int checks;
    static void check(boolean condition, String name) {
        checks++;
        if (!condition) throw new AssertionError(name);
    }
    public static void main(String[] args) {
        RotationPolicyTest policy = new RotationPolicyTest();
        policy.changing = true;
        policy.app.displayManager.display.rotation = 1;
        // Every combination of the documented rotation companion flags is allowed.
        for (int companions : new int[] {0, 256, 1024, 1280}) {
            policy.changes = 128 | companions;
            check(policy.isChangingScreenOrientation(), "rotation " + companions);
        }
        // Every other bit, including unknown future flags, prevents retention.
        for (int bit = 0; bit < 32; bit++) {
            int flag = 1 << bit;
            if ((flag & (128 | 256 | 1024)) != 0) continue;
            policy.changes = 128 | 1024 | flag;
            check(!policy.isChangingScreenOrientation(), "unrelated configuration bit " + bit);
        }
        policy.changes = 128 | 1024;
        policy.changing = false;
        check(!policy.isChangingScreenOrientation(), "background/normal destruction");
        policy.changing = true;
        policy.finishing = true;
        check(!policy.isChangingScreenOrientation(), "finishing");
        policy.finishing = false;
        policy.changes = 0;
        check(!policy.isChangingScreenOrientation(), "explicit recreate");
        policy.changes = 1024;
        check(!policy.isChangingScreenOrientation(), "size only");
        policy.changes = 128 | 1024;
        policy.app.displayManager.display.rotation = 0;
        check(!policy.isChangingScreenOrientation(), "window resize without display rotation");
        policy.app.displayManager.display.rotation = 1;
        policy.app.displayManager.display.id = 1;
        check(!policy.isChangingScreenOrientation(), "different display");
        policy.app.displayManager.display.id = 0;
        policy.initialDisplayRotation = 3;
        policy.app.displayManager.display.rotation = 0;
        check(policy.isChangingScreenOrientation(), "reverse rotation");

        policy.app.displayManager.display = null;
        check(!policy.isChangingScreenOrientation(), "display removed");
        policy.app.displayManager.display = new Display();
        policy.initialDisplayRotation = -1;
        check(!policy.isChangingScreenOrientation(), "initial display unavailable");
        policy.app.displayManager = null;
        check(!policy.isChangingScreenOrientation(), "display service unavailable");

        for (int mode = 0; mode < 8; mode++) {
            Layers layers = new Layers();
            MapRendererContext context = new MapRendererContext();
            NativeCoreContext.context = context;
            context.renderer = layers.atlasMapRendererView;
            if (mode == 2) layers.atlasMapRendererView.available = false;
            if (mode == 3) NativeCoreContext.context = null;
            if (mode == 4) context.renderer = new Renderer();
            if (mode == 5 || mode == 6) {
                layers.app.car = new NavigationSession();
                layers.app.car.started = mode == 5;
            }
            if (mode == 7) layers.atlasMapRendererView = null;
            layers.onDestroy(mode != 1);
            boolean retained = mode == 0 || mode == 6;
            boolean carOrAbsent = mode == 5 || mode == 7;
            check(layers.resets == (retained || carOrAbsent ? 0 : 1), "release fallback " + mode);
            if (layers.atlasMapRendererView != null) {
                check(layers.atlasMapRendererView.destroys == (retained || carOrAbsent ? 0 : 1), "destroy " + mode);
                check(layers.atlasMapRendererView.preparations == (mode == 0 || mode == 2 || mode == 6 ? 1 : 0), "prepare " + mode);
            }
            check(layers.mapView.cleared == 1 && layers.app.removals == 1, "UI cleanup " + mode);
        }
        System.out.println("Android rotation policy and cleanup: " + checks + " assertions passed");
    }
}
'''.replace('__POLICY__', block(activity, 'private boolean isChangingScreenOrientation()')) \
   .replace('__DESTROY__', block(view, 'public void onDestroy(boolean changingScreenOrientation)')) \
   .replace('__DISPLAY__', block(activity, 'private Display getDeviceDisplay()'))

with tempfile.TemporaryDirectory(prefix='renderer-isolated-tests-') as temp:
    work = Path(temp)
    source_file = work / 'RotationPolicyTest.java'
    source_file.write_text(android_test)
    java_bin = Path(os.environ['JAVA_HOME']) / 'bin' if 'JAVA_HOME' in os.environ else Path('')
    subprocess.run([str(java_bin / 'javac'), '-d', temp, str(source_file)], check=True)
    subprocess.run([str(java_bin / 'java'), '-cp', temp, 'RotationPolicyTest'], check=True, timeout=15)
