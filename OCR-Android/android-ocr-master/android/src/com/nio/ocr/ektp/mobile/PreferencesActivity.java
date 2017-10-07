package com.nio.ocr.ektp.mobile;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class PreferencesActivity extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {
  public static final String KEY_HELP_VERSION_SHOWN = "preferences_help_version_shown";


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.preferences);

  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,String key) {

    }


  @Override
  protected void onResume() {
    super.onResume();

    getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
  }


  @Override
  protected void onPause() {
    super.onPause();
    getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
  }
}