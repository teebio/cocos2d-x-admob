/*
 * Copyright (c) 2013 Daniel Rakos
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.rastergrid;

import java.lang.ref.WeakReference;

import org.cocos2dx.lib.Cocos2dxActivity;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Message;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

public class AdMobHelper
{
    protected static final int ADMOB_HELPER_INIT            = 1;
    protected static final int ADMOB_HELPER_DELETE          = 2;
    protected static final int ADMOB_HELPER_SET_PARENT      = 3;
    protected static final int ADMOB_HELPER_SET_VISIBLE     = 4;
    protected static final int ADMOB_HELPER_SET_ALIGNMENT   = 5;
    protected static final int ADMOB_HELPER_LOAD_AD         = 6;
    protected static final int ADMOB_HELPER_USE_LOCATION    = 7;

    protected static final int USE_LOCATION_NONE            = 0;
    protected static final int USE_LOCATION_COARSE          = 1;
    protected static final int USE_LOCATION_FINE            = 2;

    public static class InitMessage
    {
        public int adSize;
        public String adUnitId;

        public InitMessage(int adSize, String adUnitId)
        {
            this.adSize   = adSize;
            this.adUnitId = adUnitId;
        }
    }

    public static class SetParentMessage
    {
        public int parent;

        public SetParentMessage(int parent)
        {
            this.parent = parent;
        }
    }

    public static class SetVisibleMessage
    {
        public boolean visible;

        public SetVisibleMessage(boolean visible)
        {
            this.visible = visible;
        }
    }

    public static class SetAlignmentMessage
    {
        public int horizontal;
        public int vertical;

        public SetAlignmentMessage(int horizontal, int vertical)
        {
            this.horizontal = horizontal;
            this.vertical	= vertical;
        }
    }

    public static class UseLocationMessage
    {
        public int location;

        public UseLocationMessage(int location)
        {
            this.location = location;
        }
    }

    protected int mParent;
    protected Cocos2dxActivity mActivity;
    protected AdView mAdView;
    protected RelativeLayout mLayout;
    protected Location mLocation;
    protected static Handler mHandler;

    public AdMobHelper(Cocos2dxActivity activity)
    {
        mActivity = activity;

        // Parent is zero by default
        mParent = 0;

        // Create wrapper relative layout
        mLayout = new RelativeLayout(mActivity);

        // Add layout to content view
        activity.addContentView(mLayout, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

        // Init JNI handlers
        initJNI(new WeakReference<AdMobHelper>(this));
        mHandler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                switch (msg.what)
                {
                case ADMOB_HELPER_INIT:
                    {
                        InitMessage msgBody = (InitMessage)msg.obj;
                        AdMobHelper.this.init(msgBody.adSize, msgBody.adUnitId);
                    }
                    break;
                case ADMOB_HELPER_DELETE:
                    {
                        AdMobHelper.this.mAdView = null;
                    }
                    break;
                case ADMOB_HELPER_SET_PARENT:
                    {
                        SetParentMessage msgBody = (SetParentMessage)msg.obj;
                        AdMobHelper.this.setParent(msgBody.parent);
                    }
                    break;
                case ADMOB_HELPER_SET_VISIBLE:
                    {
                        SetVisibleMessage msgBody = (SetVisibleMessage)msg.obj;
                        AdMobHelper.this.setVisible(msgBody.visible);
                    }
                    break;
                case ADMOB_HELPER_SET_ALIGNMENT:
                    {
                        SetAlignmentMessage msgBody = (SetAlignmentMessage)msg.obj;
                        AdMobHelper.this.setAlignment(msgBody.horizontal, msgBody.vertical);
                    }
                    break;
                case ADMOB_HELPER_LOAD_AD:
                    {
                        AdMobHelper.this.loadAd();
                    }
                    break;
                case ADMOB_HELPER_USE_LOCATION:
                    {
                        UseLocationMessage msgBody = (UseLocationMessage)msg.obj;
                        AdMobHelper.this.useLocation(msgBody.location);
                    }
                    break;
                }
                super.handleMessage(msg);
            }
        };
    }

    public void init(int adSize, String adUnitId)
    {
        // If AdView already exists then remove it (two ads cannot be used at the same time)
        if (mAdView != null)
        {
            setParent(0);
            mAdView = null;
        }

        // This array maps integer ad size to the ones accepted by AdMob
        AdSize mapAdSize[] =
        {
            AdSize.SMART_BANNER,
            AdSize.BANNER,
            AdSize.IAB_MRECT,
            AdSize.IAB_BANNER,
            AdSize.IAB_LEADERBOARD,
            AdSize.IAB_WIDE_SKYSCRAPER
        };

        // Create AdView
        mAdView = new AdView(mActivity, mapAdSize[adSize], adUnitId);
        mAdView.setLayoutParams(
                new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    public void setParent(int parent)
    {
        if (parent == 0 && mParent != 0)
        {
            // Remove from layout
            mLayout.removeView(mAdView);
        }
        else
        if (parent != 0 && mParent == 0)
        {
            // Add to layout
            mLayout.addView(mAdView);
        }
        mParent = parent;
    }

    public void setVisible(boolean visible)
    {
        if (mAdView != null)
        {
            mAdView.setVisibility(visible ? AdView.VISIBLE : AdView.INVISIBLE);
        }
    }

    public void setAlignment(int horizontal, int vertical)
    {
        RelativeLayout.LayoutParams layoutParams =
            new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                            ViewGroup.LayoutParams.WRAP_CONTENT);

        layoutParams.addRule(horizontal > 0 ? RelativeLayout.ALIGN_PARENT_RIGHT
                           : horizontal < 0 ? RelativeLayout.ALIGN_PARENT_LEFT
                                            : RelativeLayout.CENTER_HORIZONTAL);
        layoutParams.addRule(vertical > 0 ? RelativeLayout.ALIGN_PARENT_TOP
                           : vertical < 0 ? RelativeLayout.ALIGN_PARENT_BOTTOM
                                          : RelativeLayout.CENTER_VERTICAL);

        mAdView.setLayoutParams(layoutParams);
    }

    public void loadAd()
    {
        // Create ad request and set location
        AdRequest request = new AdRequest();
        request.setLocation(mLocation);

        // Request ad
        mAdView.loadAd(request);
    }

    public void useLocation(int location)
    {
        LocationManager locationManager = (LocationManager)mActivity.getSystemService(Context.LOCATION_SERVICE);

        switch (location)
        {
        case USE_LOCATION_NONE:
            mLocation = null;
            break;
        case USE_LOCATION_COARSE:
            mLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            break;
        case USE_LOCATION_FINE:
            mLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            break;
        }
    }

    private native void initJNI(Object wadk_this);

    private static void nativeInit(int adSize, String adUnitId)
    {
        Message msg = new Message();
        msg.what = ADMOB_HELPER_INIT;
        msg.obj  = new InitMessage(adSize, adUnitId);
        mHandler.sendMessage(msg);
    }

    private static void nativeDelete()
    {
        Message msg = new Message();
        msg.what = ADMOB_HELPER_DELETE;
        mHandler.sendMessage(msg);
    }

    private static void nativeSetParent(int parent)
    {
        Message msg = new Message();
        msg.what = ADMOB_HELPER_SET_PARENT;
        msg.obj  = new SetParentMessage(parent);
        mHandler.sendMessage(msg);
    }

    private static void nativeSetVisible(boolean visible)
    {
        Message msg = new Message();
        msg.what = ADMOB_HELPER_SET_VISIBLE;
        msg.obj  = new SetVisibleMessage(visible);
        mHandler.sendMessage(msg);
    }

    private static void nativeSetAlignment(int horizontal, int vertical)
    {
        Message msg = new Message();
        msg.what = ADMOB_HELPER_SET_ALIGNMENT;
        msg.obj  = new SetAlignmentMessage(horizontal, vertical);
        mHandler.sendMessage(msg);
    }

    private static void nativeLoadAd()
    {
        Message msg = new Message();
        msg.what = ADMOB_HELPER_LOAD_AD;
        mHandler.sendMessage(msg);
    }

    private static void nativeUseLocation(int location)
    {
        Message msg = new Message();
        msg.what = ADMOB_HELPER_USE_LOCATION;
        msg.obj  = new UseLocationMessage(location);
        mHandler.sendMessage(msg);
    }
}
