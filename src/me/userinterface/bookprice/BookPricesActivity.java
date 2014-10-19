package me.userinterface.bookprice;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import me.userinterface.bookprice.utils.BookAdapter;
import me.userinterface.bookprice.utils.BookItem;

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
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import com.devspark.appmsg.AppMsg;

public class BookPricesActivity extends Activity {

	// Adapter for book price list
	BookAdapter bookAdapter;
	ListView priceList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_book_price_list);

		bookAdapter = new BookAdapter(this);
		// Get handle to the list view
		priceList = (ListView) findViewById(R.id.book_price_list);
		priceList.setAdapter(bookAdapter);

		Bundle b = getIntent().getExtras();
		String isbn = "";
		if (b != null) {
			isbn = b.getString("isbn");
		}

		if (isbn != "")
			new FetchPriceData(isbn).execute();
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
			pDialog = ProgressDialog.show(BookPricesActivity.this, null,
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
					bookAdapter.clearList();
					JSONObject mainObject = new JSONObject(resultJSON);
					if (mainObject != null && mainObject.has("data")) {
						JSONArray data = mainObject.getJSONArray("data");
						BookItem bi;
						for (int i = 0; i < data.length(); i++) {
							bi = new BookItem();
							JSONObject obj = data.getJSONObject(i);
							if (obj != null && obj.has("title")) {
								bi.setTitle(obj.getString("title"));
							}
							if (obj != null && obj.has("url")) {
								bi.setUrl(obj.getString("url"));
							}
							if (obj != null && obj.has("source")) {
								bi.setSource(obj.getString("source"));
							}
							if (obj != null && obj.has("price")) {
								bi.setPrice((float) obj.getDouble("price"));
							}
							if (obj != null && obj.has("thumbnail")) {
								bi.setThumbnailUrl(obj.getString("thumbnail"));
							}
							bookAdapter.addItem(bi);
						}
					}
				} catch (JSONException ex) {
					ex.printStackTrace();
				}
				bookAdapter.notifyDataSetChanged();
			} else {
				AppMsg.makeText(
						BookPricesActivity.this,
						"Sorry, could not fetch prices at the moment. Please try again later.",
						AppMsg.STYLE_ALERT).show();
			}
		}
	}
}
