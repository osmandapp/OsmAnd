/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.support.v7.internal.view;

import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.view.View;
import android.view.animation.Interpolator;

import java.util.ArrayList;

/**
 * A very naive implementation of a set of
 * {@link android.support.v4.view.ViewPropertyAnimatorCompat}.
 *
 * @hide
 */
public class ViewPropertyAnimatorCompatSet {

    private final ArrayList<ViewPropertyAnimatorCompat> mAnimators;

    private long mDuration = -1;
    private Interpolator mInterpolator;
    private ViewPropertyAnimatorListener mListener;

    private boolean mIsStarted;

    public ViewPropertyAnimatorCompatSet() {
        mAnimators = new ArrayList<ViewPropertyAnimatorCompat>();
    }

    public ViewPropertyAnimatorCompatSet play(ViewPropertyAnimatorCompat animator) {
        if (!mIsStarted) {
            mAnimators.add(animator);
        }
        return this;
    }

    public void start() {
        if (mIsStarted) return;
        for (ViewPropertyAnimatorCompat animator : mAnimators) {
            if (mDuration >= 0) {
                animator.setDuration(mDuration);
            }
            if (mInterpolator != null) {
                animator.setInterpolator(mInterpolator);
            }
            if (mListener != null) {
                animator.setListener(mProxyListener);
            }
            animator.start();
        }

        mIsStarted = true;
    }

    private void onAnimationsEnded() {
        mIsStarted = false;
    }

    public void cancel() {
        if (!mIsStarted) {
            return;
        }
        for (ViewPropertyAnimatorCompat animator : mAnimators) {
            animator.cancel();
        }
        mIsStarted = false;
    }

    public ViewPropertyAnimatorCompatSet setDuration(long duration) {
        if (!mIsStarted) {
            mDuration = duration;
        }
        return this;
    }

    public ViewPropertyAnimatorCompatSet setInterpolator(Interpolator interpolator) {
        if (!mIsStarted) {
            mInterpolator = interpolator;
        }
        return this;
    }

    public ViewPropertyAnimatorCompatSet setListener(ViewPropertyAnimatorListener listener) {
        if (!mIsStarted) {
            mListener = listener;
        }
        return this;
    }

    private final ViewPropertyAnimatorListenerAdapter mProxyListener
            = new ViewPropertyAnimatorListenerAdapter() {
        private boolean mProxyStarted = false;
        private int mProxyEndCount = 0;

        @Override
        public void onAnimationStart(View view) {
            if (mProxyStarted) {
                return;
            }
            mProxyStarted = true;
            if (mListener != null) {
                mListener.onAnimationStart(null);
            }
        }

        void onEnd() {
            mProxyEndCount = 0;
            mProxyStarted = false;
            onAnimationsEnded();
        }

        @Override
        public void onAnimationEnd(View view) {
            if (++mProxyEndCount == mAnimators.size()) {
                if (mListener != null) {
                    mListener.onAnimationEnd(null);
                }
                onEnd();
            }
        }
    };
}
