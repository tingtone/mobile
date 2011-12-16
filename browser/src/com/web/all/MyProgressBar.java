package com.web.all;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class MyProgressBar extends TextView {

	private int progress;
	private Bitmap resource;
	
	public MyProgressBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		resource = BitmapFactory.decodeResource(getResources(), R.drawable.url_progress);
	}
	
	
	
	@Override
	protected void onDraw(Canvas canvas) {
		if (progress == 100 || progress == 0) {
			super.onDraw(canvas);
			return ;
		}
		int w = getWidth() * progress / 100;
		int offsetX = getScrollX(), offsetY = getScrollY();
		Paint paint = new Paint();
		for (int i = offsetX; i < offsetX + w; i++) {
			canvas.drawBitmap(resource, i, offsetY, paint);
		}
		super.onDraw(canvas);
	}



	public void setProgress(int newProgress) {
		progress = newProgress;
		postInvalidate();
		if (newProgress == 100 || newProgress ==0) {
			setVisibility(View.GONE);
		} else {
			setVisibility(View.VISIBLE);
		}
	}

}
