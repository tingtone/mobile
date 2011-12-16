package com.web.all;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.*;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {
    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;
    private static final String LOG_TAG = "database";
	private static String CREATE_FAVOURITE = "create table if not exists favourites(_id INTEGER PRIMARY KEY, title TEXT, url TEXT);";
	private static String DROP_FAVOURITE = "drop table if exists favourites";
    private static String DB_PATH = "/data/data/com.web.all/databases/";
    private static String ZIP_DB_NAME = "site.zip";
    private static String DB_NAME = "site.db";
    private static String DBPath = DB_PATH + DB_NAME;
    private static String TABLE_SITE = "site";
    private static String TABLE_CATE = "cate";
    private static String FAV_TABLE_NAME = "favourites";
    private SQLiteDatabase siteDataBase;
    private final Context context;
    private static int DB_VERSION = 3;
    
    class FAV_TABLE_COL {
    	public static final String ID_COL = "_id";
    	public static final String TITLE_COL = "title";
    	public static final String URL_COL = "url";
    }
    
    class FAV_TABLE_INDEX {
    	public static final int ID_INDEX = 0;
    	public static final int TITLE_INDEX = 1;
    	public static final int URL_IDNEX = 2;
    }
    
    public DBHelper(Context context) {
        super(context, DB_NAME, null, 1);
        this.context = context;
    }

    public void createDatabase() throws IOException {
        this.getReadableDatabase();
        Log.i(LOG_TAG, "gotReadableDB");
        copyDataBase();
        Log.i(LOG_TAG, "copiedDB");
        siteDataBase = SQLiteDatabase.openDatabase(DBPath, null,
                SQLiteDatabase.OPEN_READWRITE);
        initFavourite();
        Constant.setKey(Constant.DB_VERSION_KEY, DB_VERSION);
        fixSpellBBug();
    }

    public boolean checkDataBase() {

        String DBFileName = DB_PATH + DB_NAME;

        return new File(DBFileName).exists();

    }

    public void copyDataBase() throws IOException {

        // open local database file as the input stream
        InputStream assetInput = context.getAssets().open(ZIP_DB_NAME);
        Log.i(LOG_TAG, "opened zip_db");
        ZipInputStream zipIn = new ZipInputStream(assetInput);

        ZipEntry entry = zipIn.getNextEntry();

        entry.getName();

        // path to the just created empty database
        String DBFileName = DB_PATH + DB_NAME;

        OutputStream DBOutput = new FileOutputStream(DBFileName);

        byte[] buffer = new byte[1024];

        int length;

        while ((length = zipIn.read(buffer)) > 0) {
            DBOutput.write(buffer, 0, length);
            Log.i(LOG_TAG, "zipread" + length);
        }
        Log.i(LOG_TAG, "read end");
        // close the streams
        DBOutput.flush();
        DBOutput.close();

        zipIn.close();
        assetInput.close();
    }

    public void openDataBase() throws SQLiteException {
        // open the database
        siteDataBase = SQLiteDatabase.openDatabase(DBPath, null,
                SQLiteDatabase.OPEN_READWRITE);
        upgradeDB();
    }
    
    private void fixSpellBBug() {
    	ContentValues values = new ContentValues();
		values.put("p_name", "society");
		siteDataBase.update(TABLE_CATE, values, "p_name='soiety' and p_id=9", null);
		values.clear();
		values.put("ch_name", "society");
		siteDataBase.update(TABLE_CATE, values, "p_id=0 and ch_id=9 and ch_name='soiety'", null);
    }
    private void upgradeDB() {
    	int oldV = Constant.getKey(Constant.DB_VERSION_KEY, 1);
    	if (DB_VERSION > oldV) {
    		Constant.setKey(Constant.DB_VERSION_KEY, DB_VERSION);
    		if (oldV == 1) {
    			//fix the spell bug
    			siteDataBase.execSQL(DROP_FAVOURITE);
    			initFavourite();
    			fixSpellBBug();
    		}
    		if (DB_VERSION == 3) {
    			Constant.setKey(Constant.HOME_TITLE_KEY, Constant.HOME_TITLE);
        		Constant.setKey(Constant.HOME_URL_KEY, Constant.HOME_URL);
    		}
    	}
    } 
    private void initFavourite() {
    	siteDataBase.execSQL(CREATE_FAVOURITE);
    	for (int i = 0; i < Constant.defaultFavourite.length / 2; i++) {
    		String title = Constant.defaultFavourite[2 * i];
    		String url = Constant.defaultFavourite[2* i + 1];
    		ContentValues values = new ContentValues();
    		values.put(FAV_TABLE_COL.TITLE_COL, title);
    		values.put(FAV_TABLE_COL.URL_COL, url);
    		siteDataBase.insert(FAV_TABLE_NAME, null, values);
    	}
    }
    
    public void queryFav(ArrayList<Site> list) {
    	Cursor c;
		c = siteDataBase.query(FAV_TABLE_NAME, new String[] {
				FAV_TABLE_COL.ID_COL, FAV_TABLE_COL.TITLE_COL,
				FAV_TABLE_COL.URL_COL }, null, null, null, null, null);
    	list.clear();
    	if (c != null && c.getCount() != 0) {
    		c.moveToFirst();
    		for (int i = 0 ; i < c.getCount(); i++) {
    			c.moveToPosition(i);
				list.add(new Site(c.getInt(FAV_TABLE_INDEX.ID_INDEX), c
						.getString(FAV_TABLE_INDEX.TITLE_INDEX), 0, null, c
						.getString(FAV_TABLE_INDEX.URL_IDNEX), 0, 0, 0));
    		}
    	}
    	c.close();
    }
    
    public void insertFav(String title, String url) {
    	ContentValues values = new ContentValues();
    	values.put(FAV_TABLE_COL.TITLE_COL, title);
    	values.put(FAV_TABLE_COL.URL_COL, url);
    	siteDataBase.insert(FAV_TABLE_NAME, null, values);
    }
    
    public void deleteFav(int id) {
    	siteDataBase.delete(FAV_TABLE_NAME, FAV_TABLE_COL.ID_COL + "=" + id, null);
    }
    
    public void updateFav(int id, String title, String url) {
    	ContentValues values = new ContentValues();
    	values.put(FAV_TABLE_COL.TITLE_COL, title);
    	values.put(FAV_TABLE_COL.URL_COL, url);
    	siteDataBase.update(FAV_TABLE_NAME, values, FAV_TABLE_COL.ID_COL + "=" + id, null);
    }

    public Cursor queryFav() {
    	return siteDataBase.query(FAV_TABLE_NAME, new String[] {
				FAV_TABLE_COL.ID_COL, FAV_TABLE_COL.TITLE_COL,
				FAV_TABLE_COL.URL_COL }, null, null, null, null, null);
    }
    
    public void getSitesByCate(String p_cate, String ch_cate,
            List<Site> siteList, int select) {

        Cursor c;
        int p_id, ch_id;
        siteList.clear();
        if (null != ch_cate) {
            c = siteDataBase.query(TABLE_CATE, new String[] { "p_id", "p_name",
                    "ch_id", "ch_name" }, "p_name=\"" + p_cate
                    + "\" and ch_name=\"" + ch_cate + "\"", null, null, null,
                    null);

            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                p_id = c.getInt(0);
                ch_id = c.getInt(2);
                this.getSitesByCateId(p_id, ch_id, siteList, select);
                c.close();
            }
        } else {
            c = siteDataBase.query(TABLE_CATE, new String[] { "p_id", "p_name",
                    "ch_id", "ch_name" }, "p_name=\"" + p_cate + "\"", null,
                    null, null, null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                p_id = c.getInt(0);
                ch_id = -1;
                this.getSitesByCateId(p_id, ch_id, siteList, select);
                c.close();
            }
        }
        
    }

    private void getSitesByCateId(int p_id, int ch_id, List<Site> siteList,
            int select) {
        if (ch_id >= 0) {
            Cursor c = siteDataBase.query(TABLE_SITE, new String[] { "id",
                    "name", "rank", "norm_url", "mob_url", "visit_num",
                    "p_cateid", "ch_cateid" }, "p_cateid=" + p_id
                    + " and ch_cateid=" + ch_id, null, null, null,
                    "visit_num desc,rank");

            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                int count = 0;
                do {
                    Site s = new Site(c.getInt(0), c.getString(1), c.getInt(2),
                            c.getString(3), c.getString(4), c.getInt(5), c
                                    .getInt(6), c.getInt(7));

                    siteList.add(s);
                    count++;
                    if (select > 0 && count >= select) {
                        break;
                    }
                } while (c.moveToNext());
                c.close();
            }
            
        } else {
            Cursor c = siteDataBase.query(TABLE_SITE, new String[] { "id",
                    "name", "rank", "norm_url", "mob_url", "visit_num",
                    "p_cateid", "ch_cateid" }, "p_cateid=" + p_id, null, null,
                    null, "visit_num desc,rank");

            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                int count = 0;
                do {
                    Site s = new Site(c.getInt(0), c.getString(1), c.getInt(2),
                            c.getString(3), c.getString(4), c.getInt(5), c
                                    .getInt(6), c.getInt(7));

                    siteList.add(s);
                    count++;
                    if (select > 0 && count >= select) {
                        break;
                    }
                } while (c.moveToNext());
                c.close();
            }
            
        }
        
    }

    public void getAllCateName(List<String> list) {
        Cursor c = siteDataBase.query(TABLE_CATE, new String[] { "p_id",
                "ch_name" }, "p_id=0", null, null, null, null);
        if (c != null && c.getCount() > 0) {
            c.moveToFirst();
            do {
                list.add(c.getString(1));

            } while (c.moveToNext());
            c.close();
        }
        
    }

    public void getAllChildCateByName(String p_name, List<String> list) {
        Cursor c = siteDataBase.query(TABLE_CATE, new String[] { "p_name",
                "ch_name" }, "p_name=\"" + p_name + "\"", null, null, null,
                null);
        if (c != null && c.getCount() > 0) {
            c.moveToFirst();
            do {
                list.add(c.getString(1));
            } while (c.moveToNext());
            c.close();
        }
        
    }

    public Site getSiteById(int id) {
        Cursor c = siteDataBase.query(TABLE_SITE, new String[] { "id", "name",
                "rank", "norm_url", "mob_url", "visit_num", "p_cateid",
                "ch_cateid" }, "id=" + id, null, null, null, null);
        if (c != null && c.getCount() > 0) {
            c.moveToFirst();
            Site s = new Site(c.getInt(0), c.getString(1), c.getInt(2), c
                .getString(3), c.getString(4), c.getInt(5), c.getInt(6), c
                .getInt(7));
            c.close();
            return s;
        }
        
        return null;
    }

    public void searchBySiteName(String name, List<Site> list) {
        Cursor c = siteDataBase.query(TABLE_SITE, new String[] { "id", "name",
                "rank", "norm_url", "mob_url", "visit_num", "p_cateid",
                "ch_cateid" }, null, null, null, null, null);
        String str = "";
        if (c != null && c.getCount() > 0) {
            c.moveToFirst();
            do {
                str = c.getString(1);
                if (str.contains(name)) {
                    list.add(new Site(c.getInt(0), c.getString(1), c.getInt(2),
                            c.getString(3), c.getString(4), c.getInt(5), c
                                    .getInt(6), c.getInt(7)));
                }
            } while (c.moveToNext());
            c.close();
        }
        
    }

    public Site searchBySiteUrl(String url) {
        Cursor c = siteDataBase.query(TABLE_SITE, new String[] { "id", "name",
                "rank", "norm_url", "mob_url", "visit_num", "p_cateid",
                "ch_cateid" }, "norm_url=\"" + url + "\"", null, null, null,
                null);

        if (c != null && c.getCount() > 0) {
            c.moveToFirst();
            Site s = new Site(c.getInt(0), c.getString(1), c.getInt(2), c
                .getString(3), c.getString(4), c.getInt(5), c.getInt(6), c
                .getInt(7));
            c.close();
            return s;
        }
        return null;
    }

    public void updateSiteInfo(Site info) {
            siteDataBase.execSQL("update site set visit_num=" + info.visit_num
                    + " where id=" + info.id);
    }

    public synchronized void close() {
        if (this.siteDataBase.isOpen()) {
            siteDataBase.close();
        }
        super.close();
    }

    @Override
    public void onCreate(SQLiteDatabase arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub

    }

}

class Site implements Serializable {
    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;
    int id;
    int rank;
    String name;
    String n_url;
    String m_url;
    int p_cateid;
    int ch_cateid;
    int visit_num;

    public Site(int id, String name, int rank, String n_url, String m_url,
            int visit_num, int p_cateid, int ch_cateid) {
        this.id = id;
        this.rank = rank;
        this.name = name;
        this.n_url = n_url;
        this.m_url = m_url;
        this.visit_num = visit_num;
        this.p_cateid = p_cateid;
        this.ch_cateid = ch_cateid;
    }

    public String toString() {
        return id + "\t" + name + "\t" + rank + "\t" + n_url + "\t" + m_url
                + "\t" + visit_num + "\t" + p_cateid + "\t" + ch_cateid;
    }
}
