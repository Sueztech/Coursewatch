package com.sueztech.ktec.coursewatch;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
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

@SuppressWarnings("WeakerAccess")
public class LoginActivity extends AppCompatActivity
        implements DialogInterface.OnShowListener, DialogInterface.OnCancelListener,
        DialogInterface.OnDismissListener, Requests.ResponseListener<JSONObject>,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "LoginActivity";

    private static final int RC_READ = 1;
    private static final int RC_SAVE = 2;

    private static final int RID_LOGIN = 1;

    @BindView(R.id.emailEditText)
    protected EditText emailEditText;

    @BindView(R.id.passwordEditText)
    protected EditText passEditText;

    @BindView(R.id.loginButton)
    protected Button loginButton;

    private MessageDigest messageDigest;
    private ProgressDialog progressDialog;
    private Request loginRequest;

    private GoogleApiClient mGoogleApiClient;
    private boolean mIsResolving;

    private String mSessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            Log.wtf(TAG, e.toString());
        }

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
        progressDialog.setMessage(getString(R.string.login_progress));
        progressDialog.setOnShowListener(this);
        progressDialog.setOnCancelListener(this);
        progressDialog.setOnDismissListener(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                .enableAutoManage(this, 0, this).addApi(Auth.CREDENTIALS_API).build();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_READ) {
            if (resultCode == RESULT_OK) {
                Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                processRetrievedCredential(credential);
            }
        } else if (requestCode == RC_SAVE) {
            goToContent();
        }
        mIsResolving = false;
    }

    private boolean areFieldsValid() {

        Log.v(TAG, "areFieldsValid");

        boolean valid = true;

        String password = passEditText.getText().toString();
        if (password.isEmpty()) {
            passEditText.setError(getString(R.string.err_400_pass_login));
            passEditText.requestFocus();
            valid = false;
        }

        String email = emailEditText.getText().toString();
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

    @OnClick(R.id.loginButton)
    protected void doLogin() {

        Log.v(TAG, "doLogin");

        if (!areFieldsValid()) {
            return;
        }

        progressDialog.show();

        Map<String, String> params = new HashMap<>();
        params.put("email", emailEditText.getText().toString());
        messageDigest.update(passEditText.getText().toString().getBytes());
        params.put("pass", Utils.bytesToHex(messageDigest.digest()));
        loginRequest = Requests.addJsonRequest(RID_LOGIN, Config.Urls.Sso.LOGIN, params, this);

    }

    @OnClick(R.id.signupTextView)
    protected void doSignup() {
        Log.v(TAG, "doSignup");
        startActivity(new Intent(this, SignupActivity.class));
    }

    private void onLoginRequestSuccess(JSONObject response) {

        Log.v(TAG, "onLoginRequestSuccess");

        try {

            Credential credential = new Credential.Builder(emailEditText.getText().toString())
                    .setPassword(passEditText.getText().toString()).build();

            switch (response.getInt("status")) {

                case 200:
                    mSessionId = response.getString("data");
                    saveCredential(credential);
                    break;

                case 401:
                    JSONArray fields = response.getJSONArray("data");
                    for (int i = 0; i < fields.length(); i++) {
                        switch (fields.getString(i)) {
                            case "email":
                            case "pass":
                                passEditText.setError(getString(R.string.err_401_email_pass));
                                passEditText.requestFocus();
                                deleteCredential(credential);
                                requestCredentials();
                                break;
                            case "time":
                                passEditText.setError(getString(R.string.err_401_time));
                                passEditText.requestFocus();
                                break;
                            default:
                                Log.e(TAG, "Unknown field (401 response): " + fields.getString(i));
                                break;
                        }
                    }
                    break;

                case 500:
                    throw new JSONException("Server error: " + response.get("data"));
                default:
                    throw new JSONException("Unexpected status: " + response.getInt("status"));

            }

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            onLoginRequestFail();
            return;
        }

        progressDialog.dismiss();

    }

    private void onLoginRequestFail() {
        Log.v(TAG, "onLoginRequestSuccess");
        progressDialog.dismiss();
        Toast.makeText(this, R.string.login_error, Toast.LENGTH_SHORT).show();
    }

    private void requestCredentials() {

        Log.d(TAG, "requestCredentials");

        CredentialRequest request = new CredentialRequest.Builder().setPasswordLoginSupported(true)
                .build();

        Auth.CredentialsApi.request(mGoogleApiClient, request)
                .setResultCallback(new ResultCallback<CredentialRequestResult>() {
                    @Override
                    public void onResult(@NonNull CredentialRequestResult credentialRequestResult) {
                        Status status = credentialRequestResult.getStatus();
                        if (status.isSuccess()) {
                            processRetrievedCredential(credentialRequestResult.getCredential());
                        } else if (status.getStatusCode()
                                == CommonStatusCodes.RESOLUTION_REQUIRED) {
                            resolveResult(status, RC_READ);
                        } else if (status.getStatusCode() == CommonStatusCodes.SIGN_IN_REQUIRED) {
                            Log.d(TAG, "Sign in required");
                        } else {
                            Log.w(TAG, "Unexpected status: " + status.getStatusCode());
                        }
                    }
                });

    }

    private void resolveResult(Status status, int requestCode) {

        Log.d(TAG, "resolveResult: " + status + ", " + requestCode);

        if (mIsResolving) {
            Log.w(TAG, "resolveResult: already resolving");
            return;
        }

        if (status.hasResolution()) {
            try {
                status.startResolutionForResult(this, requestCode);
                mIsResolving = true;
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            goToContent();
        }

    }

    private void processRetrievedCredential(Credential credential) {
        Log.d(TAG, "processRetrievedCredential: " + credential);
        emailEditText.setText(credential.getId());
        passEditText.setText(credential.getPassword());
        doLogin();
    }

    private void saveCredential(Credential credential) {
        Log.d(TAG, "saveCredential: " + credential);
        Auth.CredentialsApi.save(mGoogleApiClient, credential)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            goToContent();
                        } else {
                            Log.e(TAG, "Could not save credential: " + status.getStatusCode() + " "
                                    + status.getStatusMessage());
                            resolveResult(status, RC_SAVE);
                        }
                    }
                });
    }

    private void deleteCredential(Credential credential) {
        Log.d(TAG, "deleteCredential: " + credential);
        Auth.CredentialsApi.delete(mGoogleApiClient, credential);
    }

    private void goToContent() {
        Log.v(TAG, "goToContent");
        setResult(RESULT_OK, new Intent().putExtra("sessionId", mSessionId));
        finish();
    }

    @Override
    public void onShow(DialogInterface dialog) {
        loginButton.setEnabled(false);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (loginRequest != null) {
            loginRequest.cancel();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        loginButton.setEnabled(true);
    }


    @Override
    public void onResponse(int id, JSONObject response) {
        onLoginRequestSuccess(response);
    }

    @Override
    public void onError(int id, Exception error) {
        onLoginRequestFail();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.v(TAG, "onConnected");
        requestCredentials();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended: " + cause);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: " + connectionResult);
    }

}
