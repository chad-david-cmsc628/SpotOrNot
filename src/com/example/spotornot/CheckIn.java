package com.example.spotornot;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.os.AsyncTask;

// This class extends AsyncTask and is used to make an HTTP POST request to check a user into a parking space
public class CheckIn extends AsyncTask<String, Long, String>{

	@Override
	protected String doInBackground(String... arg0) {
		String [] lotParams = arg0;
		HttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost("http://mpss.csce.uark.edu/~chad-david/SpotOrNot/check_in.php");
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		pairs.add((NameValuePair) new BasicNameValuePair("lot_id", lotParams[0]));
		pairs.add((NameValuePair) new BasicNameValuePair("user_id", lotParams[1]));
		
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