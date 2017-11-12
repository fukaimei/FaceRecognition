package com.fukaimei.facerecognition;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.facepp.error.FaceppParseException;
import com.fukaimei.facerecognition.utils.FaceppDetect;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements OnClickListener {

	private ImageView mPhoto;
	private Button mDetect, mGetImg;
	private TextView mTip;
	private View mWaiting;

	private Canvas canvas;
	private Paint mPaint;
	private String mCurrentPhotoStr;
	private Bitmap mBitmapPhoto;
	private static final int PICK_CODE = 0x110;
	private static final int MSG_SUCCESS = 0x111;
	private static final int MSG_ERROR = 0x112;
	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
				case MSG_SUCCESS:
					mWaiting.setVisibility(View.GONE);
					JSONObject rs = (JSONObject) msg.obj;
					prepareResultBitmap(rs);

					mPhoto.setImageBitmap(mBitmapPhoto);

					break;
				case MSG_ERROR:
					mWaiting.setVisibility(View.GONE);
					String errorMsg = (String) msg.obj;
					if (TextUtils.isEmpty(errorMsg)) {
						mTip.setText("Error.");
					} else {
						mTip.setText(errorMsg);
					}
					break;
			}
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// 申请访问SD卡的动态权限
		sdPermissions();
		mPhoto = (ImageView) findViewById(R.id.id_photo);
		mDetect = (Button) findViewById(R.id.id_detect);
		mGetImg = (Button) findViewById(R.id.id_getImg);
		mTip = (TextView) findViewById(R.id.id_tip);
		mWaiting = findViewById(R.id.id_waiting);
		mDetect.setOnClickListener(this);
		mGetImg.setOnClickListener(this);
		mPaint = new Paint();
	}

	// 定义访问SD卡的动态权限
	private void sdPermissions() {
		if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{
					android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
		}
	}

	/**
	 * 重写onRequestPermissionsResult方法
	 * 获取动态权限请求的结果,再访问SD卡
	 */
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

		} else {
			Toast.makeText(this, "用户拒绝了权限", Toast.LENGTH_SHORT).show();
		}
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	private void prepareResultBitmap(JSONObject rs) {
		// TODO Auto-generated method stub
		Bitmap bitmap = Bitmap.createBitmap(mBitmapPhoto.getWidth(), mBitmapPhoto.getHeight(),
				mBitmapPhoto.getConfig());
		canvas = new Canvas(bitmap);
		canvas.drawBitmap(mBitmapPhoto, 0, 0, null);

		try {
			JSONArray faces = rs.getJSONArray("face");
			int faceCount = faces.length();
			mTip.setText("find " + faceCount);

			for (int i = 0; i < faceCount; i++) {
				// 拿到单独face对象
				JSONObject face = faces.getJSONObject(i);
				JSONObject positonObj = face.getJSONObject("position");
				// get face position
				float x = (float) positonObj.getJSONObject("center").getDouble("x");
				float y = (float) positonObj.getJSONObject("center").getDouble("y");
				float w = (float) positonObj.getDouble("width");
				float h = (float) positonObj.getDouble("height");

				x = x / 100 * bitmap.getWidth();
				y = y / 100 * bitmap.getHeight();
				w = w / 100 * bitmap.getWidth();
				h = h / 100 * bitmap.getHeight();

				mPaint.setColor(0xffffffff);
				mPaint.setStrokeWidth(3);
				// draw box
				canvas.drawLine(x - w / 2, y - h / 2, x - w / 2, y + h / 2, mPaint);// 左
				canvas.drawLine(x - w / 2, y + h / 2, x + w / 2, y + h / 2, mPaint);// 下
				canvas.drawLine(x + w / 2, y + h / 2, x + w / 2, y - h / 2, mPaint);// 右
				canvas.drawLine(x - w / 2, y - h / 2, x + w / 2, y - h / 2, mPaint);// 上

				// get age and gender
				int age = face.getJSONObject("attribute").getJSONObject("age").getInt("value");
				String gender = face.getJSONObject("attribute").getJSONObject("gender").getString("value");

				Bitmap ageBitmap = buildAgeBitmap(age, gender.equals("Male"));

				int ageWidth = ageBitmap.getWidth();
				int ageHeight = ageBitmap.getHeight();
				if(bitmap.getWidth()<=mPhoto.getWidth() && bitmap.getHeight()<=mPhoto.getHeight()){
					float ratio = Math.max(bitmap.getWidth() * 1.0f / mPhoto.getWidth(),
							bitmap.getHeight() * 1.0f / mPhoto.getHeight());

					ageBitmap = Bitmap.createScaledBitmap(ageBitmap, (int) (ageWidth * ratio),
							(int) (ageHeight * ratio), false);
					canvas.drawBitmap(ageBitmap, x - ageBitmap.getWidth() / 2, y - h / 2 - ageBitmap.getHeight(), null);
				}
				mBitmapPhoto = bitmap;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	};

	private Bitmap buildAgeBitmap(int age, boolean isMale) {
		TextView tv = (TextView) mWaiting.findViewById(R.id.id_age_and_gender);
		tv.setText(age + "");
		if (isMale) {
			tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.male), null, null, null);
		} else {
			tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.female), null, null, null);
		}
		tv.setDrawingCacheEnabled(true);
		Bitmap bitmap = Bitmap.createBitmap(tv.getDrawingCache());
		tv.destroyDrawingCache();
		return bitmap;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == PICK_CODE) {
			if (data != null) {
				Uri uri = data.getData();
				Cursor cursor = getContentResolver().query(uri, null, null, null, null);
				cursor.moveToFirst();

				int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
				mCurrentPhotoStr = cursor.getString(index);
				cursor.close();

				resizePhoto();
				mPhoto.setImageBitmap(mBitmapPhoto);
				mTip.setText("点击检测 ==>");
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void resizePhoto() {

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;

		BitmapFactory.decodeFile(mCurrentPhotoStr, options);

		double ratio = Math.max(options.outWidth * 1.0d / 1024f, options.outHeight * 1.0d / 1024f);
		options.inSampleSize = (int) Math.ceil(ratio);
		options.inJustDecodeBounds = false;

		mBitmapPhoto = BitmapFactory.decodeFile(mCurrentPhotoStr, options);
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
			case R.id.id_getImg:
				Intent intent = new Intent(Intent.ACTION_PICK);
				intent.setType("image/*");
				startActivityForResult(intent, PICK_CODE);
				break;

			case R.id.id_detect:
				if (mBitmapPhoto != null && !mBitmapPhoto.isRecycled()) {
					mWaiting.setVisibility(View.VISIBLE);
					FaceppDetect.detect(mBitmapPhoto, new FaceppDetect.CallBack() {
						@Override
						public void success(JSONObject result) {
							Message msg = mHandler.obtainMessage(MSG_SUCCESS);
							msg.obj = result;
							msg.sendToTarget();
						}

						@Override
						public void error(FaceppParseException exception) {

							Message msg = mHandler.obtainMessage(MSG_ERROR);
							msg.obj = exception.getErrorMessage();
							msg.sendToTarget();
						}
					});
				} else {
					Toast.makeText(MainActivity.this, "请选择您要检测的人脸图片", Toast.LENGTH_SHORT).show();
				}
				break;
		}
	}
}
