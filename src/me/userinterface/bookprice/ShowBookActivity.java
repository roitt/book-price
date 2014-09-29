package me.userinterface.bookprice;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;

public class ShowBookActivity extends Activity {
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
	}
}
