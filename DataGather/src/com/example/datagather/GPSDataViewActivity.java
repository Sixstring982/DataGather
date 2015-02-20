package com.example.datagather;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class GPSDataViewActivity extends Activity {
	private static final String TAG = ">>GPS Data View";
	private Activity self = this;
	private DatabaseHandler db;
	private ArrayList<DataPointGPS> gpsPointList;
	private ListView listview;
	private GPSDataPointArrayAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gpsdataview);

		
		db = new DatabaseHandler(this);
		gpsPointList = db.getAllGPSDataPoints();
		
		listview = (ListView) findViewById(R.id.listview);
		adapter = new GPSDataPointArrayAdapter(this, gpsPointList);
		listview.setAdapter(adapter);

		listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, final View view,int position, long id) {
				final String item = (String) parent.getItemAtPosition(position);

			}

		});
	}

	private class GPSDataPointArrayAdapter extends BaseAdapter {

		private final Context context;
		private LayoutInflater inflater;
		private ArrayList<DataPointGPS> gpsPointList;
		private Time time = new Time();
		
		

		public GPSDataPointArrayAdapter(Context _context, ArrayList<DataPointGPS> _gpsPointList) {
			super();
			context = _context;
			gpsPointList = _gpsPointList;
			inflater = LayoutInflater.from(context); 
		}

		@Override
		  public View getView(int position, View convertView, ViewGroup parent) {
			 if (convertView == null) {
		            convertView = inflater.inflate (R.layout.list_item_gpspoint, parent, false);

		            //This assumes layout/row_left.xml includes a TextView with an id of "textview"
		            convertView.setTag (R.id.gpsDataPoint_list_item_DataTextView, convertView.findViewById(R.id.gpsDataPoint_list_item_DataTextView));
		        }

			
		    TextView data = (TextView) convertView.getTag(R.id.gpsDataPoint_list_item_DataTextView);
		    time.set(gpsPointList.get(position).getTime());
		    
		    String longitude = String.format("%.2f", gpsPointList.get(position).getLongitude());
		    String latitude  = String.format("%.2f", gpsPointList.get(position).getLatitude());
		    String altitude  = String.format("%.2f", gpsPointList.get(position).getAltitude());
		    
		    data.setText(time.format("  %m/%d/%y %H:%M:%S [ "+longitude+"� "+latitude+"� "+altitude+"m ]"));
		    
		    
		    //data.setText(" >"+longitude+"� "+latitude+"� "+altitude+"m");
		    //data.setText("0.0� 0.0� 0.0m");
		    return convertView;
		  }

		@Override
		public int getCount() {
			return  gpsPointList.size();
		}

		@Override
		public Object getItem(int position) {
			return gpsPointList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

	}

}
