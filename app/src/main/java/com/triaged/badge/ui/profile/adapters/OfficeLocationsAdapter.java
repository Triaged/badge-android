package com.triaged.badge.ui.profile.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.triaged.badge.app.R;
import com.triaged.badge.database.table.OfficeLocationsTable;
import com.triaged.badge.models.Contact;
import com.triaged.badge.app.SyncManager;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by Will on 7/14/14.
 * Revised by Sadegh on 10/1/14.
 */
public class OfficeLocationsAdapter extends CursorAdapter {

    private LayoutInflater inflater;
    private int resourceId;
    public int usersOffice;
    public String usersOfficeName;
    Context context;

    public OfficeLocationsAdapter(Context context, Cursor cursor, int resourceId) {
        super(context, cursor, false);
        this.inflater = LayoutInflater.from(context);
        this.context = context;
        this.resourceId = resourceId;
        usersOffice = SyncManager.getMyUser().primaryOfficeLocationId;
        usersOfficeName = SyncManager.getMyUser().officeName;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View newView = inflater.inflate(resourceId, parent, false);
        ViewHolder holder = new ViewHolder(newView);
        newView.setTag(holder);
        return newView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();
        holder.officeName.setText(Contact.getStringSafelyFromCursor(cursor, OfficeLocationsTable.CLM_NAME));
        String address = Contact.getStringSafelyFromCursor(cursor, OfficeLocationsTable.CLM_ADDRESS);
        String city = Contact.getStringSafelyFromCursor(cursor, OfficeLocationsTable.CLM_CITY);
        String zip = Contact.getStringSafelyFromCursor(cursor, OfficeLocationsTable.CLM_ZIP);
        String country = Contact.getStringSafelyFromCursor(cursor, OfficeLocationsTable.CLM_COUNTRY);
        String details = String.format("%s, %s %s, %s", address, city, zip, country);
        holder.officeDetails.setText(details);

        if (usersOffice > 0 && usersOffice == Contact.getIntSafelyFromCursor(cursor, OfficeLocationsTable.COLUMN_ID)) {
            holder.selectedIcon.setVisibility(View.VISIBLE);
        } else {
            holder.selectedIcon.setVisibility(View.INVISIBLE);
        }
    }

    public class ViewHolder {
        @InjectView(R.id.office_title) TextView officeName;
        @InjectView(R.id.office_details) TextView officeDetails;
        @InjectView(R.id.selected_icon) ImageView selectedIcon;

        public ViewHolder(View row) {
            ButterKnife.inject(this, row);
        }
    }
}
