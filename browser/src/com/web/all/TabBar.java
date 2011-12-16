package com.web.all;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;

public class TabBar extends LinearLayout {
	public TabBar(Context context) {
		this(context, null);
	}
	
	public TabBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
	}
	
	private void init(Context context) {
		setFocusable(false);
	}
	
    @Override
    public void addView(View child) {
        if (child.getLayoutParams() == null) {
            final LinearLayout.LayoutParams lp = new LayoutParams(
                    0,
                    LayoutParams.WRAP_CONTENT, 1);
            lp.setMargins(0, 0, 0, 0);
            child.setLayoutParams(lp);
        }

        // Ensure you can navigate to the tab with the keyboard, and you can touch it
        child.setFocusable(true);
        child.setClickable(true);

        super.addView(child);
    }

    public void show() {
        fade(View.VISIBLE, 0.0f, 1.0f);
    }
    
    public void hide() {
        fade(View.GONE, 1.0f, 0.0f);
    }
    
    private void fade(int visibility, float startAlpha, float endAlpha) {
        AlphaAnimation anim = new AlphaAnimation(startAlpha, endAlpha);
        anim.setDuration(500);
        startAnimation(anim);
        setVisibility(visibility);
    }

}
