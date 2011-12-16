package com.web.all;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ExpandableListActivity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Browser;
import android.provider.Browser.BookmarkColumns;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebIconDatabase.IconListener;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Vector;


/**
 * Activity for displaying the browser's history, divided into
 * days of viewing.
 */
public class BrowserHistoryPage extends ExpandableListActivity {
    private MyExpandableListAdapter          mAdapter;
    private boolean                 mMaxTabsOpen;
    private HistoryItem             mContextHeader;
    private ContentResolver mResolver;
    // Implementation of WebIconDatabase.IconListener
    private class IconReceiver implements IconListener {
        public void onReceivedIcon(String url, Bitmap icon) {
            setListAdapter(mAdapter);
        }
    }
    // Instance of IconReceiver
    private final IconReceiver mIconReceiver = new IconReceiver();

    /**
     * Report back to the calling activity to load a site.
     * @param url   Site to load.
     * @param newWindow True if the URL should be loaded in a new window
     */
    private void loadUrl(String url, boolean newWindow) {
        Intent intent = new Intent().setAction(url);
        if (newWindow) {
            Bundle b = new Bundle();
            b.putBoolean(Constant.NEW_TAB, true);
            intent.putExtras(b);
        }
        setResultToParent(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setTitle(R.string.browser_history);
        this.setGroup();
        /********************************/
        
        mResolver = this.getContentResolver();
        mAdapter = new MyExpandableListAdapter(this.CATE_NAME,this.child);
        setListAdapter(mAdapter);
        final ExpandableListView list = getExpandableListView();
        /*
        View v = new ViewStub(this, R.layout.empty_history);
        addContentView(v, new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT));
        list.setEmptyView(v);
        */
        // Do not post the runnable if there is nothing in the list.
        /*
        if (list.getExpandableListAdapter().getGroupCount() > 0) {
            list.post(new Runnable() {
                public void run() {
                    // In case the history gets cleared before this event
                    // happens.
                  /*
                  try {
                    if (list.getExpandableListAdapter().getGroupCount() > 0) {
                        list.expandGroup(0);
                    }
                  } catch (Exception e) {
                    
                  }
                  
                }
            });
        }*/
        mMaxTabsOpen = getIntent().getBooleanExtra("maxTabsOpen", false);
        CombinedBookmarkHistoryActivity.getIconListenerSet(getContentResolver())
                .addListener(mIconReceiver);
        
        // initialize the result to canceled, so that if the user just presses
        // back then it will have the correct result
//        setResultToParent(RESULT_CANCELED, null);
    }

/*    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.history, menu);
        return true;
    }
*/
/*    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.clear_history_menu_id).setVisible(Browser.canClearHistory(this.getContentResolver()));
        return true;
    }
*/    
/*    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_history_menu_id:
                // FIXME: Need to clear the tab control in browserActivity 
                // as well
                Browser.clearHistory(getContentResolver());
                mAdapter.refreshData();
                return true;
                
            default:
                break;
        }  
        return super.onOptionsItemSelected(item);
    }
*/
    private static final int SET_DEFAULT = Menu.FIRST;
    
    
@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getItemId() == SET_DEFAULT) {
			AlertDialog mQuitDialog;
			final ExpandableListContextMenuInfo i = 
	            (ExpandableListContextMenuInfo) item.getMenuInfo();
			mQuitDialog = new AlertDialog.Builder(this).setMessage(
					R.string.change_default_message).setPositiveButton("Yes",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							Constant.setCate(CATE_NAME[(int) i.id]);
						}
					}).setNegativeButton("No",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {

						}
					}).setTitle(CATE_NAME[(int) i.id]).create();
			mQuitDialog.show();

		}
		return super.onContextItemSelected(item);
	}

	/*    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ExpandableListContextMenuInfo i = 
            (ExpandableListContextMenuInfo) item.getMenuInfo();
        HistoryItem historyItem = (HistoryItem) i.targetView;
        String url = historyItem.getUrl();
        String title = historyItem.getName();
        switch (item.getItemId()) {
          case R.id.default_tab:
            if (item.isChecked()) {
              Constant.wb.removeDefaultTab(title);
            } else {
              Constant.wb.saveDefaultTab(title,url);
            }
            return true;
            case R.id.open_context_menu_id:
                loadUrl(url, false);
                return true;
            case R.id.new_window_context_menu_id:
                loadUrl(url, true);
                return true;
            case R.id.save_to_bookmarks_menu_id:
                if (historyItem.isBookmark()) {
                    Bookmarks.removeFromBookmarks(this, getContentResolver(),
                            url);
                } else {
                    Browser.saveBookmark(this, title, url);
                }
                return true;
            case R.id.share_link_context_menu_id:
                Browser.sendString(this, url);
                return true;
            case R.id.delete_context_menu_id:
                Browser.deleteFromHistory(getContentResolver(), url);
                mAdapter.refreshData();
                return true;
            default:
                break;
        }
        return super.onContextItemSelected(item);
    }
*/    
    @Override
    public boolean onChildClick(ExpandableListView parent, View v,
            int groupPosition, int childPosition, long id) {
        /*if (v instanceof HistoryItem) {
            loadUrl(((HistoryItem) v).getUrl(), false);
            return true;
        }
        return false;*/
    	List<Site> list=new ArrayList<Site>();
		Constant.getSitesByCate(CATE_NAME[groupPosition],
				child[groupPosition][childPosition], list, 0);
		
		Intent intent=new Intent();
		intent.setClass(BrowserHistoryPage.this, SiteList.class);
		List<String> sl=new ArrayList<String>();
		for(int i=0;i<list.size();i++){
			sl.add(list.get(i).toString());
		}
		
		Bundle bundle = new Bundle();
		bundle.putStringArrayList("KEY_SITE", (ArrayList<String>) sl);
		
		intent.putExtras(bundle);
		
		//startActivity(intent);
		startActivityForResult(intent, 1);
		
		return true;
    }

    
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		//super.onActivityResult(requestCode, resultCode, data);
		if(resultCode==RESULT_OK){
			loadUrl(data.getAction(), false);
		}
		
	}

	// This Activity is generally a sub-Activity of CombinedHistoryActivity. In
    // that situation, we need to pass our result code up to our parent.
    // However, if someone calls this Activity directly, then this has no
    // parent, and it needs to set it on itself.
    private void setResultToParent(int resultCode, Intent data) {
        Activity a = getParent() == null ? this : getParent();
        a.setResult(resultCode, data);
    }

    private class ChangeObserver extends ContentObserver {
        public ChangeObserver() {
            super(new Handler());
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

/*        @Override
        public void onChange(boolean selfChange) {
            mAdapter.refreshData();
        }
*/    }
    
    private static Calendar beginningOfDay(Calendar c) {
      c.set(Calendar.HOUR_OF_DAY, 0);
      c.set(Calendar.MINUTE, 0);
      c.set(Calendar.SECOND, 0);
      c.set(Calendar.MILLISECOND, 0);
      return c;
    }
    private static long getDateByMill(int day) {
      Calendar c = Calendar.getInstance();
      beginningOfDay(c);
      
      // Create the bins
      c.roll(Calendar.DAY_OF_YEAR, -day);
      return c.getTimeInMillis(); 
    }
   
/*     final static String[] social= {"http://touch.facebook.com/","fast fbook",
      "http://m.facebook.com/","comments fbook","http://lite.facebook.com/","lite fbook","http://www.facebook.com/login.php?m2w","facebook",
      "http://m.twitter.com/", "twitter","http://m.myspace.com/","myspace"};
     
     final static String[] social1 ={ "http://m.renren.com/","renren","http://m.linkedin.com/","linkedin",
         "http://m.meetup.com/","meetup","http://m.match.com","match"};
    
     final static String[] travel = {"http://m.southwest.com","southwest","http://m.expedia.com","expedia",
      "http://mobile.travelocity.com","travelocity"
    };
    
     final static String[] other = {
    "http://m.ups.com","ups","http://m.usps.com","usps","http://fedex.com/mobi","fedex","http://www.mobile.ca.gov","ca government",
    "http://m.ask.com","ask","http://m.answers.com","answers","http://m.wikipedia.com","wikipedia","http://m.weather.com","weather",
    "http://mobile.wunderground.com","wunderground","http://m.biblegateway.com","biblegateway","http://m.mapquest.com","mapquest"
    };
     final static String[] entertainment = {
        "http://m.comcast.com","comcast","http://m.directv.com","directv","http://m.tvguide.com","tvguide","http://m.moviefone.com","moviefone",
        "htp://m.imdb.com","imdb","http://mobile.fandango.com","fandango","http://m.netflix.com","netflix","http://m.ticketmaster.com","ticketmaster",
        "http://m.ebaumsworld.com","ebaumsworld","http://m.people.com","people","http://m.perezhilton.com","perezhilton","http://m.flickr.com","flickr"
    };
     final static String[] lifestyle = {
        "http://m.foodnetwork.com","foodnetwork","http://m.yelp.com","yelp","http://m.local.com","local","http://m.citysearch.com","citysearch",
        "http://m.superpages.com","superpages","http://m.cdc.com","cdc","http://m.ivillage.com","ivillage","http://m.ivillage.com","ivillage",
        "http://m.astrology.com","astrology","http://m.careerbuilder.com","careerbuilder","http://mobile.monster.com","monster"
    };
     final static String[] newmedia = {
        "http://m.wordpress.com","wordpress","http://m.livejournal.com","livejournal","http://m.typepad.com","typepad","http://m.geocities.com","geocities",
        "http://m.digg.com","digg","http://m.cnet.com","cnet","http://m.tmz.com","tmz","http://m.break.com","break","http://m.dailymotion.com","dailymotion",
        "http://m.youtube.com","youtube","http://m.metacafe.com","metacafe","http://m.photobucket.com","photobucket","http://m.kodakgallery.com","kodakgallery"
    };
    
     final static String[] portal = {
    	 "http://m.yahoo.com","yahoo","http://m.live.com","live",
         "http://m.msn.com","msn","http://m.aol.com","aol","http://m.microsoft.com","microsoft","http://m.verizon.com","verizon"
    };
     final static String[] news = {
     "htp://m.cnn.com","cnn","http://m.bbc.com","bbc","http://m.foxnews.com","foxnews","http://m.cbsnews.com","cbsnews",
        "http://m.bloomberg.com","bloomberg","http://m.usatoday.com","usatoday","http://m.newsweek.com","newsweek","http://m.washingtonpost.com","washingtonpost",
        "http://m.reuters.com","reuters"
    };
    
     final static String[] shopping ={
        "http://m.walmart.com","walmart","http://m.target.com","target","http://m.kmart.com","kmart","http://m.bestbuy.com","bestbuy","http://m.gap.com","gap",
        "http://m.sears.com","sears","http://m.jcpenney.com","jcpenney","http://m.amazon.com","amazon","http://m.ebay.com","ebay",
        "http://www.mobile.overstock.com","overstock","http://m.lowes.com","lowes","http://www.mobilehomedepotmi.com","homedepot",
        "http://m.pronto.com","pronto","http://m.nextag.com","nextag","http://m.qvc.com","qvc","http://m.bathandbodyworks.com","bathandbodyworks",
        "http://m.shopzilla.com","shopzilla"
    };
    
     final static String[] car = {
      "http://m.cars.com","cars",
      "http://m.kbb.com","kbb","http://www.autotrader.mobi","autotrader"
    };
    
     final static String[] banking ={
    "http://m.paypal.com","paypal",
    "http://www.wf.com","wellsfargo","http://m.bankofamerica.com","bankofamerica","https://mobilebanking.capitalone.com","capitalone",
    "http://www.chase.mobi","chase"};
    
     final static String[] sports ={
        "http://m.nhl.com","nhl","http://m.nba.com","nba","http://m.nascar.com","nascar","http://wap.wwe.com","wwe",
        "http://m.mlb.com","mlb","http://m.espn.com","espn","http://sports.mobile.msn.com","foxsports","http://m.gamespot.com","gamespot",
        "http://www.ign.mobi","ign"
    };*/

    //private final static int[] CATE_NAME = {@ads_url@};
    //private final static String[][] child = {@full_home_url@};
     /*************add by zychen**********************/
     private String[]CATE_NAME;
     private String[][] child;
     
     private void setGroup(){
		/*if (Constant.USE_DATABASE) {
			Toast.makeText(this, R.string.set_default_tab, Toast.LENGTH_LONG)
					.show();
		}*/
    	 List<String> pcList=new ArrayList<String>();
         Constant.getAllCateName(pcList);
         
         CATE_NAME=new String[pcList.size()];
         child=new String[pcList.size()][];
         for(int i=0;i<pcList.size();i++){
        	 CATE_NAME[i] = pcList.get(i);
         }
         List<String> chList=new ArrayList<String>();
         for(int i=0;i<pcList.size();i++){
         	Constant.getAllChildCateByName(CATE_NAME[i], chList);
         	child[i]=new String[chList.size()];
         	for(int j=0;j<chList.size();j++){
         		child[i][j]=chList.get(j);
         	}
         	chList=new ArrayList<String>();
         }
     }
     
     
     private class MyExpandableListAdapter extends BaseExpandableListAdapter {

 		private String[] groups;
 		private String[][] children;
 		public MyExpandableListAdapter(String[] group,String[][] children){
 			this.groups=group;
 			this.children=children;
 		}
         
         public Object getChild(int groupPosition, int childPosition) {
             return children[groupPosition][childPosition];
         }

         public long getChildId(int groupPosition, int childPosition) {
             return childPosition;
         }

         public int getChildrenCount(int groupPosition) {
             return children[groupPosition].length;
         }

         // ȡ���б��е�ĳһ��� View
         public View getChildView(int groupPosition, int childPosition,
                 boolean isLastChild, View convertView, ViewGroup parent) {
             TextView textView = getGenericView(false);
             textView.setText(getChild(groupPosition, childPosition).toString());
             return textView;
         }

         public Object getGroup(int groupPosition) {
             return groups[groupPosition];
         }

         public int getGroupCount() {
             return groups.length;
         }

         public long getGroupId(int groupPosition) {
             return groupPosition;
         }

         // ȡ���б��е�ĳһ��� View
         public View getGroupView(int groupPosition, boolean isExpanded,
                 View convertView, ViewGroup parent) {
             TextView textView = getGenericView(true);
             textView.setText(getGroup(groupPosition).toString());
             return textView;
         }

         public boolean hasStableIds() {
             return true;
         }

         
         public boolean isChildSelectable(int groupPosition, int childPosition) {
             return true;
         }
                 
         // ��ȡĳһ��� View ���߼�
         private TextView getGenericView(boolean isGroupItem) {
             AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
                     ViewGroup.LayoutParams.FILL_PARENT, 50);
             TextView textView;
             if (!isGroupItem) {
            	 textView = new TextView(BrowserHistoryPage.this);
             } else {
            	 textView = new GroupView(BrowserHistoryPage.this);
             }
             textView.setLayoutParams(lp);
             textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
             textView.setPadding(32, 0, 0, 0);
             return textView;
         }
     }

     private class GroupView extends TextView {

		public GroupView(Context context) {
			super(context);
		}
    	 
     }
     /***********************************************/
    private class HistoryAdapter implements ExpandableListAdapter {
        
        // Array for each of our bins.  Each entry represents how many items are
        // in that bin.
        private int mItemMap[];
        private Vector<DataSetObserver> mObservers;
        private Cursor mCursor;
        
        HistoryAdapter() {
            mObservers = new Vector<DataSetObserver>();
            
            final String whereClause = Browser.BookmarkColumns.VISITS + " > 0"
                     + " AND " + Browser.BookmarkColumns.DATE + " > " + getDateByMill(7);
            final String orderBy = Browser.BookmarkColumns.DATE + " DESC";
           
            mCursor = managedQuery(
                    Browser.BOOKMARKS_URI,
                    Browser.HISTORY_PROJECTION,
                    whereClause, null, orderBy);
            
            buildMap();
            mCursor.registerContentObserver(new ChangeObserver());
        }
        
        void refreshData() {
            if (mCursor.isClosed()) {
                return;
            }
            mCursor.requery();
            buildMap();
            for (DataSetObserver o : mObservers) {
                o.onChanged();
            }
        }
        
        private void buildMap() {
            // The cursor is sorted by date
            // The ItemMap will store the number of items in each bin.
            int array[] = new int[CATE_NAME.length];
            //array[CATE_NAME.length-1] = mCursor.getCount();
            
            for (int i = 0; i < CATE_NAME.length; ++i) {
              Log.e("build ", "len " + child[i].length);
              array[i] = child[i].length;
            }
            mItemMap = array;
        }

        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                View convertView, ViewGroup parent) {
            HistoryItem item;
            if (null == convertView || !(convertView instanceof HistoryItem)) {
                item = new HistoryItem(BrowserHistoryPage.this);
                // Add padding on the left so it will be indented from the
                // arrows on the group views.
                item.setPadding(item.getPaddingLeft() + 10,
                        item.getPaddingTop(),
                        item.getPaddingRight(),
                        item.getPaddingBottom());
            } else {
                item = (HistoryItem) convertView;
            }
            /*if (groupPosition == CATE_NAME.length-1) {
              mCursor.moveToPosition(childPosition);
              item.setName(mCursor.getString(Browser.HISTORY_PROJECTION_TITLE_INDEX));
              String url = mCursor.getString(Browser.HISTORY_PROJECTION_URL_INDEX);
              item.setUrl(url);
              //item.setFavicon(CombinedBookmarkHistoryActivity.getIconListenerSet(
              //        getContentResolver()).getFavicon(url));
              item.setIsBookmark(1 ==
                      mCursor.getInt(Browser.HISTORY_PROJECTION_BOOKMARK_INDEX));
            } else {
              Log.e("view ", "len " + groupPosition + " " + childPosition);

              String url = child[groupPosition][childPosition*2]; 
              item.setUrl(url);
              item.setName(child[groupPosition][childPosition]);
              item.setIsBookmark(isBookmark(url));
            }
            
*/          Log.e("view ", "len " + groupPosition + " " + childPosition);

			String url = child[groupPosition][childPosition]; 
			item.setUrl(url);
			item.setName(child[groupPosition][childPosition]);
			item.setIsBookmark(isBookmark(url));
  
            return item;
        }
        private boolean isBookmark(String url) {
          StringBuilder sb = new StringBuilder(BookmarkColumns.URL + " = ");
          DatabaseUtils.appendEscapedSQLString(sb, url);
          sb.append(" and " + BookmarkColumns.BOOKMARK + " > 0");
          Cursor c = mResolver.query(
                  Browser.BOOKMARKS_URI,
                  Browser.HISTORY_PROJECTION,
                  sb.toString(),
                  null,
                  null);
          return c.moveToFirst();
        }
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            TextView item;
            if (null == convertView || !(convertView instanceof TextView)) {
                LayoutInflater factory = 
                        LayoutInflater.from(BrowserHistoryPage.this);
                item = (TextView) 
                        factory.inflate(R.layout.history_header, null);
            } else {
                item = (TextView) convertView;
            }
            item.setText(CATE_NAME[groupPosition]);
            return item;
        }

        public boolean areAllItemsEnabled() {
            return true;
        }

        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        public int getGroupCount() {
            return CATE_NAME.length;
        }

        public int getChildrenCount(int groupPosition) {
            return mItemMap[groupPosition];
        }

        public Object getGroup(int groupPosition) {
            return null;
        }

        public Object getChild(int groupPosition, int childPosition) {
            return null;
        }

        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        public long getChildId(int groupPosition, int childPosition) {
            return (childPosition << 3) + groupPosition;
        }

        public boolean hasStableIds() {
            return true;
        }

        public void registerDataSetObserver(DataSetObserver observer) {
            mObservers.add(observer);
        }

        public void unregisterDataSetObserver(DataSetObserver observer) {
            mObservers.remove(observer);
        }

        public void onGroupExpanded(int groupPosition) {
        
        }

        public void onGroupCollapsed(int groupPosition) {
        
        }

        public long getCombinedChildId(long groupId, long childId) {
            return childId;
        }

        public long getCombinedGroupId(long groupId) {
            return groupId;
        }

        public boolean isEmpty() {
            return mCursor.getCount() == 0;
        }
    }
}
