package com.akersp.magneticscalespeedometer

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val aboutRoot: View = findViewById(R.id.about_root)
        // Optional: Add a Toolbar and Up button
        val toolbar: Toolbar? = findViewById(R.id.about_toolbar) // Add a Toolbar with this ID in activity_about.xml if desired
        toolbar?.let {
            setSupportActionBar(it)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = getString(R.string.title_activity_about)
        }

        ViewCompat.setOnApplyWindowInsetsListener(aboutRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        val versionTextView: TextView = findViewById(R.id.versionTextView)
        try {
            val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            val versionName = packageInfo.versionName
            versionTextView.text = getString(R.string.version_placeholder, versionName)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            versionTextView.text = getString(R.string.version_placeholder, "N/A")
        }

        val aboutDescriptionTextView: TextView = findViewById(R.id.aboutDescriptionTextView)
        val htmlDescription = getString(R.string.about_page_description)

        // Use Html.fromHtml to set the text
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            aboutDescriptionTextView.text = Html.fromHtml(htmlDescription, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            aboutDescriptionTextView.text = Html.fromHtml(htmlDescription)
        }
        // Make links clickable
        aboutDescriptionTextView.movementMethod = LinkMovementMethod.getInstance()

    }

    // Handle Up button press in the toolbar (if you added one)
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}