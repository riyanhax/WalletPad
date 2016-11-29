package su.gear.walletpad;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.ActionCodeResult;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;
import su.gear.walletpad.fragments.BanksFragment;
import su.gear.walletpad.fragments.BudgetFragment;
import su.gear.walletpad.fragments.ConverterFragment;
import su.gear.walletpad.fragments.ExportFragment;
import su.gear.walletpad.fragments.ImportFragment;
import su.gear.walletpad.fragments.OperationsFragment;
import su.gear.walletpad.fragments.SummaryFragment;
import su.gear.walletpad.fragments.StatisticsFragment;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int RC_SIGN_IN = 9001;

    private DrawerLayout drawer;
    private Button signInButton;
    private RelativeLayout userInfo;

    private SummaryFragment fragment_summary;
    private OperationsFragment fragment_operations;
    private BudgetFragment fragment_budget;
    private StatisticsFragment fragment_statistics;

    private ImportFragment fragment_import;
    private BanksFragment fragment_banks;
    private ConverterFragment fragment_converter;
    private ExportFragment fragment_export;

    private GoogleApiClient mGoogleApiClient;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private RelativeLayout userLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        fragment_summary = new SummaryFragment();
        fragment_operations = new OperationsFragment();
        fragment_budget = new BudgetFragment();
        fragment_statistics = new StatisticsFragment();

        fragment_import = new ImportFragment();
        fragment_banks = new BanksFragment();
        fragment_converter = new ConverterFragment();
        fragment_export = new ExportFragment();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment_summary);
        transaction.commit();


        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Log.d(TAG, "onConnectionFailed:" + connectionResult);
                        Toast.makeText(MainActivity.this, "Google Play Services error", Toast.LENGTH_SHORT).show();
                    }
                })
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();


        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                showUserInfo(firebaseAuth.getCurrentUser());
            }
        };

        View header = navigationView.getHeaderView(0);
        signInButton = (Button) header.findViewById(R.id.sign_in);
        userInfo = (RelativeLayout) header.findViewById(R.id.user);
        userLoader = (RelativeLayout) header.findViewById(R.id.user_loader);

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });
        userInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signOut(true);
            }
        });

        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                if (connected) {
                    Log.e(TAG, "CONNECTED");
                } else {
                    Log.e(TAG, "DISCONNECTED");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                System.err.println("Listener was cancelled");
            }
        });
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.e(TAG, mAuth.getCurrentUser() == null ? "NULL" : "USER");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // outState.putBoolean();
    }

    private void showUserLoader() {
        signInButton.setVisibility(View.GONE);
        userInfo.setVisibility(View.GONE);
        userLoader.setVisibility(View.VISIBLE);
    }

    private void showUserInfo(FirebaseUser user) {
        userLoader.setVisibility(View.GONE);
        if (user == null) {
            signInButton.setVisibility(View.VISIBLE);
            userInfo.setVisibility(View.GONE);
            Log.d(TAG, "onAuthStateChanged:signed_out");
        } else {
            signInButton.setVisibility(View.GONE);
            userInfo.setVisibility(View.VISIBLE);
            ((TextView) userInfo.findViewById(R.id.username)).setText(user.getDisplayName());
            ((TextView) userInfo.findViewById(R.id.sync_info)).setText(user.getEmail());
            Uri photoUrl = user.getPhotoUrl();
            if (photoUrl != null) {
                Glide.with(this).load(photoUrl).into((CircleImageView) userInfo.findViewById(R.id.avatar));
            }
            Log.d(TAG, "onAuthStateChanged:signed_in: " + user.getUid());
        }
    }

    private void signIn() {
        showUserLoader();
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void signOut(boolean force) {
        showUserLoader();
        Toast.makeText(MainActivity.this, "Signing out", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Auth.GoogleSignInApi: Signed Out");
        if (!force) {
            mAuth.signOut();
            return;
        }
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if (status.isSuccess()) {
                    mAuth.signOut();
                    Log.d(TAG, "Auth.GoogleSignInApi: Signed Out");
                } else {
                    showUserInfo(mAuth.getCurrentUser());
                    Toast.makeText(MainActivity.this, "Sign out failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = result.getSignInAccount();
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                mAuth.signInWithCredential(credential)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                                // If sign in fails, display a message to the user. If sign in succeeds
                                // the auth state listener will be notified and logic to handle the
                                // signed in user can be handled in the listener.
                                if (!task.isSuccessful()) {
                                    Log.w(TAG, "signInWithCredential", task.getException());
                                    Toast.makeText(MainActivity.this, "Authentication failed.",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(MainActivity.this, "Authentication success.",
                                            Toast.LENGTH_SHORT).show();
                                    //showUserInfo(mAuth.getCurrentUser());
                                }
                            }
                        });
            } else {
                showUserInfo(mAuth.getCurrentUser());
                Log.e(TAG, "Google Sign In failed.");
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    public void onBackPressed() {
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
            Toast toast = Toast.makeText(getApplicationContext(), "Test", Toast.LENGTH_SHORT);
            toast.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        Fragment newFragment = null;

        if (id == R.id.nav_summary) {
            newFragment = fragment_summary;
        } else if (id == R.id.nav_operations) {
            newFragment = fragment_operations;
        } else if (id == R.id.nav_budget) {
            newFragment = fragment_budget;
        } else if (id == R.id.nav_statistics) {
            newFragment = fragment_statistics;
        } else if (id == R.id.nav_import) {
            newFragment = fragment_import;
        } else if (id == R.id.nav_banks) {
            newFragment = fragment_banks;
        } else if (id == R.id.nav_converter) {
            newFragment = fragment_converter;
        } else if (id == R.id.nav_export) {
            newFragment = fragment_export;
        } else if (id == R.id.nav_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            /*EditText editText = (EditText) findViewById(R.id.edit_message);
            String message = editText.getText().toString();
            intent.putExtra(EXTRA_MESSAGE, message);*/
            startActivity(intent);
            drawer.closeDrawer(GravityCompat.START);
            return true;
        } else if (id == R.id.nav_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            /*EditText editText = (EditText) findViewById(R.id.edit_message);
            String message = editText.getText().toString();
            intent.putExtra(EXTRA_MESSAGE, message);*/
            startActivity(intent);
            drawer.closeDrawer(GravityCompat.START);
            return true;
        } else {
            newFragment = fragment_summary;
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, newFragment);
        transaction.commit();

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}