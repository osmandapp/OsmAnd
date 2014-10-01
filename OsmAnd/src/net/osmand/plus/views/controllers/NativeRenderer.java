package net.osmand.plus.views.controllers;

import android.opengl.GLSurfaceView;
import android.util.Log;
import net.osmand.core.jni.AreaI;
import net.osmand.core.jni.IMapRenderer;
import net.osmand.core.jni.PointI;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Denis on 01.10.2014.
 */
public class NativeRenderer implements GLSurfaceView.Renderer {
	private IMapRenderer mapRenderer;


	public NativeRenderer(IMapRenderer mapRenderer){
		this.mapRenderer = mapRenderer;
	}

	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		Log.i(NativeViewController.NATIVE_TAG, "onSurfaceCreated");
		if (mapRenderer.isRenderingInitialized())
			mapRenderer.releaseRendering();
	}

	public void onSurfaceChanged(GL10 gl, int width, int height) {
		Log.i(NativeViewController.NATIVE_TAG, "onSurfaceChanged");
		mapRenderer.setViewport(new AreaI(0, 0, height, width));
		mapRenderer.setWindowSize(new PointI(width, height));

		if (!mapRenderer.isRenderingInitialized())
		{
			if (!mapRenderer.initializeRendering())
				Log.e(NativeViewController.NATIVE_TAG, "Failed to initialize rendering");
		}
	}

	public void onDrawFrame(GL10 gl) {
		mapRenderer.update();

		if (mapRenderer.prepareFrame())
			mapRenderer.renderFrame();
	}
}

