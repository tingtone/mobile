package com.web.all;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.ParseException;

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class AddBookmarkPage extends Activity {

    private EditText    mTitle;
    private EditText    mAddress;
    private TextView    mButton;
    private View        mCancelButton;
    private boolean     mEditingExisting;
    private Bundle      mMap;
    private String      mTouchIconUrl;

    private View.OnClickListener mSaveBookmark = new View.OnClickListener() {
        public void onClick(View v) {
            if (save()) {
                finish();
                Toast.makeText(AddBookmarkPage.this, R.string.bookmark_saved,
                        Toast.LENGTH_LONG).show();
            }
        }
    };

    private View.OnClickListener mCancel = new View.OnClickListener() {
        public void onClick(View v) {
            finish();
        }
    };

    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.browser_add_bookmark);
        setTitle(R.string.add_to_bookmarks);
		getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
				R.drawable.ic_tab_bookmarks_unselected);
        
        String title = null;
        String url = null;
        mMap = getIntent().getExtras();
        if (mMap != null) {
            Bundle b = mMap.getBundle("bookmark");
            if (b != null) {
                mMap = b;
                mEditingExisting = true;
                setTitle(R.string.edit_bookmark);
            }
            title = mMap.getString("title");
            url = mMap.getString("url");
            mTouchIconUrl = mMap.getString("touch_icon_url");
        }

        mTitle = (EditText) findViewById(R.id.title);
        mTitle.setText(title);
        mAddress = (EditText) findViewById(R.id.address);
        mAddress.setText(url);


        View.OnClickListener accept = mSaveBookmark;
        mButton = (TextView) findViewById(R.id.OK);
        mButton.setOnClickListener(accept);

        mCancelButton = findViewById(R.id.cancel);
        mCancelButton.setOnClickListener(mCancel);
        
        if (!getWindow().getDecorView().isInTouchMode()) {
            mButton.requestFocus();
        }
    }
    
    /* package */ static String fixUrl(String inUrl) {
      if (inUrl.startsWith("http://") || inUrl.startsWith("https://"))
          return inUrl;
      if (inUrl.startsWith("http:") ||
              inUrl.startsWith("https:")) {
          if (inUrl.startsWith("http:/") || inUrl.startsWith("https:/")) {
              inUrl = inUrl.replaceFirst("/", "//");
          } else inUrl = inUrl.replaceFirst(":", "://");
      }
      return inUrl;
  }
    
    /**
     *  Save the data to the database. 
     *  Also, change the view to dialog stating 
     *  that the webpage has been saved.
     */
    boolean save() {
        String title = mTitle.getText().toString().trim();
        String unfilteredUrl = 
                fixUrl(mAddress.getText().toString());
        boolean emptyTitle = title.length() == 0;
        boolean emptyUrl = unfilteredUrl.trim().length() == 0;
        Resources r = getResources();
        if (emptyTitle || emptyUrl) {
            if (emptyTitle) {
                mTitle.setError(r.getText(R.string.bookmark_needs_title));
            }
            if (emptyUrl) {
                mAddress.setError(r.getText(R.string.bookmark_needs_url));
            }
            return false;
        }
        String url = unfilteredUrl;
        if (!(url.startsWith("about:") || url.startsWith("data:") || url
                .startsWith("file:"))) {
            WebAddress address;
            try {
                address = new WebAddress(unfilteredUrl);
            } catch (ParseException e) {
                mAddress.setError(r.getText(R.string.bookmark_url_not_valid));
                return false;
            }
            if (address.mHost.length() == 0) {
                mAddress.setError(r.getText(R.string.bookmark_url_not_valid));
                return false;
            }
            url = address.toString();
        }
        try {
            if (mEditingExisting) {
                mMap.putString("title", title);
                mMap.putString("url", url);
                setResult(RESULT_OK, (new Intent()).setAction(
                        getIntent().toString()).putExtras(mMap));
            } else {
                final ContentResolver cr = getContentResolver();
                Bookmarks.addBookmark(null, cr, url, title, true);
                if (mTouchIconUrl != null) {
                    final Cursor c =
                            BrowserBookmarksAdapter.queryBookmarksForUrl(
                                    cr, null, url, true);
                    new DownloadTouchIcon(cr, c, url)
                            .execute(mTouchIconUrl);
                }
                setResult(RESULT_OK);
            }
        } catch (IllegalStateException e) {
            setTitle(r.getText(R.string.no_database));
            return false;
        }
        return true;
    }
}
