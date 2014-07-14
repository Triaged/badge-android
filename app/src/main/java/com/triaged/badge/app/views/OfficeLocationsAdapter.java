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
import com.triaged.badge.data.Department;

/**
 * Created by Will on 7/14/14.
 */
public class OfficeLocationsAdapter extends CursorAdapter {

    protected DataProviderService.LocalBinding dataProviderServiceBinding;
    private LayoutInflater inflater;
    private int resourceId;

    public OfficeLocationsAdapter(Context context, DataProviderService.LocalBinding dataProviderServiceBinding , int resourceId) {
        super(context, dataProviderServiceBinding.getDepartmentCursor(), false );
        this.dataProviderServiceBinding = dataProviderServiceBinding;
        this.inflater = LayoutInflater.from(context);
        this.resourceId = resourceId;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        ViewHolder holder = new ViewHolder();

        View newView =  inflater.inflate(resourceId, parent, false);
        holder.officeName = (TextView) newView.findViewById(R.id.office_title);
        holder.officeDetails = (TextView) newView.findViewById(R.id.office_details);
        newView.setTag(holder);
        // Department d = getCachedDepartment(cursor);

        holder.officeName.setText("NAME");
        holder.officeDetails.setText("DETAILS");
        return newView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();
        // Department d = getCachedDepartment( cursor );
        holder.officeName.setText("NAME");
        holder.officeDetails.setText("DETAILS");
    }

    class ViewHolder {
        TextView officeName;
        TextView officeDetails;
        ImageView selectedIcon;
    }

    public void refresh() {

    }

}
