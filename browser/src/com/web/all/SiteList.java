package com.web.all;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.provider.Browser;
import android.provider.Browser.BookmarkColumns;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

public class SiteList extends ListActivity {
    private ArrayList<Site> siteList;
    private ContentResolver mResolver;
    private static final String LOG_TAG = "SITELIST";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_list);

        siteList = this.getListOfSite();
        mResolver = this.getContentResolver();

        ArrayList<HashMap<String, String>> mylist = new ArrayList<HashMap<String, String>>();
        for (int i = 0; i < siteList.size(); i++) {
            Log.i(LOG_TAG, "into sitelist");
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("ItemTitle", siteList.get(i).name);
            mylist.add(map);
        }

        MyListAdapter mAdapter = new MyListAdapter();
        setListAdapter(mAdapter);

        /**
         *register context listener
         */
        registerForContextMenu(getListView());
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        AdapterView.AdapterContextMenuInfo menuInfo;

        menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        int index = menuInfo.position;

        int itemID = item.getItemId();
        Log.i(LOG_TAG, "" + index + " " + itemID);
        switch (itemID) {
        case 0:
            final Site s = (Site) (getListView().getItemAtPosition(index));
            s.visit_num = s.visit_num + 1;

            Constant.updateSiteInfo(s);
            loadUrl(s.m_url, false);
            break;
        case 1:
            this.showSiteInfo((Site) (getListView().getItemAtPosition(index)));
            break;
        }
        return super.onContextItemSelected(item);
    }

    private void showSiteInfo(Site site) {
        new AlertDialog.Builder(SiteList.this).setTitle(site.name).setMessage(
                "mobile url: " + site.m_url + "\n" + "normal url: "
                        + site.n_url + "\n" + "site rank: " + site.rank).show();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        // TODO Auto-generated method stub
        menu.setHeaderTitle("Operation");

        menu.add(0, 0, 0, "Visit");
        menu.add(0, 1, 0, "View Detail");

        super.onCreateContextMenu(menu, v, menuInfo);
    }

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

    private void setResultToParent(int resultCode, Intent data) {
        Activity a = getParent() == null ? this : getParent();
        a.setResult(resultCode, data);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        // TODO Auto-generated method stub
        super.onListItemClick(l, v, position, id);

        final Site s = (Site) (getListView().getItemAtPosition(position));

        s.visit_num = s.visit_num + 1;
        try {
        	Constant.updateSiteInfo(s);
		} catch (Exception e) {
		}
        loadUrl(s.m_url, false);

    }

    public ArrayList<Site> getListOfSite() {
        Bundle bundle = this.getIntent().getExtras();

        ArrayList<String> list = bundle.getStringArrayList("KEY_SITE");
        ArrayList<Site> sList = new ArrayList<Site>();
        for (int i = 0; i < list.size(); i++) {
            sList.add(parseStringToSite(list.get(i)));
        }
        return sList;
    }

    public Site parseStringToSite(String s) {
        String[] str = s.split("\t");
        Site site = new Site(Integer.parseInt(str[0]), str[1], Integer
                .parseInt(str[2]), str[3], str[4], Integer.parseInt(str[5]),
                Integer.parseInt(str[6]), Integer.parseInt(str[7]));
        return site;
    }

    private class MyListAdapter implements ListAdapter {

        public boolean areAllItemsEnabled() {
            // TODO Auto-generated method stub
            return true;
        }

        public boolean isEnabled(int arg0) {
            // TODO Auto-generated method stub
            return true;
        }

        public int getCount() {
            // TODO Auto-generated method stub
            return siteList.size();
        }

        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return siteList.get(position);
        }

        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        public int getItemViewType(int arg0) {
            // TODO Auto-generated method stub
            return 0;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            // TODO Auto-generated method stub
            HistoryItem item;
            if (null == convertView || !(parent instanceof HistoryItem)) {
                item = new HistoryItem(SiteList.this);
            } else {
                item = (HistoryItem) convertView;
            }
            Log.i(LOG_TAG, "len " + position);
            String url = siteList.get(position).m_url;
            item.setUrl(url);
            item.setName(siteList.get(position).name);
            item.setIsBookmark(isBookmark(url));
            return item;
        }

        private boolean isBookmark(String url) {
            StringBuilder sb = new StringBuilder(BookmarkColumns.URL + " = ");
            DatabaseUtils.appendEscapedSQLString(sb, url);
            sb.append(" and " + BookmarkColumns.BOOKMARK + " > 0");
            Cursor c = mResolver.query(Browser.BOOKMARKS_URI,
                    Browser.HISTORY_PROJECTION, sb.toString(), null, null);
            return c.moveToFirst();
        }

        public int getViewTypeCount() {
            // TODO Auto-generated method stub
            return 1;
        }

        public boolean hasStableIds() {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean isEmpty() {
            // TODO Auto-generated method stub
            return false;
        }

        public void registerDataSetObserver(DataSetObserver arg0) {
            // TODO Auto-generated method stub

        }

        public void unregisterDataSetObserver(DataSetObserver arg0) {
            // TODO Auto-generated method stub

        }

    }

}
