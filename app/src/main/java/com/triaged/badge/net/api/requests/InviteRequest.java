package com.triaged.badge.net.api.requests;

import com.triaged.badge.models.Invite;

/**
 * Created by Sadegh Kazemy on 9/24/14.
 */
public class InviteRequest {

    Invite[] invites;

    public InviteRequest(Invite[] invites) {
        this.invites = invites;
    }

}
