package com.example.dots.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.dots.R;
import com.example.dots.urls.MyURLs;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * To start a game you first have to login using your username and password. If you are not registered
 * yet press newPlayer button that starts new activity where you can register.
 */
public class LoginActivity extends AppCompatActivity {
    private EditText editTextUserName;
    private EditText editTextPassword;
    private RequestQueue queue;
    private StringRequest stringRequest;
    private String userName;
    private String password;
    private int mStatusCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        editTextUserName = findViewById(R.id.editTextUserName);
        editTextPassword = findViewById(R.id.editTextPassword);
        Button buttonLogin = findViewById(R.id.buttonLogin);
        Button buttonNewPlayer = findViewById(R.id.buttonNewPlayer);

        queue = Volley.newRequestQueue(this);

        buttonNewPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this,RegisterActivity.class);
                startActivity(intent);
            }
        });

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                queue.add(stringRequest);
            }
        });



        stringRequest = new StringRequest(Request.Method.GET, MyURLs.LOGIN_URL,
                new Response.Listener<String>() {
            @Override
            public void onResponse(String  response) {

                if(mStatusCode == HttpURLConnection.HTTP_OK){
                    Toast.makeText(LoginActivity.this, R.string.success, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this,MainActivity.class);
                    Bundle extras = new Bundle();
                    extras.putString("UserName",userName);
                    extras.putString("Password",password);
                    intent.putExtras(extras);
                    startActivity(intent);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                if(networkResponse!=null && networkResponse.statusCode == HttpURLConnection.HTTP_UNAUTHORIZED){
                    Toast.makeText(LoginActivity.this,R.string.authorizationFailed,Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(LoginActivity.this, R.string.serverError, Toast.LENGTH_SHORT).show();
                }
            }
        }){

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                mStatusCode = response.statusCode;
                return super.parseNetworkResponse(response);
            }

            @Override
            public Map<String,String> getHeaders() throws AuthFailureError{
                HashMap<String,String> headers = new HashMap<>();
                userName = editTextUserName.getText().toString();
                password = editTextPassword.getText().toString();
                String credentials = userName + ":" + password;
                String encodedCredentials = Base64.encodeToString(credentials.getBytes(),Base64.NO_WRAP);
                headers.put("Authorization","Basic "+encodedCredentials);
                return headers;
            }
        };



    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        editTextUserName.setText(savedInstanceState.getString("userName"));
        editTextPassword.setText(savedInstanceState.getString("password"));
    }



    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("userName",editTextUserName.getText().toString());
        outState.putString("password",editTextPassword.getText().toString());
    }
}
