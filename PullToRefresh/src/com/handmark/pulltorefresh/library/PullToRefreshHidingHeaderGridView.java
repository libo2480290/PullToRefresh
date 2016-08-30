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
package com.handmark.pulltorefresh.library;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.GridView;

import com.handmark.pulltorefresh.library.internal.EmptyViewMethodAccessor;

public class PullToRefreshHidingHeaderGridView extends PullToRefreshAdapterViewBase<GridView>{

    public PullToRefreshHidingHeaderGridView(Context context) {
        super(context);
    }

    public PullToRefreshHidingHeaderGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PullToRefreshHidingHeaderGridView(Context context, Mode mode) {
        super(context, mode);
    }

    public PullToRefreshHidingHeaderGridView(Context context, Mode mode, AnimationStyle style) {
        super(context, mode, style);
    }

    @Override
    public final Orientation getPullToRefreshScrollDirection() {
        return Orientation.VERTICAL;
    }

    @Override
    protected final GridView createRefreshableView(Context context, AttributeSet attrs) {
        final GridView gv;
        if (VERSION.SDK_INT >= VERSION_CODES.GINGERBREAD) {
            gv = new InternalGridViewSDK9(context, attrs);
        } else {
            gv = new InternalGridView(context, attrs);
        }

        // Use Generated ID (from res/values/ids.xml)
        gv.setId(R.id.gridview);
        return gv;
    }

    class InternalGridView extends GridView implements EmptyViewMethodAccessor {

        public InternalGridView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        public void setEmptyView(View emptyView) {
            PullToRefreshHidingHeaderGridView.this.setEmptyView(emptyView);
        }

        @Override
        public void setEmptyViewInternal(View emptyView) {
            super.setEmptyView(emptyView);
        }
    }

    @TargetApi(9)
    final class InternalGridViewSDK9 extends InternalGridView {

        public InternalGridViewSDK9(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        protected boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX,
                                       int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {

            final boolean returnValue = super.overScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX,
                    scrollRangeY, maxOverScrollX, maxOverScrollY, isTouchEvent);

            // Does all of the hard work...
            OverscrollHelper.overScrollBy(PullToRefreshHidingHeaderGridView.this, deltaX, scrollX, deltaY, scrollY, isTouchEvent);

            return returnValue;
        }
    }

    private OnScrollingListener mScrollingListener;

    public void setOnScrollingListener(OnScrollingListener listener){
        this.mScrollingListener = listener;
    }

    /**
     * An OnScrollListener can be set on a RecyclerView to receive messages when a scrolling event has occurred on that
     * RecyclerView.
     *
     * @see com.handmark.pulltorefresh.library.PullToRefreshHidingHeaderGridView#setOnScrollingListener(OnScrollingListener)
     */
    public interface OnScrollingListener {
        /**
         * The view is not scrolling. Note navigating the list using the trackball counts as
         * being in the idle state since these transitions are not animated.
         */
        public static int SCROLL_STATE_IDLE = 0;

        /**
         * The user is scrolling using touch, and their finger is still on the screen
         */
        public static int SCROLL_STATE_TOUCH_SCROLL = 1;

        /**
         * The user had previously been scrolling using touch and had performed a fling. The
         * animation is now coasting to a stop
         */
        public static int SCROLL_STATE_FLING = 2;

        public void onScrollStateChanged(int newState);

        public void onScrolled(float dx , float dy);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {

        if (!isPullToRefreshEnabled()) {
            return false;
        }

        //FIXME
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        final int action = event.getAction();
        final int actionIndex = event.getActionIndex();

        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            mIsBeingDragged = false;
            return false;
        }

        if (action != MotionEvent.ACTION_DOWN && mIsBeingDragged) {
            return true;
        }

        switch (action) {
            case MotionEvent.ACTION_MOVE: {
                // If we're refreshing, and the flag is set. Eat all MOVE events
                if (!mScrollingWhileRefreshingEnabled && isRefreshing()) {
                    return true;
                }

                final float x = event.getX();
                final float y = event.getY();

                if (mScrollState != SCROLL_STATE_DRAGGING) {
                    final float dx = x - mInitialMotionX;
                    final float dy = y - mInitialMotionY;
                    boolean startScroll = false;
                    if (Math.abs(dy) > mTouchSlop) {
                        mLastMotionY = mInitialMotionY + mTouchSlop * (dy < 0 ? -1 : 1);
                        startScroll = true;
                    }
                    if (startScroll) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                        setScrollState(SCROLL_STATE_DRAGGING);
                    }
                }

                if (isReadyForPull()) {
                    //final float y = event.getY(), x = event.getX();
                    final float diff, oppositeDiff, absDiff;

                    // We need to use the correct values, based on scroll
                    // direction
                    switch (getPullToRefreshScrollDirection()) {
                        case HORIZONTAL:
                            diff = x - mLastMotionX;
                            oppositeDiff = y - mLastMotionY;
                            break;
                        case VERTICAL:
                        default:
                            diff = y - mLastMotionY;
                            oppositeDiff = x - mLastMotionX;
                            break;
                    }
                    absDiff = Math.abs(diff);

                    if (absDiff > mTouchSlop && (!mFilterTouchEvents || absDiff > Math.abs(oppositeDiff))) {
                        if (mMode.showHeaderLoadingLayout() && diff >= 1f && isReadyForPullStart()) {
                            mLastMotionY = y;
                            mLastMotionX = x;
                            mIsBeingDragged = true;
                            if (mMode == Mode.BOTH) {
                                mCurrentMode = Mode.PULL_FROM_START;
                            }
                        } else if (mMode.showFooterLoadingLayout() && diff <= -1f && isReadyForPullEnd()) {
                            mLastMotionY = y;
                            mLastMotionX = x;
                            mIsBeingDragged = true;
                            if (mMode == Mode.BOTH) {
                                mCurrentMode = Mode.PULL_FROM_END;
                            }
                        }
                    }
                }
                break;
            }
            case MotionEvent.ACTION_DOWN: {
                if (isReadyForPull()) {
                    mLastMotionY = mInitialMotionY = event.getY();
                    mLastMotionX = mInitialMotionX = event.getX();
                    //FIXME previously false
                    mIsBeingDragged = false;
                } else {
                    mInitialMotionY = mLastMotionY = event.getY();
                    mInitialMotionX = mLastMotionX = event.getX();
                    if (mScrollState == SCROLL_STATE_SETTLING) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                        setScrollState(SCROLL_STATE_DRAGGING);
                    }
                }
                break;
            }

            //FIXME
            case MotionEvent.ACTION_POINTER_DOWN:
                mScrollPointerId = event.getPointerId(actionIndex);
                mLastMotionX = mInitialMotionX = event.getX(actionIndex) + 0.5f;
                mLastMotionY = mInitialMotionY = event.getY(actionIndex) + 0.5f;
                break;

            //FIXME
            case MotionEvent.ACTION_POINTER_UP: {
                onPointerUp(event);
            }
            break;

            //FIXME
            case MotionEvent.ACTION_UP: {
                mVelocityTracker.clear();
            }
            break;

            //FIXME
            case MotionEvent.ACTION_CANCEL: {
                cancelTouch();
            }
        }

        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isPullToRefreshEnabled()) {
            return false;
        }

        //FIXME
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        final int actionIndex = event.getActionIndex();

        // If we're refreshing, and the flag is set. Eat the event
        if (!mScrollingWhileRefreshingEnabled && isRefreshing()) {
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN && event.getEdgeFlags() != 0) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE: {
                //FIXME
                {
                    final float x = event.getX();
                    final float y = event.getY();

                    if (mScrollState != SCROLL_STATE_DRAGGING) {
                        final float dx = x - mInitialMotionX;
                        final float dy = y - mInitialMotionX;
                        boolean startScroll = false;
                        if (Math.abs(dy) > mTouchSlop) {
                            mLastMotionY = mInitialMotionY + mTouchSlop * (dy < 0 ? -1 : 1);
                            startScroll = true;
                        }
                        if (startScroll) {
                            getParent().requestDisallowInterceptTouchEvent(true);
                            setScrollState(SCROLL_STATE_DRAGGING);
                            //FIXME add here
                            //mIsBeingDragged = true;
                        }
                    }
                    //if (mScrollState == SCROLL_STATE_DRAGGING) {
                        final float dx = x - mLastMotionX;
                        final float dy = y - mLastMotionY;
                        scrollByInternal(0, -dy);
                    //}
                    mLastMotionX = x;
                    mLastMotionY = y;
                }

                if (mIsBeingDragged) {
                    mLastMotionY = event.getY();
                    mLastMotionX = event.getX();
                    pullEvent();
                    return true;
                }
                break;
            }

            case MotionEvent.ACTION_DOWN: {
                if (isReadyForPull()) {

                    mLastMotionY = mInitialMotionY = event.getY();
                    mLastMotionX = mInitialMotionX = event.getX();
                    return true;
                } else {
                    mScrollPointerId = event.getPointerId(0);

                    mLastMotionY = mInitialMotionY = event.getY()+0.5f;
                    mLastMotionX = mInitialMotionX = event.getX()+0.5f;
                }
                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {

                //FIXME
                mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
                final float xvel = 0;
                final float yvel = -VelocityTrackerCompat.getYVelocity(mVelocityTracker, mScrollPointerId);
                //if (!((xvel != 0 || yvel != 0) && fling((int) xvel, (int) yvel))) {
                if (!(xvel != 0 || yvel != 0)) {
                    setScrollState(SCROLL_STATE_IDLE);
                }
                mVelocityTracker.clear();
                //releaseGlows();

                if (mIsBeingDragged) {
                    mIsBeingDragged = false;

                    if (mState == State.RELEASE_TO_REFRESH) {

                        if (null != mOnRefreshListener) {
                            setState(State.REFRESHING, true);
                            mOnRefreshListener.onRefresh(this);
                            return true;

                        } else if (null != mOnRefreshListener2) {
                            setState(State.REFRESHING, true);
                            if (mCurrentMode == Mode.PULL_FROM_START) {
                                mOnRefreshListener2.onPullDownToRefresh(this);
                            } else if (mCurrentMode == Mode.PULL_FROM_END) {
                                mOnRefreshListener2.onPullUpToRefresh(this);
                            }
                            return true;
                        }
                    }

                    // If we're already refreshing, just scroll back to the top
                    if (isRefreshing()) {
                        smoothScrollTo(0);
                        return true;
                    }

                    // If we haven't returned by here, then we're not in a state
                    // to pull, so just reset
                    setState(State.RESET);

                    return true;
                }
                break;
            }

            //FIXME
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                mScrollPointerId = MotionEventCompat.getPointerId(event, actionIndex);
                mInitialMotionX = mLastMotionX = event.getX(actionIndex) + 0.5f;
                mInitialMotionY = mLastMotionY = event.getY(actionIndex) + 0.5f;
            }
            break;

            //FIXME
            case MotionEventCompat.ACTION_POINTER_UP: {
                onPointerUp(event);
            }
            break;

        }

        return false;
    }

    private int mScrollPointerId = INVALID_POINTER;
    private int mScrollState = SCROLL_STATE_IDLE;

    private static final int INVALID_POINTER = -1;
    private VelocityTracker mVelocityTracker;
    private static final int mMaxFlingVelocity = 8000;
    /**
     * The view is not currently scrolling.
     *
     * @see #getScrollState()
     */
    public static final int SCROLL_STATE_IDLE = 0;

    /**
     * The view is currently being dragged by outside input such as user touch input.
     *
     * @see #getScrollState()
     */
    public static final int SCROLL_STATE_DRAGGING = 1;

    /**
     * The view is currently animating to a final position while not under outside control.
     *
     * @see #getScrollState()
     */
    public static final int SCROLL_STATE_SETTLING = 2;

    /**
     * Return the current scrolling state of the view.
     *
     * @return {@link #SCROLL_STATE_IDLE}, {@link #SCROLL_STATE_DRAGGING} or {@link #SCROLL_STATE_SETTLING}
     */
    public int getScrollState() {
        return mScrollState;
    }

    private void setScrollState(int state) {
        if (state == mScrollState) {
            return;
        }
        mScrollState = state;
        /*if (state != SCROLL_STATE_SETTLING) {
            stopScroll();
        }*/
        if (mScrollingListener != null) {
            mScrollingListener.onScrollStateChanged(state);
        }
    }

    private void onPointerUp(MotionEvent e) {
        final int actionIndex = MotionEventCompat.getActionIndex(e);
        if (e.getPointerId(actionIndex) == mScrollPointerId) {
            // Pick a new pointer to pick up the slack.
            final int newIndex = actionIndex == 0 ? 1 : 0;
            mScrollPointerId = e.getPointerId(newIndex);
            mInitialMotionX = mLastMotionX = (int) (e.getX(newIndex) + 0.5f);
            mInitialMotionY = mLastMotionY = (int) (e.getY(newIndex) + 0.5f);
        }
    }

    private void cancelTouch() {
        mVelocityTracker.clear();
        //releaseGlows();
        setScrollState(SCROLL_STATE_IDLE);
    }

    /**
     * Does not perform bounds checking. Used by internal methods that have already validated input.
     */
    void scrollByInternal(float x , float y) {

        if (mScrollingListener != null && (x != 0 || y != 0)) {
            mScrollingListener.onScrolled(x, y);
        }
    }

}
