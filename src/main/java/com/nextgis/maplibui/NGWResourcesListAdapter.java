/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplibui;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.nextgis.maplib.datasource.ngw.Connection;
import com.nextgis.maplib.datasource.ngw.Connections;
import com.nextgis.maplib.datasource.ngw.INGWResource;
import com.nextgis.maplib.datasource.ngw.Resource;
import com.nextgis.maplib.datasource.ngw.ResourceGroup;

import static com.nextgis.maplib.util.Constants.*;
import static com.nextgis.maplibui.R.attr;


public class NGWResourcesListAdapter extends BaseAdapter implements AdapterView.OnItemClickListener
{
    protected Connections             mConnections;
    protected int                     mCurrentResourceId;
    protected INGWResource            mCurrentResource;
    protected SelectNGWResourceDialog mSelectNGWResourceDialog;
    protected boolean                 mLoading;
    protected PathView mPathView;


    public NGWResourcesListAdapter(SelectNGWResourceDialog dialog)
    {
        mSelectNGWResourceDialog = dialog;
        mLoading = false;
    }


    public void setConnections(Connections connections)
    {
        mConnections = connections;
    }


    public Connections getConnections()
    {
        return mConnections;
    }


    public int getCurrentResourceId()
    {
        return mCurrentResourceId;
    }


    public void setCurrentResourceId(int id)
    {
        mCurrentResourceId = id;
        mCurrentResource = mConnections.getResourceById(id);
        if (null != mCurrentResource) {
            notifyDataSetChanged();
            if(null != mPathView)
                mPathView.onUpdate(mCurrentResource);
        }
    }

    public void setPathLayout(LinearLayout linearLayout){
        mPathView = new PathView(linearLayout);
        mPathView.onUpdate(mCurrentResource);
    }

    @Override
    public int getCount()
    {
        if (null == mCurrentResource)
            return 0;
        if (mLoading)
            return 2;
        return mCurrentResource.getChildrenCount() + 1; //add up button or add connections button
    }


    @Override
    public Object getItem(int i)
    {
        if (null == mCurrentResource || mLoading)
            return null;
        if (mCurrentResource.getType() == Connection.NGWResourceTypeConnections) {
            if (i > mCurrentResource.getChildrenCount())
                return null;
            return mCurrentResource.getChild(i);
        } else if (mCurrentResource.getType() == Connection.NGWResourceTypeConnection) {
            if (i == 0)
                return null;
            return mCurrentResource.getChild(i - 1);
        } else if (mCurrentResource.getType() == Connection.NGWResourceTypeResourceGroup) {
            if (i == 0)
                return null;
            return mCurrentResource.getChild(i - 1);
        }
        return null;
    }


    @Override
    public long getItemId(int i)
    {
        INGWResource resource = (INGWResource) getItem(i);
        if (null == resource)
            return NOT_FOUND;
        return resource.getId();
    }


    @Override
    public View getView(
            int i,
            View view,
            ViewGroup viewGroup)
    {
        if (mLoading && i > 0) {
            //show loading view
            return getLoadingView(view);
        }

        switch (mCurrentResource.getType()) {
            case Connection.NGWResourceTypeConnections:
                final Connection connection = (Connection) getItem(i);
                return getConnectionView(connection, view);
            case Connection.NGWResourceTypeConnection:
            case Connection.NGWResourceTypeResourceGroup:
                Resource resource = (Resource) getItem(i);
                return getResourceView(resource, view);
            default:
                return null;
        }
    }


    protected View getLoadingView(View view)
    {
        View v = view;
        if (null == v || v.getId() != R.id.loading_row) {
            Context context = mSelectNGWResourceDialog.getActivity();
            LayoutInflater inflater = LayoutInflater.from(context);
            v = inflater.inflate(R.layout.layout_loading_row, null);
            v.setId(R.id.loading_row);
        }
        return v;
    }


    protected View getConnectionView(
            Connection connection,
            View view)
    {
        View v = view;
        Context context = mSelectNGWResourceDialog.getActivity();
        if (null == connection) { //create add account button
            if (null == v || v.getId() != R.id.resourcegroup_row) {
                LayoutInflater inflater = LayoutInflater.from(context);
                v = inflater.inflate(R.layout.layout_resourcegroup_row, null);
                v.setId(R.id.resourcegroup_row);
            }
            ImageView ivIcon = (ImageView) v.findViewById(R.id.ivIcon);
            ivIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_add_account));

            TextView tvText = (TextView) v.findViewById(R.id.tvName);
            tvText.setText(context.getString(R.string.add_account));

            TextView tvDesc = (TextView) v.findViewById(R.id.tvDesc);
            tvDesc.setText(context.getString(R.string.add_account_summary));
        } else {
            if (null == v || v.getId() != R.id.resourcegroup_row) {
                LayoutInflater inflater = LayoutInflater.from(context);
                v = inflater.inflate(R.layout.layout_resourcegroup_row, null);
                v.setId(R.id.resourcegroup_row);
            }
            ImageView ivIcon = (ImageView) v.findViewById(R.id.ivIcon);
            ivIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_ngw));

            TextView tvText = (TextView) v.findViewById(R.id.tvName);
            tvText.setText(connection.getName());

            TextView tvDesc = (TextView) v.findViewById(R.id.tvDesc);
            tvDesc.setText(context.getString(R.string.ngw_account));
        }

        return v;
    }


    protected View getResourceView(
            Resource resource,
            View view)
    {
        View v = view;
        Context context = mSelectNGWResourceDialog.getActivity();
        if (null == resource) { //create up button
            if (null == v || v.getId() != R.id.resourcegroup_row) {
                LayoutInflater inflater = LayoutInflater.from(context);
                v = inflater.inflate(R.layout.layout_resourcegroup_row, null);
                v.setId(R.id.resourcegroup_row);
            }
            ImageView ivIcon = (ImageView) v.findViewById(R.id.ivIcon);
            ivIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_ngw_folder));

            TextView tvText = (TextView) v.findViewById(R.id.tvName);
            tvText.setText(context.getString(R.string.up_dots));

            TextView tvDesc = (TextView) v.findViewById(R.id.tvDesc);
            tvDesc.setText(context.getString(R.string.up));
        } else {
            if (null == v || v.getId() != R.id.resourcegroup_row) {
                LayoutInflater inflater = LayoutInflater.from(context);
                v = inflater.inflate(R.layout.layout_resourcegroup_row, null);
                v.setId(R.id.resourcegroup_row);
            }
            ImageView ivIcon = (ImageView) v.findViewById(R.id.ivIcon);
            ivIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_ngw_folder));

            TextView tvText = (TextView) v.findViewById(R.id.tvName);
            tvText.setText(resource.getName());

            TextView tvDesc = (TextView) v.findViewById(R.id.tvDesc);
            tvDesc.setText(context.getString(R.string.resource_group));
        }

        return v;
    }

    @Override
    public void onItemClick(
            AdapterView<?> adapterView,
            View view,
            int i,
            long l)
    {
        if (mCurrentResource.getType() == Connection.NGWResourceTypeConnections) {
            if (i >= mCurrentResource.getChildrenCount()) {
                //start add account activity
                mSelectNGWResourceDialog.onAddAccount();
            } else {
                Connection connection = (Connection) mCurrentResource.getChild(i);
                mCurrentResource = connection;
                if (connection.isConnected()) {
                    notifyDataSetChanged();
                } else {
                    NGWResourceAsyncTask task = new NGWResourceAsyncTask(mSelectNGWResourceDialog.getActivity(), connection);
                    task.execute();
                }
            }
        } else if (mCurrentResource.getType() == Connection.NGWResourceTypeConnection ||
                   mCurrentResource.getType() == Connection.NGWResourceTypeResourceGroup) {
            if (i == 0) {
                //go up
                INGWResource resource = mCurrentResource.getParent();
                if(resource instanceof Resource){
                    Resource resourceGroup = (Resource)resource;
                    if(resourceGroup.getRemoteId() == 0){
                        resource = resource.getParent();
                    }
                }
                mCurrentResource = resource;
                notifyDataSetChanged();
            }
            else {
                //go deep
                ResourceGroup resourceGroup = (ResourceGroup) mCurrentResource.getChild(i - 1);
                mCurrentResource = resourceGroup;
                if (resourceGroup.isChildrenLoaded()) {
                    notifyDataSetChanged();
                } else {
                    NGWResourceAsyncTask task = new NGWResourceAsyncTask(mSelectNGWResourceDialog.getActivity(),
                                                                         resourceGroup);
                    task.execute();
                }
            }
        }
        mPathView.onUpdate(mCurrentResource);
    }


    /**
     * A path view class. the path is a resources names divide by arrows in head of dialog.
     * If user click on name, the dialog follow the specified path.
     */
    protected class PathView{
        protected LinearLayout mLinearLayout;


        public PathView(LinearLayout linearLayout)
        {
            mLinearLayout = linearLayout;
        }

        public void onUpdate(INGWResource mCurrentResource){
            if(null == mLinearLayout || null == mCurrentResource)
                return;
            mLinearLayout.removeAllViewsInLayout();
            INGWResource parent = mCurrentResource;
            while (null != parent){
                //skip root resource
                if(parent instanceof Resource){
                    Resource resource = (Resource)parent;
                    if(resource.getRemoteId() == 0){
                        parent = parent.getParent();
                        continue;
                    }
                }

                final int id = parent.getId();
                TextView name = new TextView(mSelectNGWResourceDialog.getActivity());
                String sName = parent.getName();
                name.setText(sName);
                name.setTypeface(name.getTypeface(), Typeface.BOLD);
                name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                name.setSingleLine(true);
                name.setMaxLines(1);
                name.setBackgroundResource(android.R.drawable.list_selector_background);
                name.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        setCurrentResourceId(id);
                    }
                });

                mLinearLayout.addView(name, 0);

                parent = parent.getParent();

                if(null != parent){
                    ImageView image = new ImageView(mSelectNGWResourceDialog.getActivity());
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(30,30);
                    image.setLayoutParams(params);
                    image.setImageDrawable(mSelectNGWResourceDialog.getActivity().getResources().getDrawable(R.drawable.ic_next));
                    mLinearLayout.addView(image, 0);
                }
            }
        }
    }


    /**
     * A async task to execute resources functions (connect, loadChildren, etc.) asynchronously.
     */
    protected class NGWResourceAsyncTask
            extends AsyncTask<Void, Void, Void>
    {
        protected INGWResource mINGWResource;
        protected Context mContext;
        protected String mError;

        public NGWResourceAsyncTask(Context context, INGWResource INGWResource)
        {
            mINGWResource = INGWResource;
            mContext = context;
        }


        @Override
        protected void onPreExecute()
        {
            mLoading = true;
            notifyDataSetChanged();
        }


        @Override
        protected Void doInBackground(Void... voids)
        {
            if(mINGWResource instanceof Connection){
                Connection connection = (Connection)mINGWResource;
                if(connection.connect())
                    connection.loadChildren();
                else
                    mError = mContext.getString(R.string.error_connect_failed);
            }
            else if(mINGWResource instanceof ResourceGroup){
                ResourceGroup resourceGroup = (ResourceGroup)mINGWResource;
                resourceGroup.loadChildren();
            }
            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid)
        {
            if(null != mError && mError.length() > 0){
                Toast.makeText(mContext, mError, Toast.LENGTH_SHORT)
                     .show();
            }
            mLoading = false;
            notifyDataSetChanged();
        }
    }
}