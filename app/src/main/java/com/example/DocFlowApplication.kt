package com.example

import android.util.Log
import androidx.multidex.MultiDexApplication
import com.pdftron.common.PDFNetException
import com.pdftron.pdf.PDFNet

/**
 * Custom Application class for initializing client-side SDKs including the
 * Apryse (PDFTron) Android SDK for offline, on-device document processing.
 */
class DocFlowApplication : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        initializeApryseSdk()
    }

    private fun initializeApryseSdk() {
        try {
            // Read optional license key from AndroidManifest metadata
            val appInfo = packageManager.getApplicationInfo(
                packageName,
                android.content.pm.PackageManager.GET_META_DATA
            )
            val metaLicenseKey = appInfo.metaData?.getString("pdftron_license_key") ?: ""
            PDFNet.initialize(metaLicenseKey)
            Log.i(TAG, "Apryse PDFNet initialized successfully")

            PDFNet.enableJavaScript(true)
        } catch (e: PDFNetException) {
            Log.e(TAG, "Failed to initialize Apryse PDFNet", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error initializing Apryse SDK", e)
        }
    }

    companion object {
        private const val TAG = "DocFlowApplication"
    }
}
