package com.web.all;

import android.content.Context;
import android.graphics.Canvas;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.webkit.WebView;

public class ScWebView extends WebView {
	
	// Following are for swipe
	/* package */ GestureDetector mGestureDetector;
    private static final int TOUCH_MODE_INITIAL_STATE = 0;
    /**
     * Indicates we just received the touch event and we are waiting to see if
     * it is a tap or a scroll gesture.
     */
    private static final int TOUCH_MODE_DOWN = 1;

    /**
     * Indicates the touch gesture is a vertical scroll
     */
    private static final int TOUCH_MODE_VSCROLL = 0x20;

    /**
     * Indicates the touch gesture is a horizontal scroll
     */
    private static final int TOUCH_MODE_HSCROLL = 0x40;
    private static final int HORIZONTAL_SCROLL_THRESHOLD = 80;
    private static final int VERTICAL_SCROLL_THRESHOLD = -40;
    private int mTouchMode = TOUCH_MODE_INITIAL_STATE;
    private int mViewStartX;
    private int mViewStartY;
    //private int mPreviousDirection;
    //private int mPreviousDistanceX;
    private TabControl mTabs;
    public ScWebView(Context context, TabControl tabs) {
		super(context);
		mTabs = tabs;
		mGestureDetector = new GestureDetector(new WebViewGestureListener(mTabs));
	}
	
    void doScroll(MotionEvent e1, MotionEvent e2, float deltaX, float deltaY) {
        // Use the distance from the current point to the initial touch instead
        // of deltaX and deltaY to avoid accumulating floating-point rounding
        // errors.  Also, we don't need floats, we can use ints.
        int distanceX = (int) e1.getX() - (int) e2.getX();
        int distanceY = (int) e1.getY() - (int) e2.getY();

        // If we haven't figured out the predominant scroll direction yet,
        // then do it now.
        if (mTouchMode == TOUCH_MODE_DOWN) {
            int absDistanceX = Math.abs(distanceX);
            int absDistanceY = Math.abs(distanceY);
            //Log.e("doScroll", "absDistanceX " + absDistanceX + " absDistanceY " + absDistanceY);
            // If the x distance is at least twice the y distance, then lock
            // the scroll horizontally.  Otherwise scroll vertically.
            if (absDistanceX > absDistanceY) {
                boolean edge = atHEdge(distanceX);
            	if (edge && ! isShiftPressed) {
	            	//Log.e("doScroll", "TOUCH_MODE_HSCROLL");
	                mTouchMode = TOUCH_MODE_HSCROLL;
	                mViewStartX = distanceX;
	                //initNextView(-mViewStartX);
	                // TODO: only invalidate if we reach edge
	                invalidate();
            	}
            }
        } else if ((mTouchMode & TOUCH_MODE_HSCROLL) != 0) {
        	
        	//Log.e("HScroll", "" + distanceX);
            // We are already scrolling horizontally, so check if we
            // changed the direction of scrolling 
            mViewStartX = distanceX;
            invalidate();
        } else if ((mTouchMode & TOUCH_MODE_VSCROLL) != 0 && mViewStartY < VERTICAL_SCROLL_THRESHOLD) {
        	//Log.e("doScroll", "V");
        	mViewStartY = (distanceY > VERTICAL_SCROLL_THRESHOLD)? VERTICAL_SCROLL_THRESHOLD: distanceY;
            invalidate();
        }
        
    }
    
    public void onDown(MotionEvent ev) {
        mTouchMode = TOUCH_MODE_DOWN;
        mViewStartX = 0;
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
    	canvas.save();
    	int x = 0, y = 0;
    	if ((mTouchMode & TOUCH_MODE_HSCROLL) != 0) {
    		x = -mViewStartX;
    	} else if ((mTouchMode & TOUCH_MODE_VSCROLL) != 0) {
    		y = -mViewStartY;
    	}
    	canvas.translate(x, y);
    	super.onDraw(canvas);
    	canvas.restore();
    	
        if ((mTouchMode & TOUCH_MODE_HSCROLL) != 0) {
        	// switch view start
        	Tab nextTab = null;
            if (mViewStartX > 0) {
                // if there is only 1 tab, show part of search box too?
            	nextTab  = mTabs.getRightTab();
            } else {
            	nextTab  = mTabs.getLeftTab();
            }
        	
        	if (nextTab != null) {
        		ScWebView nextView = nextTab.getWebView();
        		if (nextView != null) {
        			int move;
	            	if (mViewStartX > 0) {
	            		move = getRange(this) - mViewStartX;
	            	} else {
	            		move = -(getRange(nextView) + mViewStartX);
	            	}
	            	//Log.e("range","mViewStartX " + mViewStartX + " move " + move);
	            	canvas.save();
		            canvas.translate(move, 0);
		            // Prevent infinite recursive calls to onDraw().
		            nextView.mTouchMode = TOUCH_MODE_INITIAL_STATE;
		            nextView.capturePicture().draw(canvas);
		            //nextView.onDrawPic(canvas, nextView);
		            canvas.restore();
        		}
        	}
        } else if (y != 0 && (mTouchMode & TOUCH_MODE_VSCROLL) != 0) {
        	/*
        	canvas.save();
        	Log.e("ymove", " " + (VERTICAL_SCROLL_THRESHOLD-mViewStartY));
            canvas.translate(0, VERTICAL_SCROLL_THRESHOLD-mViewStartY);
        	Drawable d = getContext().getResources().getDrawable(R.drawable.urlbar);
        	d.setBounds(0, 0, getWidth(), -VERTICAL_SCROLL_THRESHOLD);
        	d.draw(canvas);
            canvas.restore();
            */
        }
    }
    
    private int getRange(ScWebView w) {
		int width = w.getWidth();
		int range = w.computeHorizontalScrollRange();
        if (range <= width) {
        	range = width;
        }
        return range;
    }
    // use mViewStartX
    private boolean atHEdge(int xMove) {
        // computeHorizontalScrollRange returns how big is the screen
        // if it is bigger than getWidth() then we should detect edge
        // protected int mScrollX; so we could compare this to see.
    	int range =  computeHorizontalScrollRange();
    	int width = getWidth();
    	if (range <= width)
    		return true;
    	int scroll= getScrollX();
    	//Log.e("atEdge","range " + range + " width " + width + " xMove " + xMove + " mScrollx " + scroll);
    	if (xMove < 0) {
    		// scroll left
    		//int move = (scroll > mViewStartX) : scroll? mViewStartX;
    		return scroll == 0;
     	  
    	} else {
    		// scroll right
    		return range <= (width + scroll); 
    	}
    }
    
    /*private boolean atVEdge() {
    	return this.getScrollY() == 0;
    }*/
    
   	public boolean onTouchEvent(MotionEvent ev) {
   		//if this is multi touch, allow it
   		if (Constant.sdk >= 5) {
   		  if (Api5.getPointerCount(ev) >= 2) {
   			boolean r = false;
            try {
            	r = super.onTouchEvent(ev);
            } catch (Exception e) {
            	
            }
            return r;
   		  }
   		}
   		if (isShiftPressed) {
   			if (ev.getAction() == MotionEvent.ACTION_UP) {
   				//this is a trick, after we set "select text" state, whenever we 
   				//receive a action up event, we cancel this state
   				isShiftPressed = false;
   			}
   			return super.onTouchEvent(ev);
   		}
        int action = ev.getAction();
        //Log.e("view", "" + action);
        switch (action) {
        case MotionEvent.ACTION_DOWN:
            mGestureDetector.onTouchEvent(ev);
            return super.onTouchEvent(ev);

        case MotionEvent.ACTION_MOVE:
            mGestureDetector.onTouchEvent(ev);
            boolean r = false;
            try {
            	r = super.onTouchEvent(ev);
            } catch (Exception e) {
            	
            }
            return r;

        case MotionEvent.ACTION_UP:
            mGestureDetector.onTouchEvent(ev);
            //if (mOnFlingCalled) {
            //    return true;
            //}
            if ((mTouchMode & TOUCH_MODE_HSCROLL) != 0) {
                mTouchMode = TOUCH_MODE_INITIAL_STATE;
                boolean right = mViewStartX > 0;
                if (Math.abs(mViewStartX) > HORIZONTAL_SCROLL_THRESHOLD) {
                    // The user has gone beyond the threshold so switch views
                    mTabs.switchViews(right);
                    //return true;
                } else {
                	invalidate();
                }
                mViewStartX = 0;
            } else if ((mTouchMode & TOUCH_MODE_VSCROLL) != 0) {
                mTouchMode = TOUCH_MODE_INITIAL_STATE;
                if (mViewStartY <= VERTICAL_SCROLL_THRESHOLD) {
                    // The user has gone beyond the threshold so switch views
                	// TODO add search
                    //mTabs.mActivity.startSearch(null, false,null, false);
                    mViewStartY = 0;
                    //return true;
                } else {
                	mViewStartY = 0;
                	invalidate();
                }
                
            }
            return super.onTouchEvent(ev);

        // This case isn't expected to happen.
        case MotionEvent.ACTION_CANCEL:
            mGestureDetector.onTouchEvent(ev);
            //mScrolling = false;
            //resetSelectedHour();
            return super.onTouchEvent(ev);

        default:
            return super.onTouchEvent(ev);
        }
    }

   	//this indicates that the webview is on "select text" state
   	private boolean isShiftPressed = false;
   	
   	public void emulateShift() {
   		isShiftPressed = true;
   		//this is hiden before sdk 2.2, but still works on device 1.5-2.1
   		emulateShiftHeld();
   	}
   	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (!isShiftPressed && (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT)) {
			isShiftPressed = true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (isShiftPressed && (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT)) {
			isShiftPressed = false;
		}
		return super.onKeyUp(keyCode, event);
	}
   	
   	
}
