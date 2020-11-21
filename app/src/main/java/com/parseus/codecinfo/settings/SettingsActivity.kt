package com.parseus.codecinfo.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentTransaction
import androidx.preference.*
import com.dci.dev.appinfobadge.AppInfoBadge
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.parseus.codecinfo.R
import com.parseus.codecinfo.databinding.SettingsMainBinding
import com.parseus.codecinfo.getDefaultThemeOption
import com.parseus.codecinfo.isBatterySaverDisallowed

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: SettingsMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        binding = SettingsMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.content, SettingsFragment()).commit()
        }
    }

    override fun finish() {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(ALIASES_CHANGED, aliasesChanged)
            putExtra(FILTER_TYPE_CHANGED, filterTypeChanged)
            putExtra(SORTING_CHANGED, sortingChanged)
            putExtra(IMMERSIVE_CHANGED, immersiveChanged)
        })
        super.finish()
    }

    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT == 29 && isTaskRoot && supportFragmentManager.backStackEntryCount == 0) {
            // Workaround for a memory leak from https://issuetracker.google.com/issues/139738913
            finishAfterTransition()
        } else {
            super.onBackPressed()
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            findPreference<CheckBoxPreference>("immersive_mode")?.apply {
                if (Build.VERSION.SDK_INT >= 19) {
                    setOnPreferenceChangeListener { _, _ ->
                        immersiveChanged = true
                        true
                    }
                } else {
                    isVisible = false
                }
            }

            findPreference<CheckBoxPreference>("show_aliases")?.apply {
                if (Build.VERSION.SDK_INT >= 29) {
                    setOnPreferenceChangeListener { _, _ ->
                        aliasesChanged = true
                        true
                    }
                } else {
                    isVisible = false
                }
            }

            findPreference<ListPreference>("dark_theme")!!.apply {
                setDarkThemeOptions(this)
                setOnPreferenceChangeListener { _, newValue ->
                    AppCompatDelegate.setDefaultNightMode(DarkTheme.getAppCompatValue((newValue as String).toInt()))
                    true
                }
            }

            val filterType = findPreference<ListPreference>("filter_type")
            filterType!!.setOnPreferenceChangeListener { _, _ ->
                filterTypeChanged = true
                true
            }

            val sortingType = findPreference<ListPreference>("sort_type")
            sortingType!!.setOnPreferenceChangeListener { _, _ ->
                sortingChanged = true
                true
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.preferences_screen)
        }

        @SuppressLint("InflateParams")
        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            return when (preference.key) {
                "feedback" -> {
                    val feedbackEmail = getString(R.string.feedback_email)
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$feedbackEmail")
                        putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject))
                    }
                    if (intent.resolveActivity(requireActivity().packageManager) != null) {
                        startActivity(Intent.createChooser(intent, getString(R.string.choose_email)))
                    } else {
                        val clipboard = ContextCompat.getSystemService(requireContext(), ClipboardManager::class.java)
                        clipboard!!.setPrimaryClip(ClipData.newPlainText("email", feedbackEmail))

                        Snackbar.make(requireActivity().findViewById(android.R.id.content),
                                R.string.no_email_apps, Snackbar.LENGTH_LONG).show()
                    }
                    true
                }

                "help" -> {
                    if (activity != null && Build.VERSION.SDK_INT >= 21) {
                        showNewAppInfoDialog()
                    } else {
                        showOldAppInfoDialog()
                    }
                    true
                }

                else -> super.onPreferenceTreeClick(preference)
            }
        }

        private fun showOldAppInfoDialog() {
            val builder = MaterialAlertDialogBuilder(requireActivity())
            val dialogView = layoutInflater.inflate(R.layout.about_app_dialog, null)
            builder.setView(dialogView)
            val alertDialog = builder.create()

            dialogView.findViewById<View>(R.id.ok_button).setOnClickListener { alertDialog.dismiss() }

            try {
                val versionTextView: TextView = dialogView.findViewById(R.id.version_text_view)
                versionTextView.text = getString(R.string.app_version,
                        requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0).versionName)
            } catch (e: Exception) {}

            alertDialog.show()
        }

        private fun showNewAppInfoDialog() {
            activity?.let {
                val isInDarkMode = (it.resources.configuration.uiMode and UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES
                val appInfoFragment = AppInfoBadge
                        .darkMode { isInDarkMode }
                        .withRater { false }
                        .withPermissions { false }
                        .withEmail { getString(R.string.feedback_email) }
                        .withSite { getString(R.string.source_code_link) }
                        .show()
                it.supportFragmentManager.beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .add(android.R.id.content, appInfoFragment)
                        .addToBackStack(null)
                        .commit()
            }
        }

        private fun setDarkThemeOptions(listPreference: ListPreference) {
            val entries = mutableListOf<CharSequence>()
            val entryValues = mutableListOf<CharSequence>()

            // Light and Dark (always present)
            entries.add(getString(R.string.app_theme_light))
            entries.add(getString(R.string.app_theme_dark))
            entryValues.add(DarkTheme.Light.value.toString())
            entryValues.add(DarkTheme.Dark.value.toString())

            // Set by battery saver (if not blacklisted)
            if (!isBatterySaverDisallowed()) {
                entries.add(getString(R.string.app_theme_battery_saver))
                entryValues.add(DarkTheme.BatterySaver.value.toString())
            }

            // System default (Android 9.0+)
            if (Build.VERSION.SDK_INT >= 28) {
                entries.add(getString(R.string.app_theme_system_default))
                entryValues.add(DarkTheme.SystemDefault.value.toString())
            }

            listPreference.entries = entries.toTypedArray()
            listPreference.entryValues = entryValues.toTypedArray()
            listPreference.setDefaultValue(getDefaultThemeOption().toString())
            listPreference.value = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getString("dark_theme", getDefaultThemeOption().toString())
        }

    }

    companion object {
        var aliasesChanged = false
        var filterTypeChanged = false
        var sortingChanged = false
        var immersiveChanged = false
        const val ALIASES_CHANGED = "aliases_changed"
        const val FILTER_TYPE_CHANGED = "filter_type_changed"
        const val SORTING_CHANGED = "sorting_changed"
        const val IMMERSIVE_CHANGED = "immersive_changed"
    }

}