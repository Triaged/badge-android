package com.triaged.badge.ui.base.views;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * A custom viewpager to prevent user from swiping
 * between pages
 * Created by Sadegh Kazemy on 9/23/14.
 */

    public class FlexViewPager extends ViewPager {

        private boolean enabled;

        public FlexViewPager(Context context, AttributeSet attrs) {
            super(context, attrs);
            this.enabled = true;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (this.enabled) {
                return super.onTouchEvent(event);
            }

            return false;
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent event) {
            if (this.enabled) {
                return super.onInterceptTouchEvent(event);
            }

            return false;
        }

        public void setPagingEnabled(boolean enabled) {
            this.enabled = enabled;
        } }