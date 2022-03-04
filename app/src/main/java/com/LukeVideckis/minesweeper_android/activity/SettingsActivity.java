package com.LukeVideckis.minesweeper_android.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.CompoundButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import com.LukeVideckis.minesweeper_android.R;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
    public static final String
            TOGGLE_FLAGS_SETTING = "toggleFlagsSetting",
            GENERATE_GAMES_WITH_8_SETTING = "hasAn8Setting";
    private SharedPreferences sharedPreferences;

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (buttonView.getId() == R.id.defaultSetFlags) {
            SwitchCompat toggleFlagsSetting = findViewById(R.id.defaultSetFlags);
            editor.putBoolean(TOGGLE_FLAGS_SETTING, isChecked);
            toggleFlagsSetting.setChecked(isChecked);
        } else if (buttonView.getId() == R.id.isGuaranteed8) {
            SwitchCompat toggleGamesWith8Setting = findViewById(R.id.isGuaranteed8);
            editor.putBoolean(GENERATE_GAMES_WITH_8_SETTING, isChecked);
            toggleGamesWith8Setting.setChecked(isChecked);
        }
        editor.apply();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish(); // close this activity and return to preview activity (if there is any)
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        SwitchCompat toggleFlagsSetting = findViewById(R.id.defaultSetFlags);
        SwitchCompat toggleGamesWith8Setting = findViewById(R.id.isGuaranteed8);

        sharedPreferences = getSharedPreferences(StartScreenActivity.MY_PREFERENCES, Context.MODE_PRIVATE);

        toggleFlagsSetting.setChecked(sharedPreferences.getBoolean(TOGGLE_FLAGS_SETTING, false));
        toggleGamesWith8Setting.setChecked(sharedPreferences.getBoolean(GENERATE_GAMES_WITH_8_SETTING, false));

        toggleFlagsSetting.setOnCheckedChangeListener(this);
        toggleGamesWith8Setting.setOnCheckedChangeListener(this);
    }
}