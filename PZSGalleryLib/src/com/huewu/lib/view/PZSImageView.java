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

/**
 * class Pinch Zoom & Swipe Image View.
 * @author huewu.yang
 * @date 2012. 08. 23
 */
public class PZSImageView extends ImageView {

	private static final String TAG = "GalleryImageView";

	public static final int PZS_ACTION_INIT = 100;
	public static final int PZS_ACTION_SCALE = 1001;
	public static final int PZS_ACTION_TRANSLATE = 1002;
	public static final int PZS_ACTION_SCALE_TO_TRANSLATE = 1003;
	public static final int PZS_ACTION_TRANSLATE_TO_SCALE = 1004;	
	public static final int PZS_ACTION_FIT_CENTER = 1005;
	public static final int PZS_ACTION_CANCEL = -1;

	//TODO below 2 values should be able to set from attributes.
	private final static float MAX_SCALE_TO_SCREEN = 2.f;
	private final static float MIN_SCALE_TO_SCREEN = .5f;
	
	private float mMinScaleFactor = 0.5f;	//scale to screen width / height.
	private float mMaxScaleFactor = 2.f;	//scale to screen width / height.
	
	private static final long DOUBLE_TAP_MARGIN_TIME = 200;
	private static final float MIN_SCALE_SPAN = 10.f;

	private boolean mIsFirstDraw = true;
	private int mImageWidth;
	private int mImageHeight;

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
			calculateScaleFactorLimit();
		}

		setImageMatrix(mCurrentMatrix);
		canvas.drawRGB(200, 0, 0);

		super.onDraw(canvas);
	}

	private void calculateScaleFactorLimit() {
		
		//set max / min scale factor. 
		
		//max: double size of screen width or height.
		mMaxScaleFactor = Math.max( getHeight() * MAX_SCALE_TO_SCREEN / mImageHeight, 
				getWidth() * MAX_SCALE_TO_SCREEN / mImageWidth);
		
		//min: half size of screen width or height.
		mMinScaleFactor = Math.min( getHeight() * MIN_SCALE_TO_SCREEN / mImageHeight, 
				getWidth() * MIN_SCALE_TO_SCREEN / mImageWidth);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		int action = parseMotionEvent(event);

		switch(action){
		case PZS_ACTION_INIT:
			initGestureAction(event.getX(), event.getY());
			break;
		case PZS_ACTION_SCALE:
			handleScale(event);
			break;
		case PZS_ACTION_TRANSLATE:
			handleTranslate(event);
			break;
		case PZS_ACTION_TRANSLATE_TO_SCALE:
			initGestureAction(event.getX(), event.getY());
			break;
		case PZS_ACTION_SCALE_TO_TRANSLATE:
			int activeIndex = (event.getActionIndex() == 0 ? 1 : 0);
			initGestureAction(event.getX(activeIndex), event.getY(activeIndex));
			break;
		case PZS_ACTION_FIT_CENTER:
			fitCenter();
			initGestureAction(event.getX(), event.getY());
			break;
		case PZS_ACTION_CANCEL:
			break;
		}
		
		validateMatrix();
		updateMatrix();
		return true; // indicate event was handled
	}

	private int parseMotionEvent(MotionEvent ev) {

		switch (ev.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			if( isDoubleTap(ev) )
				return PZS_ACTION_FIT_CENTER;
			else
				return PZS_ACTION_INIT;
		case MotionEvent.ACTION_POINTER_DOWN:
			//more than one pointer is pressed...
			return PZS_ACTION_TRANSLATE_TO_SCALE;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
			if( ev.getPointerCount() == 2 ){
				return PZS_ACTION_SCALE_TO_TRANSLATE;
			}else{
				return PZS_ACTION_INIT;
			}
		case MotionEvent.ACTION_MOVE:
			if( ev.getPointerCount() == 1 )
				return PZS_ACTION_TRANSLATE;
			else if( ev.getPointerCount() == 2 )
				return PZS_ACTION_SCALE;
			return 0;
		}
		return 0;
	}

	/*
	 * protected methods.
	 */

	private Matrix mCurrentMatrix = new Matrix();
	private Matrix mSavedMatrix = new Matrix();

	// Remember some things for zooming
	private PointF mStartPoint = new PointF();
	private PointF mMidPoint = new PointF();
	private float mInitScaleSpan = 1f;

	/**
	 * check user double tapped this view.. or not.
	 * @param current motion event.
	 * @return true if user double tapped this view.
	 */
	private long mLastTocuhDownTime = 0;
	protected boolean isDoubleTap(MotionEvent ev){
		//if old pointer is tapped?
		if( ev.getPointerCount() > 1){ 
			//if there are more than one pointer... reset
			mLastTocuhDownTime = 0;
			return false;
		}

		long downTime = ev.getDownTime();
		long diff = downTime - mLastTocuhDownTime; 
		mLastTocuhDownTime = downTime;

		return diff < DOUBLE_TAP_MARGIN_TIME;
	}

	protected void initGestureAction(float x, float y) {
		mSavedMatrix.set(mCurrentMatrix);
		mStartPoint.set(x, y);
		mInitScaleSpan = 0.f;
	}

	protected void handleScale(MotionEvent event){
		float newSpan = spacing(event);

		//if two finger is too close, pointer index is bumped.. so just ignore it.
		if( newSpan < MIN_SCALE_SPAN )
			return;

		if( mInitScaleSpan == 0.f ){
			//init values. scale gesture action is just started.
			mInitScaleSpan = newSpan;
			midPoint(mMidPoint, event);
		}else{
			float scale = normalizeScaleFactor(mSavedMatrix, newSpan, mInitScaleSpan);
			mCurrentMatrix.set(mSavedMatrix);
			mCurrentMatrix.postScale(scale, scale, mMidPoint.x, mMidPoint.y);
		}
	}

	private float normalizeScaleFactor(Matrix curMat, float newSpan, float stdSpan) {
		
		float values[] = new float[9];
		curMat.getValues(values);
		float scale = values[Matrix.MSCALE_X];
			
		if( stdSpan == newSpan ){
			return scale;
		} else {
			float newScaleFactor = newSpan / stdSpan;
			float candinateScale = scale * newScaleFactor; 
			
			if( candinateScale > mMaxScaleFactor ){
				return mMaxScaleFactor / scale;
			}else if( candinateScale < mMinScaleFactor ){
				return mMinScaleFactor / scale;
			}else{
				return newScaleFactor;
			}
		}
	}
	
	protected void validateMatrix(){
		float values[] = new float[9];
		mCurrentMatrix.getValues(values);
		
		//get current matrix values.
		float scale = values[Matrix.MSCALE_X];
		float tranX = values[Matrix.MTRANS_X];
		float tranY = values[Matrix.MTRANS_Y];
				
		int imageHeight = (int) (scale * mImageHeight);
		int imageWidth = (int) (scale * mImageWidth);
		
		//max x pos.
		//min x pos.
		
		float minX = 0.f;
		float maxX = 0.f;
		float minY = 0.f;
		float maxY = 0.f;
		
		//don't think about optimize code. first, just write case by case.

		//check TOP & BOTTOM
		if( imageHeight > getHeight() ){
			//image height is taller than view
			//MIN Y
			minY = getHeight() - imageHeight - getPaddingTop() * 2.f;
			//MAX Y
			maxY = 0.f;
		}else{
			minY = 0.f;
			minY = maxY = (getHeight() - imageHeight - getPaddingTop() - getPaddingBottom() ) / 2.f;
		}

		//check LEFT & RIGHT
		if( imageWidth > getWidth() ){
			//image width is longer than view
			//MIN X
			minX = getWidth() - imageWidth - getPaddingRight() * 2.f;
			//MAX X
			maxX = 0.f;
		}else{
			//minX = 0.f;
			minX = maxX = (getWidth() - imageWidth - getPaddingLeft() - getPaddingRight()) / 2.f; //s * 2.f;
		}
		
		if(tranX < minX)
			tranX = minX;
		else if(tranX > maxX)
			tranX = maxX;
		
		if(tranY < minY)
			tranY = minY;
		else if(tranY > maxY)
			tranY = maxY;
		
		values[Matrix.MTRANS_X] = tranX;
		values[Matrix.MTRANS_Y] = tranY;
		mCurrentMatrix.setValues(values);		
	}
	
	protected void updateMatrix(){
		setImageMatrix(mCurrentMatrix);
	}

	protected void handleTranslate(MotionEvent event){
		mCurrentMatrix.set(mSavedMatrix);
		mCurrentMatrix.postTranslate(event.getX() - mStartPoint.x, event.getY() - mStartPoint.y);
	}

	protected void fitCenter(){
		//move image to center....
		mCurrentMatrix.reset();

		float scaleX = (getWidth() - getPaddingLeft() - getPaddingRight()) / (float)mImageWidth;
		float scaleY = (getHeight() - getPaddingTop() - getPaddingBottom()) / (float)mImageHeight;
		float scale = Math.min(scaleX, scaleY);

		float dx = (getWidth() - getPaddingLeft() - getPaddingRight() - mImageWidth * scale) / 2.f;
		float dy = (getHeight() - getPaddingTop() - getPaddingBottom() - mImageHeight * scale) / 2.f;

		mCurrentMatrix.postScale(scale, scale);
		mCurrentMatrix.postTranslate(dx, dy);
		setImageMatrix(mCurrentMatrix);
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
