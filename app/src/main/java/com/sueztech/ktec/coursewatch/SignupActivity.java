package com.sueztech.ktec.coursewatch;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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
    protected EditText passEditText;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.confirmPasswordEditText)
    protected EditText passConfEditText;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.phoneEditText)
    protected EditText telEditText;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.collegeSpinner)
    protected Spinner collegeSpinner;

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
        telEditText.addTextChangedListener(new PhoneNumberFormattingTextWatcher(phoneUtil));
//        telEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
//                if (i == EditorInfo.IME_ACTION_DONE) {
//                    doSignup();
//                    return true;
//                }
//                return false;
//            }
//        });

        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            Log.wtf(Config.TAG, e.toString());
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
                Log.e(Config.TAG, error.toString());
                onInitFail();
            }
        });

        requestQueue.add(collegesRequest);

    }

    private void finishInit(JSONObject response) {

        Log.i(Config.TAG, response.toString());

        ArrayList<College> collegeArrayList = new ArrayList<>();

        try {

            if (response.getInt("status") != 200) {
                throw new JSONException("Got unexpected status " + response.get("status"));
            }
            JSONArray collegeJSONArray = response.getJSONArray("data");

            for (int i = 0; i < collegeJSONArray.length(); i++) {
                collegeArrayList.add(new College(collegeJSONArray.getJSONObject(i)));
            }

        } catch (JSONException e) {
            Log.e(Config.TAG, e.toString());
            onInitFail();
            return;
        }

        collegeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, collegeArrayList));

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
        Toast.makeText(this, R.string.unexpected_error, Toast.LENGTH_SHORT).show();
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
        String pass = passEditText.getText().toString();
        String passConf = passConfEditText.getText().toString();
        String tel = telEditText.getText().toString();

        if (tel.isEmpty()) {
            telEditText.setError(getString(R.string.err_400_tel));
            telEditText.requestFocus();
            valid = false;
        }
        try {
            Phonenumber.PhoneNumber phoneNumber = phoneUtil.parse(tel, "US");
            if (!phoneUtil.isValidNumber(phoneNumber)) {
                throw new NumberParseException(NumberParseException.ErrorType.NOT_A_NUMBER, "");
            }
        } catch (NumberParseException e) {
            Log.e(Config.TAG, e.toString());
            telEditText.setError(getString(R.string.err_403_tel));
            telEditText.requestFocus();
            valid = false;
        }

        if (passConf.isEmpty()) {
            passConfEditText.setError(getString(R.string.err_400_passConf));
            passConfEditText.requestFocus();
            valid = false;
        } else if (!passConf.equals(pass)) {
            passConfEditText.setError(getString(R.string.err_403_passConf));
            passConfEditText.requestFocus();
            valid = false;
        }

        if (pass.isEmpty()) {
            passEditText.setError(getString(R.string.err_400_pass_signup));
            passEditText.requestFocus();
            valid = false;
        }

        if (email.isEmpty()) {
            emailEditText.setError(getString(R.string.err_400_email));
            emailEditText.requestFocus();
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError(getString(R.string.err_403_email));
            emailEditText.requestFocus();
            valid = false;
        }

        if (name.isEmpty()) {
            nameEditText.setError(getString(R.string.err_40x_name));
            nameEditText.requestFocus();
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
                Log.e(Config.TAG, error.toString());
                onSignupRequestFail();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("name", nameEditText.getText().toString());
                params.put("email", emailEditText.getText().toString());
                messageDigest.update(passEditText.getText().toString().getBytes());
                params.put("pass", Util.bytesToHex(messageDigest.digest()));
                messageDigest.update(passConfEditText.getText().toString().getBytes());
                params.put("passConf", Util.bytesToHex(messageDigest.digest()));
                params.put("tel", telEditText.getText().toString());
                params.put("college", ((College) collegeSpinner.getSelectedItem()).getId());
                return params;
            }

        };

        requestQueue.add(signupRequest);

    }

    private void onSignupRequestSuccess(String response) {

        Log.i(Config.TAG, response);

        try {

            JSONObject responseJson = new JSONObject(response);
            int status = responseJson.getInt("status");

            if (status == 200) {
                // TODO: show email verification screen
                Toast.makeText(this, R.string.signup_successful, Toast.LENGTH_LONG).show();
                setResult(RESULT_OK);
                finish();
            } else if (status == 406) {
                JSONArray fields = responseJson.getJSONArray("data");
                for (int i = 0; i < fields.length(); i++) {
                    switch (fields.getString(i)) {
                        case "email":
                            emailEditText.setError(getString(R.string.err_406_email));
                            emailEditText.requestFocus();
                            break;
                        default:
                            Log.e(Config.TAG, "Got unknown field " + fields.getString(i) + " in 406 response");
                            break;
                    }
                }
            } else if (status == 500) {
                // Exception occurred on the server
                throw new JSONException("Server error: " + responseJson.get("data"));
            } else {
                throw new JSONException("Got unexpected status " + status);
            }

        } catch (JSONException e) {
            Log.e(Config.TAG, e.toString());
            onSignupRequestFail();
            return;
        }

        progressDialog.dismiss();

    }

    private void onSignupRequestFail() {
        progressDialog.dismiss();
        Toast.makeText(this, R.string.signup_error, Toast.LENGTH_SHORT).show();
    }

}
