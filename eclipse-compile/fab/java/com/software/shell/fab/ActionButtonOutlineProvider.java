/*
 * Copyright 2015 Shell Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * File created: 2015-02-25 19:54:28
 */

package com.software.shell.fab;

import android.annotation.TargetApi;
import android.graphics.Outline;
import android.os.Build;
import android.view.View;
import android.view.ViewOutlineProvider;

/**
 * An implementation of the {@link android.view.ViewOutlineProvider}
 * for <b>Action Button</b>
 * 
 * Used for drawing the elevation shadow for {@code API 21 Lollipop} and higher 
 *
 * @author Vladislav
 * @version 1.0.0
 * @since 1.0.0
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class ActionButtonOutlineProvider extends ViewOutlineProvider {
	
	/**
	 * Outline provider width 
	 */
	private int width;

	/**
	 * Outline provider height
	 */
	private int height;

	/**
	 * Creates an instance of the {@link com.software.shell.fab.ActionButtonOutlineProvider}
	 *  
	 * @param width initial outline provider width
	 * @param height initial outline provider height
	 */
	ActionButtonOutlineProvider(int width, int height) {
		this.width = width;
		this.height = height;
	}

	/**
	 * Called to get the provider to populate the Outline. This method will be called by a View 
	 * when its owned Drawables are invalidated, when the View's size changes, or if invalidateOutline()
	 * is called explicitly. The input outline is empty and has an alpha of 1.0f
	 *
	 * @param view a view, which builds the outline
	 * @param outline an empty outline, which is to be populated
	 */
	@Override
	public void getOutline(View view, Outline outline) {
		outline.setOval(0, 0, width, height);
	}
	
}
