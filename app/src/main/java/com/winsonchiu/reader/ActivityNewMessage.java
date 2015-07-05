/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.app.Activity;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.squareup.picasso.Picasso;
import com.winsonchiu.reader.data.Reddit;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ActivityNewMessage extends AppCompatActivity {

    private static final String TAG = ActivityNewMessage.class.getCanonicalName();
    private Reddit reddit;

    private Toolbar toolbar;
    private EditText editTextRecipient;
    private EditText editTextSubject;
    private EditText editTextMessage;
    private FloatingActionButton floatingActionButton;

    private RelativeLayout layoutCaptcha;
    private String captchaId;
    private ImageView imageCaptcha;
    private EditText editCaptcha;
    private ImageButton buttonCaptchaRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_message);

        reddit = Reddit.getInstance(this);

        editTextRecipient = (EditText) findViewById(R.id.edit_recipient);
        editTextSubject = (EditText) findViewById(R.id.edit_subject);
        editTextMessage = (EditText) findViewById(R.id.edit_message);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.new_post));
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        floatingActionButton = (FloatingActionButton) findViewById(R.id.fab_submit_post);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitMessage();
            }
        });

        layoutCaptcha = (RelativeLayout) findViewById(R.id.layout_captcha);
        imageCaptcha = (ImageView) findViewById(R.id.image_captcha);
        editCaptcha = (EditText) findViewById(R.id.edit_captcha);
        buttonCaptchaRefresh = (ImageButton) findViewById(R.id.button_captcha_refresh);
        buttonCaptchaRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadCaptcha();
            }
        });

        reddit.loadGet(Reddit.OAUTH_URL + "/api/needs_captcha",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if ("true".equalsIgnoreCase(response)) {
                            layoutCaptcha.setVisibility(View.VISIBLE);
                            loadCaptcha();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }, 0);

    }

    private void submitMessage() {

        String recipient = editTextRecipient.getText().toString();

        if (recipient.startsWith("/u/")) {
            recipient = recipient.substring(3);
        }

        Map<String, String> params = new HashMap<>();
        params.put("api_type", "json");
        params.put("subject", editTextSubject.getText().toString());
        params.put("text", editTextMessage.getText().toString());
        params.put("to", recipient);
        if (!TextUtils.isEmpty(captchaId)) {
            params.put("iden", captchaId);
            params.put("captcha", editCaptcha.getText().toString());
        }

        reddit.loadPost(Reddit.OAUTH_URL + "/api/compose", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Submit new response: " + response);

                try {
                    JSONObject jsonObject = new JSONObject(response).getJSONObject("json");
                    String error = jsonObject.getJSONArray("errors").optString(
                            0);
                    if (!TextUtils.isEmpty(error)) {

                        String captcha = jsonObject.optString("captcha");

                        if (!TextUtils.isEmpty(captcha)) {
                            captchaId = captcha;
                            editCaptcha.setText("");
                            Picasso.with(ActivityNewMessage.this)
                                    .load(Reddit.BASE_URL + "/captcha/" + captchaId + ".png")
                                    .resize(getResources().getDisplayMetrics().widthPixels, 0).into(
                                    imageCaptcha);
                        }

                        Toast.makeText(ActivityNewMessage.this, "Error: " + error, Toast.LENGTH_LONG)
                                .show();
                        return;
                    }
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }

                setResult(Activity.RESULT_OK);
                finish();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(ActivityNewMessage.this, "Error submitting post", Toast.LENGTH_LONG)
                        .show();
            }
        }, params, 0);
    }

    private void loadCaptcha() {

        Map<String, String> params = new HashMap<>();
        params.put("api_type", "json");

        reddit.loadPost(Reddit.OAUTH_URL + "/api/new_captcha", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    captchaId = jsonObject.getJSONObject("json").getJSONObject("data").getString(
                            "iden");
                    Log.d(TAG, "captchaId: " + captchaId);
                    Picasso.with(ActivityNewMessage.this).load(Reddit.BASE_URL + "/captcha/" + captchaId + ".png").resize(getResources().getDisplayMetrics().widthPixels, 0).into(
                            imageCaptcha);
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }, params, 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}