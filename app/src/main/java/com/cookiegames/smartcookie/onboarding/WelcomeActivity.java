package com.cookiegames.smartcookie.onboarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.chyrta.onboarder.OnboarderActivity;
import com.chyrta.onboarder.OnboarderPage;
import com.cookiegames.smartcookie.MainActivity;
import com.cookiegames.smartcookie.R;

import java.util.ArrayList;
import java.util.List;

public class WelcomeActivity extends OnboarderActivity {

    List<OnboarderPage> onboarderPages;
    SharedPreferences prefs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onboarderPages = new ArrayList<OnboarderPage>();

        OnboarderPage onboarderPage1 = new OnboarderPage(R.string.slide_1_title, R.string.slide_1_desc, R.mipmap.ic_launcher);
        OnboarderPage onboarderPage2 = new OnboarderPage(R.string.slide_2_title, R.string.slide_2_desc, R.drawable.ic_action_plus);
        OnboarderPage onboarderPage3 = new OnboarderPage(R.string.slide_3_title, R.string.slide_3_desc, R.drawable.ic_round_settings);
        OnboarderPage onboarderPage4 = new OnboarderPage(R.string.slide_4_title, R.string.slide_4_desc, R.drawable.ic_action_bookmark);

        onboarderPage1.setBackgroundColor(R.color.onboarding);
        onboarderPage2.setBackgroundColor(R.color.onboarding);
        onboarderPage3.setBackgroundColor(R.color.onboarding);
        onboarderPage4.setBackgroundColor(R.color.onboarding);

       //TODO: Switch to for loop + center text if/when library is fixed

        onboarderPages.add(onboarderPage1);
        onboarderPages.add(onboarderPage2);
        onboarderPages.add(onboarderPage3);
        onboarderPages.add(onboarderPage4);

        setOnboardPagesReady(onboarderPages);

    }

    @Override
    public void onSkipButtonPressed() {
        super.onSkipButtonPressed();
    }

    @Override
    public void onFinishButtonPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

    }

}