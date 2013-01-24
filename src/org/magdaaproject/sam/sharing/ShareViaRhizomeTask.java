/*
 * Copyright (C) 2013 The Serval Project
 * Portions Copyright (C) 2012, 2013 The MaGDAA Project
 *
 * This file is part of the Serval SAM Software, a fork of the MaGDAA SAM software
 * which is located here: https://github.com/magdaaproject/survey-acquisition-management
 *
 * Serval SAM Software is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
/*
 * Copyright (C) 2012, 2013 The MaGDAA Project
 *
 * This file is part of the MaGDAA SAM Software
 *
 * MaGDAA SAM Software is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.magdaaproject.sam.sharing;

import java.io.File;
import java.io.IOException;
import java.util.zip.Deflater;

import org.servalproject.sam.R;
import org.magdaaproject.utils.FileUtils;
import org.magdaaproject.utils.serval.RhizomeUtils;
import org.odk.collect.InstanceProviderAPI;
import org.zeroturnaround.zip.ZipException;
import org.zeroturnaround.zip.ZipUtil;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

/**
 * a class used to archive and share an instance file on the Serval Mesh via Rhizome
 *
 */
public class ShareViaRhizomeTask extends AsyncTask<Void, Void, Integer> {
	
	/*
	 * private class level constants
	 */
	//private static final boolean sVerboseLog = true;
	private static final String sLogTag = "ShareViarRhizomeTask";
	
	private static final int sMaxLoops = 10;
	private static final int sSleepTime = 2000;
	
	/*
	 * private class level variables
	 */
	private Context context;
	private Uri     instanceUri;
	
	/**
	 * construct a new task object with required variables
	 * 
	 * @param context a context that can be used to get system resources
	 * 
	 * @param instanceUri the URI to the new instance record
	 */
	public ShareViaRhizomeTask(Context context, Uri instanceUri) {
		this.context = context;
		this.instanceUri = instanceUri;
	}

	/*
	 * (non-Javadoc)
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	@Override
	protected Integer doInBackground(Void... arg0) {
		
		// get information about this instance
		ContentResolver mContentResolver = context.getContentResolver();
		
		String[] mProjection = new String[2];
		mProjection[0] = InstanceProviderAPI.InstanceColumns.STATUS;
		mProjection[1] = InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH;
		
		Cursor mCursor;
		
		// get the data, checking until the instance is finalised
		boolean mHaveInstance = false;
		int     mLoopCount = 0;
		
		String mInstancePath = null;
		
		while(mHaveInstance == false && mLoopCount <= sMaxLoops) {
			
			mCursor = mContentResolver.query(
					instanceUri,
					mProjection,
					null,
					null,
					null);
			
			// check on the status of the instance
			if(mCursor != null && mCursor.getCount() > 0) {
				
				mCursor.moveToFirst();
				
				// status is "complete" ODK has finished with the instance
				if(mCursor.getString(0).equals(InstanceProviderAPI.STATUS_COMPLETE) == true) {
					mInstancePath = mCursor.getString(1);
				}
				
			}
			
			if(mCursor != null) {
				mCursor.close();
			}
			
			// sleep the thread, an extra sleep even if the instance is finalised won't hurt
			mLoopCount++;
			try {
				Thread.sleep(sSleepTime);
			} catch (InterruptedException e) {
				Log.w(sLogTag, "thread interrupted during sleep unexpectantly", e);
				return null;
			}
		}
		
		// check to see if an instance file was found
		if(mInstancePath == null) {
			return null;
		}
		
		// parse the instance path
		File mInstanceFile = new File(mInstancePath);
		
		// check to make sure file is accessible
		if(FileUtils.isFileReadable(mInstancePath) == false) {
			Log.w(sLogTag, "instance file is not accessible '" + mInstancePath + "'");
			return null;
		}
		
		// create a zip file of the instance directory
		String mTempPath = Environment.getExternalStorageDirectory().getPath();
		mTempPath += context.getString(R.string.system_file_path_rhizome_data);
		mTempPath += mInstanceFile.getName() + ".instance.sam.magdaa";
		
		try {
			ZipUtil.pack(
					new File(mInstanceFile.getParent()),
					new File(mTempPath),
					Deflater.BEST_COMPRESSION);
		} catch (ZipException e) {
			Log.e(sLogTag, "unable to create the zip file", e);
			return null;
		}
		
		// share the file via Rhizome
		try {
			if(RhizomeUtils.shareFile(context, mTempPath)) {
				Log.i(sLogTag, "new instance file shared via Rhizome '" + mTempPath + "'");
				return 0;
			} else {
				return null;
			}
		} catch (IOException e) {
			Log.e(sLogTag, "unable to share the zip file", e);
			return null;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
	 */
	@Override
    protected void onPostExecute(Integer result) {
		
		//TODO determine if need do anything once file shared, especially on UI thread?
		
	}

}