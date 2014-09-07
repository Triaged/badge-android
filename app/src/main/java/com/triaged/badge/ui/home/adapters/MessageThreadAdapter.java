package com.triaged.badge.ui.home.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.triaged.badge.app.R;
import com.triaged.badge.database.table.ContactsTable;
import com.triaged.badge.database.table.MessagesTable;
import com.triaged.badge.models.Contact;
import com.triaged.badge.net.DataProviderService;

import org.ocpsoft.prettytime.PrettyTime;

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

    public MessageThreadAdapter(Context context, String threadId, DataProviderService.LocalBinding dataProviderServiceBinding) {
        super(context, dataProviderServiceBinding.getMessages(threadId), false);
        this.threadId = threadId;
        inflater = LayoutInflater.from(context);
        this.dataProviderServiceBinding = dataProviderServiceBinding;
        this.prettyTime = new PrettyTime();
        this.messageDate = new Date();
    }


    @Override
    public View newView(final Context context, Cursor cursor, ViewGroup parent) {
        int viewType = getItemViewType(cursor);
        int resourceId;
        if (viewType == 0) {
            resourceId = R.layout.item_my_message;
        } else {
            resourceId = R.layout.item_other_message;
        }
        View v = inflater.inflate(resourceId, parent, false);
        MessageHolder holder = new MessageHolder();
        holder.message = (TextView) v.findViewById(R.id.message_text);
        holder.timestamp = (TextView) v.findViewById(R.id.timestamp);
        holder.userPhoto = (ImageView) v.findViewById(R.id.contact_thumb);
        holder.photoPlaceholder = (TextView) v.findViewById(R.id.no_photo_thumb);
        holder.progressBar = (ProgressBar) v.findViewById(R.id.pending_status);
        holder.messageFailedButton = (ImageButton) v.findViewById(R.id.failed_status);
        v.setTag(holder);
        return v;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final MessageHolder holder = (MessageHolder) view.getTag();
        holder.message.setText(cursor.getString(cursor.getColumnIndex(MessagesTable.COLUMN_MESSAGES_BODY)));
        long messageTimeMillis = cursor.getLong(cursor.getColumnIndex(MessagesTable.COLUMN_MESSAGES_TIMESTAMP)) / 1000l;
        if (messageTimeMillis > System.currentTimeMillis()) {
            messageTimeMillis = System.currentTimeMillis() - 5000;
        }
        messageDate.setTime(messageTimeMillis);
        // TODO this is probably not the right timestamp format.
        holder.timestamp.setText(prettyTime.format(messageDate));
        holder.userPhoto.setVisibility(View.VISIBLE);
        holder.userPhoto.setImageBitmap(null);
        String first = cursor.getString(cursor.getColumnIndex(ContactsTable.COLUMN_CONTACT_FIRST_NAME));
        String last = cursor.getString(cursor.getColumnIndex(ContactsTable.COLUMN_CONTACT_LAST_NAME));
        holder.photoPlaceholder.setText(Contact.constructInitials(first, last));
        holder.photoPlaceholder.setVisibility(View.VISIBLE);
        String avatarUrl = cursor.getString(cursor.getColumnIndex(ContactsTable.COLUMN_CONTACT_AVATAR_URL));

//        dataProviderServiceBinding.setSmallContactImage( avatarUrl, holder.userPhoto, holder.photoPlaceholder );
        ImageLoader.getInstance().displayImage(avatarUrl, holder.userPhoto, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                holder.photoPlaceholder.setVisibility(View.GONE);
            }
        });


        if (cursor.getInt(cursor.getColumnIndex(MessagesTable.COLUMN_MESSAGES_FROM_ID)) == dataProviderServiceBinding.getLoggedInUser().id) {
            holder.progressBar.setVisibility(View.GONE);
            holder.messageFailedButton.setVisibility(View.GONE);
            int status = cursor.getInt(cursor.getColumnIndex(MessagesTable.COLUMN_MESSAGES_ACK));
            if (status == DataProviderService.MSG_STATUS_ACKNOWLEDGED) {
                // Log.d(MessageThreadAdapter.class.getName(), "ACKd " + cursor.getString(cursor.getColumnIndex(CompanySQLiteHelper.COLUMN_MESSAGES_BODY)));
            } else if (status == DataProviderService.MSG_STATUS_PENDING) {
                // Pending
                // Log.d(MessageThreadAdapter.class.getName(), "HAVE NOT ACKd " + cursor.getString(cursor.getColumnIndex(CompanySQLiteHelper.COLUMN_MESSAGES_BODY)));
                holder.progressBar.setVisibility(View.VISIBLE);
                holder.photoPlaceholder.setVisibility(View.GONE);
                holder.userPhoto.setVisibility(View.GONE);
            } else if (status == DataProviderService.MSG_STATUS_FAILED) {
                holder.messageFailedButton.setVisibility(View.VISIBLE);
                holder.photoPlaceholder.setVisibility(View.GONE);
                holder.userPhoto.setVisibility(View.GONE);
            }
            final String guid = cursor.getString(cursor.getColumnIndex(MessagesTable.COLUMN_MESSAGES_GUID));
            holder.messageFailedButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dataProviderServiceBinding.retryMessageAsync(guid);
                }
            });
        }
    }

    /**
     * This adapter uses 2 different types of views (my message and other message)
     */
    @Override
    public int getViewTypeCount() {
        return 2;
    }

    /**
     * Determine which view to use
     */
    @Override
    public int getItemViewType(int position) {
        return getItemViewType((Cursor) getItem(position));
    }

    /**
     * Determine which view to use based on whether it's my msg or not
     */
    public int getItemViewType(Cursor messageCursor) {
        if (messageCursor.getInt(messageCursor.getColumnIndex(MessagesTable.COLUMN_MESSAGES_FROM_ID)) == dataProviderServiceBinding.getLoggedInUser().id) {
            return 0;
        } else {
            return 1;
        }
    }

    /**
     * Call when no adapter no longer needed. Closes cursor.
     */
    public void destroy() {
        getCursor().close();
    }

    class MessageHolder {
        TextView message;
        TextView timestamp;
        ImageView userPhoto;
        TextView photoPlaceholder;
        ProgressBar progressBar;
        ImageButton messageFailedButton;
    }
}
