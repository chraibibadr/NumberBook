package ma.emsi.numberbook;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.hbb20.CountryCodePicker;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private HashMap<String, String> contacts = new HashMap<>();
    String insertUrl = "http://10.0.2.2:8080/contacts";
    private Button search;
    private EditText phone;
    private CountryCodePicker ccp;
    private JSONObject response;
    private FrameLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Read Contacts Permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Permissions.Request_READ_CONTACTS(this, 1);
            return;
        }

        phone = findViewById(R.id.phone);
        search = findViewById(R.id.search);
        ccp = findViewById(R.id.ccp);
        container = findViewById(R.id.container);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("CCP", ccp.getSelectedCountryCode());
                Log.d("CCP", ccp.getSelectedCountryName());
                container.removeAllViews();
                String tele = phone.getText().toString();
                if (tele.length() == 10)
                    tele = tele.substring(1);
                Log.d("TELE", tele);
                searchContact(ccp.getSelectedCountryCode() + tele);
            }
        });

        phone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                container.removeAllViews();
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    container.removeAllViews();
            }

            @Override
            public void afterTextChanged(Editable editable) {
                container.removeAllViews();
            }
        });

        contacts.putAll(getNumber(MainActivity.this.getContentResolver()));
        for (Map.Entry<String, String> set : contacts.entrySet()) {
            if (set.getValue().length() == 10)
                addContactJson(set.getKey(), "212" + set.getValue().substring(1));
            else
                addContactJson(set.getKey(), set.getValue().replaceAll("[^a-zA-Z0-9]", ""));
        }
    }

    public HashMap<String, String> getNumber(ContentResolver cr) {
        Cursor phones = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);

        HashMap<String, String> contacts = new HashMap<>();
        while (phones.moveToNext()) {
            @SuppressLint("Range") String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            @SuppressLint("Range") String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            contacts.put(name, phoneNumber);
        }
        phones.close();// close cursor
        return contacts;
    }

    void addContactJson(final String name, final String phone) {
        // Optional Parameters to pass as POST request
        JSONObject js = new JSONObject();
        try {
            js.put("id", null);
            js.put("name", name);
            js.put("phone", phone);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Make request for JSONObject
        JsonObjectRequest jsonObjReq = new JsonObjectRequest(
                Request.Method.POST, insertUrl, js,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("MainActivity", response.toString());
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("MainActivity", error.toString());
            }
        }) {
            /**
             * Passing some request headers
             */
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }
        };

        // Adding request to request queue
        Volley.newRequestQueue(this).add(jsonObjReq);
    }

    public void searchContact(String phone) {
        List<String> c = new ArrayList<>();
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        String url = "http://10.0.2.2:8080/contacts/" + phone;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            MainActivity.this.response = new JSONObject(response);
                            c.add(MainActivity.this.response.getString("name"));
                            c.add(MainActivity.this.response.getString("phone"));

                            Bundle bundle = new Bundle();
                            bundle.putString("name", c.get(0));
                            bundle.putString("phone", "+" + c.get(1));
                            SearchResult srFragment = new SearchResult();
                            srFragment.setArguments(bundle);
                            getSupportFragmentManager().beginTransaction().add(R.id.container, srFragment).commit();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (c.toString().equals("[]"))
                            getSupportFragmentManager().beginTransaction().add(R.id.container, new NoSearchResult()).commit();
                        Log.d("MainActivity", c.toString());
                        Log.d("MainActivity", phone);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("MainActivity", error.toString());
            }
        });
        queue.add(stringRequest);
    }
}