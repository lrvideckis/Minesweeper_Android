package com.LukeVideckis.minesweeper_android.activity.activityHelpers;

import android.app.Dialog;
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
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

            String gameWonGenericText = "You completed " + difficultyStr + " " + modeStr + "-mode minesweeper in " + CompletionTimeFormatter.formatTime(completionTime) + " seconds!";

            //server stores time format as longs to avoid type conversion
            //server stores unique keys (completion time is the key), so I'm
            //assuming no 2 times will have the same exact nano-time
            int leaderboardRank = 5;//TODO - send request to get this info

            if (false) {//TODO
                getNewDialogBuilder()
                        .setMessage(gameWonGenericText + " Either your time is too slow or internet is disabled to make the leaderboard.")
                        .show();
                return;
            }

            AlertDialog.Builder builder = getNewDialogBuilder()
                    .setMessage(gameWonGenericText + " This achieves rank " + leaderboardRank + ".\n\nEnter your name below to add this time to the leaderboard.");

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
                //TODO 2) update dialog to have 2 buttons: cancel and view leaderboard
            });

            //to prevent accidentally tapping outside the dialog, closing it, and losing the chance to add entry to leaderboard.
            builder.setCancelable(false);
            builder.setNegativeButton("Cancel", (dialog, which) -> {
                dialog.dismiss();
            });

            builder.show();

        } else {
            StringBuilder gameWonMessage = new StringBuilder();
            gameWonMessage.append("You completed ");
            try {
                gameWonMessage.append(difficultyDeterminer.getDifficultyAsString() + " ");
            } catch (Exception e) {
                e.printStackTrace();
            }
            gameWonMessage.append(modeStr + "-mode minesweeper in " + CompletionTimeFormatter.formatTime(completionTime) + " seconds!\n\nReason(s) why adding to leaderboard is disabled:\n");
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
                    .show();
        }
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        try {
            ((GameActivity)gameContext).startNewGame();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        try {
            ((GameActivity)gameContext).startNewGame();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class AddEntryToLeaderboardThread extends Thread {
        private String difficultyStr, modeStr, playerName;
        private long completionTime;
        private AlertDialog loadingScreenForLeaderboard;

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
                StringBuilder rawUrl = new StringBuilder("https://j8u9lipy35.execute-api.us-east-2.amazonaws.com");
                rawUrl.append("/add_entry");
                rawUrl.append("?difficulty_mode=" + difficultyStr + "_" + modeStr);
                rawUrl.append("&completion_time=" + completionTime);

                URL url = new URL(rawUrl.toString());

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
                    //TODO: handle exception from `result`
                } finally {
                    urlConnection.disconnect();
                    loadingScreenForLeaderboard.dismiss();
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
