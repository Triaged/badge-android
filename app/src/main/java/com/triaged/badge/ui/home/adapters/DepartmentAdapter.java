package com.triaged.badge.ui.home.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.triaged.badge.app.R;
import com.triaged.badge.database.table.DepartmentsTable;
import com.triaged.badge.net.DataProviderService;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by Sadegh Kazemy on 9/7/14.
 */
public class DepartmentAdapter extends CursorAdapter {

    protected DataProviderService.LocalBinding dataProviderServiceBinding;
    private Context context;
    private LayoutInflater inflater;
    public Cursor departmentsCursor;

    public DepartmentAdapter(Context context, Cursor cursor) {
        super(context, cursor, false);
        this.context = context;
        this.inflater = LayoutInflater.from(context);

        this.departmentsCursor = departmentsCursor;
    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        View row = inflater.inflate(R.layout.item_department_with_count, parent, false);
        ViewHolder holder = new ViewHolder(row);
        row.setTag(holder);
        return row;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();

        holder.name = cursor.getString(cursor.getColumnIndexOrThrow(DepartmentsTable.CLM_NAME));
        holder.id = cursor.getInt(cursor.getColumnIndexOrThrow(DepartmentsTable.COLUMN_ID));
        String numberOfContacts = cursor.getString(cursor.getColumnIndexOrThrow(DepartmentsTable.CLM_CONTACTS_NUMBER));

        holder.deptNameView.setText(holder.name);
        holder.deptCountView.setText(String.valueOf(numberOfContacts));
    }


    public class ViewHolder {
        public String name;
        public int id;

        @InjectView(R.id.dept_name) TextView deptNameView;
        @InjectView(R.id.dept_count) TextView deptCountView;

        ViewHolder(View row) {
            ButterKnife.inject(this, row);
        }
    }

}
