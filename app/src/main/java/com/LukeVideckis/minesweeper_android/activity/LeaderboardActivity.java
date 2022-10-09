package com.LukeVideckis.minesweeper_android.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.LukeVideckis.minesweeper_android.R;
import com.LukeVideckis.minesweeper_android.miscHelpers.CompletionTimeFormatter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;

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

        String difficultyStr = getIntent().getStringExtra(StartScreenActivity.DIFFICULTY_STR);
        String gameModeStr = getIntent().getStringExtra(StartScreenActivity.GAME_MODE);

        TextView difficultyModeText = findViewById(R.id.leaderboardTitleText);
        difficultyModeText.setText(convertCaseForUI(difficultyStr + " " + gameModeStr + "-mode"));

        AlertDialog noInternetDialog = new AlertDialog.Builder(this)
                .setMessage("Enable internet to load the leaderboard.")
                .create();

        AlertDialog loadingScreenForGetLeaderboard = new AlertDialog.Builder(this)
                .setMessage("Loading " + difficultyStr + ", " + gameModeStr + "-mode leaderboard")
                .show();

        //calls AWS to get leaderboard + update UI
        new LeaderboardThread(difficultyStr, gameModeStr, noInternetDialog, loadingScreenForGetLeaderboard).start();
    }

    private String convertCaseForUI(String text) {
        String[] words = text.replace('-', ' ').split(" ");
        StringBuilder titleCase = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                titleCase.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
            }
        }
        return titleCase.toString();
    }

    private void updateLeaderboardUITable(JSONArray leaderboardJson) throws JSONException {
        TableLayout leaderboard_ui_table = findViewById(R.id.leaderboard_ui_table);

        //TODO: set limit on player name length
        //TODO: test font on smaller screen
        //aws guarantees leaderboard is sorted by time
        for (int i = 0; i < leaderboardJson.length(); i++) {

            JSONObject leaderboardEntry = leaderboardJson.getJSONObject(i);

            final int paddingAmount = 8;

            TextView rankText = new TextView(this);
            rankText.setText(String.valueOf(i + 1));
            rankText.setPadding(paddingAmount, 0, paddingAmount, 0);
            rankText.setTextSize(20);

            TextView nameText = new TextView(this);
            nameText.setText(leaderboardEntry.getString("player_name"));
            nameText.setPadding(paddingAmount, 0, paddingAmount, 0);
            nameText.setTextSize(20);

            TextView completionTimeText = new TextView(this);
            long timeNanoseconds = leaderboardEntry.getLong("completion_time");
            completionTimeText.setText(CompletionTimeFormatter.formatTime(timeNanoseconds));
            completionTimeText.setPadding(paddingAmount, 0, paddingAmount, 0);
            completionTimeText.setTextSize(20);

            TextView dateAddedText = new TextView(this);
            dateAddedText.setText(leaderboardEntry.getString("date_of_score"));
            dateAddedText.setPadding(paddingAmount, 0, paddingAmount, 0);
            dateAddedText.setTextSize(20);

            TableRow newLeaderboardEntry = new TableRow(this);
            newLeaderboardEntry.addView(rankText);
            newLeaderboardEntry.addView(nameText);
            newLeaderboardEntry.addView(completionTimeText);
            newLeaderboardEntry.addView(dateAddedText);

            leaderboard_ui_table.addView(newLeaderboardEntry);
        }
    }

    private class LeaderboardThread extends Thread {
        private final String difficultyStr, gameModeStr;
        private final AlertDialog noInternetDialog, loadingScreenForGetLeaderboard;

        LeaderboardThread(String difficultyStr, String gameModeStr, AlertDialog noInternetDialog, AlertDialog loadingScreenForGetLeaderboard) {
            this.difficultyStr = difficultyStr;
            this.gameModeStr = gameModeStr;
            this.noInternetDialog = noInternetDialog;
            this.loadingScreenForGetLeaderboard = loadingScreenForGetLeaderboard;
        }

        @Override
        public void run() {
            try {
                String urlRaw = "https://j8u9lipy35.execute-api.us-east-2.amazonaws.com" + "/get_leaderboard" +
                        "?difficulty_mode=" + difficultyStr + "_" + gameModeStr;

                URL url = new URL(urlRaw);
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
                            loadingScreenForGetLeaderboard.dismiss();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                } catch (UnknownHostException e) {//internet is disabled
                    e.printStackTrace();
                    runOnUiThread(loadingScreenForGetLeaderboard::dismiss);
                    runOnUiThread(noInternetDialog::show);
                } finally {
                    urlConnection.disconnect();
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }
    }
}