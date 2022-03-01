package com.LukeVideckis.minesweeper_android.miscHelpers;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import com.LukeVideckis.minesweeper_android.R;
import com.LukeVideckis.minesweeper_android.view.GameCanvas;

public class PopupHelper {
    private PopupHelper() throws Exception {
        throw new Exception("No instances allowed!");
    }

    public static PopupWindow initializePopup(Context context, int layoutId) {
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        @SuppressLint("InflateParams") final View view = inflater.inflate(layoutId, null);
        final PopupWindow popup = new PopupWindow(
                view,
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        popup.setFocusable(true);
        popup.setElevation(5.0f);
        return popup;
    }

    public static void dimBehind(PopupWindow popupWindow) {
        View container = popupWindow.getContentView().getRootView();
        Context context = popupWindow.getContentView().getContext();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams p = (WindowManager.LayoutParams) container.getLayoutParams();
        p.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        p.dimAmount = 0.5f;
        wm.updateViewLayout(container, p);
    }

    public static void displayPopup(PopupWindow popup, View parentView, Resources resources) {
        if (parentView.getTag().equals(resources.getString(R.string.is_linear_layout))) {
            LinearLayout linearLayout = (LinearLayout) parentView;
            popup.showAtLocation(linearLayout, Gravity.CENTER, 0, 0);
        } else if (parentView.getTag().equals(resources.getString(R.string.is_game_canvas_layout))) {
            GameCanvas gameCanvasLayout = (GameCanvas) parentView;
            popup.showAtLocation(gameCanvasLayout, Gravity.CENTER, 0, 0);
        } else if (parentView.getTag().equals(resources.getString(R.string.is_relative_layout))) {
            RelativeLayout relativeLayout = (RelativeLayout) parentView;
            popup.showAtLocation(relativeLayout, Gravity.CENTER, 0, 0);
        }
        dimBehind(popup);
    }
}
