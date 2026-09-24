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
import android.graphics.Matrix;
import android.graphics.Shader;
import com.dragracing.game.engine.RaceEngine;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TrackRenderer {
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
        sceneryPaint.setColor(Color.parseColor("#303B42"));
        crowdPaint.setColor(Color.parseColor("#3C474D"));
        roadPaint.setColor(Color.parseColor("#30383D"));
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

    private void updateEnvironmentPaints() {
        switch (environmentType) {
            case TOKYO_NIGHT:
            case FESTIVAL_NIGHT:
                sceneryPaint.setColor(Color.parseColor("#25204A"));
                roadPaint.setColor(Color.parseColor("#20252D"));
                break;
            case DESERT_SUNSET:
                sceneryPaint.setColor(Color.parseColor("#5D4037"));
                roadPaint.setColor(Color.parseColor("#263238"));
                break;
            case ABANDONED_INDUSTRIAL:
            case SUBWAY_GRAFFITI:
                sceneryPaint.setColor(Color.parseColor("#263238"));
                roadPaint.setColor(Color.parseColor("#121212"));
                break;
            case HIGHWAY_SUNSET:
                sceneryPaint.setColor(Color.parseColor("#455A64"));
                roadPaint.setColor(Color.parseColor("#263238"));
                break;
            case COASTAL_DAY:
                sceneryPaint.setColor(Color.parseColor("#2E7D78"));
                roadPaint.setColor(Color.parseColor("#37474F"));
                break;
            case SNOW_AURORA:
                sceneryPaint.setColor(Color.parseColor("#B0BEC5"));
                roadPaint.setColor(Color.parseColor("#263238"));
                break;
            case FOREST_DAWN:
                sceneryPaint.setColor(Color.parseColor("#285943"));
                roadPaint.setColor(Color.parseColor("#30383D"));
                break;
            case PRO_TRACK_DAY:
                sceneryPaint.setColor(Color.parseColor("#78909C"));
                roadPaint.setColor(Color.parseColor("#37474F"));
                break;
        }
    }

    public void render(Canvas canvas, float width, float height, double viewDistance, RaceEngine raceEngine) {
        // Keep the track over the grey lower section of the background.
        float trackAreaBottom = height * 0.78f;
        float horizonY = trackAreaBottom * 0.62f;
        float trackHeight = trackAreaBottom - horizonY;
        float laneHeight = trackHeight / 2.0f;

        // Start line anchor
        float startX = width * 0.25f;
        float barrierY = horizonY;

        drawAssetTrack(canvas, width, height, horizonY, trackAreaBottom, startX, viewDistance);
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

        drawTrackObjects(canvas, width, horizonY, trackAreaBottom, viewDistance);

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
                       float horizonY, float trackAreaBottom, float startX,
                       double viewDistance) {
        Bitmap themeBackground = loadAsset(environmentType.backgroundAsset());
        Bitmap baseTileset = loadAsset("tilesets");
        Bitmap themeTileset = loadAsset(environmentType.themeTilesetAsset());
            if (themeBackground == null || baseTileset == null || themeTileset == null) {
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
        }

        // The first road tile is one half of the strip. Shrink it to one lane,
        // then mirror it above the divider to build the complete two-lane road.
        int sourceTileWidth = Math.min(128, baseTileset.getWidth());
        int sourceTileHeight = Math.min(96, baseTileset.getHeight());
        Rect sourceRoadTile = new Rect(0, 395, sourceTileWidth, 395 + sourceTileHeight);
        float roadTop = horizonY + 10.0f;
        float dividerY = roadTop + (trackAreaBottom - roadTop) * 0.5f;
        float laneHeight = dividerY - horizonY;
        float destinationTileWidth = Math.min(128.0f, width);
        
        float roadOffset = (float) ((viewDistance * 35.0) % destinationTileWidth);

        for (float x = -roadOffset; x < width; x += destinationTileWidth) {
            RectF lowerRoadTile = new RectF(x, dividerY,
                x + destinationTileWidth,
                trackAreaBottom);
            canvas.drawBitmap(baseTileset, sourceRoadTile, lowerRoadTile, assetPaint);

            canvas.save();
            Matrix mirror = new Matrix();
            mirror.setScale(1.0f, -1.0f, x + destinationTileWidth * 0.5f,
                roadTop + (dividerY - roadTop) * 0.5f);
            canvas.concat(mirror);
            RectF upperRoadTile = new RectF(x, roadTop,
                x + destinationTileWidth,
                dividerY);
            canvas.drawBitmap(baseTileset, sourceRoadTile, upperRoadTile, assetPaint);
            canvas.restore();
        }

        // The small white road-marking tile is positioned across the center seam.
        int sourceDividerWidth = Math.min(64, baseTileset.getWidth());
        int sourceDividerTop = Math.min(220, Math.max(0, baseTileset.getHeight() - 1));
        int sourceDividerBottom = Math.min(sourceDividerTop + 24, baseTileset.getHeight());
        Rect sourceDivider = new Rect(0, sourceDividerTop,
            sourceDividerWidth, sourceDividerBottom);
            
        float markerSpacing = destinationTileWidth * 1.5f;
        float markerOffset = (float) ((viewDistance * 35.0) % markerSpacing);
        
        for (float x = -markerOffset; x < width; x += markerSpacing) {
            RectF destinationDivider = new RectF(x, dividerY - 7,
                x + destinationTileWidth * 0.5f, dividerY + 7);
            canvas.drawBitmap(baseTileset, sourceDivider, destinationDivider, assetPaint);
        }

        // The original tileset supplies the universal start signal for every track.
        Rect startSignal = new Rect(0, 0, Math.min(130, baseTileset.getWidth()),
            Math.min(230, baseTileset.getHeight()));
        Rect startSignalDestination = new Rect((int) startX - 18, (int) horizonY - 78,
            (int) startX + 26, (int) horizonY + 2);
        canvas.drawBitmap(baseTileset, startSignal, startSignalDestination, assetPaint);

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

        private void drawCoastalSeaAndSky(Canvas canvas, float width, float horizonY,
                          double viewDistance, Bitmap coastalTileset) {
        double worldX = viewDistance * 35.0;
        float boatSpacing = 330.0f;
        float boatOffset = (float) (worldX * 0.42 % boatSpacing);
        Rect[] boatSources = {
            new Rect(300, 285, 550, 485),
            new Rect(555, 285, 810, 485),
            new Rect(780, 285, 1024, 485)
        };
        for (int i = -1; i < (width / boatSpacing) + 2; i++) {
            int boatIndex = Math.floorMod(i, boatSources.length);
            float x = i * boatSpacing - boatOffset + width * 0.18f;
            Rect source = boatSources[boatIndex];
            float boatHeight = 42.0f;
            float boatWidth = boatHeight * source.width() / source.height();
            RectF destination = new RectF(x, horizonY - 92.0f,
                x + boatWidth, horizonY - 92.0f + boatHeight);
            canvas.drawBitmap(coastalTileset, source, destination, assetPaint);
        }

        Rect gullSource = new Rect(500, 690, 730, 820);
        float gullSpacing = 250.0f;
        float gullOffset = (float) (worldX * 0.18 % gullSpacing);
        for (int i = -1; i < (width / gullSpacing) + 2; i++) {
            float x = i * gullSpacing - gullOffset + 90.0f;
            RectF destination = new RectF(x, horizonY - 180.0f,
                x + 62.0f, horizonY - 145.0f);
            canvas.drawBitmap(coastalTileset, gullSource, destination, assetPaint);
        }
        }

        private void drawTrackObjects(Canvas canvas, float width, float horizonY,
                          float trackAreaBottom, double viewDistance) {
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
            drawCoastalRoadsideObjects(canvas, width, horizonY, viewDistance, tileset);
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

        for (int i = -1; i < (width / objectSpacing) + 2; i++) {
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

        for (int i = -1; i < (width / objectSpacing) + 2; i++) {
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

        for (int i = -1; i < (width / spacing) + 2; i++) {
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
        for (int i = -1; i < (width / spacing) + 2; i++) {
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
        
        for (int i = -1; i < (width / spacing) + 2; i++) {
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

        private void drawCoastalRoadsideObjects(Canvas canvas, float width, float horizonY,
                            double viewDistance, Bitmap coastalTileset) {
        double worldX = viewDistance * 35.0;
        float spacing = 190.0f;
        float offset = (float) (worldX % spacing);
        long baseIndex = (long) Math.floor(worldX / spacing);

        Rect[] roadsideSources = {
            new Rect(0, 0, 345, 315),       // palm cluster
            new Rect(555, 0, 810, 275),     // tall palm
            new Rect(225, 520, 455, 705),   // small sand castle
            new Rect(390, 510, 625, 705),   // large sand castle
            new Rect(830, 785, 1024, 1024)  // clam shack
        };
        float[] heights = { 94.0f, 88.0f, 52.0f, 62.0f, 58.0f };

        for (int i = -1; i < (width / spacing) + 2; i++) {
            long absoluteIndex = baseIndex + i;
            int index = Math.floorMod(absoluteIndex, roadsideSources.length);
            Rect source = roadsideSources[index];
            float objectHeight = heights[index];
            float objectWidth = objectHeight * source.width() / source.height();
            float x = i * spacing - offset;
            float objectY = horizonY - objectHeight + 4.0f;
            RectF destination = new RectF(x, objectY, x + objectWidth, horizonY + 4.0f);
            canvas.drawBitmap(coastalTileset, source, destination, assetPaint);
        }

        drawCoastalSigns(canvas, width, horizonY, worldX, coastalTileset);
        }

        private void drawCoastalSigns(Canvas canvas, float width, float horizonY,
                          double worldX, Bitmap coastalTileset) {
        Bitmap baseTileset = loadAsset("tilesets");
        Rect coastalSign = new Rect(0, 735, 250, 1024);
        Rect speedSign = new Rect(270, 735, 380, 925);
        float spacing = 360.0f;
        float offset = (float) (worldX * 0.82 % spacing);
        long baseIndex = (long) Math.floor(worldX * 0.82 / spacing);
        for (int i = -1; i < (width / spacing) + 2; i++) {
            long absoluteIndex = baseIndex + i;
            float x = i * spacing - offset + width * 0.18f;
            Rect source = Math.floorMod(absoluteIndex, 2) == 0 ? coastalSign : speedSign;
            Bitmap bitmap = Math.floorMod(absoluteIndex, 2) == 0 ? coastalTileset : baseTileset;
            if (bitmap == null) continue;
            float height = 62.0f;
            float objectWidth = height * source.width() / source.height();
            RectF destination = new RectF(x, horizonY - height + 4.0f,
                x + objectWidth, horizonY + 4.0f);
            canvas.drawBitmap(bitmap, source, destination, assetPaint);
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

        for (int i = -1; i < (width / spacing) + 2; i++) {
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

        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId);
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
                drawProTrackDetails(canvas, width, horizonY, offset, baseIndex);
                break;
        }
    }

    private void drawTokyoDetails(Canvas canvas, float width, float horizonY, float offset, long baseIndex) {
        float spacing = 160.0f;
        for (int i = -1; i < (width / spacing) + 2; i++) {
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
        for (int i = -1; i < (width / spacing) + 2; i++) {
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
        for (int i = -1; i < (width / spacing) + 2; i++) {
            float x = (i * spacing) - (offset % spacing);
            canvas.drawRect(x, horizonY - 12, x + 180, horizonY - 5, sceneryPaint);
            canvas.drawRect(x + 22, horizonY - 48, x + 29, horizonY - 5, sceneryPaint);
            canvas.drawRect(x + 145, horizonY - 48, x + 152, horizonY - 5, sceneryPaint);
        }
    }

    private void drawCoastalDetails(Canvas canvas, float width, float horizonY, float offset, long baseIndex) {
        sceneryPaint.setColor(Color.parseColor("#66BB6A"));
        float spacing = 170.0f;
        for (int i = -1; i < (width / spacing) + 2; i++) {
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

    private void drawProTrackDetails(Canvas canvas, float width, float horizonY, float offset, long baseIndex) {
        crowdPaint.setColor(Color.parseColor("#455A64"));
        canvas.drawRect(0, horizonY - 72, width, horizonY, crowdPaint);
        crowdPaint.setColor(Color.parseColor("#90A4AE"));
        for (float x = -offset; x < width; x += 24) {
            canvas.drawRect(x, horizonY - 66, x + 15, horizonY - 56, crowdPaint);
            canvas.drawRect(x + 2, horizonY - 53, x + 13, horizonY - 46, crowdPaint);
        }
        linePaint.setColor(Color.parseColor("#ECEFF1"));
        linePaint.setStrokeWidth(3.0f);
        canvas.drawLine(0, horizonY - 38, width, horizonY - 38, linePaint);
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
        Bitmap tileset = loadAsset("tilesets");
        if (tileset == null) return;
        Rect source = new Rect(0, Math.min(220, tileset.getHeight() - 1),
                Math.min(64, tileset.getWidth()), Math.min(244, tileset.getHeight()));
        Rect destination = new Rect((int) x, (int) topY, (int) x + 24, (int) bottomY);
        canvas.drawBitmap(tileset, source, destination, assetPaint);
    }
}
