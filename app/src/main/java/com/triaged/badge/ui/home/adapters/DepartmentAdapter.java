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

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by Sadegh Kazemy on 9/7/14.
 */
public class DepartmentAdapter extends CursorAdapter {

    private LayoutInflater inflater;

    public DepartmentAdapter(Context context, Cursor cursor) {
        super(context, cursor, false);
        this.inflater = LayoutInflater.from(context);
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
        String numberOfContacts = cursor.getString(cursor.getColumnIndexOrThrow(DepartmentsTable.CLM_CONTACTS_NUMBER));
        holder.deptCountView.setText(String.valueOf(numberOfContacts));
        holder.name = cursor.getString(cursor.getColumnIndexOrThrow(DepartmentsTable.CLM_NAME));
        holder.id = cursor.getInt(cursor.getColumnIndexOrThrow(DepartmentsTable.COLUMN_ID));
        holder.deptNameView.setText(holder.name);
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
