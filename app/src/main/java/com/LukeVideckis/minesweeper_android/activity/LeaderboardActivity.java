package com.LukeVideckis.minesweeper_android.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.LukeVideckis.minesweeper_android.R;

import java.util.Objects;

public class LeaderboardActivity extends AppCompatActivity {

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
        setContentView(R.layout.leaderboard);
        Toolbar toolbar = findViewById(R.id.leaderboard_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        TableLayout leaderboard_ui_table = findViewById(R.id.leaderboard_ui_table);

        for(int i = 0; i < 100; i++) {
            TextView rankText = new TextView(this);
            rankText.setText(String.valueOf(i + 1));

            TextView nameText = new TextView(this);
            nameText.setText("player example name");

            TextView completionTimeText = new TextView(this);
            completionTimeText.setText("123");

            TextView dateAddedText = new TextView(this);
            dateAddedText.setText("Oct 2 2022");

            TableRow newLeaderboardEntry = new TableRow(this);
            newLeaderboardEntry.addView(rankText);
            newLeaderboardEntry.addView(nameText);
            newLeaderboardEntry.addView(completionTimeText);
            newLeaderboardEntry.addView(dateAddedText);

            leaderboard_ui_table.addView(newLeaderboardEntry);
        }
    }

}