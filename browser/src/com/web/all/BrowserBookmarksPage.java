package com.web.all;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Browser;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;

/**
 *  View showing the user's bookmarks in the browser.
 */
public class BrowserBookmarksPage extends Activity implements 
        View.OnCreateContextMenuListener {

    private boolean                 mGridMode;
    private GridView                mGridPage;
    private View                    mVerticalList;
    private BrowserBookmarksAdapter mBookmarksAdapter;
    private static final int        BOOKMARKS_SAVE = 1;
    private boolean                 mMaxTabsOpen;
    private BookmarkItem            mContextHeader;
    private AddNewBookmark          mAddHeader;
    private boolean                 mCanceled = false;
    private boolean                 mCreateShortcut;
    private int mType;
    public static final int SYSTEM_BOOKMARK = 0;
    public static final int SELF_BOOKMARK = 1;
    public static final int MOSTVISIT_BOOKMARK = 2;

   // private boolean                 mMostVisited;
   // private View                    mEmptyView;
    // XXX: There is no public string defining this intent so if Home changes
    // the value, we have to update this string.
    private static final String     INSTALL_SHORTCUT =
            "com.android.launcher.action.INSTALL_SHORTCUT";
    
    private final static String LOGTAG = "browser";


    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // It is possible that the view has been canceled when we get to
        // this point as back has a higher priority 
        if (mCanceled) {
            return true;
        }
        AdapterView.AdapterContextMenuInfo i = 
            (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        // If we have no menu info, we can't tell which item was selected.
        if (i == null) {
            return true;
        }
        
        switch (item.getItemId()) {
        case R.id.new_context_menu_id:
            saveCurrentPage();
            break;
        case R.id.open_context_menu_id:
            loadUrl(i.position);
            break;
        case R.id.edit_context_menu_id:
            editBookmark(i.position);
            break;
        case R.id.shortcut_context_menu_id:
            final Intent send = createShortcutIntent(i.position);
            send.setAction(INSTALL_SHORTCUT);
            sendBroadcast(send);
            break;
        case R.id.delete_context_menu_id:
            if (mType == MOSTVISIT_BOOKMARK) {
                Browser.deleteFromHistory(getContentResolver(),
                        getUrl(i.position));
                refreshList();
            } else {
                displayRemoveBookmarkDialog(i.position);
            }
            break;
        case R.id.new_window_context_menu_id:
            openInNewWindow(i.position);
            break;
        case R.id.share_link_context_menu_id:
            Browser.sendString(BrowserBookmarksPage.this, getUrl(i.position));
            break;
        // Only for the Most visited page
        case R.id.save_to_bookmarks_menu_id:
            /*HistoryItem historyItem = ((HistoryItem) i.targetView);
            // If the site is bookmarked, the item becomes remove from
            // bookmarks.
            if (historyItem.isBookmark()) {
                Bookmarks.removeFromBookmarks(this, getContentResolver(),
                            historyItem.getUrl());
            } else {
                Browser.saveBookmark(this, historyItem.getName(),
                        historyItem.getUrl());
            }*/
        	 boolean isBookmark;
             String name;
             String url;
             if (mGridMode) {
                 isBookmark = mBookmarksAdapter.getIsBookmark(i.position);
                 name = mBookmarksAdapter.getTitle(i.position);
                 url = mBookmarksAdapter.getUrl(i.position);
             } else {
                 HistoryItem historyItem = ((HistoryItem) i.targetView);
                 isBookmark = historyItem.isBookmark();
                 name = historyItem.getName();
                 url = historyItem.getUrl();
             }
             // If the site is bookmarked, the item becomes remove from
             // bookmarks.
             if (isBookmark) {
                 Bookmarks.removeFromBookmarks(this, getContentResolver(), url);
             } else {
                 Browser.saveBookmark(this, name, url);
             }
            break;
        default:
            return super.onContextItemSelected(item);
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                ContextMenuInfo menuInfo) {
            AdapterView.AdapterContextMenuInfo i = 
                    (AdapterView.AdapterContextMenuInfo) menuInfo;

            MenuInflater inflater = getMenuInflater();
            if (mType == MOSTVISIT_BOOKMARK) {
                inflater.inflate(R.menu.historycontext, menu);
            } else {
                inflater.inflate(R.menu.bookmarkscontext, menu);
            }

            if (0 == i.position && mType != MOSTVISIT_BOOKMARK) {
                menu.setGroupVisible(R.id.CONTEXT_MENU, false);
                if (mAddHeader == null) {
                    mAddHeader = new AddNewBookmark(BrowserBookmarksPage.this);
                } else if (mAddHeader.getParent() != null) {
                    ((ViewGroup) mAddHeader.getParent()).
                            removeView(mAddHeader);
                }
                mAddHeader.setUrl(getIntent().getStringExtra("url"));
                menu.setHeaderView(mAddHeader);
                return;
            }
            if (mType == MOSTVISIT_BOOKMARK) {
                if ((!mGridMode && ((HistoryItem) i.targetView).isBookmark())
                        || mBookmarksAdapter.getIsBookmark(i.position)) {
                    MenuItem item = menu.findItem(
                            R.id.save_to_bookmarks_menu_id);
                    item.setTitle(R.string.remove_from_bookmarks);
                }
            } else {
                // The historycontext menu has no ADD_MENU group.
                menu.setGroupVisible(R.id.ADD_MENU, false);
            }
            if (mMaxTabsOpen) {
                menu.findItem(R.id.new_window_context_menu_id).setVisible(
                        false);
            }
            if (mContextHeader == null) {
                mContextHeader = new BookmarkItem(BrowserBookmarksPage.this);
            } else if (mContextHeader.getParent() != null) {
                ((ViewGroup) mContextHeader.getParent()).
                        removeView(mContextHeader);
            }
            if (mGridMode) {
                mBookmarksAdapter.populateBookmarkItem(mContextHeader,
                        i.position);
            } else {
                BookmarkItem b = (BookmarkItem) i.targetView;
                b.copyTo(mContextHeader);
            }
            menu.setHeaderView(mContextHeader);
        }

    /**
     *  Create a new BrowserBookmarksPage.
     */  
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction())) {
            mCreateShortcut = true;
        }
        Intent intent = getIntent();
        mMaxTabsOpen = intent.getBooleanExtra("maxTabsOpen", false);
        mType = intent.getIntExtra(BOOKMARK_TYPE, SYSTEM_BOOKMARK);
        if (mCreateShortcut) {
            setTitle(R.string.browser_bookmarks_page_bookmarks_text);
        }
        mBookmarksAdapter = new BrowserBookmarksAdapter(this,
            intent.getStringExtra(Constant.URL),
            intent.getCharSequenceExtra(Constant.TITLE),
            intent.getCharSequenceExtra(Constant.PATTERN),
            mCreateShortcut,
            mType);
        /*
        if (mType == BrowserBookmarksPage.MOSTVISIT_BOOKMARK) {
            mEmptyView = new ViewStub(this, R.layout.empty_history);
        }
        */
        switchViewMode(true);
    }
    public static final String BOOKMARK_TYPE = "self"; 
    /**
     *  Set the ContentView to be either the grid of thumbnails or the vertical
     *  list.  Pass true to set it to the grid.
     */
    private void switchViewMode(boolean gridMode) {
        mGridMode = gridMode;
        mBookmarksAdapter.switchViewMode(gridMode);
        if (mGridMode) {
            if (mGridPage == null) {
                mGridPage = new GridView(this);
                mGridPage.setAdapter(mBookmarksAdapter);
                mGridPage.setOnItemClickListener(mListener);
                mGridPage.setNumColumns(GridView.AUTO_FIT);
                // Keep this in sync with bookmark_thumb and
                // BrowserActivity.updateScreenshot
                mGridPage.setColumnWidth(100);
                mGridPage.setFocusable(true);
                mGridPage.setFocusableInTouchMode(true);
                mGridPage.setSelector(android.R.drawable.gallery_thumb);
                mGridPage.setVerticalSpacing(10);
                /*
                if (mType == BrowserBookmarksPage.MOSTVISIT_BOOKMARK) {
                    mGridPage.setEmptyView(mEmptyView);
                }
                */
                if (!mCreateShortcut) {
                    mGridPage.setOnCreateContextMenuListener(this);
                }
            }
            setContentView(mGridPage);
        } else {
            if (null == mVerticalList) {
                LayoutInflater factory = LayoutInflater.from(this);
                mVerticalList = factory.inflate(R.layout.browser_bookmarks_page,
                        null);

                ListView listView
                        = (ListView) mVerticalList.findViewById(R.id.list);
                listView.setAdapter(mBookmarksAdapter);
                listView.setDrawSelectorOnTop(false);
                listView.setVerticalScrollBarEnabled(true);
                listView.setOnItemClickListener(mListener);
                /*
                if (mType == BrowserBookmarksPage.MOSTVISIT_BOOKMARK) {
                    listView.setEmptyView(mEmptyView);
                }
*/
                if (!mCreateShortcut) {
                    listView.setOnCreateContextMenuListener(this);
                }
            }
            setContentView(mVerticalList);
        }
        /*
        if (mType == BrowserBookmarksPage.MOSTVISIT_BOOKMARK) {
            addContentView(mEmptyView, new LayoutParams(
                    LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        }*/
    }

    private static final int SAVE_CURRENT_PAGE = 1000;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == SAVE_CURRENT_PAGE) {
                saveCurrentPage();
            }
        }
    };

    private AdapterView.OnItemClickListener mListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView parent, View v, int position, long id) {
            // It is possible that the view has been canceled when we get to
            // this point as back has a higher priority 
            if (mCanceled) {
                android.util.Log.e(LOGTAG, "item clicked when dismissing");
                return;
            }
            if (!mCreateShortcut) {
                if (0 == position && mType != BrowserBookmarksPage.MOSTVISIT_BOOKMARK) {
                    // XXX: Work-around for a framework issue.
                    mHandler.sendEmptyMessage(SAVE_CURRENT_PAGE);
                } else {
                    loadUrl(position);
                }
            } else {
                final Intent intent = createShortcutIntent(position);
                setResultToParent(RESULT_OK, intent);
                finish();
            }
        }
    };

    private Intent createShortcutIntent(int position) {
        String url = getUrl(position);
        String title = getBookmarkTitle(position);
        Bitmap touchIcon = getTouchIcon(position);

        final Intent i = new Intent();
        final Intent shortcutIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse(url));
        long urlHash = url.hashCode();
        long uniqueId = (urlHash << 32) | shortcutIntent.hashCode();
        shortcutIntent.putExtra(Browser.EXTRA_APPLICATION_ID,
                Long.toString(uniqueId));
        i.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        i.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
        // Use the apple-touch-icon if available
        if (touchIcon != null) {
            // Make a copy so we can modify the pixels.
            Bitmap copy = touchIcon.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(copy);

            // Construct a path from a round rect. This will allow drawing with
            // an inverse fill so we can punch a hole using the round rect.
            Path path = new Path();
            path.setFillType(Path.FillType.INVERSE_WINDING);
            path.addRoundRect(new RectF(0, 0, touchIcon.getWidth(),
                    touchIcon.getHeight()), 8f, 8f, Path.Direction.CW);

            // Construct a paint that clears the outside of the rectangle and
            // draw.
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            canvas.drawPath(path, paint);

            i.putExtra(Intent.EXTRA_SHORTCUT_ICON, copy);
        } else {
            Bitmap favicon = getFavicon(position);
            if (favicon == null) {
                i.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                        Intent.ShortcutIconResource.fromContext(
                                BrowserBookmarksPage.this,
                                R.drawable.ic_launcher_shortcut_browser_bookmark));
            } else {
                Bitmap icon = BitmapFactory.decodeResource(getResources(),
                        R.drawable.ic_launcher_shortcut_browser_bookmark);

                // Make a copy of the regular icon so we can modify the pixels.
                Bitmap copy = icon.copy(Bitmap.Config.ARGB_8888, true);
                Canvas canvas = new Canvas(copy);

                // Make a Paint for the white background rectangle and for
                // filtering the favicon.
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG
                        | Paint.FILTER_BITMAP_FLAG);
                p.setStyle(Paint.Style.FILL_AND_STROKE);
                p.setColor(Color.WHITE);

                // Create a rectangle that is slightly wider than the favicon
                final float iconSize = 16; // 16x16 favicon
                final float padding = 2;   // white padding around icon
                final float rectSize = iconSize + 2 * padding;
                final float y = icon.getHeight() - rectSize;
                RectF r = new RectF(0, y, rectSize, y + rectSize);

                // Draw a white rounded rectangle behind the favicon
                canvas.drawRoundRect(r, 2, 2, p);

                // Draw the favicon in the same rectangle as the rounded
                // rectangle but inset by the padding
                // (results in a 16x16 favicon).
                r.inset(padding, padding);
                canvas.drawBitmap(favicon, null, r, p);
                i.putExtra(Intent.EXTRA_SHORTCUT_ICON, copy);
            }
        }
        // Do not allow duplicate items
        i.putExtra("duplicate", false);
        return i;
    }

    private void saveCurrentPage() {
        Intent i = new Intent(BrowserBookmarksPage.this,
                AddBookmarkPage.class);
        i.putExtras(getIntent());
        startActivityForResult(i, BOOKMARKS_SAVE);
    }

    private void loadUrl(int position) {
        Intent intent = (new Intent()).setAction(getUrl(position));
        setResultToParent(RESULT_OK, intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        if (!mCreateShortcut) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.bookmarks, menu);
            // Most visited page does not have an option to bookmark the last
            // viewed page.
            menu.findItem(R.id.new_context_menu_id).setVisible(
                mType != BrowserBookmarksPage.MOSTVISIT_BOOKMARK);
            return true;
        }
        return result;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mBookmarksAdapter.getCount() == 0) {
            // No need to show the menu if there are no items.
            return false;
        }
        menu.findItem(R.id.switch_mode_menu_id).setTitle(
                mGridMode ? R.string.switch_to_list
                : R.string.switch_to_thumbnails);
        menu.findItem(R.id.switch_mode_menu_id).setIcon(
        		mGridMode ? android.R.drawable.ic_menu_agenda
        				: android.R.drawable.ic_menu_mapmode);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.new_context_menu_id:
            saveCurrentPage();
            break;

        case R.id.switch_mode_menu_id:
            switchViewMode(!mGridMode);
            break;

        default:
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void openInNewWindow(int position) {
        Bundle b = new Bundle();
        b.putBoolean(Constant.NEW_TAB, true);
        setResultToParent(RESULT_OK,
                (new Intent()).setAction(getUrl(position)).putExtras(b));

        finish();
    }
    

    private void editBookmark(int position) {
        Intent intent = new Intent(BrowserBookmarksPage.this, 
            AddBookmarkPage.class);
        intent.putExtra("bookmark", getRow(position));
        startActivityForResult(intent, BOOKMARKS_SAVE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        switch(requestCode) {
            case BOOKMARKS_SAVE:
                if (resultCode == RESULT_OK) {
                    Bundle extras;
                    if (data != null && (extras = data.getExtras()) != null) {
                        // If there are extras, then we need to save
                        // the edited bookmark. This is done in updateRow()
                        String title = extras.getString("title");
                        String url = extras.getString("url");
                        if (title != null && url != null) {
                            mBookmarksAdapter.updateRow(extras);
                        }
                    } else {
                        // extras == null then a new bookmark was added to
                        // the database.
                        refreshList();
                    }
                }
                break;
            default:
                break;
        }
    }
    
    private void displayRemoveBookmarkDialog(int position) {
        // Put up a dialog asking if the user really wants to
        // delete the bookmark
        final int deletePos = position;
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_bookmark)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(getText(R.string.delete_bookmark_warning).toString().replace(
                        "%s", getBookmarkTitle(deletePos)))
                .setPositiveButton(R.string.save, 
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                deleteBookmark(deletePos);
                            }
                        })
                .setNegativeButton(R.string.do_not_save, null)
                .show();
    }

    /**
     *  Refresh the shown list after the database has changed.
     */
    private void refreshList() {
        mBookmarksAdapter.refreshList();
    }
    
    /**
     *  Return a hashmap representing the currently highlighted row.
     */
    public Bundle getRow(int position) {
        return mBookmarksAdapter.getRow(position);
    }

    /**
     *  Return the url of the currently highlighted row.
     */
    public String getUrl(int position) {
        return mBookmarksAdapter.getUrl(position);
    }

    /**
     * Return the favicon of the currently highlighted row.
     */
    public Bitmap getFavicon(int position) {
        return mBookmarksAdapter.getFavicon(position);
    }

    private Bitmap getTouchIcon(int position) {
        return mBookmarksAdapter.getTouchIcon(position);
    }

    public String getBookmarkTitle(int position) {
        return mBookmarksAdapter.getTitle(position);
    }

    /**
     *  Delete the currently highlighted row.
     */
    public void deleteBookmark(int position) {
        mBookmarksAdapter.deleteRow(position);
    }
    public boolean dispatchKeyEvent(KeyEvent event) {    
        if (event.getKeyCode() ==  KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            setResultToParent(RESULT_CANCELED, null);
            mCanceled = true;
        }
        return super.dispatchKeyEvent(event);
    }

    // This Activity is generally a sub-Activity of CombinedHistoryActivity. In
    // that situation, we need to pass our result code up to our parent.
    // However, if someone calls this Activity directly, then this has no
    // parent, and it needs to set it on itself.
    private void setResultToParent(int resultCode, Intent data) {
        Activity a = getParent() == null ? this : getParent();
        a.setResult(resultCode, data);
    }
}
