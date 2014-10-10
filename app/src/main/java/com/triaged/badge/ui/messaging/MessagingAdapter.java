package com.triaged.badge.ui.messaging;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.triaged.badge.app.App;
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

/**
 * Created by Sadegh Kazemy on 9/9/14.
 */
public class MessagingAdapter  extends CursorAdapter {

    private LayoutInflater inflater;
    private PrettyTime prettyTime;
    private Date messageDate;
    private String threadId;

    private static final int ROW_TYPES = 1;

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
        View row = inflater.inflate(R.layout.row_message, parent, false);
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
        // TODO this is probably not the right date format.
        holder.date.setText(prettyTime.format(messageDate) + ",");
        holder.avatar.setVisibility(View.VISIBLE);
        holder.avatar.setImageBitmap(null);
        int cnt = cursor.getInt(cursor.getColumnIndexOrThrow("cnt"));
        if (participantsNumber > 1) {
            holder.readBy.setVisibility(View.VISIBLE);
            holder.readBy.setText(String.format("Ready by %s/%s", cnt, participantsNumber));
        } else {
            if (cnt == 0) {
                holder.readBy.setVisibility(View.INVISIBLE);
            } else {
                holder.readBy.setVisibility(View.VISIBLE);
                holder.readBy.setText("Read");
            }
        }
        holder.avatarText.setVisibility(View.VISIBLE);
        String avatarUrl = cursor.getString(cursor.getColumnIndex(UsersTable.CLM_AVATAR_URL));

        ImageLoader.getInstance().displayImage(avatarUrl, holder.avatar, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                holder.avatarText.setVisibility(View.INVISIBLE);
            }
        });

        String first = cursor.getString(cursor.getColumnIndex(UsersTable.CLM_FIRST_NAME));
        String last = cursor.getString(cursor.getColumnIndex(UsersTable.CLM_LAST_NAME));
        holder.name.setText(String.format("%s %s", first, last));
        holder.avatarText.setText(Contact.constructInitials(first, last));

        int userId = cursor.getInt(cursor.getColumnIndex(MessagesTable.CLM_AUTHOR_ID));
        if ( userId == App.accountId()) {
            holder.name.setText("Me");
//            holder.progressBar.setVisibility(View.GONE);
//            holder.messageFailedButton.setVisibility(View.GONE);
            int status = cursor.getInt(cursor.getColumnIndex(MessagesTable.CLM_ACK));
            if (status == Message.MSG_STATUS_ACKNOWLEDGED) {
                // Log.d(MessageThreadAdapter.class.getName(), "ACKd " + cursor.getString(cursor.getColumnIndex(CompanySQLiteHelper.CLM_BODY)));
            } else if (status == Message.MSG_STATUS_PENDING) {
                // Pending
                // Log.d(MessageThreadAdapter.class.getName(), "HAVE NOT ACKd " + cursor.getString(cursor.getColumnIndex(CompanySQLiteHelper.CLM_BODY)));
//                holder.progressBar.setVisibility(View.VISIBLE);
//                holder.avatarText.setVisibility(View.INVISIBLE);
//                holder.avatar.setVisibility(View.INVISIBLE);
            } else if (status == Message.MSG_STATUS_FAILED) {
//                holder.messageFailedButton.setVisibility(View.VISIBLE);
//                holder.avatarText.setVisibility(View.INVISIBLE);
//                holder.avatar.setVisibility(View.INVISIBLE);
            }
            final String guid = cursor.getString(cursor.getColumnIndex(MessagesTable.CLM_GUID));
//            holder.messageFailedButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    MessageProcessor.getInstance().retryMessage(guid);
//                }
//            });
        } else {
            holder.name.setText(String.format("%s %s", first, last));
        }
    }

    @Override
    public int getViewTypeCount() {
        return ROW_TYPES;
    }

    public class MessageHolder {
        String messageId;

        @InjectView(R.id.message_name) TextView name;
        @InjectView(R.id.contact_thumb) ImageView avatar;
        @InjectView(R.id.no_photo_thumb) TextView avatarText;
//        @Optional @InjectView(R.id.pending_status) ProgressBar progressBar;
//        @Optional @InjectView(R.id.failed_status) ImageButton messageFailedButton;
        @InjectView(R.id.message_date) TextView date;
        @InjectView(R.id.readby) TextView readBy;
        @InjectView(R.id.message_text) TextView message;

        MessageHolder(View row) {
            ButterKnife.inject(this, row);
        }
    }
}
