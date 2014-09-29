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
import com.triaged.badge.database.helper.OfficeLocationHelper;
import com.triaged.badge.database.table.OfficeLocationsTable;
import com.triaged.badge.models.Contact;
import com.triaged.badge.net.DataProviderService;

/**
 * Created by Will on 7/14/14.
 */
public class OfficeLocationsAdapter extends CursorAdapter {

    protected DataProviderService.LocalBinding dataProviderServiceBinding;
    private LayoutInflater inflater;
    private int resourceId;
    public int usersOffice;
    public String usersOfficeName;
    Context context;

    public OfficeLocationsAdapter(Context context, int resourceId) {
        super(context, OfficeLocationHelper.getOfficeLocationsCursor(context), false);
        this.inflater = LayoutInflater.from(context);
        this.context = context;
        this.resourceId = resourceId;
        usersOffice = dataProviderServiceBinding.getLoggedInUser().primaryOfficeLocationId;
        usersOfficeName = dataProviderServiceBinding.getLoggedInUser().officeName;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        ViewHolder holder = new ViewHolder();

        View newView = inflater.inflate(resourceId, parent, false);
        holder.officeName = (TextView) newView.findViewById(R.id.office_title);
        holder.officeDetails = (TextView) newView.findViewById(R.id.office_details);
        holder.selectedIcon = (ImageView) newView.findViewById(R.id.selected_icon);
        newView.setTag(holder);
        setupView(holder, cursor);
        return newView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();
        setupView(holder, cursor);
    }

    private void setupView(ViewHolder holder, Cursor cursor) {
        holder.officeName.setText(Contact.getStringSafelyFromCursor(cursor, OfficeLocationsTable.CLM_NAME));
        String address = Contact.getStringSafelyFromCursor(cursor, OfficeLocationsTable.CLM_ADDRESS);
        String city = Contact.getStringSafelyFromCursor(cursor, OfficeLocationsTable.CLM_CITY);
        String zip = Contact.getStringSafelyFromCursor(cursor, OfficeLocationsTable.CLM_ZIP);
        String country = Contact.getStringSafelyFromCursor(cursor, OfficeLocationsTable.CLM_COUNTRY);
        String details = String.format("%s, %s %s, %s", address, city, zip, country);
        holder.officeDetails.setText(details);

        holder.selectedIcon.setVisibility((usersOffice > 0 && usersOffice == Contact.getIntSafelyFromCursor(cursor, OfficeLocationsTable.COLUMN_ID)) ? View.VISIBLE : View.INVISIBLE);
    }

    public class ViewHolder {
        TextView officeName;
        TextView officeDetails;
        ImageView selectedIcon;
    }

    public void refresh() {
        changeCursor(OfficeLocationHelper.getOfficeLocationsCursor(context));
        notifyDataSetChanged();
    }



    public void destroy() {
        getCursor().close();
    }

}
