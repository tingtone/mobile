package com.web.all;


import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class TabControl {
    // Log Tag
    private static final String LOGTAG = "TabControl";
    // Maximum number of tabs.
    static final int MAX_TABS = 8;
    // Static instance of an empty callback.
    private static final WebViewClient mEmptyClient =
            new WebViewClient();
    // Instance of BackgroundChromeClient for background tabs.
    private final BackgroundChromeClient mBackgroundChromeClient =
            new BackgroundChromeClient();
    // Private array of WebViews that are used as tabs.
    private ArrayList<Tab> mTabs = new ArrayList<Tab>(MAX_TABS);
    // Current position in mTabs.
    private int mCurrentTab = -1;
    // A private instance of BrowserActivity to interface with when adding and
    // switching between tabs.
    public final WebBrowse mActivity;

    // Background WebChromeClient for focusing tabs
    private class BackgroundChromeClient extends WebChromeClient {
        @Override
        //TODO: what's this? is it always background, then no need to find if it is current tab?
        public void onRequestFocus(WebView view) {
            Tab t = getTabFromView(view);
            if (t != getCurrentTab()) {
            	switchViews(t, false);
            }
        }
    }
    
    // TODO, make loadHome work
    private void switchViews(Tab nextTab, boolean loadHome) {
    	if (nextTab == null)
    		return;
		//Tab nextTab = getTab(nextTabIdx);
      	getCurrentTab().detachFromView();
		// move to next view, may release tab due to memory
		setCurrentTab(nextTab);
		nextTab.attachToView();
    }

    public void switchViews(Tab nextTab) {
    	if (nextTab == null)
    		return;
		//Tab nextTab = getTab(nextTabIdx);
      	getCurrentTab().detachFromView();
		// move to next view, may release tab due to memory
		setCurrentTab(nextTab);
		nextTab.attachToView();
    }
    
    TabBar mTabbar;
        
    public void unselect() {
    	getCurrentTab().setSelected(false);
    }
	
	
    public void switchViews(boolean right) {
    	Tab nextTab;
    	if (right) {
    		nextTab = getRightTab();
    		if (nextTab == null) {
    			mActivity.newTab();
    			return;
    		}
    	} else {
    		nextTab = getLeftTab();
    		if (nextTab == null) {
    		  /*nextTab = createNewTab(true, null, "google");
              if (nextTab != null) {
            	  nextTab.setCurrentTab();
            	  mActivity.startSearch(null, false,null, false);
            	  return ;
              }*/
    		}
    	}
    	switchViews(nextTab, false);
    }
    
    // Directory to store thumbnails for each WebView.
    private final File mThumbnailDir;

    /**
     * Construct a new TabControl object that interfaces with the given
     * BrowserActivity instance.
     * @param activity A BrowserActivity instance that TabControl will interface
     *                 with.
     */
    public TabControl(WebBrowse activity) {
        mActivity = activity;

        mThumbnailDir = activity.getDir("thumbnails", 0);
        mTabbar = (TabBar) activity.findViewById(R.id.tabBar);
    }

    File getThumbnailDir() {
        return mThumbnailDir;
    }

    WebBrowse getBrowserActivity() {
        return mActivity;
    }

    /**
     * Return the current tab's main WebView. This will always return the main
     * WebView for a given tab and not a subwindow.
     * @return The current tab's WebView.
     */
    public WebView getCurrentWebView() {
        Tab t = getTab(mCurrentTab);
        if (t == null) {
            return null;
        }
        return t.getWebView();
    }

    WebView getCurrentTopWebView() {
        Tab t = getTab(mCurrentTab);
        if (t == null) {
            return null;
        }
        return t.getWebView();
    }

    Tab getTab(int index) {
        if (index >= 0 && index < mTabs.size()) {
            return mTabs.get(index);
        }
        return null;
    }

    Tab getCurrentTab() {
        return getTab(mCurrentTab);
    }

    int getCurrentIndex() {
        return mCurrentTab;
    }
    
    public int getTabIndex(Tab tab) {
        return mTabs.indexOf(tab);
    }

    Tab createNewTab(boolean background, String url, String pattern) {
        int size = mTabs.size();
        // Return false if we have maxed out on tabs
        if (MAX_TABS == size) {
        	Toast.makeText(mActivity, R.string.tab_out_max_warning, Toast.LENGTH_LONG).show();
            return null;
        }
        final ScWebView w = createNewWebView();
        // Create a new tab and add it to the tab list
        Tab t = new Tab(w, url, this, pattern);
        mTabs.add(t);
        // Initially put the tab in the background.
        if (background) {
          putTabInBackground(t);
        }

		mTabbar.addView(t.me);
        return t;
    }

    boolean removeTab(Tab t) {
    	//Log.e("TabCtrl","remove tab");
    	if (t == null) {
            return false;
        }
        // Only remove the tab if it is the current one.
        if (getCurrentTab() == t) {
            putTabInBackground(t);
            if (mTabs.size() == 0) {
                mCurrentTab = -1;
            } else {
            	 mCurrentTab = 0;
            }
        }

        // Only destroy the WebView if it still exists.
        t.destroy();
        // Remove it from our list of tabs.
        mTabs.remove(t);
		mTabbar.removeView(t.me);
        return true;
    }
     
    void clearHistory() {
        int size = getTabCount();
        for (int i = 0; i < size; i++) {
            Tab t = mTabs.get(i);
            t.clearHistory();
        }
    }

    void destroy() {
        for (Tab t : mTabs) {
        	t.destroy();
        }
        mTabs.clear();
    }

    /**
     * Returns the number of tabs created.
     * @return The number of tabs created.
     */
    int getTabCount() {
        return mTabs.size();
    }

    // Used for saving and restoring each Tab
    private static final String WEBVIEW = "webview";
    private static final String NUMTABS = "numTabs";
    private static final String CURRTAB = "currentTab";

    /**
     * Save the state of all the Tabs.
     * @param outState The Bundle to save the state to.
     */
    void saveState(Bundle outState) {
        final int numTabs = getTabCount();
        outState.putInt(NUMTABS, numTabs);
        final int index = getCurrentIndex();
        outState.putInt(CURRTAB, (index >= 0 && index < numTabs) ? index : 0);
        for (int i = 0; i < numTabs; i++) {
            final Tab t = getTab(i);
            outState.putBundle(WEBVIEW + i, t.saveState());
        }
    }

    /**
     * Restore the state of all the tabs.
     * @param inState The saved state of all the tabs.
     * @return True if there were previous tabs that were restored. False if
     *         there was no saved state or restoring the state failed.
     */
    boolean restoreState(Bundle inState) {
        final int numTabs = (inState == null)
                ? -1 : inState.getInt(NUMTABS, -1);
        if (numTabs == -1) {
            return false;
        } else {
            final int currentTab = inState.getInt(CURRTAB, -1);
            for (int i = 0; i < numTabs; i++) {
                if (i == currentTab) {
                    Tab t = createNewTab(false, null, "");
                    // Me must set the current tab before restoring the state
                    // so that all the client classes are set.
                    setCurrentTab(t);
                    if (!t.restoreState(inState.getBundle(WEBVIEW + i))) {
                       // t.loadUrl(WebBrowse.HOME_URL);
                    }
                } else {
                    // Create a new tab and don't restore the state yet, add it
                    // to the tab list
                    Tab t = new Tab(null, null, this, "");
                    t.restoreAsBackground(inState.getBundle(WEBVIEW + i));
                    mTabs.add(t);
                }
            }
        }
        return true;
    }

    /**
     * Free the memory in this order, 1) free the background tab; 2) free the
     * WebView cache;
     */
    void freeMemory() {
        // free the least frequently used background tab
        Tab t = getLeastUsedTab();
        if (t != null) {
            Log.w(LOGTAG, "Free a tab in the browser");
            t.free();
            // force a gc
            System.gc();
            return;
        }

        // free the WebView cache
        Log.w(LOGTAG, "Free WebView cache");
        WebView view = getCurrentWebView();
        if (view != null) {
            view.clearCache(false);
        }
        // force a gc
        System.gc();
    }
    
    public Tab getRightTab() {
    	return getTab(mCurrentTab + 1);
    }
    
    public Tab getLeftTab() {
    	return getTab(mCurrentTab - 1);
    }

    private Tab getLeastUsedTab() {
        // Don't do anything if we only have 1 tab.
    	int len = mTabs.size();
        if (len == 1) {
            return null;
        }
        Tab t;
        int i = 0;
        do {
            t = mTabs.get(i++);
        } while (i < len && t != null && t.getWebView() == null);

        // Don't do anything if the last remaining tab is the current one.
        if (t == getCurrentTab()) {
            return null;
        }
        return t;
    }

    Tab getTabFromView(WebView view) {
        final int size = getTabCount();
        for (int i = 0; i < size; i++) {
            final Tab t = getTab(i);
            if (t.hasView(view)) {
                return t;
            }
        }
        return null;
    }

    public ScWebView createNewWebView() {
        // Create a new WebView
        ScWebView w = new ScWebView(mActivity, this);
        if (Constant.sdk >= 5) {
          Api5.setScrollbarFadingEnabled(w, true);
        }
        w.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        WebSettings s = w.getSettings();
    		s.setJavaScriptEnabled(true);
    		s.setLoadsImagesAutomatically(true);
    		w.setMapTrackballToArrowKeys(false); // use trackball directly
    		s.setSupportZoom(true);
    	   	s.setBuiltInZoomControls(true);
    		s.setSupportMultipleWindows(true);
    		s.setSaveFormData(true);
    		s.setSavePassword(true);
    		
    		s.setNeedInitialFocus(false);
    		s.setLightTouchEnabled(true);
    		if (Constant.sdk >= 8) {
    			Api5.setPluginState(s, 1);
    		} else {
    			s.setPluginsEnabled(true);
    		}
    		if (Constant.sdk >= 7) {
    			Api5.moreSettingFor7(s);
    		}
        return w;
    }

    /**
     * Put the current tab in the background and set newTab as the current tab.
     * @param newTab The new tab. If newTab is null, the current tab is not
     *               set.
     */
    boolean setCurrentTab(Tab newTab) {
        return setCurrentTab(newTab, false);
    }

    /**
     * If force is true, this method skips the check for newTab == current.
     */
    public boolean setCurrentTab(Tab newTab, boolean force) {
        Tab current = getTab(mCurrentTab);
        if (current == newTab && !force) {
            return true;
        }
        if (current != null) {
            // Remove the current WebView and the container of the subwindow
            putTabInBackground(current);
            current.detachFromView();
        }

        if (newTab == null) {
            return false;
        }

        // Display the new current tab
        if (current != null) {
        	current.setTabActive(false);
        }
        newTab.setTabActive(true);
        mCurrentTab = mTabs.indexOf(newTab);
        newTab.display();
        Constant.wb.refreshProgress();
        return true;
    }

    /*
     * Put the tab in the background using all the empty/background clients.
     */
    private void putTabInBackground(Tab t) {
        WebView mainView = t.getWebView();
        // Set an empty callback so that default actions are not triggered.
        //mainView.setWebViewClient(mEmptyClient);
        //mainView.setWebChromeClient(mBackgroundChromeClient);
        mainView.setOnCreateContextMenuListener(null);
    }
}
