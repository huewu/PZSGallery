package com.example.galleryimageviewsample;

import java.io.IOException;
import java.io.InputStream;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.widget.ImageView;

@SuppressLint("ParserError")
public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	InputStream is;
		try {
			is = getAssets().open("android2.jpg");
	    	Bitmap bm = BitmapFactory.decodeStream(is);
	        ImageView iv = (ImageView) findViewById(R.id.img);
	        iv.setImageBitmap(bm);
	    	is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    
}//end of class
