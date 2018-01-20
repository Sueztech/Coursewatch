package com.sueztech.ktec.coursewatch;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LoginActivity extends AppCompatActivity {

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.emailEditText)
    protected EditText emailEditText;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.passwordEditText)
    protected EditText passEditText;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.loginButton)
    protected Button loginButton;

    private MessageDigest messageDigest;
    private ProgressDialog progressDialog;
    private RequestQueue requestQueue;
    private StringRequest loginRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            Log.wtf(Config.LOG_TAG, e.toString());
        }

        requestQueue = Volley.newRequestQueue(this);

        passEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE) {
                    doLogin();
                    return true;
                }
                return false;
            }
        });

        progressDialog = new ProgressDialog(this, R.style.AppTheme_LoginActivity_ProgressDialog);
        progressDialog.setMessage("Authenticating...");
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                if (loginRequest != null) {
                    loginRequest.cancel();
                }
            }
        });
        progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                loginButton.setEnabled(true);
            }
        });

    }

    private boolean validate() {

        boolean valid = true;

        String email = emailEditText.getText().toString();
        String password = passEditText.getText().toString();

        if (password.isEmpty()) {
            passEditText.setError(getString(R.string.err_400_pass_login));
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

        return valid;

    }

    @SuppressWarnings("WeakerAccess")
    @OnClick(R.id.loginButton)
    protected void doLogin() {

        if (!validate()) {
            return;
        }

        loginButton.setEnabled(false);
        progressDialog.show();

        loginRequest = new StringRequest(Request.Method.POST, Config.SSO_LOGIN_URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                onLoginRequestSuccess(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(Config.LOG_TAG, error.toString());
                onLoginRequestFail();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                messageDigest.update(passEditText.getText().toString().getBytes());
                Map<String, String> params = new HashMap<>();
                params.put("email", emailEditText.getText().toString());
                params.put("pass", Util.bytesToHex(messageDigest.digest()));
                return params;
            }

        };

        requestQueue.add(loginRequest);

    }

    private void onLoginRequestSuccess(String response) {
        Log.i(Config.LOG_TAG, response);
        try {

            JSONObject responseJson = new JSONObject(response);
            int status = responseJson.getInt("status");

            if (status == 200) {
                // TODO: return session ID
                setResult(RESULT_OK);
                finish();
            } else if (status == 401) {
                JSONArray fields = responseJson.getJSONArray("data");
                for (int i = 0; i < fields.length(); i++) {
                    switch (fields.getString(i)) {
                        case "email":
                        case "pass":
                            passEditText.setError(getString(R.string.err_401_email_pass));
                            passEditText.requestFocus();
                            break;
                        case "time":
                            passEditText.setError(getString(R.string.err_401_time));
                            passEditText.requestFocus();
                            break;
                        default:
                            Log.e(Config.LOG_TAG, "Got unknown field " + fields.getString(i) + " in 401 response");
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
            Log.e(Config.LOG_TAG, e.toString());
            onLoginRequestFail();
            return;
        }

        progressDialog.dismiss();

    }

    private void onLoginRequestFail() {
        progressDialog.dismiss();
        Toast.makeText(this, R.string.login_error, Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.signupTextView)
    protected void doSignup() {
        startActivity(new Intent(this, SignupActivity.class));
    }

}
