package com.triaged.badge.ui.entrance;


import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.triaged.badge.app.R;
import com.triaged.badge.database.helper.UserHelper;
import com.triaged.badge.database.provider.UserProvider;
import com.triaged.badge.events.LogedinSuccessfully;
import com.triaged.badge.models.Account;
import com.triaged.badge.net.api.RestService;
import com.triaged.badge.net.mime.TypedJsonString;
import com.triaged.utils.GeneralUtils;
import com.triaged.utils.SharedPreferencesHelper;

import org.apache.http.HttpStatus;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class OnboardingConfirmFragment extends Fragment implements View.OnFocusChangeListener,
        View.OnKeyListener, TextWatcher {

    private OnConfirmListener mListener;

    ProgressDialog progressDialog;
    @InjectView(R.id.root_view) RelativeLayout rootView;
    @InjectView(R.id.confirm_textview) View confirmTitleView;
    @InjectView(R.id.pin_layout) View pinLayoutView;
    @InjectView(R.id.pin_hidden_edittext) EditText pinHidden;
    @InjectView(R.id.first_digit) EditText firstDigit;
    @InjectView(R.id.second_digit) EditText secondDigit;
    @InjectView(R.id.third_digit) EditText thirdDigit;
    @InjectView(R.id.forth_digit) EditText forthDigit;
    @InjectView(R.id.continue_btn) Button continueBtn;

    @OnClick(R.id.continue_btn)
    void validatePin() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setTitle("Please wait");
            progressDialog.setMessage("Validating");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();

        JsonObject authParams = new JsonObject();
        authParams.addProperty("id", SharedPreferencesHelper.instance().getInteger(R.string.pref_account_id_key, -1));
        authParams.addProperty("challenge_code", pinHidden.getText().toString());

        JsonObject postData = new JsonObject();
        postData.add("auth_params", authParams);
        TypedJsonString typedJsonString = new TypedJsonString(postData.toString());
        RestService.instance().badge().validate(typedJsonString, new Callback<Account>() {
            @Override
            public void success(Account account, Response response) {
                progressDialog.dismiss();
                // TODO: Need to check for null values
                SharedPreferencesHelper.instance()
                        .putString(R.string.pref_api_token, account.getAuthenticationToken())
                        .putString(R.string.pref_account_company_id_key, account.getCompanyId())
                        .commit();
                getActivity().getContentResolver().insert(UserProvider.CONTENT_URI, UserHelper.toContentValue(account.getCurrentUser()));
                EventBus.getDefault().post(new LogedinSuccessfully());

                mListener.onConfirmSucceed();
                GeneralUtils.dismissKeyboard(getActivity());
            }

            @Override
            public void failure(RetrofitError error) {
                if (error.getResponse().getStatus() == HttpStatus.SC_UNAUTHORIZED) {
                    Toast.makeText(getActivity(), "Wrong pin code", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getActivity(), "Something went wrong", Toast.LENGTH_LONG).show();
                }
                progressDialog.dismiss();
            }
        });
    }

    public OnboardingConfirmFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_onboarding_confirm, container, false);
        ButterKnife.inject(this, root);

        pinHidden.addTextChangedListener(this);

        firstDigit.setOnFocusChangeListener(this);
        secondDigit.setOnFocusChangeListener(this);
        thirdDigit.setOnFocusChangeListener(this);
        forthDigit.setOnFocusChangeListener(this);

        firstDigit.setOnKeyListener(this);
        secondDigit.setOnKeyListener(this);
        thirdDigit.setOnKeyListener(this);
        forthDigit.setOnKeyListener(this);
        pinHidden.setOnKeyListener(this);

        setupLayoutChangeListener();
        return root;
    }

    private float densityMultiplier = 1;
    boolean keyboardVisible = false;
    private void setupLayoutChangeListener() {
        densityMultiplier = getResources().getDisplayMetrics().density;
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int rootViewHeight = rootView.getRootView().getHeight();
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);
                int heightDiff = rootViewHeight - (r.bottom - r.top);
                ViewGroup.MarginLayoutParams titleParams = (ViewGroup.MarginLayoutParams) confirmTitleView.getLayoutParams();
                ViewGroup.MarginLayoutParams pinlayoutParam = (ViewGroup.MarginLayoutParams) pinLayoutView.getLayoutParams();
                if (heightDiff > (densityMultiplier * 75)) {
                    keyboardVisible = true;
                    titleParams.topMargin = 0;
                    pinlayoutParam.topMargin = (int) (64 * densityMultiplier);

                } else if (keyboardVisible) {
                    keyboardVisible = false;
                    titleParams.topMargin = (int) (64 * densityMultiplier);
                    pinlayoutParam.topMargin = (int) (92 * densityMultiplier);
                }
                confirmTitleView.setLayoutParams(titleParams);
                pinLayoutView.setLayoutParams(pinlayoutParam);
            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnConfirmListener) activity;
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
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            switch (v.getId()) {
                case R.id.first_digit:
                case R.id.second_digit:
                case R.id.third_digit:
                case R.id.forth_digit:
                    pinHidden.setFocusable(true);
                    pinHidden.setFocusableInTouchMode(true);
                    pinHidden.requestFocus();
                    GeneralUtils.showKeyboard(pinHidden);
                    switch (pinHidden.getText().length()) {
                        case 0:
                            firstDigit.setBackgroundResource(R.drawable.badge_textfield_focused_holo_light);
                            break;
                        case 1:
                            secondDigit.setBackgroundResource(R.drawable.badge_textfield_focused_holo_light);
                            break;

                        case 2:
                            thirdDigit.setBackgroundResource(R.drawable.badge_textfield_focused_holo_light);
                            break;
                        default:
                            forthDigit.setBackgroundResource(R.drawable.badge_textfield_focused_holo_light);
                            break;
                    }
                default:
                    break;
            }
        }
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            final int id = v.getId();
            if (id == R.id.pin_hidden_edittext && keyCode == KeyEvent.KEYCODE_DEL) {
                if (pinHidden.getText().length() == 4) forthDigit.setText("");
                else if (pinHidden.getText().length() == 3) thirdDigit.setText("");
                else if (pinHidden.getText().length() == 2) secondDigit.setText("");
                else if (pinHidden.getText().length() == 1) firstDigit.setText("");

                if (pinHidden.length() > 0) pinHidden.setText(pinHidden.getText().subSequence(0, pinHidden.length() - 1));
                return true;
            }

        }
        return false;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        firstDigit.setBackgroundResource(R.drawable.badge_textfield_default_holo_light);
        secondDigit.setBackgroundResource(R.drawable.badge_textfield_default_holo_light);
        thirdDigit.setBackgroundResource(R.drawable.badge_textfield_default_holo_light);
        forthDigit.setBackgroundResource(R.drawable.badge_textfield_default_holo_light);
        continueBtn.setEnabled(false);

        if (s.length() == 0) {
            firstDigit.setBackgroundResource(R.drawable.badge_textfield_focused_holo_light);
            firstDigit.setText("");
        } else if (s.length() == 1) {
            secondDigit.setBackgroundResource(R.drawable.badge_textfield_focused_holo_light);
            firstDigit.setText(s.charAt(0) + "");
            secondDigit.setText("");
            thirdDigit.setText("");
            forthDigit.setText("");
        } else if (s.length() == 2) {
            thirdDigit.setBackgroundResource(R.drawable.badge_textfield_focused_holo_light);
            secondDigit.setText(s.charAt(1) + "");
            thirdDigit.setText("");
            forthDigit.setText("");
        } else if (s.length() == 3) {
            forthDigit.setBackgroundResource(R.drawable.badge_textfield_focused_holo_light);
            thirdDigit.setText(s.charAt(2) + "");
            forthDigit.setText("");
        } else if (s.length() == 4) {
            forthDigit.setText(s.charAt(3) + "");
            continueBtn.setEnabled(true);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnConfirmListener {
        public void onConfirmSucceed();
    }
}
