package com.example.spotornot;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

public class MapActivity extends Activity implements OnMarkerClickListener, OnClickListener {

	private GoogleMap map;
	private Marker lot_21, lot_22;
	private static LatLng UMBC_LAT_LONG, LOT_22_LATLNG;
	private Polygon parkingLot;
	private MarkerWindowLayout markerInfoWindow;
	private PopupWindow lotPopupWindow;
	private LinearLayout lotWindowLayout;
	private Button checkInButton;
	private TextView checkInLabel;
	private Integer currentLotId;
	private View googleMapView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);
		googleMapView = findViewById(R.id.map);
		
		// UMBC LatLngs
		UMBC_LAT_LONG = new LatLng(39.255622, -76.711143);
		LOT_22_LATLNG = new LatLng(39.257183, -76.717921);
		
		// Instantiate GoogleMap object
		map = ((MapFragment)getFragmentManager().findFragmentById(R.id.map)).getMap();
		map.animateCamera(CameraUpdateFactory.newLatLngZoom(UMBC_LAT_LONG, 16), 1000, null);
		map.setOnMarkerClickListener(this);
		
		//lot_22 = map.addMarker(new MarkerOptions ().position(LOT_22_LATLNG)
		//		.title("22")
		//		.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
		//);
		
		ParkingMapLoad loadParkingMap = new ParkingMapLoad();
		String parkingMapResponse = "";
		
		
		try {
			parkingMapResponse = loadParkingMap.execute().get();
			Log.i("MyApp", "Map Response: " + parkingMapResponse);
			if (!parkingMapResponse.isEmpty()) {
				JSONArray parkingMap = new JSONArray(parkingMapResponse);
				for (int i = 0; i < parkingMap.length(); i++) {
					JSONObject lot = (JSONObject) parkingMap.get(i);
					map.addMarker(new MarkerOptions ().position(new LatLng(lot.getDouble("latitude"), lot.getDouble("longitude")))
							.title(String.valueOf(lot.getInt("lot_id")))
							.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
					);
				}
			}
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ExecutionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Instantiate AsyncTask object used to update the parking stats during onCreate
		/*ParkingStatUpdate updateParkingStats = new ParkingStatUpdate();
		String parkingStatResponse = "";
		
	
		try {
			parkingStatResponse = updateParkingStats.execute().get();
			Log.i("MyApp", "Response: " + parkingStatResponse);
			if (!parkingStatResponse.isEmpty()) {
				JSONObject parkingStats = new JSONObject(parkingStatResponse);
				Log.i("MyApp", "Response: " + parkingStats.toString());
				// Iterate over all parking lots and update their stats
				//for (int i = 0; i < userLocations.length(); i++) {
				//	lot_21.setSnippet("Lot 21: " + parkingStats.get("spots_filled").toString() + " filled.");
				//}
			}
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		
		lotWindowLayout = new LinearLayout(this);
		
		checkInLabel = new TextView(this);
		checkInLabel.setText("Check in to a parking spot");
		checkInButton = new Button(this);
		checkInButton.setText("Check In");
		checkInButton.setOnClickListener(this);
		
		lotWindowLayout.addView(checkInLabel);
		lotWindowLayout.addView(checkInButton);
		
		/*
		parkingLot = map.addPolygon(new PolygonOptions()
			.add(new LatLng(39.255622, -76.711143), new LatLng(39.255622, -76.72), new LatLng(39.26, -76.72), new LatLng(39.26, -76.711143))
			.strokeColor(Color.BLUE)
			.fillColor(Color.RED)
		);
		*/
	}

	private static class ParkingMapLoad extends AsyncTask<Void, Long, String>{

		@Override
		protected String doInBackground(Void... params) {
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost("http://mpss.csce.uark.edu/~chad-david/SpotOrNot/load_parking_map.php");

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
	
	private static class ParkingStatUpdate extends AsyncTask<Void, Long, String>{

		@Override
		protected String doInBackground(Void... params) {
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost("http://mpss.csce.uark.edu/~chad-david/SpotOrNot/get_current_stats.php");

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
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.map, menu);
		return true;
	}
	
	private static class CheckIn extends AsyncTask<String, Long, String>{

		@Override
		protected String doInBackground(String... arg0) {
			String [] lotParams = arg0;
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost("http://mpss.csce.uark.edu/~chad-david/SpotOrNot/update_parking_stats.php");
			List<NameValuePair> pairs = new ArrayList<NameValuePair>();
			pairs.add((NameValuePair) new BasicNameValuePair("lot_id", lotParams[0]));
			
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

	@Override
	public boolean onMarkerClick(Marker arg0) {
		// Set a local variable to the lot_id of the current lot's marker that was clicked 
		currentLotId = Integer.valueOf(arg0.getTitle());
		// Instantiate and display the image popup window
		lotPopupWindow = new PopupWindow(lotWindowLayout, 600, 900, true);
		//lotPopupWindow.setOnDismissListener(this);
		lotPopupWindow.showAtLocation(googleMapView, Gravity.CENTER, 0, 0);
		
		Log.i("MyApp", "Setting current lot id: " + currentLotId);
		return true;
	}

	@Override
	public void onClick(View v) {
		Log.i("MyApp", "Current Lot ID: " + currentLotId);
		
		String [] params = new String [1];
		params[0] = currentLotId.toString();
		
		// Instantiate AsyncTask object used for checking into a parking spot
		CheckIn checkInObj = new CheckIn();
		
		try {
			Log.i("MyApp", "Current Lot ID: " + currentLotId);
			String checkInResponse = checkInObj.execute(params).get();
			Log.i("MyApp", "Check in response: " + checkInResponse);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
