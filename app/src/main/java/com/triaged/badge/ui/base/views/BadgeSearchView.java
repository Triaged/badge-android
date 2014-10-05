package com.triaged.badge.ui.base.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;
import android.widget.SearchView;

import com.triaged.badge.app.R;

/**
 * Created by Sadegh Kazemy on 10/3/14.
 */
public class BadgeSearchView extends SearchView {

    AutoCompleteTextView editText;

    public BadgeSearchView(Context context) {
        super(context);
        setupSearchEditText();
    }

    public BadgeSearchView(Context context, AttributeSet attrSet) {
        super(context, attrSet);
        setupSearchEditText();
    }

    protected void setupSearchEditText() {
        int searchHintImageId = getResources().getIdentifier("android:id/search_src_text", null, null);
        editText = (AutoCompleteTextView) findViewById(searchHintImageId);
        editText.setHintTextColor(getResources().getColor(R.color.lighter_gray));
        editText.setTextColor(getResources().getColor(R.color.white));
    }

    public void setHintText(CharSequence hint) {
        if (editText == null) {
            setQueryHint(hint);
        } else {
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder("   ");
            spannableStringBuilder.append(hint);
            // Add the icon as an spannable
            Drawable searchIcon = getResources().getDrawable(R.drawable.ic_action_search);
            int textSize = (int) (editText.getTextSize() * 1.25);
            searchIcon.setBounds(0, 0, textSize, textSize);
            spannableStringBuilder.setSpan(new ImageSpan(searchIcon), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            // Set the new hint text
            editText.setHint(spannableStringBuilder);
        }
    }

    public void setHintColor(int hintColor) {
        if (editText != null) {
            editText.setHintTextColor(hintColor);
        }
    }
}
