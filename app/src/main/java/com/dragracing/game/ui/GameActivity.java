package com.dragracing.game.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.dragracing.game.R;
import com.dragracing.game.data.Car;
import com.dragracing.game.data.PlayerData;
import com.dragracing.game.engine.RaceEngine;
import com.dragracing.game.render.TrackRenderer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameActivity extends AppCompatActivity {
    private static final int[] OPPONENT_COLORS = {
        Color.parseColor("#F44336"),
        Color.parseColor("#2196F3"),
        Color.parseColor("#4CAF50"),
        Color.parseColor("#FF9800"),
        Color.parseColor("#9C27B0"),
        Color.parseColor("#00BCD4"),
        Color.parseColor("#FFEB3B"),
        Color.parseColor("#E91E63"),
        Color.parseColor("#FFFFFF"),
        Color.parseColor("#212121")
    };

    private FrameLayout gameContainer;
    private GameView gameView;
    private RaceEngine raceEngine;
    private String raceMode;
    private int difficulty;
    private double raceDistance;
    private boolean isBossRace;
    private int environmentIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Hide navigation bar and status bar for immersive full screen
        hideSystemUI();

        gameContainer = findViewById(R.id.gameContainer);
        raceMode = getIntent().getStringExtra("race_mode");
        if (raceMode == null) raceMode = "QUICK";
        difficulty = getIntent().getIntExtra("difficulty", 1);
        isBossRace = getIntent().getBooleanExtra("is_boss", false);
        environmentIndex = getIntent().getIntExtra("environment_index", -1);
        
        if ("CAREER".equals(raceMode)) {
            // Random distance for career races: 1/4, 1/2 or 1 mile
            double[] distances = {RaceEngine.DISTANCE_1_4, RaceEngine.DISTANCE_1_2, RaceEngine.DISTANCE_1};
            raceDistance = distances[new Random().nextInt(distances.length)];
        } else {
            raceDistance = getIntent().getDoubleExtra("distance", RaceEngine.DISTANCE_1_4);
        }

        startRace();
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

    private void startRace() {
        PlayerData playerData = PlayerData.getInstance(this);
        Car playerCar = playerData.getCurrentCar();
        Car opponentCar = null;

        if (!"TEST".equals(raceMode)) {
            opponentCar = generateOpponentCar(playerCar, raceMode, difficulty);
        }

        raceEngine = new RaceEngine(playerCar, opponentCar, raceMode, difficulty, raceDistance);

        if (gameView != null) {
            gameContainer.removeView(gameView);
        }

        gameView = new GameView(this, raceEngine, new GameView.RaceFinishListener() {
            @Override
            public void onRaceFinished(RaceEngine engine) {
                runOnUiThread(() -> showResultDialog(engine));
            }

            @Override
            public void onPauseRequested() {
                runOnUiThread(() -> showPauseDialog());
            }
        });

        TrackRenderer.EnvironmentType[] environments = TrackRenderer.EnvironmentType.values();
        TrackRenderer.EnvironmentType envType;
        if (environmentIndex >= 0 && environmentIndex < environments.length) {
            envType = environments[environmentIndex];
        } else if ("CAREER".equals(raceMode)) {
            int careerStep = getIntent().getIntExtra("career_step", 0);
            envType = TrackRenderer.EnvironmentType.careerEnvironment(difficulty, careerStep, isBossRace);
        } else {
            envType = environments[new Random().nextInt(environments.length)];
        }
        gameView.getTrackRenderer().setEnvironmentType(envType);

        gameContainer.addView(gameView);
    }

    private Car generateOpponentCar(Car playerCar, String mode, int diff) {
        List<Car> allCars = PlayerData.getInstance(this).getAllCars();
        Car performanceBase = null;
        Random random = new Random();

        if ("CAREER".equals(mode)) {
            if (isBossRace) {
                // Boss uses a specific car for the stage
                int index = Math.min(allCars.size() - 1, Math.max(0, diff - 1));
                performanceBase = allCars.get(index);
            } else {
                int careerStep = getIntent().getIntExtra("career_step", 0);
                int index = Math.max(0, diff - 2 + (careerStep % 2));
                index = Math.min(allCars.size() - 1, index);
                performanceBase = allCars.get(index);
            }
        }
        Car base = chooseRandomOpponentModel(allCars, playerCar, random);
        if (performanceBase == null && !"QUICK".equals(mode)) {
            performanceBase = base;
        }

        // Clone car specs for AI opponent
        double hpMult = "QUICK".equals(mode) ? getQuickRaceOpponentMultiplier(diff) : (0.85 + diff * 0.08);
        if (isBossRace) hpMult += 0.15; // Bosses are harder

        double horsepower = "QUICK".equals(mode)
            ? playerCar.getEffectiveHorsepower() * hpMult
            : performanceBase.getBaseHorsepower() * hpMult;
        double weight = "QUICK".equals(mode)
            ? playerCar.getEffectiveWeight() / hpMult
            : performanceBase.getBaseWeight();
        double grip = "QUICK".equals(mode)
            ? playerCar.getEffectiveGrip() * (0.96 + (hpMult - 1.0) * 0.15)
            : performanceBase.getBaseGrip();
        double shiftTime = "QUICK".equals(mode)
            ? Math.max(0.08, playerCar.getShiftTimeSeconds() / (0.98 + (hpMult - 1.0) * 0.35))
            : performanceBase.getBaseShiftTime();

        Car opp = new Car(
                "ai_" + base.getId(),
                (isBossRace ? "BOSS " : "Rival ") + base.getName(),
                base.getCarClass(),
                base.getPrice(),
                chooseRandomOpponentColor(playerCar, random),
                horsepower,
                weight,
                grip,
                shiftTime,
                performanceBase != null ? performanceBase.getMaxRpm() : base.getMaxRpm(),
                performanceBase != null ? performanceBase.getIdleRpm() : base.getIdleRpm(),
                performanceBase != null ? performanceBase.getOptimalShiftMinRpm() : base.getOptimalShiftMinRpm(),
                performanceBase != null ? performanceBase.getOptimalShiftMaxRpm() : base.getOptimalShiftMaxRpm(),
                performanceBase != null ? performanceBase.getGearRatios() : base.getGearRatios(),
                performanceBase != null ? performanceBase.getFinalDrive() : base.getFinalDrive(),
                base.getBodyType()
        );
        opp.setImageResourceName(base.getImageResourceName());

        // Equip AI with upgrades matching difficulty
        int level = "QUICK".equals(mode) ? 0 : Math.min(5, diff - 1);
        if (isBossRace) level = Math.min(5, diff);

        opp.setEngineLevel(level);
        opp.setTiresLevel(level);
        opp.setNitroLevel(diff >= 2 ? level : 0);

        return opp;
    }

    private Car chooseRandomOpponentModel(List<Car> cars, Car playerCar, Random random) {
        List<Car> alternatives = new ArrayList<>();
        for (Car car : cars) {
            if (!car.getId().equals(playerCar.getId())) {
                alternatives.add(car);
            }
        }
        List<Car> candidates = alternatives.isEmpty() ? cars : alternatives;
        return candidates.get(random.nextInt(candidates.size()));
    }

    private int chooseRandomOpponentColor(Car playerCar, Random random) {
        int color;
        do {
            color = OPPONENT_COLORS[random.nextInt(OPPONENT_COLORS.length)];
        } while (color == playerCar.getColor());
        return color;
    }

    private double getQuickRaceOpponentMultiplier(int diff) {
        switch (diff) {
            case 1: return 0.78;
            case 2: return 1.00;
            case 3: return 1.25;
            case 4: return 1.60;
            default: return 1.00;
        }
    }

    private void showResultDialog(RaceEngine engine) {
        ResultDialog dialog = new ResultDialog(this, engine, new ResultDialog.DialogActionCallback() {
            @Override
            public void onRematch() {
                startRace();
            }

            @Override
            public void onGarage() {
                Intent intent = new Intent(GameActivity.this, GarageActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onContinue() {
                if ("CAREER".equals(raceMode) && engine.isPlayerWon() && !engine.isFalseStart()) {
                    PlayerData.getInstance(GameActivity.this).advanceCareer();
                }
                finish();
            }
        });
        dialog.show();
    }

    private void showPauseDialog() {
        gameView.setPaused(true);
        PauseDialog dialog = new PauseDialog(this, new PauseDialog.PauseActionCallback() {
            @Override
            public void onContinue() {
                gameView.setPaused(false);
            }

            @Override
            public void onSetting() {
                // In a real app, this would open settings
            }

            @Override
            public void onQuit() {
                finish();
            }
        });
        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
    }
}
