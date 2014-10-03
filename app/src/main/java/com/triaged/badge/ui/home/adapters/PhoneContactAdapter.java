package com.triaged.badge.ui.home.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.triaged.badge.app.App;
import com.triaged.badge.app.R;
import com.triaged.badge.models.Invite;
import com.triaged.badge.net.api.RestService;
import com.triaged.badge.net.api.requests.InviteRequest;
import com.triaged.badge.ui.IRow;
import com.triaged.badge.ui.home.InviteFriendFragment;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
* Created by Sadegh Kazemy on 9/25/14.
*/
public class PhoneContactAdapter extends ArrayAdapter<IRow> {

    public PhoneContactAdapter(Context context, int resource, List<IRow> contactList) {
        super(context, resource, contactList);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (getItem(position).getType() == IRow.CONTENT_ROW) {
            ViewHolder holder;
            if (convertView == null) {
                LayoutInflater layoutInflater = LayoutInflater.from(getContext());
                convertView = layoutInflater.inflate(R.layout.row_phone_contact_invite, parent, false);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            InviteFriendFragment.PhoneContact phoneContact = (InviteFriendFragment.PhoneContact) getItem(position);
            holder.nameView.setText(phoneContact.name);
            holder.subtextView.setVisibility(View.VISIBLE);
            holder.position = position;
            if (TextUtils.isEmpty(phoneContact.email)) {
                holder.subtextView.setText(phoneContact.phone);
            } else {
                holder.subtextView.setText(phoneContact.email);
                if (phoneContact.email.equals(phoneContact.name)) {
                    holder.subtextView.setVisibility(View.INVISIBLE);
                }
            }

            if (phoneContact.hasInvited) {
                holder.inviteView.setText("Sent!");
                holder.inviteView.setEnabled(false);
            } else {
                holder.inviteView.setText("Invite");
                holder.inviteView.setEnabled(true);
            }

        } else {
            HeaderHolder headerHolder;
            if (convertView == null) {
                LayoutInflater layoutInflater = LayoutInflater.from(getContext());
                convertView = layoutInflater.inflate(R.layout.row_phone_contact_header, parent, false);
                headerHolder = new HeaderHolder(convertView);
                convertView.setTag(headerHolder);
            } else {
                headerHolder = (HeaderHolder) convertView.getTag();
            }
            InviteFriendFragment.HeaderRow headerRow = (InviteFriendFragment.HeaderRow) getItem(position);
            headerHolder.headerTitleView.setText(headerRow.name);
        }
        return convertView;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getType();
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    class ViewHolder {
        int position;
        @InjectView(R.id.contact_name) TextView nameView;
        @InjectView(R.id.subtext_view) TextView subtextView;
        @InjectView(R.id.invite_button)
        Button inviteView;

        @OnClick(R.id.invite_button)
        void sendInvitation() {
            final InviteFriendFragment.PhoneContact phoneContact = (InviteFriendFragment.PhoneContact) getItem(position);
            Invite invite = new Invite(phoneContact.email, phoneContact.phone);
            InviteRequest inviteRequest = new InviteRequest(new Invite[]{invite});
            RestService.instance().badge().invite(inviteRequest, new Callback<Response>() {
                @Override
                public void success(Response response, Response response2) {
                    phoneContact.hasInvited = true;
                    notifyDataSetChanged();
                }

                @Override
                public void failure(RetrofitError error) {
                    App.gLogger.e(error);
                }
            });
        }

        ViewHolder(View row) {
            ButterKnife.inject(this, row);
        }
    }

    class HeaderHolder {
        @InjectView(R.id.header_title) TextView headerTitleView;

        HeaderHolder(View row) {
            ButterKnife.inject(this, row);
        }
    }
}
