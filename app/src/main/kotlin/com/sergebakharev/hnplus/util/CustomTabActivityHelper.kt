package com.sergebakharev.hnplus.util

import android.app.Activity
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.sergebakharev.hnplus.model.HNFeedPost

// Copyright 2015 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * This is a helper class to manage the connection to the Custom Tabs Service.
 */
object CustomTabActivityHelper {
    /**
     * Opens the URL on a Custom Tab if possible. Otherwise fallsback to opening it on a WebView.
     *
     * @param activity The host activity.
     * @param customTabsIntent a CustomTabsIntent to be used if Custom Tabs is available.
     * @param post The HNFeedPost article that must be opened
     * @param overrideHtmlProvider something something HTML
     * @param fallback a CustomTabFallback to be used if Custom Tabs is not available.
     */
    fun openCustomTab(
        activity: Activity,
        customTabsIntent: CustomTabsIntent,
        post: HNFeedPost,
        overrideHtmlProvider: String?,
        fallback: CustomTabFallback?
    ) {
        val packageName = CustomTabsHelper.getPackageNameToUse(activity)

        // If we cant find a package name, it means theres no browser that supports
        // Chrome Custom Tabs installed. So, we fallback to the webview
        if (packageName == null) {
            fallback?.openUri(activity, post, overrideHtmlProvider)
        } else {
            customTabsIntent.intent.setPackage(packageName)
            customTabsIntent.launchUrl(activity, Uri.parse(post.uRL))
        }
    }

    /**
     * To be used as a fallback to open the Uri when Custom Tabs is not available.
     */
    interface CustomTabFallback {
        /**
         *
         * @param activity The Activity that wants to open the Uri.
         * @param post The HNFeedPost article that must be opened
         * @param overrideHtmlProvider something something HTML
         */
        fun openUri(activity: Activity, post: HNFeedPost?, overrideHtmlProvider: String?)
    }
}
