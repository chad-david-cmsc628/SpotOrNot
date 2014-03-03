package com.example.spotornot;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
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
import android.os.Build;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;

// This activity is used to create the interactive GoogleMap that will serve as the main interaction point of the application
public class MapActivity extends Activity implements OnMarkerClickListener, OnDismissListener, OnClickListener {

	// Declare variables
	private GoogleMap map;
	private static LatLng UMBC_LAT_LONG, UMBC_SATELLITE_LAT_LONG;
	private PopupWindow lotPopupWindow;
	private LinearLayout lotWindowLayout;
	private Button checkInButton, closePopButton;
	private ImageView zoneLetterImage;
	private TextView lotName, totalNumSpotsView, numSpotsLeftView, checkInMessage;
	private Integer currentLotId, checkedInLotId;
	private View googleMapView;
	private String parkingStatResponse = "";
	private HashMap<Integer, Integer> parkingLotStats;
	private HashMap<Integer, HashMap<String, String>> parkingLotInfo;
	private int numSpotsLeft = 0;
	private int loggedInUserId;
	private String loggedInUserName = "";
	private String loggedInUserZone = "";
	private AlphaAnimation fadeIn;
	private static final int A_ZONE_COLOR = Color.rgb(255, 0, 76);
	private static final int A_ZONE_COLOR_ALPHA = Color.argb(90, 255, 0, 76);
	private static final int B_ZONE_COLOR = Color.rgb(0, 102, 0);
	private static final int C_ZONE_COLOR = Color.rgb(255, 218, 18);
	private static final int C_ZONE_COLOR_ALPHA = Color.argb(90, 255, 218, 18);
	private static final int F_ZONE_COLOR = Color.rgb(255, 121, 50);
	private List<Marker> markerList = new ArrayList<Marker>();
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);
		googleMapView = findViewById(R.id.map);
		
		// Get info of the logged-in user
		Bundle extras = getIntent().getExtras();
		loggedInUserId = (Integer) extras.get("user_id");
		loggedInUserName = (String) extras.get("user_name");
		loggedInUserZone = (String) extras.get("zone_letter");
		
		if ((Integer) extras.get("checked_in_lot_id") == 0) {
			checkedInLotId = null;
		} else {
			checkedInLotId = (Integer) extras.get("checked_in_lot_id");
		}
		
		// LatLngs for UMBC Campus and the satellite lots
		UMBC_LAT_LONG = new LatLng(39.255622, -76.711143);
		UMBC_SATELLITE_LAT_LONG = new LatLng(39.237356, -76.713118);
		
		// Instantiate GoogleMap object
		map = ((MapFragment)getFragmentManager().findFragmentById(R.id.map)).getMap();
		// Zoom in to UMBC campus or the satellite lots depending on the user's parking permit
		if (loggedInUserZone.equals("F")) {
			map.animateCamera(CameraUpdateFactory.newLatLngZoom(UMBC_SATELLITE_LAT_LONG, 16), 1000, null);
		} else {
			map.animateCamera(CameraUpdateFactory.newLatLngZoom(UMBC_LAT_LONG, 16), 1000, null);
		}	
		
		// Register the map with the OnMarkerClickListener interface
		map.setOnMarkerClickListener(this);
		
		// Instantiate a HashMap object that will used to store the counts of available parking spots in each lot on campus
		parkingLotStats = new HashMap<Integer, Integer>();
		
		// Retrieve the initial parking lot stats on app creation
		UpdateParkingStats();
		
		// ParkingMapLoad AsyncTask retrieves the data used to create the parking lot polygons and markers
		ParkingMapLoad loadParkingMap = new ParkingMapLoad();
		String parkingMapResponse = "";
		
		// Instantiate a HashMap to store the information for each parking lot
		parkingLotInfo = new HashMap<Integer, HashMap<String, String>>();
		
		String [] loadMapParams = new String[1];
		loadMapParams[0] = loggedInUserZone;
		
		// Create the polygons and map markers for all parking lots relevant to the current logged-in user
		try {
			parkingMapResponse = loadParkingMap.execute(loadMapParams).get();
			
			if (!parkingMapResponse.isEmpty()) {
				JSONObject parkingMap = new JSONObject(parkingMapResponse);
				JSONArray parkingMapInfo = (JSONArray) parkingMap.get("lot_info");
				JSONArray parkingMapCoordinates = (JSONArray) parkingMap.get("lot_coordinates");
				
				// Iterate through each parking lot and extract out its information to create a marker and keep track of it
				for (int i = 0; i < parkingMapInfo.length(); i++) {
					JSONObject lot = (JSONObject) parkingMapInfo.get(i);
					
					HashMap<String, String> lotInfo = new HashMap<String, String>();
					lotInfo.put("lot_id", lot.getString("lot_id"));
					lotInfo.put("lot_name", lot.getString("lot_name"));
					lotInfo.put("zone_letter", lot.getString("zone_letter"));
					lotInfo.put("zone_description", lot.getString("zone_description"));
					lotInfo.put("regular_capacity", lot.getString("regular_capacity"));
					lotInfo.put("marker_latitude", lot.getString("marker_latitude"));
					lotInfo.put("marker_longitude", lot.getString("marker_longitude"));
					parkingLotInfo.put(Integer.valueOf(lot.getInt("lot_id")), lotInfo);
					
					// Default the marker color to green which signifies a lot with spots available
					// Red markers signify that the lot is full
					// Blue markers signify that the number of available spots in a parking lot is unknown because it could not be
					// correctly calculated (this means that an exception was thrown, or the number of spots filled is greater 
					// than the parking lot's capacity. database check constraints would prevent the latter from occurring.)
					float markerColor = BitmapDescriptorFactory.HUE_GREEN;
					int numSpotsOpen = calcNumOpenSpots(lot);
					
					// Calculate the number of open spots in a lot and set the marker color accordingly
					if (numSpotsOpen == 0) {
						markerColor = BitmapDescriptorFactory.HUE_RED;
					} else if (numSpotsOpen < 0) {
						markerColor = BitmapDescriptorFactory.HUE_BLUE;
					}
					
					// Add markers to the map
					markerList.add(map.addMarker(new MarkerOptions ().position(new LatLng(lot.getDouble("marker_latitude"), lot.getDouble("marker_longitude")))
							.title(String.valueOf(lot.getInt("lot_id")))
							.icon(BitmapDescriptorFactory.defaultMarker(markerColor))		
					));
				}
				
				LatLng [] latLngs;
				String lotType;
				int polygonColor;
				
				// Use the set of (latitude,longitude) coordinates for each lot/zone to draw its polygon at the appropriate location
				for (int j = 0; j < parkingMapCoordinates.length(); j++) {
					JSONObject lotCoordinatesObj = (JSONObject) parkingMapCoordinates.get(j);
					JSONArray latLngString = new JSONArray(lotCoordinatesObj.get("lat_lng_string").toString());
					
					lotType = lotCoordinatesObj.get("lot_type").toString();
					
					// Determine the color and opacity of the polygon that should be created
					// Parking lots and parking spaces are solid-colored polygons. 
					// Sections or regions of street parking (i.e. along Hilltop Circle) are shown as semi-transparent polygons
					if (loggedInUserZone.equals("A")) {
						if (lotType.equals("R")) {
							polygonColor = A_ZONE_COLOR_ALPHA;
						} else {
							polygonColor = A_ZONE_COLOR;
						}
					} else if (loggedInUserZone.equals("B")) {
						polygonColor = B_ZONE_COLOR;
					} else if (loggedInUserZone.equals("C")) {
						if (lotType.equals("R")) {
							polygonColor = C_ZONE_COLOR_ALPHA;
						} else {
							polygonColor = C_ZONE_COLOR;
						}
					} else {
						polygonColor = F_ZONE_COLOR;
					}
					
					latLngs = new LatLng[latLngString.length()];
					
					// Iterate through each (latitude,longitude) coordinate pair and create LatLng objects out of them
					for (int k = 0; k < latLngString.length(); k++) {
						JSONObject latLng = (JSONObject) latLngString.get(k);
						latLngs[k] = new LatLng(latLng.getDouble("latitude"), latLng.getDouble("longitude"));
					}
					
					// Add the polygons to the map
					map.addPolygon(new PolygonOptions()
								.add(latLngs)
								.strokeColor(polygonColor)
								.fillColor(polygonColor)
								.strokeWidth(2));
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
		
		// Instantiate animation objects to fade-in the check in success message
		fadeIn = new AlphaAnimation(0.0F, 1.0F);
		fadeIn.setDuration(2000);
		
		// Instantiate and set up the various views used in the marker window popup
		lotWindowLayout = new LinearLayout(this);
		lotWindowLayout.setOrientation(LinearLayout.VERTICAL);
		lotWindowLayout.setBackgroundColor(Color.WHITE);
		
		zoneLetterImage = new ImageView(this);
		zoneLetterImage.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		LinearLayout.LayoutParams zoneLetterImageLayoutParams = (LinearLayout.LayoutParams) zoneLetterImage.getLayoutParams();
		zoneLetterImageLayoutParams.setMargins(20, 25, 20, 25);
		zoneLetterImageLayoutParams.gravity = Gravity.CENTER_HORIZONTAL;
		
		lotName = new TextView(this);
		lotName = new TextView(this);
		lotName.setTypeface(Typeface.create("Arial", Typeface.BOLD), Typeface.BOLD);
		lotName.setGravity(Gravity.CENTER_HORIZONTAL);
		lotName.setTextSize(37);
		lotName.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		LinearLayout.LayoutParams lotNameLayoutParams = (LinearLayout.LayoutParams) lotName.getLayoutParams();
		lotNameLayoutParams.setMargins(20, 0, 20, 0);
		lotNameLayoutParams.gravity = Gravity.CENTER_HORIZONTAL;
		
		totalNumSpotsView = new TextView(this);
		totalNumSpotsView.setTypeface(Typeface.create("Arial", Typeface.BOLD), Typeface.BOLD);
		totalNumSpotsView.setGravity(Gravity.CENTER_HORIZONTAL);
		totalNumSpotsView.setTextSize(20);
		
		numSpotsLeftView = new TextView(this);
		numSpotsLeftView.setTypeface(Typeface.create("Arial", Typeface.BOLD), Typeface.BOLD);
		numSpotsLeftView.setGravity(Gravity.CENTER_HORIZONTAL);
		numSpotsLeftView.setTextSize(20);
		
		checkInMessage = new TextView(this);
		checkInMessage.setTypeface(Typeface.create("Arial", Typeface.BOLD), Typeface.BOLD);
		checkInMessage.setTextSize(20);
		checkInMessage.setTextColor(Color.rgb(14, 131, 0));
		checkInMessage.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		LinearLayout.LayoutParams checkInMsgLayoutParams = (LinearLayout.LayoutParams) checkInMessage.getLayoutParams();
		checkInMsgLayoutParams.setMargins(0, 25, 0, 25);
		checkInMsgLayoutParams.gravity = Gravity.CENTER_HORIZONTAL;
		
		checkInButton = new Button(this);
		checkInButton.setText("Check In");
		checkInButton.setOnClickListener(this);
		checkInButton.setBackgroundResource(R.raw.check_in_button);
		
		if (checkedInLotId != null) {
			checkInButton.setBackgroundResource(R.raw.check_in_button_disabled);
			checkInButton.setEnabled(false);
		}
		
		checkInButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		LinearLayout.LayoutParams checkInLayoutParams = (LinearLayout.LayoutParams) checkInButton.getLayoutParams();
		checkInLayoutParams.setMargins(50, 25, 50, 20);
		checkInLayoutParams.gravity = Gravity.CENTER_HORIZONTAL;
		
		closePopButton = new Button(this);
		closePopButton.setText("Close");
		closePopButton.setOnClickListener(this);
		closePopButton.setBackgroundResource(R.raw.close_popup_button);
		closePopButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		LinearLayout.LayoutParams closePopLayoutParams = (LinearLayout.LayoutParams) closePopButton.getLayoutParams();
		closePopLayoutParams.setMargins(50, 0, 50, 50);
		closePopLayoutParams.gravity = Gravity.CENTER_HORIZONTAL;
	}

	// Function used to clear old markers from the map and draw new ones according to the current parking lot stats
	private void UpdateMapMarkers () {
		// Remove old map markers and clear the marker list
		for (int x = 0; x < markerList.size(); x++) {
			markerList.get(x).remove();
		}
		markerList.clear();
		
		Iterator<Entry<Integer, HashMap<String, String>>> parkingLotIterator = parkingLotInfo.entrySet().iterator();
		
		// Iterate through each parking lot and update its marker
		while (parkingLotIterator.hasNext()) {
			Map.Entry<Integer, HashMap<String, String>> pairs = (Map.Entry<Integer, HashMap<String, String>>) parkingLotIterator.next();
			
			HashMap<String,String> parkingLot = pairs.getValue();
			
			float markerColor = BitmapDescriptorFactory.HUE_GREEN;
			
			int numSpotsOpen = calcNumOpenSpots(parkingLot);
			
			if (numSpotsOpen == 0) {
				markerColor = BitmapDescriptorFactory.HUE_RED;
			} else if (numSpotsOpen < 0) {
				markerColor = BitmapDescriptorFactory.HUE_BLUE;
			}
			
			// Create new map markers and add keep track of these new ones
			markerList.add(map.addMarker(new MarkerOptions ().position(new LatLng(Double.valueOf(parkingLot.get("marker_latitude")), Double.valueOf(parkingLot.get("marker_longitude"))))
					.title(parkingLot.get("lot_id"))
					.icon(BitmapDescriptorFactory.defaultMarker(markerColor))
			));
		}
	}
	
	// Update parking stats for the map
	private boolean UpdateParkingStats () {
		// Instantiate AsyncTask object used to update the parking stats during onCreate
		ParkingStatUpdate updateParkingStats = new ParkingStatUpdate();
		
		try {
			// Execute AsyncTask used to retrieve the latest parking stats from the MySQL database
			parkingStatResponse = updateParkingStats.execute().get();
			
			if (!parkingStatResponse.isEmpty()) {
				JSONArray parkingStats = new JSONArray(parkingStatResponse);
			
				// Clear the parking lot stats
				parkingLotStats.clear();
				// Iterate over all parking lots and update their stats
				for (int i = 0; i < parkingStats.length(); i++) {
					JSONObject lotStats = (JSONObject) parkingStats.get(i);
					parkingLotStats.put(Integer.valueOf(lotStats.getInt("lot_id")), Integer.valueOf(lotStats.getInt("spots_filled")));
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		} catch (ExecutionException e) {
			e.printStackTrace();
			return false;
		} catch (JSONException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	// Initialize the menu bar the first time it gets displayed
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}
	
	// Callback function executed when a menu item is selected
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Depending on which item was selected, take the appropriate action
	    switch (item.getItemId()) {
	    	// User checks out
	        case R.id.check_out:
	        	String [] checkOutParams = new String [2];
	        	checkOutParams[0] = checkedInLotId.toString();
	        	checkOutParams[1] = String.valueOf(loggedInUserId);
	        	CheckOut checkOutObj = new CheckOut();
	        	
				try {
					// Execute the AsyncTask to check a user out of a parking spot
					String checkOutResponse = checkOutObj.execute(checkOutParams).get();
					// On a successful checkout, update the menu bar, parking stats, and the map markers
					if (checkOutResponse != null && checkOutResponse.equals("1")) {
						checkedInLotId = null;
			        	checkInButton.setBackgroundResource(R.raw.check_in_button);
			        	checkInButton.setEnabled(true);
			        	invalidateOptionsMenu();
			        	UpdateParkingStats();
			        	UpdateMapMarkers();
			            return true;
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return false;
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return false;
				}
	        	return false;
	        // User checks in
	        case R.id.checked_in:
	        	// Display the name of the lot that the user is currently checked into
	        	return true;
	        // User is not checked in
	        case R.id.not_checked_in:
	        	// Do Nothing
	        	return true;
	        // User refreshes the map
	        case R.id.refresh_map:
	        	boolean updated = UpdateParkingStats();
	        	UpdateMapMarkers();
	        	return updated;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	// Function used to update the menu bar
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem checked_in = menu.findItem(R.id.checked_in);
		MenuItem checked_out = menu.findItem(R.id.check_out);
		MenuItem not_checked = menu.findItem(R.id.not_checked_in);
		MenuItem refresh_map = menu.findItem(R.id.refresh_map);
		MenuItem checked_in_lot = menu.findItem(R.id.checked_in_lot);
		
		// Always display the refresh map button
		refresh_map.setVisible(true);
		
		if(checkedInLotId != null) {
			// User is checked-in to a spot, so show "Checked In" and "Check Out" menu items and hide the "Not Checked In" item
			checked_in.setVisible(true);
			checked_in_lot.setTitle(parkingLotInfo.get(checkedInLotId).get("lot_name"));
			checked_out.setVisible(true);
			not_checked.setVisible(false);
		}
		else {
			// User is not checked into a spot, so show the "Not Checked In" item and hide the rest
			checked_in.setVisible(false);
			checked_out.setVisible(false);
			not_checked.setVisible(true);
		}
	    super.onPrepareOptionsMenu(menu);
	    
	    return true;
	}

	// Click handler for the map markers
	@Override
	public boolean onMarkerClick(Marker arg0) {
		// Set a local variable to the lot_id of the current lot's marker that was clicked 
		currentLotId = Integer.valueOf(arg0.getTitle());
		
		HashMap<String, String> selectedLot = parkingLotInfo.get(Integer.valueOf(currentLotId));
		String zoneString = selectedLot.get("zone_letter");
		zoneString = zoneString.replace(",", "");
		
		// If we are currently keeping track of the selected lot, populate the marker window popup with its information
		if (selectedLot != null) {
			// Determine the currently selected lot's zone(s) and display the appropriate permit image(s)
			if (zoneString.length() > 1) {
				if (zoneString.equals("AC")) {
					zoneLetterImage.setBackgroundResource(R.raw.zone_ac);
				} else if (zoneString.equals("AD")) {
					zoneLetterImage.setBackgroundResource(R.raw.zone_ad);
				} else {
					zoneLetterImage.setBackgroundResource(R.raw.zone_acde);
				}
			} else {
				if (zoneString.equals("A")) {
					zoneLetterImage.setBackgroundResource(R.raw.zone_a);
				} else if (zoneString.equals("B")) {
					zoneLetterImage.setBackgroundResource(R.raw.zone_b);
				} else if (zoneString.equals("C")) {
					zoneLetterImage.setBackgroundResource(R.raw.zone_c);
				} else {
					zoneLetterImage.setBackgroundResource(R.raw.zone_f);
				}
			}
			
			totalNumSpotsView.setText("Total Spots: " + selectedLot.get("regular_capacity"));
			
			// Determine and display the number of open spots for the selected lot
			numSpotsLeft = calcNumOpenSpots(selectedLot);
			
			if (numSpotsLeft == 0) {
				numSpotsLeftView.setTextColor(Color.RED);
				numSpotsLeftView.setText("Spots Open: " + numSpotsLeft);
			}
			else if (numSpotsLeft > 0) {
				numSpotsLeftView.setTextColor(Color.BLACK);
				numSpotsLeftView.setText("Spots Open: " + numSpotsLeft);
			} else {
				numSpotsLeftView.setTextColor(Color.BLUE);
				numSpotsLeftView.setText("Spots Open: Unknown");	
			}
			
			lotName.setText(selectedLot.get("lot_name"));
			
			// Enable/disable the "Check In" button depending on the number of open spots in a lot
			if (checkedInLotId == null) {
				if (selectedLot.containsKey("full") && selectedLot.get("full").equals("1")) {
					checkInButton.setBackgroundResource(R.raw.check_in_button_disabled);
					checkInButton.setEnabled(false);
				} else if (selectedLot.containsKey("status_unknown")) {
					if (selectedLot.get("status_unknown") != null && selectedLot.get("status_unknown").equals("1")) {
						checkInButton.setBackgroundResource(R.raw.check_in_button_disabled);
						checkInButton.setEnabled(false);
					}
				}
				else {
					checkInButton.setBackgroundResource(R.raw.check_in_button);
					checkInButton.setEnabled(true);
				}
			}
			else {
				checkInButton.setBackgroundResource(R.raw.check_in_button_disabled);
				checkInButton.setEnabled(false);
			}
		}
		
		// Add all child views to the lot window popup
		lotWindowLayout.addView(zoneLetterImage);
		lotWindowLayout.addView(lotName);
		lotWindowLayout.addView(totalNumSpotsView);
		lotWindowLayout.addView(numSpotsLeftView);
		lotWindowLayout.addView(checkInButton);
		lotWindowLayout.addView(closePopButton);
		
		// Instantiate and display the image popup window
		lotPopupWindow = new PopupWindow(lotWindowLayout, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, true);
		lotPopupWindow.setOnDismissListener(this);
		lotPopupWindow.showAtLocation(googleMapView, Gravity.CENTER, 0, 0);
		
		return true;
	}
	
	// (Parameter: JSONObject) Calculate the number of open spots left in a lot and also set whether the lot is full
	private int calcNumOpenSpots (JSONObject lot_) {
		// Default the numSpots variable to a negative number to indicate an unsuccessful or erroneous operation
		int numSpots = -1;
		try {
			if (parkingLotStats.get(lot_.getInt("lot_id")) != null) {
				numSpots = lot_.getInt("regular_capacity") - parkingLotStats.get(lot_.getInt("lot_id")).intValue();
				if (numSpots == 0) {
					parkingLotInfo.get(lot_.getInt("lot_id")).put("full", "1");
				} else if (numSpots > 0) {
					parkingLotInfo.get(lot_.getInt("lot_id")).put("full", "0");
				} else {
					parkingLotInfo.get(lot_.getInt("lot_id")).put("status_unknown", "1");
				}
			} else {
				parkingLotInfo.get(lot_.getInt("lot_id")).put("full", "0");
				numSpots = lot_.getInt("regular_capacity");
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
		return numSpots;
	}
	
	// (Parameter: HashMap<String, String>) Calculate the number of open spots left in a lot and also set whether the lot is full
	private int calcNumOpenSpots (HashMap<String, String> lot_) {
		// Default numSpots to a negative number to indicate an erroneous operation
		int numSpots = -1;
		
		if (parkingLotStats.get(Integer.valueOf(lot_.get("lot_id"))) != null) {
			numSpots = Integer.valueOf(lot_.get("regular_capacity")).intValue() - parkingLotStats.get(Integer.valueOf(lot_.get("lot_id"))).intValue();
			if (numSpots == 0) {
				parkingLotInfo.get(Integer.valueOf(lot_.get("lot_id"))).put("full", "1");
			} else if (numSpots > 0) {
				parkingLotInfo.get(Integer.valueOf(lot_.get("lot_id"))).put("full", "0");
			} else {
				parkingLotInfo.get(Integer.valueOf(lot_.get("lot_id"))).put("status_unknown", "1");
			}
		} else {
			parkingLotInfo.get(Integer.valueOf(lot_.get("lot_id"))).put("full", "0");
			numSpots = Integer.valueOf(lot_.get("regular_capacity"));
		}

		return numSpots;
	}

	// Click handler for the "Check In" and "Close" buttons on the popup window
	@Override
	public void onClick(View v) {
		if (((Button) v).getText().equals("Check In")) {
			String [] checkInParams = new String [2];
			checkInParams[0] = currentLotId.toString();
			checkInParams[1] = String.valueOf(loggedInUserId);
			
			// Instantiate AsyncTask object used for checking into a parking spot
			CheckIn checkInObj = new CheckIn();
			
			try {
				String checkInResponse = checkInObj.execute(checkInParams).get();
			
				if (checkInResponse != null && checkInResponse.equals("1")) {
					// Set the user as checked in and track the lot id
					checkedInLotId = currentLotId;
					
					if (lotWindowLayout.findViewWithTag(checkInMessage) == null) {
						lotWindowLayout.addView(checkInMessage, 4);
					}
					checkInMessage.setText("Checked In!");
					checkInMessage.startAnimation(fadeIn);
					
					// Update the options menu, parking stats, and map markers
					invalidateOptionsMenu();
					UpdateParkingStats();
					UpdateMapMarkers();
					
					int numSpotsOpen = calcNumOpenSpots(parkingLotInfo.get(Integer.valueOf(checkedInLotId)));
					if (numSpotsOpen == 0) {
						numSpotsLeftView.setTextColor(Color.RED);
						numSpotsLeftView.setText("Spots Open: " + numSpotsOpen);
					} else if (numSpotsOpen > 0) {
						numSpotsLeftView.setTextColor(Color.BLACK);
						numSpotsLeftView.setText("Spots Open: " + numSpotsOpen);
					} else {
						numSpotsLeftView.setTextColor(Color.BLUE);
						numSpotsLeftView.setText("Spots Open: Unknown");	
					}
					
					checkInButton.setBackgroundResource(R.raw.check_in_button_disabled);
					checkInButton.setEnabled(false);
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (((Button) v).getText().equals("Close")) {
			lotPopupWindow.dismiss();
		}
	}

	// Handler for executing code on dismissal of the popup window
	@Override
	public void onDismiss() {
		// Remove all views to avoid the exception thrown when attempting to add views to a parent view twice
		totalNumSpotsView.setText("");
		numSpotsLeftView.setText("");
		lotWindowLayout.removeAllViews();
	}
}
