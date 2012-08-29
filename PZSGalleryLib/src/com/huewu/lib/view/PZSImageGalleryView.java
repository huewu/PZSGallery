package com.huewu.lib.view;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;

import com.huewu.libs.galleryimageview.R;

public class PZSImageGalleryView extends RelativeLayout implements OnTouchListener {
	
	//view + header + bottom.
	//should be able to modify.
	
	private static final String TAG = "PZSImageGalleryView";

	public PZSImageGalleryView(Context context) {
		super(context);
		init();
	}

	public PZSImageGalleryView(Context context, AttributeSet attrs) {
		super(context, attrs);
		//attrs.getAttributeResourceValue(namespace, attribute, defaultValue);
		//add custom attribute for header & footer & image bitmap.
		init();
	}

	public PZSImageGalleryView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	public void setImageBitmap(Bitmap bm){
		PZSImageView iv = mImageViewRef.get();
		if(iv != null)
			iv.setImageBitmap(bm);
	}
	
	private WeakReference<PZSImageView> mImageViewRef = null;
	private WeakReference<View> mHeaderRef = null;
	private WeakReference<View> mFooterRef = null;
	
	private void init() {
		//set up default view.
		LayoutInflater inflater = LayoutInflater.from(getContext());
		//inflater.inflate(R.layout.pzs_gallery, null);
		PZSImageView iv = new PZSImageView(getContext());
		iv.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		iv.setOnTouchListener(this);
		addView(iv);
		mImageViewRef = new WeakReference<PZSImageView>(iv);

		//addView(child);
		View top = (View) inflater.inflate(R.layout.pzs_header, null);
		addView(top, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT );
		mHeaderRef = new WeakReference<View>(top);
		
		View bottom = (View) inflater.inflate(R.layout.pzs_bottom, null);
		RelativeLayout.LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.addRule(ALIGN_PARENT_BOTTOM);
		addView(bottom, params);
		mFooterRef = new WeakReference<View>(bottom);
		
		
	}
	
	@Override
	protected void onWindowVisibilityChanged(int visibility) {
		super.onWindowVisibilityChanged(visibility);
		if( visibility == View.VISIBLE )
			mFooterAndHeaderAnumationHandler.sendEmptyMessage(
					FooterAndHeaderAnimationHander.START_SHOW_ANI);
	}
	
	private Handler mFooterAndHeaderAnumationHandler = new FooterAndHeaderAnimationHander(); 
	
	private class FooterAndHeaderAnimationHander extends Handler {
		
		private final static int START_HIDE_ANI = 100;
		private final static int START_SHOW_ANI = 101;
		private static final long AUTO_HIDE_DELAY = 2000;	//ms
		
		private boolean mIsShown = false;
		
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what){
			case START_HIDE_ANI:
				hideHeaderAndFooterBar();
				break;
			case START_SHOW_ANI:
				removeMessages(START_HIDE_ANI);
				showHeaderAndFooterBar();
				//hide automatically after 5 sec.
				sendEmptyMessageDelayed(START_HIDE_ANI, AUTO_HIDE_DELAY);
				break;
			}
		}
		
		private void hideHeaderAndFooterBar() {
			if( !mIsShown )
				return;

			//start animation.
			Animation ani1 = AnimationUtils.loadAnimation(getContext(), R.anim.slide_out_up);
			ani1.setFillAfter(true);
			Animation ani2 = AnimationUtils.loadAnimation(getContext(), R.anim.slide_out_down);
			ani2.setFillAfter(true);
			
			View v;
			v = mHeaderRef.get();
			if( v != null )
				v.startAnimation(ani1);
			v = mFooterRef.get();
			if( v != null )
				v.startAnimation(ani2);
			
			mIsShown = false;
		}

		private void showHeaderAndFooterBar() {
			if( mIsShown )
				return;
			
			//start animation.
			Animation ani1 = AnimationUtils.loadAnimation(getContext(), R.anim.slide_in_down);
			Animation ani2 = AnimationUtils.loadAnimation(getContext(), R.anim.slide_in_up);
			
			View v;
			v = mHeaderRef.get();
			if( v != null )
				v.startAnimation(ani1);
			v = mFooterRef.get();
			if( v != null )
				v.startAnimation(ani2);
			
			mIsShown = true;
		}		
	}

	@Override
	public boolean onTouch(View v, MotionEvent ev) {
		Log.v(TAG, "Touch gallery view!");
		
		switch (ev.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			if( ev.getPointerCount() == 1 ){
				Log.v(TAG, "Click gallery view!");
				mFooterAndHeaderAnumationHandler.sendEmptyMessage(FooterAndHeaderAnimationHander.START_SHOW_ANI);
				//start hide timer.
				//need a handler!
			}
		}
		return false;
	}
	
	//top header view
	//bottom nav view
	
}//end of class.
