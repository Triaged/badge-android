package com.triaged.badge.ui.entrance;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.triaged.badge.app.R;
import com.triaged.badge.ui.home.InviteFriendFragment;
import com.triaged.badge.ui.home.MainActivity;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class OnboardingInviteColleagueActivity extends Activity {

    Fragment inviteFriendFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_invite_colleague);
        if (savedInstanceState == null) {
            inviteFriendFragment = new InviteFriendFragment();
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }

    public void showPhoneContacts() {
        getFragmentManager().beginTransaction()
                .replace(R.id. container, inviteFriendFragment)
                .commit();
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        @OnClick(R.id.organize_button)
        void showContacts() {
            ((OnboardingInviteColleagueActivity) getActivity()).showPhoneContacts();
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            setHasOptionsMenu(true);
            View rootView = inflater.inflate(R.layout.fragment_onboarding_invite_colleague, container, false);
            ButterKnife.inject(this, rootView);
            return rootView;
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.onboarding_invite_colleague, menu);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            if (item.getItemId() == R.id.action_skip) {
                startActivity(new Intent(getActivity(), MainActivity.class));
                getActivity().finish();
                return true;
            }
            return false;
        }
    }
}
