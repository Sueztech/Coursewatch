package com.sueztech.ktec.coursewatch;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LoginActivity extends AppCompatActivity {

    @BindView(R.id.emailEditText)
    EditText emailEditText;
    @BindView(R.id.passwordEditText)
    EditText passwordEditText;
    @BindView(R.id.loginButton)
    Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

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

    }

    @OnClick(R.id.loginButton)
    protected void doLogin() {

        if (!validate()) {
            return;
        }

        loginButton.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(this, R.style.AppTheme_LoginActivity_ProgressDialog);
        progressDialog.setMessage("Authenticating...");
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                               @Override
                                               public void onCancel(DialogInterface dialogInterface) {
                                                   // TODO: Abort authentication
                                                   loginButton.setEnabled(true);
                                               }
                                           }
        );
        progressDialog.show();

        // TODO: Do authentication

        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        loginButton.setEnabled(true);
                        progressDialog.dismiss();
                    }
                }, 3000);

    }

    @OnClick(R.id.signupTextView)
    protected void doSignup() {
        Toast.makeText(this, "Signing up...", Toast.LENGTH_SHORT).show();
    }

    private boolean validate() {

        boolean valid = true;

        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Please enter a valid email address");
            valid = false;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("Please enter your password");
            valid = false;
        }

        return valid;

    }

}
