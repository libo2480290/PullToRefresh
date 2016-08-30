/*******************************************************************************
 * Copyright 2011, 2012 Chris Banes.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.handmark.pulltorefresh.library.internal;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshBase.Orientation;
import com.handmark.pulltorefresh.library.R;

public class BDPRotateLoadingLayout extends LoadingLayout {

    private final int[] animationDrawables= new int[] {
            R.drawable.loading_in0001, R.drawable.loading_in0002, R.drawable.loading_in0003, R.drawable.loading_in0004,
            R.drawable.loading_in0005, R.drawable.loading_in0006, R.drawable.loading_in0007, R.drawable.loading_in0008,
            R.drawable.loading_in0009, R.drawable.loading_in0010, R.drawable.loading_in0011, R.drawable.loading_in0012,
            R.drawable.loading_in0013, R.drawable.loading_in0014, R.drawable.loading_in0015, R.drawable.loading_in0016,
            R.drawable.loading_in0017, R.drawable.loading_in0018, R.drawable.loading_in0019, R.drawable.loading_in0020,
            R.drawable.loading_in0021, R.drawable.loading_in0022, R.drawable.loading_in0023, R.drawable.loading_in0024
    } ;

    public BDPRotateLoadingLayout(Context context, Mode mode, Orientation scrollDirection, TypedArray attrs) {
        super(context, mode, scrollDirection, attrs);
    }

    public void onLoadingDrawableSet(Drawable imageDrawable) {

    }

    protected void onPullImpl(float scaleOfLayout) {
        Log.d("PullToRefresh", "scaleOfLayout = "+scaleOfLayout);

        if (scaleOfLayout < 0.7f) {
            mHeaderImage.setImageResource(animationDrawables[0]);
        } else {
            int which = (int) ((scaleOfLayout - 0.7f) * animationDrawables.length / 1f);
            which = which > animationDrawables.length - 1 ? animationDrawables.length - 1 : which;
            mHeaderImage.clearAnimation();
            mHeaderImage.setImageResource(animationDrawables[which]);
        }
    }

    @Override
    protected void refreshingImpl() {
        mHeaderImage.setImageResource(getAnimationDrawableResId());
        AnimationDrawable animationDrawable = (AnimationDrawable)mHeaderImage.getDrawable();
        animationDrawable.start();
    }

    @Override
    protected void resetImpl() {
        mHeaderImage.clearAnimation();
        resetImageRotation();
    }

    private void resetImageRotation() {
    }

    @Override
    protected void pullToRefreshImpl() {
        // NO-OP
    }

    @Override
    protected void releaseToRefreshImpl() {
        // NO-OP
    }

    @Override
    protected int getDefaultDrawableResId() {
        return R.drawable.loading_in0016;
    }

    protected int getAnimationDrawableResId() {
        return R.anim.bdp_rotate_anim;
    }

}
