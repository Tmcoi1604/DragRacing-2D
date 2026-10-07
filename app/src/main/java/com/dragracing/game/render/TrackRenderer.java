package com.dragracing.game.render;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import com.dragracing.game.engine.RaceEngine;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TrackRenderer {
    private static final float TRACK_AREA_HEIGHT_RATIO = 0.78f;
    private static final float HORIZON_HEIGHT_RATIO = 0.62f;
    private static final float COASTAL_ROAD_TOP_RATIO = 574.0f / 768.0f;

    public enum EnvironmentType {
        TOKYO_NIGHT,
        ABANDONED_INDUSTRIAL,
        DESERT_SUNSET,
        HIGHWAY_SUNSET,
        COASTAL_DAY,
        SNOW_AURORA,
        SUBWAY_GRAFFITI,
        FOREST_DAWN,
        FESTIVAL_NIGHT,
        PRO_TRACK_DAY;

        public String getDisplayName() {
            switch (this) {
                case TOKYO_NIGHT: return "Thành phố đêm";
                case ABANDONED_INDUSTRIAL: return "Khu công nghiệp";
                case DESERT_SUNSET: return "Sa mạc";
                case HIGHWAY_SUNSET: return "Đại lộ cao tốc";
                case COASTAL_DAY: return "Đường đua ven biển";
                case SNOW_AURORA: return "Núi tuyết và cực quang";
                case SUBWAY_GRAFFITI: return "Hầm tàu điện graffiti";
                case FOREST_DAWN: return "Khu rừng  bình minh";
                case FESTIVAL_NIGHT: return "Đường đua lễ hội";
                case PRO_TRACK_DAY: return "Đường đua chuyên dụng";
                default: return name();
            }
        }

        private String backgroundAsset() {
            switch (this) {
                case TOKYO_NIGHT: return "night_city";
                case ABANDONED_INDUSTRIAL: return "dusk_industrial";
                case DESERT_SUNSET: return "dusk_desert";
                case HIGHWAY_SUNSET: return "dusk_highway";
                case COASTAL_DAY: return "day_coastal";
                case SNOW_AURORA: return "night_snowpass";
                case SUBWAY_GRAFFITI: return "underground";
                case FOREST_DAWN: return "dawn_forest";
                case FESTIVAL_NIGHT: return "night_festival";
                case PRO_TRACK_DAY: return "day_racetrack";
                default: return "day_racetrack";
            }
        }

        private String themeTilesetAsset() {
            switch (this) {
                case TOKYO_NIGHT: return "tilesets_city";
                case ABANDONED_INDUSTRIAL: return "tilesets_industrial";
                case DESERT_SUNSET: return "tilesets_desert";
                case HIGHWAY_SUNSET: return "tilesets_highway";
                case COASTAL_DAY: return "tilesets_coastal";
                case SNOW_AURORA: return "tilesets_snowpass";
                case SUBWAY_GRAFFITI: return "tilesets_underground";
                case FOREST_DAWN: return "tilesets_forest";
                case FESTIVAL_NIGHT: return "tilesets_festival";
                case PRO_TRACK_DAY: return "tilesets_racetrack";
                default: return "tilesets_racetrack";
            }
        }

        public static EnvironmentType careerEnvironment(int stage, int step, boolean boss) {
            EnvironmentType[] environments = values();
            int index = ((stage - 1) * 4 + step + (boss ? 5 : 0)) % environments.length;
            return environments[index];
        }
    }

    private final Paint skyPaint = new Paint();
    private final Paint sceneryPaint = new Paint();
    private final Paint crowdPaint = new Paint();
    private final Paint roadPaint = new Paint();
    private final Paint barrierPaint = new Paint();
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint finishLinePaint = new Paint();
    private final Paint assetPaint = new Paint();
    private final Context context;
    private final Map<String, Bitmap> assetCache = new HashMap<>();

    private EnvironmentType environmentType = EnvironmentType.TOKYO_NIGHT;

    public TrackRenderer(Context context) {
        this.context = context.getApplicationContext();
        skyPaint.setAntiAlias(false);
        sceneryPaint.setAntiAlias(false);
        crowdPaint.setAntiAlias(false);
        roadPaint.setAntiAlias(false);
        barrierPaint.setAntiAlias(false);
        linePaint.setAntiAlias(false);
        lightPaint.setAntiAlias(false);
        finishLinePaint.setAntiAlias(false);
        sceneryPaint.setColor(Color.parseColor("#303B42"));
        crowdPaint.setColor(Color.parseColor("#3C474D"));
        roadPaint.setColor(Color.parseColor("#121212"));
        linePaint.setColor(Color.WHITE);
        textPaint.setColor(Color.YELLOW);
        textPaint.setTextSize(22.0f);
        textPaint.setFakeBoldText(true);
        assetPaint.setFilterBitmap(false);
    }

    public void setEnvironmentType(EnvironmentType type) {
        this.environmentType = type;
        updateEnvironmentPaints();
    }

    public float getRoadTop(float height) {
        float trackAreaBottom = height * TRACK_AREA_HEIGHT_RATIO;
        if (environmentType == EnvironmentType.COASTAL_DAY) {
            return trackAreaBottom * COASTAL_ROAD_TOP_RATIO;
        }
        return trackAreaBottom * HORIZON_HEIGHT_RATIO + 10.0f;
    }

    private void updateEnvironmentPaints() {
        switch (environmentType) {
            case TOKYO_NIGHT:
            case FESTIVAL_NIGHT:
                sceneryPaint.setColor(Color.parseColor("#25204A"));
                break;
            case DESERT_SUNSET:
                sceneryPaint.setColor(Color.parseColor("#5D4037"));
                break;
            case ABANDONED_INDUSTRIAL:
            case SUBWAY_GRAFFITI:
                sceneryPaint.setColor(Color.parseColor("#263238"));
                break;
            case HIGHWAY_SUNSET:
                sceneryPaint.setColor(Color.parseColor("#455A64"));
                break;
            case COASTAL_DAY:
                sceneryPaint.setColor(Color.parseColor("#2E7D78"));
                break;
            case SNOW_AURORA:
                sceneryPaint.setColor(Color.parseColor("#B0BEC5"));
                break;
            case FOREST_DAWN:
                sceneryPaint.setColor(Color.parseColor("#285943"));
                break;
            case PRO_TRACK_DAY:
                sceneryPaint.setColor(Color.parseColor("#78909C"));
                break;
        }
        roadPaint.setColor(Color.parseColor("#121212"));
    }

    public void render(Canvas canvas, float width, float height, double viewDistance, RaceEngine raceEngine) {
        // Keep the track over the grey lower section of the background.
        float trackAreaBottom = height * TRACK_AREA_HEIGHT_RATIO;
        float horizonY = trackAreaBottom * HORIZON_HEIGHT_RATIO;
        float roadTop = getRoadTop(height);
        float trackHeight = trackAreaBottom - horizonY;
        float laneHeight = trackHeight / 2.0f;

        // Start line anchor
        float startX = width * 0.25f;
        float barrierY = roadTop - 10.0f;

        drawAssetTrack(canvas, width, height, horizonY, trackAreaBottom, roadTop, startX, viewDistance);
        if (false) {
        // 1. Sky
        int skyTop;
        int skyBottom;
        switch (environmentType) {
            case DESERT_SUNSET:
            case HIGHWAY_SUNSET:
                skyTop = Color.parseColor("#B8324B");
                skyBottom = Color.parseColor("#FFB86B");
                break;
            case SNOW_AURORA:
                skyTop = Color.parseColor("#101A45");
                skyBottom = Color.parseColor("#487D9A");
                break;
            case FOREST_DAWN:
                skyTop = Color.parseColor("#D76B62");
                skyBottom = Color.parseColor("#FFD08A");
                break;
            case COASTAL_DAY:
            case PRO_TRACK_DAY:
                skyTop = Color.parseColor("#4FA9D1");
                skyBottom = Color.parseColor("#BFE8E8");
                break;
            case ABANDONED_INDUSTRIAL:
            case SUBWAY_GRAFFITI:
                skyTop = Color.parseColor("#28333B");
                skyBottom = Color.parseColor("#697984");
                break;
            case TOKYO_NIGHT:
            case FESTIVAL_NIGHT:
            default:
                skyTop = Color.parseColor("#14152F");
                skyBottom = Color.parseColor("#4D3F68");
                break;
        }

        skyPaint.setShader(new LinearGradient(
                0, 0, 0, horizonY,
                skyTop, skyBottom,
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(0, 0, width, horizonY, skyPaint);

        // 2. Parallax Scenery
        float sceneryOffset = (float) (viewDistance * 3.0) % 200.0f;
        for (float x = -sceneryOffset; x < width + 100; x += 60) {
            float variation = Math.abs((int) (x * 17)) % 45;
            switch (environmentType) {
                case DESERT_SUNSET:
                    canvas.drawCircle(x + 30, horizonY, 28.0f + variation, sceneryPaint);
                    break;
                case COASTAL_DAY:
                    sceneryPaint.setColor(Color.parseColor("#2D8E98"));
                    canvas.drawRect(x, horizonY - 12, x + 60, horizonY, sceneryPaint);
                    canvas.drawCircle(x + 24, horizonY - 30 - variation * 0.25f, 18, sceneryPaint);
                    sceneryPaint.setColor(Color.parseColor("#2E7D78"));
                    break;
                case SNOW_AURORA:
                    sceneryPaint.setColor(Color.WHITE);
                    canvas.drawRect(x, horizonY - 20 - variation, x + 70, horizonY, sceneryPaint);
                    sceneryPaint.setColor(Color.parseColor("#B0BEC5"));
                    canvas.drawRect(x + 20, horizonY - 28 - variation, x + 35, horizonY, sceneryPaint);
                    break;
                case FOREST_DAWN:
                    canvas.drawRect(x + 23, horizonY - 48 - variation, x + 31, horizonY, sceneryPaint);
                    canvas.drawCircle(x + 27, horizonY - 54 - variation, 28, sceneryPaint);
                    break;
                case ABANDONED_INDUSTRIAL:
                    canvas.drawRect(x, horizonY - 40 - variation, x + 38, horizonY, sceneryPaint);
                    canvas.drawCircle(x + 19, horizonY - 40 - variation, 15, sceneryPaint);
                    break;
                case SUBWAY_GRAFFITI:
                    canvas.drawRect(x, horizonY - 35, x + 58, horizonY, sceneryPaint);
                    linePaint.setColor(Color.parseColor("#F06292"));
                    canvas.drawRect(x + 8, horizonY - 25, x + 22, horizonY - 18, linePaint);
                    linePaint.setColor(Color.parseColor("#29B6F6"));
                    canvas.drawRect(x + 28, horizonY - 30, x + 50, horizonY - 22, linePaint);
                    break;
                case HIGHWAY_SUNSET:
                    canvas.drawRect(x, horizonY - 16, x + 60, horizonY - 8, sceneryPaint);
                    canvas.drawRect(x + 12, horizonY - 45, x + 20, horizonY - 8, sceneryPaint);
                    canvas.drawRect(x + 40, horizonY - 45, x + 48, horizonY - 8, sceneryPaint);
                    break;
                case TOKYO_NIGHT:
                case FESTIVAL_NIGHT:
                    canvas.drawRect(x, horizonY - 35 - variation, x + 48, horizonY, sceneryPaint);
                    linePaint.setColor(environmentType == EnvironmentType.TOKYO_NIGHT
                            ? Color.parseColor("#00E5FF") : Color.parseColor("#FF4081"));
                    canvas.drawRect(x + 8, horizonY - 25, x + 17, horizonY - 18, linePaint);
                    canvas.drawRect(x + 28, horizonY - 40, x + 37, horizonY - 33, linePaint);
                    break;
                case PRO_TRACK_DAY:
                default:
                    canvas.drawRect(x, horizonY - 25 - variation * 0.5f, x + 48, horizonY, sceneryPaint);
                    break;
            }
        }

        if (environmentType == EnvironmentType.SNOW_AURORA) {
            linePaint.setColor(Color.parseColor("#50E3C2"));
            linePaint.setStrokeWidth(7.0f);
            canvas.drawLine(0, horizonY - 90, width * 0.35f, horizonY - 125, linePaint);
            canvas.drawLine(width * 0.35f, horizonY - 125, width * 0.7f, horizonY - 92, linePaint);
            canvas.drawLine(width * 0.7f, horizonY - 92, width, horizonY - 120, linePaint);
        }

        drawEnvironmentDetails(canvas, width, horizonY, viewDistance);

        // 3. Grandstand & Stadium Lights
        if (environmentType != EnvironmentType.PRO_TRACK_DAY) {
            float grandstandHeight = environmentType == EnvironmentType.SUBWAY_GRAFFITI ? 18.0f : 26.0f;
            canvas.drawRect(0, horizonY - grandstandHeight, width, horizonY, crowdPaint);

            float poleOffset = (float) (viewDistance * 8.0) % 240.0f;
            for (float px = -poleOffset; px < width + 200; px += 240) {
                linePaint.setColor(Color.parseColor("#4B5160"));
                linePaint.setStrokeWidth(3.0f);
                canvas.drawLine(px, horizonY - grandstandHeight - 40, px, horizonY, linePaint);

                // Lamp glow
                lightPaint.setColor(Color.argb(220, 255, 255, 220));
                canvas.drawCircle(px, horizonY - grandstandHeight - 40, 8.0f, lightPaint);
                lightPaint.setColor(Color.argb(35, 255, 255, 200));
                canvas.drawCircle(px, horizonY - grandstandHeight - 25, 36.0f, lightPaint);
            }
        }

        // 4. Barrier Wall
        int barrierPrimary = environmentType == EnvironmentType.ABANDONED_INDUSTRIAL
            ? Color.parseColor("#FBC02D")
            : environmentType == EnvironmentType.COASTAL_DAY ? Color.parseColor("#0277BD") : Color.parseColor("#B71C1C");
        barrierPaint.setColor(barrierPrimary);
        canvas.drawRect(0, barrierY, width, barrierY + 10, barrierPaint);
        barrierPaint.setColor(environmentType == EnvironmentType.ABANDONED_INDUSTRIAL ? Color.BLACK : Color.WHITE);
        float stripeOffset = (float) (viewDistance * 25.0) % 50.0f;
        for (float bx = -stripeOffset; bx < width + 50; bx += 50) {
            canvas.drawRect(bx, barrierY, bx + 25, barrierY + 10, barrierPaint);
        }

        // 5. Asphalt Track Surface
        canvas.drawRect(0, barrierY + 10, width, trackAreaBottom, roadPaint);

        // Center Lane Divider (Dashed Yellow)
        float dividerY = barrierY + 10 + laneHeight;
        linePaint.setColor(Color.parseColor("#FFC107"));
        linePaint.setStrokeWidth(3.0f);
        float dashOffset = (float) (viewDistance * 35.0) % 46.0f;
        for (float lx = -dashOffset; lx < width + 46; lx += 46) {
            canvas.drawLine(lx, dividerY, lx + 23, dividerY, linePaint);
        }

        // Bottom track edge line
        linePaint.setColor(Color.argb(180, 255, 255, 255));
        linePaint.setStrokeWidth(3.0f);
        canvas.drawLine(0, trackAreaBottom - 2, width, trackAreaBottom - 2, linePaint);

        }

        drawTrackObjects(canvas, width, horizonY, trackAreaBottom, roadTop,
            viewDistance, raceEngine);

        // 6. Starting Line
        double startLineWorldX = -viewDistance * 35.0 + startX;
        if (startLineWorldX > -80 && startLineWorldX < width + 80) {
            linePaint.setColor(Color.WHITE);
            linePaint.setStrokeWidth(6.0f);
            canvas.drawLine((float) startLineWorldX, barrierY + 10, (float) startLineWorldX, trackAreaBottom, linePaint);

                drawAssetStartSignal(canvas, (float) startLineWorldX + 22.0f,
                    barrierY - 6.0f, raceEngine);
        }

        // 7. Finish Line
        double raceDist = raceEngine != null ? raceEngine.getRaceDistance() : 402.336;
        double finishLineWorldX = (raceDist - viewDistance) * 35.0 + startX;
        if (finishLineWorldX > -150 && finishLineWorldX < width + 200) {
            drawAssetFinishLine(canvas, (float) finishLineWorldX, barrierY + 10, trackAreaBottom);
        }
    }

        private boolean drawAssetTrack(Canvas canvas, float width, float height,
                       float horizonY, float trackAreaBottom, float roadTop, float startX,
                       double viewDistance) {
        Bitmap themeBackground = loadAsset(environmentType.backgroundAsset());
        Bitmap themeTileset = loadAsset(environmentType.themeTilesetAsset());
            if (themeBackground == null || themeTileset == null) {
            return false;
        }

        Rect backgroundSource = new Rect(0, 0, themeBackground.getWidth(),
            themeBackground.getHeight());
        Rect backgroundDestination = new Rect(0, 0, (int) width, (int) trackAreaBottom);
        canvas.drawBitmap(themeBackground, backgroundSource, backgroundDestination, assetPaint);

        if (environmentType == EnvironmentType.TOKYO_NIGHT) {
            drawTokyoFarTrain(canvas, width, horizonY);
        } else if (environmentType == EnvironmentType.COASTAL_DAY) {
            drawCoastalSeaAndSky(canvas, width, horizonY, viewDistance, themeTileset);
        } else if (environmentType == EnvironmentType.DESERT_SUNSET) {
            drawDesertDunes(canvas, width, horizonY, viewDistance, themeTileset);
        } else if (environmentType == EnvironmentType.FESTIVAL_NIGHT) {
            drawFestivalSkyDecor(canvas, width, horizonY, viewDistance, themeTileset);
        }

        // Keep the asphalt surface consistent across all environments.
        canvas.drawRect(0, roadTop, width, trackAreaBottom, roadPaint);
        float dividerY = roadTop + (trackAreaBottom - roadTop) * 0.5f;

        linePaint.setColor(Color.parseColor("#FFC107"));
        linePaint.setStrokeWidth(3.0f);
        float dashOffset = (float) (viewDistance * 35.0) % 46.0f;
        for (float x = -dashOffset; x < width + 46.0f; x += 46.0f) {
            canvas.drawLine(x, dividerY, x + 23.0f, dividerY, linePaint);
        }

        // Each environment contributes a recognizable prop from its own tileset.
        if (environmentType == EnvironmentType.TOKYO_NIGHT) {
            return drawTokyoBuildings(canvas, width, horizonY, themeTileset, viewDistance);
        }
        return true;
        }

        private boolean drawTokyoBuildings(Canvas canvas, float width, float horizonY,
                           Bitmap tileset, double viewDistance) {
        int sourceHeight = Math.min(350, tileset.getHeight());
        int segmentWidth = Math.max(1, tileset.getWidth() / 8);
        float screenSegmentWidth = width / 8.0f;
        
        double worldX = viewDistance * 35.0 * 0.24;
        float scrollOffset = (float) (worldX % screenSegmentWidth);
        long segmentBaseIndex = (long) Math.floor(worldX / screenSegmentWidth);

        float[] skylineHeights = {
            0.72f, 0.48f, 0.88f, 0.56f,
            0.96f, 0.62f, 0.80f, 0.44f
        };

        for (int i = -1; i <= (int)(width / screenSegmentWidth) + 1; i++) {
            long absoluteIndex = segmentBaseIndex + i;
            int arrayIndex = (int) Math.floorMod(absoluteIndex, skylineHeights.length);
            
            int sourceLeft = arrayIndex * segmentWidth;
            int sourceRight = arrayIndex == skylineHeights.length - 1
                ? tileset.getWidth() : Math.min(sourceLeft + segmentWidth, tileset.getWidth());
            Rect source = new Rect(sourceLeft, 0, sourceRight, sourceHeight);
            
            float buildingHeight = Math.max(64.0f, horizonY * skylineHeights[arrayIndex]);
            float left = i * screenSegmentWidth - scrollOffset;
            RectF destination = new RectF(left, horizonY - buildingHeight,
                    left + screenSegmentWidth + 3.0f, horizonY + 4.0f);
            canvas.drawBitmap(tileset, source, destination, assetPaint);
        }
        return true;
        }

        private void drawTokyoFarTrain(Canvas canvas, float width, float horizonY) {
        Bitmap tileset = loadAsset("tilesets_city");
        if (tileset == null) return;

        Rect source = new Rect(650, 548, Math.min(1024, tileset.getWidth()),
            Math.min(705, tileset.getHeight()));
        Rect destination = new Rect((int) (width * 0.48f), (int) horizonY - 92,
            (int) width + 80, (int) horizonY - 8);
        canvas.drawBitmap(tileset, source, destination, assetPaint);
        }

        private void drawDesertDunes(Canvas canvas, float width, float horizonY,
                          double viewDistance, Bitmap desertTileset) {
        Rect[] duneSources = {
            new Rect(8, 80, 120, 130),
            new Rect(125, 78, 235, 130),
            new Rect(235, 80, 324, 130),
            new Rect(310, 35, 520, 132),
            new Rect(8, 180, 193, 250),
            new Rect(192, 188, 315, 248),
            new Rect(308, 145, 515, 250)
        };
        float spacing = 270.0f;
        double worldX = viewDistance * 35.0 * 0.16;
        float offset = (float) (worldX % spacing);
        long baseIndex = (long) Math.floor(worldX / spacing);

        for (int i = firstVisibleSegment(width, spacing); i < (width / spacing) + 2; i++) {
            long segment = baseIndex + i;
            int duneIndex = (int) Math.floorMod(worldHash(segment, 1), duneSources.length);
            Rect source = duneSources[duneIndex];
            float duneHeight = 92.0f + worldRandom(segment, 2) * 54.0f;
            float duneWidth = duneHeight * source.width() / source.height();
            float x = i * spacing - offset + worldRandom(segment, 3) * 42.0f;
            RectF destination = new RectF(x, horizonY - duneHeight + 3.0f,
                x + duneWidth, horizonY + 3.0f);
            canvas.drawBitmap(desertTileset, source, destination, assetPaint);
        }
        }

        private void drawFestivalSkyDecor(Canvas canvas, float width, float horizonY,
                               double viewDistance, Bitmap festivalTileset) {
        Rect[] fireworkSources = {
            new Rect(30, 435, 224, 700),
            new Rect(220, 540, 340, 710),
            new Rect(332, 438, 475, 590)
        };
        float fireworkSpacing = 370.0f;
        double fireworkWorldX = viewDistance * 35.0 * 0.18;
        float fireworkOffset = (float) (fireworkWorldX % fireworkSpacing);
        long fireworkBaseIndex = (long) Math.floor(fireworkWorldX / fireworkSpacing);

        for (int i = firstVisibleSegment(width, fireworkSpacing); i < (width / fireworkSpacing) + 2; i++) {
            long segment = fireworkBaseIndex + i;
            if (worldRandom(segment, 43) < 0.3f) continue;

            Rect source = fireworkSources[(int) Math.floorMod(
                worldHash(segment, 44), fireworkSources.length)];
            float height = 66.0f + worldRandom(segment, 45) * 40.0f;
            float drawWidth = height * source.width() / source.height();
            float x = i * fireworkSpacing - fireworkOffset
                + worldRandom(segment, 46) * (fireworkSpacing - drawWidth);
            float y = horizonY - 310.0f + worldRandom(segment, 47) * 95.0f;
            RectF destination = new RectF(x, y, x + drawWidth, y + height);
            canvas.drawBitmap(festivalTileset, source, destination, assetPaint);
        }

        Rect[] garlandSources = {
            new Rect(8, 228, 238, 310),
            new Rect(256, 228, 491, 310),
            new Rect(8, 310, 238, 393),
            new Rect(256, 310, 491, 393)
        };
        float garlandSpacing = 260.0f;
        double garlandWorldX = viewDistance * 35.0 * 0.32;
        float garlandOffset = (float) (garlandWorldX % garlandSpacing);
        long garlandBaseIndex = (long) Math.floor(garlandWorldX / garlandSpacing);

        for (int i = firstVisibleSegment(width, garlandSpacing); i < (width / garlandSpacing) + 2; i++) {
            long segment = garlandBaseIndex + i;
            int garlandIndex = (int) Math.floorMod(worldHash(segment, 41), garlandSources.length);
            Rect source = garlandSources[garlandIndex];
            float drawWidth = 230.0f;
            float height = drawWidth * source.height() / source.width();
            float x = i * garlandSpacing - garlandOffset;
            float y = horizonY - 235.0f + worldRandom(segment, 42) * 55.0f;
            RectF destination = new RectF(x, y, x + drawWidth, y + height);
            canvas.drawBitmap(festivalTileset, source, destination, assetPaint);
        }
        }

        private void drawDesertRoadsideObjects(Canvas canvas, float width, float roadTop,
                                    double viewDistance, Bitmap desertTileset) {
        Rect[] cactusSources = {
            new Rect(538, 22, 612, 150),
            new Rect(618, 84, 675, 148),
            new Rect(682, 40, 755, 150),
            new Rect(535, 155, 619, 250),
            new Rect(625, 190, 685, 250),
            new Rect(699, 162, 763, 250)
        };
        Rect[] signSources = {
            new Rect(23, 782, 173, 966),
            new Rect(860, 756, 972, 984)
        };
        float spacing = 290.0f;
        double worldX = viewDistance * 35.0;
        float offset = (float) (worldX % spacing);
        long baseIndex = (long) Math.floor(worldX / spacing);

        for (int i = firstVisibleSegment(width, spacing); i < (width / spacing) + 2; i++) {
            long segment = baseIndex + i;
            if (worldRandom(segment, 4) < 0.18f) continue;

            boolean useCactus = worldRandom(segment, 5) < 0.72f;
            Rect source;
            float objectHeight;
            if (useCactus) {
                int cactusIndex = (int) Math.floorMod(worldHash(segment, 6), cactusSources.length);
                source = cactusSources[cactusIndex];
                objectHeight = 42.0f + worldRandom(segment, 7) * 38.0f;
            } else {
                int signIndex = (int) Math.floorMod(worldHash(segment, 8), signSources.length);
                source = signSources[signIndex];
                objectHeight = signIndex == 0 ? 68.0f : 76.0f;
            }

            float objectWidth = objectHeight * source.width() / source.height();
            float x = i * spacing - offset + worldRandom(segment, 9) * 54.0f;
            x += worldRandom(segment, 10) < 0.5f ? width * 0.035f : width * 0.91f;
            RectF destination = new RectF(x, roadTop - objectHeight + 4.0f,
                x + objectWidth, roadTop + 4.0f);
            canvas.drawBitmap(desertTileset, source, destination, assetPaint);
        }
        }

        private void drawHighwayRoadsideObjects(Canvas canvas, float width, float roadTop,
                                     double viewDistance, Bitmap highwayTileset) {
        Rect[] objectSources = {
            new Rect(530, 286, 767, 490),   // electronic traffic sign
            new Rect(785, 294, 999, 488),   // roadside billboard
            new Rect(20, 750, 272, 970),    // exit sign
            new Rect(535, 748, 632, 975),   // speed limit sign
            new Rect(780, 30, 1018, 250),   // street lights
            new Rect(300, 750, 512, 975),   // striped road barrier
            new Rect(800, 570, 1005, 700),  // concrete barriers and cones
            new Rect(655, 568, 790, 700),   // oil drums
            new Rect(515, 544, 635, 700),   // fuel pump
            new Rect(255, 540, 510, 700),   // cargo crates
            new Rect(10, 570, 245, 700),    // road debris and tires
            new Rect(5, 300, 265, 485),     // red truck
            new Rect(270, 300, 518, 485),   // blue truck
            new Rect(525, 52, 753, 250)     // roadside service hut
        };
        float[] heights = {
            82.0f, 78.0f, 88.0f, 84.0f, 100.0f, 74.0f, 62.0f,
            58.0f, 70.0f, 68.0f, 60.0f, 76.0f, 76.0f, 86.0f
        };
        float spacing = 275.0f;
        double worldX = viewDistance * 35.0;
        float offset = (float) (worldX % spacing);
        long baseIndex = (long) Math.floor(worldX / spacing);

        for (int i = firstVisibleSegment(width, spacing); i < (width / spacing) + 2; i++) {
            long segment = baseIndex + i;
            if (worldRandom(segment, 11) < 0.22f) continue;

            int objectIndex = (int) Math.floorMod(worldHash(segment, 12), objectSources.length);
            Rect source = objectSources[objectIndex];
            float objectHeight = heights[objectIndex];
            float objectWidth = objectHeight * source.width() / source.height();
            float x = i * spacing - offset + worldRandom(segment, 13) * 45.0f;
            boolean leftSide = worldRandom(segment, 14) < 0.5f;
            x += leftSide ? width * 0.025f : width * 0.975f - objectWidth;

            RectF destination = new RectF(x, roadTop - objectHeight + 4.0f,
                x + objectWidth, roadTop + 4.0f);
            canvas.drawBitmap(highwayTileset, source, destination, assetPaint);
        }
        }

        private void drawFestivalRoadsideObjects(Canvas canvas, float width, float roadTop,
                                      double viewDistance, Bitmap festivalTileset) {
        Rect[] stallSources = {
            new Rect(15, 45, 147, 206),
            new Rect(156, 45, 276, 206),
            new Rect(280, 45, 385, 206),
            new Rect(387, 45, 505, 206),
            new Rect(505, 47, 628, 207),
            new Rect(632, 17, 1005, 208)
        };
        Rect[] objectSources = {
            new Rect(495, 218, 770, 418),  // food truck
            new Rect(514, 450, 718, 613),  // festival sign
            new Rect(512, 615, 720, 775),  // drinks sign
            new Rect(886, 524, 1002, 761), // crowd
            new Rect(15, 788, 80, 895),    // cheering visitor
            new Rect(84, 775, 159, 875),   // visitor with camera
            new Rect(160, 775, 224, 875),  // photographer
            new Rect(255, 779, 313, 875),  // visitor
            new Rect(332, 858, 518, 952)   // festival merchandise cart
        };
        float[] heights = { 112.0f, 104.0f, 104.0f, 114.0f, 102.0f, 114.0f, 106.0f, 108.0f, 100.0f };
        float spacing = 280.0f;
        double worldX = viewDistance * 35.0;
        float offset = (float) (worldX % spacing);
        long baseIndex = (long) Math.floor(worldX / spacing);

        for (int i = firstVisibleSegment(width, spacing); i < (width / spacing) + 2; i++) {
            long segment = baseIndex + i;
            if (worldRandom(segment, 51) < 0.14f) continue;

            float choice = worldRandom(segment, 52);
            Rect source;
            float objectHeight;
            if (choice < 0.39f) {
                int stallIndex = (int) Math.floorMod(worldHash(segment, 53), stallSources.length);
                source = stallSources[stallIndex];
                objectHeight = stallIndex == 5
                    ? 128.0f
                    : (185.0f * 1.15f) * source.height() / source.width();
            } else {
                float objectChoice = (choice - 0.39f) / 0.61f;
                int objectIndex = Math.min((int) (objectChoice * objectSources.length),
                    objectSources.length - 1);
                source = objectSources[objectIndex];
                objectHeight = objectIndex == 0
                    ? (185.0f * 1.15f) * source.height() / source.width()
                    : heights[objectIndex];
            }

            float objectWidth = objectHeight * source.width() / (float) source.height();
            float x = i * spacing - offset + worldRandom(segment, 54) * 42.0f;
            boolean leftSide = worldRandom(segment, 55) < 0.5f;
            x += leftSide ? width * 0.02f : width * 0.98f - objectWidth;

            RectF destination = new RectF(x, roadTop - objectHeight + 4.0f,
                x + objectWidth, roadTop + 4.0f);
            canvas.drawBitmap(festivalTileset, source, destination, assetPaint);
        }
        }

        private void drawProTrackObjects(Canvas canvas, float width, float horizonY,
                              float roadTop, double viewDistance, RaceEngine raceEngine,
                              Bitmap raceTileset) {
        Rect grandstandSource = new Rect(20, 250, 470, 490);
        double raceDistance = raceEngine != null ? raceEngine.getRaceDistance() : 402.336;
        float startX = width * 0.25f;
        double startWorldX = startX - viewDistance * 35.0;
        if (startWorldX > -width * 0.7f && startWorldX < width * 1.2f) {
            drawGrandstandAt(canvas, width, horizonY, (float) startWorldX,
                grandstandSource, raceTileset);
        }

        double finishWorldX = (raceDistance - viewDistance) * 35.0 + startX;
        if (finishWorldX > -width * 0.7f && finishWorldX < width * 1.2f) {
            drawGrandstandAt(canvas, width, horizonY, (float) finishWorldX,
                grandstandSource, raceTileset);
        }

        Bitmap commonTileset = loadAsset("tilesets");
        if (commonTileset == null) return;

        Rect[] raceSources = {
            new Rect(490, 280, 635, 478),   // drag racing news billboard
            new Rect(645, 280, 790, 478),   // VP Racing billboard
            new Rect(800, 326, 1005, 486),  // tire transport cart
            new Rect(385, 530, 550, 660),   // camera operator
            new Rect(685, 700, 770, 845),   // loudspeaker
            new Rect(780, 755, 900, 845),   // video camera
            new Rect(25, 525, 105, 660),    // track marshal
            new Rect(125, 530, 225, 662),   // track marshal
            new Rect(240, 525, 352, 658),   // track mechanic
            new Rect(25, 690, 130, 840),    // cheering spectator
            new Rect(130, 690, 240, 840),   // spectator group
            new Rect(905, 890, 990, 988)    // fuel can
        };
        float[] heights = { 112.0f, 112.0f, 74.0f, 78.0f, 74.0f, 68.0f,
            76.0f, 76.0f, 78.0f, 78.0f, 82.0f, 54.0f };
        float spacing = 305.0f;
        double worldX = viewDistance * 35.0;
        float offset = (float) (worldX % spacing);
        long baseIndex = (long) Math.floor(worldX / spacing);

        for (int i = firstVisibleSegment(width, spacing); i < (width / spacing) + 2; i++) {
            long segment = baseIndex + i;
            if (worldRandom(segment, 61) < 0.13f) continue;

            int objectIndex = (int) Math.floorMod(worldHash(segment, 62), raceSources.length);
            Rect source = raceSources[objectIndex];
            float objectHeight = heights[objectIndex];
            float objectWidth = objectHeight * source.width() / (float) source.height();
            float x = i * spacing - offset + worldRandom(segment, 63) * 44.0f;
            boolean leftSide = worldRandom(segment, 64) < 0.5f;
            x += leftSide ? width * 0.02f : width * 0.98f - objectWidth;

            RectF destination = new RectF(x, roadTop - objectHeight + 4.0f,
                x + objectWidth, roadTop + 4.0f);
            canvas.drawBitmap(commonTileset, source, destination, assetPaint);
        }
        }

        private void drawGrandstandAt(Canvas canvas, float width, float horizonY,
                           float centerX, Rect source, Bitmap raceTileset) {
        float standWidth = width * 0.72f;
        float standHeight = horizonY * 0.72f;
        RectF destination = new RectF(centerX - standWidth * 0.5f,
            horizonY - standHeight, centerX + standWidth * 0.5f, horizonY + 3.0f);
        canvas.drawBitmap(raceTileset, source, destination, assetPaint);
        }

        private void drawSnowRoadsideObjects(Canvas canvas, float width, float roadTop,
                                  double viewDistance, Bitmap snowTileset) {
        Rect[] objectSources = {
            new Rect(635, 21, 767, 245),   // tall pine
            new Rect(781, 54, 896, 246),   // medium pine
            new Rect(912, 102, 1005, 246), // small pine
            new Rect(535, 290, 740, 493),  // Night Run Speedway sign
            new Rect(787, 292, 990, 493),  // Summit Chalet sign
            new Rect(895, 565, 1003, 688), // snowman
            new Rect(597, 565, 725, 693),  // snow barrels
            new Rect(7, 510, 293, 700),    // snow rocks
            new Rect(390, 35, 615, 250)    // mountain chalet
        };
        float[] heights = {
            112.0f, 98.0f, 82.0f, 132.0f, 132.0f, 72.0f, 64.0f, 72.0f, 102.0f
        };
        float spacing = 320.0f;
        double worldX = viewDistance * 35.0;
        float offset = (float) (worldX % spacing);
        long baseIndex = (long) Math.floor(worldX / spacing);

        for (int i = firstVisibleSegment(width, spacing); i < (width / spacing) + 2; i++) {
            long segment = baseIndex + i;
            if (worldRandom(segment, 21) < 0.16f) continue;

            int objectIndex = (int) Math.floorMod(worldHash(segment, 22), objectSources.length);
            Rect source = objectSources[objectIndex];
            float objectHeight = heights[objectIndex];
            float objectWidth = objectHeight * source.width() / (float) source.height();
            float x = i * spacing - offset + worldRandom(segment, 23) * 52.0f;
            boolean leftSide = worldRandom(segment, 24) < 0.5f;
            x += leftSide ? width * 0.025f : width * 0.975f - objectWidth;

            RectF destination = new RectF(x, roadTop - objectHeight + 4.0f,
                x + objectWidth, roadTop + 4.0f);
            canvas.drawBitmap(snowTileset, source, destination, assetPaint);
        }
        }

        private void drawForestRoadsideObjects(Canvas canvas, float width, float roadTop,
                                    double viewDistance, Bitmap forestTileset) {
        Bitmap commonTileset = loadAsset("tilesets");
        if (commonTileset == null) return;

        Rect[] forestSources = {
            new Rect(5, 158, 130, 300),    // large tree cluster
            new Rect(249, 160, 345, 305),  // broad tree
            new Rect(4, 8, 130, 148),      // leafy trees
            new Rect(165, 5, 248, 150)     // slender tree
        };
        Rect[] commonSources = {
            new Rect(418, 746, 480, 875),  // warning sign
            new Rect(545, 740, 600, 878),  // trail signpost
            new Rect(8, 738, 105, 879),     // street light
            new Rect(130, 738, 205, 879),   // street light
            new Rect(12, 922, 108, 990),    // low shrubs
            new Rect(145, 925, 232, 990)    // bushes
        };

        float spacing = 300.0f;
        double worldX = viewDistance * 35.0;
        float offset = (float) (worldX % spacing);
        long baseIndex = (long) Math.floor(worldX / spacing);

        for (int i = firstVisibleSegment(width, spacing); i < (width / spacing) + 2; i++) {
            long segment = baseIndex + i;
            float choice = worldRandom(segment, 31);
            Rect source;
            Bitmap sourceTileset;
            float objectHeight;

            if (choice < 0.62f) {
                int treeIndex = (int) Math.floorMod(worldHash(segment, 32), forestSources.length);
                source = forestSources[treeIndex];
                sourceTileset = forestTileset;
                objectHeight = (0.42f + worldRandom(segment, 33) * 0.08f) * roadTop;
            } else if (choice < 0.74f) {
                source = commonSources[(int) Math.floorMod(worldHash(segment, 34), 2)];
                sourceTileset = commonTileset;
                objectHeight = roadTop * 0.34f;
            } else if (choice < 0.87f) {
                int lampIndex = 2 + (int) Math.floorMod(worldHash(segment, 35), 2);
                source = commonSources[lampIndex];
                sourceTileset = commonTileset;
                objectHeight = roadTop * 0.39f;
            } else {
                int bushIndex = 4 + (int) Math.floorMod(worldHash(segment, 36), 2);
                source = commonSources[bushIndex];
                sourceTileset = commonTileset;
                objectHeight = roadTop * 0.19f;
            }

            float objectWidth = objectHeight * source.width() / (float) source.height();
            float x = i * spacing - offset + worldRandom(segment, 37) * 50.0f;
            boolean leftSide = worldRandom(segment, 38) < 0.5f;
            x += leftSide ? width * 0.025f : width * 0.975f - objectWidth;

            RectF destination = new RectF(x, roadTop - objectHeight + 4.0f,
                x + objectWidth, roadTop + 4.0f);
            canvas.drawBitmap(sourceTileset, source, destination, assetPaint);
        }
        }

        private float worldRandom(long segment, int salt) {
        return (float) (Integer.toUnsignedLong(worldHash(segment, salt)) / 4294967296.0);
        }

        private int worldHash(long segment, int salt) {
        long value = segment * 0x9E3779B97F4A7C15L + salt * 0xD1B54A32D192ED03L;
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return (int) (value >>> 32);
        }

        private int firstVisibleSegment(float width, float spacing) {
        return -(int) Math.ceil(width / spacing) - 2;
        }

        private void drawCoastalSeaAndSky(Canvas canvas, float width, float horizonY,
                          double viewDistance, Bitmap coastalTileset) {
        double worldX = viewDistance * 35.0;
        float boatSpacing = 330.0f;
        float boatOffset = (float) (worldX * 0.42 % boatSpacing);
        Rect[] boatSources = {
            new Rect(45, 360, 250, 500),    // Boat 1
            new Rect(305, 355, 520, 480),   // Boat 2
            new Rect(535, 300, 755, 430),   // Boat 3
            new Rect(770, 300, 1005, 430)   // Boat 4
        };
        for (int i = firstVisibleSegment(width, boatSpacing); i < (width / boatSpacing) + 2; i++) {
            int boatIndex = Math.floorMod(i, boatSources.length);
            float x = i * boatSpacing - boatOffset + width * 0.18f;
            Rect source = boatSources[boatIndex];
            float boatHeight = 42.0f;
            float boatWidth = boatHeight * source.width() / (float) source.height();
            RectF destination = new RectF(x, horizonY - 92.0f,
                x + boatWidth, horizonY - 92.0f + boatHeight);
            canvas.drawBitmap(coastalTileset, source, destination, assetPaint);
        }

        Rect[] gullSources = {
            new Rect(611, 761, 670, 815),
            new Rect(517, 778, 605, 817),
            new Rect(532, 829, 620, 872)
        };
        float gullSpacing = 220.0f;
        float gullOffset = (float) (worldX * 0.18 % gullSpacing);
        for (int i = firstVisibleSegment(width, gullSpacing); i < (width / gullSpacing) + 2; i++) {
            int gullIndex = Math.floorMod(i, gullSources.length);
            Rect source = gullSources[gullIndex];
            float x = i * gullSpacing - gullOffset + 90.0f;
            float gullHeight = 32.0f;
            float gullWidth = gullHeight * source.width() / (float) source.height();
            float gullY = horizonY - 175.0f - (gullIndex % 3) * 12.0f;
            RectF destination = new RectF(x, gullY,
                x + gullWidth, gullY + gullHeight);
            canvas.drawBitmap(coastalTileset, source, destination, assetPaint);
        }
        }

        private void drawTrackObjects(Canvas canvas, float width, float horizonY,
                          float trackAreaBottom, float roadTop, double viewDistance,
                          RaceEngine raceEngine) {
        Bitmap tileset = loadAsset(environmentType.themeTilesetAsset());
        if (tileset == null) return;

        if (environmentType == EnvironmentType.TOKYO_NIGHT) {
            drawTokyoRoadsideObjects(canvas, width, horizonY, viewDistance, tileset);
            drawTokyoBaseSigns(canvas, width, horizonY, viewDistance);
            return;
        }

        if (environmentType == EnvironmentType.ABANDONED_INDUSTRIAL) {
            drawIndustrialRoadsideObjects(canvas, width, horizonY, trackAreaBottom,
                    viewDistance, tileset);
            return;
        }

        if (environmentType == EnvironmentType.COASTAL_DAY) {
            drawCoastalRoadsideObjects(canvas, width, roadTop, viewDistance, tileset);
            return;
        }

        if (environmentType == EnvironmentType.DESERT_SUNSET) {
            drawDesertRoadsideObjects(canvas, width, roadTop, viewDistance, tileset);
            return;
        }

        if (environmentType == EnvironmentType.HIGHWAY_SUNSET) {
            drawHighwayRoadsideObjects(canvas, width, roadTop, viewDistance, tileset);
            return;
        }

        if (environmentType == EnvironmentType.SNOW_AURORA) {
            drawSnowRoadsideObjects(canvas, width, roadTop, viewDistance, tileset);
            return;
        }

        if (environmentType == EnvironmentType.FOREST_DAWN) {
            drawForestRoadsideObjects(canvas, width, roadTop, viewDistance, tileset);
            return;
        }

        if (environmentType == EnvironmentType.FESTIVAL_NIGHT) {
            drawFestivalRoadsideObjects(canvas, width, roadTop, viewDistance, tileset);
            return;
        }

        if (environmentType == EnvironmentType.PRO_TRACK_DAY) {
            drawProTrackObjects(canvas, width, horizonY, roadTop, viewDistance,
                raceEngine, tileset);
            return;
        }

        float startX = width * 0.25f;
        float objectSpacing = 180.0f;
        
        double worldX = viewDistance * 35.0;
        float offset = (float) (worldX % objectSpacing);
        long baseIndex = (long) Math.floor(worldX / objectSpacing);

        int sheetWidth = tileset.getWidth();
        int sheetHeight = tileset.getHeight();
        Rect[] objectSources = {
                new Rect(0, 0, Math.min(300, sheetWidth), Math.min(235, sheetHeight)),
                new Rect(Math.min(300, sheetWidth), 0, Math.min(600, sheetWidth), Math.min(235, sheetHeight)),
                new Rect(Math.min(600, sheetWidth), 0, Math.min(900, sheetWidth), Math.min(235, sheetHeight))
        };
        Rect barrierSource = new Rect(0, Math.min(235, sheetHeight),
                Math.min(300, sheetWidth), Math.min(470, sheetHeight));

        for (int i = firstVisibleSegment(width, objectSpacing); i < (width / objectSpacing) + 2; i++) {
            long absoluteIndex = baseIndex + i;
            int objectIndex = (int) Math.floorMod(absoluteIndex, 3);
            float x = startX + (i * objectSpacing) - offset;
            
            float propY = horizonY - (objectIndex == 0 ? 66.0f : 48.0f);
            RectF destination = new RectF(x, propY, x + 58.0f, propY + 70.0f);
            canvas.drawBitmap(tileset, objectSources[objectIndex],
                destination, assetPaint);
        }

        // Fixed barriers close both sides of the strip using the barrier sprite.
        float barrierSpacing = 110.0f;
        float barrierOffset = (float) (worldX % barrierSpacing);
        for (float x = -barrierOffset; x < width; x += barrierSpacing) {
                RectF upperBarrier = new RectF(x, horizonY - 18.0f,
                    x + 100.0f, horizonY + 10.0f);
                canvas.drawBitmap(tileset, barrierSource, upperBarrier, assetPaint);
                RectF lowerBarrier = new RectF(x, trackAreaBottom - 28.0f,
                    x + 100.0f, trackAreaBottom);
            canvas.drawBitmap(tileset, barrierSource, lowerBarrier, assetPaint);
        }
        }

        private void drawIndustrialRoadsideObjects(Canvas canvas, float width, float horizonY,
                          float trackAreaBottom, double viewDistance,
                          Bitmap industrialTileset) {
        double worldX = viewDistance * 35.0;
        float objectSpacing = 185.0f;
        float objectOffset = (float) (worldX % objectSpacing);
        long objectBaseIndex = (long) Math.floor(worldX / objectSpacing);

        Rect[] industrialSources = {
            new Rect(0, 0, 270, 245),       // factory buildings
            new Rect(575, 0, 815, 245),     // warehouse
            new Rect(555, 270, 800, 520),   // crane
            new Rect(815, 270, 1024, 520),  // smokestack
            new Rect(0, 555, 180, 720),     // scrap pile
            new Rect(610, 535, 780, 735),   // cable spool
            new Rect(815, 555, 1024, 710)   // industrial fence
        };
        float[] objectHeights = { 92.0f, 82.0f, 96.0f, 94.0f, 54.0f, 62.0f, 58.0f };

        for (int i = firstVisibleSegment(width, objectSpacing); i < (width / objectSpacing) + 2; i++) {
            long absoluteIndex = objectBaseIndex + i;
            int objectIndex = (int) Math.floorMod(absoluteIndex, industrialSources.length);
            boolean leftSide = (absoluteIndex & 1L) == 0L;
            Rect source = industrialSources[objectIndex];
            float height = objectHeights[objectIndex];
            float objectWidth = height * source.width() / (float) source.height();
            float x = i * objectSpacing - objectOffset;
            float objectY = horizonY - height + 4.0f;
            if (!leftSide) x += width * 0.52f;

            RectF destination = new RectF(x, objectY, x + objectWidth, horizonY + 4.0f);
            canvas.drawBitmap(industrialTileset, source, destination, assetPaint);
        }

        drawIndustrialOilMarks(canvas, width, horizonY, trackAreaBottom, worldX);
        drawIndustrialBaseSigns(canvas, width, horizonY, worldX);
        }

        private void drawIndustrialOilMarks(Canvas canvas, float width, float horizonY,
                            float trackAreaBottom, double worldX) {
        Bitmap baseTileset = loadAsset("tilesets");
        if (baseTileset == null) return;

        Rect[] oilSources = {
            new Rect(535, 650, 625, 710),
            new Rect(650, 635, 760, 720),
            new Rect(815, 635, 910, 710)
        };
        float spacing = 430.0f;
        float offset = (float) (worldX % spacing);
        long baseIndex = (long) Math.floor(worldX / spacing);
        float roadTop = horizonY + 10.0f;
        float laneHeight = (trackAreaBottom - roadTop) * 0.5f;

        for (int i = firstVisibleSegment(width, spacing); i < (width / spacing) + 2; i++) {
            long absoluteIndex = baseIndex + i;
            if (Math.floorMod(absoluteIndex, 3) != 1) continue;
            int oilIndex = (int) Math.floorMod(absoluteIndex, oilSources.length);
            float x = i * spacing - offset + 70.0f;
            float y = roadTop + (Math.floorMod(absoluteIndex, 2) == 0 ? 10.0f : laneHeight + 12.0f);
            Rect source = oilSources[oilIndex];
            RectF destination = new RectF(x, y, x + 88.0f, y + 34.0f);
            canvas.drawBitmap(baseTileset, source, destination, assetPaint);
        }
        }

        private void drawIndustrialBaseSigns(Canvas canvas, float width, float horizonY,
                             double worldX) {
        Bitmap baseTileset = loadAsset("tilesets");
        if (baseTileset == null) return;

        Rect[] signSources = {
            new Rect(300, 735, 390, 900),
            new Rect(425, 735, 510, 900),
            new Rect(545, 720, 640, 900)
        };
        float spacing = 360.0f;
        float offset = (float) ((worldX * 0.8) % spacing);
        long baseIndex = (long) Math.floor((worldX * 0.8) / spacing);
        for (int i = firstVisibleSegment(width, spacing); i < (width / spacing) + 2; i++) {
            long absoluteIndex = baseIndex + i;
            if (Math.floorMod(absoluteIndex, 3) == 0) continue;
            int signIndex = (int) Math.floorMod(absoluteIndex, signSources.length);
            Rect source = signSources[signIndex];
            float height = 58.0f;
            float signWidth = height * source.width() / source.height();
            float x = i * spacing - offset + width * 0.24f;
            RectF destination = new RectF(x, horizonY - height + 4.0f,
                x + signWidth, horizonY + 4.0f);
            canvas.drawBitmap(baseTileset, source, destination, assetPaint);
        }
        }

        private void drawTokyoRoadsideObjects(Canvas canvas, float width, float horizonY,
                              double viewDistance, Bitmap tileset) {
        float spacing = 155.0f;
        double worldX = viewDistance * 35.0;
        float offset = (float) (worldX % spacing);
        long baseIndex = (long) Math.floor(worldX / spacing);

        Rect[] sources = {
            new Rect(0, 350, 230, 565),       // torii gate
            new Rect(250, 350, 625, 565),     // cherry trees
            new Rect(645, 370, 1024, 565),    // food stalls
            new Rect(250, 725, 370, 1024),    // vending machines
            new Rect(505, 735, 820, 875),     // neon signs
            new Rect(850, 710, 1024, 1024)    // utility pole
        };
        
        for (int i = firstVisibleSegment(width, spacing); i < (width / spacing) + 2; i++) {
            long absoluteIndex = baseIndex + i;
            int index = (int) Math.floorMod(absoluteIndex, sources.length);
            float x = (i * spacing) - offset;
            
            Rect source = sources[index];
            float objectHeight = index == 0 ? 92.0f : index == 1 ? 68.0f : 58.0f;
            float objectWidth = objectHeight * (source.width() / (float) source.height());
            float objectY = horizonY - objectHeight + 5.0f;
                RectF destination = new RectF(x, objectY,
                    x + objectWidth, horizonY + 5.0f);
            canvas.drawBitmap(tileset, source, destination, assetPaint);
        }
        }

        private void drawCoastalRoadsideObjects(Canvas canvas, float width, float roadTop,
                            double viewDistance, Bitmap coastalTileset) {
        double worldX = viewDistance * 35.0;
        float spacing = 190.0f;
        float offset = (float) (worldX % spacing);
        long baseIndex = (long) Math.floor(worldX / spacing);

        Rect[] roadsideSources = {
            new Rect(9, 20, 342, 351),       // palm cluster
            new Rect(360, 24, 519, 242),     // beach hut
            new Rect(550, 20, 838, 242),     // tall palms
            new Rect(22, 512, 220, 693),     // beach umbrella
            new Rect(242, 541, 367, 675),    // small sand castle
            new Rect(364, 528, 516, 674),    // large sand castle
            new Rect(515, 477, 643, 695),    // lighthouse
            new Rect(642, 553, 782, 690),    // beach crates
            new Rect(800, 543, 1003, 678),   // beach debris
            new Rect(837, 792, 985, 970)     // clam shack
        };
        float[] heights = { 98.0f, 92.0f, 102.0f, 56.0f, 50.0f, 60.0f, 82.0f, 54.0f, 68.0f, 65.0f };

        for (int i = firstVisibleSegment(width, spacing); i < (width / spacing) + 2; i++) {
            long absoluteIndex = baseIndex + i;
            int index = Math.floorMod(absoluteIndex, roadsideSources.length);
            Rect source = roadsideSources[index];
            float objectHeight = heights[index];
            float objectWidth = objectHeight * source.width() / (float) source.height();
            float x = i * spacing - offset;
            float objectY = roadTop - objectHeight + 4.0f;
            RectF destination = new RectF(x, objectY, x + objectWidth, roadTop + 4.0f);
            canvas.drawBitmap(coastalTileset, source, destination, assetPaint);
        }

        drawCoastalSigns(canvas, width, roadTop, worldX, coastalTileset);
        }

        private void drawCoastalSigns(Canvas canvas, float width, float roadTop,
                          double worldX, Bitmap coastalTileset) {
        Rect coastalSign = new Rect(20, 735, 250, 970);
        Rect speedSign = new Rect(284, 755, 372, 965);
        float spacing = 360.0f;
        float offset = (float) (worldX * 0.82 % spacing);
        long baseIndex = (long) Math.floor(worldX * 0.82 / spacing);
        for (int i = firstVisibleSegment(width, spacing); i < (width / spacing) + 2; i++) {
            long absoluteIndex = baseIndex + i;
            float x = i * spacing - offset + width * 0.18f;
            Rect source = Math.floorMod(absoluteIndex, 2) == 0 ? coastalSign : speedSign;
            float height = 62.0f;
            float objectWidth = height * source.width() / (float) source.height();
            RectF destination = new RectF(x, roadTop - height + 4.0f,
                x + objectWidth, roadTop + 4.0f);
            canvas.drawBitmap(coastalTileset, source, destination, assetPaint);
        }
        }

        private void drawTokyoBaseSigns(Canvas canvas, float width, float horizonY,
                        double viewDistance) {
        Bitmap baseTileset = loadAsset("tilesets");
        if (baseTileset == null) return;

        Rect[] signSources = {
            new Rect(285, 735, 390, 900),
            new Rect(420, 735, 505, 900),
            new Rect(540, 720, 635, 900)
        };
        float spacing = 260.0f;
        double worldX = viewDistance * 28.0;
        float offset = (float) (worldX % spacing);
        long baseIndex = (long) Math.floor(worldX / spacing);

        for (int i = firstVisibleSegment(width, spacing); i < (width / spacing) + 2; i++) {
            long absoluteIndex = baseIndex + i;
            int index = (int) Math.floorMod(absoluteIndex, signSources.length);
            float x = (i * spacing) - offset;
            
            Rect source = signSources[index];
            float height = 58.0f;
            float signWidth = height * source.width() / source.height();
                RectF destination = new RectF(x, horizonY - height + 4.0f,
                    x + signWidth, horizonY + 4.0f);
            canvas.drawBitmap(baseTileset, source, destination, assetPaint);
        }
        }

        private Bitmap loadAsset(String assetName) {
        Bitmap cached = assetCache.get(assetName);
        if (cached != null) return cached;

        int resourceId = context.getResources().getIdentifier(
            assetName, "drawable", context.getPackageName());
        if (resourceId == 0) return null;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(
            context.getResources(), resourceId, options);
        if (bitmap != null) assetCache.put(assetName, bitmap);
        return bitmap;
        }

    private void drawEnvironmentDetails(Canvas canvas, float width, float horizonY, double viewDistance) {
        double worldX = viewDistance * 2.5;
        float spacing = 320.0f;
        float offset = (float) (worldX % spacing);
        long baseIndex = (long) Math.floor(worldX / spacing);

        switch (environmentType) {
            case TOKYO_NIGHT:
                drawTokyoDetails(canvas, width, horizonY, offset, baseIndex);
                break;
            case ABANDONED_INDUSTRIAL:
                drawIndustrialDetails(canvas, width, horizonY, offset, baseIndex);
                break;
            case DESERT_SUNSET:
                drawDesertDetails(canvas, width, horizonY, offset);
                break;
            case HIGHWAY_SUNSET:
                drawHighwayDetails(canvas, width, horizonY, offset, baseIndex);
                break;
            case COASTAL_DAY:
                drawCoastalDetails(canvas, width, horizonY, offset, baseIndex);
                break;
            case SNOW_AURORA:
                drawSnowDetails(canvas, width, horizonY, offset, baseIndex);
                break;
            case SUBWAY_GRAFFITI:
                drawSubwayDetails(canvas, width, horizonY, offset, baseIndex);
                break;
            case FOREST_DAWN:
                drawForestDetails(canvas, width, horizonY, offset, baseIndex);
                break;
            case FESTIVAL_NIGHT:
                drawFestivalDetails(canvas, width, horizonY, offset, baseIndex);
                break;
            case PRO_TRACK_DAY:
                break;
        }
    }

    private void drawTokyoDetails(Canvas canvas, float width, float horizonY, float offset, long baseIndex) {
        float spacing = 160.0f;
        for (int i = firstVisibleSegment(width, spacing); i < (width / spacing) + 2; i++) {
            float x = (i * spacing) - (offset % spacing);
            sceneryPaint.setColor(Color.parseColor("#101526"));
            canvas.drawRect(x, horizonY - 105, x + 105, horizonY, sceneryPaint);
            canvas.drawRect(x + 18, horizonY - 145, x + 68, horizonY - 105, sceneryPaint);
            linePaint.setColor(Color.parseColor("#00E5FF"));
            canvas.drawRect(x + 12, horizonY - 82, x + 92, horizonY - 72, linePaint);
            linePaint.setColor(Color.parseColor("#FF4081"));
            canvas.drawRect(x + 28, horizonY - 124, x + 86, horizonY - 114, linePaint);
            drawWindows(canvas, x + 10, horizonY - 62, 5, 2, 13, Color.parseColor("#FFE082"));
        }

        linePaint.setStrokeWidth(2.0f);
        linePaint.setColor(Color.argb(105, 160, 220, 255));
        for (float x = 12 - (offset * 0.7f); x < width; x += 29) {
            canvas.drawLine(x, 8, x - 13, horizonY - 4, linePaint);
        }
    }

    private void drawIndustrialDetails(Canvas canvas, float width, float horizonY, float offset, long baseIndex) {
        float spacing = 220.0f;
        for (int i = firstVisibleSegment(width, spacing); i < (width / spacing) + 2; i++) {
            float x = (i * spacing) - (offset % spacing);
            sceneryPaint.setColor(Color.parseColor("#263238"));
            canvas.drawRect(x, horizonY - 95, x + 110, horizonY, sceneryPaint);
            canvas.drawRect(x + 18, horizonY - 145, x + 34, horizonY - 95, sceneryPaint);
            canvas.drawCircle(x + 26, horizonY - 151, 18, sceneryPaint);
            linePaint.setColor(Color.parseColor("#546E7A"));
            linePaint.setStrokeWidth(3.0f);
            canvas.drawLine(x + 120, horizonY - 122, x + 188, horizonY - 122, linePaint);
            canvas.drawLine(x + 150, horizonY - 122, x + 150, horizonY, linePaint);
            canvas.drawLine(x + 120, horizonY - 122, x + 120, horizonY, linePaint);
            sceneryPaint.setColor(Color.parseColor("#D84315"));
            canvas.drawRect(x + 128, horizonY - 32, x + 168, horizonY - 8, sceneryPaint);
            sceneryPaint.setColor(Color.parseColor("#1565C0"));
            canvas.drawRect(x + 171, horizonY - 32, x + 211, horizonY - 8, sceneryPaint);
        }
    }

    private void drawDesertDetails(Canvas canvas, float width, float horizonY, float offset) {
        sceneryPaint.setColor(Color.parseColor("#795548"));
        Path mountains = new Path();
        mountains.moveTo(0, horizonY);
        mountains.lineTo(width * 0.18f, horizonY - 80);
        mountains.lineTo(width * 0.34f, horizonY);
        mountains.lineTo(width * 0.5f, horizonY - 58);
        mountains.lineTo(width * 0.72f, horizonY);
        mountains.lineTo(width * 0.88f, horizonY - 95);
        mountains.lineTo(width, horizonY);
        mountains.close();
        canvas.drawPath(mountains, sceneryPaint);

        float stationX = width * 0.66f - offset * 0.3f;
        sceneryPaint.setColor(Color.parseColor("#D84315"));
        canvas.drawRect(stationX, horizonY - 45, stationX + 95, horizonY - 8, sceneryPaint);
        canvas.drawRect(stationX + 14, horizonY - 62, stationX + 81, horizonY - 45, sceneryPaint);
        linePaint.setColor(Color.parseColor("#FFCC80"));
        canvas.drawRect(stationX + 33, horizonY - 36, stationX + 62, horizonY - 25, linePaint);
        linePaint.setColor(Color.parseColor("#ECEFF1"));
        canvas.drawRect(stationX + 4, horizonY - 8, stationX + 25, horizonY + 4, linePaint);
        canvas.drawRect(stationX + 69, horizonY - 8, stationX + 90, horizonY + 4, linePaint);
        lightPaint.setColor(Color.argb(85, 255, 224, 150));
        for (float x = -offset; x < width; x += 56) canvas.drawCircle(x, horizonY + 3, 8, lightPaint);
    }

    private void drawHighwayDetails(Canvas canvas, float width, float horizonY, float offset, long baseIndex) {
        linePaint.setColor(Color.argb(125, 255, 220, 145));
        linePaint.setStrokeWidth(8.0f);
        canvas.drawLine(width * 0.76f, 0, width * 0.67f, horizonY, linePaint);
        canvas.drawLine(width * 0.88f, 0, width * 0.72f, horizonY, linePaint);
        sceneryPaint.setColor(Color.parseColor("#37474F"));
        float spacing = 180.0f;
        for (int i = firstVisibleSegment(width, spacing); i < (width / spacing) + 2; i++) {
            float x = (i * spacing) - (offset % spacing);
            canvas.drawRect(x, horizonY - 12, x + 180, horizonY - 5, sceneryPaint);
            canvas.drawRect(x + 22, horizonY - 48, x + 29, horizonY - 5, sceneryPaint);
            canvas.drawRect(x + 145, horizonY - 48, x + 152, horizonY - 5, sceneryPaint);
        }
    }

    private void drawCoastalDetails(Canvas canvas, float width, float horizonY, float offset, long baseIndex) {
        sceneryPaint.setColor(Color.parseColor("#66BB6A"));
        float spacing = 170.0f;
        for (int i = firstVisibleSegment(width, spacing); i < (width / spacing) + 2; i++) {
            float x = (i * spacing) - (offset % spacing);
            canvas.drawRect(x + 42, horizonY - 76, x + 49, horizonY, sceneryPaint);
            linePaint.setColor(Color.parseColor("#81C784"));
            linePaint.setStrokeWidth(4.0f);
            canvas.drawLine(x + 45, horizonY - 72, x + 18, horizonY - 92, linePaint);
            canvas.drawLine(x + 45, horizonY - 72, x + 72, horizonY - 93, linePaint);
        }
        sceneryPaint.setColor(Color.parseColor("#795548"));
        canvas.drawRect(width * 0.04f, horizonY - 52, width * 0.24f, horizonY, sceneryPaint);
        linePaint.setColor(Color.parseColor("#78909C"));
        linePaint.setStrokeWidth(4.0f);
        canvas.drawLine(width * 0.26f, horizonY - 29, width * 0.74f, horizonY - 29, linePaint);
        for (float x = width * 0.28f; x < width * 0.74f; x += 34) {
            canvas.drawLine(x, horizonY - 29, x, horizonY, linePaint);
        }
    }

    private void drawSnowDetails(Canvas canvas, float width, float horizonY, float offset, long baseIndex) {
        linePaint.setStrokeWidth(2.0f);
        linePaint.setColor(Color.argb(180, 255, 255, 255));
        for (float x = 7 - offset; x < width; x += 31) {
            float y = 10 + Math.abs((int) (x * 7)) % 90;
            canvas.drawLine(x, y, x - 5, y + 9, linePaint);
        }
        linePaint.setColor(Color.parseColor("#455A64"));
        linePaint.setStrokeWidth(2.0f);
        canvas.drawLine(0, horizonY - 88, width, horizonY - 72, linePaint);
        for (float x = 20 - offset; x < width; x += 110) {
            canvas.drawLine(x, horizonY - 88, x + 75, horizonY - 72, linePaint);
        }
        sceneryPaint.setColor(Color.parseColor("#5D4037"));
        for (float x = 20 - offset; x < width; x += 130) {
            canvas.drawRect(x, horizonY - 45, x + 5, horizonY, sceneryPaint);
            lightPaint.setColor(Color.parseColor("#FFD54F"));
            canvas.drawRect(x - 4, horizonY - 54, x + 13, horizonY - 43, lightPaint);
        }
    }

    private void drawSubwayDetails(Canvas canvas, float width, float horizonY, float offset, long baseIndex) {
        sceneryPaint.setColor(Color.parseColor("#15191F"));
        canvas.drawRect(0, 0, 18, horizonY, sceneryPaint);
        canvas.drawRect(width - 18, 0, width, horizonY, sceneryPaint);
        linePaint.setStrokeWidth(3.0f);
        linePaint.setColor(Color.parseColor("#FFEE58"));
        for (float x = 28 - offset; x < width; x += 72) {
            canvas.drawLine(x, 12, x, horizonY - 34, linePaint);
            lightPaint.setColor(Color.argb(55, 255, 238, 88));
            canvas.drawCircle(x, 18, 20, lightPaint);
        }
        for (float x = 12 - offset; x < width; x += 145) {
            linePaint.setColor(Color.parseColor("#F06292"));
            canvas.drawRect(x, horizonY - 52, x + 45, horizonY - 45, linePaint);
            linePaint.setColor(Color.parseColor("#29B6F6"));
            canvas.drawRect(x + 55, horizonY - 39, x + 105, horizonY - 32, linePaint);
        }
    }

    private void drawForestDetails(Canvas canvas, float width, float horizonY, float offset, long baseIndex) {
        lightPaint.setColor(Color.argb(45, 255, 244, 180));
        for (float x = -90 - offset; x < width + 100; x += 125) {
            canvas.drawRect(x, 0, x + 13, horizonY, lightPaint);
        }
        lightPaint.setColor(Color.argb(35, 220, 240, 225));
        canvas.drawRect(0, horizonY - 30, width, horizonY, lightPaint);
        sceneryPaint.setColor(Color.parseColor("#1B4332"));
        for (float x = -offset; x < width + 130; x += 130) {
            canvas.drawRect(x + 47, horizonY - 110, x + 63, horizonY, sceneryPaint);
            canvas.drawCircle(x + 55, horizonY - 125, 42, sceneryPaint);
            canvas.drawCircle(x + 25, horizonY - 94, 28, sceneryPaint);
        }
    }

    private void drawFestivalDetails(Canvas canvas, float width, float horizonY, float offset, long baseIndex) {
        linePaint.setStrokeWidth(2.0f);
        linePaint.setColor(Color.parseColor("#F06292"));
        canvas.drawLine(0, horizonY - 82, width, horizonY - 82, linePaint);
        for (float x = 20 - offset; x < width; x += 54) {
            lightPaint.setColor((Math.abs((int) x) % 2 == 0)
                    ? Color.parseColor("#FFEB3B") : Color.parseColor("#00E5FF"));
            canvas.drawCircle(x, horizonY - 78, 6, lightPaint);
        }
        drawFirework(canvas, width * 0.24f, horizonY - 125, Color.parseColor("#FF4081"));
        drawFirework(canvas, width * 0.78f, horizonY - 145, Color.parseColor("#FFD740"));
        crowdPaint.setColor(Color.parseColor("#263238"));
        for (float x = -offset; x < width; x += 18) {
            canvas.drawCircle(x + 7, horizonY - 22, 5, crowdPaint);
            canvas.drawRect(x + 3, horizonY - 17, x + 11, horizonY, crowdPaint);
        }
    }

    private void drawWindows(Canvas canvas, float x, float y, int columns, int rows,
                             float spacing, int color) {
        linePaint.setColor(color);
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                canvas.drawRect(x + column * spacing, y + row * 16,
                        x + column * spacing + 7, y + row * 16 + 7, linePaint);
            }
        }
    }

    private void drawFirework(Canvas canvas, float x, float y, int color) {
        linePaint.setColor(color);
        linePaint.setStrokeWidth(2.0f);
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0;
            canvas.drawLine(x, y, x + (float) Math.cos(angle) * 27,
                    y + (float) Math.sin(angle) * 27, linePaint);
        }
        lightPaint.setColor(color);
        canvas.drawCircle(x, y, 4, lightPaint);
    }

        private void drawAssetStartSignal(Canvas canvas, float x, float y, RaceEngine raceEngine) {
        Bitmap trafficLight = loadAsset("traffic_light_frames");
        if (trafficLight == null) return;

        int lightState = raceEngine != null ? raceEngine.getTreeLightState() : 0;
        int frame = lightState == 5 ? 3 : Math.max(0, Math.min(4, lightState));
        int frameWidth = trafficLight.getWidth() / 5;
        Rect source = new Rect(frame * frameWidth, 0,
            (frame + 1) * frameWidth, trafficLight.getHeight());
        Rect destination = new Rect((int) x - 42, (int) y - 82,
            (int) x + 42, (int) y + 2);
        canvas.drawBitmap(trafficLight, source, destination, assetPaint);
    }

    private void drawAssetFinishLine(Canvas canvas, float x, float topY, float bottomY) {
        float stripeWidth = 42.0f;
        float cellWidth = stripeWidth * 0.5f;
        float cellHeight = 20.0f;

        finishLinePaint.setColor(Color.parseColor("#101820"));
        canvas.drawRect(x - 4.0f, topY, x + stripeWidth + 4.0f, bottomY, finishLinePaint);

        int row = 0;
        for (float y = topY; y < bottomY; y += cellHeight, row++) {
            float nextY = Math.min(y + cellHeight, bottomY);
            for (int column = 0; column < 2; column++) {
                boolean brightCell = ((row + column) & 1) == 0;
                finishLinePaint.setColor(brightCell
                    ? Color.parseColor("#FFEB3B")
                    : Color.parseColor("#171717"));
                float left = x + column * cellWidth;
                canvas.drawRect(left, y, left + cellWidth, nextY, finishLinePaint);
            }
        }

        finishLinePaint.setColor(Color.parseColor("#00E5FF"));
        canvas.drawRect(x - 4.0f, topY, x - 1.0f, bottomY, finishLinePaint);
        finishLinePaint.setColor(Color.parseColor("#FF4081"));
        canvas.drawRect(x + stripeWidth + 1.0f, topY, x + stripeWidth + 4.0f,
            bottomY, finishLinePaint);
    }
}
