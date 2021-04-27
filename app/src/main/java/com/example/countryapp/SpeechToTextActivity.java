package com.example.countryapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class SpeechToTextActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SPEECH_INPUT = 1000;
    private TextView mMicTextView;

    private List<CountryDataItem> countryDataItemArrayList = new ArrayList<>();
    private List<HashMap<String, String>> countryDataItemHashMap = new ArrayList<>();
    private SimpleAdapter simpleAdapter;
    private String requestUrl = "https://restcountries.eu/rest/v2/name/";
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech_to_text);
        requestQueue = Volley.newRequestQueue(this);

        mMicTextView = findViewById(R.id.micTextView);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {                  //tietojen tallentaminen kännykän kääntämisen varalta
        // talleta tänne kaikki aktiviteetin data (tietojäsenet), jos tarpeen
        outState.putSerializable("STATE_DATA", (Serializable) countryDataItemHashMap);
        outState.putString("TEXT_VIEW", mMicTextView.getText().toString());
        // call superclass to save any view hierarchy
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {     //tietojen palauttaminen kännykän kääntämisen varalta
        countryDataItemHashMap = (ArrayList<HashMap<String,String>>) savedInstanceState.getSerializable("STATE_DATA");
        mMicTextView.setText(savedInstanceState.getString("TEXT_VIEW"));

        simpleAdapter = new SimpleAdapter(this, countryDataItemHashMap,
                R.layout.country_list_item_layout,
                new String[] {"description", "value"},
                new int[] {R.id.descriptionTextView, R.id.valueTextView}
        );

        ListView countryListView = (ListView) findViewById(R.id.countryListView);
        countryListView.setAdapter(simpleAdapter);
    }

    public void openMainActivity(View view) {
        Intent openMainActivityIntent = new Intent(this, MainActivity.class);
        startActivity(openMainActivityIntent);
    }

    private void getCountryDataAndUpdateUI(String countryName) {
        // päivitä maa ja hae data kyseiselle maalle
        StringRequest stringRequest = new StringRequest(Request.Method.GET, requestUrl + countryName,
                response -> {
                    //response sisältää response stringin (JSON)
                    parseJson( response );
                },
                error -> {
                    // error sisältää volleyltä tulleen mahdollisen virheen
                    Toast.makeText( this, "Error while making the request", Toast.LENGTH_LONG ).show();
                }
        );
        //Lisätään request jonoon
        if (requestQueue != null)
            requestQueue.add( stringRequest );
    }

    private void parseJson(String jsonResponse){
        //kaivetaan data JSON responsesta
        try {
            countryDataItemArrayList.clear();

            JSONArray countryArray = new JSONArray(jsonResponse);
            JSONObject main = countryArray.getJSONObject(0);
            String name = main.getString("name");
            countryDataItemArrayList.add(new CountryDataItem("Name", name));
            String capital = main.getString("capital");
            countryDataItemArrayList.add(new CountryDataItem("Capital", capital));
            String population = String.valueOf(main.getInt("population"));
            countryDataItemArrayList.add(new CountryDataItem("Population", population));
            String region = main.getString("region");
            countryDataItemArrayList.add(new CountryDataItem("Region", region));
            String currency = main.getJSONArray("currencies").getJSONObject(0).getString("name");
            countryDataItemArrayList.add(new CountryDataItem("Currency", currency));
            //päivitetään UI
            updateUI();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private void updateUI(){
        //poistetaan vanhan haun data
        countryDataItemHashMap.clear();
        //siirretään data HashMappeihin, jotka listataan yhteen lopuksi
        for (CountryDataItem i : countryDataItemArrayList){
            HashMap<String, String> countryDataHashItem = new HashMap<>();
            countryDataHashItem.put("description", i.mDescription);
            countryDataHashItem.put("value", i.mValue);
            countryDataItemHashMap.add(countryDataHashItem);
        }
        //käytetään tehtyä listaa listanäytön luomiseen SimpleAdapterin avulla
        simpleAdapter = new SimpleAdapter(this, countryDataItemHashMap,
                R.layout.country_list_item_layout,
                new String[] {"description", "value"},
                new int[] {R.id.descriptionTextView, R.id.valueTextView}
        );

        ListView countryListView = (ListView) findViewById(R.id.countryListView);
        countryListView.setAdapter(simpleAdapter);
    }

    public void speak(View view) {  //metodi puheen tallentamisen palvelulle
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Sano maan nimi, jonka tietoja haluat nähdä");

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
        }
        catch (Exception e) {
            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {   //puheen tallentaminen ohjelman käyttöön
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_CODE_SPEECH_INPUT:{
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    mMicTextView.setText(result.get(0));
                }
                break;
            }
        }
        getCountryDataAndUpdateUI(mMicTextView.getText().toString());
    }
}