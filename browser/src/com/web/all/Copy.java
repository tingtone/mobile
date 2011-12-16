package com.web.all;

import android.app.Activity;
import android.content.Context;
import android.text.ClipboardManager;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;

public class Copy implements OnMenuItemClickListener {
    private CharSequence mText;
    private Activity mAct;
    public boolean onMenuItemClick(MenuItem item) {
        copy(mText, mAct);
        return true;
    }
    
    public static void copy(CharSequence text, Activity a) {
    	ClipboardManager clip = (ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clip != null) {
        	clip.setText(text);
        }
    }
    public Copy(CharSequence toCopy, Activity a) {
        mText = toCopy;
        mAct = a;
    }
}