package com.dragracing.game.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Base64;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.core.content.res.ResourcesCompat;

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
    private final Rect nitroSourceRect = new Rect();
    private final RectF shiftUpRect = new RectF();
    private final RectF shiftDownRect = new RectF();
    private final RectF nitroButtonRect = new RectF();
    private final RectF pauseButtonRect = new RectF();
    private final RectF speedometerRect = new RectF();
    private final RectF speedDisplayRect = new RectF();
    private final RectF gearDisplayRect = new RectF();
    private final RectF clockwiseRect = new RectF();

    private boolean gasPedalPressed = false;
    private boolean finishReported = false;
    private boolean isPaused = false;

    private volatile String shiftFeedbackText = "";
    private volatile long feedbackUntil;
    private boolean launchFeedbackShown;
    private float lastWidth, lastHeight;
    private Bitmap shiftUpBitmap;
    private Bitmap shiftDownBitmap;
    private Bitmap nitrousBitmap;
    private Bitmap gasBitmap;
    private Bitmap[] shiftUpAnimation;
    private Bitmap[] overrevAnimation;
    private Bitmap[] shiftingAnimation;
    private long animationStartedAt;
    private Bitmap[] activeAnimation;

    public RaceEngine getRaceEngine() { return raceEngine; }
    public TrackRenderer getTrackRenderer() { return trackRenderer; }

    private void loadHudAssets() {
        shiftUpBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.shift_up);
        shiftDownBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.shift_down);
        nitrousBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.active_nitrous_button);
        if (nitrousBitmap != null) {
            int cropSize = Math.round(Math.min(nitrousBitmap.getWidth(), nitrousBitmap.getHeight()) * 0.80f);
            int cropLeft = (nitrousBitmap.getWidth() - cropSize) / 2;
            int cropTop = (nitrousBitmap.getHeight() - cropSize) / 2;
            nitroSourceRect.set(cropLeft, cropTop, cropLeft + cropSize, cropTop + cropSize);
        }
        gasBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.gas_button);
        shiftUpAnimation = loadPiskelAnimation(R.raw.shift_up_animation);
        overrevAnimation = loadPiskelAnimation(R.raw.overev_animation);
        shiftingAnimation = loadPiskelAnimation(R.raw.shifting_animation);
    }

    private Bitmap[] loadPiskelAnimation(int resourceId) {
        try (InputStream stream = getResources().openRawResource(resourceId)) {
            String json = new String(readAll(stream), StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(json);
            JSONObject piskel = root.getJSONObject("piskel");
            JSONArray layers = piskel.getJSONArray("layers");
            if (layers.length() == 0) return new Bitmap[0];

            String layer0Str = layers.getString(0);
            JSONObject layer0 = new JSONObject(layer0Str);
            JSONArray chunks = layer0.getJSONArray("chunks");
            if (chunks.length() == 0) return new Bitmap[0];

            JSONObject chunk = chunks.getJSONObject(0);
            String encoded = chunk.getString("base64PNG");
            int comma = encoded.indexOf(',');
            if (comma >= 0) encoded = encoded.substring(comma + 1);
            byte[] png = Base64.decode(encoded, Base64.DEFAULT);
            Bitmap sheet = BitmapFactory.decodeByteArray(png, 0, png.length);
            if (sheet == null) return new Bitmap[0];

            int frameWidth = piskel.getInt("width");
            int frameHeight = piskel.getInt("height");
            JSONArray layout = chunk.getJSONArray("layout");
            int frameCount = layout.length();
            Bitmap[] frames = new Bitmap[frameCount];

            int cols = sheet.getWidth() / frameWidth;
            if (cols < 1) cols = 1;

            for (int i = 0; i < frameCount; i++) {
                int x = (i % cols) * frameWidth;
                int y = (i / cols) * frameHeight;
                if (x + frameWidth <= sheet.getWidth() && y + frameHeight <= sheet.getHeight()) {
                    frames[i] = Bitmap.createBitmap(sheet, x, y, frameWidth, frameHeight);
                } else {
                    frames[i] = sheet;
                }
            }
            return frames;
        } catch (Exception error) {
            Log.e("GameView", "Unable to load Piskel animation resource " + resourceId, error);
            return new Bitmap[0];
        }
    }

    private byte[] readAll(InputStream stream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = stream.read(buffer)) != -1) output.write(buffer, 0, count);
        return output.toByteArray();
    }

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
            pixelTypeface = ResourcesCompat.getFont(context, R.font.pixel_font);
        } catch (Exception e) {
            pixelTypeface = Typeface.MONOSPACE;
        }
        
        textPaint.setTypeface(pixelTypeface);
        textPaint.setAntiAlias(false); // Sharp edges for pixel look
        
        gaugePaint.setAntiAlias(false);
        buttonPaint.setAntiAlias(false);
        panelPaint.setAntiAlias(false);

        hudBgPaint.setColor(Color.argb(200, 15, 18, 24));
        loadHudAssets();
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

        float gaugeSize = Math.min(width * 0.30f, 460.0f);
        gaugeSize = Math.min(gaugeSize, Math.max(190.0f, height * 0.60f));
        float gaugeCenterX = width * 0.5f;
        float gaugeCenterY = height - 24.0f - gaugeSize * 0.5f;
        speedometerRect.set(gaugeCenterX - gaugeSize * 0.5f, gaugeCenterY - gaugeSize * 0.5f,
            gaugeCenterX + gaugeSize * 0.5f, gaugeCenterY + gaugeSize * 0.5f);

        float boxWidth = Math.min(108.0f, Math.max(64.0f, width * 0.08f));
        float boxHeight = Math.min(76.0f, Math.max(54.0f, dashHeight * 0.68f));
        float lightColumnWidth = Math.min(38.0f, Math.max(24.0f, width * 0.035f));
        float paddleWidth = Math.min(76.0f, Math.max(48.0f, width * 0.06f));
        float paddleHeight = Math.min(104.0f, Math.max(72.0f, dashHeight * 0.88f));
        float gap = Math.min(12.0f, Math.max(6.0f, width * 0.008f));
        float displayCenterY = Math.min(height - 24.0f - boxHeight * 0.5f,
            gaugeCenterY + gaugeSize * 0.36f);
        speedDisplayRect.set(speedometerRect.left - gap - boxWidth, displayCenterY - boxHeight * 0.5f,
            speedometerRect.left - gap, displayCenterY + boxHeight * 0.5f);
        float lightsLeft = speedometerRect.right + gap;
        gearDisplayRect.set(lightsLeft + lightColumnWidth + gap, displayCenterY - boxHeight * 0.5f,
            lightsLeft + lightColumnWidth + gap + boxWidth, displayCenterY + boxHeight * 0.5f);

        float paddleCenterY = displayCenterY;
        shiftDownRect.set(speedDisplayRect.left - gap - paddleWidth, paddleCenterY - paddleHeight * 0.5f,
            speedDisplayRect.left - gap, paddleCenterY + paddleHeight * 0.5f);
        shiftUpRect.set(gearDisplayRect.right + gap, paddleCenterY - paddleHeight * 0.5f,
            gearDisplayRect.right + gap + paddleWidth, paddleCenterY + paddleHeight * 0.5f);

        // Gas is on the right and intentionally larger for touch accessibility.
        float gasSize = Math.min(132.0f, Math.max(96.0f, dashHeight * 0.72f));
        gasPedalRect.set(width - padding - gasSize, height - gasSize - 24.0f,
                width - padding, height - 24.0f);

        // Nitro button moved to the left side of the dashboard, matching the reference layout.
        float nitroSize = Math.min(120.0f, Math.max(90.0f, dashHeight * 0.90f));
        float nitroCenterX = padding + nitroSize * 0.5f;
        float nitroCenterY = height - nitroSize * 0.5f - 24.0f;
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

        CarPhysics player = raceEngine.getPlayerCar();
        if (player.isHasLaunched() && !launchFeedbackShown) {
            launchFeedbackShown = true;
            switch (player.getLastLaunchResult()) {
                case PERFECT:
                    showFeedback("PERFECT LAUNCH");
                    break;
                case BAD:
                    showFeedback("BAD LAUNCH");
                    break;
                case GOOD:
                    showFeedback("GOOD LAUNCH");
                    break;
                default:
                    break;
            }
        }

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
            float roadTop = trackRenderer.getRoadTop(height);
            float laneHeight = (trackAreaBottom - roadTop) / 2.0f;
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
            float laneCenterOffset = (laneHeight - carRenderHeight) * 0.5f;
            if (raceEngine.getOpponentCar() != null && oppScreenX + carRenderWidth > 0) {
                float oppScreenY = roadTop + laneCenterOffset;
                carRenderer.render(canvas, raceEngine.getOpponentCar(), oppScreenX, oppScreenY, carScale, true);
            }

            float playerScreenY = roadTop + laneHeight + laneCenterOffset;
            carRenderer.render(canvas, raceEngine.getPlayerCar(), playerScreenX, playerScreenY, carScale, false);

            // 3. Draw Dashboard
            drawDashboard(canvas, width, height, trackAreaBottom);

            // 4. Draw Progress Bar
            drawTopProgressBar(canvas, width);

            // 5. Draw Pause Button
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
        drawSpeedometer(canvas, player);
        drawPaddleShifters(canvas);

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

    private void drawSpeedometer(Canvas canvas, CarPhysics player) {
        float cx = speedometerRect.centerX();
        float cy = speedometerRect.centerY();
        float radius = speedometerRect.width() * 0.46f;
        float maxRpm = player.getCar().getMaxRpm();
        float rpm = (float) Math.max(0.0, Math.min(maxRpm, player.getRpm()));
        float optimalMin = player.getCar().getOptimalShiftMinRpm();
        float optimalMax = player.getCar().getOptimalShiftMaxRpm();

        gaugePaint.setStyle(Paint.Style.FILL);
        gaugePaint.setColor(Color.argb(235, 12, 19, 25));
        canvas.drawCircle(cx, cy, radius, gaugePaint);
        gaugePaint.setStyle(Paint.Style.STROKE);
        gaugePaint.setStrokeCap(Paint.Cap.ROUND);
        gaugePaint.setStrokeWidth(Math.max(3.0f, radius * 0.075f));
        gaugePaint.setColor(Color.argb(255, 72, 88, 98));
        RectF arcBounds = new RectF(cx - radius * 0.82f, cy - radius * 0.82f,
                cx + radius * 0.82f, cy + radius * 0.82f);
        canvas.drawArc(arcBounds, 150.0f, 240.0f, false, gaugePaint);
        float optimalStart = 150.0f + (optimalMin / maxRpm) * 240.0f;
        float optimalSweep = ((optimalMax - optimalMin) / maxRpm) * 240.0f;
        gaugePaint.setColor(Color.parseColor("#A6E7FD"));
        canvas.drawArc(arcBounds, optimalStart, optimalSweep, false, gaugePaint);
        gaugePaint.setColor(Color.parseColor("#FF5252"));
        canvas.drawArc(arcBounds, optimalStart + optimalSweep,
                Math.max(0.0f, 240.0f - (optimalStart + optimalSweep - 150.0f)), false, gaugePaint);

        for (int tick = 0; tick <= 10; tick++) {
            float fraction = tick / 10.0f;
            double angle = Math.toRadians(150.0f + fraction * 240.0f);
            float inner = radius * (tick % 2 == 0 ? 0.66f : 0.70f);
            float outer = radius * 0.78f;
            gaugePaint.setStrokeWidth(tick % 2 == 0 ? 2.5f : 1.5f);
            gaugePaint.setColor(Color.WHITE);
            canvas.drawLine(cx + (float) Math.cos(angle) * inner, cy + (float) Math.sin(angle) * inner,
                    cx + (float) Math.cos(angle) * outer, cy + (float) Math.sin(angle) * outer, gaugePaint);
            if (tick % 2 == 0) {
                textPaint.setTextAlign(Paint.Align.CENTER);
                textPaint.setTextSize(Math.max(10.0f, radius * 0.12f));
                textPaint.setColor(Color.WHITE);
                float labelRadius = radius * 0.56f;
                canvas.drawText(String.valueOf(Math.round((maxRpm / 1000.0f) * fraction)),
                        cx + (float) Math.cos(angle) * labelRadius,
                        cy + (float) Math.sin(angle) * labelRadius + textPaint.getTextSize() * 0.35f, textPaint);
            }
        }

        double needleAngle = Math.toRadians(150.0f + (rpm / maxRpm) * 240.0f);
        gaugePaint.setStrokeWidth(Math.max(3.0f, radius * 0.035f));
        gaugePaint.setColor(Color.parseColor("#FFD166"));
        canvas.drawLine(cx, cy, cx + (float) Math.cos(needleAngle) * radius * 0.68f,
                cy + (float) Math.sin(needleAngle) * radius * 0.68f, gaugePaint);
        gaugePaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, radius * 0.065f, gaugePaint);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(Math.max(18.0f, radius * 0.25f));
        canvas.drawText(String.format(Locale.US, "%,d", (int) rpm), cx, cy + radius * 0.34f, textPaint);
        textPaint.setColor(Color.parseColor("#A6E7FD"));
        textPaint.setTextSize(Math.max(10.0f, radius * 0.11f));
        canvas.drawText("RPM", cx, cy + radius * 0.51f, textPaint);

        drawInfoBox(canvas, speedDisplayRect, String.valueOf((int) player.getSpeedKmh()), "KPH", Color.WHITE);
        drawInfoBox(canvas, gearDisplayRect,
                player.isHasLaunched() ? String.valueOf(player.getCurrentGear()) : "N", "GEAR", Color.parseColor("#FFD166"));
        drawShiftLights(canvas, player, speedometerRect.right + Math.min(12.0f, Math.max(6.0f, lastWidth * 0.008f)),
                gearDisplayRect.centerY());
        drawLaunchFeedback(canvas, speedometerRect.centerX(), speedometerRect.top - 10.0f);
    }

    private void drawInfoBox(Canvas canvas, RectF bounds, String value, String label, int valueColor) {
        hudBgPaint.setStyle(Paint.Style.FILL);
        hudBgPaint.setColor(Color.argb(235, 18, 27, 34));
        canvas.drawRoundRect(bounds, 8.0f, 8.0f, hudBgPaint);
        gaugePaint.setStyle(Paint.Style.STROKE);
        gaugePaint.setStrokeWidth(2.0f);
        gaugePaint.setColor(Color.argb(210, 112, 139, 151));
        canvas.drawRoundRect(bounds, 8.0f, 8.0f, gaugePaint);
        gaugePaint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(valueColor);
        textPaint.setTextSize(bounds.height() * 0.46f);
        canvas.drawText(value, bounds.centerX(), bounds.top + bounds.height() * 0.58f, textPaint);
        textPaint.setColor(Color.LTGRAY);
        textPaint.setTextSize(bounds.height() * 0.17f);
        canvas.drawText(label, bounds.centerX(), bounds.bottom - bounds.height() * 0.12f, textPaint);
    }

    private void drawShiftLights(Canvas canvas, CarPhysics player, float left, float centerY) {
        float rpm = (float) player.getRpm();
        float optimalMin = player.getCar().getOptimalShiftMinRpm();
        float optimalMax = player.getCar().getOptimalShiftMaxRpm();
        float lightW = Math.min(30.0f, Math.max(18.0f, speedometerRect.width() * 0.065f));
        float lightH = Math.min(20.0f, Math.max(12.0f, speedometerRect.width() * 0.045f));
        float gap = lightH * 0.42f;
        float totalHeight = lightH * 4.0f + gap * 3.0f;
        float[] thresholds = {optimalMin * 0.58f, optimalMin * 0.73f, optimalMin * 0.88f};
        int activeCount = 0;
        for (float threshold : thresholds) {
            if (rpm >= threshold) activeCount++;
        }
        int sequenceStep = activeCount == 0 ? -1
                : (int) ((System.currentTimeMillis() / 160L) % (activeCount + 1));

        for (int index = 0; index < 3; index++) {
            boolean active = rpm >= thresholds[index]
                    && (!gasPedalPressed || sequenceStep == index);
            float top = centerY - totalHeight * 0.5f + (2 - index) * (lightH + gap);
            drawIndicator(canvas, left + (30.0f - lightW) * 0.5f, top, lightW, lightH,
                    active ? Color.parseColor("#A6E7FD") : Color.argb(90, 115, 133, 143));
        }

        boolean inPerfectRange = rpm >= optimalMin && rpm <= optimalMax;
        boolean overRev = rpm > optimalMax;
        int topColor = inPerfectRange ? Color.parseColor("#5CFF83")
                : overRev ? Color.parseColor("#FF3B45") : Color.argb(75, 115, 133, 143);
        float topSize = lightH * 1.55f;
        float topY = centerY - totalHeight * 0.5f;
        drawIndicator(canvas, left + (30.0f - topSize) * 0.5f, topY - gap * 0.35f,
                topSize, topSize, topColor);
    }

    private void drawIndicator(Canvas canvas, float left, float top, float width, float height, int color) {
        gaugePaint.setStyle(Paint.Style.FILL);
        gaugePaint.setColor(color);
        canvas.drawRoundRect(left, top, left + width, top + height, height * 0.5f, height * 0.5f, gaugePaint);
    }

    private void drawLaunchFeedback(Canvas canvas, float centerX, float baseline) {
        if (shiftFeedbackText.isEmpty() || System.currentTimeMillis() > feedbackUntil) return;
        int color = shiftFeedbackText.contains("PERFECT") ? Color.parseColor("#A6E7FD")
                : shiftFeedbackText.contains("BAD") || shiftFeedbackText.contains("OVEREV")
                ? Color.parseColor("#FF5252") : Color.WHITE;
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(Math.max(16.0f, speedometerRect.width() * 0.065f));
        textPaint.setColor(color);
        canvas.drawText(shiftFeedbackText, centerX, baseline, textPaint);
    }

    private void showFeedback(String text) {
        shiftFeedbackText = text;
        feedbackUntil = System.currentTimeMillis() + 1500L;
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

    private void drawPaddleShifters(Canvas canvas) {
        if (shiftDownBitmap != null) {
            canvas.drawBitmap(shiftDownBitmap, null, shiftDownRect, buttonPaint);
        }
        if (shiftUpBitmap != null) {
            canvas.drawBitmap(shiftUpBitmap, null, shiftUpRect, buttonPaint);
        }
    }

    private void drawVerticalGasPedal(Canvas canvas) {
        if (gasBitmap != null) {
            canvas.drawBitmap(gasBitmap, null, gasPedalRect, buttonPaint);
        }
    }

    private void drawNitroButton(Canvas canvas, CarPhysics player) {
        if (nitrousBitmap != null) {
            float centerX = nitroButtonRect.centerX();
            float centerY = nitroButtonRect.centerY();
            float radius = nitroButtonRect.width() * 0.5f;
            buttonPaint.setColor(Color.argb(230, 18, 22, 28));
            canvas.drawCircle(centerX, centerY, radius, buttonPaint);
            canvas.drawBitmap(nitrousBitmap, nitroSourceRect, nitroButtonRect, buttonPaint);

            float labelWidth = nitroButtonRect.width() * 0.70f;
            float labelHeight = nitroButtonRect.height() * 0.19f;
            float labelCenterY = centerY + nitroButtonRect.height() * 0.27f;
            hudBgPaint.setColor(Color.argb(220, 15, 18, 22));
            canvas.drawRoundRect(
                centerX - labelWidth * 0.5f,
                labelCenterY - labelHeight * 0.5f,
                centerX + labelWidth * 0.5f,
                labelCenterY + labelHeight * 0.5f,
                labelHeight * 0.35f,
                labelHeight * 0.35f,
                hudBgPaint
            );
            textPaint.setColor(Color.WHITE);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            textPaint.setTextSize(nitroButtonRect.width() * 0.13f);
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("NITRO", centerX, labelCenterY + textPaint.getTextSize() * 0.35f, textPaint);
        }
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
                } else if (nitroButtonRect.contains(x, y)) {
                    if (raceEngine.getState() == RaceEngine.RaceState.RACING) {
                        if (raceEngine.playerNitro()) {
                            soundManager.vibrate(100);
                        }
                    }
                } else if (gasPedalRect.contains(x, y)) {
                    if (raceEngine.getState() == RaceEngine.RaceState.RACING) {
                        // Gas pedal only applies before the light turns green, while nitro is separate.
                        gasPedalPressed = true;
                    } else {
                        gasPedalPressed = true;
                        if (raceEngine.getState() == RaceEngine.RaceState.STAGING) {
                            raceEngine.startCountdown();
                        }
                    }
                } else if (shiftUpRect.contains(x, y)) {
                    CarPhysics.ShiftResult shiftResult = raceEngine.playerShiftUp();
                    if (shiftResult == CarPhysics.ShiftResult.PERFECT) {
                        showFeedback("PERFECT SHIFT");
                        startAnimation(shiftUpAnimation);
                        soundManager.playPerfectShiftSound();
                    } else if (shiftResult == CarPhysics.ShiftResult.OVER_REV) {
                        showFeedback("OVEREV");
                        startOverrevAnimation();
                        soundManager.playShiftSound();
                    } else if (shiftResult != CarPhysics.ShiftResult.NONE) {
                        showFeedback("GOOD SHIFT");
                        startAnimation(shiftingAnimation != null && shiftingAnimation.length > 0 ? shiftingAnimation : shiftUpAnimation);
                        soundManager.playShiftSound();
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

    private void startAnimation(Bitmap[] animation) {
        if (animation != null && animation.length > 0) {
            activeAnimation = animation;
            animationStartedAt = System.currentTimeMillis();
        }
    }

    private void startOverrevAnimation() {
        if (overrevAnimation != null && overrevAnimation.length > 0) {
            startAnimation(overrevAnimation);
        } else {
            startAnimation(shiftUpAnimation);
        }
    }
}
