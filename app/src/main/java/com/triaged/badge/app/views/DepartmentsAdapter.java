package com.triaged.badge.app.views;

import android.content.Context;
import android.database.Cursor;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.triaged.badge.app.DataProviderService;
import com.triaged.badge.app.R;
import com.triaged.badge.data.CompanySQLiteHelper;
import com.triaged.badge.data.Contact;
import com.triaged.badge.data.Department;

import org.w3c.dom.Text;

/**
 * Created by Will on 7/9/14.
 */
public class DepartmentsAdapter extends CursorAdapter {

    protected static LruCache<Integer, Department > departmentCache = new LruCache<Integer, Department>(30);
    protected DataProviderService.LocalBinding dataProviderServiceBinding;
    private Context context;
    private LayoutInflater inflater;
    private int resourceId;

    public DepartmentsAdapter(Context context, DataProviderService.LocalBinding dataProviderServiceBinding , int resourceId) {
        super(context, dataProviderServiceBinding.getDepartmentCursor(), false );
        this.dataProviderServiceBinding = dataProviderServiceBinding;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.resourceId = resourceId;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        ViewHolder holder = new ViewHolder();

        View newView =  inflater.inflate(resourceId, parent, false);
        holder.deptNameView = (TextView) newView.findViewById(R.id.dept_name);
        holder.deptCountView = (TextView) newView.findViewById(R.id.dept_count);
        newView.setTag(holder);
        Department d = getCachedDepartment(cursor);

        holder.deptNameView.setText(d.name);
        if (holder.deptCountView != null) {
            holder.deptCountView.setText(String.valueOf(d.numContacts));
        }
        return newView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();
        Department d = getCachedDepartment( cursor );
        holder.deptNameView.setText(d.name);
        if (holder.deptCountView != null) {
            holder.deptCountView.setText(String.valueOf(d.numContacts));
        }
    }

    /**
     * Get the department pojo at a specific position in the db.
     * Moves the cursor to the correct position then gets the
     * department at that position.
     *
     * @param position the zero indexed position
     * @return either a new department or a previously cached version.
     */

    public Department getCachedDepartment( int position ) {
        return getCachedDepartment( (Cursor)getItem( position ) );
    }

    /**
     * Get the department pojo for a cursor pre-moved to the correct position.
     *
     * @param c the db cursor
     * @return either a new department or a previously cached version.
     */
    public Department getCachedDepartment( Cursor c ) {
        int id = Contact.getIntSafelyFromCursor( c, CompanySQLiteHelper.COLUMN_DEPARTMENT_ID );
        Department dept = departmentCache.get( id );
        if( dept == null ) {
            dept = new Department();
            dept.fromCursor( c );
            departmentCache.put( id, dept );
        }
        return dept;
    }

    /**
     * Call when adapter is going away for good (listview or containing activity being destroyed)
     */
    public void destroy() {
        getCursor().close();
    }

    /**
     * Call this to indicate we should reload the list of depts
     * from the db because it changed locally.
     */
    public void refresh() {
        departmentCache.evictAll();
        changeCursor(dataProviderServiceBinding.getDepartmentCursor());
        notifyDataSetChanged();
    }

    class ViewHolder {
        TextView deptNameView;
        TextView deptCountView;
    }

}
