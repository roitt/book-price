package me.userinterface.bookprice;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import me.userinterface.bookprice.utils.ConnectionInspector;
import me.userinterface.bookprice.utils.ZBarConstants;
import me.userinterface.bookprice.zbar.CameraPreview;
import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ZBarScannerActivity extends Activity implements
		Camera.PreviewCallback, ZBarConstants {

	private JSONObject jObj;
	private String googleBooksURL = "https://www.googleapis.com/books/v1/volumes?q=isbn:";

	private CameraPreview mPreview;
	private Camera mCamera;
	private ImageScanner mScanner;
	private Handler mAutoFocusHandler;
	private boolean mPreviewing = true;
	private ProgressDialog pDialog;

	private String caller = "";
	ConnectionInspector conIns;

	private FrameLayout preview;
	RelativeLayout cameraOverlay;
	ImageView centerMarker;
	TextView scanText;

	static {
		System.loadLibrary("iconv");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!isCameraAvailable()) {
			// Cancel request if there is no rear-facing camera.
			cancelRequest();
			return;
		}
		setContentView(R.layout.activity_scanner);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		mAutoFocusHandler = new Handler();

		// Create and configure the ImageScanner;
		setupScanner();

		scanText = (TextView) findViewById(R.id.ref_txt);
		centerMarker = (ImageView) findViewById(R.id.center_marker);
	}

	public void setupScanner() {
		mScanner = new ImageScanner();
		mScanner.setConfig(0, Config.X_DENSITY, 3);
		mScanner.setConfig(0, Config.Y_DENSITY, 3);

		int[] symbols = getIntent().getIntArrayExtra(SCAN_MODES);
		if (symbols != null) {
			mScanner.setConfig(Symbol.NONE, Config.ENABLE, 0);
			for (int symbol : symbols) {
				mScanner.setConfig(symbol, Config.ENABLE, 1);
			}
		}

		mPreview = new CameraPreview(this, this, autoFocusCB);

		preview = (FrameLayout) findViewById(R.id.camera_preview);
		preview.addView(mPreview);

		cameraOverlay = (RelativeLayout) findViewById(R.id.camera_overlay_layout);
		cameraOverlay.bringToFront();
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Open the default i.e. the first rear facing camera.
		mCamera = Camera.open();
		if (mCamera == null) {
			// Cancel request if mCamera is null.
			cancelRequest();
			return;
		}

		mPreview.setCamera(mCamera);
		mPreview.showSurfaceView();

		mPreviewing = true;
	}

	@Override
	protected void onPause() {
		super.onPause();

		// Because the Camera object is a shared resource, it's very
		// important to release it when the activity is paused.
		if (mCamera != null) {
			mPreview.setCamera(null);
			mCamera.cancelAutoFocus();
			mCamera.setPreviewCallback(null);
			mCamera.stopPreview();
			mCamera.release();

			// According to Jason Kuang on
			// http://stackoverflow.com/questions/6519120/how-to-recover-camera-preview-from-sleep,
			// there might be surface recreation problems when the device goes
			// to sleep. So lets just hide it and
			// recreate on resume
			mPreview.hideSurfaceView();

			mPreviewing = false;
			mCamera = null;
		}
		centerMarker.setImageResource(R.drawable.center_cam_marker);
		scanText.setText("Scanning.");
	}

	public boolean isCameraAvailable() {
		PackageManager pm = getPackageManager();
		return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
	}

	public void cancelRequest() {
		Intent dataIntent = new Intent();
		dataIntent.putExtra(ERROR_INFO, "Camera unavailable");
		setResult(Activity.RESULT_CANCELED, dataIntent);
		finish();
	}

	public void onPreviewFrame(byte[] data, Camera camera) {
		Camera.Parameters parameters = camera.getParameters();
		Camera.Size size = parameters.getPreviewSize();

		Image barcode = new Image(size.width, size.height, "Y800");
		barcode.setData(data);

		int result = mScanner.scanImage(barcode);

		if (result != 0) {
			mCamera.cancelAutoFocus();
			mCamera.setPreviewCallback(null);
			mCamera.stopPreview();
			mPreviewing = false;
			SymbolSet syms = mScanner.getResults();
			for (Symbol sym : syms) {
				String symData = sym.getData();
				if (!TextUtils.isEmpty(symData)) {
					centerMarker
							.setImageResource(R.drawable.center_cam_marker_ok);
					scanText.setText("Done");

					conIns = new ConnectionInspector(ZBarScannerActivity.this);
					if (conIns.isConnectingToInternet()) {
						pDialog = ProgressDialog.show(ZBarScannerActivity.this,
								null, "Fetching book details. Please wait.",
								true);
						if (caller.equals("show")) {
							FetchBookData fbd = new FetchBookData();
							fbd.execute(googleBooksURL + symData);
						}
					} else {
						Toast.makeText(ZBarScannerActivity.this,
								"Please connect to internet and try again.",
								Toast.LENGTH_SHORT).show();
					}
					break;
				} else {
					Toast.makeText(ZBarScannerActivity.this,
							"Failed to scan barcode.", Toast.LENGTH_LONG)
							.show();
				}
			}
		}
	}

	class FetchBookData extends AsyncTask<String, Void, String> {

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub
			HttpClient httpClient = new DefaultHttpClient();
			HttpResponse response;
			String responseString = "";
			try {
				response = httpClient.execute(new HttpGet(params[0]));
				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					response.getEntity().writeTo(out);
					out.close();
					responseString = out.toString();
				} else {
					response.getEntity().getContent().close();
					throw new IOException(statusLine.getReasonPhrase());
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return responseString;
		}

		@Override
		protected void onPostExecute(String resultJSON) {
			String title = "";
			String subtitle = "";
			String author = "";
			String publisher = "";
			String pubDate = "";
			String thumbLink = "";
			String smallThumbLink = "";
			String id = "";
			String isbn_10 = "";
			String isbn_13 = "";
			String desc = "";
			try {
				jObj = new JSONObject(resultJSON);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				JSONArray items = jObj.getJSONArray("items");

				title = "";
				subtitle = "";
				author = "";
				publisher = "";
				pubDate = "";
				thumbLink = "";
				id = "";
				isbn_10 = "";
				isbn_13 = "";
				desc = "";

				JSONObject item = items.getJSONObject(0);

				if (item.has("id")) {
					id = item.getString("id");
				}

				if (item.has("volumeInfo")) {
					JSONObject volumeInfo = item.getJSONObject("volumeInfo");
					if (volumeInfo.has("title"))
						title = volumeInfo.getString("title");
					if (volumeInfo.has("subtitle"))
						subtitle = volumeInfo.getString("subtitle");
					if (volumeInfo.has("description"))
						desc = volumeInfo.getString("description");
					if (volumeInfo.has("publisher"))
						publisher = volumeInfo.getString("publisher");
					if (volumeInfo.has("publishedDate"))
						pubDate = volumeInfo.getString("publishedDate");

					if (volumeInfo.has("authors")) {
						JSONArray authors = volumeInfo.getJSONArray("authors");
						author = authors.join(", ");
					}

					if (volumeInfo.has("industryIdentifiers")) {
						JSONArray industryIdentifiers = volumeInfo
								.getJSONArray("industryIdentifiers");
						for (int j = 0; j < industryIdentifiers.length(); j++) {
							JSONObject tempISBN = industryIdentifiers
									.getJSONObject(j);
							String type = "";
							if (tempISBN.has("type"))
								type = tempISBN.getString("type");
							String identifier = "";
							if (tempISBN.has("identifier"))
								identifier = tempISBN.getString("identifier");

							if (type.equals("ISBN_10")) {
								isbn_10 = identifier;
							} else if (type.equals("ISBN_13")) {
								isbn_13 = identifier;
							}
						}
					}

					if (volumeInfo.has("imageLinks")) {
						JSONObject imageLinks = volumeInfo
								.getJSONObject("imageLinks");

						if (imageLinks.has("smallThumbnail"))
							smallThumbLink = imageLinks
									.getString("smallThumbnail");
						if (imageLinks.has("thumbnail"))
							thumbLink = imageLinks.getString("thumbnail");
					}

					Intent iSell = new Intent(ZBarScannerActivity.this,
							ShowBookActivity.class);
					iSell.putExtra("id", id);
					iSell.putExtra("isbn10", isbn_10);
					iSell.putExtra("isbn13", isbn_13);
					iSell.putExtra("title", title);
					iSell.putExtra("subtitle", subtitle);
					iSell.putExtra("authors", author);
					iSell.putExtra("publisher", publisher);
					iSell.putExtra("publisheddate", pubDate);
					iSell.putExtra("description", desc);
					iSell.putExtra("thumblink", thumbLink);
					iSell.putExtra("smallThumblink", smallThumbLink);
					startActivity(iSell);

					centerMarker.setImageResource(R.drawable.center_cam_marker);
					scanText.setText("Scanning.");
					pDialog.dismiss();
				}

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private Runnable doAutoFocus = new Runnable() {
		public void run() {
			if (mCamera != null && mPreviewing) {
				mCamera.autoFocus(autoFocusCB);
			}
		}
	};

	// Mimic continuous auto-focusing
	Camera.AutoFocusCallback autoFocusCB = new Camera.AutoFocusCallback() {
		public void onAutoFocus(boolean success, Camera camera) {
			mAutoFocusHandler.postDelayed(doAutoFocus, 1000);
		}
	};
}
