package com.sueztech.ktec.coursewatch;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.support.annotation.NonNull;
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
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

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

public class LoginActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

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

    private GoogleApiClient mGoogleApiClient;
    private boolean mIsResolving;

    private static final String TAG = "LoginActivity";
    private static final int RC_READ = 1;
    private static final int RC_SAVE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, 0, this)
                .addApi(Auth.CREDENTIALS_API)
                .build();

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

                String username = emailEditText.getText().toString();
                String password = passEditText.getText().toString();
                Credential credential = new Credential.Builder(username)
                        .setPassword(password)
                        .build();
                saveCredential(credential);

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

    private void saveCredential(Credential credential) {
        // Credential is valid so save it.
        Auth.CredentialsApi.save(mGoogleApiClient,
                credential).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if (status.isSuccess()) {
                    Log.d(Config.LOG_TAG, "Credential saved");
                    goToContent();
                } else {
                    Log.d(Config.LOG_TAG, "Attempt to save credential failed " +
                            status.getStatusMessage() + " " +
                            status.getStatusCode());
                    resolveResult(status, RC_SAVE);
                }
            }
        });
    }

    private void goToContent() {
        // TODO: return session ID
        setResult(RESULT_OK);
        finish();
    }

    private void resolveResult(Status status, int requestCode) {
        // We don't want to fire multiple resolutions at once since that can result
        // in stacked dialogs after rotation or another similar event.
        if (mIsResolving) {
            Log.w(TAG, "resolveResult: already resolving.");
            return;
        }

        Log.d(TAG, "Resolving: " + status);
        if (status.hasResolution()) {
            Log.d(TAG, "STATUS: RESOLVING");
            try {
                status.startResolutionForResult(this, requestCode);
                mIsResolving = true;
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "STATUS: Failed to send resolution.", e);
            }
        } else {
            goToContent();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(Config.LOG_TAG, "onActivityResult:" + requestCode + ":" + resultCode + ":" +
                data);
        if (requestCode == RC_READ) {
            if (resultCode == RESULT_OK) {
                Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                processRetrievedCredential(credential);
            } else {
                Log.e(TAG, "Credential Read: NOT OK");
//                setSignInEnabled(true);
            }
        } else if (requestCode == RC_SAVE) {
            Log.d(Config.LOG_TAG, "Result code: " + resultCode);
            if (resultCode == RESULT_OK) {
                Log.d(Config.LOG_TAG, "Credential Save: OK");
            } else {
                Log.e(Config.LOG_TAG, "Credential Save Failed");
            }
            goToContent();
        }
        mIsResolving = false;
    }

    private void onLoginRequestFail() {
        progressDialog.dismiss();
        Toast.makeText(this, R.string.login_error, Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.signupTextView)
    protected void doSignup() {
        startActivity(new Intent(this, SignupActivity.class));
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(Config.LOG_TAG, "onConnected");
        requestCredentials();
    }

    private void requestCredentials() {
//        setSignInEnabled(false);

        CredentialRequest request = new CredentialRequest.Builder()
                .setPasswordLoginSupported(true)
                .build();

        Auth.CredentialsApi.request(mGoogleApiClient, request).setResultCallback(
                new ResultCallback<CredentialRequestResult>() {
                    @Override
                    public void onResult(@NonNull CredentialRequestResult credentialRequestResult) {
                        Status status = credentialRequestResult.getStatus();
                        if (credentialRequestResult.getStatus().isSuccess()) {
                            // Successfully read the credential without any user interaction, this
                            // means there was only a single credential and the user has auto
                            // sign-in enabled.
                            Credential credential = credentialRequestResult.getCredential();
                            processRetrievedCredential(credential);
                        } else if (status.getStatusCode() == CommonStatusCodes.RESOLUTION_REQUIRED) {
//                            setFragment(null);
                            // This is most likely the case where the user has multiple saved
                            // credentials and needs to pick one.
                            resolveResult(status, RC_READ);
                        } else if (status.getStatusCode() == CommonStatusCodes.SIGN_IN_REQUIRED) {
//                            setFragment(null);
                            // This is most likely the case where the user does not currently
                            // have any saved credentials and thus needs to provide a username
                            // and password to sign in.
                            Log.d(Config.LOG_TAG, "Sign in required");
//                            setSignInEnabled(true);
                        } else {
                            Log.w(Config.LOG_TAG, "Unrecognized status code: " + status.getStatusCode());
//                            setFragment(null);
//                            setSignInEnabled(true);
                        }
                    }
                }
        );

    }

    private void processRetrievedCredential(Credential credential) {
        emailEditText.setText(credential.getId());
        passEditText.setText(credential.getPassword());
        doLogin();
//        if (Util.isValidCredential(credential)) {
//            goToContent();
//        } else {
        // This is likely due to the credential being changed outside of
        // Smart Lock,
        // ie: away from Android or Chrome. The credential should be deleted
        // and the user allowed to enter a valid credential.
//            Log.d(TAG, "Retrieved credential invalid, so delete retrieved" +
//                    " credential.");
//        }
    }


    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(Config.LOG_TAG, "onConnectionSuspended: " + cause);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(Config.LOG_TAG, "onConnectionFailed: " + connectionResult);
    }

}
