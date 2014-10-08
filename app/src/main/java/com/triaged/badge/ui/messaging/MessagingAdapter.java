package com.triaged.badge.ui.messaging;

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
import com.triaged.badge.app.App;
import com.triaged.badge.app.MessageProcessor;
import com.triaged.badge.app.R;
import com.triaged.badge.database.table.UsersTable;
import com.triaged.badge.database.table.MessagesTable;
import com.triaged.badge.models.Contact;
import com.triaged.badge.models.Message;
import com.triaged.badge.app.SyncManager;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.Date;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;

/**
 * Created by Sadegh Kazemy on 9/9/14.
 */
public class MessagingAdapter  extends CursorAdapter {

    private LayoutInflater inflater;
    private PrettyTime prettyTime;
    private Date messageDate;
    private String threadId;

    public void setParticipantsNumber(int participantsNumber) {
        this.participantsNumber = participantsNumber;
        notifyDataSetChanged();
    }

    private int participantsNumber = 1;

    public MessagingAdapter(Context context, Cursor cursor) {
        super(context, cursor, false);
        inflater = LayoutInflater.from(context);
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
        View row = inflater.inflate(resourceId, parent, false);
        MessageHolder holder = new MessageHolder(row);
        row.setTag(holder);
        return row;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final MessageHolder holder = (MessageHolder) view.getTag();

        holder.messageId = cursor.getString(cursor.getColumnIndexOrThrow(MessagesTable.CLM_ID));
        holder.message.setText(cursor.getString(cursor.getColumnIndex(MessagesTable.CLM_BODY)));
        long messageTimeMillis = cursor.getLong(cursor.getColumnIndex(MessagesTable.CLM_TIMESTAMP)) / 1000l;
        if (messageTimeMillis > System.currentTimeMillis()) {
            messageTimeMillis = System.currentTimeMillis() - 5000;
        }
        messageDate.setTime(messageTimeMillis);
        // TODO this is probably not the right timestamp format.
        holder.timestamp.setText(prettyTime.format(messageDate));
        holder.userPhoto.setVisibility(View.VISIBLE);
        holder.userPhoto.setImageBitmap(null);
        String cnt = cursor.getString(cursor.getColumnIndexOrThrow("cnt"));
        holder.readBy.setText(String.format("Ready by %s/%s", cnt, participantsNumber));
        String first = cursor.getString(cursor.getColumnIndex(UsersTable.CLM_FIRST_NAME));
        String last = cursor.getString(cursor.getColumnIndex(UsersTable.CLM_LAST_NAME));
        holder.photoPlaceholder.setText(Contact.constructInitials(first, last));
        holder.photoPlaceholder.setVisibility(View.VISIBLE);
        String avatarUrl = cursor.getString(cursor.getColumnIndex(UsersTable.CLM_AVATAR_URL));

        ImageLoader.getInstance().displayImage(avatarUrl, holder.userPhoto, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                holder.photoPlaceholder.setVisibility(View.GONE);
            }
        });


        if (cursor.getInt(cursor.getColumnIndex(MessagesTable.CLM_AUTHOR_ID)) == SyncManager.getMyUser().id) {
            holder.progressBar.setVisibility(View.GONE);
            holder.messageFailedButton.setVisibility(View.GONE);
            int status = cursor.getInt(cursor.getColumnIndex(MessagesTable.CLM_ACK));
            if (status == Message.MSG_STATUS_ACKNOWLEDGED) {
                // Log.d(MessageThreadAdapter.class.getName(), "ACKd " + cursor.getString(cursor.getColumnIndex(CompanySQLiteHelper.CLM_BODY)));
            } else if (status == Message.MSG_STATUS_PENDING) {
                // Pending
                // Log.d(MessageThreadAdapter.class.getName(), "HAVE NOT ACKd " + cursor.getString(cursor.getColumnIndex(CompanySQLiteHelper.CLM_BODY)));
                holder.progressBar.setVisibility(View.VISIBLE);
                holder.photoPlaceholder.setVisibility(View.GONE);
                holder.userPhoto.setVisibility(View.GONE);
            } else if (status == Message.MSG_STATUS_FAILED) {
                holder.messageFailedButton.setVisibility(View.VISIBLE);
                holder.photoPlaceholder.setVisibility(View.GONE);
                holder.userPhoto.setVisibility(View.GONE);
            }
            final String guid = cursor.getString(cursor.getColumnIndex(MessagesTable.CLM_GUID));
            holder.messageFailedButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MessageProcessor.getInstance().retryMessage(guid);
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
        Cursor cursor = (Cursor) getItem(position);
        if (cursor.getInt(cursor.getColumnIndex(MessagesTable.CLM_AUTHOR_ID)) == App.accountId()) {
            return 0;
        } else {
            return 1;
        }
    }

    /**
     * Determine which view to use based on whether it's my msg or not
     */
    public int getItemViewType(Cursor messageCursor) {
        if (messageCursor.getInt(messageCursor.getColumnIndex(MessagesTable.CLM_AUTHOR_ID)) == SyncManager.getMyUser().id) {
            return 0;
        } else {
            return 1;
        }
    }

    public class MessageHolder {
        String messageId;

        @InjectView(R.id.message_text) TextView message;
        @InjectView(R.id.timestamp) TextView timestamp;
        @InjectView(R.id.contact_thumb) ImageView userPhoto;
        @InjectView(R.id.no_photo_thumb) TextView photoPlaceholder;
        @Optional @InjectView(R.id.pending_status) ProgressBar progressBar;
        @Optional @InjectView(R.id.failed_status) ImageButton messageFailedButton;
        @InjectView(R.id.read_by_text) TextView readBy;

        MessageHolder(View row) {
            ButterKnife.inject(this, row);
        }
    }
}
