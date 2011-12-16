package com.web.all;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.webkit.WebBackForwardList;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ProgressBar;


/**
 * Private class for maintaining Tabs with a main WebView and a subwindow.
 */
public class Tab implements OnClickListener{
    // Main WebView
    private ScWebView mMainView;
    private Bundle mSavedState;

    // Keep the original url around to avoid killing the old WebView if the
    // url has not changed.
    private String mOriginalUrl;
    private boolean mAttachToView = false;
    TabControl mTabs;
    String mPattern;
    // Construct a new tab
    public Tab(ScWebView w, String url, TabControl control, String pattern) {
        mMainView = w;
        mOriginalUrl = url;
        mTabs = control;
        mPattern = pattern;
		createIndicatorView(mTabs.mTabbar, mTabs.mActivity);
    }
    
    public void clearHistory() {
        // TODO: if a tab is freed due to low memory, its history is not
        // cleared here.
        if (mMainView != null) {
            mMainView.clearHistory();
        }
    }

    void destroy() {
        if (mMainView != null) {
            mMainView.destroy();
            mMainView = null;
        }
    }
    private static final String ORIGINALURL = "originalUrl";

    public Bundle saveState() {
        // If the WebView is null it means we ran low on memory and we
        // already stored the saved state in mSavedState.
        if (mMainView == null) {
            return mSavedState;
        }
        
        final Bundle b = new Bundle();
        if (mOriginalUrl != null) {
            b.putString(ORIGINALURL, mOriginalUrl);
        }

        // TODO: why Remember the saved state?
        mSavedState = b;
        return b;
    }
    
    public void restoreAsBackground(Bundle b) {
        if (b != null) {
            mSavedState = b;
            mOriginalUrl = mSavedState.getString(ORIGINALURL);
        }
    }
    
    public boolean restoreState() {
    	return restoreState(mSavedState);
    }
    
    /*
     * Restore the state of the tab.
     */
    public boolean restoreState(Bundle b) {
        if (b == null) {
            return false;
        }
        // Restore the internal state even if the WebView fails to restore.
        // This will maintain the app id, original url and close-on-exit values.
        mSavedState = null;
        mOriginalUrl = b.getString(ORIGINALURL);
        return true;
    }


    /**
     * Return the top window of this tab; either the subwindow if it is not
     * null or the main window.
     * @return The top window of this tab.
     */
    public WebView getTopWindow() {
        return mMainView;
    }
        
    public void attachToView() {
    	if (mAttachToView) {
    		return;
    	}
    	mAttachToView = true;
    	mTabs.mActivity.attachTabToContentView(mMainView);
		this.setSelected(true);
    }
    public boolean mInLoad = false;

    public void loadUrl(String url) {
    	attachToView();
    	mMainView.loadUrl(url);
    }
    
    public void detachFromView() {
    	if (!mAttachToView) {
    		return;
    	}
    	//Log.e("Tab","detachFromView");

    	mTabs.mActivity.removeTabFromContentView(mMainView);
        mAttachToView = false;
		this.setSelected(false);
    }
    
    void free() {
        // Store the WebView's state.
        saveState();
        mMainView.destroy();
        mMainView = null;
    }
    
    public ScWebView getWebView() {
        return mMainView;
    }

    /**
     * Recreate the main WebView of the given tab. Returns true if the WebView
     * was deleted.
     */
    boolean recreateWebView(String url) {
        if (mMainView != null) {
            if (url != null && url.equals(mOriginalUrl)) {
                // The original url matches the current url. Just go back to the
                // first history item so we can load it faster than if we
                // rebuilt the WebView.
                final WebBackForwardList list = mMainView.copyBackForwardList();
                if (list != null) {
                	mMainView.goBackOrForward(-list.getCurrentIndex());
                	mMainView.clearHistory(); // maintains the current page.
                    return false;
                }
            }
            mMainView.destroy();
        }
        // Create a new WebView. If this tab is the current tab, we need to put
        // back all the clients so force it to be the current tab.
        mMainView = mTabs.createNewWebView();
        if (mTabs.getCurrentTab() == this) {
            mTabs.setCurrentTab(this, true);
        }
        // Clear the saved state except for the app id and close-on-exit
        // values.
        mSavedState = null;
        // Save the new url in order to avoid deleting the WebView.
        mOriginalUrl = url;
        return true;
    }
    
    public void display() {
        boolean needRestore = (mMainView == null);
        if (needRestore) {
            // Same work as in createNewTab() except don't do new Tab()
            mMainView = mTabs.createNewWebView();
        }
        WebBrowse mActivity = mTabs.mActivity;
        mMainView.setWebViewClient(mActivity.getWebViewClient());
        mMainView.setWebChromeClient(mActivity.mWebChromeClient);
        mMainView.setOnCreateContextMenuListener(mActivity);
        
        if (needRestore) {
            if (!restoreState()) {
            	//mMainView.loadUrl(WebBrowse.HOME_URL);
            }
        }
    	
    }
    public void loadHome() {
    	//I think this should be better
    	while(mMainView.canGoBack()) {
    		mMainView.goBack();
    	}
    }
       
    public void setTabActive(boolean active) {
		if (active) {
			tv.setText("");
			favicon.setVisibility(View.VISIBLE);
		} else {
			tv.setText(getTitle());
			favicon.setVisibility(View.INVISIBLE);
		}
    }
    
    public void setCurrentTab() {
      mTabs.setCurrentTab(this);
      attachToView();
    }
    
    /**
     * Get the title of this tab.  Valid after calling populatePickerData, 
     * but before calling wipePickerData, or if the webview has been 
     * destroyed.  If the url has no title, use the url instead.
     * 
     * @return The WebView's title (or url) or null.
     */
    public CharSequence getTitle() {
      if (mMainView != null) {
  		return mMainView.getTitle();
      }
      return null;
    }

    public boolean hasView(WebView view) {
    	return mMainView == view;
    }

    View me;
    private ProgressBar progress;
    private ImageView favicon;
    private TextView tv;
    private CharSequence mTitle;
   // private final static String TITLE = "Close";
    public void setSelected(boolean selected) {
    	me.setSelected(selected);
    	if (!selected && mTitle != null) {
          favicon.setVisibility(View.GONE);
    	  setTitleNoCon(mTitle);
    	}
    }

	public void onClick(View v) {
		// unselect 
	  if (mTabs.getCurrentTab() != this) {
		mTabs.unselect();
		// switch tab view
		setCurrentTab();
		//mTabs.setCurTabOwnerView();
		me.setSelected(true);
	  } else {
	    this.mTabs.mActivity.closeCurrentTab();
	  }
	}

    //private final CharSequence mLabel; 
    private void createIndicatorView(ViewGroup vg, Context c) {
        LayoutInflater inflater =
                (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // tab_indicator
        me = inflater.inflate(R.layout.tab_indicator,
                vg, // tab widget is the parent
                false); // no inflate params

        tv = (TextView) me.findViewById(R.id.title);
        favicon = (ImageView) me.findViewById(R.id.close_tab);
		progress = (ProgressBar) me.findViewById(R.id.progress_small);
		progress.setVisibility(View.INVISIBLE);
        me.setOnClickListener(this);
    }
    
    public void updateTabView() {
      mPattern = Constant.getTitleFromUrl(mMainView.getUrl());
      setTitle(getTitle());
      /*
        WebView w = getTopWindow();
        if (w != null) {
            Bitmap b = w.getFavicon();
            if (b != null) {
            	favicon.setImageBitmap(b);
            }
        }
        */
    }
    public void setTitle(CharSequence title) {
      //Log.e("title", (String) title);
      mTitle = title;
      if (mTabs.getCurrentTab() == this) {
        favicon.setVisibility(View.VISIBLE);
        return;
      }
      favicon.setVisibility(View.GONE);

      setTitleNoCon(title);
    }
    
	private void setTitleNoCon(CharSequence title) {
		if (title == null) {
			return;
		}
		if (title.length() < 10) {
			tv.setText(title);
		} else {
			// CharSequence cTitile = tv.getText();
			// if (cTitile == null || cTitile.length() == 0) {
			tv.setText(mPattern);
			// }
		}

	}
    
    /*
    public void updateFavicon(Bitmap icon) {
    	favicon.setImageBitmap(icon);
    }
*/
    public void showProcess(boolean show) {
    	if(show == true)
			progress.setVisibility(View.VISIBLE);
		else
			progress.setVisibility(View.INVISIBLE);
    }    
};

