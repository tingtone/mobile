package com.web.all;

import android.app.Activity;
import android.app.TabActivity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.Browser;
import android.webkit.WebIconDatabase.IconListener;
import android.widget.TabHost;
import android.view.Window;

import java.util.HashMap;
import java.util.Vector;

public class CombinedBookmarkHistoryActivity extends TabActivity
        implements TabHost.OnTabChangeListener {
    /* package */ static String BOOKMARKS_TAB = "bookmark";
    /* package */ static String VISITED_TAB = "visited";
    /* package */ static String HISTORY_TAB = "history";
    /* package */ static String STARTING_TAB = "tab";

    static class IconListenerSet implements IconListener {
        // Used to store favicons as we get them from the database
        // FIXME: We use a different method to get the Favicons in
        // BrowserBookmarksAdapter. They should probably be unified.
        private HashMap<String, Bitmap> mUrlsToIcons;
        private Vector<IconListener> mListeners;

        public IconListenerSet() {
            mUrlsToIcons = new HashMap<String, Bitmap>();
            mListeners = new Vector<IconListener>();
        }
        public void onReceivedIcon(String url, Bitmap icon) {
            mUrlsToIcons.put(url, icon);
            for (IconListener listener : mListeners) {
                listener.onReceivedIcon(url, icon);
            }
        }
        public void addListener(IconListener listener) {
            mListeners.add(listener);
        }
        public Bitmap getFavicon(String url) {
            return (Bitmap) mUrlsToIcons.get(url);
        }
    }
    private static IconListenerSet sIconListenerSet;
    static IconListenerSet getIconListenerSet(ContentResolver cr) {
        if (null == sIconListenerSet) {
            sIconListenerSet = new IconListenerSet();
            Browser.requestAllIcons(cr, null, sIconListenerSet);
        }
        return sIconListenerSet;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.tabs);
        getTabHost().setOnTabChangedListener(this);

        Intent startIntent = getIntent();
        Bundle extras = startIntent.getExtras();
        String pattern = startIntent.getStringExtra(Constant.PATTERN);
        Resources resources = getResources();

        getIconListenerSet(getContentResolver());
        Intent bookmarksIntent = new Intent(this, BrowserBookmarksPage.class);
        bookmarksIntent.putExtras(extras);
        try {
        	createTab(bookmarksIntent, R.string.tab_bookmarks, BOOKMARKS_TAB, R.drawable.browser_bookmark_tab);
        } catch (Exception e) {
		
		}

        Intent visitedIntent = new Intent(this, BrowserBookmarksPage.class);
        // Need to copy extras so the bookmarks activity and this one will be
        // different
        Bundle visitedExtras = new Bundle(extras);

        visitedExtras.putInt(BrowserBookmarksPage.BOOKMARK_TYPE, BrowserBookmarksPage.MOSTVISIT_BOOKMARK);
        visitedIntent.putExtras(visitedExtras);
        try {
        	createTab(visitedIntent, R.string.tab_most_visited, VISITED_TAB, R.drawable.browser_visited_tab);
        } catch (Exception e) {
        	
		}

        Intent historyIntent = new Intent(this, BrowserHistoryPage.class);
        historyIntent.putExtras(extras);
        try {
        	createTab(historyIntent, R.string.tab_res, HISTORY_TAB, R.drawable.browser_resource_tab);
        } catch (Exception e) {
		}

        if (pattern != null && pattern.length() > 0) {
          Intent selfIntent = new Intent(this, BrowserBookmarksPage.class);
          selfIntent.putExtra(BrowserBookmarksPage.BOOKMARK_TYPE, BrowserBookmarksPage.SELF_BOOKMARK);
          selfIntent.putExtras(extras);
          
          createTab(selfIntent, pattern, pattern, R.drawable.browser_history_tab);
        }

        String defaultTab = extras.getString(STARTING_TAB);
        //if (defaultTab != null) {
            getTabHost().setCurrentTab(2);
        //}
    }

    private void createTab(Intent intent, int labelResId, String tab, int iconResId) {
        createTab(intent, getText(labelResId), tab, iconResId);
    }
    
    private void createTab(Intent intent,  CharSequence tabName, String tab, int iconResId) {
    	/*
       LayoutInflater factory = LayoutInflater.from(this);
       View tabHeader = factory.inflate(R.layout.tab_header, null);
       TextView textView = (TextView) tabHeader.findViewById(R.id.tab_label);
       textView.setText(tabName);
       TabHost tabHost = getTabHost();
       tabHost.addTab(tabHost.newTabSpec(tab).setIndicator(tabHeader).setContent(intent));
       */
        TabHost tabHost = getTabHost();
       tabHost.addTab(tabHost.newTabSpec(tab).setIndicator(tabName, getResources().getDrawable(iconResId)).setContent(intent));
   }
    
    // Copied from DialTacts Activity
    /** {@inheritDoc} */
    public void onTabChanged(String tabId) {
        Activity activity = getLocalActivityManager().getActivity(tabId);
        if (activity != null) {
            activity.onWindowFocusChanged(true);
        }
    }

    
}
