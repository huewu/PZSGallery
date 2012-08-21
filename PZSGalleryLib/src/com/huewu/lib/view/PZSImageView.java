package com.huewu.lib.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

public class PZSImageView extends ImageView {

	private static final String TAG = "GalleryImageView";
	private static final float MIN_SCALE_FACTOR = 0.5f;
	private static final float MAX_SCALE_FACTOR = 2.f;
	
	Matrix mCurrentMatrix = new Matrix();
	Matrix mSavedMatrix = new Matrix();

	// We can be in one of these 3 states
	static final int NONE = 0;
	static final int DRAG = 1;
	static final int ZOOM = 2;
	private static final long DOUBLE_TAB_MARGIN = 200;
	int mMode = NONE;

	// Remember some things for zooming
	PointF mStartPoint = new PointF();
	PointF mMidPoint = new PointF();
	float mOldDist = 1f;
	private boolean mIsFirstDraw = true;
	private int mImageWidth;
	private int mImageHeight;
	private long mLastTocuhDownTime;

	public PZSImageView(Context context) {
		super(context);
		init();
	}

	public PZSImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public PZSImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		setScaleType(ScaleType.MATRIX);
		Matrix mat = getImageMatrix();
		mat.reset();
		setImageMatrix(mat);
	}
	
	@Override
	public void setImageBitmap(Bitmap bm) {
		super.setImageBitmap(bm);

		mIsFirstDraw = true;
		mImageWidth = bm.getWidth();
		mImageHeight = bm.getHeight();
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		
		if( mIsFirstDraw  == true ){
			mIsFirstDraw = false;
			fitCenter();
		}
		
		setImageMatrix(mCurrentMatrix);
		canvas.drawRGB(200, 0, 0);
		
		super.onDraw(canvas);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		// Handle touch events here...
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			long downTime = event.getDownTime();
			Log.d(TAG, "TouchTime diff: " + (downTime - mLastTocuhDownTime));
			if( downTime - mLastTocuhDownTime < DOUBLE_TAB_MARGIN ){
				//double tab!
				fitCenter();
				mMode = NONE;
			}else{
				mSavedMatrix.set(mCurrentMatrix);
				mStartPoint.set(event.getX(), event.getY());
				Log.d(TAG, "mode=DRAG");
				mMode = DRAG;
			}
			mLastTocuhDownTime = downTime;
			break;
		case MotionEvent.ACTION_POINTER_DOWN:
			mOldDist = spacing(event);
			Log.d(TAG, "oldDist=" + mOldDist);
			if (mOldDist > 3f) {
				mSavedMatrix.set(mCurrentMatrix);
				midPoint(mMidPoint, event);
				mMode = ZOOM;
				Log.d(TAG, "mode=ZOOM");
			}
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
			if( event.getPointerCount() == 2){
				//remained active point cout is 1.
				Log.d(TAG, "mode=DRAG");
				int activeIndex = (event.getActionIndex() == 0 ? 1 : 0);
				mStartPoint.set(event.getX(activeIndex), event.getY(activeIndex));
				mSavedMatrix.set(mCurrentMatrix);
				mMode = DRAG;
			}else{
				mMode = NONE;
				Log.d(TAG, "mode=NONE");
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if (mMode == DRAG) {
				// ...
				mCurrentMatrix.set(mSavedMatrix);
				mCurrentMatrix.postTranslate(event.getX() - mStartPoint.x,
						event.getY() - mStartPoint.y);
			}
			else if (mMode == ZOOM) {
				float newDist = spacing(event);
				Log.d(TAG, "newDist=" + newDist);
				if (newDist > 2f) {
					mCurrentMatrix.set(mSavedMatrix);
					float scale = newDist / mOldDist;
					mCurrentMatrix.postScale(scale, scale, mMidPoint.x, mMidPoint.y);
				}
			}
			break;
		}

		setImageMatrix(mCurrentMatrix);
		return true; // indicate event was handled
	}
	
	private void fitCenter(){
		//move image to center....
		mCurrentMatrix.reset();
		
		float scaleX = (getWidth() - getPaddingLeft() - getPaddingRight()) / (float)mImageWidth;
		float scaleY = (getHeight() - getPaddingTop() - getPaddingBottom()) / (float)mImageHeight;
		float scale = Math.min(scaleX, scaleY);
		
		float dx = (getWidth() - getPaddingLeft() - getPaddingRight() - mImageWidth * scale) / 2.f;
		float dy = (getHeight() - getPaddingTop() - getPaddingBottom() - mImageHeight * scale) / 2.f;
		
		mCurrentMatrix.postScale(scale, scale);
		mCurrentMatrix.postTranslate(dx, dy);
		
	}
	
	/** Determine the space between the first two fingers */
	private float spacing(MotionEvent event) {
		// ...
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return FloatMath.sqrt(x * x + y * y);
	}

	/** Calculate the mid point of the first two fingers */
	private void midPoint(PointF point, MotionEvent event) {
		// ...
		float x = event.getX(0) + event.getX(1);
		float y = event.getY(0) + event.getY(1);
		point.set(x / 2, y / 2);
	}

}//end of class
