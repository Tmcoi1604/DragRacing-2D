package com.dragracing.game.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;
import com.dragracing.game.R;
import com.dragracing.game.data.PlayerData;
import com.dragracing.game.engine.RaceEngine;
import java.util.Locale;

public class ResultDialog extends Dialog {
    public interface DialogActionCallback {
        void onRematch();
        void onGarage();
        void onContinue();
    }

    private final RaceEngine engine;
    private final DialogActionCallback callback;

    public ResultDialog(Context context, RaceEngine engine, DialogActionCallback callback) {
        super(context);
        this.engine = engine;
        this.callback = callback;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_result);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        setCancelable(false);

        TextView tvTitle = findViewById(R.id.tvResultTitle);
        TextView tvPrize = findViewById(R.id.tvPrizeCash);
        TextView tvBasePrize = findViewById(R.id.tvBasePrize);
        TextView tvLaunchBonus = findViewById(R.id.tvLaunchBonus);
        TextView tvShiftBonus = findViewById(R.id.tvShiftBonus);
        TextView tvOvertakeBonus = findViewById(R.id.tvOvertakeBonus);
        TextView tvPlayerET = findViewById(R.id.tvPlayerET);
        TextView tvOpponentET = findViewById(R.id.tvOpponentET);
        TextView tvTopSpeed = findViewById(R.id.tvTopSpeed);
        TextView tvPerfectShifts = findViewById(R.id.tvPerfectShifts);
        ImageButton btnRematch = findViewById(R.id.btnRematch);
        ImageButton btnGarage = findViewById(R.id.btnGarage);
        ImageButton btnContinue = findViewById(R.id.btnContinue);

        // Apply Pixel Font to entire dialog
        Typeface pixelFont = androidx.core.content.res.ResourcesCompat.getFont(getContext(), R.font.pixel_font);
        applyFontToViewTree(findViewById(android.R.id.content), pixelFont);

        int prize = engine.calculatePrizeMoney();

        // Title and Colors
        if (engine.isFalseStart()) {
            tvTitle.setText("XUẤT PHÁT SỚM (FALSE START)");
            tvTitle.setTextColor(Color.parseColor("#FF1744"));
        } else if (engine.isPlayerWon()) {
            if (engine.getOpponentCar() != null && engine.getOpponentCar().getCar().getName().startsWith("BOSS")) {
                tvTitle.setText("BOSS ĐÃ BỊ HẠ GỤC!");
            } else {
                tvTitle.setText("CHIẾN THẮNG! (VICTORY)");
            }
            tvTitle.setTextColor(Color.parseColor("#FFD600"));
        } else {
            tvTitle.setText("THẤT BẠI! (DEFEATED)");
            tvTitle.setTextColor(Color.parseColor("#FF5252"));
        }

        tvPrize.setText(String.format(Locale.US, "+$%d", prize));
        tvBasePrize.setText(String.format(Locale.US, "+$%d", engine.getBasePrizeForDisplay()));
        tvLaunchBonus.setText(String.format(Locale.US, "+$%d", engine.getLaunchBonus()));
        tvShiftBonus.setText(String.format(Locale.US, "+$%d", engine.getShiftBonus()));
        tvOvertakeBonus.setText(String.format(Locale.US, "+$%d", engine.getOvertakeBonus()));
        tvPlayerET.setText(String.format(Locale.US, "%.3f s", engine.getPlayerElapsedTime()));
        tvOpponentET.setText(String.format(Locale.US, "%.3f s", engine.getOpponentElapsedTime()));
        tvTopSpeed.setText(String.format(Locale.US, "%.1f km/h", engine.getPlayerTopSpeedKmh()));
        tvPerfectShifts.setText(String.valueOf(engine.getPlayerPerfectShifts()));

        // Save rewards and record
        PlayerData playerData = PlayerData.getInstance(getContext());
        playerData.addCash(prize);
        if (engine.isPlayerWon() && !engine.isFalseStart()) {
            playerData.updateBestET(engine.getRaceDistance(), (float) engine.getPlayerElapsedTime());
        }

        btnRematch.setOnClickListener(v -> {
            dismiss();
            if (callback != null) callback.onRematch();
        });

        btnGarage.setOnClickListener(v -> {
            dismiss();
            if (callback != null) callback.onGarage();
        });

        btnContinue.setOnClickListener(v -> {
            dismiss();
            if (callback != null) callback.onContinue();
        });
    }

    private void applyFontToViewTree(View root, Typeface font) {
        if (root == null || font == null) return;
        if (root instanceof TextView) {
            ((TextView) root).setTypeface(font);
        } else if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyFontToViewTree(group.getChildAt(i), font);
            }
        }
    }
}
