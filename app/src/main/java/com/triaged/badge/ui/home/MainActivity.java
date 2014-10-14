package com.triaged.badge.ui.home;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import com.triaged.badge.app.R;
import com.triaged.badge.ui.base.BadgeActivity;
import com.triaged.badge.ui.base.views.FlexViewPager;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class MainActivity extends BadgeActivity implements ActionBar.TabListener {

    private boolean shouldRegister = true;

    ActionBar.Tab messageTab;
    ActionBar.Tab contactsTab;
    ActionBar.Tab myProfileTab;

    Fragment messagesFragment;
    Fragment contactFragment;
    Fragment tasksFragment;

    @InjectView(R.id.viewpager) FlexViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupUi();
    }

    private void setupUi() {
        getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        getActionBar().setDisplayShowTitleEnabled(false);
        getActionBar().setDisplayShowHomeEnabled(false);
        getActionBar().setDisplayUseLogoEnabled(false);

        setContentView(R.layout.activity_mian);
        ButterKnife.inject(this);

        messageTab = getActionBar().newTab()
                .setIcon(R.drawable.messages_unselected)
                .setTabListener(this);

        contactsTab = getActionBar().newTab()
                .setIcon(R.drawable.contacts_selected)
                .setTabListener(this);

        myProfileTab = getActionBar().newTab()
                .setIcon(R.drawable.profile_unselected)
                .setTabListener(this);

        getActionBar().addTab(messageTab);
        getActionBar().addTab(contactsTab);
        getActionBar().addTab(myProfileTab);

        viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                changeTabDrawable(position);
            }
        });

        messagesFragment = MessagesFragments.newInstance();
        contactFragment = UsersFragment.newInstance();
        tasksFragment = TasksFragment.newInstance();

        viewPager.setAdapter(new FragmentPagerAdapter(getFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 0:
                        return messagesFragment;

                    case 1:
                        return contactFragment;

                    case 2:
                        return tasksFragment;

                    default:
                        return contactFragment;
                }
            }

            @Override
            public int getCount() {
                return 3;
            }
        });
        viewPager.setCurrentItem(1);
    }

    private void changeTabDrawable(int position) {
        getActionBar().setSelectedNavigationItem(position);
        switch (position) {
            case 0:
                getActionBar().setIcon(R.drawable.messages_selected);
                messageTab.setIcon(R.drawable.messages_selected);
                contactsTab.setIcon(R.drawable.contacts_unselected);
                myProfileTab.setIcon(R.drawable.profile_unselected);
                break;

            case 1:
                getActionBar().setIcon(R.drawable.contacts_selected);
                messageTab.setIcon(R.drawable.messages_unselected);
                contactsTab.setIcon(R.drawable.contacts_selected);
                myProfileTab.setIcon(R.drawable.profile_unselected);
                break;

            case 2:
                getActionBar().setIcon(R.drawable.profile_selected);
                messageTab.setIcon(R.drawable.messages_unselected);
                contactsTab.setIcon(R.drawable.contacts_unselected);
                myProfileTab.setIcon(R.drawable.profile_selected);
                break;
        }
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {

    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

    }
}
