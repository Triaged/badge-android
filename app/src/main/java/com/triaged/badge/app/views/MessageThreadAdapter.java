package com.triaged.badge.app.views;

import android.content.Context;
import android.database.Cursor;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.triaged.badge.app.DataProviderService;
import com.triaged.badge.app.R;
import com.triaged.badge.data.CompanySQLiteHelper;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Will on 7/16/14.
 */
public class MessageThreadAdapter extends CursorAdapter {

    private LayoutInflater inflater;
    private DataProviderService.LocalBinding dataProviderServiceBinding;
    private PrettyTime prettyTime;
    private Date messageDate;
    private String threadId;

    public MessageThreadAdapter(Context context, String threadId, DataProviderService.LocalBinding dataProviderServiceBinding ) {
        super( context,  dataProviderServiceBinding.getMessages( threadId ), false );
        this.threadId = threadId;
        inflater = LayoutInflater.from(context);
        this.dataProviderServiceBinding = dataProviderServiceBinding;
        this.prettyTime = new PrettyTime();
        this.messageDate = new Date();
    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = inflater.inflate( getItemViewType( cursor ), parent, false);
        MessageHolder holder = new MessageHolder();
        holder.message = (TextView) v.findViewById(R.id.message_text);
        holder.timestamp = (TextView) v.findViewById(R.id.timestamp);
        v.setTag(holder);
        return v;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        MessageHolder holder = (MessageHolder)view.getTag();
        holder.message.setText( cursor.getString( cursor.getColumnIndex( CompanySQLiteHelper.COLUMN_MESSAGES_BODY ) ) );
        messageDate.setTime( cursor.getLong( cursor.getColumnIndex( CompanySQLiteHelper.COLUMN_MESSAGES_TIMESTAMP ) ) );
        holder.message.setText( prettyTime.format( messageDate ) );
    }

    /** This adapter uses 2 different types of views (my message and other message) */
    @Override
    public int getViewTypeCount() {
        return 2;
    }

    /** Determine which view to use */
    @Override
    public int getItemViewType(int position) {
        return getItemViewType( (Cursor) getItem( position ) );
    }

    public int getItemViewType( Cursor messageCursor ) {
        if ( messageCursor.getInt( messageCursor.getColumnIndex(CompanySQLiteHelper.COLUMN_MESSAGES_FROM_ID ) ) == dataProviderServiceBinding.getLoggedInUser().id ) {
            return R.layout.item_my_message;
        } else {
            return R.layout.item_other_message;
        }
    }

    class MessageHolder {
        TextView message;
        TextView timestamp;
        ImageButton userPhoto;
    }
}
