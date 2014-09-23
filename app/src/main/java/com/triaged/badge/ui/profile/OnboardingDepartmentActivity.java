package com.triaged.badge.ui.profile;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.triaged.badge.net.mime.TypedJsonString;
import com.triaged.badge.app.App;
import com.triaged.badge.app.R;
import com.triaged.badge.database.provider.DepartmentProvider;
import com.triaged.badge.database.table.DepartmentsTable;
import com.triaged.badge.models.Department;
import com.triaged.badge.net.api.RestService;
import com.triaged.badge.ui.base.BadgeActivity;
import com.triaged.badge.ui.base.MixpanelActivity;
import com.triaged.badge.ui.home.adapters.DepartmentsAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Allow the user to select their department or add a new one.
 * <p/>
 * Created by Will on 7/14/14.
 */
public class OnboardingDepartmentActivity extends MixpanelActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String DEPT_NAME_EXTRA = "department";

    private ListView departmentsListView = null;
    private DepartmentsAdapter departmentsAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_department);
        TextView backButton = (TextView) findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        departmentsListView = (ListView) findViewById(R.id.departments_list);

        departmentsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(OnboardingDepartmentActivity.this, OnboardingPositionActivity.class);
                Department cachedDept = departmentsAdapter.getItem(position);
                intent.putExtra(DEPT_NAME_EXTRA, cachedDept.name);
                setResult(cachedDept.id, intent);
                finish();
            }
        });
        LayoutInflater inflater = LayoutInflater.from(this);
        TextView addView = (TextView) inflater.inflate(R.layout.item_add_new, null);
        addView.setText(getString(R.string.add_new_department));
        departmentsListView.addFooterView(addView);
        addView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(OnboardingDepartmentActivity.this);
                alertDialog.setTitle("Add Department");
                final EditText input = new EditText(OnboardingDepartmentActivity.this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
                input.setLayoutParams(lp);
                alertDialog.setView(input);
                alertDialog.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, int which) {
//                        Toast.makeText(OnboardingDepartmentActivity.this, input.getText().toString(), Toast.LENGTH_SHORT).show();
                        // First, attempt to create new dept async. Only on success do we hide the dialog.
                        final String departmentName = input.getText().toString();
                        JSONObject postData = new JSONObject();
                        JSONObject departmentData = new JSONObject();
                        try {
                            postData.put("department", departmentData);
                            departmentData.put("name", departmentName);
                        } catch (JSONException e) {
                            App.gLogger.e("JSON exception creating post body for create department", e);
                            return;
                        }
                        TypedJsonString typedJsonString = new TypedJsonString(postData.toString());
                        RestService.instance().badge().createDepartment(typedJsonString, new Callback<Department>() {
                            @Override
                            public void success(Department department, Response response) {
                                // Put into database.
                                ContentValues values = new ContentValues();
                                values.put(DepartmentsTable.COLUMN_ID, department.id);
                                values.put(DepartmentsTable.COLUMN_DEPARTMENT_NAME, department.name);
                                values.put(DepartmentsTable.COLUMN_DEPARTMENT_NUM_CONTACTS, department.usersCount);
                                getContentResolver().insert(DepartmentProvider.CONTENT_URI, values);
                                // Start next activity.
                                Intent intent = new Intent(OnboardingDepartmentActivity.this, OnboardingPositionActivity.class);
                                intent.putExtra(DEPT_NAME_EXTRA, departmentName);
                                setResult(department.id, intent);
                                finish();
                            }

                            @Override
                            public void failure(RetrofitError error) {
                                Toast.makeText(OnboardingDepartmentActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });

                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                    }
                });
                alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                        dialog.cancel();
                    }
                });
                alertDialog.show();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }
        });

        getLoaderManager().initLoader(0, null, this);

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, DepartmentProvider.CONTENT_URI,
                null, null, null, DepartmentsTable.COLUMN_DEPARTMENT_NAME);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        departmentsAdapter = new DepartmentsAdapter(OnboardingDepartmentActivity.this,
                R.layout.item_department_no_count, data);
        departmentsListView.setAdapter(departmentsAdapter);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
