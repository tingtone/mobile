package com.web.all;

import java.util.Date;

import android.view.MotionEvent;
import android.webkit.WebSettings;
import android.webkit.WebView;

import android.content.Intent;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Intents.Insert;

public class Api5 {
  public static final void setScrollbarFadingEnabled(WebView w, boolean set) {
    w.setScrollbarFadingEnabled(set);
  }
  
  public static final int getPointerCount(MotionEvent ev) {
    return ev.getPointerCount();
  }
  
  public static final Intent getInsertContactIntent(String extra) {
	  Intent addIntent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
	  addIntent.putExtra(Insert.PHONE, Uri.decode(extra));
	  addIntent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
	  return addIntent;
  }
  
  public static final void setPluginState(WebSettings s, int state) {
	  switch (state) {
	  case 0:
		  s.setPluginState(WebSettings.PluginState.OFF);
		  break;
	  case 1:
		  s.setPluginState(WebSettings.PluginState.ON);
		  break;
	  case 2:
		  s.setPluginState(WebSettings.PluginState.ON_DEMAND);
		  break;
	  }
  }
  
  public static Date getValidBefore(SslCertificate certificate) {
	  return certificate.getValidNotBeforeDate();
  }
  
  public static Date getValidAfter(SslCertificate certificate) {
	  return certificate.getValidNotAfterDate();
  }
  
  public static final void moreSettingFor7(WebSettings s) {
	  s.setLoadWithOverviewMode(true);
  }
}
