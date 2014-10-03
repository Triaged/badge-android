package com.triaged.badge.ui.entrance;


import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Rect;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.mobsandgeeks.saripaar.Rule;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.Email;
import com.mobsandgeeks.saripaar.annotation.Required;
import com.triaged.badge.net.mime.TypedJsonString;
import com.triaged.badge.app.R;
import com.triaged.badge.net.api.RestService;
import com.triaged.badge.net.api.responses.AuthenticationResponse;
import com.triaged.utils.SharedPreferencesHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class OnboardingCreateFragment extends Fragment implements Validator.ValidationListener {
    private static final String ARG_FIRST_NAME = "first_name";
    private static final String ARG_LAST_NAME = "last_name";
    private static final String ARG_EMAIL = "email";
    private static final String ARG_PHONE = "phone";

    private OnSignUpListener mListener;
    Validator validator;

    ProgressDialog progressDialog;
    @InjectView(R.id.root_view) RelativeLayout rootView;
    @InjectView(R.id.login_title) View loginTitle;
    @Required(order = 1) @InjectView(R.id.first_name) EditText firstNameView;
    @Required(order = 2) @InjectView(R.id.last_name) EditText lastNameView;
    @Required(order = 3) @Email(order = 4, messageResId = R.string.invalid_email_error)
    @InjectView(R.id.email) EditText emailView;
    @InjectView(R.id.phone) EditText phoneView;

    @OnClick(R.id.signup)
    void onSignupClicked() {
        validator.validate();
    }


    public OnboardingCreateFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            firstNameView.setText(getArguments().getString(ARG_FIRST_NAME));
            lastNameView.setText(getArguments().getString(ARG_LAST_NAME));
            emailView.setText(getArguments().getString(ARG_EMAIL));
            phoneView.setText(getArguments().getString(ARG_PHONE));
        }
        validator = new Validator(this);
        validator.setValidationListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_FIRST_NAME, firstNameView.getText().toString());
        outState.putString(ARG_LAST_NAME, lastNameView.getText().toString());
        outState.putString(ARG_EMAIL, emailView.getText().toString());
        outState.putString(ARG_PHONE, phoneView.getText().toString());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_onboarding_create, container, false);
        ButterKnife.inject(this, root);
        setupLayoutChangeListener();
        return root;
    }

    private float densityMultiplier = 1;
    boolean keyboardVisible = true;

    private void setupLayoutChangeListener() {
        densityMultiplier = getResources().getDisplayMetrics().density;
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int rootViewHeight = rootView.getRootView().getHeight();
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);
                int heightDiff = rootViewHeight - (r.bottom - r.top);
                if (heightDiff > (densityMultiplier * 75)) {
                    keyboardVisible = true;
                    loginTitle.setVisibility(View.GONE);
                    ((ViewGroup.MarginLayoutParams) firstNameView.getLayoutParams()).topMargin = -8;

                } else if (keyboardVisible) {
                    keyboardVisible = false;
                    loginTitle.setVisibility(View.VISIBLE);
                    ((ViewGroup.MarginLayoutParams) firstNameView.getLayoutParams()).topMargin = (int) (36 * densityMultiplier);
                }
            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnSignUpListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSignUpListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onValidationSucceeded() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setTitle("Please wait");
            progressDialog.setMessage("Signing up");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();

        JsonObject authParams = new JsonObject();
        authParams.addProperty("first_name", firstNameView.getText().toString());
        authParams.addProperty("last_name", lastNameView.getText().toString());
        authParams.addProperty("email", emailView.getText().toString());
        authParams.addProperty("phone_number", phoneView.getText().toString());

        JsonObject postData = new JsonObject();
        postData.add("auth_params", authParams);
        TypedJsonString typedJsonString = new TypedJsonString(postData.toString());
        RestService.instance().badge().signUp(typedJsonString, new Callback<AuthenticationResponse>() {
            @Override
            public void success(AuthenticationResponse authenticationResponse, Response response) {
                storeUserInfoInSharedPreferences(authenticationResponse.id(), emailView.getText().toString());
                progressDialog.dismiss();
                mListener.onSignUpSucceed();
            }

            @Override
            public void failure(RetrofitError error) {
                Toast.makeText(getActivity(), "Something went wrong", Toast.LENGTH_LONG).show();
                progressDialog.dismiss();
            }
        });
    }

    private void storeUserInfoInSharedPreferences(final int accountId, final String accountEmail) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                SharedPreferencesHelper.instance()
                        .putBoolean(R.string.pref_is_a_company_email_key, isACompanyEmail(accountEmail))
                        .putString(R.string.pref_account_email_key, accountEmail)
                        .putInt(R.string.pref_account_id_key, accountId)
                        .commit();
            }
        });
        thread.start();
    }

    private boolean isACompanyEmail(String email) {
        if (email != null) {
            int atIndex = email.lastIndexOf('@');
            if (atIndex > 0) {
                String host = email.substring(atIndex + 1);
                try {
                    InputStream inputStream = getActivity().getAssets().open("blacklist.hosts");
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8" );
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        if (line.equals(host)) {
                            return false;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    @Override
    public void onValidationFailed(View failedView, Rule<?> failedRule) {
        String message = failedRule.getFailureMessage();

        if (failedView instanceof EditText) {
            failedView.requestFocus();
            ((EditText) failedView).setError(message);
        } else {
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnSignUpListener {
        public void onSignUpSucceed();
    }


}
