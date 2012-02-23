/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This file is part of FasterGPS.
 * 
 * FasterGPS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FasterGPS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FasterGPS.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.fastergps.ui;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.fastergps.R;
import org.fastergps.util.Constants;
import org.fastergps.util.Log;
import org.fastergps.util.Utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

public class AdvancedSettingsActivity extends PreferenceActivity {

    // Intent extras to config read in base activity
    public static final String EXTRA_CONFIG = "org.fastergps.config";

    private Activity mActivity;
    private HashMap<String, String> config;
    private HashMap<String, String> configCopy;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = this;

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.containsKey(EXTRA_CONFIG)) {

                config = (HashMap<String, String>) extras.getSerializable(EXTRA_CONFIG);

                Log.i(Constants.TAG, "Result from intent extras:");
                Utils.logConfig(config);
            }
        }

        // add possible other config options
        HashMap<String, String> possibleConfig = Utils.getPossibleConfig();
        possibleConfig.putAll(config);
        // swap
        config = possibleConfig;

        // copy to remove from while building
        configCopy = (HashMap<String, String>) config.clone();

        setPreferenceScreen(createPreferenceHierarchy());

    }

    /**
     * Set summary of preference based on key to get String from strings.xml and current value
     * 
     * @param preference
     * @param key
     * @param value
     */
    private void setSummary(Preference preference, String key, String value) {
        String summary = "";
        try {
            summary = Utils.getResourceString(key, mActivity);
        } catch (IllegalArgumentException e) {
            Log.e(Constants.TAG, "No summary in strings.xml for " + key);
        }

        if (summary.equals("")) {
            preference.setSummary(getString(R.string.pref_current_value) + " " + value);
        } else {
            preference.setSummary(summary + "\n" + getString(R.string.pref_current_value) + " "
                    + value);
        }
    }

    /**
     * Adds all keys that contain keyContains to the category
     * 
     * @param category
     * @param keyContains
     */
    private void addPreferencesToCategory(PreferenceCategory category, String keyContains) {
        Iterator<Entry<String, String>> it = configCopy.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry<String, String> pairs = it.next();

            Log.d(Constants.TAG, pairs.getKey() + " = " + pairs.getValue());

            // add preference not for NTP_SERVER and only for those who contain String keyContains
            if (!pairs.getKey().equals("NTP_SERVER")
                    && (pairs.getKey().contains(keyContains) || keyContains.equals(""))) {
                // add edit text preference
                EditTextPreference preference = new EditTextPreference(this);
                preference.setDialogTitle(pairs.getKey());
                preference.setKey(pairs.getKey());
                preference.setTitle(pairs.getKey());

                setSummary(preference, pairs.getKey(), pairs.getValue());
                preference.setDefaultValue(pairs.getValue());

                preference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        Log.d(Constants.TAG, preference.getKey() + " changed!");

                        config.put(preference.getKey(), (String) newValue);
                        Log.d(Constants.TAG, "Following config will now be written:");
                        Utils.logConfig(config);
                        Utils.writeConfig(mActivity, config);
                        setSummary(preference, preference.getKey(), (String) newValue);

                        return true;
                    }
                });
                category.addPreference(preference);
                // remove from configCopy
                it.remove();
            }
        }
    }

    /**
     * Build preferences with categories
     * 
     * @return
     */
    private PreferenceScreen createPreferenceHierarchy() {
        // Root
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(mActivity);

        // General
        PreferenceCategory generalCat = new PreferenceCategory(mActivity);
        generalCat.setTitle(R.string.pref_category_general);
        root.addPreference(generalCat);

        addPreferencesToCategory(generalCat, "DEBUG_LEVEL");
        addPreferencesToCategory(generalCat, "INTERMEDIATE_POS");
        addPreferencesToCategory(generalCat, "ACCURACY_THRES");
        addPreferencesToCategory(generalCat, "REPORT_POSITION_USE_SUPL_REFLOC");
        addPreferencesToCategory(generalCat, "ENABLE_WIPER");

        // XTRA
        PreferenceCategory xtraCat = new PreferenceCategory(mActivity);
        xtraCat.setTitle(R.string.pref_category_xtra);
        root.addPreference(xtraCat);

        addPreferencesToCategory(xtraCat, "XTRA");

        // AGPS SUPL
        PreferenceCategory suplCat = new PreferenceCategory(mActivity);
        suplCat.setTitle(R.string.pref_category_supl);
        root.addPreference(suplCat);

        addPreferencesToCategory(suplCat, "SUPL");

        // AGPS C2K
        PreferenceCategory c2kCat = new PreferenceCategory(mActivity);
        c2kCat.setTitle(R.string.pref_category_c2k);
        root.addPreference(c2kCat);

        addPreferencesToCategory(c2kCat, "C2K");

        // AGPS Carrier
        PreferenceCategory agpsCat = new PreferenceCategory(mActivity);
        agpsCat.setTitle(R.string.pref_category_carrier);
        root.addPreference(agpsCat);

        addPreferencesToCategory(agpsCat, "CURRENT_CARRIER");
        addPreferencesToCategory(agpsCat, "DEFAULT_AGPS_ENABLE");
        addPreferencesToCategory(agpsCat, "DEFAULT_SSL_ENABLE");

        // Other
        PreferenceCategory otherCat = new PreferenceCategory(mActivity);
        otherCat.setTitle(R.string.pref_category_other);
        root.addPreference(otherCat);

        addPreferencesToCategory(otherCat, "");

        return root;
    }
}