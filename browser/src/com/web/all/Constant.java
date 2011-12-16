package com.web.all;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Build;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

interface CallBack {
	void onDataInitFinish();
}

public class Constant {
	public static final HashMap<String, String[]> SPECILAL_SITES = new HashMap<String, String[]>();
	// public static int CUR_APP_IDX ;
	public static final String TITLE = "title";
	public static final String URL = "url";
	public static final String PATTERN = "pattern";
	public static final String NEW_TAB = "new_window";
	public static ProgressDialog dataInitDialog;
	public static final int sdk = Integer.valueOf(Build.VERSION.SDK);
	public static final String MOBILE_BROWSER_PREF = "mobile_browser_preference";
	public static final String DB_VERSION_KEY = "db_version";
	public static final String HOME_TITLE_KEY = "home_title";
	public static final String HOME_URL_KEY = "home_url";
	public static final String CATE_KEY = "cate";
	public static final String FIRST_START_KEY = "first";
	//setting for browser
	public static SharedPreferences mSetting;

	public static String homeUrl() {
		return "http://" + ".com";
	}

	public static WebBrowse wb;
	public static DBHelper dbHelper;
	public static String category = "@cate@";
	
	public static void setCate(String cate) {
		Constant.category = cate;
		wb.saveDefaultCate();
	}

	public static int getKey(String key, int defValue) {
		return Constant.mSetting.getInt(key, defValue);
	}

	public static void setKey(String key, int value) {
		Editor editor = Constant.mSetting.edit();
		editor.putInt(key, value);
		editor.commit();
	}
	
	public static void setKey(String key, String value) {
		Editor editor = Constant.mSetting.edit();
		editor.putString(key, value);
		editor.commit();
	}
	
    public static boolean hasKey(String key) {
        return mSetting.getBoolean(key, false);
    }
  
    public static void setBoolKey(String key) {
        Editor e = mSetting.edit();
        e.putBoolean(key, true);
        e.commit();
    }
	
	public static void initDB(Context c) {
		dbHelper = new DBHelper(c);

		boolean dbExist = dbHelper.checkDataBase();
		if (!dbExist) {
			dataInitDialog = new ProgressDialog(wb);
			dataInitDialog.setCancelable(false);
			dataInitDialog.setMessage(wb.getResources().getString(
					R.string.database_init_msg));
			dataInitDialog.show();
			new Thread(new Runnable() {

				public void run() {
					try {
						dbHelper.createDatabase();
						wb.onDataInitFinish();
					} catch (IOException e) {
						e.printStackTrace();
						wb.onDataInitFinish();
					}
				}

			}).start();
		} else {
			dbHelper.openDataBase();
			wb.onDataInitFinish();
		}
		if (!USE_DATABASE) {
			DEFAULT_SITES_TITLES = getSitesTitle();
		}
	}

	public static String getTitleFromUrl(String url) {
		try {
			URL u = new URL(url);
			String longhost = u.getHost();
			int firstDot = longhost.indexOf('.');
			if (firstDot != -1) {
				int secondDot = longhost.indexOf('.', firstDot + 1);
				if (secondDot != -1) {
					return longhost.substring(firstDot + 1, secondDot);
				}
			}
			return longhost;
		} catch (MalformedURLException e) {
			return null;
		}

	}

	public static int getTabUserAgent(String url) {
		return url.indexOf(' ') != -1 ? 2 : 1;
	}
	
	public static final boolean USE_DATABASE = true;
	 
	public static String[] EMPTY_TABLE = {};

	public static String DEFAULT_TABLE[] = EMPTY_TABLE;
	public static String[] DEFAULT_SITES_TITLES;
	
	public static String getDefaultHomeUrl() {
		return DEFAULT_TABLE[1];
	}
	
	public static String getDefaultHomeTitle() {
		return DEFAULT_TABLE[0];
	}
	
	public static String[] getSitesTitle() {
		int length = DEFAULT_TABLE.length / 2 + 2;
		String[] temp = new String[length];
		for (int i = 0 ;i < length - 2; i++) {
			temp[i] = DEFAULT_TABLE[i * 2];
		}
		temp[length - 2] = "Bookmark";
		temp[length - 1] = "Search";
		return temp;
	}
	
	public static int getSitesLen() {
		return DEFAULT_SITES_TITLES.length - 2;
	}
	
	public static String getUrlByIndex(int index) {
		return DEFAULT_TABLE[2 * index + 1];
	}
	
	public static String[] defaultFavourite =
	{
		"MSN", "http://extreme.mobile.msn.com",
		"Twitter", "http://mobile.twitter.com",
		"Yahoo", "http://m.yahoo.com",
		"Craigslist", "http://craigslist.org.moovweb.com",
		"Best Buy", "http://m.bestbuy.com/m/b/",
		"NBA", "http://m.nba.com"
	};
	
	//public static String HOME_URL = "http://google.com/m";
	public static String HOME_URL = "http://mobilebrowser2.appspot.com";
	public static String CHANGE_NEWS = "http://mobilebrowser2.appspot.com/choosetype.jsp";
	//public static String HOME_TITLE = "Google";
	public static String HOME_TITLE = "News";
	public static String PREFIX = "http://";
	
	public static void queryFav(ArrayList<Site> list) {
		reOpenDatabase();
		dbHelper.queryFav(list);
	}
	
	public static Cursor queryFav() {
		reOpenDatabase();
		return dbHelper.queryFav();
	}
	
	private static void reOpenDatabase() {
		if (dbHelper == null) {
			dbHelper = new DBHelper(wb);
			dbHelper.openDataBase();
			mSetting = wb.getSharedPreferences(Constant.MOBILE_BROWSER_PREF, 0);
		}
	}
	
	public static void updateSiteInfo(Site info) {
		reOpenDatabase();
		dbHelper.updateSiteInfo(info);
	}
	
	public static void getAllCateName(List<String> pcList) {
		reOpenDatabase();
		dbHelper.getAllCateName(pcList);
	}
	
	public static void getAllChildCateByName(String name, List<String> chList) {
		reOpenDatabase();
		dbHelper.getAllChildCateByName(name, chList);
	}
	
	public static void getSitesByCate(String p_cate, String ch_cate,
            List<Site> siteList, int select) {
		reOpenDatabase();
		dbHelper.getSitesByCate(p_cate, ch_cate, siteList, select);
	}
	
	public static void insertFav(String title, String url) {
		reOpenDatabase();
		dbHelper.insertFav(title, url);
	}
	
	public static void deleteFav(int id) {
		dbHelper.deleteFav(id);
	}
	
	public static void updateFav(int id, String title, String url) {
		dbHelper.updateFav(id, title, url);
	}
}
