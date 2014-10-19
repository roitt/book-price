package me.userinterface.bookprice;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.HttpResponse;
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.devspark.appmsg.AppMsg;
import com.squareup.picasso.Picasso;

public class ShowBookActivity extends Activity {

	private ImageView thumbnailIV;
	private TextView titleTV;
	private TextView authorsTV;
	private TextView publisherTV;
	private TextView descriptionTV;
	private Button compareBTN;

	@SuppressWarnings("unused")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_book_item_show);

		Bundle extras = getIntent().getExtras();
		if (extras == null)
			return;

		final String id = extras.getString("id");
		final String isbn10 = extras.getString("isbn10");
		final String isbn13 = extras.getString("isbn13");
		final String title = extras.getString("title");
		final String subTitle = extras.getString("subtitle");
		final String authors = extras.getString("authors");
		final String description = extras.getString("description");
		final String publisher = extras.getString("publisher");
		final String pubDate = extras.getString("publisheddate");
		final String thumbLink = extras.getString("thumblink");
		final String smallThumbLink = extras.getString("smallThumblink");

		thumbnailIV = (ImageView) findViewById(R.id.book_cover);
		titleTV = (TextView) findViewById(R.id.book_title);
		authorsTV = (TextView) findViewById(R.id.book_author);
		publisherTV = (TextView) findViewById(R.id.book_pub_date);
		descriptionTV = (TextView) findViewById(R.id.book_description);

		// Lazy profile picture
		if (thumbLink != null && thumbLink.length() > 0 && thumbLink != "")
			Picasso.with(ShowBookActivity.this).load(thumbLink)
					.placeholder(R.drawable.ic_book_def)
					.error(R.drawable.ic_book_def).into(thumbnailIV);

		titleTV.setText(title);
		authorsTV.setText("By " + authors);
		descriptionTV.setText(description);
		publisherTV.setText(publisher + " " + pubDate);

		compareBTN = (Button) findViewById(R.id.compare_prices);
		compareBTN.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent i = new Intent(ShowBookActivity.this,
						BookPricesActivity.class);
				i.putExtra("isbn", isbn10);
				startActivity(i);
				// new FetchPriceData(isbn10).execute();
			}
		});
	}

	private class FetchPriceData extends AsyncTask<String, Void, String> {
		ProgressDialog pDialog;
		String base_address = "http://api.dataweave.in/v1/book_search/searchByIsbn/?";
		String api_key = "8a6bc2a929d699c8aa8ffabb3e932c22a1c45cee";
		String api_address = "";

		protected FetchPriceData(String isbn) {
			api_address = base_address + "api_key=" + api_key + "&isbn=" + isbn;
		}

		@Override
		protected void onPreExecute() {
			pDialog = ProgressDialog.show(ShowBookActivity.this, null,
					"Please wait while we give you the best prices.", true);
		}

		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub
			Log.v("do InBackground", "doInBackground");
			HttpClient httpClient = new DefaultHttpClient();
			HttpResponse response;
			String responseString = "";
			try {
				response = httpClient.execute(new HttpGet(api_address));
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				response.getEntity().writeTo(out);
				out.close();
				responseString = out.toString();
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				return "";
			} catch (IOException e) {
				e.printStackTrace();
				return "";
			}
			return responseString;
		}

		@Override
		protected void onPostExecute(String resultJSON) {
			pDialog.dismiss();
			if (resultJSON != "") {
				try {
					JSONObject mainObject = new JSONObject(resultJSON);
					if (mainObject != null && mainObject.has("data")) {
						JSONArray data = mainObject.getJSONArray("data");
						for (int i = 0; i < data.length(); i++) {

						}
					}
				} catch (JSONException ex) {
					ex.printStackTrace();
				}

			} else {
				AppMsg.makeText(
						ShowBookActivity.this,
						"Sorry, could not fetch prices at the moment. Please try again later.",
						AppMsg.STYLE_ALERT).show();
			}
		}
	}
}
