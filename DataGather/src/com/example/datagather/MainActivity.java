package com.example.datagather;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;

public class MainActivity extends Activity {

	private Activity self = this;
	private static final String TAG = ">>MAIN";
	private static final String DATA_PREFS_NAME = "CurrentData";

	private LocationManager locationManager;
	private LocationListener locationListener;

	

	private boolean activityVisible;
	private boolean gpsProviderEnabled = true;
	private boolean networkConnected   = false;
	private boolean capturingGPSData = false;
	private  String dataPostUrl = "http://174.56.72.130:3000/submit";
	private  String thisPhoneNumber = "";

	DatabaseHandler db;
	private int numberOfSavedGPSDataPoints = 0;

	private Location lastLocation;
	private Time time = new Time();
	private long timeOfLastSave_MiliSec = 0;
	private final long saveFrequency_MiliSec = 5000;
	private final int maxGPSDataPoints = 50000;
	
	private ToggleButton tbtn_gps;
	private TableLayout gpstable;
	private TextView txtview_CurrentLocationTitle;
	private TextView txtview_CurrentLon,
                     txtview_CurrentLat,
                     txtview_CurrentAlt,
                     txtview_CurrentTime,
                     txtview_PointsSaved,
                     txtview_httpReult;

	void onLocationReceived(Location loc) {
		lastLocation = loc;

		if ((lastLocation.getTime() - timeOfLastSave_MiliSec) > saveFrequency_MiliSec) {
			// --Try to save this update
			DataPointGPS gpspoint = new DataPointGPS();
			gpspoint.setLongitude(lastLocation.getLongitude());
			gpspoint.setLatitude(lastLocation.getLatitude());
			gpspoint.setAltitude(lastLocation.getAltitude());
			gpspoint.setTime(lastLocation.getTime());

			if (numberOfSavedGPSDataPoints < maxGPSDataPoints) {
				db.addGPSDataPoint(gpspoint);
				numberOfSavedGPSDataPoints = db.getGPSDataPointCount();
				txtview_CurrentLocationTitle.setText("GPS Location - Current");
			} else {
				// TOO MUCH DATA!!!
				// --Turn off capture
				turnOffGPSDataCapture();
				tbtn_gps.setChecked(false);
				txtview_CurrentLocationTitle.setText("GPS Location - DATA FULL!");
				txtview_CurrentLocationTitle.setTextColor(Color.argb(255, 150, 0, 0));

				// ---Build Alert
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage("You have used up all your space for GPS Data (" + maxGPSDataPoints
						+ " data Points).\nYou must Send your data or Delete it before continuing to capture.");
				builder.setTitle("GPS Data");
				builder.setPositiveButton("Ok", null);
				AlertDialog dialog = builder.create();
				dialog.show();
			}

			// --update time of last save to compare next location's time to.
			timeOfLastSave_MiliSec = lastLocation.getTime();
		}

		if (activityVisible) {
			updateUI();
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// ----------------------------------------------------------
		// Get Telephone Number for ID. 
		TelephonyManager tMgr = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
		thisPhoneNumber = tMgr.getLine1Number();
		
		
		// ----------------------------------------------------------
		// Location Manager
		locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		locationListener = new LocationListener() {

			// Called when a new location is found by the network location
			// provider.
			public void onLocationChanged(Location location) {
				Log.d(TAG, "Got location from " + location.getProvider() + ": " + location.getLatitude() + ", " + location.getLongitude());
				onLocationReceived(location);
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {
				Log.d(TAG, "Status Changed " + provider + " " + status);
				Toast.makeText(self, provider + " " + status, Toast.LENGTH_LONG).show();
			}

			public void onProviderEnabled(String provider) {
				Log.d(TAG, provider + " enabled");
				if (provider.equals("gps")) {
					gpsProviderEnabled = true;
				}
				updateGPSViewStateStyle();
				Toast.makeText(self, provider + " enabled", Toast.LENGTH_LONG).show();
			}

			public void onProviderDisabled(String provider) {

				Log.d(TAG, provider + " disabled");
				if (provider.equals("gps")) {
					gpsProviderEnabled = false;
				}
				updateGPSViewStateStyle();
				Toast.makeText(self, provider + " disabled", Toast.LENGTH_LONG).show();
			}
		};

		// ----------------------------------------------------------
		// new database
		db = new DatabaseHandler(this);
		numberOfSavedGPSDataPoints = db.getGPSDataPointCount();

		// ----------------------------------------------------------
		// Get Stored data
		SharedPreferences mostRecentData = getSharedPreferences(DATA_PREFS_NAME, 0);
		float lastupdated_longitude = mostRecentData.getFloat("longitude", 0.0f);
		float lastupdated_latitude = mostRecentData.getFloat("latitude", 0.0f);
		float lastupdated_altitude = mostRecentData.getFloat("altitude", 0.0f);
		String lastupdated_time = mostRecentData.getString("time", "--:--:-- --/--/----");

		// ----------------------------------------------------------
		// set up UI
		tbtn_gps = (ToggleButton) findViewById(R.id.tb_gatherGPSData);
		gpstable = (TableLayout) findViewById(R.id.table_GPSData);
		txtview_CurrentLocationTitle = (TextView) findViewById(R.id.t_gpsTitleTextView);
		txtview_CurrentLon = (TextView) findViewById(R.id.t_longitudeTextView);
		txtview_CurrentLat = (TextView) findViewById(R.id.t_latitudeTextView);
		txtview_CurrentAlt = (TextView) findViewById(R.id.t_altitudeTextView);
		txtview_CurrentTime = (TextView) findViewById(R.id.t_lastUpdatedTextView);
		txtview_PointsSaved = (TextView) findViewById(R.id.t_pointsSavedTextView);
		txtview_httpReult   = (TextView) findViewById(R.id.t_httpResultTextView);

		// ----------------------------------------------------------
		// fill UI with saved old data.
		txtview_CurrentLon.setText(Float.toString(lastupdated_longitude) + "�");
		txtview_CurrentLat.setText(Float.toString(lastupdated_latitude) + "�");
		txtview_CurrentAlt.setText(Float.toString(lastupdated_altitude) + "m");
		txtview_CurrentTime.setText(lastupdated_time);
		txtview_PointsSaved.setText("" + numberOfSavedGPSDataPoints);

		// ----------------------------------------------------------
		// check if you are connected or not
		if (isConnected()) {
			Toast.makeText(self, "Network : Connected", Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(self, "Network : Not Connected", Toast.LENGTH_LONG).show();
		}

	}

	@Override
	public void onStart() {
		super.onStart();
		Log.d(TAG, "Started");
		activityVisible = true;
	}

	@Override
	public void onStop() {
		Log.d(TAG, "Stopped");
		activityVisible = false;

		// ---Save View Info
		if (lastLocation != null) {
			// save the Last location into Shared Preferences so they can be
			// displayed the next time the app starts
			SharedPreferences mostRecentData =
                              getSharedPreferences(DATA_PREFS_NAME, 0);

            time.set(lastLocation.getTime());

            // Make and commit edits in background
			mostRecentData.edit()
                    .putFloat("longitude", (float) lastLocation.getLongitude())
                    .putFloat("latitude", (float) lastLocation.getLatitude())
                    .putFloat("altitude", (float) lastLocation.getAltitude())
                    .putString("time", time.format("%H:%M:%S %m/%d/%Y"))
                    .apply();
		}

		super.onStop();
	}

	// ----------------------------------------------------------
	// Network Functions
	public boolean isConnected() {
		ConnectivityManager connMgr =
                (ConnectivityManager)
                        getSystemService(Activity.CONNECTIVITY_SERVICE);

		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected())
		{
			networkConnected = true;
			return true;
		}
		else
		{
			networkConnected = false;
			return false;
		}
	}
	
	public void httpPOSTResult(String result) {
        new AlertDialog.Builder(this)
		.setMessage("Post Successful!\n" + result + "\nDo you " +
                           "want to delete all locally saved GPS Data?")
		.setTitle("GPS Data")
        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
			public void onClick(DialogInterface dialog, int id) {
				clearGPSData();
			}
		})
		.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
			public void onClick(DialogInterface dialog, int id) {
				// User cancelled the dialog
			}
		}).create().show();
	}
	
	// ----------------------------------------------------------
	// Data Functions
	public void turnOnGPSDataCapture() {
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                                               0, 0, locationListener);
		capturingGPSData = true;

		updateGPSViewStateStyle();
		Toast.makeText(self, "Storing GPS Location Data on Local Device.",
                       Toast.LENGTH_LONG).show();
	}

	public void turnOffGPSDataCapture() {
		capturingGPSData = false;
		locationManager.removeUpdates(locationListener);

		updateGPSViewStateStyle();
		Toast.makeText(self, "No longer storing GPS Location Data.",
                       Toast.LENGTH_LONG).show();
	}

	public void packDataGPSData() {

		ArrayList<DataPointGPS> gpspoints = db.getAllGPSDataPoints(); 
				
		//JSONObject jsonGPSData = new JSONObject();
		//JSONArray jsonGPSPoints = new JSONArray(gpspoints);
		//ArrayList<JSONObject> jsonGPSPoints = new ArrayList<JSONObject>();
		
		//Made my own JSON converter because the java one was double escaping all the quotes. :(
		String jsonGPSData = "{";
		jsonGPSData+="\"phonenumber\":\""+thisPhoneNumber+"\",";
		jsonGPSData+="\"data\":[";
		
		for (DataPointGPS  gpspoint : gpspoints)
		{
			jsonGPSData+=gpspoint.toString()+",";
		}
		jsonGPSData+="}";
       
        HttpAsyncTask httptask = new HttpAsyncTask(this);
        httptask.setJsonObjectToPost(jsonGPSData);
        //txtview_httpReult.setText( ">>"+jsonGPSData.toString());
        httptask.execute(dataPostUrl);
	}
	
	
	public void clearGPSData() {
		db.clearGPSDataPoints();
		numberOfSavedGPSDataPoints = db.getGPSDataPointCount();
		lastLocation = null;
		txtview_CurrentLocationTitle.setText("GPS Location - None");
		txtview_CurrentLocationTitle.setTextColor(Color.argb(255, 0, 0, 0));
		updateUI();
		Toast.makeText(self, "GPS Data deleted.", Toast.LENGTH_LONG).show();
	}

	// ----------------------------------------------------------
	// Button Functions
	public void onToggleClicked_gatherGPSData(View view) {
		if (((ToggleButton) view).isChecked()) {
			turnOnGPSDataCapture();
		} else {
			turnOffGPSDataCapture();
		}
	}

	public void onClickedSendGPSDataPoints(View view) {
		if (isConnected())
		{
			numberOfSavedGPSDataPoints = db.getGPSDataPointCount();
			
			new AlertDialog.Builder(this)
			.setMessage("Sending " + numberOfSavedGPSDataPoints +
                        " GPS Data Points. Would you like to continue?")
			.setTitle("GPS Data")

			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
                    packDataGPSData();
				}
			})
			.setNegativeButton("No", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					// User cancelled the dialog
				}
			}).create().show();
			
		}
		else
		{
			new AlertDialog.Builder(this)
			.setMessage("You are not connected to a network. Please enable your " +
                        "Wireless connection or Data Service and try again.")
			.setTitle("GPS Data")
            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
				}
			}).create().show();
		}
	}

	public void onClickedClearGPSDataPoints(View view) {

		new AlertDialog.Builder(this)
		.setMessage("Are you sure you want to delete all saved GPS Data?")
		.setTitle("GPS Data")
		.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				clearGPSData();
			}
		})
		.setNegativeButton("No", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				// User cancelled the dialog
			}
		}).create().show();
	}

	public void onClickedViewGPSDataPoints(View view) {
		Intent intent = new Intent(this, GPSDataViewActivity.class);
		startActivity(intent);
	}

	// ----------------------------------------------------------
	// View Functions
	private void updateGPSViewStateStyle() {
		if (capturingGPSData) {
			if (gpsProviderEnabled) {
				txtview_CurrentLocationTitle.setText("GPS Location - Pending ...");
				txtview_CurrentLocationTitle.setTextColor(Color.argb(255, 0, 0, 0));
				setTextViewStyle_Active(txtview_CurrentLon);
				setTextViewStyle_Active(txtview_CurrentLat);
				setTextViewStyle_Active(txtview_CurrentAlt);
				setTextViewStyle_Active(txtview_CurrentTime);
				gpstable.setBackgroundColor(Color.argb(255, 157, 255, 208));
			} else {
				txtview_CurrentLocationTitle.setText("GPS Location - GPS Disabled!");
				txtview_CurrentLocationTitle.setTextColor(Color.argb(255, 150, 0, 0));
				setTextViewStyle_ActiveError(txtview_CurrentLon);
				setTextViewStyle_ActiveError(txtview_CurrentLat);
				setTextViewStyle_ActiveError(txtview_CurrentAlt);
				setTextViewStyle_ActiveError(txtview_CurrentTime);
				gpstable.setBackgroundColor(Color.argb(255, 255, 167, 167));
			}
		} else {
			txtview_CurrentLocationTitle.setText("GPS Location - Most Recent");
			txtview_CurrentLocationTitle.setTextColor(Color.argb(255, 0, 0, 0));
			setTextViewStyle_Inactive(txtview_CurrentLon);
			setTextViewStyle_Inactive(txtview_CurrentLat);
			setTextViewStyle_Inactive(txtview_CurrentAlt);
			setTextViewStyle_Inactive(txtview_CurrentTime);
			gpstable.setBackgroundColor(Color.argb(255, 214, 214, 214));
		}

	}

	private void setTextViewStyle_Active(TextView textview) {
		textview.setTextColor(Color.argb(255, 0, 88, 38));
		// textview.setBackgroundColor(Color.argb(255, 157, 255, 208));
	}

	private void setTextViewStyle_ActiveError(TextView textview) {
		textview.setTextColor(Color.argb(255, 88, 0, 0));
		// textview.setBackgroundColor(Color.argb(255, 255, 167, 167));
	}

	private void setTextViewStyle_Inactive(TextView textview) {
		textview.setTextColor(Color.argb(255, 110, 110, 110));
		// textview.setBackgroundColor(Color.argb(255, 214, 214, 214));
	}

	public void updateUI() {
		Log.d(TAG, "UI Update");

		if (lastLocation != null) {
			// durationSeconds = getDurationSeconds(mLastLocation.getTime());

			txtview_CurrentLon.setText(Double.toString(lastLocation.getLongitude()) + "�");
			txtview_CurrentLat.setText(Double.toString(lastLocation.getLatitude()) + "�");
			txtview_CurrentAlt.setText(Double.toString(lastLocation.getAltitude()) + "m");
			time.set(lastLocation.getTime());
			txtview_CurrentTime.setText(time.format("%H:%M:%S %m/%d/%Y"));
			txtview_PointsSaved.setText("" + numberOfSavedGPSDataPoints);
		} else {
			txtview_CurrentLon.setText(0.0f + "�");
			txtview_CurrentLat.setText(0.0f + "�");
			txtview_CurrentAlt.setText(0.0f + "m");
			txtview_CurrentTime.setText("--:--:-- --/--/----");
			txtview_PointsSaved.setText("" + numberOfSavedGPSDataPoints);
		}
	}

	// ----------------------------------------------------------
	// Auto Gen Menu stuff.
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
