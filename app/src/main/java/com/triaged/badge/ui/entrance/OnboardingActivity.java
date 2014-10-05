package com.triaged.badge.ui.entrance;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.text.TextUtils;
import com.triaged.badge.app.R;
import com.triaged.badge.events.LogedinSuccessfully;
import com.triaged.badge.ui.base.views.NonSwipeableViewPager;
import com.triaged.badge.ui.home.InviteFriendFragment;
import com.triaged.badge.ui.home.MainActivity;
import com.triaged.utils.SharedPreferencesHelper;


import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

public class OnboardingActivity extends Activity implements
        OnboardingCreateFragment.OnSignUpListener, OnboardingConfirmFragment.OnConfirmListener {


    @InjectView(R.id.viewpager) NonSwipeableViewPager viewPager;
    List<Fragment> fragmentList = new ArrayList<Fragment>(2);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!TextUtils.isEmpty(SharedPreferencesHelper.instance().getString(R.string.pref_api_token, ""))) {
            EventBus.getDefault().post(new LogedinSuccessfully());
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_onboarding);
        ButterKnife.inject(this);

        fragmentList.add(new OnboardingCreateFragment());
        fragmentList.add(new OnboardingConfirmFragment());
        fragmentList.add(new InviteFriendFragment());

        viewPager.setPagingEnabled(false);
        viewPager.setAdapter(new FragmentStatePagerAdapter(getFragmentManager()) {
            @Override
            public Fragment getItem(int i) {
                return fragmentList.get(i);
            }

            @Override
            public int getCount() {
                return fragmentList.size();
            }
        });

        // If already singed up, go to the confirmation fragment.
        if (SharedPreferencesHelper.instance().getInteger(R.string.pref_account_id_key, -1) > 0 ) {
            viewPager.setCurrentItem(1);
        }
    }

    @Override
    public void onSignUpSucceed() {
        viewPager.postDelayed(new Runnable() {
            @Override
            public void run() {
                viewPager.setCurrentItem(1);
            }
        }, 400);
    }

    @Override
    public void onConfirmSucceed() {
        viewPager.postDelayed(new Runnable() {
            @Override
            public void run() {
                viewPager.setCurrentItem(2);
            }
        }, 400);
    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 1) {
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
        } else {
            super.onBackPressed();
        }
    }
}
