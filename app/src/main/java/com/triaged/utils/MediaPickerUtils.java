package com.triaged.utils;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;

/**
 * Provide an easy way to pick a image from gallery,
 * or capture a image from camera and get the result.
 *
 * Created by Sadegh Kazemy on 10/3/14.
 */
public class MediaPickerUtils {

    /**
     * Start Gallery/Document application of photo for user,
     * to select a picture
     * @param activity The activity that we start the camera from.
     * @param requestCode The request code used by the caller activity to get the result.
     */
    public static void getImageFromGallery(final Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT < 19) {
            Intent galleryIntent = new Intent();
            galleryIntent.setType("image/*");
            galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
            activity.startActivityForResult(galleryIntent, requestCode);
        } else {
            Intent galleryIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            galleryIntent.addCategory(Intent.CATEGORY_OPENABLE);
            galleryIntent.setType("image/*");
            activity.startActivityForResult(galleryIntent, requestCode);
        }
    }


    public static String processImagePath(Intent data, Context context) {
        Uri imageUri = data.getData();
        String filePath = null;
        // Get file path from URI
        if (Build.VERSION.SDK_INT < 19) {
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = context.getContentResolver().query(imageUri, filePathColumn, null, null, null);
            if (cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                filePath = cursor.getString(columnIndex);
            }
        } else {
            final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            // Check for the freshest data.
            context.getContentResolver().takePersistableUriPermission(imageUri, takeFlags);
            // now extract ID from Uri path using getLastPathSegment() and then split with ":"
            // then call get Uri to for Internal storage or External storage for media I have used getStorageUri()
            String id = imageUri.getLastPathSegment().split(":")[1];

            Cursor imageCursor = context.getContentResolver().query(
                    ContentUris.withAppendedId(getStorageUri(), Long.parseLong(id)),
                    new String []{MediaStore.Images.Media.DATA },
                    null, null, null);

            if (imageCursor.moveToFirst()) {
                filePath = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
        }
        return filePath;
    }


    /**
     * Open camera application for user to capture a picture.
     * @param activity The activity that we start the camera from.
     * @param requestCode The request code used by the caller activity to get the result.
     * @param desFile Destination to store captured picture.
     * @return
     */
    public static boolean captureImageFromCamera(final Activity activity, int requestCode, File desFile) {
        Intent cameraIntent = new Intent("android.media.action.IMAGE_CAPTURE");
        Uri mImageUri;
        try {
            mImageUri = Uri.fromFile(desFile);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
            activity.startActivityForResult(cameraIntent, requestCode);
        }
        catch(Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // By using this method get the Uri of Internal/External Storage for Media
    public static Uri getStorageUri() {
        String state = Environment.getExternalStorageState();
        if(!state.equalsIgnoreCase(Environment.MEDIA_MOUNTED))
            return MediaStore.Images.Media.INTERNAL_CONTENT_URI;

        return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    }
}
