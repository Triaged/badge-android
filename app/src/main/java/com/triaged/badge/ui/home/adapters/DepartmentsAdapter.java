package com.triaged.badge.ui.home.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.triaged.badge.app.R;
import com.triaged.badge.models.Department;
import com.triaged.badge.net.DataProviderService;

import java.util.LinkedList;
import java.util.List;

/**
 * This is a simple array adapter of {@link com.triaged.badge.models.Department} pojos.
 *
 * @author Created by Will on 7/9/14.
 */
public class DepartmentsAdapter extends ArrayAdapter<Department> {

    protected DataProviderService.LocalBinding dataProviderServiceBinding;
    private Context context;
    private LayoutInflater inflater;
    private int resourceId;
    private boolean onlyNonEmptyDepartments;
    private List<Department> baseList;
    public Cursor departmentsCursor;

    public DepartmentsAdapter(Context context, int resourceId, DataProviderService.LocalBinding dataProviderServiceBinding, Cursor departmentsCursor) {
        super(context, resourceId);
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.resourceId = resourceId;
        this.onlyNonEmptyDepartments = onlyNonEmptyDepartments;
        this.dataProviderServiceBinding = dataProviderServiceBinding;
        baseList = new LinkedList<Department>();
        this.departmentsCursor = departmentsCursor;
        addDepartments();
    }

    private void addDepartments() {
        clear();
        baseList.clear();
        // Cursor c = dataProviderServiceBinding.getDepartmentCursor( onlyNonEmptyDepartments );
        Cursor c = departmentsCursor;
        while (c.moveToNext()) {
            Department dept = new Department();
            dept.fromCursor(c);
            add(dept);
            baseList.add(dept);
        }

        c.close();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v;
        Department d = getItem(position);
        if (convertView != null) {
            v = convertView;
        } else {
            v = newView(parent);
        }
        bindView(v, d);
        return v;
    }

    private View newView(ViewGroup parent) {

        ViewHolder holder = new ViewHolder();

        View newView = inflater.inflate(resourceId, parent, false);
        holder.deptNameView = (TextView) newView.findViewById(R.id.dept_name);
        holder.deptCountView = (TextView) newView.findViewById(R.id.dept_count);
        newView.setTag(holder);
        return newView;
    }

    private void bindView(View view, Department d) {
        ViewHolder holder = (ViewHolder) view.getTag();
        holder.deptNameView.setText(d.name);
        if (holder.deptCountView != null) {
            holder.deptCountView.setText(String.valueOf(d.usersCount));
        }
    }

    public void setFilter(String filter) {
        filter = filter.toLowerCase();
        clear();
        for (Department d : baseList) {
            if (d.name.toLowerCase().indexOf(filter) > -1) {
                add(d);
            }
        }
        notifyDataSetChanged();
    }

    public void clearFilter() {
        for (Department d : baseList) {
            add(d);
        }
        notifyDataSetChanged();
    }

    /**
     * Call when adapter is going away for good (listview or containing activity being destroyed)
     */
    public void destroy() {
        //getCursor().close();
    }

    /**
     * Call this to indicate we should reload the list of depts
     * from the db because it changed locally.
     */
    public void refresh() {
        addDepartments();
        notifyDataSetChanged();
    }

    class ViewHolder {
        TextView deptNameView;
        TextView deptCountView;
    }

}
