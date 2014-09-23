package com.triaged.badge.ui.entrance;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import com.triaged.badge.app.R;
import com.triaged.badge.ui.base.views.NonSwipeableViewPager;

import org.apache.http.conn.scheme.HostNameResolver;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class OnboardingActivity extends Activity implements OnboardingCreateFragment.OnSignUpListener {


    @InjectView(R.id.viewpager) NonSwipeableViewPager viewPager;
    List<Fragment> fragmentList = new ArrayList<Fragment>(2);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        ButterKnife.inject(this);

        fragmentList.add(new OnboardingCreateFragment());
        fragmentList.add(new OnboardingConfirmFragment());

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
    }

    @Override
    public void onSuccess() {
        Handler handler = new Handler(getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                viewPager.setCurrentItem(1);
            }
        }, 600);

    }
}
