package com.web.all;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 *  Custom layout for an item representing a bookmark in the browser.
 */
class BookmarkItem extends LinearLayout {

    protected TextView    mTextView;
    protected TextView    mUrlText;
   // protected ImageView   mImageView;
    protected String      mUrl;

    /**
     *  Instantiate a bookmark item, including a default favicon.
     *
     *  @param context  The application context for the item.
     */
    BookmarkItem(Context context) {
        super(context);

        LayoutInflater factory = LayoutInflater.from(context);
        factory.inflate(R.layout.history_item, this);
        mTextView = (TextView) findViewById(R.id.title);
        mUrlText = (TextView) findViewById(R.id.url);
     //   mUrlText = (TextView) findViewById(R.id.url);
     //   mImageView = (ImageView) findViewById(R.id.favicon);
        View star = findViewById(R.id.star);
        star.setVisibility(View.GONE);
    }

    /**
     *  Copy this BookmarkItem to item.
     *  @param item BookmarkItem to receive the info from this BookmarkItem.
     */
    /* package */ void copyTo(BookmarkItem item) {
        item.mTextView.setText(mTextView.getText());
   //     item.mUrlText.setText(mUrlText.getText());
    //    item.mImageView.setImageDrawable(mImageView.getDrawable());
    }

    /**
     * Return the name assigned to this bookmark item.
     */
    /* package */ String getName() {
        return mTextView.getText().toString();
    }

    /**
     * Return the TextView which holds the name of this bookmark item.
     */
    /* package */ TextView getNameTextView() {
        return mTextView;
    }

    /* package */ String getUrl() {
        return mUrl;
    }

    /**
     *  Set the new name for the bookmark item.
     *
     *  @param name The new name for the bookmark item.
     */
    /* package */ void setName(String name) {
        mTextView.setText(name);
    }
    
    /**
     *  Set the new url for the bookmark item.
     *  @param url  The new url for the bookmark item.
     */
    /* package */ void setUrl(String url) {
        mUrl = url;
        if (mUrlText != null) {
        	mUrlText.setText(mUrl);
        }
    }
}
