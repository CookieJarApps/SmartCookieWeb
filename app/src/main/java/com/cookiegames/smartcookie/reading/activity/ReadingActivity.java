// Copyright 2020 CookieJarApps MPL
package com.cookiegames.smartcookie.reading.activity;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import javax.inject.Inject;

import com.cookiegames.smartcookie.R;
import com.cookiegames.smartcookie.di.Injector;
import com.cookiegames.smartcookie.di.MainScheduler;
import com.cookiegames.smartcookie.di.NetworkScheduler;
import com.cookiegames.smartcookie.dialog.BrowserDialog;
import com.cookiegames.smartcookie.preference.UserPreferences;
import com.cookiegames.smartcookie.utils.ThemeUtils;
import com.cookiegames.smartcookie.utils.Utils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import net.dankito.readability4j.Article;
import net.dankito.readability4j.Readability4J;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;

public class ReadingActivity extends AppCompatActivity {

    private static final String LOAD_READING_URL = "ReadingUrl";

    /**
     * Launches this activity with the necessary URL argument.
     *
     * @param context The context needed to launch the activity.
     * @param url     The URL that will be loaded into reading mode.
     */
    public static void launch(@NonNull Context context, @NonNull String url) {
        final Intent intent = new Intent(context, ReadingActivity.class);
        intent.putExtra(LOAD_READING_URL, url);
        context.startActivity(intent);
    }

    private static final String TAG = "ReadingActivity";

    @BindView(R.id.textViewTitle) TextView mTitle;
    @BindView(R.id.textViewBody) TextView mBody;

    @Inject UserPreferences mUserPreferences;
    @Inject @NetworkScheduler Scheduler mNetworkScheduler;
    @Inject @MainScheduler Scheduler mMainScheduler;

    private boolean mInvert;
    @Nullable private String mUrl = null;
    private int mTextSize;
    @Nullable private ProgressDialog mProgressDialog;
    private Disposable mPageLoaderSubscription;

    private static final float XXLARGE = 30.0f;
    private static final float XLARGE = 26.0f;
    private static final float LARGE = 22.0f;
    private static final float MEDIUM = 18.0f;
    private static final float SMALL = 14.0f;
    private static final float XSMALL = 10.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Injector.getInjector(this).inject(this);

        overridePendingTransition(R.anim.slide_in_from_right, R.anim.fade_out_scale);
        mInvert = mUserPreferences.getInvertColors();
        final int color;
        if (mInvert) {
            setTheme(R.style.Theme_SettingsTheme_Black);
            color = ThemeUtils.getPrimaryColorDark(this);
            getWindow().setBackgroundDrawable(new ColorDrawable(color));
        } else {
            setTheme(R.style.Theme_SettingsTheme);
            color = ThemeUtils.getPrimaryColor(this);
            getWindow().setBackgroundDrawable(new ColorDrawable(color));
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reading_view);
        ButterKnife.bind(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mTextSize = mUserPreferences.getReadingTextSize();
        mBody.setTextSize(getTextSize(mTextSize));
        mTitle.setText(getString(R.string.untitled));
        mBody.setText(getString(R.string.loading));

        mTitle.setVisibility(View.INVISIBLE);
        mBody.setVisibility(View.INVISIBLE);

        Intent intent = getIntent();
        try {
            if (!loadPage(intent)) {
                //setText(getString(R.string.untitled), getString(R.string.loading_failed));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static float getTextSize(int size) {
        switch (size) {
            case 0:
                return XSMALL;
            case 1:
                return SMALL;
            case 2:
                return MEDIUM;
            case 3:
                return LARGE;
            case 4:
                return XLARGE;
            case 5:
                return XXLARGE;
            default:
                return MEDIUM;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.reading, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private class loadData extends AsyncTask<Void, Void, Void> {
        String extractedContentHtml;
        String extractedContentHtmlWithUtf8Encoding;
        String extractedContentPlainText;
        String title;
        String byline;
        String excerpt;
        @Override
        protected Void doInBackground(Void... voids) {
            URL url;
            try {
                URL google = new URL(mUrl);
                BufferedReader in = new BufferedReader(new InputStreamReader(google.openStream()));
                String input;
                StringBuffer stringBuffer = new StringBuffer();
                while ((input = in.readLine()) != null)
                {
                    stringBuffer.append(input);
                }
                in.close();
                String htmlData = stringBuffer.toString();

                Readability4J readability4J = new Readability4J(mUrl, htmlData); // url is just needed to resolve relative urls
                Article article = readability4J.parse();

                extractedContentHtml = article.getContent();
                extractedContentHtmlWithUtf8Encoding = article.getContentWithUtf8Encoding();
                extractedContentPlainText = article.getTextContent();
                title = article.getTitle();
                byline = article.getByline();
               excerpt = article.getExcerpt();
            } catch (IOException e){
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            setText(title, Html.fromHtml(extractedContentHtml.replaceAll("image copyright", getResources().getString(R.string.reading_mode_image_copyright) + " ").replaceAll("image caption", getResources().getString(R.string.reading_mode_image_caption) + " ").replaceAll("<a", "<span").replaceAll("</a>", "</span>")));

            dismissProgressDialog();
        }
    }

    private boolean loadPage(@Nullable Intent intent) throws IOException {
        if (intent == null) {
            return false;
        }
        mUrl = intent.getStringExtra(LOAD_READING_URL);
        if (mUrl == null) {
            return false;
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(Utils.getDomainName(mUrl));
        }

        mProgressDialog = new ProgressDialog(ReadingActivity.this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage(getString(R.string.loading));
        mProgressDialog.show();
        BrowserDialog.setDialogSize(ReadingActivity.this, mProgressDialog);

        new loadData().execute();

        return true;
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    private static class ReaderInfo {
        @NonNull private final String mTitleText;
        @NonNull private final String mBodyText;

        ReaderInfo(@NonNull String title, @NonNull String body) {
            mTitleText = title;
            mBodyText = body;
        }

        @NonNull
        public String getTitle() {
            return mTitleText;
        }

        @NonNull
        public String getBody() {
            return mBodyText;
        }
    }

    private void setText(String title, Spanned body) {
        if (mTitle == null || mBody == null)
            return;
        if (mTitle.getVisibility() == View.INVISIBLE) {
            mTitle.setAlpha(0.0f);
            mTitle.setVisibility(View.VISIBLE);
            mTitle.setText(title);
            ObjectAnimator animator = ObjectAnimator.ofFloat(mTitle, "alpha", 1.0f);
            animator.setDuration(300);
            animator.start();
        } else {
            mTitle.setText(title);
        }

        if (mBody.getVisibility() == View.INVISIBLE) {
            mBody.setAlpha(0.0f);
            mBody.setVisibility(View.VISIBLE);
            mBody.setText(body);
            ObjectAnimator animator = ObjectAnimator.ofFloat(mBody, "alpha", 1.0f);
            animator.setDuration(300);
            animator.start();
        } else {
            mBody.setText(body);
        }
    }

    @Override
    protected void onDestroy() {

        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            overridePendingTransition(R.anim.fade_in_scale, R.anim.slide_out_to_right);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.invert_item:
                mUserPreferences.setInvertColors(!mInvert);
                if (mUrl != null) {
                    ReadingActivity.launch(this, mUrl);
                    finish();
                }
                break;
            case R.id.text_size_item:

                View view = LayoutInflater.from(this).inflate(R.layout.dialog_seek_bar, null);
                final SeekBar bar = view.findViewById(R.id.text_size_seekbar);
                bar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(SeekBar view, int size, boolean user) {
                        mBody.setTextSize(getTextSize(size));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar arg0) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar arg0) {
                    }

                });
                bar.setMax(5);
                bar.setProgress(mTextSize);

                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setView(view)
                    .setTitle(R.string.size)
                    .setPositiveButton(android.R.string.ok, (dialog, arg1) -> {
                        mTextSize = bar.getProgress();
                        mBody.setTextSize(getTextSize(mTextSize));
                        mUserPreferences.setReadingTextSize(bar.getProgress());
                    });
                Dialog dialog = builder.show();
                BrowserDialog.setDialogSize(this, dialog);
                break;
            default:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
