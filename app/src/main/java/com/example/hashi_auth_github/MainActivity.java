package com.example.hashi_auth_github;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GithubAuthProvider;


import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity {

    private static final String REDIRECT_URL_CALLBACK = "github.com/hashinology.oauth2token";
    private final String TAG = getClass().getName();
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private Button btnGit;
    private TextView txtGit;
    private LinearLayout llGit;
    private boolean signed;

    private SecureRandom random = new SecureRandom();
    private ImageView imgGit;

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        initViews();
    }

    private void initViews() {

/*linked views*/
        llGit = (LinearLayout) findViewById(R.id.llGit);
        imgGit = (ImageView) findViewById(R.id.imgGit);
        txtGit = (TextView) findViewById(R.id.txtGit);
        btnGit = (Button) findViewById(R.id.btnGit);
/*checking firebase login status */
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    signed = true;
                    llGit.setVisibility(View.VISIBLE);
                    txtGit.setText(user.getDisplayName() + "\n" + user.getEmail());
//                    Picasso.with(MainActivity.this).load(user.getPhotoUrl()).into(imgGit);
                    btnGit.setText(R.string.sign_out);
                    Toast.makeText(MainActivity.this, R.string.signed_in, Toast.LENGTH_SHORT).show();
                } else {
                    signed = false;
                    llGit.setVisibility(View.GONE);
                    btnGit.setText(R.string.sign_in);
                    Toast.makeText(MainActivity.this, R.string.signed_out, Toast.LENGTH_SHORT).show();
                }
            }
        };


        //Called after the github server redirect us to REDIRECT_URL_CALLBACK
        Uri uri = getIntent().getData();
        if (uri != null && uri.toString().startsWith(REDIRECT_URL_CALLBACK)) {
            String code = uri.getQueryParameter("code");
            String state = uri.getQueryParameter("state");
            if (code != null && state != null)
                sendPost(code, state);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    /*gave here client id from firebase*/
    public void signInOut(View view) {
        if (!signed) {
            //https://developer.github.com/apps/building-integrations/setting-up-and-registering-oauth-apps/about-authorization-options-for-oauth-apps/
            //GET http://github.com/login/oauth/authorize
            HttpUrl httpUrl = new HttpUrl.Builder()
                    .scheme("http")
                    .host("github.com")
                    .addPathSegment("login")
                    .addPathSegment("oauth")
                    .addPathSegment("authorize")
                    .addQueryParameter("client_id", "c8e82e6fbb7d8518094e")
                    .addQueryParameter("redirect_uri", REDIRECT_URL_CALLBACK)
                    .addQueryParameter("state", getRandomString())
                    .addQueryParameter("scope", "user:email")
                    .build();

            Log.d(TAG, httpUrl.toString());

            //Approach 1
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(httpUrl.toString()));
            startActivity(intent);


        } else {
            mAuth.signOut();
        }
    }

    private String getRandomString() {
        return new BigInteger(130, random).toString(32);
    }

    private void sendPost(String code, String state) {
        //POST https://github.com/login/oauth/access_token

        /*gave here client id and secert id from your github account */

        OkHttpClient okHttpClient = new OkHttpClient();
        FormBody form = new FormBody.Builder()
                .add("client_id", "fbfa6c04579b3051aec7")
                .add("client_secret", "debcc760ea8ec320a39a9c5c46d193e4d6c0729d")
                .add("code", code)
                .add("redirect_uri", REDIRECT_URL_CALLBACK)
                .add("state", state)
                .build();

        Request request = new Request.Builder()
                .url("https://github.com/login/oauth/access_token")
                .post(form)
                .build();


        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "onFailure: " + e.toString(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //access_token=e72e16c7e42f292c6912e7710c838347ae178b4a&token_type=bearer
                String responseBody = response.body().string();
                String[] splitted = responseBody.split("=|&");
                if (splitted[0].equalsIgnoreCase("access_token"))
                    signInWithToken(splitted[1]);
                else
                    Toast.makeText(MainActivity.this, "splitted[0] =>" + splitted[0], Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void signInWithToken(String token) {
        AuthCredential credential = GithubAuthProvider.getCredential(token);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                        if (!task.isSuccessful()) {
                            task.getException().printStackTrace();
                            Log.w(TAG, "signInWithCredential", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

}
