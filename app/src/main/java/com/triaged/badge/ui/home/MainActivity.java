package com.triaged.badge.ui.home;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.net.Uri;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import com.triaged.badge.app.App;
import com.triaged.badge.app.R;
import com.triaged.badge.ui.base.BadgeActivity;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class MainActivity extends BadgeActivity implements ActionBar.TabListener {


    private boolean shouldRegister = true;

    @InjectView(R.id.viewpager)
    ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupUi();
    }


    ActionBar.Tab messageTab;
    ActionBar.Tab contactsTab;

    Fragment messagesFragment;
    Fragment contactFragment;

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

        getActionBar().addTab(messageTab);
        getActionBar().addTab(contactsTab);

        viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                changeTabDrawable(position);
            }
        });

        messagesFragment = MessagesFragments.newInstance();
        contactFragment = ContactsFragment.newInstance();

        viewPager.setAdapter(new FragmentPagerAdapter(getFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 0:
                        return messagesFragment;

                    case 1:
                        return contactFragment;

                    default:
                        return contactFragment;
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        });

        viewPager.setCurrentItem(1);


    }

    private void changeTabDrawable(int position) {
        getActionBar().setSelectedNavigationItem(position);
        switch (position) {
            case 0:
                messageTab.setIcon(R.drawable.messages_selected);
                contactsTab.setIcon(R.drawable.contacts_unselected);
                break;

            case 1:
                messageTab.setIcon(R.drawable.messages_unselected);
                contactsTab.setIcon(R.drawable.contacts_selected);
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


    /**
     * Every time we get to the contacts screen, do a quick check to see if we've registered the device yet.
     * If not, do it assuming the user is logged in!
     */
    private void lazyDeviceRegistration() {
        if (shouldRegister && dataProviderServiceBinding.getLoggedInUser() != null) {
            String regId = getRegistrationId(this);
            if (regId.isEmpty()) {
                // This will async generate a new reg id and
                // send it up to the cloud
                ensureGcmRegistration();
            } else {
                // Re-register device
                ((App) getApplication()).dataProviderServiceBinding.registerDevice(regId);
            }
            shouldRegister = false;
        }
    }


}
