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
    protected EditText passwordEditText;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.loginButton)
    protected Button loginButton;

    private MessageDigest messageDigest;
    private ProgressDialog progressDialog;
    private RequestQueue requestQueue;
    private StringRequest loginRequest;

    private static final int REQUEST_SIGNUP = 1;

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

        passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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
        String password = passwordEditText.getText().toString();

        if (password.isEmpty()) {
            passwordEditText.setError(getString(R.string.err_400_pass_login));
            passwordEditText.requestFocus();
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
                messageDigest.update(passwordEditText.getText().toString().getBytes());
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
        progressDialog.dismiss();
        Toast.makeText(this, "Authentication request successful", Toast.LENGTH_SHORT).show();
    }

    private void onLoginRequestFail() {
        progressDialog.dismiss();
        Toast.makeText(this, "An error occurred during authentication", Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.signupTextView)
    protected void doSignup() {
        startActivityForResult(new Intent(this, SignupActivity.class), REQUEST_SIGNUP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SIGNUP) {
            if (resultCode == RESULT_OK) {
                this.finish();
            }
        }
    }

}
