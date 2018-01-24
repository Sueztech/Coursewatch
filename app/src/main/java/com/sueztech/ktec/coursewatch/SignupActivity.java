package com.sueztech.ktec.coursewatch;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Patterns;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Request;

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

@SuppressWarnings("WeakerAccess")
public class SignupActivity extends AppCompatActivity
        implements Requests.ResponseListener<JSONObject>, DialogInterface.OnShowListener,
        DialogInterface.OnCancelListener, DialogInterface.OnDismissListener {

    private static final String TAG = "SignupActivity";

    private static final int RID_COLLEGE_LIST = 1;
    private static final int RID_SIGNUP = 2;

    @BindView(R.id.nameEditText)
    protected EditText nameEditText;

    @BindView(R.id.emailEditText)
    protected EditText emailEditText;

    @BindView(R.id.passwordEditText)
    protected EditText passEditText;

    @BindView(R.id.confirmPasswordEditText)
    protected EditText passConfEditText;

    @BindView(R.id.phoneEditText)
    protected EditText telEditText;

    @BindView(R.id.collegeSpinner)
    protected Spinner collegeSpinner;

    @BindView(R.id.signupButton)
    protected Button signupButton;

    private MessageDigest messageDigest;
    private PhoneNumberUtil phoneUtil;
    private ProgressDialog progressDialog;
    private Request signupRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        ButterKnife.bind(this);

        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            Log.wtf(TAG, e.toString());
        }

        phoneUtil = PhoneNumberUtil.createInstance(this);
        telEditText.addTextChangedListener(new PhoneNumberFormattingTextWatcher(phoneUtil));

        progressDialog = new ProgressDialog(this, R.style.AppTheme_LoginActivity_ProgressDialog);
        progressDialog.setOnShowListener(this);
        progressDialog.setOnCancelListener(this);
        progressDialog.setOnDismissListener(this);

        progressDialog.setMessage(getString(R.string.loading_progress));
        progressDialog.setCancelable(false);
        progressDialog.show();

        Requests.addJsonRequest(RID_COLLEGE_LIST, Config.Urls.Static.COLLEGE_LIST, null, this);

    }

    private void onCollegeListRequestSuccess(JSONObject collegeListResponse) {

        Log.v(TAG, "onCollegeListRequestSuccess");

        ArrayList<College> collegeList = new ArrayList<>();

        try {

            int status = collegeListResponse.getInt("status");
            if (status != 200) {
                throw new JSONException("Unexpected status: " + status);
            }

            JSONArray collegeListJson = collegeListResponse.getJSONArray("data");
            for (int i = 0; i < collegeListJson.length(); i++) {
                collegeList.add(new College(collegeListJson.getJSONObject(i)));
            }

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            onCollegeListRequestFail();
            return;
        }

        collegeSpinner.setAdapter(
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                        collegeList));

        progressDialog.dismiss();
        progressDialog.setCancelable(true);
        progressDialog.setMessage(getString(R.string.signup_progress));

    }

    private void onCollegeListRequestFail() {
        Log.v(TAG, "onCollegeListRequestFail");
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

    private boolean areFieldsValid() {

        Log.v(TAG, "areFieldsValid");

        boolean valid = true;

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
            Log.e(TAG, e.toString());
            telEditText.setError(getString(R.string.err_403_tel));
            telEditText.requestFocus();
            valid = false;
        }

        String pass = passEditText.getText().toString();
        String passConf = passConfEditText.getText().toString();
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

        String name = nameEditText.getText().toString();
        if (name.isEmpty()) {
            nameEditText.setError(getString(R.string.err_40x_name));
            nameEditText.requestFocus();
            valid = false;
        }

        return valid;

    }

    @OnClick(R.id.signupButton)
    protected void doSignup() {

        Log.v(TAG, "doSignup");

        if (!areFieldsValid()) {
            return;
        }

        progressDialog.show();

        Map<String, String> params = new HashMap<>();
        params.put("name", nameEditText.getText().toString());
        params.put("email", emailEditText.getText().toString());
        messageDigest.update(passEditText.getText().toString().getBytes());
        params.put("pass", Utils.bytesToHex(messageDigest.digest()));
        messageDigest.update(passConfEditText.getText().toString().getBytes());
        params.put("passConf", Utils.bytesToHex(messageDigest.digest()));
        params.put("tel", telEditText.getText().toString());
        params.put("college", ((College) collegeSpinner.getSelectedItem()).getId());
        signupRequest = Requests.addJsonRequest(RID_SIGNUP, Config.Urls.Sso.SIGNUP, params, this);

    }

    private void onSignupRequestSuccess(JSONObject response) {

        Log.v(TAG, "onSignupRequestSuccess");

        try {

            switch (response.getInt("status")) {

                case 200:
                    // TODO: show email verification screen
                    Toast.makeText(this, R.string.signup_successful, Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK);
                    finish();
                    break;

                case 406:
                    JSONArray fields = response.getJSONArray("data");
                    for (int i = 0; i < fields.length(); i++) {
                        switch (fields.getString(i)) {
                            case "email":
                                emailEditText.setError(getString(R.string.err_406_email));
                                emailEditText.requestFocus();
                                break;
                            default:
                                Log.e(TAG, "Unknown field (406 response): " + fields.getString(i));
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
            onSignupRequestFail();
            return;
        }

        progressDialog.dismiss();

    }

    private void onSignupRequestFail() {
        Log.v(TAG, "onSignupRequestFail");
        progressDialog.dismiss();
        Toast.makeText(this, R.string.signup_error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResponse(int id, JSONObject response) {
        switch (id) {
            case RID_COLLEGE_LIST:
                onCollegeListRequestSuccess(response);
                break;
            case RID_SIGNUP:
                onSignupRequestSuccess(response);
                break;
        }
    }

    @Override
    public void onError(int id, Exception error) {
        switch (id) {
            case RID_COLLEGE_LIST:
                onCollegeListRequestFail();
                break;
            case RID_SIGNUP:
                onSignupRequestFail();
                break;
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        signupButton.setEnabled(false);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (signupRequest != null) {
            signupRequest.cancel();
        }
    }

    @Override
    public void onShow(DialogInterface dialog) {
        signupButton.setEnabled(true);
    }

}
