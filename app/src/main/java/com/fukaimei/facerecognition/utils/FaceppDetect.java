package com.fukaimei.facerecognition.utils;

import org.apache.http.HttpRequest;
import org.json.JSONObject;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;
import com.fukaimei.facerecognition.Constant;

import android.graphics.Bitmap;
import android.util.Log;

public class FaceppDetect {

	private BitmapByteUtil bitmapByteUtil = new BitmapByteUtil();
	public interface CallBack{
		void success(JSONObject result);
		void error(FaceppParseException exception);
	}
	
	public static void detect(final Bitmap bitmap,final CallBack callBack){
		new Thread(new Runnable() {
			public void run() {
				try {
					BitmapByteUtil bitmapByteUtil = new BitmapByteUtil();
					//request
					HttpRequests httpRequests = new HttpRequests(Constant.APP_KEY, Constant.APP_SECRET, true, true);
					
					Bitmap bmSmall = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight());
					PostParameters parameters = new PostParameters();
					parameters.setImg(bitmapByteUtil.bitmapToByte(bmSmall));
					JSONObject jsonObject = httpRequests.detectionDetect(parameters);
					
					Log.e("TAG", jsonObject.toString());
					if(callBack!=null){
						callBack.success(jsonObject);
					}
				} catch (FaceppParseException e) {
					e.printStackTrace();
					if(callBack!=null){
						callBack.error(e);
					}
				}
			}
		}).start();
	}
}
