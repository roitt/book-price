package me.userinterface.bookprice.utils;

import java.text.DecimalFormat;
import java.util.ArrayList;

import me.userinterface.bookprice.R;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

public class BookAdapter extends BaseAdapter {
	private ArrayList<BookItem> _list = new ArrayList<BookItem>();
	Context _context;
	LayoutInflater vi;

	public BookAdapter(Context context) {
		_context = context;
		vi = (LayoutInflater) _context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public void addItem(final BookItem li) {
		_list.add(li);
	}

	public void clearList() {
		_list.clear();
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return _list.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return _list.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		View row = convertView;
		ViewHolder holder = null;
		if (row == null) {

			holder = new ViewHolder();
			row = vi.inflate(R.layout.book_price_item, parent, false);

			holder.thumb = (ImageView) row.findViewById(R.id.bpi_thumbnail);
			holder.title = (TextView) row.findViewById(R.id.bpi_title);
			holder.seller = (TextView) row.findViewById(R.id.bpi_seller);
			holder.price = (TextView) row.findViewById(R.id.bpi_price);
			row.setTag(holder);
		} else {
			holder = (ViewHolder) row.getTag();
		}

		// ensure no out of bounds exception
		if (_list.size() > position) {
			BookItem bookItem = _list.get(position);
			String thumbLink = bookItem.getThumbnailUrl();
			if (thumbLink != null && thumbLink.length() > 0 && thumbLink != "")
				Picasso.with(_context).load(thumbLink)
						.placeholder(R.drawable.ic_book_def)
						.error(R.drawable.ic_book_def).into(holder.thumb);
			holder.title.setText(bookItem.getTitle());
			if (bookItem.getSource() != null
					&& bookItem.getSource().length() != 0)
				holder.seller.setText("Sold by " + bookItem.getSource());
			float priceInDollars = (float) (bookItem.getPrice() / 61.28);
			DecimalFormat format = new DecimalFormat("#.##");
			holder.price.setText("$ " + format.format(priceInDollars));
		} else {
			Log.e("No items in adapter", Integer.toString(_list.size()));
		}

		return row;
	}

	public static class ViewHolder {
		ImageView thumb;
		TextView title;
		TextView seller;
		TextView price;
	}
}
