package com.LukeVideckis.minesweeper_android.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.LukeVideckis.minesweeper_android.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;

public class LeaderboardActivity extends AppCompatActivity {
    private LeaderboardThread leaderboardThread;

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

        //calls AWS to get leaderboard + update UI
        leaderboardThread = new LeaderboardThread();
        leaderboardThread.start();
    }

    private void updateLeaderboardUITable(JSONArray leaderboardJson) throws JSONException {
        TableLayout leaderboard_ui_table = findViewById(R.id.leaderboard_ui_table);

        //TODO: set limit on player name length
        //aws guarantees leaderboard is sorted by time
        for (int i = 0; i < leaderboardJson.length(); i++) {

            JSONObject leaderboardEntry = leaderboardJson.getJSONObject(i);

            final int paddingAmount = 8;

            TextView rankText = new TextView(this);
            rankText.setText(String.valueOf(i + 1));
            rankText.setPadding(paddingAmount, 0, paddingAmount, 0);

            TextView nameText = new TextView(this);
            nameText.setText(leaderboardEntry.getString("player_name"));
            nameText.setPadding(paddingAmount, 0, paddingAmount, 0);

            TextView completionTimeText = new TextView(this);
            long timeNanoseconds = leaderboardEntry.getLong("completion_time");
            String timeFormatted = String.format("%.2f", timeNanoseconds / 1000000000.0);
            completionTimeText.setText(timeFormatted);
            completionTimeText.setPadding(paddingAmount, 0, paddingAmount, 0);

            TextView dateAddedText = new TextView(this);
            dateAddedText.setText(leaderboardEntry.getString("date_of_score"));
            dateAddedText.setPadding(paddingAmount, 0, paddingAmount, 0);

            TableRow newLeaderboardEntry = new TableRow(this);
            newLeaderboardEntry.addView(rankText);
            newLeaderboardEntry.addView(nameText);
            newLeaderboardEntry.addView(completionTimeText);
            newLeaderboardEntry.addView(dateAddedText);

            leaderboard_ui_table.addView(newLeaderboardEntry);
        }
    }

    private class LeaderboardThread extends Thread {
        @Override
        public void run() {
            try {
                URL url = new URL("https://j8u9lipy35.execute-api.us-east-2.amazonaws.com/get_leaderboard?difficulty_mode=easy_regular");
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("User-Agent", "lrvideckis_minesweeper_android_app");

                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        result.append(line);
                    }
                    JSONArray leaderboardJson = new JSONArray(result.toString());
                    runOnUiThread(() -> {
                        try {
                            updateLeaderboardUITable(leaderboardJson);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                } finally {
                    urlConnection.disconnect();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}