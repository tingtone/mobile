package com.web.all;

import android.app.Activity;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class EditHomeActivity extends Activity {

	private TextView nameTextView;
	private TextView urlTextView;
	private Button okBtn, cancelBtn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_LEFT_ICON);
		setContentView(R.layout.browser_add_bookmark);
		getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_menu_home);
		nameTextView = (TextView) findViewById(R.id.title);
		urlTextView = (TextView) findViewById(R.id.address);
		okBtn = (Button) findViewById(R.id.OK);
		cancelBtn = (Button) findViewById(R.id.cancel);
		init();
	}
	
	private void init() {
		final Editor editor = Constant.mSetting.edit();
		nameTextView.setText(Constant.mSetting.getString(Constant.HOME_TITLE_KEY, Constant.HOME_TITLE));
		urlTextView.setText(Constant.mSetting.getString(Constant.HOME_URL_KEY, Constant.HOME_URL));
		cancelBtn.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				finish();
			}
		});
		
		okBtn.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				if (validateData()) {
					editor.putString(Constant.HOME_TITLE_KEY, nameTextView.getText().toString());
					editor.putString(Constant.HOME_URL_KEY, urlTextView.getText().toString());
					editor.commit();
					finish();
				}
			}
		});
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
	
	private boolean urlValidating(String url) {
		if (url.startsWith("http://") || url.startsWith("https://")) {
			return true;
		}
		return false;
	}
	
}
