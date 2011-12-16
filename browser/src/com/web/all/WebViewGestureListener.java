package com.web.all;


import android.view.GestureDetector;
import android.view.MotionEvent;

public class WebViewGestureListener extends GestureDetector.SimpleOnGestureListener {
	private TabControl mTabs;
	public WebViewGestureListener(TabControl tabs) {
		mTabs = tabs;
	}
    @Override
    public boolean onSingleTapUp(MotionEvent ev) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent ev) {
    }

    @Override
    public void onLongPress(MotionEvent ev) {
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        ScWebView view = (ScWebView) mTabs.getCurrentWebView();
        view.doScroll(e1, e2, distanceX, distanceY);
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        //CalendarView view = (CalendarView) mViewSwitcher.getCurrentView();
        //view.doFling(e1, e2, velocityX, velocityY);
        return true;
    }

    @Override
    public boolean onDown(MotionEvent ev) {
        ScWebView view = (ScWebView) mTabs.getCurrentWebView();
        view.onDown(ev);
        return true;
    }
}