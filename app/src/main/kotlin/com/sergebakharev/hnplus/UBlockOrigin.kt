package com.sergebakharev.hnplus

import android.util.Log
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController

object UBlockOrigin {
    const val EXTENSION_ID = "uBlock0@raymondhill.net"
    const val EXTENSION_NAME = "uBlock Origin"
    const val XPI_URI =
        "resource://android/assets/extensions/uBlock0_1.74.0.firefox.signed.xpi"

    private const val TAG = "UBlockOrigin"
    private const val DASHBOARD_PAGE = "dashboard.html"

    class InstallPromptDelegate : WebExtensionController.PromptDelegate {
        override fun onInstallPromptRequest(
            extension: WebExtension,
            permissions: Array<out String?>,
            origins: Array<out String?>,
            dataCollectionPermissions: Array<out String?>
        ): GeckoResult<WebExtension.PermissionPromptResponse?>? {
            if (isUBlock(extension)) {
                Log.i(TAG, "Allow uBlock Origin")
                return GeckoResult.fromValue(
                    WebExtension.PermissionPromptResponse(true, true, false)
                )
            }
            return super.onInstallPromptRequest(
                extension,
                permissions,
                origins,
                dataCollectionPermissions
            )
        }
    }

    fun isUBlock(extension: WebExtension): Boolean {
        return extension.id == EXTENSION_ID || extension.metaData.name == EXTENSION_NAME
    }

    fun optionsPageUrl(extension: WebExtension): String? {
        val options = extension.metaData.optionsPageUrl
        if (!options.isNullOrEmpty()) return options
        val base = extension.metaData.baseUrl
        if (!base.isNullOrEmpty()) {
            return "${base.trimEnd('/')}/$DASHBOARD_PAGE"
        }
        return null
    }

    fun ensureInstalled(runtime: GeckoRuntime): GeckoResult<WebExtension> {
        val controller = runtime.webExtensionController
        controller.promptDelegate = InstallPromptDelegate()

        val result = GeckoResult<WebExtension>()
        var completed = false

        fun complete(extension: WebExtension) {
            if (!completed) {
                completed = true
                result.complete(extension)
            }
        }

        fun fail(error: Throwable) {
            if (!completed) {
                completed = true
                result.completeExceptionally(error)
            }
        }

        fun waitUntilReady() {
            controller.setAddonManagerDelegate(object : WebExtensionController.AddonManagerDelegate {
                override fun onReady(extension: WebExtension) {
                    if (isUBlock(extension)) {
                        complete(extension)
                    }
                }
            })
        }

        fun useOrWait(extension: WebExtension) {
            if (optionsPageUrl(extension) != null) {
                complete(extension)
            } else {
                waitUntilReady()
            }
        }

        controller.list().accept({ extensions ->
            val existing = extensions?.firstOrNull { isUBlock(it) }
            if (existing != null) {
                useOrWait(existing)
                return@accept
            }

            waitUntilReady()
            controller.install(XPI_URI).accept({ installed ->
                if (installed != null && optionsPageUrl(installed) != null) {
                    complete(installed)
                }
            }, { error ->
                Log.e(TAG, "Error installing uBlock Origin", error)
                controller.list().accept({ retry ->
                    val found = retry?.firstOrNull { isUBlock(it) }
                    if (found != null) {
                        useOrWait(found)
                    } else {
                        fail(error ?: Exception("uBlock Origin install failed"))
                    }
                }, { listError ->
                    fail(listError ?: error ?: Exception("uBlock Origin install failed"))
                })
            })
        }, { error ->
            fail(error ?: Exception("Could not list WebExtensions"))
        })

        return result
    }
}
