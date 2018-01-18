package com.sueztech.ktec.coursewatch;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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

    private PhoneNumberUtil phoneUtil;

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

        Toast.makeText(this, "Validation successful", Toast.LENGTH_SHORT).show();

    }

}
