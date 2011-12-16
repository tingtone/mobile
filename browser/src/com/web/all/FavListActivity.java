package com.web.all;


import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FavListActivity extends ListActivity {

	public static final String LOG_TAG = "Fav Debug";
	public static final boolean LOG_ENABLED = true;
	private Cursor mCursor;
	public static final String TYPE_EXTRA = "type";
	public static final String TYPE_REMOVE = "remove";
	public static final int ACTION_ADD = 1;
	public static final int ACTION_EDIT = 2;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fav_list);
		try {
			mCursor = Constant.queryFav();
			mCursor.moveToFirst();
		} catch (Exception e) {
			mCursor = null;
		}
		if (mCursor != null) {
			setListAdapter(new Adapter());
		}
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent intent = new Intent(this, FavSetActivity.class);
		if (position < mCursor.getCount()) {
			mCursor.moveToPosition(position);
			//type: 1->edit, 2-> add
			intent.putExtra(TYPE_EXTRA, ACTION_EDIT);
			intent.putExtra(DBHelper.FAV_TABLE_COL.ID_COL, mCursor
					.getInt(DBHelper.FAV_TABLE_INDEX.ID_INDEX));
			intent.putExtra(DBHelper.FAV_TABLE_COL.TITLE_COL, mCursor
					.getString(DBHelper.FAV_TABLE_INDEX.TITLE_INDEX));
			intent.putExtra(DBHelper.FAV_TABLE_COL.URL_COL, mCursor
					.getString(DBHelper.FAV_TABLE_INDEX.URL_IDNEX));
			startActivityForResult(intent, ACTION_EDIT);
		} else {
			if (mCursor.getCount() >= 10) {
				Toast.makeText(this, R.string.fav_exceed_warning, Toast.LENGTH_LONG).show();
				return ;
			}
			intent.putExtra(TYPE_EXTRA, ACTION_ADD);
			startActivityForResult(intent, ACTION_ADD);
		} 
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK && data != null) {
			if (requestCode == ACTION_ADD) {
				String title = data.getExtras().getString(DBHelper.FAV_TABLE_COL.TITLE_COL);
				String url = data.getExtras().getString(DBHelper.FAV_TABLE_COL.URL_COL);
				Constant.insertFav(title, url);
			} else if (requestCode == ACTION_EDIT) {
				int id = data.getExtras().getInt(DBHelper.FAV_TABLE_COL.ID_COL);
				String remove = data.getExtras().getString(TYPE_REMOVE);
				if (LOG_ENABLED) {Log.e(LOG_TAG, "remove is " + remove);}
				if (remove != null && remove.equals("true")) {
					Constant.deleteFav(id);
				} else {
					String title = data.getExtras().getString(DBHelper.FAV_TABLE_COL.TITLE_COL);
					String url = data.getExtras().getString(DBHelper.FAV_TABLE_COL.URL_COL);
					Constant.updateFav(id, title, url);
				}
			}
			Adapter adapter = (Adapter) getListAdapter();
			adapter.onChanged();
		}
	}
	
	private interface NotifyDataChange {
		void onChanged();
	}

	private class Adapter extends BaseAdapter implements NotifyDataChange{
		public Adapter() {
			super();
			startManagingCursor(mCursor);
		} 
		
		private void refreshList() {
            if (mCursor != null) {
                stopManagingCursor(mCursor);
                mCursor.deactivate();
            }
            try {
                mCursor = Constant.queryFav();
                startManagingCursor(mCursor);
                notifyDataSetChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
		
		public int getCount() {
			return mCursor.getCount() + 1;
		}

		public Object getItem(int arg0) {
			return null;
		}

		public long getItemId(int arg0) {
			return 0;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = LayoutInflater.from(FavListActivity.this);
			if (position != getCount() -1) {//list item
				mCursor.moveToPosition(position);
				convertView = applyItem(inflater);
				return convertView;
			} else if (position == getCount() - 1) {
				LinearLayout newLayout = new LinearLayout(FavListActivity.this);
				inflater.inflate(R.layout.fav_list_add, newLayout);
				convertView = newLayout;
				return convertView;
			}
			return null;
		}
		
		private LinearLayout applyItem(LayoutInflater inflater) {
			LinearLayout newLayout= new LinearLayout(FavListActivity.this);
			inflater.inflate(R.layout.fav_list_row, newLayout);
			TextView title = (TextView) newLayout.findViewById(R.id.title_text);
			TextView url = (TextView) newLayout.findViewById(R.id.url_text);
			title.setText(mCursor.getString(DBHelper.FAV_TABLE_INDEX.TITLE_INDEX));
			url.setText(mCursor.getString(DBHelper.FAV_TABLE_INDEX.URL_IDNEX));
			return newLayout;
		}

		public void onChanged() {
			refreshList();
		}
	}
	
}
