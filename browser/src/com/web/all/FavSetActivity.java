package com.web.all;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class FavSetActivity extends Activity {

	private TextView nameTextView;
	private TextView urlTextView;
	private int action;
	private Button okBtn, cancelBtn, rmBtn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_LEFT_ICON);
		setContentView(R.layout.browser_add_bookmark);
		getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_list_bookmark);
		nameTextView = (TextView) findViewById(R.id.title);
		urlTextView = (TextView) findViewById(R.id.address);
		okBtn = (Button) findViewById(R.id.OK);
		cancelBtn = (Button) findViewById(R.id.cancel);
		rmBtn = (Button) findViewById(R.id.remove_site);
		rmBtn.setVisibility(View.VISIBLE);
		init();
	}
	
	private void init() {
		action = getIntent().getExtras().getInt(FavListActivity.TYPE_EXTRA);
		if (action == FavListActivity.ACTION_ADD) {
			if (Constant.wb.getCurrentUrl() != null && Constant.wb.getCurrentUrl().length() != 0) {
				urlTextView.setText(Constant.wb.getCurrentUrl());
			} else {
				urlTextView.setText(Constant.PREFIX);
			}
			if (Constant.wb.getCurrentTitle() != null && Constant.wb.getCurrentTitle().length() != 0) {
				nameTextView.setText(Constant.wb.getCurrentTitle());
			}
			initListener();
		} else if (action == FavListActivity.ACTION_EDIT) {
			rmBtn.setVisibility(View.VISIBLE);
			String title = getIntent().getExtras().getString(DBHelper.FAV_TABLE_COL.TITLE_COL);
			String url = getIntent().getExtras().getString(DBHelper.FAV_TABLE_COL.URL_COL);
			nameTextView.setText(title);
			urlTextView.setText(url);
			initListener();
		}
	}
	
	private boolean urlValidating(String url) {
		if (url.startsWith("http://") || url.startsWith("https://")) {
			return true;
		}
		return false;
	}
	
	private boolean validateData() {
		String msg = null;
		String title = nameTextView.getText().toString();
		String url = urlTextView.getText().toString();
		if (title.length() == 0 || url.length() ==0) {
			msg = getResources().getString(R.string.empty_data_error);
		} else if (!urlValidating(url)) {
			msg = getResources().getString(R.string.url_invalid);
		}
		if (msg != null) {
			Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}
	private void initListener() {
		okBtn.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				if (validateData()) {
					Bundle bundle = new Bundle();
					bundle.putString(DBHelper.FAV_TABLE_COL.TITLE_COL, nameTextView.getText().toString());
					bundle.putString(DBHelper.FAV_TABLE_COL.URL_COL, urlTextView.getText().toString());
					if (action == FavListActivity.ACTION_EDIT) {
						bundle.putInt(DBHelper.FAV_TABLE_COL.ID_COL,
								getIntent().getExtras().getInt(
										DBHelper.FAV_TABLE_COL.ID_COL));
					}
					Intent intent = new Intent();
					intent.putExtras(bundle);
					setResult(RESULT_OK, intent);
					finish();
				}
			}
		});
		
		cancelBtn.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				finish();
			}
		});
		
		rmBtn.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				if (action == FavListActivity.ACTION_EDIT) {
					Intent intent = new Intent();
					intent.putExtra(DBHelper.FAV_TABLE_COL.ID_COL, getIntent().getExtras().getInt(DBHelper.FAV_TABLE_COL.ID_COL));
					intent.putExtra(FavListActivity.TYPE_REMOVE, "true");
					setResult(RESULT_OK, intent);
					finish();
				}
			}
		});
	}
}
