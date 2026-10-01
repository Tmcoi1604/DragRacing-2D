package com.dragracing.game;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.dragracing.game.audio.SoundManager;
import com.dragracing.game.data.Car;
import com.dragracing.game.data.PlayerData;
import com.dragracing.game.engine.RaceEngine;
import com.dragracing.game.ui.CareerActivity;
import com.dragracing.game.ui.CarPreviewView;
import com.dragracing.game.ui.GameActivity;
import com.dragracing.game.ui.GarageActivity;
import com.dragracing.game.render.TrackRenderer;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private PlayerData playerData;
    private SoundManager soundManager;

    private TextView tvMainCash, tvBestET, tvCurrentCarInfo;
    private Button btnSoundToggle;
    private FrameLayout mainCarPreviewContainer;
    private CarPreviewView previewView;
    private Spinner spinnerDistance;
    private Spinner spinnerDifficulty;
    private Spinner spinnerEnvironment;

    private final String[] distances = {"1/4 dặm", "1/2 dặm", "1 dặm", "2 dặm"};
    private final double[] distanceValues = {
            RaceEngine.DISTANCE_1_4,
            RaceEngine.DISTANCE_1_2,
            RaceEngine.DISTANCE_1,
            RaceEngine.DISTANCE_2
    };
        private final String[] difficulties = {"Dễ", "Thường", "Khó", "Rất Khó"};
            private final String[] environments = new String[]{
                "Thành phố đêm", "Khu công nghiệp", "Sa mạc",
                "Đại lộ cao tốc", "Đường đua ven biển",
                "Núi tuyết và cực quang", "Hầm tàu điện graffiti", "Khu rừng",
                "Đường đua lễ hội", "Đường đua chuyên dụng"
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hideSystemUI();

        playerData = PlayerData.getInstance(this);
        soundManager = SoundManager.getInstance(this);

        // Apply Pixel Font to entire activity
        Typeface pixelFont = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.pixel_font);
        
        initViews(pixelFont);
        
        applyFontToViewTree(findViewById(android.R.id.content), pixelFont);
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }

    private void initViews(Typeface pixelFont) {
        tvMainCash = findViewById(R.id.tvMainCash);
        tvBestET = findViewById(R.id.tvBestET);
        tvCurrentCarInfo = findViewById(R.id.tvCurrentCarInfo);
        btnSoundToggle = findViewById(R.id.btnSoundToggle);
        mainCarPreviewContainer = findViewById(R.id.mainCarPreviewContainer);
        spinnerDistance = findViewById(R.id.spinnerDistance);
        spinnerDifficulty = findViewById(R.id.spinnerDifficulty);
        spinnerEnvironment = findViewById(R.id.spinnerEnvironment);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, distances) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView && pixelFont != null) {
                    ((TextView) view).setTypeface(pixelFont);
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView && pixelFont != null) {
                    ((TextView) view).setTypeface(pixelFont);
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDistance.setAdapter(adapter);

        ArrayAdapter<String> difficultyAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, difficulties) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView && pixelFont != null) ((TextView) view).setTypeface(pixelFont);
                return view;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView && pixelFont != null) ((TextView) view).setTypeface(pixelFont);
                return view;
            }
        };
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDifficulty.setAdapter(difficultyAdapter);

        ArrayAdapter<String> environmentAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, environments) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView && pixelFont != null) ((TextView) view).setTypeface(pixelFont);
                return view;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView && pixelFont != null) ((TextView) view).setTypeface(pixelFont);
                return view;
            }
        };
        environmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEnvironment.setAdapter(environmentAdapter);

        previewView = new CarPreviewView(this, playerData.getCurrentCar());
        mainCarPreviewContainer.addView(previewView);

        findViewById(R.id.btnQuickRace).setOnClickListener(v -> {
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra("race_mode", "QUICK");
            intent.putExtra("difficulty", spinnerDifficulty.getSelectedItemPosition() + 1);
            intent.putExtra("distance", distanceValues[spinnerDistance.getSelectedItemPosition()]);
            intent.putExtra("environment_index", spinnerEnvironment.getSelectedItemPosition());
            startActivity(intent);
        });

        findViewById(R.id.btnCareer).setOnClickListener(v -> {
            startActivity(new Intent(this, CareerActivity.class));
        });

        findViewById(R.id.btnGarage).setOnClickListener(v -> {
            startActivity(new Intent(this, GarageActivity.class));
        });

        findViewById(R.id.btnTestRun).setOnClickListener(v -> {
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra("race_mode", "TEST");
            intent.putExtra("distance", distanceValues[spinnerDistance.getSelectedItemPosition()]);
            intent.putExtra("environment_index", spinnerEnvironment.getSelectedItemPosition());
            startActivity(intent);
        });

        findViewById(R.id.btnExit).setOnClickListener(v -> {
            finishAffinity();
        });

        btnSoundToggle.setOnClickListener(v -> {
            boolean current = soundManager.isSoundEnabled();
            soundManager.setSoundEnabled(!current);
            btnSoundToggle.setText(soundManager.isSoundEnabled() ? "ÂM THANH: BẬT" : "ÂM THANH: TẮT");
        });
    }

    private void updateUI() {
        Car car = playerData.getCurrentCar();
        tvMainCash.setText(String.format(Locale.US, "$%,d", playerData.getCash()));

        double selectedDistance = distanceValues[spinnerDistance.getSelectedItemPosition()];
        float bestET = playerData.getBestET(selectedDistance);
        String distLabel = distances[spinnerDistance.getSelectedItemPosition()];
        
        if (bestET > 0.001f) {
            tvBestET.setText(String.format(Locale.US, "Kỷ lục %s: %.3fs", distLabel, bestET));
        } else {
            tvBestET.setText(String.format(Locale.US, "Kỷ lục %s: --.---s", distLabel));
        }

        tvCurrentCarInfo.setText(String.format(Locale.US, "%s (%.0f HP)", car.getName(), car.getEffectiveHorsepower()));
        previewView.setCar(car);
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

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        updateUI();
    }
}
