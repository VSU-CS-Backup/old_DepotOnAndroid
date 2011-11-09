package com.depot.cs4900;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;  
import java.util.Scanner;

import org.apache.http.client.ResponseHandler;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.ViewFlipper;
import android.widget.EditText;

import com.depot.cs4900.data.CatalogEntry;
import com.depot.cs4900.data.CatalogList;
import com.depot.cs4900.network.HTTPRequestHelper;

public class NewProduct extends Activity {
	private static final String CLASSTAG = NewProduct.class.getSimpleName();

	Prefs myprefs = null;

	private EditText title_text;
	private EditText desciption_text;
	private EditText price_text;

	private Button create_button;
	private Button cancel_button;

	private ProgressDialog progressDialog;

	private CatalogEntry product;
	private CatalogList catalog;

	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			Log.v(Constants.LOGTAG,
							" "
							+ NewProduct.CLASSTAG
							+ " create worker thread done.");
			progressDialog.dismiss();


			if (myprefs.getMode() == Constants.AUTO_SYNCH) {
				String bundleResult = msg.getData().getString("RESPONSE");

				// Pattern pattern =  Pattern.compile("<id type=\"integer\">\\d+<id>");
				Scanner s = new Scanner(bundleResult);
				int id = 0;
				while (s.hasNextLine()){
					String line = s.nextLine();
					if (line.contains("id type")){
						Scanner s1= new Scanner(line).useDelimiter("\\D+");

						id = s1.nextInt();
						break;
					}			
				}
				
				//Bundle b = msg.getData();
				//CatalogEntry ce = CatalogEntry.fromBundle(b);
				
				catalog = CatalogList.parse(NewProduct.this);
				catalog.delete(product);
				product.set_product_id(new Integer(id).toString());
				catalog.create(product);
				Log.v(Constants.LOGTAG,
						" "
						+ NewProduct.CLASSTAG + " "
						+ product + ", ~~~~~~~~~~~~~~~~~~~~~~~~~");

			}
			finish();
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.new_product);

		myprefs = new Prefs(getApplicationContext());

		product = new CatalogEntry();

		title_text = (EditText) findViewById(R.id.product_title);
		desciption_text = (EditText) findViewById(R.id.product_description);
		price_text = (EditText) findViewById(R.id.product_price);

		// update
		create_button = (Button) findViewById(R.id.product_new_button);
		create_button.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				createProdut();
			}
		});
		// cancel
		cancel_button = (Button) findViewById(R.id.product_cancel_button);
		cancel_button.setOnClickListener(new Button.OnClickListener() {

			public void onClick(View v) {
				finish();

			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.v(Constants.LOGTAG + ": " + NewProduct.CLASSTAG, " onResume");
		if (myprefs.getMode() == Constants.AUTO_SYNCH) {
			this.setTitle(this.CLASSTAG+ " - Online");
			if (myprefs.isValid())
				this.setTitle(this.getTitle() + ": " + myprefs.getUserName());
			else
				this.setTitle(this.getTitle() + ": Unknown User");
		} else
			this.setTitle(this.CLASSTAG + " - Offline");
	}

	private void createProdut() {

		Log.v(Constants.LOGTAG, " " + NewProduct.CLASSTAG + " updateProduct");

		this.progressDialog = ProgressDialog.show(this, " Working...",
				" Creating Product", true, false);
		
		// Get ready to send the HTTP PUT request to update the Product data on
		// the server
		
		final ResponseHandler<String> responseHandler = HTTPRequestHelper
				.getResponseHandlerInstance(this.handler);
		final HashMap<String, String> params = new HashMap<String, String>();
		if (!title_text.getText().toString().equals("")) {
			params.put("title", title_text.getText().toString());
		}
		if (!desciption_text.getText().toString().equals("")) {
			params.put("description", desciption_text.getText().toString());
		}
		if (!price_text.getText().toString().equals("")) {
			params.put("price", price_text.getText().toString());
		}
		params.put("image_url", "unknown.jpg");
		

		// Create a new product locally
		product.set_product_id("-1");
		product.set_title(title_text.getText().toString());
		product.set_description(desciption_text.getText().toString());
		product.set_price(price_text.getText().toString());
		product.set_popularity("0");
		catalog = CatalogList.parse(NewProduct.this);
		catalog.create(product);

		// update product on the server in a separate thread for
		// ProgressDialog/Handler
		// when complete send "empty" message to handler
		new Thread() {
			@Override
			public void run() {
				// networking stuff ...
				HTTPRequestHelper helper = new HTTPRequestHelper(
						responseHandler);
				if (myprefs.getMode() == Constants.AUTO_SYNCH && myprefs.isValid() && !myprefs.getUserName().equals("admin")){
					helper.performPost(HTTPRequestHelper.MIME_TEXT_PLAIN, myprefs.getServer() + "/products.xml", 
							null, null, null, params);
				}	
				else
					handler.sendEmptyMessage(0);
			}
		}.start();
	}
	
	
}
