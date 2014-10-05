package com.triaged.badge.ui.home.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.triaged.badge.app.App;
import com.triaged.badge.app.R;
import com.triaged.badge.models.Invite;
import com.triaged.badge.net.api.RestService;
import com.triaged.badge.net.api.requests.InviteRequest;
import com.triaged.badge.ui.IRow;
import com.triaged.badge.ui.home.InviteFriendFragment;

import java.util.HashSet;
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

    HashSet invitedSet;
    SharedPreferences invitedSharedPreferences = getContext().getSharedPreferences("invited", Context.MODE_PRIVATE);
    public PhoneContactAdapter(Context context, int resource, List<IRow> contactList) {
        super(context, resource, contactList);
        String invitedString = invitedSharedPreferences.getString("invited_set", "");
        invitedSet = App.gson.fromJson(invitedString, HashSet.class);
        if (invitedSet == null) {
            invitedSet = new HashSet<String>(2);
        }
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
                if (invitedSet.contains(phoneContact.phone)) {
                    phoneContact.hasInvited = true;
                } else {
                    phoneContact.hasInvited = false;
                }
            } else {
                holder.subtextView.setText(phoneContact.email);
                if (invitedSet.contains(phoneContact.email)) {
                    phoneContact.hasInvited = true;
                } else {
                    phoneContact.hasInvited = false;
                }
                if (phoneContact.email.equals(phoneContact.name)) {
                    holder.subtextView.setVisibility(View.INVISIBLE);
                }
            }

            if (phoneContact.hasInvited) {
                holder.inviteView.setImageResource(R.drawable.invite_tick_button);
                holder.inviteView.setEnabled(false);
            } else {
                holder.inviteView.setImageResource(R.drawable.invite_add_button);
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
        @InjectView(R.id.invite_button) ImageButton inviteView;

        @OnClick(R.id.invite_button)
        void sendInvitation() {
            final InviteFriendFragment.PhoneContact phoneContact = (InviteFriendFragment.PhoneContact) getItem(position);
            Invite invite = new Invite(phoneContact.email, phoneContact.phone);
            InviteRequest inviteRequest = new InviteRequest(new Invite[]{invite});
            RestService.instance().badge().invite(inviteRequest, new Callback<Response>() {
                @Override
                public void success(Response response, Response response2) {
                    if (phoneContact.email != null)
                        invitedSet.add(phoneContact.email);
                    if (phoneContact.phone != null)
                        invitedSet.add(phoneContact.phone);
                    invitedSharedPreferences.edit().putString("invited_set", App.gson.toJson(invitedSet)).commit();

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
