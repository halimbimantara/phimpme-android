package org.fossasia.phimpme.accounts;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterAuthClient;

import org.fossasia.phimpme.R;
import org.fossasia.phimpme.base.PhimpmeProgressBarHandler;
import org.fossasia.phimpme.base.RecyclerItemClickListner;
import org.fossasia.phimpme.base.ThemedActivity;
import org.fossasia.phimpme.data.local.AccountDatabase;
import org.fossasia.phimpme.data.local.DatabaseHelper;
import org.fossasia.phimpme.leafpic.util.ThemeHelper;
import org.fossasia.phimpme.sharedrupal.DrupalLogin;
import org.fossasia.phimpme.utilities.ActivitySwitchHelper;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmQuery;

import static android.support.design.widget.Snackbar.LENGTH_LONG;

/**
 * Created by pa1pal on 13/6/17.
 */

public class AccountActivity extends ThemedActivity implements AccountContract.View,
        RecyclerItemClickListner.OnItemClickListener {

    private Toolbar toolbar;
    private ThemeHelper themeHelper;
    private RecyclerView accountsRecyclerView;
    private AccountAdapter accountAdapter;
    private AccountPresenter accountPresenter;
    private Realm realm = Realm.getDefaultInstance();
    private RealmQuery<AccountDatabase> realmResult;
    private PhimpmeProgressBarHandler phimpmeProgressBarHandler;
    private TwitterAuthClient client;
    private CallbackManager callbackManager;
    private LoginManager loginManager;
    private AccountDatabase account;
    private DatabaseHelper databaseHelper;
    private Context context;
    public String[] accountsList = {"Twitter", "Facebook", "Drupal"};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        themeHelper = new ThemeHelper(this);
        accountAdapter = new AccountAdapter();
        accountPresenter = new AccountPresenter(realm);
        phimpmeProgressBarHandler = new PhimpmeProgressBarHandler(this);
        accountPresenter.attachView(this);
        databaseHelper = new DatabaseHelper(realm);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        accountsRecyclerView = (RecyclerView) findViewById(R.id.accounts_recycler_view);
        client = new TwitterAuthClient();
        callbackManager = CallbackManager.Factory.create();
        setSupportActionBar(toolbar);
        loginManager = LoginManager.getInstance();
        toolbar.setPopupTheme(themeHelper.getPopupToolbarStyle());
        toolbar.setBackgroundColor(themeHelper.getPrimaryColor());
        setUpRecyclerView();
        // Calling presenter function to load data from database
        accountPresenter.loadFromDatabase();
        getSupportActionBar().setTitle(R.string.title_account);
        //toolbar.getPopupTheme() = themeHelper.getPopupToolbarStyle();
        toolbar.setBackgroundColor(themeHelper.getPrimaryColor());
        getSupportActionBar().setTitle(R.string.title_account);
        phimpmeProgressBarHandler.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_accounts_activity, menu);
        return true;
    }

    @Override
    public void setUpRecyclerView() {
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        accountsRecyclerView.setLayoutManager(layoutManager);
        accountsRecyclerView.setAdapter(accountAdapter);

        accountsRecyclerView.addOnItemTouchListener(new RecyclerItemClickListner(this, this));
    }

    @Override
    public void setUpAdapter(@NotNull RealmQuery<AccountDatabase> accountDetails) {
        this.realmResult = accountDetails;
        accountAdapter.setResults(realmResult);
    }

    @Override
    public void showError() {
        Toast.makeText(this, "No account signed in", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showComplete() {
        phimpmeProgressBarHandler.hide();
    }

    @Override
    public int getContentViewId() {
        return R.layout.activity_accounts;
    }

    @Override
    public int getNavigationMenuItemId() {
        return R.id.navigation_accounts;
    }

    @Override
    public void onItemClick(final View childView, final int position) {
        final Switch signInSignOut = (Switch) childView.findViewById(R.id.sign_in_sign_out_switch);

       /* boolean isSignedIn = realmResult.equalTo("name"
                , accountsList[position]).isValid();
        boolean isChecked = signInSignOut.isChecked();*/
        //String name = realmResult.equalTo("name", accountsList[position]).findAll().get(0).getName();

        if (!signInSignOut.isChecked()) {
            switch (position) {
                case 0:
                    signInTwitter();
                    accountPresenter.loadFromDatabase();
                    break;

                case 1:
                    FacebookSdk.sdkInitialize(this);
                    //signInFacebook(childView);
                    break;

                case 3:
                    Intent drupalShare = new Intent(getContext(), DrupalLogin.class);
                    startActivity(drupalShare);
                    break;

                default:
                    Toast.makeText(this, R.string.feature_not_present,
                            Toast.LENGTH_SHORT).show();
            }
        } else {
            new AlertDialog.Builder(this)
                    .setMessage(accountsList[position])
                    .setTitle(getString(R.string.sign_out_dialog_title))
                    .setPositiveButton(R.string.yes_action,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    databaseHelper
                                            .deleteSignedOutAccount(accountsList[position]);
                                    accountAdapter.notifyDataSetChanged();
                                    accountPresenter.loadFromDatabase();
                                    signInSignOut.setChecked(false);
                                }
                            })
                    .setNegativeButton(R.string.no_action,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //TODO: Implement negative button action
                                }
                            })
                    .show();
        }
    }

    @Override
    public void onItemLongPress(View childView, int position) {
        // TODO: long press to implemented
    }

    /**
     * Create twitter login and session
     */
    public void signInTwitter() {
        /**
         * When user clicks then we first check if it is already exist.
         */
        if (accountPresenter.checkAlreadyExist(accountsList[0])) {
            Toast.makeText(this, R.string.already_signed_in,
                    Toast.LENGTH_SHORT).show();
        } else {
            client.authorize(this, new Callback<TwitterSession>() {
                @Override
                public void success(Result<TwitterSession> result) {

                    // Begin realm transaction
                    realm.beginTransaction();

                    // Creating Realm object for AccountDatabase Class
                    account = realm.createObject(AccountDatabase.class,
                            accountsList[0]);

                    // Creating twitter session, after user authenticate
                    // in twitter popup
                    TwitterSession session = TwitterCore.getInstance()
                            .getSessionManager().getActiveSession();
                    Log.d("Twitter Credentials", session.toString());


                    // Writing values in Realm database
                    account.setUsername(session.getUserName());
                    account.setToken(String.valueOf(session.getAuthToken()));

                    // Finally committing the whole data
                    realm.commitTransaction();
                }

                @Override
                public void failure(TwitterException e) {
                    // TODO: implement on failure
                }
            });
        }
    }

    /**
     * Create Facebook login and session
     *
     * @param childView
     */
    public void signInFacebook(final View childView) {
        if (accountPresenter.checkAlreadyExist(accountsList[1])) {
            Toast.makeText(this, R.string.already_signed_in,
                    Toast.LENGTH_SHORT).show();
        } else {
            List<String> permissionNeeds = Arrays.asList("publish_actions");

            loginManager.logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
            loginManager.logInWithPublishPermissions(this, permissionNeeds);

            loginManager.registerCallback(callbackManager,
                    new FacebookCallback<LoginResult>() {
                        @Override
                        public void onSuccess(LoginResult loginResult) {
                            // Begin realm transaction
                            realm.beginTransaction();

                            // Creating Realm object for AccountDatabase Class
                            account = realm.createObject(AccountDatabase.class,
                                    accountsList[1]);

                            // Writing values in Realm database
                            account.setUsername(loginResult
                                    .getAccessToken().getUserId());
                            account.setToken(String.valueOf(loginResult
                                    .getAccessToken().getToken()));

                            GraphRequest.newMeRequest(
                                    loginResult.getAccessToken(),
                                    new GraphRequest.GraphJSONObjectCallback() {
                                        @Override
                                        public void onCompleted(JSONObject jsonObject
                                                , GraphResponse graphResponse) {
                                                                /*try {
                                                                    account.setName(jsonObject
                                                                            .getString("name"));
                                                                } catch (JSONException e) {
                                                                    e.printStackTrace();
                                                                }*/
                                        }
                                    });

                            // Finally committing the whole data
                            realm.commitTransaction();

                        }

                        @Override
                        public void onCancel() {
                            Snackbar.make(childView,
                                    getString(R.string.facebook_login_cancel), LENGTH_LONG).show();
                        }

                        @Override
                        public void onError(FacebookException e) {
                            Snackbar.make(childView,
                                    getString(R.string.facebook_login_error), LENGTH_LONG).show();

                            Log.d("error", e.toString());
                        }
                    });
        }
    }

    @Override
    public Context getContext() {
        this.context = this;
        return context;
    }

    @Override
    public void onResume() {
        super.onResume();
        ActivitySwitchHelper.setContext(this);
        setNavigationBarColor(ThemeHelper.getPrimaryColor(this));
        setStatusBarColor();
        accountPresenter.loadFromDatabase();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        client.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}
