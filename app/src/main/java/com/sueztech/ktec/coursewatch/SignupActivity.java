package com.sueztech.ktec.coursewatch;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import io.michaelrocks.libphonenumber.android.NumberParseException;
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil;
import io.michaelrocks.libphonenumber.android.Phonenumber;

public class SignupActivity extends AppCompatActivity {

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.nameEditText)
    protected EditText nameEditText;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.emailEditText)
    protected EditText emailEditText;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.passwordEditText)
    protected EditText passwordEditText;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.confirmPasswordEditText)
    protected EditText confirmPasswordEditText;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.phoneEditText)
    protected EditText phoneEditText;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.signupButton)
    protected Button signupButton;

    private MessageDigest messageDigest;
    private PhoneNumberUtil phoneUtil;
    private ProgressDialog progressDialog;
    private RequestQueue requestQueue;
    private StringRequest signupRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ButterKnife.bind(this);

        phoneUtil = PhoneNumberUtil.createInstance(this);
        phoneEditText.addTextChangedListener(new PhoneNumberFormattingTextWatcher(phoneUtil));
        phoneEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE) {
                    doSignup();
                    return true;
                }
                return false;
            }
        });

        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            Log.wtf(Config.LOG_TAG, e.toString());
        }

        requestQueue = Volley.newRequestQueue(this);

        progressDialog = new ProgressDialog(this, R.style.AppTheme_LoginActivity_ProgressDialog);

        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);

        progressDialog.show();

        JsonObjectRequest collegesRequest = new JsonObjectRequest(Config.SSO_COLLEGES_URL, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                finishInit(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(Config.LOG_TAG, error.toString());
                onInitFail();
            }
        });

        requestQueue.add(collegesRequest);

    }

    private void finishInit(JSONObject response) {
        Log.i(Config.LOG_TAG, response.toString());
        progressDialog.dismiss();
        progressDialog.setCancelable(true);
        progressDialog.setMessage("Signing up...");
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                if (signupRequest != null) {
                    signupRequest.cancel();
                }
            }
        });
        progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                signupButton.setEnabled(true);
            }
        });
    }

    private void onInitFail() {
        progressDialog.dismiss();
        Toast.makeText(this, "An unexpected error occurred. Please try again.", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_CANCELED, null);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean validate() {

        boolean valid = true;

        String name = nameEditText.getText().toString();
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();
        String phone = phoneEditText.getText().toString();

        try {
            Phonenumber.PhoneNumber phoneNumber = phoneUtil.parse(phone, "US");
            if (!phoneUtil.isValidNumber(phoneNumber)) {
                phoneEditText.setError("Please enter a valid phone number");
                valid = false;
            }
        } catch (NumberParseException e) {
            Log.e(Config.LOG_TAG, e.toString());
            phoneEditText.setError("Please enter a valid phone number");
            valid = false;
        }

        if (!confirmPassword.equals(password)) {
            confirmPasswordEditText.setError("Passwords do not match!");
            valid = false;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("Please enter a password");
            valid = false;
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Please enter a valid email address");
            valid = false;
        }

        if (name.isEmpty()) {
            nameEditText.setError("Please enter your name");
            valid = false;
        }

        return valid;

    }

    @SuppressWarnings("WeakerAccess")
    @OnClick(R.id.signupButton)
    protected void doSignup() {

        if (!validate()) {
            return;
        }

        signupButton.setEnabled(false);
        progressDialog.show();

        signupRequest = new StringRequest(Request.Method.POST, Config.SSO_SIGNUP_URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                onSignupRequestSuccess(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(Config.LOG_TAG, error.toString());
                onSignupRequestFail();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("name", nameEditText.getText().toString());
                params.put("email", emailEditText.getText().toString());
                messageDigest.update(passwordEditText.getText().toString().getBytes());
                params.put("pass", Util.bytesToHex(messageDigest.digest()));
                messageDigest.update(confirmPasswordEditText.getText().toString().getBytes());
                params.put("passConf", Util.bytesToHex(messageDigest.digest()));
                params.put("tel", phoneEditText.getText().toString());
                return params;
            }

        };

        requestQueue.add(signupRequest);

    }

    private void onSignupRequestSuccess(String response) {
        Log.i(Config.LOG_TAG, response);
        progressDialog.dismiss();
        Toast.makeText(this, "Signup request successful", Toast.LENGTH_SHORT).show();
    }

    private void onSignupRequestFail() {
        progressDialog.dismiss();
        Toast.makeText(this, "An error occurred during signup", Toast.LENGTH_SHORT).show();
    }

}
