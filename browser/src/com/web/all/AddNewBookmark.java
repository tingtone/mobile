package com.web.all;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 *  Custom layout for an item representing a bookmark in the browser.
 */
 // FIXME: Remove BrowserBookmarkItem
class AddNewBookmark extends LinearLayout {

    private TextView    mTextView;
    private TextView    mUrlText;
    private ImageView   mImageView;

    /**
     *  Instantiate a bookmark item, including a default favicon.
     *
     *  @param context  The application context for the item.
     */
    AddNewBookmark(Context context) {
        super(context);

        setWillNotDraw(false);
        LayoutInflater factory = LayoutInflater.from(context);
        factory.inflate(R.layout.add_new_bookmark, this);
        mTextView = (TextView) findViewById(R.id.title);
        mUrlText = (TextView) findViewById(R.id.url);
        mImageView = (ImageView) findViewById(R.id.favicon);
    }

    /**
     *  Set the new url for the bookmark item.
     *  @param url  The new url for the bookmark item.
     */
    /* package */ void setUrl(String url) {
        mUrlText.setText(url);
    }
}
