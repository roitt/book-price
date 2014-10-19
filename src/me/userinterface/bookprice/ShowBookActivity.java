package me.userinterface.bookprice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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
			}
		});
	}
}
