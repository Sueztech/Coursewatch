package com.sueztech.ktec.coursewatch;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        Requests.ResponseListener<JSONObject> {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_LOGIN = 1;
    private static final int STATUS_REQUEST = 1;

    private GoogleApiClient mGoogleApiClient;

    private String sessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme_NoActionBar);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Requests.initQueue(this);

        startActivityForResult(new Intent(this, LoginActivity.class), REQUEST_LOGIN);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LOGIN) {
            if (resultCode == RESULT_OK) {
                sessionId = data.getStringExtra("sessionId");
                finishInit();
            } else {
                finish();
            }
        }
    }

    private void finishInit() {

        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                .enableAutoManage(this, 0, this).addApi(Auth.CREDENTIALS_API).build();

        Map<String, String> params = new HashMap<>();
        params.put("session", sessionId);
        Requests.addJsonRequest(STATUS_REQUEST, Config.Urls.Sso.STATUS, params, this);

    }

    private void onStatusRequestSuccess(JSONObject response) {
        try {
            switch (response.getInt("status")) {
                case 200:
                    getUserData();
                    break;
                case 400:
                    throw new JSONException("Got status 400, invalid session ID");
                default:
                    throw new JSONException("Unexpected status: " + response.getInt("status"));
            }
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            onStatusRequestFail();
        }
    }

    private void onStatusRequestFail() {
        Toast.makeText(this, R.string.unexpected_error, Toast.LENGTH_SHORT).show();
        startActivityForResult(new Intent(this, LoginActivity.class), REQUEST_LOGIN);
    }

    private void getUserData() {
        Snackbar.make(findViewById(R.id.fab), sessionId, Snackbar.LENGTH_INDEFINITE).show();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_logout) {
            Auth.CredentialsApi.disableAutoSignIn(mGoogleApiClient);
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "GoogleApiClient connected");
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "GoogleApiClient is suspended with cause code: " + cause);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "GoogleApiClient failed to connect: " + connectionResult);
    }

    @Override
    public void onResponse(int id, JSONObject response) {
        switch (id) {
            case STATUS_REQUEST:
                onStatusRequestSuccess(response);
                break;
        }
    }

    @Override
    public void onError(int id, Exception error) {
        switch (id) {
            case STATUS_REQUEST:
                onStatusRequestFail();
                break;
        }
    }
}
