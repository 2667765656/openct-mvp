package cc.metapro.openct.preference;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import butterknife.BindView;
import butterknife.ButterKnife;
import cc.metapro.openct.R;
import cc.metapro.openct.utils.Constants;
import cc.metapro.openct.widget.DailyClassWidget;

public class SettingsActivity extends AppCompatActivity {

    private static Preference.OnPreferenceChangeListener
            sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);
                preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
            } else {
                preference.setSummary(stringValue);
            }

            if (preference.getKey().equals(Constants.PREF_CURRENT_WEEK_KEY)) {
                DailyClassWidget.update(preference.getContext());
            }

            if (preference.getKey().equals(Constants.PREF_CMS_PASSWORD_KEY)) {
                String prev = preference.getSharedPreferences()
                        .getString(Constants.PREF_CMS_PASSWORD_KEY, stringValue);

                if (!stringValue.equals(prev)) {
                    SharedPreferences.Editor editor = preference.getEditor();
                    editor.putBoolean(Constants.PREF_CMS_PASSWORD_ENCRYPTED, false);
                    editor.apply();
                }
            }

            if (preference.getKey().equals(Constants.PREF_LIB_PASSWORD_KEY)) {
                String prev = preference.getSharedPreferences()
                        .getString(Constants.PREF_LIB_PASSWORD_KEY, stringValue);

                if (!stringValue.equals(prev)) {
                    SharedPreferences.Editor editor = preference.getEditor();
                    editor.putBoolean(Constants.PREF_LIB_PASSWORD_ENCRYPTED, false);
                    editor.apply();
                }
            }
            return true;
        }
    };
    @BindView(R.id.pref_toolbar)
    Toolbar mToolbar;

    private static void bindSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
    }

    private static void passwordOperation(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preference);

        ButterKnife.bind(this);

        // setup toolbar
        setSupportActionBar(mToolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        getFragmentManager().beginTransaction()
                .replace(R.id.pref_container, new SchoolPreferenceFragment())
                .commit();
    }

    public static class SchoolPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_school);
            setHasOptionsMenu(false);

            bindSummaryToValue(findPreference(Constants.PREF_SCHOOL_NAME_KEY));
            bindSummaryToValue(findPreference(Constants.PREF_CURRENT_WEEK_KEY));
            bindSummaryToValue(findPreference(Constants.PREF_CMS_USERNAME_KEY));
            bindSummaryToValue(findPreference(Constants.PREF_LIB_USERNAME_KEY));

            passwordOperation(findPreference(Constants.PREF_CMS_PASSWORD_KEY));
            passwordOperation(findPreference(Constants.PREF_LIB_PASSWORD_KEY));
        }
    }
}
