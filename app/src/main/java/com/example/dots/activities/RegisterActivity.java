package com.example.dots.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.dots.R;
import com.example.dots.urls.MyURLs;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private EditText userNameEditText;
    private EditText passwordEditText;
    private EditText repeatPasswordEditText;
    private RequestQueue requestQueue;
    private StringRequest stringRequest;
    private int statusCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        userNameEditText = findViewById(R.id.editTextUserName);
        passwordEditText = findViewById(R.id.editTextPassword);
        repeatPasswordEditText = findViewById(R.id.editTextRepeatPassword);
        Button registerButton = findViewById(R.id.buttonRegister);
        requestQueue = Volley.newRequestQueue(this);
        if (savedInstanceState != null){
            userNameEditText.setText(savedInstanceState.getString("userName"));
            passwordEditText.setText(savedInstanceState.getString("password"));
            repeatPasswordEditText.setText(savedInstanceState.getString("repeatPassword"));
        }
        // create post request to register on server
        stringRequest = new StringRequest(Request.Method.POST, MyURLs.REGISTER_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if(statusCode == 201){
                            Toast.makeText(RegisterActivity.this,R.string.registerSucceed,Toast.LENGTH_SHORT).show();
                            RegisterActivity.this.finish();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                if(networkResponse != null && networkResponse.statusCode == HttpURLConnection.HTTP_CONFLICT){
                    Toast.makeText(RegisterActivity.this,R.string.registrationError, Toast.LENGTH_SHORT).show();
                }else if(networkResponse != null && networkResponse.statusCode == HttpURLConnection.HTTP_BAD_REQUEST){
                    Toast.makeText(RegisterActivity.this,R.string.invalidRegistrationParams, Toast.LENGTH_SHORT).show();
                } else{
                    Toast.makeText(RegisterActivity.this,R.string.serverError, Toast.LENGTH_SHORT).show();
                }

            }
        }){
            protected Response<String> parseNetworkResponse(NetworkResponse response){
                statusCode = response.statusCode;
                return super.parseNetworkResponse(response);
            }


                @Override
                public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }

                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("Name", userNameEditText.getText().toString().trim());
                params.put("Password", passwordEditText.getText().toString().trim());
                return params;
            }
        };
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String password = passwordEditText.getText().toString();
                String confirmPassword = repeatPasswordEditText.getText().toString();
                if(password.equals(confirmPassword)){
                    requestQueue.add(stringRequest);
                }else{
                    Toast.makeText(RegisterActivity.this,R.string.confirmPasswordError, Toast.LENGTH_SHORT).show();
                }

            }
        });


    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("userName",userNameEditText.getText().toString());
        outState.putString("password",passwordEditText.getText().toString());
        outState.putString("repeatPassword",repeatPasswordEditText.getText().toString());
        super.onSaveInstanceState(outState);
    }
}
