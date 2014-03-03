package com.example.spotornot;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

// This activity is used to handle user login for the app
public class LoginActivity extends Activity implements OnFocusChangeListener, OnClickListener, Runnable {

	// Declare variables
	private ImageView logo;
	private EditText usernameField, passwordField;
	private Button loginButton;
	private InputMethodManager inputManager;
	private TextView errorMessage;
	private final String missingCredentialsMsg = "Please enter both your username and password.";
	private final String invalidCredentialsMsg = "Invalid username and password combination.";
	private int loggedInUserId;
	private String loggedInUserName = "";
	private String loggedInUserZone = "";
	private int checkedInLotId;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		
		logo = (ImageView) findViewById(R.id.logo); 
		logo.setImageResource(R.raw.spotornot_logo);
		
		usernameField = (EditText) findViewById(R.id.usernameField);
		passwordField = (EditText) findViewById(R.id.passwordField);
		
		usernameField.setBackgroundResource(R.raw.input_field);
		passwordField.setBackgroundResource(R.raw.input_field);
		
		usernameField.setOnFocusChangeListener(this);
		passwordField.setOnFocusChangeListener(this);
		
		loginButton = (Button) findViewById(R.id.loginButton);
		loginButton.setBackgroundResource(R.raw.login);
		loginButton.setOnClickListener(this);
		
		errorMessage = (TextView) findViewById(R.id.errorMessage);
		
		inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.map, menu);
		return true;
	}

	// Callback that executes code to hide the keyboard or clear the error message text
	@Override
	public void onFocusChange(View arg0, boolean arg1) {
		if (arg1 == false) {
			inputManager.hideSoftInputFromWindow(arg0.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
		} else if (arg1 == true && errorMessage.getText().equals(missingCredentialsMsg)) {
			errorMessage.setText("");
		} else if (arg1 == true && errorMessage.getText().equals(invalidCredentialsMsg)) {
			errorMessage.setText("");
		}
	}

	// Performs all the necessary actions for when a user clicks the login button
	@Override
	public void onClick(View arg0) {
		String [] params = new String [2];
		params[0] = usernameField.getText().toString();
		params[1] = passwordField.getText().toString();
		
		// Make sure both username and password fields are filled in before attempting a login
		if (!params[0].equals("") && !params[1].equals("")) {
			errorMessage.setText("");
			try {
				String loginResponse = new LoginAuthentication().execute(params).get();
				
				if (loginResponse.equals("0")) {
					errorMessage.setText(invalidCredentialsMsg);
				}
				else if (!loginResponse.isEmpty()) {
					JSONObject loggedInUser = new JSONObject(loginResponse);
					try {
						// Set the current logged-in user's info, which will be passed to the map activity
						loggedInUserId = loggedInUser.getInt("user_id");
						loggedInUserName = loggedInUser.getString("user_name");
						loggedInUserZone = loggedInUser.getString("zone_letter");
						if (!loggedInUser.getString("current_lot").equals("null")) {
							checkedInLotId = Integer.valueOf(loggedInUser.getString("current_lot"));
						} else {
							checkedInLotId = 0;
						}
						
						if (loggedInUserId > 0) {
							errorMessage.setText("Login success!");
							(new Thread(this)).start();
						}
					} catch (NumberFormatException e) {
						e.printStackTrace();
					} catch (ArrayIndexOutOfBoundsException e) {
						e.printStackTrace();
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			errorMessage.setText(missingCredentialsMsg);
		}
	}
	
	// AsyncTask that makes an HTTP POST request to validate and login a user
	private static class LoginAuthentication extends AsyncTask<String, Long, String>{

		@Override
		protected String doInBackground(String... arg0) {
			String[] userCredentials = arg0;
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost("http://mpss.csce.uark.edu/~chad-david/SpotOrNot/login.php");
			List<NameValuePair> pairs = new ArrayList<NameValuePair>();
			pairs.add((NameValuePair) new BasicNameValuePair("username", userCredentials[0]));
			pairs.add((NameValuePair) new BasicNameValuePair("password", userCredentials[1]));

			try {
				post.setEntity(new UrlEncodedFormEntity(pairs));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String responseBody = "";
			try {
				responseBody = client.execute(post, responseHandler);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return responseBody;
		}	
          
	}

	// Launch the map activity in a new thread
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			Thread.sleep(2000);
			// Pass the userid of the currently-logged in user to the map activity
			Intent myIntent = new Intent(this, MapActivity.class);
			myIntent.putExtra("user_id", loggedInUserId);
			myIntent.putExtra("user_name", loggedInUserName);
			myIntent.putExtra("zone_letter", loggedInUserZone);
			myIntent.putExtra("checked_in_lot_id", checkedInLotId);
			startActivity(myIntent);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
