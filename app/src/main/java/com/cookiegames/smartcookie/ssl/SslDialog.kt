package com.cookiegames.smartcookie. ssl

import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.extensions.inflater
import com.cookiegames.smartcookie.extensions.resizeAndShow
import android.content.Context
import android.net.http.SslCertificate
import android.text.format.DateFormat
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import okio.internal.commonToUtf8String

/**
 * Shows an informative dialog with the provided [SslCertificate] information.
 */
fun Context.showSslDialog(sslCertificate: SslCertificate, sslState: SslState) {
    val by = sslCertificate.issuedBy
    val to = sslCertificate.issuedTo
    val issueDate = sslCertificate.validNotBeforeDate
    val expireDate = sslCertificate.validNotAfterDate
    val cert = sslCertificate.x509Certificate

    val dateFormat = DateFormat.getDateFormat(applicationContext)

    val contentView = inflater.inflate(R.layout.dialog_ssl_info, null, false).apply {
        findViewById<TextView>(R.id.ssl_layout_issue_by).text = by.cName
        findViewById<TextView>(R.id.ssl_layout_issue_to).text = to.oName?.takeIf(String::isNotBlank)
            ?: to.cName
        findViewById<TextView>(R.id.ssl_layout_issue_date).text = dateFormat.format(issueDate)
        findViewById<TextView>(R.id.ssl_layout_expire_date).text = dateFormat.format(expireDate)
        findViewById<TextView>(R.id.ssl_layout_serial_number).text = cert?.sigAlgName
    }

    val icon = createSslDrawableForState(sslState)

    MaterialAlertDialogBuilder(this)
        .setIcon(icon)
        .setTitle(to.cName)
        .setView(contentView)
        .setPositiveButton(R.string.action_ok, null)
        .resizeAndShow()
}
