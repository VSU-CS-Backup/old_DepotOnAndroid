/*
 * ShowSettings.java Author: Zhigaung Xu
 * zxu@valdosta.edu
 */

package com.depot.cs4900;

import java.io.FileOutputStream;
import java.util.HashMap;

import org.apache.http.client.ResponseHandler;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.depot.cs4900.data.CatalogList;
import com.depot.cs4900.network.*;


public class ShowSettings extends Activity {
	private static final String CLASSTAG = ShowSettings.class.getSimpleName();
	
	Prefs myprefs = null;
	AlertDialog.Builder adb;// = new AlertDialog.Builder(this);

	EditText serverurl;
	EditText user_name;
	EditText password;
	Button savebutton;

	private ProgressDialog progressDialog;
	// use a handler to update the UI (send the handler messages from other
	// threads)
	private final Handler handler = new Handler() {

		@Override
		public void handleMessage(final Message msg) {
			progressDialog.dismiss();
			String bundleResult = msg.getData().getString("RESPONSE");

			// Invalid login
			// Make sure the create action in the sessions controller on the
			// server is updated.
			if (bundleResult.startsWith("Invalid")
					|| bundleResult.startsWith("Error")) {
				ShowSettings.this.myprefs.setValid(false);
				Toast.makeText(ShowSettings.this, "Invalid. Log in again.",
						Toast.LENGTH_SHORT).show();
			} else { // A successful response to either a login or a logout request
				// save off values
				if (!ShowSettings.this.myprefs.isValid()){
					ShowSettings.this.myprefs.setServer(serverurl.getText()
							.toString());
					ShowSettings.this.myprefs.setUserName(user_name.getText()
							.toString());
					ShowSettings.this.myprefs.setPassword(password.getText()
							.toString());
					ShowSettings.this.myprefs.setValid(true);
					ShowSettings.this.myprefs.save();
					savebutton.setText("Log Out");

				}
				else{
					ShowSettings.this.myprefs.setServer(serverurl.getText()
							.toString());
					ShowSettings.this.myprefs.setUserName("Unknown");
					ShowSettings.this.myprefs.setPassword("Unknown");
					ShowSettings.this.myprefs.setValid(false);
					ShowSettings.this.myprefs.save();
					savebutton.setText("Log In");
				}
				// we're done!
				finish();	
			}
		}
	};

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		setContentView(R.layout.showsettings);

		this.myprefs = new Prefs(getApplicationContext());

		this.adb = new AlertDialog.Builder(this);

		savebutton = (Button) findViewById(R.id.set_button);

		// create anonymous click listener to handle the "save"
		savebutton.setOnClickListener(new Button.OnClickListener() {

			public void onClick(View v) {
				try {
					if (ShowSettings.this.savebutton.getText().toString()
							.equals("Log In")) { // Log in

						// get the string and do something with it.

						serverurl = (EditText) findViewById(R.id.server_url);
						if (serverurl.getText().length() == 0) {

							AlertDialog ad = ShowSettings.this.adb.create();
							ad.setMessage("Please Enter The URL of The Server");
							ad.show();
							return;
						}

						user_name = (EditText) findViewById(R.id.user_name);
						if (user_name.getText().length() == 0) {
							AlertDialog ad = ShowSettings.this.adb.create();
							ad.setMessage("Please Enter The User Name");
							ad.show();
							return;
						}

						password = (EditText) findViewById(R.id.password);
						if (password.getText().length() == 0) {
							AlertDialog ad = ShowSettings.this.adb.create();
							ad.setMessage("Please Enter The Password");
							ad.show();
							return;
						}

						performRequest(serverurl.getText().toString()
								+ "/login.xml", "POST", user_name.getText()
								.toString(), password.getText().toString());
					} else { // Log out
						serverurl = (EditText) findViewById(R.id.server_url);
						if (serverurl.getText().length() == 0) {

							AlertDialog ad = ShowSettings.this.adb.create();
							ad.setMessage("Please Enter The URL of The Server");
							ad.show();
							return;
						}
						performRequest(serverurl.getText().toString()
								+ "/logout.xml", "DELETE",null, null);
					}

				} catch (Exception e) {
					Log.i(ShowSettings.this.CLASSTAG, "Failed to Save Settings ["
							+ e.getMessage() + "]");
				}
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.v(Constants.LOGTAG, " " + ShowSettings.CLASSTAG + " onResume");

		// load screen
		PopulateScreen();
	}

	private void PopulateScreen() {
		try {
			final EditText serverurl = (EditText) findViewById(R.id.server_url);
			final EditText user_name = (EditText) findViewById(R.id.user_name);
			final EditText password = (EditText) findViewById(R.id.password);

			serverurl.setText(this.myprefs.getServer());
			user_name.setText(this.myprefs.getUserName());
			password.setText(this.myprefs.getPassword());

			if (this.myprefs.isValid())
				this.savebutton.setText("Log Out");
			else
				this.savebutton.setText("Log In");
		} catch (Exception e) {

		}
	}

	/**
	 * Perform asynchronous HTTP using Apache <code>HttpClient</code> via
	 * <code>HttpRequestHelper</code> and <code>ResponseHandler</code>.
	 * 
	 * @param url
	 * @param method
	 * @param user
	 * @param pass
	 */
	private void performRequest(final String url, final String method,
			final String user, final String pass) {

		Log.d(Constants.LOGTAG, " " + ShowSettings.CLASSTAG + " request url - "
				+ url);
		Log.d(Constants.LOGTAG, " " + ShowSettings.CLASSTAG
				+ " request method - " + method);
		Log.d(Constants.LOGTAG, " " + ShowSettings.CLASSTAG + " user - " + user);
		Log.d(Constants.LOGTAG, " " + ShowSettings.CLASSTAG + " password - "
				+ pass);

		final HashMap<String, String> params = new HashMap<String, String>();
		if ((user != null) && (pass != null)) {
			params.put("name", user);
			params.put("password", pass);
		}

		final ResponseHandler<String> responseHandler = HTTPRequestHelper
				.getResponseHandlerInstance(this.handler);

		this.progressDialog = ProgressDialog.show(this, "working . . .",
				"performing HTTP " + method + " request");

		// do the HTTP dance in a separate thread (the responseHandler will fire
		// when complete)
		new Thread() {

			@Override
			public void run() {
				HTTPRequestHelper helper = new HTTPRequestHelper(
						responseHandler);
				if (method.equals("POST")) {
					//helper.performPost(HTTPRequestHelper.MIME_TEXT_PLAIN, url,
					//		null, null, null, params);
					helper.performPost(HTTPRequestHelper.MIME_TEXT_PLAIN, url,
							user, pass, null, params);
				} else 
				if (method.equals("DELETE")){
					helper.performDelete(HTTPRequestHelper.MIME_TEXT_PLAIN, url, null, null, null, null);
				}
				else{
					Message msg = handler.obtainMessage();
					Bundle bundle = new Bundle();
					bundle.putString("RESPONSE", "ERROR - see logcat");
					msg.setData(bundle);
					handler.sendMessage(msg);
					Log.w(Constants.LOGTAG, " " + CLASSTAG
							+ " has to be a POST method");
				}
			}
		}.start();
	}
}
