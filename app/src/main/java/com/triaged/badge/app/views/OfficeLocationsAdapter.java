package com.triaged.badge.app.views;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.triaged.badge.app.DataProviderService;
import com.triaged.badge.app.R;
import com.triaged.badge.data.CompanySQLiteHelper;
import com.triaged.badge.data.Contact;
import com.triaged.badge.data.Department;

/**
 * Created by Will on 7/14/14.
 */
public class OfficeLocationsAdapter extends CursorAdapter {

    protected DataProviderService.LocalBinding dataProviderServiceBinding;
    private LayoutInflater inflater;
    private int resourceId;
    public int usersOffice;

    public OfficeLocationsAdapter(Context context, DataProviderService.LocalBinding dataProviderServiceBinding , int resourceId) {
        super(context, dataProviderServiceBinding.getOfficeLocationsCursor(), false );
        this.dataProviderServiceBinding = dataProviderServiceBinding;
        this.inflater = LayoutInflater.from(context);
        this.resourceId = resourceId;
        usersOffice = dataProviderServiceBinding.getLoggedInUser().currentOfficeLocationId;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        ViewHolder holder = new ViewHolder();

        View newView =  inflater.inflate(resourceId, parent, false);
        holder.officeName = (TextView) newView.findViewById(R.id.office_title);
        holder.officeDetails = (TextView) newView.findViewById(R.id.office_details);
        newView.setTag(holder);
        setupView( holder, cursor );
        return newView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();
        setupView( holder, cursor );
    }

    private void setupView( ViewHolder holder, Cursor cursor ) {
        holder.officeName.setText(Contact.getStringSafelyFromCursor( cursor, CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_NAME ) );
        String address = Contact.getStringSafelyFromCursor( cursor, CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_ADDRESS );
        String city = Contact.getStringSafelyFromCursor( cursor, CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_CITY );
        String zip = Contact.getStringSafelyFromCursor( cursor, CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_ZIP );
        String country = Contact.getStringSafelyFromCursor( cursor, CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_COUNTRY );
        String details = String.format( "%s, %s %s, %s", address, city, zip, country );
        holder.officeDetails.setText( details );

        holder.selectedIcon.setVisibility( usersOffice == Contact.getIntSafelyFromCursor( cursor, CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_ID ) ? View.VISIBLE : View.INVISIBLE);
    }

    public class ViewHolder {
        TextView officeName;
        TextView officeDetails;
        ImageView selectedIcon;
    }

    public void destroy() {
        getCursor().close();
    }

}
