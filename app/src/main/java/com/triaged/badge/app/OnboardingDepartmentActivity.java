package com.triaged.badge.app;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.triaged.badge.app.views.DepartmentsAdapter;
import com.triaged.badge.data.Department;

/**
 * Allow the user to select their department or add a new one.
 *
 * Created by Will on 7/14/14.
 */
public class OnboardingDepartmentActivity extends BadgeActivity {

    public static final String DEPT_NAME_EXTRA = "department";

    private ListView departmentsListView = null;
    private DepartmentsAdapter departmentsAdapter = null;
    protected DataProviderService.LocalBinding dataProviderServiceBinding = null;

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
                intent.putExtra( DEPT_NAME_EXTRA, cachedDept.name);
                setResult( cachedDept.id, intent );
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
                        dataProviderServiceBinding.createNewDepartmentAsync( departmentName, new DataProviderService.AsyncSaveCallback() {
                            @Override
                            public void saveSuccess( int newId ) {
                                Intent intent = new Intent(OnboardingDepartmentActivity.this, OnboardingPositionActivity.class);
                                intent.putExtra( DEPT_NAME_EXTRA, departmentName );
                                setResult( newId, intent );
                                finish();
                            }

                            @Override
                            public void saveFailed(String reason) {
                                Toast.makeText( OnboardingDepartmentActivity.this, reason, Toast.LENGTH_SHORT ).show();
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

        dataProviderServiceBinding = ((BadgeApplication)getApplication()).dataProviderServiceBinding;
        departmentsAdapter = new DepartmentsAdapter( this, R.layout.item_department_no_count, dataProviderServiceBinding, false );
        departmentsListView.setAdapter( departmentsAdapter );
    }

}
