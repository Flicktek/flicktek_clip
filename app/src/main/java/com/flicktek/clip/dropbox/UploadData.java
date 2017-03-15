/*
 * Copyright (c) 2011 Dropbox, Inc.
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */


package com.flicktek.clip.dropbox;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.UploadRequest;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxFileSizeException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Here we show uploading a file in a background thread, trying to show
 * typical exception handling and flow of control for an app that uploads a
 * file from Dropbox.
 */
public class UploadData extends AsyncTask<Void, Long, Boolean> {
    private static final int BUFFER = 4098;

    private DropboxAPI<?> mApi;
    private String mPath;
    private File mFile;

    private long mFileLen;
    private UploadRequest mRequest;
    private Context mContext;
    private final ProgressDialog mDialog;

    private String mErrorMsg;

    public void zip(String[] files, String zipFile) {
        String[] _files = files;
        String _zipFile = zipFile;

        try {
            BufferedInputStream origin = null;
            FileOutputStream dest = new FileOutputStream(_zipFile);

            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));

            byte data[] = new byte[BUFFER];

            for (int i = 0; i < _files.length; i++) {
                Log.d("add:", _files[i]);
                Log.v("Compress", "Adding: " + _files[i]);
                FileInputStream fi = new FileInputStream(_files[i]);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(_files[i].substring(_files[i].lastIndexOf("/") + 1));
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }

            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String[] s = new String[1];
    String destination;

    public UploadData(Context context, DropboxAPI<?> api, String dropboxPath, String captureFile, File fileOrigin) {
        // We set the context this way so we don't accidentally leak activities
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mContext = context.getApplicationContext();
        } else {
            mContext = context;
        }

        mDialog = new ProgressDialog(context);
        mDialog.setMax(100);
        mDialog.setMessage("Uploading " + captureFile + " " + fileOrigin.length() + " bytes");
        mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mDialog.setProgress(0);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        }

        mDialog.setButton(ProgressDialog.BUTTON_POSITIVE, "Cancel", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // This will cancel the putFile operation
                mRequest.abort();
            }
        });
        mDialog.show();

        //--------------------------------------------------------------------------

        s[0] = fileOrigin.getAbsolutePath();
        File[] myfile = context.getExternalFilesDirs(null);
        destination = myfile[0].getAbsolutePath() + captureFile + ".zip";

        //--------------------------------------------------------------------------

        mApi = api;
        mPath = dropboxPath;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            zip(s, destination);
            File file = new File(destination);
            mFile = file;
            mFileLen = file.length();

            // By creating a request, we get a handle to the putFile operation,
            // so we can cancel it later if we want to
            FileInputStream fis = new FileInputStream(mFile);
            String path = mPath + mFile.getName();
            mRequest = mApi.putFileOverwriteRequest(path, fis, mFile.length(),
                    new ProgressListener() {
                @Override
                public long progressInterval() {
                    // Update the progress bar every half-second or so
                    return 500;
                }

                @Override
                public void onProgress(long bytes, long total) {
                    publishProgress(bytes);
                }
            });

            if (mRequest != null) {
                mRequest.upload();
                return true;
            }

        } catch (DropboxUnlinkedException e) {
            // This session wasn't authenticated properly or user unlinked
            mErrorMsg = "This app wasn't authenticated properly.";
        } catch (DropboxFileSizeException e) {
            // File size too big to upload via the API
            mErrorMsg = "This file is too big to upload";
        } catch (DropboxPartialFileException e) {
            // We canceled the operation
            mErrorMsg = "Upload canceled";
        } catch (DropboxServerException e) {
            // Server-side exception.  These are examples of what could happen,
            // but we don't do anything special with them here.
            if (e.error == DropboxServerException._401_UNAUTHORIZED) {
                // Unauthorized, so we should unlink them.  You may want to
                // automatically log the user out in this case.
            } else if (e.error == DropboxServerException._403_FORBIDDEN) {
                // Not allowed to access this
            } else if (e.error == DropboxServerException._404_NOT_FOUND) {
                // path not found (or if it was the thumbnail, can't be
                // thumbnailed)
            } else if (e.error == DropboxServerException._507_INSUFFICIENT_STORAGE) {
                // user is over quota
            } else {
                // Something else
            }
            // This gets the Dropbox error, translated into the user's language
            mErrorMsg = e.body.userError;
            if (mErrorMsg == null) {
                mErrorMsg = e.body.error;
            }
        } catch (DropboxIOException e) {
            // Happens all the time, probably want to retry automatically.
            mErrorMsg = "Network error.  Try again.";
        } catch (DropboxParseException e) {
            // Probably due to Dropbox server restarting, should retry
            mErrorMsg = "Dropbox error.  Try again.";
        } catch (DropboxException e) {
            // Unknown error
            mErrorMsg = "Unknown error.  Try again.";
        } catch (FileNotFoundException e) {
        }
        return false;
    }

    @Override
    protected void onProgressUpdate(Long... progress) {
        int percent = (int)(100.0*(double)progress[0]/mFileLen + 0.5);
        mDialog.setProgress(percent);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        mDialog.dismiss();
        if (result) {
            showToast("File successfully uploaded");
        } else {
            showToast(mErrorMsg);
        }
    }

    private void showToast(String msg) {
        Toast error = Toast.makeText(mContext, msg, Toast.LENGTH_LONG);
        error.show();
    }
}
