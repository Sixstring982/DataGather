package com.example.datagather;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

class HttpAsyncTask extends AsyncTask<String, Void, String> {
    
	private Context context;
	String jsonObjectToPost = "";

	public HttpAsyncTask(Context _context) {
			super();
			context = _context;
		}
	
	public void setJsonObjectToPost(JSONObject jsonObject){
		jsonObjectToPost = jsonObject.toString();
	}
	
	public void setJsonObjectToPost(String _jsonObjectString){
		jsonObjectToPost = _jsonObjectString;
	}
	
	
	@Override
    protected String doInBackground(String... urls) {

       // person = new Person();
       // person.setName(etName.getText().toString());
       // person.setCountry(etCountry.getText().toString());
       // person.setTwitter(etTwitter.getText().toString());

        return POST_JSON(urls[0],jsonObjectToPost);//person);
    }
    
    public String POST_JSON(String url, String json){
        InputStream inputStream = null;
        String result = "";
        try {
 
            // 1. create HttpClient
            HttpClient httpclient = new DefaultHttpClient();
 
            // 2. make POST request to the given URL
            HttpPost httpPost = new HttpPost(url);
 
            //String json = "";
 
            // 3. build jsonObject
            //JSONObject jsonObject = new JSONObject();
            //jsonObject.accumulate("name", person.getName());
            //jsonObject.accumulate("country", person.getCountry());
            //jsonObject.accumulate("twitter", person.getTwitter());
 
            // 4. convert JSONObject to JSON to String
            //json = jsonObject.toString();
 
            // ** Alternative way to convert Person object to JSON string usin Jackson Lib 
            // ObjectMapper mapper = new ObjectMapper();
            // json = mapper.writeValueAsString(person); 
 
             // 5. set json to StringEntity
            StringEntity se = new StringEntity(json);
 
            // 6. set httpPost Entity
            httpPost.setEntity(se);
 
            // 7. Set some headers to inform server about the type of the content   
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
 
            // 8. Execute POST request to the given URL
            HttpResponse httpResponse = httpclient.execute(httpPost);
 
            // 9. receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();
 
            // 10. convert inputstream to string
            if(inputStream != null)
                result = convertInputStreamToString(inputStream);
            else
                result = "Did not work!";
 
        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }
 
        // 11. return result
        return result;
    }
 
   

	private String convertInputStreamToString(InputStream inputStream) throws IOException{
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;
 
        inputStream.close();
        return result;
 
    }   
    
    // onPostExecute displays the results of the AsyncTask.
    @Override
    protected void onPostExecute(String result) {
    	
    	((MainActivity) context).httpPOSTResult(result);
    	
    	
   }
}
