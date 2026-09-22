package com.dragracing.game.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.dragracing.game.R;
import com.dragracing.game.audio.SoundManager;
import com.dragracing.game.engine.CarPhysics;
import com.dragracing.game.engine.RaceEngine;
import com.dragracing.game.render.CarRenderer;
import com.dragracing.game.render.TrackRenderer;
import java.util.Locale;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    public interface RaceFinishListener {
        void onRaceFinished(RaceEngine engine);
        void onPauseRequested();
    }

    private SurfaceHolder surfaceHolder;
    private Thread gameThread;
    private volatile boolean isRunning = false;

    private final RaceEngine raceEngine;
    private final RaceFinishListener finishListener;
    private final SoundManager soundManager;

    private final TrackRenderer trackRenderer;
    private final CarRenderer carRenderer;

    // UI Paints
    private final Paint textPaint = new Paint();
    private final Paint gaugePaint = new Paint();
    private final Paint buttonPaint = new Paint();
    private final Paint panelPaint = new Paint();
    private final Paint hudBgPaint = new Paint();

    // Control button bounds
    private final RectF gasPedalRect = new RectF();
    private final RectF shiftUpRect = new RectF();
    private final RectF shiftDownRect = new RectF();
    private final RectF nitroButtonRect = new RectF();
    private final RectF pauseButtonRect = new RectF();

    private boolean gasPedalPressed = false;
    private boolean finishReported = false;
    private boolean isPaused = false;

    private String shiftFeedbackText = "";
    private float lastWidth, lastHeight;

    public RaceEngine getRaceEngine() { return raceEngine; }
    public TrackRenderer getTrackRenderer() { return trackRenderer; }

    public GameView(Context context, RaceEngine raceEngine, RaceFinishListener finishListener) {
        super(context);
        this.raceEngine = raceEngine;
        this.finishListener = finishListener;
        this.soundManager = SoundManager.getInstance(context);
        this.carRenderer = new CarRenderer(context);
        this.trackRenderer = new TrackRenderer(context);

        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);

        setFocusable(true);
        
        // Load Pixel Font with high compatibility
        Typeface pixelTypeface = null;
        try {
            pixelTypeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.pixel_font);
        } catch (Exception e) {
            pixelTypeface = Typeface.MONOSPACE;
        }
        
        textPaint.setTypeface(pixelTypeface);
        textPaint.setAntiAlias(false); // Sharp edges for pixel look
        
        gaugePaint.setAntiAlias(false);
        buttonPaint.setAntiAlias(false);
        panelPaint.setAntiAlias(false);

        hudBgPaint.setColor(Color.argb(200, 15, 18, 24));
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isRunning = true;
        soundManager.startEngineAudio();
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        lastWidth = width;
        lastHeight = height;
        layoutControls(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isRunning = false;
        soundManager.stopEngineAudio();
        boolean retry = true;
        while (retry) {
            try {
                if (gameThread != null) {
                    gameThread.join(300);
                }
                retry = false;
            } catch (InterruptedException ignored) {}
        }
    }

    private void layoutControls(int width, int height) {
        float dashTop = height * 0.78f;
        float dashHeight = height - dashTop;
        float padding = 20.0f;

        // Center Tachometer
        float cx = width / 2.0f;
        float radius = Math.min(dashHeight * 0.62f, width * 0.17f);
        float cy = height - Math.max(12.0f, radius * 0.08f); // Keep the gauge inside the surface

        // Vertical Gas Pedal on the Right (Upper part of dash to avoid overlap)
        float gasWidth = 70.0f;
        float gasHeight = 160.0f;
        gasPedalRect.set(width - gasWidth - padding * 2, dashTop - gasHeight * 0.4f, width - padding * 2, dashTop + gasHeight * 0.6f);

        // Shift Paddles flanking the tachometer - Enlarged
        float paddleWidth = Math.min(140.0f, width * 0.10f);
        float paddleHeight = Math.min(180.0f, dashHeight * 0.72f);
        float paddleBottom = height - 16.0f;
        
        // Shift Down (Left)
        shiftDownRect.set(cx - radius - paddleWidth - 20, paddleBottom - paddleHeight,
            cx - radius - 10, paddleBottom);
        
        // Shift Up (Right)
        shiftUpRect.set(cx + radius + 10, paddleBottom - paddleHeight,
            cx + radius + paddleWidth + 20, paddleBottom);

        // Nitro button moved to the left side of the dashboard, matching the reference layout
        float nitroSize = Math.min(120.0f, width * 0.15f);
        float nitroCenterX = Math.max(padding + nitroSize * 0.7f, width * 0.12f);
        float nitroCenterY = height * 0.74f;
        nitroButtonRect.set(
            nitroCenterX - nitroSize * 0.5f,
            nitroCenterY - nitroSize * 0.5f,
            nitroCenterX + nitroSize * 0.5f,
            nitroCenterY + nitroSize * 0.5f
        );

        // Pause Button (Top Left)
        float pauseSize = 60.0f;
        pauseButtonRect.set(padding, padding, padding + pauseSize, padding + pauseSize);
    }

    public void setPaused(boolean paused) {
        this.isPaused = paused;
        if (isPaused) {
            soundManager.stopEngineAudio();
        } else {
            soundManager.startEngineAudio();
        }
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        final double nsPerSecond = 1000000000.0;

        while (isRunning) {
            long now = System.nanoTime();
            double dt = (now - lastTime) / nsPerSecond;
            lastTime = now;
            if (dt > 0.05) dt = 0.05;

            if (!isPaused) {
                update(dt);
            }
            render();

            long sleepMs = 16 - (long) ((System.nanoTime() - now) / 1000000);
            if (sleepMs > 0) {
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    private void update(double dt) {
        if (raceEngine.getState() == RaceEngine.RaceState.RACING) {
            if (raceEngine.isPlayerFinished()) {
                raceEngine.getPlayerCar().setThrottle(0.0);
            } else {
                raceEngine.getPlayerCar().setThrottle(1.0);
            }
        } else {
            raceEngine.getPlayerCar().setThrottle(gasPedalPressed ? 1.0 : 0.0);
        }
        
        raceEngine.update(dt);

        soundManager.updateEngineRpm(
                raceEngine.getPlayerCar().getRpm(),
                raceEngine.getPlayerCar().isNitroActive()
        );

        // Fade engine volume out smoothly once the player finishes the race
        if (raceEngine.isPlayerFinished()) {
            double progressPastFinish = raceEngine.getPlayerCar().getDistance() - raceEngine.getRaceDistance();
            // Sound fades to zero over a buffer of 6.0 meters past the finish line
            float vol = (float) Math.max(0.0, 1.0 - (progressPastFinish / 6.0));
            soundManager.setEngineVolume(vol);
            if (vol == 0f && raceEngine.getState() == RaceEngine.RaceState.FINISHED) {
                soundManager.stopEngineAudio();
            }
        } else {
            soundManager.setEngineVolume(1.0f);
        }

        if (raceEngine.getState() == RaceEngine.RaceState.FINISHED && !finishReported) {
            finishReported = true;
            if (raceEngine.isPlayerWon()) {
                soundManager.playWinSound();
            } else {
                soundManager.playLaunchSound();
            }
            if (finishListener != null) {
                post(() -> finishListener.onRaceFinished(raceEngine));
            }
        }
    }

    private void render() {
        if (!surfaceHolder.getSurface().isValid()) return;

        Canvas canvas = surfaceHolder.lockCanvas();
        if (canvas == null) return;

        try {
            int width = canvas.getWidth();
            int height = canvas.getHeight();

            // 1. Draw Drag Strip
            double playerDist = raceEngine.getPlayerCar().getDistance();
            double oppDist = raceEngine.getOpponentCar() != null ? raceEngine.getOpponentCar().getDistance() : playerDist;
            
            trackRenderer.render(canvas, width, height, playerDist, raceEngine);

            // 2. Draw Cars
            float trackAreaBottom = height * 0.78f;
            float horizonY = trackAreaBottom * 0.62f;
            float trackHeight = trackAreaBottom - horizonY;
            float laneHeight = trackHeight / 2.0f;
            float carScale = 1.15f;
            float carRenderHeight = 45.0f * carScale;
            float carRenderWidth = 185.0f * carScale;

            // Base position is centered around the start point
            float startLineX = width * 0.25f;
            float baseX = startLineX - carRenderWidth;

            // Calculate positions based on distance relative to player
            float playerScreenX = baseX;
            float oppScreenX = (float) ((oppDist - playerDist) * 35.0 + baseX);

            // Dynamic adjustment based on speed/distance gap to shift player rightward and opponent leftward
            double distanceGap = playerDist - oppDist;
            
            if (distanceGap > 0) {
                // Player is ahead: push player forward towards the right edge of screen
                // Max shift limits the player car to not exceed the right edge (width - carRenderWidth - padding)
                float maxPlayerShift = width * 0.55f; 
                float shiftFactor = (float) Math.min(maxPlayerShift, distanceGap * 25.0);
                
                playerScreenX += shiftFactor;
                oppScreenX += shiftFactor; // Opponent shifts back relative to player position
            }

            // Render opponent if it is still within the visible screen area
            float roadTop = horizonY + 10.0f;
            float laneCenterOffset = (laneHeight - carRenderHeight) * 0.5f;
            if (raceEngine.getOpponentCar() != null && oppScreenX + carRenderWidth > 0) {
                float oppScreenY = roadTop + laneCenterOffset;
                carRenderer.render(canvas, raceEngine.getOpponentCar(), oppScreenX, oppScreenY, carScale, true);
            }

            float playerScreenY = roadTop + laneHeight + laneCenterOffset;
            carRenderer.render(canvas, raceEngine.getPlayerCar(), playerScreenX, playerScreenY, carScale, false);

            // 3. Draw Dashboard
            drawDashboard(canvas, width, height, trackAreaBottom);

            // 4. Draw HUD Messages
            drawCenterMessages(canvas, width, trackAreaBottom);

            // 5. Draw Progress Bar
            drawTopProgressBar(canvas, width);

            // 6. Draw Pause Button
            drawPauseButton(canvas);

        } finally {
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawTopProgressBar(Canvas canvas, int width) {
        float barWidth = width * 0.5f;
        float barHeight = 6.0f;
        float barX = (width - barWidth) / 2.0f;
        float barY = 25.0f;

        hudBgPaint.setColor(Color.argb(180, 50, 50, 50));
        canvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, hudBgPaint);

        float playerProgress = (float) Math.min(1.0, raceEngine.getPlayerCar().getDistance() / raceEngine.getRaceDistance());
        buttonPaint.setColor(Color.parseColor("#00E5FF"));
        canvas.drawCircle(barX + playerProgress * barWidth, barY + barHeight / 2.0f, 6.0f, buttonPaint);

        if (raceEngine.getOpponentCar() != null) {
            float oppProgress = (float) Math.min(1.0, raceEngine.getOpponentCar().getDistance() / raceEngine.getRaceDistance());
            buttonPaint.setColor(Color.parseColor("#FF1744"));
            canvas.drawCircle(barX + oppProgress * barWidth, barY + barHeight / 2.0f, 4.0f, buttonPaint);
        }
    }

    private void drawDashboard(Canvas canvas, int width, int height, float dashTop) {
        // Cockpit background
        panelPaint.setShader(null);
        panelPaint.setColor(Color.parseColor("#1A1C1E"));
        
        Path cockpitPath = new Path();
        cockpitPath.moveTo(0, height);
        cockpitPath.lineTo(0, dashTop + 40);
        cockpitPath.quadTo(width/2f, dashTop - 20, width, dashTop + 40);
        cockpitPath.lineTo(width, height);
        cockpitPath.close();
        canvas.drawPath(cockpitPath, panelPaint);

        CarPhysics player = raceEngine.getPlayerCar();
        float cx = width * 0.62f;
        float gaugeRadius = Math.min((height - dashTop) * 0.52f, width * 0.22f);
        float cy = height - Math.max(18.0f, gaugeRadius * 0.55f);

        // 1. Central Speedometer
        drawModernSpeedometer(canvas, cx, cy, gaugeRadius, player);

        // 2. Info Boxes
        drawInfoBoxes(canvas, cx, cy, gaugeRadius, player);

        // 3. Paddle Shifters
        drawPaddleShifters(canvas, cx, cy, gaugeRadius);

        // 4. Nitro
        if (raceEngine.getState() == RaceEngine.RaceState.RACING && player.getCar().getNitroLevel() > 0) {
            // Show if not used yet OR currently active
            if (!player.isNitroUsed() || player.isNitroActive()) {
                drawNitroButton(canvas, player);
            }
        }

        // 5. Vertical Gas Pedal
        if (raceEngine.getState() != RaceEngine.RaceState.RACING && raceEngine.getState() != RaceEngine.RaceState.FINISHED) {
            drawVerticalGasPedal(canvas);
        }
    }

    private void drawModernSpeedometer(Canvas canvas, float cx, float cy, float radius, CarPhysics player) {
        gaugePaint.setStyle(Paint.Style.STROKE);
        gaugePaint.setStrokeWidth(radius * 0.15f);
        gaugePaint.setColor(Color.parseColor("#2C3E50"));
        
        RectF bounds = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);
        canvas.drawArc(bounds, 180, 180, false, gaugePaint);

        // --- NEW: RPM Zone Arc (Inner) ---
        float rpmRadius = radius * 0.75f;
        RectF rpmBounds = new RectF(cx - rpmRadius, cy - rpmRadius, cx + rpmRadius, cy + rpmRadius);
        float maxRpm = (float) player.getCar().getMaxRpm();
        float optMin = (float) player.getCar().getOptimalShiftMinRpm();
        float optMax = (float) player.getCar().getOptimalShiftMaxRpm();

        gaugePaint.setStrokeWidth(radius * 0.05f);
        // Background for RPM track
        gaugePaint.setColor(Color.argb(50, 255, 255, 255));
        canvas.drawArc(rpmBounds, 180, 180, false, gaugePaint);

        // Perfect Zone (Green)
        gaugePaint.setColor(Color.parseColor("#4CAF50"));
        canvas.drawArc(rpmBounds, 180 + (optMin / maxRpm) * 180, ((optMax - optMin) / maxRpm) * 180, false, gaugePaint);

        // Redline (Red)
        gaugePaint.setColor(Color.parseColor("#F44336"));
        canvas.drawArc(rpmBounds, 180 + (optMax / maxRpm) * 180, 180 - (optMax / maxRpm) * 180, false, gaugePaint);

        // RPM Indicator Dot on the inner arc
        float rpmAngle = 180 + ((float)player.getRpm() / maxRpm) * 180;
        double rpmRad = Math.toRadians(rpmAngle);
        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(Color.WHITE);
        canvas.drawCircle((float)(cx + Math.cos(rpmRad) * rpmRadius), (float)(cy + Math.sin(rpmRad) * rpmRadius), 6f, dotPaint);
        // ---------------------------------

        // Speed Ticks and Numbers
        float maxSpeed = 320f;
        gaugePaint.setStrokeWidth(4.0f);
        gaugePaint.setColor(Color.WHITE);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(14.0f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        for (int i = 0; i <= 320; i += 20) {
            float angle = 180 + (i / maxSpeed) * 180;
            double rad = Math.toRadians(angle);
            float innerR = radius - 15;
            float outerR = radius + 5;
            canvas.drawLine(
                (float)(cx + Math.cos(rad) * innerR), (float)(cy + Math.sin(rad) * innerR),
                (float)(cx + Math.cos(rad) * outerR), (float)(cy + Math.sin(rad) * outerR),
                gaugePaint
            );
            
            if (i % 40 == 0) {
                float textR = radius - 40;
                canvas.drawText(String.valueOf(i), 
                    (float)(cx + Math.cos(rad) * textR), (float)(cy + Math.sin(rad) * textR + 5), 
                    textPaint);
            }
        }

        // Needle (Speed)
        float currentSpeed = (float) player.getSpeedKmh();
        float needleAngle = 180 + Math.min(1.0f, currentSpeed / maxSpeed) * 180;
        double needleRad = Math.toRadians(needleAngle);
        gaugePaint.setStrokeWidth(8.0f);
        gaugePaint.setColor(Color.parseColor("#FFD600"));
        canvas.drawLine(cx, cy, (float)(cx + Math.cos(needleRad) * (radius - 10)), (float)(cy + Math.sin(needleRad) * (radius - 10)), gaugePaint);
        
        // Digital RPM Display (Center)
        hudBgPaint.setColor(Color.argb(150, 0, 0, 0));
        canvas.drawRect(cx - 40, cy - radius * 0.45f, cx + 40, cy - radius * 0.15f, hudBgPaint);
        textPaint.setColor(Color.parseColor("#00E5FF"));
        textPaint.setTextSize(24.0f);
        canvas.drawText(String.valueOf((int) player.getRpm()), cx, cy - radius * 0.28f, textPaint);
        textPaint.setTextSize(10.0f);
        textPaint.setColor(Color.GRAY);
        canvas.drawText("RPM", cx, cy - radius * 0.20f, textPaint);

        textPaint.setColor(Color.GRAY);
        textPaint.setTextSize(16.0f);
        canvas.drawText("KM/H", cx, cy - radius * 0.6f, textPaint);
    }

    private void drawInfoBoxes(Canvas canvas, float cx, float cy, float radius, CarPhysics player) {
        hudBgPaint.setColor(Color.argb(220, 30, 35, 40));

        float boxW = 96f;
        float boxH = 86f;
        float boxX = cx + radius + 24f;
        float boxY = cy - 62f;
        canvas.drawRoundRect(boxX, boxY, boxX + boxW, boxY + boxH, 18f, 18f, hudBgPaint);

        textPaint.setColor(Color.parseColor("#FFD600"));
        textPaint.setTextSize(40.0f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        String gear = !player.isHasLaunched() ? "N" : String.valueOf(player.getCurrentGear());
        canvas.drawText(gear, boxX + boxW / 2f, boxY + 44f, textPaint);
        textPaint.setTextSize(12.0f);
        textPaint.setColor(Color.GRAY);
        canvas.drawText("GEAR", boxX + boxW / 2f, boxY + 62f, textPaint);
    }

    private void drawPaddleShifters(Canvas canvas, float cx, float cy, float radius) {
        buttonPaint.setColor(Color.argb(180, 80, 85, 90));
        
        // Down Paddle (Left) - Bigger and adjusted
        canvas.drawRoundRect(shiftDownRect, 15, 15, buttonPaint);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(48.0f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("▼", shiftDownRect.centerX(), shiftDownRect.centerY(), textPaint);
        textPaint.setTextSize(16.0f);
        canvas.drawText("DOWN", shiftDownRect.centerX(), shiftDownRect.centerY() + 50, textPaint);

        // Up Paddle (Right) - Bigger and adjusted
        buttonPaint.setColor(Color.argb(180, 80, 85, 90));
        canvas.drawRoundRect(shiftUpRect, 15, 15, buttonPaint);
        textPaint.setColor(Color.parseColor("#00E676"));
        textPaint.setTextSize(48.0f);
        canvas.drawText("▲", shiftUpRect.centerX(), shiftUpRect.centerY(), textPaint);
        textPaint.setTextSize(16.0f);
        canvas.drawText("UP", shiftUpRect.centerX(), shiftUpRect.centerY() + 50, textPaint);
    }

    private void drawVerticalGasPedal(Canvas canvas) {
        buttonPaint.setColor(Color.parseColor("#424242"));
        canvas.drawRect(gasPedalRect, buttonPaint);
        
        // Bevel for pixel look
        buttonPaint.setColor(Color.parseColor("#757575"));
        canvas.drawRect(gasPedalRect.left, gasPedalRect.top, gasPedalRect.right, gasPedalRect.top + 4, buttonPaint);
        canvas.drawRect(gasPedalRect.left, gasPedalRect.top, gasPedalRect.left + 4, gasPedalRect.bottom, buttonPaint);
        
        buttonPaint.setColor(Color.parseColor("#212121"));
        canvas.drawRect(gasPedalRect.right - 4, gasPedalRect.top, gasPedalRect.right, gasPedalRect.bottom, buttonPaint);
        canvas.drawRect(gasPedalRect.left, gasPedalRect.bottom - 4, gasPedalRect.right, gasPedalRect.bottom, buttonPaint);
        
        Paint dotPaint = new Paint();
        dotPaint.setColor(Color.BLACK);
        for (int i = 0; i < 4; i++) {
            canvas.drawRect(gasPedalRect.centerX() - 15, gasPedalRect.top + 30 + i * 35, gasPedalRect.centerX() - 3, gasPedalRect.top + 30 + i * 35 + 8, dotPaint);
            canvas.drawRect(gasPedalRect.centerX() + 3, gasPedalRect.top + 30 + i * 35, gasPedalRect.centerX() + 15, gasPedalRect.top + 30 + i * 35 + 8, dotPaint);
        }
    }

    private void drawNitroButton(Canvas canvas, CarPhysics player) {
        buttonPaint.setColor(player.isNitroActive() ? Color.CYAN : Color.parseColor("#0288D1"));
        canvas.drawRect(nitroButtonRect, buttonPaint);
        
        // Bevel
        buttonPaint.setColor(player.isNitroActive() ? Color.WHITE : Color.parseColor("#03A9F4"));
        canvas.drawRect(nitroButtonRect.left, nitroButtonRect.top, nitroButtonRect.right, nitroButtonRect.top + 4, buttonPaint);
        canvas.drawRect(nitroButtonRect.left, nitroButtonRect.top, nitroButtonRect.left + 4, nitroButtonRect.bottom, buttonPaint);
        
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32.0f);
        canvas.drawText("NOS", nitroButtonRect.centerX(), nitroButtonRect.centerY() + 10, textPaint);
    }

    private void drawPauseButton(Canvas canvas) {
        buttonPaint.setColor(Color.argb(200, 40, 40, 40));
        canvas.drawRect(pauseButtonRect, buttonPaint);
        
        // Bevel
        buttonPaint.setColor(Color.argb(200, 80, 80, 80));
        canvas.drawRect(pauseButtonRect.left, pauseButtonRect.top, pauseButtonRect.right, pauseButtonRect.top + 4, buttonPaint);
        canvas.drawRect(pauseButtonRect.left, pauseButtonRect.top, pauseButtonRect.left + 4, pauseButtonRect.bottom, buttonPaint);
        
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32.0f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("II", pauseButtonRect.centerX(), pauseButtonRect.centerY() + 12, textPaint);
    }

    private void drawCenterMessages(Canvas canvas, int width, float trackAreaBottom) {
        textPaint.setTextAlign(Paint.Align.CENTER);
        if (raceEngine.getState() == RaceEngine.RaceState.STAGING) {
            textPaint.setColor(Color.YELLOW);
            textPaint.setTextSize(36.0f);
            canvas.drawText("GIỮ GA!", width / 2f, trackAreaBottom - 20, textPaint);
        }

        CarPhysics player = raceEngine.getPlayerCar();
        if (player.isLaunchFeedback()) {
            CarPhysics.LaunchResult launchRes = player.getLastLaunchResult();
            if (launchRes != CarPhysics.LaunchResult.NONE) {
                textPaint.setTextSize(36.0f);
                if (launchRes == CarPhysics.LaunchResult.PERFECT) {
                    textPaint.setColor(Color.GREEN);
                    canvas.drawText("PERFECT LAUNCH", width / 2f, trackAreaBottom - 60, textPaint);
                } else if (launchRes == CarPhysics.LaunchResult.BAD) {
                    textPaint.setColor(Color.RED);
                    canvas.drawText("BAD LAUNCH", width / 2f, trackAreaBottom - 60, textPaint);
                } else {
                    textPaint.setColor(Color.YELLOW);
                    canvas.drawText("GOOD LAUNCH", width / 2f, trackAreaBottom - 60, textPaint);
                }
            }
        } else {
            CarPhysics.ShiftResult shiftRes = player.getLastShiftResult();
            if (shiftRes != CarPhysics.ShiftResult.NONE) {
                drawShiftIndicator(canvas, width / 2f, trackAreaBottom - 90f, shiftRes);
            }
        }
    }

    private void drawShiftIndicator(Canvas canvas, float centerX, float centerY, CarPhysics.ShiftResult shiftRes) {
        float lightSize = 18f;
        float spacing = 24f;
        float startX = centerX - (lightSize * 2.5f + spacing * 2f);

        int activeLights = 0;
        int primaryColor = Color.parseColor("#4CAF50");
        if (shiftRes == CarPhysics.ShiftResult.OVER_REV) {
            primaryColor = Color.parseColor("#F44336");
            activeLights = 1;
        } else if (shiftRes == CarPhysics.ShiftResult.GOOD) {
            primaryColor = Color.parseColor("#FFC107");
            activeLights = 2;
        } else if (shiftRes == CarPhysics.ShiftResult.PERFECT) {
            primaryColor = Color.parseColor("#4CAF50");
            activeLights = 3;
        }

        Paint lightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        for (int i = 0; i < 3; i++) {
            float x = startX + i * (lightSize + spacing);
            float y = centerY;
            lightPaint.setColor(i < activeLights ? primaryColor : Color.argb(120, 255, 255, 255));
            lightPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x, y, lightSize * 0.8f, lightPaint);

            lightPaint.setColor(i < activeLights ? Color.argb(180, 255, 255, 255) : Color.argb(60, 255, 255, 255));
            lightPaint.setStyle(Paint.Style.STROKE);
            lightPaint.setStrokeWidth(2f);
            canvas.drawCircle(x, y, lightSize * 0.95f, lightPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (pauseButtonRect.contains(x, y)) {
                    if (finishListener != null) {
                        post(() -> finishListener.onPauseRequested());
                    }
                } else if (gasPedalRect.contains(x, y)) {
                    if (raceEngine.getState() == RaceEngine.RaceState.RACING) {
                        // Nitro button replaces gas pedal
                        if (raceEngine.playerNitro()) {
                            soundManager.vibrate(100);
                        }
                    } else {
                        gasPedalPressed = true;
                        if (raceEngine.getState() == RaceEngine.RaceState.STAGING) {
                            raceEngine.startCountdown();
                        }
                    }
                } else if (shiftUpRect.contains(x, y)) {
                    if (raceEngine.getState() == RaceEngine.RaceState.COUNTDOWN) {
                        raceEngine.playerLaunch();
                    }
                    if (raceEngine.playerShiftUp() == CarPhysics.ShiftResult.PERFECT) {
                        soundManager.playPerfectShiftSound();
                    } else {
                        soundManager.playShiftSound();
                    }
                } else if (shiftDownRect.contains(x, y)) {
                    raceEngine.playerShiftDown();
                    soundManager.playShiftSound();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                // Only reset gas pedal if we were in a state that uses it
                if (gasPedalRect.contains(x, y)) {
                    gasPedalPressed = false;
                }
                break;
        }
        return true;
    }
}
