package com.web.all;

import java.io.File;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.ClipboardManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View.OnClickListener;
import android.webkit.CookieSyncManager;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.Browser;
import android.provider.Contacts;
import android.provider.Contacts.Intents.Insert;

public class WebBrowse extends Activity implements CallBack {
	// as SSLCertificateOnError has different style for landscape / portrait,
    // we have to re-open it when configuration changed
    private AlertDialog mSSLCertificateOnErrorDialog;
    private WebView mSSLCertificateOnErrorView;
    private SslErrorHandler mSSLCertificateOnErrorHandler;
    private SslError mSSLCertificateOnErrorError;
	
	public int mTabCurrentType = 0;
	private static final String LOG_TAG = "WebBrowse";
	private ArrayList<Site> cate;
	private MyProgressBar progressBar;
	TabControl mTabControl;
	FrameLayout mContentView;
	// all apps share same preferences
	private ImageView mNewTab;
	private AlertDialog mQuitDialog;
	/* package */ final static String SCHEME_WTAI = "wtai://wp/";
    /* package */ final static String SCHEME_WTAI_MC = "wtai://wp/mc;";
    /* package */ final static String SCHEME_WTAI_SD = "wtai://wp/sd;";
    /* package */ final static String SCHEME_WTAI_AP = "wtai://wp/ap;";
	
	private final int NEW_TAB = 1;
	private static final String NEW_TAB_KEY = "newtabint";
	
	private final static String DEFAULT_TAB_PREF = "defaulttab";
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Tab t = mTabControl.getCurrentTab();
			if (t != null) {
				WebView webView = t.getWebView();
				if (webView.canGoBack()) {
					webView.goBack();
					return true;
				} else {
					confirmToQuit();
					return true;
				}
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	
	public void newTab() {
		if (Constant.USE_DATABASE) {
			cate.clear();
			try {
				Constant.queryFav(cate);
			} catch (Exception e) {
			}
			
			if (cate != null) {
				showDialog(NEW_TAB);
			} else {
				final Tab t = mTabControl.createNewTab(false, null, null);
				if (t != null) {
					mTabControl.setCurrentTab(t);
					bookmarksOrHistoryPicker(null, false);
				}
			}
		} else {
			if (Constant.DEFAULT_TABLE.length != 0) {
				showDialog(NEW_TAB);
			}
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Constant.wb = this;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		cate = new ArrayList<Site>();		
		//Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(this));
		mContentView = (FrameLayout) findViewById(R.id.web);
		Constant.mSetting = getSharedPreferences(Constant.MOBILE_BROWSER_PREF, 0);
		mResolver = getContentResolver();
		// use message queue to handle the data init
		mHandler.sendEmptyMessage(DATA_INIT_START);
		mTabControl = new TabControl(this);
		mNewTab = (ImageView) findViewById(R.id.tab_new);
		progressBar = (MyProgressBar) findViewById(R.id.my_progress_bar);

		mNewTab.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				newTab();
			}

		});
		
		CookieSyncManager.createInstance(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.e(LOG_TAG, "DataBase closed");
		Constant.dbHelper.close();
	}

	public void closeCurrentTab() {
		int num = mTabControl.getTabCount();
		if (num > 1) {
			int curindex = mTabControl.getCurrentIndex();
			Tab current = mTabControl.getCurrentTab();

			final int index = curindex > 0 ? (curindex - 1) : (curindex + 1);
			Tab nextTab = mTabControl.getTab(index);

			mTabControl.switchViews(nextTab);
			mTabControl.removeTab(current);

			mTabControl.setCurrentTab(nextTab);
		} else {
			confirmToQuit();
		}
	}

	public void saveDefaultCate() {
		Editor e = Constant.mSetting.edit();
		e.putString(Constant.CATE_KEY, Constant.category);
		e.putString(Constant.HOME_TITLE_KEY, Constant.HOME_TITLE);
		e.putString(Constant.HOME_URL_KEY,Constant.HOME_URL); 
		e.commit();
	}
	
	public void saveDefaultTab(String title, String url) {
		SharedPreferences pf = getSharedPreferences(DEFAULT_TAB_PREF, 0);
		Map<String, ?> layers = pf.getAll();
		Editor e = pf.edit();
		for(String id:layers.keySet()) {
			e.remove(id);
		}
		e.putString(title, url);
		e.commit();
	}
	
	public void removeDefaultTab(String title) {
		SharedPreferences pf = getSharedPreferences(DEFAULT_TAB_PREF, 0);
		Editor e = pf.edit();
		e.remove(title);
		e.commit();
	}

	public boolean isDefaultTab(String title) {
		SharedPreferences pf = getSharedPreferences(DEFAULT_TAB_PREF, 0);
		return pf.getString(title, null) != null;
	}

	private boolean loadDefaultTab() {
		SharedPreferences pf = getSharedPreferences(DEFAULT_TAB_PREF, 0);
		Map<String, ?> layers = pf.getAll();

		if (layers.size() == 0) {
			return false;
		}
		for (String id : layers.keySet()) {
			String url = pf.getString(id, null);
			Tab tab = mTabControl.createNewTab(false, url, id);
			if (tab != null) {
				mTabControl.setCurrentTab(tab);
				tab.setTitle(id);
				tab.loadUrl(url);
			}
		}
		return true;
	}

	/* package */static final FrameLayout.LayoutParams COVER_SCREEN_PARAMS = new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.FILL_PARENT,
			ViewGroup.LayoutParams.FILL_PARENT);

	// Attach the given tab to the content view.
	public void attachTabToContentView(WebView w) {
		// Attach the main WebView.
		mContentView.addView(w, COVER_SCREEN_PARAMS);
		w.requestFocus();
	}

	// Remove the given tab from the content view.
	public void removeTabFromContentView(WebView w) {
		// Remove the main WebView.
		mContentView.removeView(w);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.e(LOG_TAG, "onresume");
		if (mTabControl != null && mTabControl.getTabCount() > 0) {
			CookieSyncManager.getInstance().startSync();
		}
		WebView.enablePlatformNotifications();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mTabControl.getTabCount() > 0) {
			CookieSyncManager.getInstance().stopSync();
		}
		WebView.disablePlatformNotifications();

	}

	public WebViewClient getWebViewClient() {
		return _webViewClient;
	}

	private void showSSLCertificateOnError(final WebView view,
			final SslErrorHandler handler, final SslError error) {
		final View certificateView = inflateCertificateView(error
				.getCertificate());
		if (certificateView == null) {
			return;
		}

		LayoutInflater factory = LayoutInflater.from(this);

		final LinearLayout placeholder = (LinearLayout) certificateView
				.findViewById(R.id.placeholder);

		if (error.hasError(SslError.SSL_UNTRUSTED)) {
			LinearLayout ll = (LinearLayout) factory.inflate(
					R.layout.ssl_warning, placeholder);
			((TextView) ll.findViewById(R.id.warning))
					.setText(R.string.ssl_untrusted);
		}

		if (error.hasError(SslError.SSL_IDMISMATCH)) {
			LinearLayout ll = (LinearLayout) factory.inflate(
					R.layout.ssl_warning, placeholder);
			((TextView) ll.findViewById(R.id.warning))
					.setText(R.string.ssl_mismatch);
		}

		if (error.hasError(SslError.SSL_EXPIRED)) {
			LinearLayout ll = (LinearLayout) factory.inflate(
					R.layout.ssl_warning, placeholder);
			((TextView) ll.findViewById(R.id.warning))
					.setText(R.string.ssl_expired);
		}

		if (error.hasError(SslError.SSL_NOTYETVALID)) {
			LinearLayout ll = (LinearLayout) factory.inflate(
					R.layout.ssl_warning, placeholder);
			((TextView) ll.findViewById(R.id.warning))
					.setText(R.string.ssl_not_yet_valid);
		}
		
		mSSLCertificateOnErrorHandler = handler;
        mSSLCertificateOnErrorView = view;
        mSSLCertificateOnErrorError = error;
        
        mSSLCertificateOnErrorDialog =
		new AlertDialog.Builder(this)
        .setTitle(R.string.ssl_certificate).setIcon(
            R.drawable.ic_dialog_browser_certificate_partially_secure)
        .setView(certificateView)
        .setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								mSSLCertificateOnErrorDialog = null;
								mSSLCertificateOnErrorView = null;
								mSSLCertificateOnErrorHandler = null;
								mSSLCertificateOnErrorError = null;
								getWebViewClient().onReceivedSslError(view,
										handler, error);
							}
                }).setOnCancelListener(
                new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        mSSLCertificateOnErrorDialog = null;
                        mSSLCertificateOnErrorView = null;
                        mSSLCertificateOnErrorHandler = null;
                        mSSLCertificateOnErrorError = null;

                        getWebViewClient().onReceivedSslError(
                                        view, handler, error);
                    }
                })
        .show();

	}
	
	private View inflateCertificateView(SslCertificate certificate) {
		if (certificate == null) {
            return null;
        }
		
		LayoutInflater factory = LayoutInflater.from(WebBrowse.this);
		View certificateView = factory.inflate(
	            R.layout.ssl_certificate, null);
		
		 // issued to:
        SslCertificate.DName issuedTo = certificate.getIssuedTo();
        if (issuedTo != null) {
            ((TextView) certificateView.findViewById(R.id.to_common))
                .setText(issuedTo.getCName());
            ((TextView) certificateView.findViewById(R.id.to_org))
                .setText(issuedTo.getOName());
            ((TextView) certificateView.findViewById(R.id.to_org_unit))
                .setText(issuedTo.getUName());
        }
        
        // issued by:
        SslCertificate.DName issuedBy = certificate.getIssuedBy();
        if (issuedBy != null) {
            ((TextView) certificateView.findViewById(R.id.by_common))
                .setText(issuedBy.getCName());
            ((TextView) certificateView.findViewById(R.id.by_org))
                .setText(issuedBy.getOName());
            ((TextView) certificateView.findViewById(R.id.by_org_unit))
                .setText(issuedBy.getUName());
        }
        
        // issued on:
        String issuedOn;
        if (Constant.sdk >= 8) {
        	issuedOn = formatCertificateDate(Api5.getValidBefore(certificate));
        } else {
        	issuedOn = reformatCertificateDate(certificate.getValidNotBefore());
        }
        
        ((TextView) certificateView.findViewById(R.id.issued_on))
            .setText(issuedOn);

        // expires on:
        String expiresOn;
        if (Constant.sdk >= 8) {
        	expiresOn = formatCertificateDate(Api5.getValidAfter(certificate));
        } else {
        	expiresOn = reformatCertificateDate(certificate.getValidNotAfter());
        }
        
        ((TextView) certificateView.findViewById(R.id.expires_on))
            .setText(expiresOn);
        
        return certificateView;
	}
	
	private String formatCertificateDate(Date certificateDate) {
		if (certificateDate == null) {
			return "";
		}
		String formattedDate = DateFormat.getDateFormat(this).format(
				certificateDate);
		if (formattedDate == null) {
			return "";
		}
		return formattedDate;
	}
	
	private String reformatCertificateDate(String certificateDate) {
	      String reformattedDate = null;

	      if (certificateDate != null) {
	          Date date = null;
	          try {
	              date = java.text.DateFormat.getInstance().parse(certificateDate);
	          } catch (ParseException e) {
	              date = null;
	          }

	          if (date != null) {
	              reformattedDate =
	                  DateFormat.getDateFormat(this).format(date);
	          }
	      }

	      return reformattedDate != null ? reformattedDate :
	          (certificateDate != null ? certificateDate : "");
	    }
	
	private WebViewClient _webViewClient = new WebViewClient() {

		public void onPageStarted(WebView webview, String url, Bitmap bitmap) {
			Tab t = mTabControl.getTabFromView(webview);
			//t.showProcess(true);
			CookieSyncManager.getInstance().resetSync();
			t.mInLoad = true;
			try {
				Browser.updateVisitedHistory(mResolver, url, true);
			} catch (Exception e) {
				
			}
		}

		public void onPageFinished(WebView webview, String url) {
			Tab t = mTabControl.getTabFromView(webview);
			CookieSyncManager.getInstance().sync();
			t.mInLoad = false;
			t.updateTabView();
		}
		
		
		
		@Override
		public void onReceivedSslError(final WebView view, final SslErrorHandler handler,
				final SslError error) {
			if (view != mTabControl.getCurrentTopWebView()) {
				handler.cancel();
				return ;
			}
			//in the future we may let the user to choose whether need to show the warning dialog
			boolean showWarnings = true;
			if (showWarnings) {
				final LayoutInflater factory = LayoutInflater
						.from(WebBrowse.this);
				final View warningsView = factory.inflate(
						R.layout.ssl_warnings, null);
				final LinearLayout placeholder = (LinearLayout) warningsView
						.findViewById(R.id.placeholder);
				if (error.hasError(SslError.SSL_UNTRUSTED)) {
					LinearLayout ll = (LinearLayout) factory.inflate(
							R.layout.ssl_warning, null);
					((TextView) ll.findViewById(R.id.warning))
							.setText(R.string.ssl_untrusted);
					placeholder.addView(ll);
				}
				
				if (error.hasError(SslError.SSL_IDMISMATCH)) {
					LinearLayout ll = (LinearLayout) factory.inflate(
							R.layout.ssl_warning, null);
					((TextView) ll.findViewById(R.id.warning))
							.setText(R.string.ssl_mismatch);
					placeholder.addView(ll);
				}
				
				if (error.hasError(SslError.SSL_EXPIRED)) {
                    LinearLayout ll = (LinearLayout)factory
                        .inflate(R.layout.ssl_warning, null);
                    ((TextView)ll.findViewById(R.id.warning))
                        .setText(R.string.ssl_expired);
                    placeholder.addView(ll);
                }
				
				if (error.hasError(SslError.SSL_NOTYETVALID)) {
                    LinearLayout ll = (LinearLayout)factory
                        .inflate(R.layout.ssl_warning, null);
                    ((TextView)ll.findViewById(R.id.warning))
                        .setText(R.string.ssl_not_yet_valid);
                    placeholder.addView(ll);
                }
				
				new AlertDialog.Builder(WebBrowse.this).setTitle(
						R.string.security_warning).setIcon(
						android.R.drawable.ic_dialog_alert).setView(
						warningsView).setPositiveButton(R.string.ssl_continue,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								handler.proceed();
							}
						}).setNeutralButton(R.string.view_certificate,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								showSSLCertificateOnError(view,
										handler, error);
							}
						}).setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								handler.cancel();
								//mActivity.resetTitleAndRevertLockIcon();
							}
						}).setOnCancelListener(
						new DialogInterface.OnCancelListener() {
							public void onCancel(DialogInterface dialog) {
								handler.cancel();
								//mActivity.resetTitleAndRevertLockIcon();
							}
						}).show();
				
			} else {
				handler.proceed();
			}
		}
		
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			Log.e(LOG_TAG, "should url is " + url);
			//for "http://" and "https://" request, we ignore
			if (url.startsWith("http://") || url.startsWith("https://")) {
				return false;
			}
	        if (url.startsWith(SCHEME_WTAI)) {
	            // wtai://wp/mc;number
	            // number=string(phone-number)
	            if (url.startsWith(SCHEME_WTAI_MC)) {
	                Intent intent = new Intent(Intent.ACTION_VIEW,
	                        Uri.parse(WebView.SCHEME_TEL +
	                        url.substring(SCHEME_WTAI_MC.length())));
	                startActivity(intent);
	                return true;
	            }
	            // wtai://wp/sd;dtmf
	            // dtmf=string(dialstring)
	            if (url.startsWith(SCHEME_WTAI_SD)) {
	                // TODO: only send when there is active voice connection
	                return false;
	            }
	            // wtai://wp/ap;number;name
	            // number=string(phone-number)
	            // name=string
	            if (url.startsWith(SCHEME_WTAI_AP)) {
	                // TODO
	                return false;
	            }
	        }

	        // The "about:" schemes are internal to the browser; don't want these to
	        // be dispatched to other apps.
	        if (url.startsWith("about:")) {
	            return false;
	        }

	        Uri uri;
            try {
                uri = Uri.parse(url);
            } catch (IllegalArgumentException ex) {
                return false;
            }

            // check whether other activities want to handle this url
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            try {
                if (startActivityIfNeeded(intent, -1)) {
                    return true;
                }
            } catch (ActivityNotFoundException ex) {
            	Log.e(LOG_TAG, "can't find activity");
                // ignore the error. If no application can handle the URL,
                // eg about:blank, assume the browser can handle it.
            }
	        Log.e(LOG_TAG, "should url no need to start activity");
	        return false;
		}
	};

	private void confirmToQuit() {

		mQuitDialog = new AlertDialog.Builder(this).setMessage(
				R.string.quit_confirm_message).setPositiveButton("Yes",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						//use moveTaskToBack?
						//moveTaskToBack(true);
						finish();
					}
				}).setNegativeButton("No",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

					}
				}).setIcon(R.drawable.ic_menu_exit).setTitle(getResources().getString(R.string.warning)).create();

		mQuitDialog.show();

	}

	private static final int HOME_ID = Menu.FIRST + 100;
	private static final int CLOSE_MENU_ID = Menu.FIRST + 102;
	private boolean isFullScreen = false;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.browser, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		MenuItem reload = menu.findItem(R.id.reload_menu_id);
		Tab t = mTabControl.getCurrentTab();
		if (t != null) {
			if (t.mInLoad) {
				reload.setTitle(R.string.stop);
			} else {
				reload.setTitle(R.string.reload);
			}
			WebView webView = t.getWebView();
			boolean cangoback = webView.canGoBack();
			menuControl(cangoback, menu, HOME_ID, R.string.go_home,
					R.drawable.home_32);
			menu.findItem(R.id.forward_menu_id).setEnabled(webView.canGoForward());
		}
		menuControl(mTabControl.getTabCount() > 1, menu,
				CLOSE_MENU_ID, R.string.close_menu_id, 0);

		return true;
	}

	private void menuControl(boolean shouldAdd, Menu menu, int id, int title,
			int icon) {
		MenuItem home = menu.findItem(id);
		if (home != null) {
			if (!shouldAdd) {
				menu.removeItem(id);
			}
		} else {
			if (shouldAdd) {
				MenuItem mi = menu.add(0, id, 0, title);
				if (icon != 0) {
					mi.setIcon(icon);
				}
			}
		}
	}

	private FindDialog mFindDialog;

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Tab t = mTabControl.getCurrentTab();
		WebView webView = t.getWebView();
		switch (item.getItemId()) {
		case R.id.find_menu_id:
			if (null == mFindDialog) {
				mFindDialog = new FindDialog(this);
			}

			mFindDialog.setWebView(webView);
			mFindDialog.show();
			return true;
		case HOME_ID:
			t.loadHome();
			return true;
		case R.id.reload_menu_id:
			if (t.mInLoad) {
				// mInLoad = false;
				Toast.makeText(this, R.string.stop_toast_text, Toast.LENGTH_SHORT).show();
				webView.stopLoading();
				_webViewClient.onPageFinished(webView, webView.getUrl());

			} else {
				webView.reload();
			}
			return true;
		case R.id.forward_menu_id:
			webView.goForward();
			return true;
		case CLOSE_MENU_ID:
			closeCurrentTab();
			return true;
		case R.id.bookmark_menu_id:
			bookmarksOrHistoryPicker(null, true);
			return true;
		case R.id.exit_menu_id:
			confirmToQuit();
			return true;
		case R.id.full_screen_menu_id:
			if (!isFullScreen) {
				mNewTab.setVisibility(View.GONE);
				isFullScreen = true;
				item.setIcon(getResources().getDrawable(
						R.drawable.exit_full_screen_icon));
				item.setTitle(R.string.exit_full_screen);
				mTabControl.mTabbar.hide();
			} else {
				mNewTab.setVisibility(View.VISIBLE);
				if (mTabControl.getTabCount() > 1)
					mNewTab.setVisibility(View.VISIBLE);
				isFullScreen = false;
				item.setIcon(getResources().getDrawable(
						R.drawable.full_screen_icon));
				item.setTitle(R.string.full_screen);
				mTabControl.mTabbar.show();
			}
			return true;
			
		case R.id.edit_fav_menu_id:
			Intent editIntent = new Intent(WebBrowse.this, FavListActivity.class);
			startActivity(editIntent);
			return true;
		
		case R.id.edit_homepage_menu_id:
			Intent homeIntent = new Intent(WebBrowse.this, EditHomeActivity.class);
			startActivity(homeIntent);
			return true;
			
		case R.id.select_text_menu_id:
			Tab current = mTabControl.getCurrentTab();
			//according to sdk, this method will be updated
			if (current != null) {
				current.getWebView().emulateShift();
			}
			return true;
			
		case R.id.clear_cache_menu_id:
			webView.clearCache(true);
			removeAllCacheFiles(this);
			return true;
		case R.id.search_menu_id:
			onSearchRequested();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		WebView webview = (WebView) v;
		WebView.HitTestResult result = webview.getHitTestResult();
		if (result == null) {
			return;
		}

		int type = result.getType();
		if (type == WebView.HitTestResult.UNKNOWN_TYPE) {
			return;
		}
		if (type == WebView.HitTestResult.EDIT_TEXT_TYPE) {
			// let TextView handles context menu
			return;
		}

		// Note, http://b/issue?id=1106666 is requesting that
		// an inflated menu can be used again. This is not available
		// yet, so inflate each time (yuk!)
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.browsercontext, menu);

		// Show the correct menu group
		final String extra = result.getExtra();
		menu.setGroupVisible(R.id.PHONE_MENU,
				type == WebView.HitTestResult.PHONE_TYPE);
		menu.setGroupVisible(R.id.EMAIL_MENU,
				type == WebView.HitTestResult.EMAIL_TYPE);
		menu.setGroupVisible(R.id.GEO_MENU,
				type == WebView.HitTestResult.GEO_TYPE);
		menu.setGroupVisible(R.id.IMAGE_MENU,
				type == WebView.HitTestResult.IMAGE_TYPE
						|| type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE);
		menu.setGroupVisible(R.id.ANCHOR_MENU,
				type == WebView.HitTestResult.SRC_ANCHOR_TYPE
						|| type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE);

		// Setup custom handling depending on the type
		switch (type) {
		case WebView.HitTestResult.PHONE_TYPE:
			menu.setHeaderTitle(Uri.decode(extra));
			menu.findItem(R.id.dial_context_menu_id).setIntent(
					new Intent(Intent.ACTION_VIEW, Uri.parse(WebView.SCHEME_TEL
							+ extra)));
			Intent addIntent = null;
			if (Constant.sdk >= 5) {
				addIntent = Api5.getInsertContactIntent(extra);
			} else {
				addIntent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
				addIntent.putExtra(Insert.PHONE, Uri.decode(extra));
				addIntent.setType(Contacts.People.CONTENT_ITEM_TYPE);
			}
			menu.findItem(R.id.add_contact_context_menu_id)
					.setIntent(addIntent);
			menu.findItem(R.id.copy_phone_context_menu_id)
					.setOnMenuItemClickListener(new Copy(extra, this));
			break;

		case WebView.HitTestResult.EMAIL_TYPE:
			menu.setHeaderTitle(extra);
			menu.findItem(R.id.email_context_menu_id).setIntent(
					new Intent(Intent.ACTION_VIEW, Uri
							.parse(WebView.SCHEME_MAILTO + extra)));
			menu.findItem(R.id.copy_mail_context_menu_id)
					.setOnMenuItemClickListener(new Copy(extra, this));
			break;

		case WebView.HitTestResult.GEO_TYPE:
			menu.setHeaderTitle(extra);
			menu.findItem(R.id.map_context_menu_id).setIntent(
					new Intent(Intent.ACTION_VIEW, Uri.parse(WebView.SCHEME_GEO
							+ URLEncoder.encode(extra))));
			menu.findItem(R.id.copy_geo_context_menu_id)
					.setOnMenuItemClickListener(new Copy(extra, this));
			break;

		case WebView.HitTestResult.SRC_ANCHOR_TYPE:
		case WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE:
			TextView titleView = (TextView) LayoutInflater.from(this).inflate(
					android.R.layout.browser_link_context_header, null);
			titleView.setText(extra);
			menu.setHeaderView(titleView);
			// decide whether to show the open link in new tab option
			menu.findItem(R.id.open_newtab_context_menu_id).setVisible(
					mTabControl.getTabCount() < TabControl.MAX_TABS);
			PackageManager pm = getPackageManager();
			Intent send = new Intent(Intent.ACTION_SEND);
			send.setType("text/plain");
			ResolveInfo ri = pm.resolveActivity(send,
					PackageManager.MATCH_DEFAULT_ONLY);
			menu.findItem(R.id.share_link_context_menu_id).setVisible(
					ri != null);
			if (type == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
				break;
			}
			// otherwise fall through to handle image part
		case WebView.HitTestResult.IMAGE_TYPE:
			if (type == WebView.HitTestResult.IMAGE_TYPE) {
				menu.setHeaderTitle(extra);
			}
			menu.findItem(R.id.view_image_context_menu_id)
					.setOnMenuItemClickListener(new OnMenuItemClickListener() {

						public boolean onMenuItemClick(MenuItem item) {
							WebView w = mTabControl.getCurrentWebView();
							if (w != null) {
								w.loadUrl(extra);
							}
							return true;
						}

					});

			break;

		default:
			break;
		}
	}

	private void loadStartTab() {
		boolean first = Constant.mSetting.getBoolean(Constant.FIRST_START_KEY, true);
		if (first) {
			// save default tab
			giveUserTips(1);
			Editor e = Constant.mSetting.edit();
			e.putBoolean(Constant.FIRST_START_KEY, false);
			e.commit();

			if (Constant.USE_DATABASE) {
				saveDefaultCate();
			} else {
				saveDefaultTab(Constant.getDefaultHomeTitle(), Constant.getDefaultHomeUrl());
			}
		}
		if (Constant.USE_DATABASE) {
			Constant.category = Constant.mSetting
			.getString(Constant.CATE_KEY, Constant.category);
		}
		if (Constant.USE_DATABASE) {
			Tab tab = mTabControl.createNewTab(false, null, null);
			if (tab != null) {
				tab.setCurrentTab();
				//load home url
				Intent intent = getIntent();
				final String action = intent.getAction();
				
				String url = null;
				if (Intent.ACTION_VIEW.equals(action)) {
					url = intent.getData().toString();
				} else if (Intent.ACTION_SEARCH.equals(action)
						|| Intent.ACTION_WEB_SEARCH.equals(action)) {
					url = intent.getStringExtra(SearchManager.QUERY);
				}
				if (url != null && handleWebSearchRequest(url, intent
						.getBundleExtra(SearchManager.APP_DATA))) {
					return;
				}
				if (url != null) {
					if (!url.startsWith("http")) {
						url = "http://" + url;
					}
					tab.getWebView().loadUrl(url);
					return;
				}
				tab.getWebView().loadUrl(Constant.mSetting
						.getString(Constant.HOME_URL_KEY, Constant.HOME_URL));
			}
		} else {
			if (!loadDefaultTab()) {
				Tab tab = mTabControl.createNewTab(false, null, null);
				if (tab != null) {
					tab.setCurrentTab();
				}
				bookmarksOrHistoryPicker(null, false);
			}
		}
	}

	private static final int FOCUS_NODE_HREF = 102;
	private static final int DATA_INIT_START = 103;
	private static final int DATA_INIT_FINISH = 104;
	private static final int DIALOG_REMOVE = 1;
	// Private handler for handling javascript and saving passwords
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case DATA_INIT_START:
				Constant.initDB(WebBrowse.this);
				break;
			case DATA_INIT_FINISH:
				if (Constant.dataInitDialog != null
						&& Constant.dataInitDialog.isShowing()) {
					Constant.dataInitDialog.dismiss();
				}
				// we have database now
				loadStartTab();
				break;
			case DIALOG_REMOVE:
				Log.e(LOG_TAG, "Dialog removed");
				removeDialog(NEW_TAB);
				break;
			case FOCUS_NODE_HREF:
				String url = (String) msg.getData().get("url");
				if (url == null || url.length() == 0) {
					break;
				}
				HashMap focusNodeMap = (HashMap) msg.obj;
				WebView view = (WebView) focusNodeMap.get("webview");
				Tab t = mTabControl.getCurrentTab();
				final WebView webView = t.getWebView();

				// Only apply the action if the top window did not change.
				if (webView != view) {
					break;
				}
				switch (msg.arg1) {
				case R.id.open_context_menu_id:
				case R.id.view_image_context_menu_id:
					webView.loadUrl(url);
					break;
				case R.id.open_newtab_context_menu_id:
					final Tab tab = mTabControl.createNewTab(false, url,
							Constant.getTitleFromUrl(url));
					if (tab != null) {
						tab.setCurrentTab();
						tab.getWebView().loadUrl(url);
					}
					break;
				case R.id.bookmark_context_menu_id:
					Intent intent = new Intent(WebBrowse.this,
							AddBookmarkPage.class);
					intent.putExtra("url", url);
					startActivity(intent);
					break;
				case R.id.share_link_context_menu_id:
					Browser.sendString(WebBrowse.this, url);
					break;
				case R.id.copy_link_context_menu_id:
					copy(url);
					break;
				}
				break;
			}
		}
	};

	private void copy(CharSequence text) {
		ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
		if (clip != null) {
			clip.setText(text);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		int id = item.getItemId();
		Tab t = mTabControl.getCurrentTab();
		final WebView webView = t.getWebView();
		final HashMap hrefMap = new HashMap();
		hrefMap.put("webview", webView);
		switch (id) {
		// -- Browser context menu
		case R.id.open_context_menu_id:
		case R.id.open_newtab_context_menu_id:
		case R.id.bookmark_context_menu_id:
			// case R.id.save_link_context_menu_id:
		case R.id.share_link_context_menu_id:
		case R.id.copy_link_context_menu_id:
			final Message msg = mHandler.obtainMessage(FOCUS_NODE_HREF, id, 0,
					hrefMap);
			webView.requestFocusNodeHref(msg);
			break;

		default:
			// For other context menus
			return onOptionsItemSelected(item);
		}
		return true;
	}

	public static void removeAllCacheFiles(final Context context) {
		// delete cache in a separate thread to not block UI.
		final Thread clearCache = new Thread() {
			public void run() {
				try {
					// delete all cache files
					File mBaseDir = new File(context.getCacheDir(),
							"webviewCache");
					try {
						String[] files = mBaseDir.list();
						if (files != null) {
							for (int i = 0; i < files.length; i++) {
								new File(mBaseDir, files[i]).delete();
							}
						}
					} catch (SecurityException e) {
					}
				} catch (Throwable e) {
				}
			}
		};
		clearCache.start();
	}

	private Dialog create(int icon, String title, String msg, final String uri,
			final int id) {
		Log.e(LOG_TAG, "create dialog");
		return new AlertDialog.Builder(this).setIcon(icon).setTitle(title)
				.setMessage(msg).setPositiveButton("Download",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								try {
									if (uri == null) {
										return;
									}
									Intent intent = new Intent(
											Intent.ACTION_VIEW, Uri.parse(uri));
									startActivity(intent);
								} catch (Exception e) {
									Toast.makeText(WebBrowse.this,
											R.string.market_init_failed,
											Toast.LENGTH_LONG).show();
								} finally {
									removeDialog(id);
								}
							}
						}).setNegativeButton("Ignore Forever",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								removeDialog(id);
							}
						}).setOnCancelListener(new OnCancelListener() {
							
							public void onCancel(DialogInterface dialog) {
								// TODO Auto-generated method stub
								removeDialog(id);
							}
						}).create();
	}

	private String[] getListFromCate() {

		int len = cate.size();
		Log.e(LOG_TAG, "cate length: " + len);
		String[] list = new String[len + 3];
		for (int i = 0; i < len; i++) {
			list[i] = cate.get(i).name;
		}
		list[len] = getResources().getString(R.string.choose_news_type);
		list[len + 1] = getResources().getString(R.string.search);
		list[len + 2] = getResources().getString(R.string.bookmark);
		return list;
	}

	private Dialog createNewWithoutDB() {
		final int pos = Constant.mSetting.getInt(NEW_TAB_KEY, 0);
		mTabCurrentType = pos;
		return new AlertDialog.Builder(this).setIcon(
				android.R.drawable.ic_dialog_info).setTitle(R.string.choose_site)
				.setSingleChoiceItems(Constant.DEFAULT_SITES_TITLES, pos,
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,
									int which) {
								mTabCurrentType = which;
							}
						}).setPositiveButton(getResources().getString(R.string.ok),
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,
									int which) {
								if (pos != mTabCurrentType) {
									SharedPreferences.Editor editor = Constant.mSetting
											.edit();
									editor.putInt(NEW_TAB_KEY, mTabCurrentType);
									editor.commit();
								}
								String url = null;
								if (mTabCurrentType < Constant.getSitesLen()) {
									url = Constant
											.getUrlByIndex(mTabCurrentType);
								}
								final Tab t = mTabControl.createNewTab(false,
										url, null);
								if (t != null) {
									t.setCurrentTab();
									if (url != null) {
										t.getWebView().loadUrl(url);
									} else if (mTabCurrentType == Constant
											.getSitesLen()) {
										bookmarksOrHistoryPicker(null, false);
									} else if (mTabCurrentType == Constant
											.getSitesLen() + 1) {
										startSearch(null, false, null, false);
									}
								}
								removeDialog(NEW_TAB);
							}
						}).setNegativeButton(getResources().getString(R.string.cancel),
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,
									int which) {
								removeDialog(NEW_TAB);
							}
						}).setOnCancelListener(new OnCancelListener() {
							
							public void onCancel(DialogInterface dialog) {
								// TODO Auto-generated method stub
								removeDialog(NEW_TAB);
							}
						}).create();
	}
	
	private Dialog createNewUseDB() {
		final int pos = Constant.mSetting.getInt(NEW_TAB_KEY, 0);
		mTabCurrentType = 0;
		return new AlertDialog.Builder(this).setIcon(
				R.drawable.ic_tab_browser_bookmark_unselected).setTitle(R.string.my_fav)
				.setSingleChoiceItems(getListFromCate(), -1,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								mTabCurrentType = whichButton;
								if (pos != mTabCurrentType) {
									SharedPreferences.Editor editor = Constant.mSetting
											.edit();
									editor.putInt(NEW_TAB_KEY, mTabCurrentType);
									editor.commit();
								}
								String url = null;
								if (mTabCurrentType < cate.size()) {
									url = cate.get(mTabCurrentType).m_url;
								}

								final Tab t = mTabControl.createNewTab(false,
										url, (url == null ? null : cate
												.get(mTabCurrentType).name));
								if (t != null) {
									t.setCurrentTab();
									if (url != null) {
										Site s = cate.get(mTabCurrentType);
										t.setTitle(s.name);
										WebView webView = t.getWebView();
										webView.loadUrl(url);
										giveUserTips(8);
									} else if (mTabCurrentType == cate.size()) {
										/*Intent intent = new Intent(
												WebBrowse.this,
												FavListActivity.class);
										startActivity(intent);*/
										WebView webView = t.getWebView();
										webView.loadUrl(Constant.CHANGE_NEWS);
									} else if (mTabCurrentType == cate.size() + 1) {
										startSearch(null, false, null, false);
									} else if (mTabCurrentType == cate.size() + 2) {
										bookmarksOrHistoryPicker(null, false);
									}
								}
								mHandler.sendEmptyMessage(DIALOG_REMOVE);
							}
						}).setOnCancelListener(new OnCancelListener() {

					public void onCancel(DialogInterface dialog) {
						mHandler.sendEmptyMessage(DIALOG_REMOVE);
					}
				}).create();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
       if (id == NEW_TAB && Constant.USE_DATABASE) {
			Log.e(LOG_TAG, "on create dialog!");
			return createNewUseDB();
		}
		return null;
	}

	public String getCurrentUrl() {
		WebView current = mTabControl.getCurrentWebView();
		if (current != null) {
			return current.getUrl();
		}
		return null;
	}
	
	public String getCurrentTitle() {
		WebView current = mTabControl.getCurrentWebView();
		if (current != null) {
			return current.getTitle();
		}
		return null;
	}
	
	/* package */void bookmarksOrHistoryPicker(String startTab, boolean fromMenu) {
		Tab curTab = mTabControl.getCurrentTab();
		WebView current = mTabControl.getCurrentWebView();
		Intent intent = new Intent(this, CombinedBookmarkHistoryActivity.class);
		if (current != null) {
			CharSequence title = current.getTitle();
			String url = current.getUrl();
			// Just in case the user opens bookmarks before a page finishes
			// loading
			// so the current history item, and therefore the page, is null.
			if (null == url) {
				url = Constant.homeUrl();
			}
			// In case the web page has not yet received its associated title.
			if (title == null) {
				title = Constant.getTitleFromUrl(url);
			}
			intent.putExtra(Constant.TITLE, title);
			intent.putExtra(Constant.URL, url);
			if (fromMenu)
				intent.putExtra(Constant.PATTERN, curTab.mPattern);
		}
		if (startTab != null) {
			intent.putExtra(CombinedBookmarkHistoryActivity.STARTING_TAB,
					startTab);
		}
		intent.putExtra("maxTabsOpen",
				mTabControl.getTabCount() >= TabControl.MAX_TABS);
		startActivityForResult(intent, COMBO_PAGE);
	}

	final static int COMBO_PAGE = 1;

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		switch (requestCode) {
		case COMBO_PAGE:

			if (resultCode == RESULT_OK && intent != null) {
				String data = intent.getAction();
				Bundle extras = intent.getExtras();
				Tab currentTab = mTabControl.getCurrentTab();

				boolean createNewTab = (currentTab == null || (extras != null
						&& extras.getBoolean(Constant.NEW_TAB, false) && currentTab
						.getWebView().getUrl() != null));
				if (createNewTab) {
					currentTab = mTabControl.createNewTab(false, data, Constant
							.getTitleFromUrl(data));
					if (currentTab != null) {
						mTabControl.setCurrentTab(currentTab);
					}else {
						return ;
					}					
				}
				if (data != null && data.length() != 0) {
					currentTab.getWebView().loadUrl(data);
				}
			}
			break;
		default:
			break;
		}
		Tab currentTab = mTabControl.getCurrentTab();
		if (currentTab != null)
			currentTab.getTopWindow().requestFocus();
		// getTopWindow().requestFocus();
	}

	@Override
	public boolean onSearchRequested() {
		Tab currentTab = mTabControl.getCurrentTab();
		startSearch(currentTab.getWebView().getUrl(), true, null, false);
		return true;
	}

	protected static final Pattern ACCEPTED_URI_SCHEMA = Pattern.compile("(?i)"
			+ // switch on case insensitive matching
			"("
			+ // begin group for schema
			"(?:http|https|file):\\/\\/"
			+ "|(?:inline|data|about|content|javascript):" + ")" + "(.*)");
	private static final int SHORTCUT_INVALID = 0;
	private static final int SHORTCUT_GOOGLE_SEARCH = 1;
	private static final int SHORTCUT_WIKIPEDIA_SEARCH = 2;
	private static final int SHORTCUT_DICTIONARY_SEARCH = 3;
	private static final int SHORTCUT_GOOGLE_MOBILE_LOCAL_SEARCH = 4;

	private int parseUrlShortcut(String url) {
		if (url == null)
			return SHORTCUT_INVALID;

		// FIXME: quick search, need to be customized by setting
		if (url.length() > 2 && url.charAt(1) == ' ') {
			switch (url.charAt(0)) {
			case 'g':
				return SHORTCUT_GOOGLE_SEARCH;
			case 'w':
				return SHORTCUT_WIKIPEDIA_SEARCH;
			case 'd':
				return SHORTCUT_DICTIONARY_SEARCH;
			case 'l':
				return SHORTCUT_GOOGLE_MOBILE_LOCAL_SEARCH;
			}
		}
		return SHORTCUT_INVALID;
	}

	final static String QuickSearch_G = "http://www.google.com/cse?cx=partner-pub-8798798908578754:1365569030&ie=ISO-8859-1&q=%s";
	// final static String SiteSearch_G =
	// "http://www.google.com/cse?cx=partner-pub-0186507398054381:fngtfuuemei&ie=ISO-8859-1&q=%s"
	// + "&as_sitesearch="+ SITE_URL;
	private ContentResolver mResolver;

	final static String QUERY_PLACE_HOLDER = "%s";

	private boolean handleWebSearchRequest(String inUrl, Bundle appData) {
		if (inUrl == null)
			return false;

		// In general, we shouldn't modify URL from Intent.
		// But currently, we get the user-typed URL from search box as well.
		String url = AddBookmarkPage.fixUrl(inUrl).trim();

		// URLs and site specific search shortcuts are handled by the regular
		// flow
		// of control, so
		// return early.
		if (Regex.WEB_URL_PATTERN.matcher(url).matches()
				|| ACCEPTED_URI_SCHEMA.matcher(url).matches()
				|| parseUrlShortcut(url) != SHORTCUT_INVALID) {
			return false;
		}

		Tab current = mTabControl.getCurrentTab();
		// String base = (appData == null)? SiteSearch_G : QuickSearch_G;
		// String u = URLUtil.composeSearchUrl(url, base, QUERY_PLACE_HOLDER);
		Browser.addSearchUrl(mResolver, inUrl);

		String u = URLUtil.composeSearchUrl(url, QuickSearch_G,
				QUERY_PLACE_HOLDER);
		// current.detachFromView();
		current.loadUrl(u);
		current.setTitle(inUrl);
		return true;
	}

	@Override
	public void onNewIntent(final Intent intent) {
		super.onNewIntent(intent);
		final String action = intent.getAction();
		String url = null;
		if (Intent.ACTION_VIEW.equals(action)) {
			url = intent.getData().toString();
		} else if (Intent.ACTION_SEARCH.equals(action)
				|| Intent.ACTION_WEB_SEARCH.equals(action)) {
			url = intent.getStringExtra(SearchManager.QUERY);
		}
		if (url == null) {
			return;
		}
		if (handleWebSearchRequest(url, intent
				.getBundleExtra(SearchManager.APP_DATA))) {
			return;
		}
		Tab current = mTabControl.getCurrentTab();
		if (!url.startsWith("http")) {
			url = "http://" + url;
		}

		current.loadUrl(url);
	}

	final WebChromeClient mWebChromeClient = new WebChromeClient() {
		@Override
		public boolean onCreateWindow(WebView view, final boolean dialog,
				final boolean userGesture, final Message msg) {
			if (mTabControl.getTabCount() >= TabControl.MAX_TABS) {
				return false;
			}
			if (userGesture) {
				final Tab newTab = mTabControl.createNewTab(false, null, "");
				if (newTab != null) {
					newTab.setCurrentTab();

					WebView.WebViewTransport transport = (WebView.WebViewTransport) msg.obj;
					transport.setWebView(mTabControl.getCurrentWebView());
					msg.sendToTarget();
				}
			}
			return true;
		}

		@Override
		public void onProgressChanged(WebView view, int newProgress) {
			super.onProgressChanged(view, newProgress);
			if (view == mTabControl.getCurrentTopWebView()) {
				progressBar.setText(view.getUrl());
				progressBar.setProgress(newProgress);
			}
		}
	};
	
	public void giveUserTips(int chance) {
		if (Popup.run(chance)) {
			Toast.makeText(this, getResources().getString(R.string.tips),
					Toast.LENGTH_LONG).show();
		}
	}
	
	public void refreshProgress() {
		Tab cur = mTabControl.getCurrentTab();
		if (cur != null) {
			WebView view = cur.getTopWindow();
			progressBar.setText(view.getUrl());
			progressBar.setProgress(view.getProgress());
		}
	}

	public void onDataInitFinish() {
		mHandler.sendEmptyMessage(DATA_INIT_FINISH);
	}
}
