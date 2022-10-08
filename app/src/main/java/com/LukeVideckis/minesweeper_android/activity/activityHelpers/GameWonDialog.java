package com.LukeVideckis.minesweeper_android.activity.activityHelpers;

import static java.lang.Integer.parseInt;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import com.LukeVideckis.minesweeper_android.R;
import com.LukeVideckis.minesweeper_android.activity.GameActivity;
import com.LukeVideckis.minesweeper_android.miscHelpers.CompletionTimeFormatter;
import com.LukeVideckis.minesweeper_android.miscHelpers.GameModeConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;

//handles logic+flow for the AlertDialog when user finishes a game
//conditions allowing user to add entry to leaderboard:
//  - user didn't use hints throughout game (mine probability + deducible squares)
//  - "generate board with 8" option *disabled* (as it makes it slightly easier)
//  - user is playing on beginner, intermediate, expert mode
//  - user's time places them in top 100 on specific leaderboard

public class GameWonDialog implements DialogInterface.OnCancelListener, DialogInterface.OnDismissListener {

    private DifficultyDeterminer difficultyDeterminer;
    private int gameMode;
    private boolean hasAn8;
    private Context gameContext;

    public GameWonDialog(Context gameContext, int numberOfRows, int numberOfCols, int numberOfMines, int gameMode, boolean hasAn8) {
        difficultyDeterminer = new DifficultyDeterminer(numberOfRows, numberOfCols, numberOfMines);
        this.gameMode = gameMode;
        this.gameContext = gameContext;
        this.hasAn8 = hasAn8;
    }

    private AlertDialog.Builder getNewDialogBuilder() {
        return new AlertDialog.Builder(gameContext)
                .setOnCancelListener(this)
                .setOnDismissListener(this);
    }

    public void showGameWonDialog(long completionTime, boolean usedHelpDuringGame) throws Exception {
        String modeStr;
        if (gameMode == R.id.normal_mode) {
            modeStr = GameModeConstants.NORMAL_MODE;
        } else if (gameMode == R.id.no_guessing_mode) {
            modeStr = GameModeConstants.NO_GUESS_MODE;
        } else {
            modeStr = GameModeConstants.GET_HELP_MODE;
        }

        if (difficultyDeterminer.isStandardDifficulty() && !usedHelpDuringGame && !hasAn8) {
            String difficultyStr = difficultyDeterminer.getDifficultyAsString();
            AlertDialog loadingGetRank =
                    getNewDialogBuilder()
                            .setMessage("Checking leaderboard")
                            .create();
            new CheckLeaderboardForRankThread(difficultyStr, modeStr, completionTime, loadingGetRank, (Activity) gameContext).start();

        } else {
            StringBuilder gameWonMessage = new StringBuilder();
            gameWonMessage.append("You completed ");
            try {
                gameWonMessage.append(difficultyDeterminer.getDifficultyAsString()).append(" ");
            } catch (Exception e) {
                e.printStackTrace();
            }
            gameWonMessage.append(modeStr).append("-mode minesweeper in ").append(CompletionTimeFormatter.formatTime(completionTime)).append(" seconds!\n\nReason(s) why adding to leaderboard is disabled:\n");
            if (!difficultyDeterminer.isStandardDifficulty()) {
                gameWonMessage.append("- Game dimension is not one of: beginner, intermediate, or expert.\n");
            }
            if (hasAn8) {
                gameWonMessage.append("- Generate boards with an 8 is enabled.\n");
            }
            if (usedHelpDuringGame) {
                gameWonMessage.append("- You used help during the game (Deducible Squares or Mine Probability).\n");
            }
            getNewDialogBuilder()
                    .setMessage(gameWonMessage)
                    .setCancelable(false)
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();
        }
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        try {
            ((GameActivity) gameContext).startNewGame();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        try {
            ((GameActivity) gameContext).startNewGame();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class AddEntryToLeaderboardThread extends Thread {
        private final String difficultyStr, modeStr, playerName;
        private final long completionTime;
        private final AlertDialog loadingScreenForLeaderboard;

        AddEntryToLeaderboardThread(String difficultyStr, String modeStr, String playerName, long completionTime, AlertDialog loadingScreenForLeaderboard) {
            this.difficultyStr = difficultyStr;
            this.modeStr = modeStr;
            this.playerName = playerName;
            this.completionTime = completionTime;
            this.loadingScreenForLeaderboard = loadingScreenForLeaderboard;
        }

        @Override
        public void run() {
            try {
                //if you send dishonest requests to this endpoint, you're actually a piece of trash
                //like congrats bro!!!!! you now added a time to the leaderboard which you didn't achieve. congrats!!!!
                //have fun going through life being a liar!!!
                //this is personal
                //if I catch you, I will tweet about it as you deserve PUBLIC SHAME
                String rawUrl = "https://j8u9lipy35.execute-api.us-east-2.amazonaws.com" + "/add_entry" +
                        "?difficulty_mode=" + difficultyStr + "_" + modeStr +
                        "&completion_time=" + completionTime;

                URL url = new URL(rawUrl);

                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("PUT");
                urlConnection.setRequestProperty("User-Agent", "lrvideckis_minesweeper_android_app");
                urlConnection.setRequestProperty("content-type", "application/json");

                OutputStream os = urlConnection.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
                JSONObject playerNameObj = new JSONObject();
                playerNameObj.put("player_name", playerName);
                osw.write(playerNameObj.toString());
                osw.flush();
                osw.close();
                os.close();

                try {
                    urlConnection.connect();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        result.append(line);
                    }
                } finally {
                    urlConnection.disconnect();
                    loadingScreenForLeaderboard.dismiss();
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class CheckLeaderboardForRankThread extends Thread {
        private final String difficultyStr, modeStr;
        private final long completionTime;
        private final AlertDialog loadingGetRank;
        private final Activity gameActivity;

        CheckLeaderboardForRankThread(String difficultyStr, String modeStr, long completionTime, AlertDialog loadingGetRank, Activity gameActivity) {
            this.difficultyStr = difficultyStr;
            this.modeStr = modeStr;
            this.completionTime = completionTime;
            this.loadingGetRank = loadingGetRank;
            this.gameActivity = gameActivity;

        }

        private void handleGameWonCase(int rank) {
            String gameWonGenericText = "You completed " + difficultyStr + " " + modeStr + "-mode minesweeper in " + CompletionTimeFormatter.formatTime(completionTime) + " seconds!";
            AlertDialog.Builder builder = getNewDialogBuilder()
                    .setMessage(gameWonGenericText + " This achieves rank " + rank + ".\n\nEnter your name below to add this time to the leaderboard.");

            EditText playerNameInput = new EditText(gameContext);
            final int maxLength = 15;
            playerNameInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
            playerNameInput.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            builder.setView(playerNameInput);

            //onClick is run on the main thread
            builder.setPositiveButton("Add Entry to Leaderboard", (dialog, which) -> {
                String playerName = playerNameInput.getText().toString();
                AlertDialog loadingScreenForLeaderboard = getNewDialogBuilder()
                        .setMessage("Adding entry to leaderboard")
                        .create();
                loadingScreenForLeaderboard.show();
                new AddEntryToLeaderboardThread(difficultyStr, modeStr, playerName, completionTime, loadingScreenForLeaderboard).start();
            });

            //to prevent accidentally tapping outside the dialog, closing it, and losing the chance to add entry to leaderboard.
            builder.setCancelable(false);
            builder.setNegativeButton("Cancel", (dialog, which) -> {
                dialog.dismiss();
            });

            gameActivity.runOnUiThread(builder::show);
        }

        @Override
        public void run() {
            try {
                gameActivity.runOnUiThread(loadingGetRank::show);

                String rawUrl = "https://j8u9lipy35.execute-api.us-east-2.amazonaws.com" + "/get_rank" +
                        "?difficulty_mode=" + difficultyStr + "_" + modeStr +
                        "&completion_time=" + completionTime;
                URL url = new URL(rawUrl);

                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("User-Agent", "lrvideckis_minesweeper_android_app");

                try {
                    urlConnection.connect();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        result.append(line);
                    }
                    int leaderboardRank = parseInt(result.toString());
                    gameActivity.runOnUiThread(loadingGetRank::dismiss);
                    handleGameWonCase(leaderboardRank);

                } catch (FileNotFoundException e) {//time too slow to make leaderboard
                    gameActivity.runOnUiThread(() -> {
                        loadingGetRank.dismiss();
                        getNewDialogBuilder()
                                .setMessage("Your time is too slow to make the leaderboard.")
                                .show();
                    });
                } catch (UnknownHostException e) {//internet is disabled
                    gameActivity.runOnUiThread(loadingGetRank::dismiss);
                    gameActivity.runOnUiThread(() ->
                            getNewDialogBuilder()
                                    .setMessage("Enable internet to add time to leaderboard.")
                                    .show()
                    );
                    e.printStackTrace();
                } finally {
                    urlConnection.disconnect();
                    //loadingScreenForLeaderboard.dismiss();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
