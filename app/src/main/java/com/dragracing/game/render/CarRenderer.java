package com.dragracing.game.render;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.CornerPathEffect;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import com.dragracing.game.data.Car;
import com.dragracing.game.engine.CarPhysics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CarRenderer {
    private final Context context;
    private final Map<String, Bitmap> bodyBitmaps = new HashMap<>();
    private final Map<String, Bitmap> tintedBodyBitmaps = new HashMap<>();
    private final Map<String, Bitmap> tyreBitmaps = new HashMap<>();

    private static final Map<String, WheelSpec> WHEEL_SPECS = new HashMap<>();
    private static class WheelSpec {
        final float rearX, frontX, centerY, radius;
        final int spokes, rimColor;

        WheelSpec(float rearX, float frontX, float centerY, float radius, int spokes, int rimColor) {
            this.rearX = rearX;
            this.frontX = frontX;
            this.centerY = centerY;
            this.radius = radius;
            this.spokes = spokes;
            this.rimColor = rimColor;
        }
    }

    static {
        addWheel("alfa_romeo_4c", .354f, .651f, .837f, .065f, 5, 0xFFBDBDBD);
        addWheel("alpine_a110", .265f, .713f, .814f, .065f, 5, 0xFFB0BEC5);
        addWheel("aston_martin_v8_vantage", .337f, .704f, .811f, .065f, 10, 0xFF9E9E9E);
        addWheel("audi_r8_v10_plus", .342f, .665f, .818f, .065f, 10, 0xFFB0BEC5);
        addWheel("bmw_m3", .338f, .691f, .807f, .065f, 5, 0xFFBDBDBD);
        addWheel("bmw_m4", .347f, .684f, .813f, .065f, 5, 0xFFBDBDBD);
        addWheel("bmw_m4_dtm_champion_edition", .261f, .694f, .784f, .064f, 10, 0xFFCFD8DC);
        addWheel("bmw_m6", .222f, .720f, .790f, .064f, 5, 0xFF9E9E9E);
        addWheel("ferrari_488_gtb", .328f, .672f, .817f, .064f, 5, 0xFFBDBDBD);
        addWheel("honda_civic_type_r", .356f, .674f, .809f, .064f, 5, 0xFFCFD8DC);
        addWheel("honda_nsx", .254f, .775f, .802f, .097f, 5, 0xFFB0BEC5);
        addWheel("honda_s2000", .267f, .684f, .830f, .065f, 5, 0xFFBDBDBD);
        addWheel("lamborghini_huracan", .332f, .665f, .836f, .064f, 10, 0xFFCFD8DC);
        addWheel("lexus_lc500", .334f, .713f, .802f, .064f, 10, 0xFFB0BEC5);
        addWheel("lotus_emira", .230f, .711f, .782f, .064f, 5, 0xFFBDBDBD);
        addWheel("lotus_exige_s", .327f, .701f, .802f, .064f, 5, 0xFFB0BEC5);
        addWheel("mercedes_amg_gt_r", .334f, .701f, .811f, .065f, 10, 0xFFBDBDBD);
        addWheel("mercedes_amg_gt_s", .353f, .687f, .844f, .064f, 10, 0xFFCFD8DC);
        addWheel("nissan_gtr_nismo", .269f, .673f, .830f, .065f, 5, 0xFFB0BEC5);
        addWheel("porsche_718_cayman_gt4", .239f, .679f, .814f, .065f, 10, 0xFFBDBDBD);
        addWheel("porsche_718_cayman_gts", .305f, .688f, .803f, .080f, 5, 0xFFCFD8DC);
        addWheel("porsche_718_cayman_s", .354f, .676f, .830f, .065f, 5, 0xFFBDBDBD);
        addWheel("porsche_911_carrera_gts", .262f, .673f, .833f, .048f, 5, 0xFFCFD8DC);
        addWheel("porsche_911_gt3", .354f, .664f, .822f, .065f, 10, 0xFFCFD8DC);
        addWheel("toyota_86gt", .264f, .710f, .807f, .065f, 5, 0xFFBDBDBD);
        addWheel("toyota_gr86", .278f, .710f, .799f, .065f, 5, 0xFFBDBDBD);
        addWheel("toyota_gr_supra_rz", .316f, .719f, .811f, .065f, 10, 0xFFCFD8DC);
    }

    private static void addWheel(String id, float rearX, float frontX, float centerY,
                                 float radius, int spokes, int rimColor) {
        WHEEL_SPECS.put(id, new WheelSpec(rearX, frontX, centerY, radius, spokes, rimColor));
    }

    // ── Paints ────────────────────────────────────────────────────────────────
    private final Paint bodyPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint body2Paint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glassPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glassHighlight = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillarPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tirePaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rimPaint       = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint spokePaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint caliperPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint brakePaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lightPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint detailPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint exhaustPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint flamePaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint smokePaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint imagePaint     = new Paint();


    private final Path bodyPath    = new Path();
    private final Path glassPath   = new Path();
    private final Path spoilerPath = new Path();
    private final Path tmpPath     = new Path();

    // Gradient cache
    private int   cachedColor = 0;
    private float cachedScale = 0;
    private float cachedX     = 0;
    private float cachedY     = 0;
    private LinearGradient bodyGradient;

    private static class Particle {
        float x, y, vx, vy, size, alpha;
        int color;
    }
    private final List<Particle> smokeParticles = new ArrayList<>();
    private final List<Particle> flameParticles = new ArrayList<>();

    public CarRenderer(Context context) {
        this.context = context.getApplicationContext();
        shadowPaint.setColor(Color.argb(120, 0, 0, 0));
        shadowPaint.setStyle(Paint.Style.FILL);
        glassPaint.setColor(Color.argb(210, 18, 32, 48));
        glassPaint.setStyle(Paint.Style.FILL);
        glassHighlight.setColor(Color.argb(55, 255, 255, 255));
        glassHighlight.setStyle(Paint.Style.FILL);
        pillarPaint.setColor(Color.parseColor("#1A1A1A"));
        pillarPaint.setStyle(Paint.Style.FILL);
        tirePaint.setColor(Color.parseColor("#111111"));
        tirePaint.setStyle(Paint.Style.FILL);
        rimPaint.setColor(Color.parseColor("#CCCCCC"));
        rimPaint.setStyle(Paint.Style.FILL);
        spokePaint.setColor(Color.parseColor("#BBBBBB"));
        spokePaint.setStyle(Paint.Style.STROKE);
        caliperPaint.setColor(Color.parseColor("#D32F2F"));
        caliperPaint.setStyle(Paint.Style.FILL);
        brakePaint.setColor(Color.parseColor("#505050"));
        brakePaint.setStyle(Paint.Style.FILL);
        exhaustPaint.setColor(Color.parseColor("#444444"));
        exhaustPaint.setStyle(Paint.Style.FILL);
        detailPaint.setColor(Color.parseColor("#1A1A1A"));
        detailPaint.setStyle(Paint.Style.STROKE);
        detailPaint.setPathEffect(new CornerPathEffect(3f));
        highlightPaint.setColor(Color.argb(75, 255, 255, 255));
        highlightPaint.setStyle(Paint.Style.FILL);
        body2Paint.setStyle(Paint.Style.FILL);
        bodyPaint.setStyle(Paint.Style.FILL);
        lightPaint.setStyle(Paint.Style.FILL);
        imagePaint.setFilterBitmap(false);
    }

    // =========================================================================
    // Public entry point
    // =========================================================================
    public void render(Canvas canvas, CarPhysics physics, float x, float y,
                       float scale, boolean isOpponent) {
        if (physics == null) return;

        Car car       = physics.getCar();
        String carId  = car.getId();
        int baseColor = car.getColor();
        Car.BodyType bodyType = car.getBodyType();

        float rotDeg = (float) Math.toDegrees(physics.getDistance() / 0.32);
        if (physics.isWheelSpinning()) {
            rotDeg += (float) ((System.currentTimeMillis() % 10000) * 1.5);
        }
        if (drawCarImage(canvas, car, x, y, scale, rotDeg, physics.isHasLaunched())) {
            spawnParticles(physics, x, y, scale);
            renderParticles(canvas, 0.016f);
            return;
        }

        float bodyH = 55 * scale;
        if (bodyGradient == null || cachedColor != baseColor
                || cachedScale != scale || cachedX != x || cachedY != y) {
            cachedColor = baseColor; cachedScale = scale; cachedX = x; cachedY = y;
            bodyGradient = new LinearGradient(x, y, x, y + bodyH,
                    blendColor(baseColor, 0xFFFFFFFF, 0.22f),
                    darken(baseColor, 0.45f), Shader.TileMode.CLAMP);
        }
        bodyPaint.setShader(bodyGradient);
        body2Paint.setColor(darken(baseColor, 0.75f));

        // Ground shadow
        float sw = getShadowWidth(carId, bodyType, scale);
        canvas.drawOval(x - 4 * scale, y + bodyH,
                x + sw, y + bodyH + 8 * scale, shadowPaint);

        canvas.save();
        canvas.rotate(physics.getSuspensionPitch(), x + 40 * scale, y + bodyH - 15 * scale);

        switch (carId) {
            case "car_starter":       drawCivicTypeR(canvas, x, y, scale, baseColor, rotDeg); break;
            case "car_muscle":        drawMustangGT500(canvas, x, y, scale, baseColor, rotDeg); break;
            case "car_classic":       drawChargerHellcat(canvas, x, y, scale, baseColor, rotDeg); break;
            case "car_jdm":           drawSkylineGTR(canvas, x, y, scale, baseColor, rotDeg); break;
            case "car_euro":          drawBMWM3(canvas, x, y, scale, baseColor, rotDeg); break;
            case "car_super":         drawLamborghiniAventador(canvas, x, y, scale, baseColor, rotDeg); break;
            case "car_hyper":         drawBugattiChiron(canvas, x, y, scale, baseColor, rotDeg); break;
            case "car_electric":      drawRimacNevera(canvas, x, y, scale, baseColor, rotDeg); break;
            case "car_economy":       drawToyotaCorolla(canvas, x, y, scale, baseColor, rotDeg); break;
            case "car_suv":           drawJeepWrangler(canvas, x, y, scale, baseColor, rotDeg); break;
            case "car_classic_sport": drawCorvetteC1(canvas, x, y, scale, baseColor, rotDeg); break;
            case "car_modern_super":  drawMcLaren720S(canvas, x, y, scale, baseColor, rotDeg); break;
            case "car_apollo":        drawApolloSport(canvas, x, y, scale, baseColor, rotDeg); break;
            default:
                if (bodyType == Car.BodyType.DRAGSTER)
                    drawDragster(canvas, x, y, scale, baseColor, rotDeg);
                else
                    drawGenericCar(canvas, x, y, scale, baseColor, rotDeg);
                break;
        }

            drawRealismDetails(canvas, carId, x, y, scale, baseColor);

        canvas.restore();
        spawnParticles(physics, x, y, scale);
        renderParticles(canvas, 0.016f);
    }

    // =========================================================================
    // 1. Honda Civic Type R
    // =========================================================================
    private void drawCivicTypeR(Canvas c, float x, float y, float s, int col, float rot) {
        float h = 50 * s, rWx = x + 30*s, fWx = x + 128*s, wR = 16*s;
        bodyPath.reset();
        bodyPath.moveTo(x+2*s,   y+h-6*s);
        bodyPath.lineTo(x+2*s,   y+h-28*s);
        bodyPath.lineTo(x+22*s,  y+h-30*s);
        bodyPath.quadTo(x+45*s,  y+2*s, x+70*s, y+2*s);
        bodyPath.lineTo(x+100*s, y+2*s);
        bodyPath.quadTo(x+118*s, y+2*s, x+130*s, y+h-18*s);
        bodyPath.lineTo(x+150*s, y+h-16*s);
        bodyPath.quadTo(x+155*s, y+h-10*s, x+152*s, y+h-6*s);
        bodyPath.close();
        c.drawPath(bodyPath, bodyPaint);
        // Roof highlight
        tmpPath.reset();
        tmpPath.moveTo(x+48*s, y+4*s); tmpPath.lineTo(x+95*s, y+4*s);
        tmpPath.lineTo(x+92*s, y+10*s); tmpPath.lineTo(x+51*s, y+10*s); tmpPath.close();
        c.drawPath(tmpPath, highlightPaint);
        // Panel crease
        detailPaint.setStrokeWidth(1.5f*s);
        c.drawLine(x+22*s, y+h-20*s, x+138*s, y+h-15*s, detailPaint);
        // Glass
        glassPath.reset();
        glassPath.moveTo(x+48*s,  y+4*s);  glassPath.lineTo(x+96*s,  y+4*s);
        glassPath.lineTo(x+118*s, y+h-20*s); glassPath.lineTo(x+32*s, y+h-22*s); glassPath.close();
        c.drawPath(glassPath, glassPaint);
        // Pillars
        pillarPaint.setStyle(Paint.Style.STROKE); pillarPaint.setStrokeWidth(3.5f*s);
        c.drawLine(x+48*s, y+4*s, x+32*s, y+h-22*s, pillarPaint);
        c.drawLine(x+96*s, y+4*s, x+118*s, y+h-20*s, pillarPaint);
        c.drawLine(x+72*s, y+3*s, x+72*s, y+h-21*s, pillarPaint);
        pillarPaint.setStyle(Paint.Style.FILL);
        drawMirror(c, x+118*s, y+h-25*s, s);
        drawSpoiler(c, x+2*s, y+h-42*s, 28*s, 6*s, s, col);
        drawLEDHeadlight(c, x+136*s, y+h-22*s, 12*s, 6*s, s);
        drawLEDTaillight(c, x+2*s, y+h-26*s, 4*s, 10*s, s);
        drawExhaust(c, x+8*s, y+h-9*s, s); drawExhaust(c, x+14*s, y+h-9*s, s);
        // Front lip
        body2Paint.setColor(darken(col, 0.5f));
        c.drawRoundRect(x+140*s, y+h-10*s, x+154*s, y+h-6*s, 2*s, 2*s, body2Paint);
        drawDetailedWheel(c, rWx, y+h, wR, rot, 5, col);
        drawDetailedWheel(c, fWx, y+h, wR, rot, 5, col);
    }

    // =========================================================================
    // 2. Ford Mustang GT500
    // =========================================================================
    private void drawMustangGT500(Canvas c, float x, float y, float s, int col, float rot) {
        float h = 48*s, rWx = x+36*s, fWx = x+148*s, wR = 18*s;
        bodyPath.reset();
        bodyPath.moveTo(x+2*s, y+h-6*s); bodyPath.lineTo(x+2*s, y+h-24*s);
        bodyPath.lineTo(x+28*s, y+h-26*s); bodyPath.lineTo(x+52*s, y+h-28*s);
        bodyPath.quadTo(x+62*s, y+5*s, x+80*s, y+4*s);
        bodyPath.lineTo(x+115*s, y+4*s);
        bodyPath.quadTo(x+128*s, y+4*s, x+138*s, y+h-20*s);
        bodyPath.lineTo(x+166*s, y+h-18*s);
        bodyPath.quadTo(x+172*s, y+h-12*s, x+170*s, y+h-6*s); bodyPath.close();
        c.drawPath(bodyPath, bodyPaint);
        // Hood power bulge
        body2Paint.setColor(darken(col, 0.85f));
        tmpPath.reset();
        tmpPath.moveTo(x+118*s, y+h-20*s); tmpPath.lineTo(x+162*s, y+h-20*s);
        tmpPath.quadTo(x+165*s, y+h-15*s, x+166*s, y+h-18*s);
        tmpPath.lineTo(x+122*s, y+h-17*s); tmpPath.close();
        c.drawPath(tmpPath, body2Paint);
        // Roof highlight
        tmpPath.reset();
        tmpPath.moveTo(x+64*s, y+6*s); tmpPath.lineTo(x+110*s, y+6*s);
        tmpPath.lineTo(x+107*s, y+13*s); tmpPath.lineTo(x+67*s, y+13*s); tmpPath.close();
        c.drawPath(tmpPath, highlightPaint);
        detailPaint.setStrokeWidth(2*s);
        c.drawLine(x+10*s, y+h-18*s, x+155*s, y+h-14*s, detailPaint);
        // Glass
        glassPath.reset();
        glassPath.moveTo(x+64*s, y+6*s); glassPath.lineTo(x+113*s, y+6*s);
        glassPath.lineTo(x+135*s, y+h-22*s); glassPath.lineTo(x+54*s, y+h-25*s); glassPath.close();
        c.drawPath(glassPath, glassPaint);
        pillarPaint.setStyle(Paint.Style.STROKE); pillarPaint.setStrokeWidth(4*s);
        c.drawLine(x+64*s, y+6*s, x+54*s, y+h-25*s, pillarPaint);
        c.drawLine(x+113*s, y+6*s, x+135*s, y+h-22*s, pillarPaint);
        c.drawLine(x+88*s, y+5*s, x+90*s, y+h-23*s, pillarPaint);
        pillarPaint.setStyle(Paint.Style.FILL);
        drawMirror(c, x+133*s, y+h-27*s, s);
        drawSpoiler(c, x+4*s, y+h-38*s, 32*s, 7*s, s, col);
        drawGrilleBars(c, x+152*s, y+h-20*s, 14*s, 12*s, s, 3);
        drawLEDHeadlight(c, x+150*s, y+h-24*s, 14*s, 7*s, s);
        drawLEDTaillight(c, x+2*s, y+h-28*s, 4*s, 14*s, s);
        drawExhaust(c, x+8*s, y+h-10*s, s); drawExhaust(c, x+15*s, y+h-10*s, s); drawExhaust(c, x+22*s, y+h-10*s, s);
        body2Paint.setColor(darken(col, 0.4f));
        c.drawRoundRect(x+155*s, y+h-9*s, x+170*s, y+h-5*s, 2*s, 2*s, body2Paint);
        drawDetailedWheel(c, rWx, y+h, wR, rot, 10, col);
        drawDetailedWheel(c, fWx, y+h, wR, rot, 10, col);
    }

    // =========================================================================
    // 3. Dodge Charger SRT Hellcat
    // =========================================================================
    private void drawChargerHellcat(Canvas c, float x, float y, float s, int col, float rot) {
        float h = 46*s, rWx = x+38*s, fWx = x+156*s, wR = 18*s;
        bodyPath.reset();
        bodyPath.moveTo(x+2*s, y+h-6*s); bodyPath.lineTo(x, y+h-24*s);
        bodyPath.lineTo(x+42*s, y+h-26*s); bodyPath.lineTo(x+60*s, y+7*s);
        bodyPath.lineTo(x+120*s, y+7*s); bodyPath.lineTo(x+136*s, y+h-26*s);
        bodyPath.lineTo(x+175*s, y+h-24*s); bodyPath.lineTo(x+178*s, y+h-6*s); bodyPath.close();
        c.drawPath(bodyPath, bodyPaint);
        body2Paint.setColor(darken(col, 0.92f));
        c.drawRect(x+60*s, y+7*s, x+120*s, y+14*s, body2Paint);
        detailPaint.setStrokeWidth(2.5f*s);
        c.drawLine(x+5*s, y+h-20*s, x+172*s, y+h-17*s, detailPaint);
        glassPath.reset();
        glassPath.moveTo(x+62*s, y+9*s); glassPath.lineTo(x+118*s, y+9*s);
        glassPath.lineTo(x+133*s, y+h-26*s); glassPath.lineTo(x+47*s, y+h-28*s); glassPath.close();
        c.drawPath(glassPath, glassPaint);
        pillarPaint.setStyle(Paint.Style.STROKE); pillarPaint.setStrokeWidth(5*s);
        c.drawLine(x+62*s, y+9*s, x+47*s, y+h-28*s, pillarPaint);
        c.drawLine(x+118*s, y+9*s, x+133*s, y+h-26*s, pillarPaint);
        pillarPaint.setStyle(Paint.Style.FILL);
        drawMirror(c, x+132*s, y+h-30*s, s);
        body2Paint.setColor(darken(col, 0.6f));
        c.drawRoundRect(x+2*s, y+h-30*s, x+40*s, y+h-26*s, 2*s, 2*s, body2Paint);
        drawCircleHeadlight(c, x+162*s, y+h-22*s, 5*s);
        drawCircleHeadlight(c, x+162*s, y+h-14*s, 5*s);
        lightPaint.setColor(Color.parseColor("#D50000"));
        c.drawRoundRect(x+1*s, y+h-30*s, x+5*s, y+h-8*s, 2*s, 2*s, lightPaint);
        drawGrilleBars(c, x+162*s, y+h-22*s, 14*s, 14*s, s, 4);
        drawExhaust(c, x+83*s, y+h-7*s, s); drawExhaust(c, x+90*s, y+h-7*s, s);
        drawDetailedWheel(c, rWx, y+h, wR, rot, 5, col);
        drawDetailedWheel(c, fWx, y+h, wR, rot, 5, col);
    }

    // =========================================================================
    // 4. Nissan Skyline GT-R R34
    // =========================================================================
    private void drawSkylineGTR(Canvas c, float x, float y, float s, int col, float rot) {
        float h = 47*s, rWx = x+32*s, fWx = x+138*s, wR = 17*s;
        bodyPath.reset();
        bodyPath.moveTo(x+2*s, y+h-6*s); bodyPath.lineTo(x+2*s, y+h-26*s);
        bodyPath.lineTo(x+38*s, y+h-28*s); bodyPath.lineTo(x+55*s, y+5*s);
        bodyPath.lineTo(x+108*s, y+5*s); bodyPath.lineTo(x+125*s, y+h-22*s);
        bodyPath.lineTo(x+160*s, y+h-20*s);
        bodyPath.quadTo(x+164*s, y+h-10*s, x+162*s, y+h-6*s); bodyPath.close();
        c.drawPath(bodyPath, bodyPaint);
        tmpPath.reset();
        tmpPath.moveTo(x+57*s, y+7*s); tmpPath.lineTo(x+105*s, y+7*s);
        tmpPath.lineTo(x+102*s, y+14*s); tmpPath.lineTo(x+60*s, y+14*s); tmpPath.close();
        c.drawPath(tmpPath, highlightPaint);
        detailPaint.setStrokeWidth(2*s);
        c.drawLine(x+8*s, y+h-20*s, x+150*s, y+h-16*s, detailPaint);
        glassPath.reset();
        glassPath.moveTo(x+57*s, y+7*s); glassPath.lineTo(x+106*s, y+7*s);
        glassPath.lineTo(x+122*s, y+h-22*s); glassPath.lineTo(x+46*s, y+h-24*s); glassPath.close();
        c.drawPath(glassPath, glassPaint);
        pillarPaint.setStyle(Paint.Style.STROKE); pillarPaint.setStrokeWidth(3.5f*s);
        c.drawLine(x+57*s, y+7*s, x+46*s, y+h-24*s, pillarPaint);
        c.drawLine(x+106*s, y+7*s, x+122*s, y+h-22*s, pillarPaint);
        c.drawLine(x+80*s, y+6*s, x+82*s, y+h-23*s, pillarPaint);
        pillarPaint.setStyle(Paint.Style.FILL);
        drawMirror(c, x+120*s, y+h-27*s, s);
        drawGTRWing(c, x, y, s, col);
        drawCircleHeadlight(c, x+150*s, y+h-20*s, 6*s);
        drawLEDTaillight(c, x+2*s, y+h-28*s, 4*s, 12*s, s);
        drawGrilleBars(c, x+150*s, y+h-20*s, 12*s, 12*s, s, 3);
        drawExhaust(c, x+8*s, y+h-10*s, s); drawExhaust(c, x+15*s, y+h-10*s, s);
        drawDetailedWheel(c, rWx, y+h, wR, rot, 10, col);
        drawDetailedWheel(c, fWx, y+h, wR, rot, 10, col);
    }

    // =========================================================================
    // 5. BMW M3 Competition
    // =========================================================================
    private void drawBMWM3(Canvas c, float x, float y, float s, int col, float rot) {
        float h = 49*s, rWx = x+32*s, fWx = x+138*s, wR = 17*s;
        bodyPath.reset();
        bodyPath.moveTo(x+2*s, y+h-6*s); bodyPath.lineTo(x+2*s, y+h-26*s);
        bodyPath.lineTo(x+30*s, y+h-28*s);
        bodyPath.quadTo(x+48*s, y+4*s, x+65*s, y+3*s);
        bodyPath.lineTo(x+105*s, y+3*s);
        bodyPath.quadTo(x+120*s, y+3*s, x+132*s, y+h-22*s);
        bodyPath.lineTo(x+160*s, y+h-20*s);
        bodyPath.quadTo(x+164*s, y+h-10*s, x+162*s, y+h-6*s); bodyPath.close();
        c.drawPath(bodyPath, bodyPaint);
        tmpPath.reset();
        tmpPath.moveTo(x+67*s, y+5*s); tmpPath.lineTo(x+103*s, y+5*s);
        tmpPath.lineTo(x+100*s, y+12*s); tmpPath.lineTo(x+70*s, y+12*s); tmpPath.close();
        c.drawPath(tmpPath, highlightPaint);
        detailPaint.setStrokeWidth(1.8f*s);
        c.drawLine(x+8*s, y+h-19*s, x+152*s, y+h-16*s, detailPaint);
        glassPath.reset();
        glassPath.moveTo(x+50*s, y+5*s); glassPath.lineTo(x+103*s, y+5*s);
        glassPath.lineTo(x+128*s, y+h-22*s); glassPath.lineTo(x+40*s, y+h-24*s); glassPath.close();
        c.drawPath(glassPath, glassPaint);
        pillarPaint.setStyle(Paint.Style.STROKE); pillarPaint.setStrokeWidth(3.5f*s);
        c.drawLine(x+50*s, y+5*s, x+40*s, y+h-24*s, pillarPaint);
        c.drawLine(x+103*s, y+5*s, x+128*s, y+h-22*s, pillarPaint);
        c.drawLine(x+76*s, y+4*s, x+80*s, y+h-23*s, pillarPaint);
        pillarPaint.setStyle(Paint.Style.FILL);
        drawMirror(c, x+126*s, y+h-27*s, s);
        body2Paint.setColor(darken(col, 0.65f));
        c.drawRoundRect(x+2*s, y+h-30*s, x+30*s, y+h-26*s, 3*s, 3*s, body2Paint);
        drawBMWGrille(c, x+145*s, y+h-20*s, s);
        drawLEDHeadlight(c, x+144*s, y+h-24*s, 14*s, 6*s, s);
        drawLEDTaillight(c, x+2*s, y+h-28*s, 4*s, 12*s, s);
        drawExhaust(c, x+10*s, y+h-10*s, s); drawExhaust(c, x+18*s, y+h-10*s, s);
        drawDetailedWheel(c, rWx, y+h, wR, rot, 10, col);
        drawDetailedWheel(c, fWx, y+h, wR, rot, 10, col);
    }

    // =========================================================================
    // 6. Lamborghini Aventador SVJ
    // =========================================================================
    private void drawLamborghiniAventador(Canvas c, float x, float y, float s, int col, float rot) {
        float h = 38*s, rWx = x+30*s, fWx = x+148*s, wR = 15*s;
        bodyPath.reset();
        bodyPath.moveTo(x+4*s, y+h-5*s); bodyPath.lineTo(x+2*s, y+h-18*s);
        bodyPath.lineTo(x+20*s, y+h-20*s); bodyPath.lineTo(x+32*s, y+9*s);
        bodyPath.quadTo(x+55*s, y+3*s, x+90*s, y+2*s);
        bodyPath.lineTo(x+150*s, y+h-10*s);
        bodyPath.quadTo(x+172*s, y+h-7*s, x+174*s, y+h-5*s); bodyPath.close();
        c.drawPath(bodyPath, bodyPaint);
        body2Paint.setColor(darken(col, 0.65f));
        tmpPath.reset();
        tmpPath.moveTo(x+35*s, y+h-12*s); tmpPath.lineTo(x+140*s, y+h-10*s);
        tmpPath.lineTo(x+138*s, y+h-7*s); tmpPath.lineTo(x+33*s, y+h-8*s); tmpPath.close();
        c.drawPath(tmpPath, body2Paint);
        tmpPath.reset();
        tmpPath.moveTo(x+55*s, y+5*s); tmpPath.lineTo(x+88*s, y+3*s);
        tmpPath.lineTo(x+85*s, y+10*s); tmpPath.lineTo(x+60*s, y+11*s); tmpPath.close();
        c.drawPath(tmpPath, highlightPaint);
        glassPath.reset();
        glassPath.moveTo(x+50*s, y+6*s); glassPath.lineTo(x+85*s, y+4*s);
        glassPath.lineTo(x+120*s, y+h-12*s); glassPath.lineTo(x+40*s, y+h-14*s); glassPath.close();
        c.drawPath(glassPath, glassPaint);
        pillarPaint.setStyle(Paint.Style.STROKE); pillarPaint.setStrokeWidth(3*s);
        c.drawLine(x+50*s, y+6*s, x+40*s, y+h-14*s, pillarPaint);
        c.drawLine(x+85*s, y+4*s, x+120*s, y+h-12*s, pillarPaint);
        pillarPaint.setStyle(Paint.Style.FILL);
        drawMirror(c, x+115*s, y+h-18*s, s);
        drawLamboWing(c, x, y, s, col);
        body2Paint.setColor(darken(col, 0.35f));
        c.drawRoundRect(x+148*s, y+h-8*s, x+174*s, y+h-4*s, 2*s, 2*s, body2Paint);
        c.drawRect(x+140*s, y+h-12*s, x+152*s, y+h-8*s, body2Paint);
        drawLEDHeadlight(c, x+155*s, y+h-14*s, 14*s, 5*s, s);
        drawLEDTaillight(c, x+2*s, y+h-20*s, 3*s, 10*s, s);
        drawExhaust(c, x+6*s, y+h-8*s, s); drawExhaust(c, x+12*s, y+h-8*s, s);
        drawExhaust(c, x+18*s, y+h-8*s, s); drawExhaust(c, x+24*s, y+h-8*s, s);
        drawDetailedWheel(c, rWx, y+h, wR, rot, 10, col);
        drawDetailedWheel(c, fWx, y+h, wR*0.95f, rot, 10, col);
    }

    // =========================================================================
    // 7. Bugatti Chiron Super Sport
    // =========================================================================
    private void drawBugattiChiron(Canvas c, float x, float y, float s, int col, float rot) {
        float h = 37*s, rWx = x+28*s, fWx = x+158*s, wR = 15*s;
        bodyPath.reset();
        bodyPath.moveTo(x+8*s, y+h-5*s); bodyPath.lineTo(x+2*s, y+h-22*s);
        bodyPath.quadTo(x+30*s, y, x+90*s, y+2*s);
        bodyPath.quadTo(x+135*s, y+2*s, x+175*s, y+h-10*s);
        bodyPath.lineTo(x+180*s, y+h-5*s); bodyPath.close();
        c.drawPath(bodyPath, bodyPaint);
        // C-stripe
        int stripe = blendColor(col, 0xFF000000, 0.5f);
        body2Paint.setColor(stripe);
        tmpPath.reset();
        tmpPath.moveTo(x+68*s, y+4*s); tmpPath.lineTo(x+110*s, y+4*s);
        tmpPath.lineTo(x+130*s, y+h-11*s); tmpPath.lineTo(x+50*s, y+h-14*s); tmpPath.close();
        c.drawPath(tmpPath, body2Paint);
        glassPath.reset();
        glassPath.moveTo(x+40*s, y+5*s); glassPath.lineTo(x+88*s, y+2*s);
        glassPath.lineTo(x+115*s, y+h-12*s); glassPath.lineTo(x+30*s, y+h-16*s); glassPath.close();
        c.drawPath(glassPath, glassPaint);
        tmpPath.reset();
        tmpPath.moveTo(x+42*s, y+6*s); tmpPath.lineTo(x+65*s, y+7*s);
        tmpPath.lineTo(x+58*s, y+h-15*s); tmpPath.lineTo(x+32*s, y+h-16*s); tmpPath.close();
        c.drawPath(tmpPath, highlightPaint);
        pillarPaint.setStyle(Paint.Style.STROKE); pillarPaint.setStrokeWidth(3*s);
        c.drawLine(x+40*s, y+5*s, x+30*s, y+h-16*s, pillarPaint);
        c.drawLine(x+88*s, y+2*s, x+115*s, y+h-12*s, pillarPaint);
        pillarPaint.setStyle(Paint.Style.FILL);
        drawMirror(c, x+112*s, y+h-18*s, s);
        body2Paint.setColor(darken(col, 0.5f));
        c.drawRoundRect(x+2*s, y+h-26*s, x+28*s, y+h-22*s, 3*s, 3*s, body2Paint);
        c.drawRect(x+10*s, y+h-22*s, x+13*s, y+h-18*s, body2Paint);
        drawLEDHeadlight(c, x+163*s, y+h-13*s, 12*s, 5*s, s);
        drawLEDTaillight(c, x+2*s, y+h-22*s, 3*s, 12*s, s);
        drawExhaust(c, x+6*s, y+h-8*s, s); drawExhaust(c, x+12*s, y+h-8*s, s);
        drawExhaust(c, x+18*s, y+h-8*s, s); drawExhaust(c, x+24*s, y+h-8*s, s);
        drawDetailedWheel(c, rWx, y+h, wR, rot, 10, col);
        drawDetailedWheel(c, fWx, y+h, wR*0.95f, rot, 10, col);
    }

    // =========================================================================
    // 8. Rimac Nevera (Electric Hypercar)
    // =========================================================================
    private void drawRimacNevera(Canvas c, float x, float y, float s, int col, float rot) {
        float h = 38*s, rWx = x+30*s, fWx = x+148*s, wR = 15*s;
        bodyPath.reset();
        bodyPath.moveTo(x+5*s, y+h-5*s); bodyPath.lineTo(x+2*s, y+h-20*s);
        bodyPath.quadTo(x+28*s, y+2*s, x+75*s, y+2*s);
        bodyPath.lineTo(x+100*s, y+2*s);
        bodyPath.quadTo(x+140*s, y+2*s, x+168*s, y+h-10*s);
        bodyPath.lineTo(x+170*s, y+h-5*s); bodyPath.close();
        c.drawPath(bodyPath, bodyPaint);
        body2Paint.setColor(darken(col, 0.75f));
        tmpPath.reset();
        tmpPath.moveTo(x+10*s, y+h-14*s); tmpPath.lineTo(x+155*s, y+h-12*s);
        tmpPath.lineTo(x+153*s, y+h-8*s); tmpPath.lineTo(x+8*s, y+h-10*s); tmpPath.close();
        c.drawPath(tmpPath, body2Paint);
        glassPath.reset();
        glassPath.moveTo(x+38*s, y+5*s); glassPath.lineTo(x+90*s, y+3*s);
        glassPath.lineTo(x+118*s, y+h-11*s); glassPath.lineTo(x+30*s, y+h-14*s); glassPath.close();
        c.drawPath(glassPath, glassPaint);
        pillarPaint.setStyle(Paint.Style.STROKE); pillarPaint.setStrokeWidth(3*s);
        c.drawLine(x+38*s, y+5*s, x+30*s, y+h-14*s, pillarPaint);
        c.drawLine(x+90*s, y+3*s, x+118*s, y+h-11*s, pillarPaint);
        pillarPaint.setStyle(Paint.Style.FILL);
        drawMirror(c, x+116*s, y+h-17*s, s);
        // EV light bar (no grille)
        lightPaint.setColor(Color.parseColor("#00E5FF"));
        c.drawRoundRect(x+155*s, y+h-16*s, x+168*s, y+h-14*s, 1*s, 1*s, lightPaint);
        lightPaint.setColor(Color.parseColor("#FF1744"));
        c.drawRoundRect(x+2*s, y+h-20*s, x+5*s, y+h-7*s, 1*s, 1*s, lightPaint);
        body2Paint.setColor(darken(col, 0.5f));
        c.drawRoundRect(x+2*s, y+h-28*s, x+30*s, y+h-24*s, 2*s, 2*s, body2Paint);
        drawDetailedWheel(c, rWx, y+h, wR, rot, 10, col);
        drawDetailedWheel(c, fWx, y+h, wR*0.95f, rot, 10, col);
    }

    // =========================================================================
    // 9. Top Fuel Dragster
    // =========================================================================
    private void drawDragster(Canvas c, float x, float y, float s, int col, float rot) {
        float h = 35*s, rWx = x+30*s, fWx = x+220*s, rWr = 22*s, fWr = 9*s;
        // Chassis rail
        body2Paint.setColor(Color.parseColor("#222222"));
        c.drawRoundRect(x+30*s, y+h-14*s, x+230*s, y+h-10*s, 3*s, 3*s, body2Paint);
        // Engine block
        bodyPath.reset();
        bodyPath.moveTo(x+35*s, y+h-5*s); bodyPath.lineTo(x+30*s, y+h-32*s);
        bodyPath.lineTo(x+80*s, y+h-28*s); bodyPath.lineTo(x+90*s, y+h-5*s); bodyPath.close();
        c.drawPath(bodyPath, bodyPaint);
        // Blower
        body2Paint.setColor(darken(col, 0.7f));
        c.drawRoundRect(x+40*s, y+h-36*s, x+72*s, y+h-28*s, 4*s, 4*s, body2Paint);
        detailPaint.setStrokeWidth(1.5f*s);
        for (int i=0;i<6;i++) { float bx=x+(44+i*5)*s; c.drawLine(bx, y+h-35*s, bx, y+h-29*s, detailPaint); }
        // Cockpit
        glassPath.reset();
        glassPath.moveTo(x+35*s, y+h-28*s); glassPath.lineTo(x+50*s, y+h-32*s);
        glassPath.lineTo(x+52*s, y+h-20*s); glassPath.lineTo(x+37*s, y+h-18*s); glassPath.close();
        c.drawPath(glassPath, glassPaint);
        // Rear wing
        body2Paint.setColor(darken(col, 0.55f));
        c.drawRoundRect(x-20*s, y+h-42*s, x+28*s, y+h-37*s, 3*s, 3*s, body2Paint);
        c.drawRect(x+5*s, y+h-37*s, x+9*s, y+h-25*s, body2Paint);
        c.drawRect(x+18*s, y+h-37*s, x+22*s, y+h-25*s, body2Paint);
        // Exhaust headers
        for (int i=0;i<4;i++) {
            exhaustPaint.setColor(i%2==0?Color.parseColor("#555555"):Color.parseColor("#444444"));
            c.drawOval(x+(75+i*4)*s, y+h-28*s, x+(78+i*4)*s, y+h-20*s, exhaustPaint);
        }
        drawDetailedWheel(c, rWx, y+h, rWr, rot, 5, col);
        drawDetailedWheel(c, fWx, y+h, fWr, rot, 3, col);
    }

    // =========================================================================
    // 10. Toyota Corolla
    // =========================================================================
    private void drawToyotaCorolla(Canvas c, float x, float y, float s, int col, float rot) {
        float h = 50*s, rWx = x+30*s, fWx = x+130*s, wR = 15*s;
        bodyPath.reset();
        bodyPath.moveTo(x+2*s, y+h-6*s); bodyPath.lineTo(x+2*s, y+h-25*s);
        bodyPath.lineTo(x+28*s, y+h-27*s);
        bodyPath.quadTo(x+50*s, y+4*s, x+70*s, y+3*s);
        bodyPath.lineTo(x+105*s, y+3*s);
        bodyPath.quadTo(x+120*s, y+3*s, x+132*s, y+h-22*s);
        bodyPath.lineTo(x+155*s, y+h-20*s);
        bodyPath.quadTo(x+160*s, y+h-10*s, x+158*s, y+h-6*s); bodyPath.close();
        c.drawPath(bodyPath, bodyPaint);
        detailPaint.setStrokeWidth(1.5f*s);
        c.drawLine(x+5*s, y+h-19*s, x+148*s, y+h-16*s, detailPaint);
        glassPath.reset();
        glassPath.moveTo(x+52*s, y+6*s); glassPath.lineTo(x+103*s, y+6*s);
        glassPath.lineTo(x+126*s, y+h-22*s); glassPath.lineTo(x+40*s, y+h-24*s); glassPath.close();
        c.drawPath(glassPath, glassPaint);
        pillarPaint.setStyle(Paint.Style.STROKE); pillarPaint.setStrokeWidth(3.5f*s);
        c.drawLine(x+52*s, y+6*s, x+40*s, y+h-24*s, pillarPaint);
        c.drawLine(x+103*s, y+6*s, x+126*s, y+h-22*s, pillarPaint);
        c.drawLine(x+78*s, y+5*s, x+80*s, y+h-23*s, pillarPaint);
        pillarPaint.setStyle(Paint.Style.FILL);
        drawMirror(c, x+124*s, y+h-27*s, s);
        drawLEDHeadlight(c, x+140*s, y+h-22*s, 12*s, 6*s, s);
        drawLEDTaillight(c, x+2*s, y+h-26*s, 4*s, 12*s, s);
        drawExhaust(c, x+10*s, y+h-9*s, s);
        drawDetailedWheel(c, rWx, y+h, wR, rot, 5, col);
        drawDetailedWheel(c, fWx, y+h, wR, rot, 5, col);
    }

    // =========================================================================
    // 11. Jeep Wrangler
    // =========================================================================
    private void drawJeepWrangler(Canvas c, float x, float y, float s, int col, float rot) {
        float h = 58*s, rWx = x+36*s, fWx = x+136*s, wR = 20*s;
        bodyPath.reset();
        bodyPath.moveTo(x+2*s, y+h-6*s); bodyPath.lineTo(x+2*s, y+h-48*s);
        bodyPath.lineTo(x+6*s, y+h-52*s); bodyPath.lineTo(x+158*s, y+h-52*s);
        bodyPath.lineTo(x+164*s, y+h-44*s); bodyPath.lineTo(x+164*s, y+h-26*s);
        bodyPath.lineTo(x+170*s, y+h-22*s); bodyPath.lineTo(x+170*s, y+h-6*s); bodyPath.close();
        c.drawPath(bodyPath, bodyPaint);
        body2Paint.setColor(darken(col, 0.7f));
        c.drawRect(x+6*s, y+h-52*s, x+158*s, y+h-44*s, body2Paint);
        detailPaint.setStrokeWidth(2.5f*s);
        c.drawLine(x+4*s, y+h-28*s, x+164*s, y+h-26*s, detailPaint);
        glassPaint.setColor(Color.argb(200, 20, 35, 50));
        c.drawRoundRect(x+14*s, y+h-50*s, x+76*s,  y+h-30*s, 3*s, 3*s, glassPaint);
        c.drawRoundRect(x+80*s, y+h-50*s, x+155*s, y+h-30*s, 3*s, 3*s, glassPaint);
        glassPaint.setColor(Color.argb(210, 18, 32, 48));
        pillarPaint.setStyle(Paint.Style.FILL);
        c.drawRect(x+10*s,  y+h-50*s, x+15*s,  y+h-28*s, pillarPaint);
        c.drawRect(x+76*s,  y+h-50*s, x+82*s,  y+h-28*s, pillarPaint);
        c.drawRect(x+153*s, y+h-50*s, x+158*s, y+h-28*s, pillarPaint);
        drawCircleHeadlight(c, x+155*s, y+h-38*s, 7*s);
        drawCircleHeadlight(c, x+155*s, y+h-24*s, 5*s);
        drawGrilleBars(c, x+158*s, y+h-42*s, 12*s, 30*s, s, 7);
        drawLEDTaillight(c, x+2*s, y+h-44*s, 4*s, 20*s, s);
        // Spare tire
        c.drawCircle(x+14*s, y+h-28*s, 14*s, tirePaint);
        c.drawCircle(x+14*s, y+h-28*s, 9*s, rimPaint);
        drawExhaust(c, x+10*s, y+h-10*s, s);
        drawDetailedWheel(c, rWx, y+h, wR, rot, 5, col);
        drawDetailedWheel(c, fWx, y+h, wR, rot, 5, col);
    }

    // =========================================================================
    // 12. Chevrolet Corvette C1
    // =========================================================================
    private void drawCorvetteC1(Canvas c, float x, float y, float s, int col, float rot) {
        float h = 44*s, rWx = x+32*s, fWx = x+140*s, wR = 16*s;
        bodyPath.reset();
        bodyPath.moveTo(x+4*s, y+h-6*s); bodyPath.lineTo(x+2*s, y+h-16*s);
        bodyPath.quadTo(x+18*s, y+h-20*s, x+30*s, y+h-24*s);
        bodyPath.quadTo(x+55*s, y+8*s, x+80*s, y+6*s);
        bodyPath.lineTo(x+100*s, y+5*s);
        bodyPath.quadTo(x+130*s, y+5*s, x+150*s, y+h-16*s);
        bodyPath.lineTo(x+170*s, y+h-14*s);
        bodyPath.quadTo(x+174*s, y+h-8*s, x+172*s, y+h-6*s); bodyPath.close();
        c.drawPath(bodyPath, bodyPaint);
        // Chrome trim
        body2Paint.setColor(Color.parseColor("#E0E0E0"));
        c.drawRoundRect(x+10*s, y+h-16*s, x+160*s, y+h-13*s, 2*s, 2*s, body2Paint);
        // Hood scoop
        body2Paint.setColor(darken(col, 0.75f));
        tmpPath.reset();
        tmpPath.moveTo(x+128*s, y+h-16*s);
        tmpPath.quadTo(x+145*s, y+h-22*s, x+162*s, y+h-16*s);
        tmpPath.lineTo(x+158*s, y+h-12*s);
        tmpPath.quadTo(x+143*s, y+h-18*s, x+130*s, y+h-12*s); tmpPath.close();
        c.drawPath(tmpPath, body2Paint);
        glassPath.reset();
        glassPath.moveTo(x+72*s, y+9*s); glassPath.lineTo(x+100*s, y+7*s);
        glassPath.lineTo(x+118*s, y+h-18*s); glassPath.lineTo(x+65*s, y+h-20*s); glassPath.close();
        c.drawPath(glassPath, glassPaint);
        pillarPaint.setStyle(Paint.Style.STROKE); pillarPaint.setStrokeWidth(3*s);
        c.drawLine(x+72*s, y+9*s, x+65*s, y+h-20*s, pillarPaint);
        c.drawLine(x+100*s, y+7*s, x+118*s, y+h-18*s, pillarPaint);
        pillarPaint.setStyle(Paint.Style.FILL);
        drawMirror(c, x+116*s, y+h-22*s, s);
        drawCircleHeadlight(c, x+160*s, y+h-20*s, 6*s);
        drawCircleHeadlight(c, x+152*s, y+h-18*s, 4*s);
        lightPaint.setColor(Color.parseColor("#D50000"));
        c.drawCircle(x+8*s, y+h-20*s, 5*s, lightPaint);
        lightPaint.setColor(Color.parseColor("#FF6F00"));
        c.drawCircle(x+16*s, y+h-19*s, 3*s, lightPaint);
        drawGrilleBars(c, x+158*s, y+h-22*s, 12*s, 12*s, s, 5);
        drawExhaust(c, x+8*s, y+h-9*s, s); drawExhaust(c, x+14*s, y+h-9*s, s);
        drawDetailedWheel(c, rWx, y+h, wR, rot, 5, Color.parseColor("#E0E0E0"));
        drawDetailedWheel(c, fWx, y+h, wR, rot, 5, Color.parseColor("#E0E0E0"));
    }

    // =========================================================================
    // 13. McLaren 720S
    // =========================================================================
    private void drawMcLaren720S(Canvas c, float x, float y, float s, int col, float rot) {
        float h = 36*s, rWx = x+30*s, fWx = x+152*s, wR = 14*s;
        bodyPath.reset();
        bodyPath.moveTo(x+5*s, y+h-5*s); bodyPath.lineTo(x+2*s, y+h-16*s);
        bodyPath.lineTo(x+18*s, y+h-22*s);
        bodyPath.quadTo(x+35*s, y+2*s, x+68*s, y+2*s);
        bodyPath.lineTo(x+92*s, y+2*s);
        bodyPath.quadTo(x+148*s, y+2*s, x+165*s, y+h-10*s);
        bodyPath.lineTo(x+167*s, y+h-5*s); bodyPath.close();
        c.drawPath(bodyPath, bodyPaint);
        body2Paint.setColor(darken(col, 0.72f));
        tmpPath.reset();
        tmpPath.moveTo(x+28*s, y+h-15*s); tmpPath.lineTo(x+145*s, y+h-12*s);
        tmpPath.lineTo(x+143*s, y+h-8*s); tmpPath.lineTo(x+26*s, y+h-11*s); tmpPath.close();
        c.drawPath(tmpPath, body2Paint);
        glassPath.reset();
        glassPath.moveTo(x+34*s, y+5*s); glassPath.lineTo(x+84*s, y+3*s);
        glassPath.lineTo(x+118*s, y+h-10*s); glassPath.lineTo(x+25*s, y+h-14*s); glassPath.close();
        c.drawPath(glassPath, glassPaint);
        tmpPath.reset();
        tmpPath.moveTo(x+36*s, y+6*s); tmpPath.lineTo(x+55*s, y+7*s);
        tmpPath.lineTo(x+46*s, y+h-14*s); tmpPath.lineTo(x+27*s, y+h-14*s); tmpPath.close();
        c.drawPath(tmpPath, highlightPaint);
        pillarPaint.setStyle(Paint.Style.STROKE); pillarPaint.setStrokeWidth(3*s);
        c.drawLine(x+34*s, y+5*s, x+25*s, y+h-14*s, pillarPaint);
        c.drawLine(x+84*s, y+3*s, x+118*s, y+h-10*s, pillarPaint);
        pillarPaint.setStyle(Paint.Style.FILL);
        drawMirror(c, x+116*s, y+h-16*s, s);
        body2Paint.setColor(darken(col, 0.5f));
        c.drawRoundRect(x+2*s, y+h-22*s, x+25*s, y+h-18*s, 2*s, 2*s, body2Paint);
        c.drawRect(x+8*s, y+h-18*s, x+11*s, y+h-14*s, body2Paint);
        drawLEDHeadlight(c, x+155*s, y+h-12*s, 10*s, 4*s, s);
        drawLEDTaillight(c, x+2*s, y+h-18*s, 3*s, 9*s, s);
        body2Paint.setColor(darken(col, 0.35f));
        c.drawRoundRect(x+152*s, y+h-7*s, x+167*s, y+h-4*s, 2*s, 2*s, body2Paint);
        drawExhaust(c, x+6*s, y+h-7*s, s); drawExhaust(c, x+12*s, y+h-7*s, s); drawExhaust(c, x+18*s, y+h-7*s, s);
        drawDetailedWheel(c, rWx, y+h, wR, rot, 10, col);
        drawDetailedWheel(c, fWx, y+h, wR*0.92f, rot, 10, col);
    }

    // =========================================================================
    // 14. Apollo Sport
    // =========================================================================
    private void drawApolloSport(Canvas c, float x, float y, float s, int col, float rot) {
        float h = 34*s, rWx = x+32*s, fWx = x+148*s, wR = 16*s;

        // Podium/Showroom Platform
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.argb(180, 20, 20, 20));
        c.drawOval(x-15*s, y+h-5*s, x+185*s, y+h+12*s, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2*s);
        p.setColor(Color.argb(100, 100, 100, 120));
        c.drawOval(x-15*s, y+h-5*s, x+185*s, y+h+12*s, p);

        bodyPath.reset();
        // Aggressive mid-engine hypercar shape
        bodyPath.moveTo(x+2*s, y+h-6*s);
        bodyPath.lineTo(x+2*s, y+h-22*s); // Tall squared rear
        bodyPath.lineTo(x+30*s, y+h-25*s); // Rear deck
        bodyPath.quadTo(x+55*s, y-2*s, x+85*s, y-2*s); // High roof hump
        bodyPath.lineTo(x+105*s, y-2*s);
        bodyPath.quadTo(x+145*s, y+4*s, x+165*s, y+h-18*s); // Long sloping nose
        bodyPath.lineTo(x+172*s, y+h-16*s);
        bodyPath.lineTo(x+170*s, y+h-6*s);
        bodyPath.close();
        c.drawPath(bodyPath, bodyPaint);

        // Roof Detail (Center spine/scoop)
        tmpPath.reset();
        tmpPath.moveTo(x+65*s, y); tmpPath.lineTo(x+95*s, y);
        tmpPath.lineTo(x+90*s, y+8*s); tmpPath.lineTo(x+70*s, y+8*s); tmpPath.close();
        c.drawPath(tmpPath, highlightPaint);

        // Side Air Intake
        body2Paint.setColor(darken(col, 0.7f));
        c.drawRoundRect(x+35*s, y+h-16*s, x+68*s, y+h-8*s, 5*s, 5*s, body2Paint);

        // Glass
        glassPath.reset();
        glassPath.moveTo(x+68*s, y); glassPath.lineTo(x+100*s, y);
        glassPath.lineTo(x+128*s, y+h-18*s); glassPath.lineTo(x+50*s, y+h-20*s); glassPath.close();
        c.drawPath(glassPath, glassPaint);

        // Spoiler
        drawSpoiler(c, x+2*s, y+h-44*s, 42*s, 10*s, s, col);

        // Details
        drawLEDHeadlight(c, x+155*s, y+h-22*s, 12*s, 6*s, s);
        drawLEDTaillight(c, x+2*s, y+h-25*s, 5*s, 16*s, s);

        // Wheels
        drawDetailedWheel(c, rWx, y+h, wR, rot, 5, col);
        drawDetailedWheel(c, fWx, y+h, wR, rot, 5, col);
    }

    // =========================================================================
    // Fallback generic car
    // =========================================================================
    private void drawGenericCar(Canvas c, float x, float y, float s, int col, float rot) {
        float h = 50*s, rWx = x+30*s, fWx = x+128*s, wR = 15*s;
        bodyPath.reset();
        bodyPath.moveTo(x+2*s, y+h-6*s); bodyPath.lineTo(x+2*s, y+h-26*s);
        bodyPath.quadTo(x+45*s, y+3*s, x+70*s, y+3*s);
        bodyPath.lineTo(x+102*s, y+3*s);
        bodyPath.quadTo(x+118*s, y+3*s, x+130*s, y+h-20*s);
        bodyPath.lineTo(x+152*s, y+h-18*s); bodyPath.lineTo(x+152*s, y+h-6*s); bodyPath.close();
        c.drawPath(bodyPath, bodyPaint);
        glassPath.reset();
        glassPath.moveTo(x+50*s, y+6*s); glassPath.lineTo(x+100*s, y+6*s);
        glassPath.lineTo(x+124*s, y+h-20*s); glassPath.lineTo(x+38*s, y+h-22*s); glassPath.close();
        c.drawPath(glassPath, glassPaint);
        drawLEDHeadlight(c, x+135*s, y+h-22*s, 12*s, 6*s, s);
        drawLEDTaillight(c, x+2*s, y+h-26*s, 4*s, 12*s, s);
        drawExhaust(c, x+10*s, y+h-9*s, s);
        drawDetailedWheel(c, rWx, y+h, wR, rot, 5, col);
        drawDetailedWheel(c, fWx, y+h, wR, rot, 5, col);
    }

    // =========================================================================
    // Detail helpers
    // =========================================================================

    private boolean drawCarImage(Canvas canvas, Car car, float x, float y, float scale,
                                 float wheelRotation, boolean animateWheels) {
        String resourceName = car.getImageResourceName();
        if (context == null || resourceName == null) return false;

        String cleanName = resourceName.startsWith("car_") ? resourceName.substring(4) : resourceName;

        Bitmap bodyBitmap = bodyBitmaps.get(cleanName);
        if (bodyBitmap == null) {
            bodyBitmap = loadBitmapFromAssetsOrDrawable(cleanName + "_body");
            if (bodyBitmap == null) {
                bodyBitmap = loadBitmapFromAssetsOrDrawable(resourceName + "_body");
            }
            if (bodyBitmap == null) {
                bodyBitmap = loadBitmapFromAssetsOrDrawable(cleanName);
            }
            if (bodyBitmap == null) {
                bodyBitmap = loadBitmapFromAssetsOrDrawable(resourceName);
            }
            if (bodyBitmap == null) return false;
            bodyBitmaps.put(cleanName, bodyBitmap);
        }

        Bitmap tyreBitmap = tyreBitmaps.get(cleanName);
        if (tyreBitmap == null) {
            tyreBitmap = loadBitmapFromAssetsOrDrawable(cleanName + "_tyre");
            if (tyreBitmap == null) {
                tyreBitmap = loadBitmapFromAssetsOrDrawable(resourceName + "_tyre");
            }
            if (tyreBitmap != null) {
                tyreBitmaps.put(cleanName, tyreBitmap);
            }
        }

        float targetWidth = 185.0f * scale;
        float targetHeight = targetWidth * bodyBitmap.getHeight() / bodyBitmap.getWidth();
        float top = y + 55.0f * scale - targetHeight;
        RectF destination = new RectF(x, top, x + targetWidth, top + targetHeight);

        String tintedKey = cleanName + ":" + car.getColor();
        Bitmap tintedBodyBitmap = tintedBodyBitmaps.get(tintedKey);
        if (tintedBodyBitmap == null) {
            tintedBodyBitmap = createPixelCarBitmap(bodyBitmap, car.getColor());
            tintedBodyBitmaps.put(tintedKey, tintedBodyBitmap);
        }

        float rearWheelRelX = 48.0f / 279.0f;
        float frontWheelRelX = 229.0f / 279.0f;
        float wheelRelY = 75.0f / 101.0f;
        float relRadius = 0.065f;

        WheelSpec spec = WHEEL_SPECS.get(cleanName);
        if (spec == null) {
            spec = WHEEL_SPECS.get(resourceName);
        }
        if (spec != null) {
            rearWheelRelX = spec.rearX;
            frontWheelRelX = spec.frontX;
            wheelRelY = spec.centerY;
            relRadius = spec.radius;
        }

        float rearWheelX = x + targetWidth * rearWheelRelX;
        float frontWheelX = x + targetWidth * frontWheelRelX;
        float wheelY = top + targetHeight * wheelRelY;
        float radius = Math.max(5.0f, targetWidth * relRadius);

        imagePaint.setAlpha(255);

        // 1. Draw rotating wheel bitmaps behind the body
        if (tyreBitmap != null) {
            drawRotatingTyre(canvas, tyreBitmap, rearWheelX, wheelY, radius, wheelRotation);
            drawRotatingTyre(canvas, tyreBitmap, frontWheelX, wheelY, radius, wheelRotation);
        } else if (animateWheels) {
            int spokes = spec != null ? spec.spokes : 5;
            int rimColor = spec != null ? spec.rimColor : 0xFFBDBDBD;
            drawAnimatedWheel(canvas, rearWheelX, wheelY, radius, wheelRotation, spokes, rimColor);
            drawAnimatedWheel(canvas, frontWheelX, wheelY, radius, wheelRotation, spokes, rimColor);
        }

        // 2. Draw body over the wheels
        canvas.drawBitmap(tintedBodyBitmap, null, destination, imagePaint);
        return true;
    }

    private void drawRotatingTyre(Canvas canvas, Bitmap tyreBitmap, float cx, float cy, float radius, float angleDeg) {
        if (tyreBitmap == null) return;
        canvas.save();
        canvas.rotate(angleDeg, cx, cy);
        RectF tyreRect = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);
        imagePaint.setFilterBitmap(true);
        canvas.drawBitmap(tyreBitmap, null, tyreRect, imagePaint);
        canvas.restore();
    }

    private Bitmap loadBitmapFromAssetsOrDrawable(String name) {
        if (context == null || name == null) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;

        int resourceId = context.getResources().getIdentifier(name, "drawable", context.getPackageName());
        if (resourceId != 0) {
            try {
                Bitmap b = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
                if (b != null) return b;
            } catch (Exception ignored) {}
        }

        String[] candidatePaths = new String[] {
            "carsimg/" + name + ".png",
            name + ".png",
            "carsimg/" + name,
            name
        };
        for (String path : candidatePaths) {
            try (InputStream stream = context.getAssets().open(path)) {
                Bitmap b = BitmapFactory.decodeStream(stream, null, options);
                if (b != null) return b;
            } catch (IOException ignored) {}
        }

        try {
            String normTarget = normalizeAssetName(name);
            String[] dirs = new String[] { "carsimg", "" };
            for (String dir : dirs) {
                String[] list = context.getAssets().list(dir);
                if (list == null) continue;
                for (String item : list) {
                    if (normalizeAssetName(item).equals(normTarget)) {
                        String fullPath = dir.isEmpty() ? item : dir + "/" + item;
                        try (InputStream stream = context.getAssets().open(fullPath)) {
                            Bitmap b = BitmapFactory.decodeStream(stream, null, options);
                            if (b != null) return b;
                        }
                    }
                }
            }
        } catch (IOException ignored) {}

        return null;
    }

    private String normalizeAssetName(String name) {
        String withoutExtension = name.replaceFirst("\\.[^.]+$", "");
        String normalized = Normalizer.normalize(withoutExtension, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private Bitmap createPixelCarBitmap(Bitmap source, int bodyColor) {
        int width = source.getWidth();
        int height = source.getHeight();
        int[] pixels = new int[width * height];
        source.getPixels(pixels, 0, width, 0, 0, width, height);

        float targetRed = Color.red(bodyColor) / 255.0f;
        float targetGreen = Color.green(bodyColor) / 255.0f;
        float targetBlue = Color.blue(bodyColor) / 255.0f;
        float[] hsv = new float[3];
        int[] hueBuckets = new int[24];
        for (int pixel : pixels) {
            if (Color.alpha(pixel) == 0) continue;
            Color.colorToHSV(pixel, hsv);
            if (hsv[1] > 0.18f && hsv[2] > 0.16f && hsv[2] < 0.97f) {
                hueBuckets[Math.min(23, (int) (hsv[0] / 15.0f))]++;
            }
        }
        int dominantBucket = 0;
        for (int i = 1; i < hueBuckets.length; i++) {
            if (hueBuckets[i] > hueBuckets[dominantBucket]) dominantBucket = i;
        }
        float dominantHue = dominantBucket * 15.0f + 7.5f;
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int alpha = Color.alpha(pixel);
            
            if (alpha == 0) {
                pixels[i] = 0;
                continue;
            }

            int red = Color.red(pixel);
            int green = Color.green(pixel);
            int blue = Color.blue(pixel);
            int maximum = Math.max(red, Math.max(green, blue));
            int minimum = Math.min(red, Math.min(green, blue));
            float saturation = maximum == 0 ? 0.0f : (maximum - minimum) / (float) maximum;
            Color.colorToHSV(pixel, hsv);
            float hueDistance = Math.abs(hsv[0] - dominantHue);
            hueDistance = Math.min(hueDistance, 360.0f - hueDistance);

            // Protect headlight and taillight colors from being changed
            boolean isLight = (red > 150 && green > 100 && blue < 90) 
                    || (red > 140 && green < 65 && blue < 65) 
                    || (red > 200 && green > 200 && blue < 120);

            // Body paint is saturated; grayscale tires, rims, glass and highlights stay intact.
            if (!isLight && saturation > 0.18f && maximum < 248 && hueDistance <= 24.0f) {
                float luminance = (red * 0.299f + green * 0.587f + blue * 0.114f) / 255.0f;
                boolean whitePaint = targetRed > 0.90f && targetGreen > 0.90f && targetBlue > 0.90f;
                boolean blackPaint = targetRed < 0.15f && targetGreen < 0.15f && targetBlue < 0.15f;
                if (whitePaint) {
                    int value = quantize8Bit(Math.min(255.0f, luminance * 255.0f * 1.18f));
                    pixels[i] = Color.argb(alpha, value, value, value);
                } else if (blackPaint) {
                    int value = quantize8Bit(Math.max(38.0f, luminance * 255.0f * 0.72f));
                    pixels[i] = Color.argb(alpha, value, value, value);
                } else {
                    float paintStrength = 0.45f + luminance * 0.55f;
                    pixels[i] = Color.argb(alpha,
                            quantize8Bit(targetRed * paintStrength * 255.0f),
                            quantize8Bit(targetGreen * paintStrength * 255.0f),
                            quantize8Bit(targetBlue * paintStrength * 255.0f));
                }
            } else {
                pixels[i] = Color.argb(alpha, red, green, blue);
            }
        }

        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        result.setPixels(pixels, 0, width, 0, 0, width, height);
        return result;
    }

    private int quantize8Bit(float channel) {
        return Math.max(0, Math.min(255, Math.round(channel)));
    }



    private void drawAnimatedWheel(Canvas canvas, float cx, float cy, float radius,
                                   float angleDeg, int spokes, int rimColor) {
        tirePaint.setStyle(Paint.Style.FILL);
        tirePaint.setColor(Color.rgb(18, 18, 20));
        canvas.drawCircle(cx, cy, radius, tirePaint);
        brakePaint.setColor(Color.rgb(72, 76, 82));
        canvas.drawCircle(cx, cy, radius * 0.77f, brakePaint);
        rimPaint.setColor(rimColor);
        canvas.drawCircle(cx, cy, radius * 0.65f, rimPaint);
        canvas.save();
        canvas.rotate(angleDeg, cx, cy);
        spokePaint.setColor(Color.rgb(45, 48, 52));
        spokePaint.setStrokeWidth(Math.max(1.0f, radius * 0.10f));
        for (int i = 0; i < spokes; i++) {
            double radians = Math.toRadians(i * 360.0 / spokes);
            float endX = cx + radius * 0.58f * (float) Math.sin(radians);
            float endY = cy - radius * 0.58f * (float) Math.cos(radians);
            canvas.drawLine(cx, cy, endX, endY, spokePaint);
        }
        canvas.restore();
        rimPaint.setColor(Color.rgb(35, 35, 38));
        canvas.drawCircle(cx, cy, radius * 0.13f, rimPaint);
        rimPaint.setColor(Color.LTGRAY);
        canvas.drawCircle(cx, cy, radius * 0.06f, rimPaint);
    }

    private void drawRealismDetails(Canvas c, String carId, float x, float y, float s, int col) {
        detailPaint.setStyle(Paint.Style.STROKE);
        detailPaint.setStrokeCap(Paint.Cap.ROUND);
        detailPaint.setStrokeWidth(1.1f * s);
        detailPaint.setColor(Color.argb(155, 18, 18, 18));

        if ("car_dragster".equals(carId)) {
            detailPaint.setStrokeWidth(1.4f * s);
            c.drawLine(x + 78*s, y + 12*s, x + 130*s, y + 20*s, detailPaint);
            c.drawLine(x + 84*s, y + 16*s, x + 142*s, y + 24*s, detailPaint);
            return;
        }

        if ("car_apollo".equals(carId)) {
            // Apollo's defining side profile: black canopy, deep side intake and sharp sill.
            detailPaint.setColor(Color.argb(210, 12, 12, 12));
            detailPaint.setStrokeWidth(1.5f * s);
            c.drawLine(x + 53*s, y + 18*s, x + 49*s, y + 28*s, detailPaint);
            c.drawLine(x + 101*s, y + 10*s, x + 126*s, y + 20*s, detailPaint);
            c.drawLine(x + 47*s, y + 29*s, x + 128*s, y + 27*s, detailPaint);
            c.drawLine(x + 72*s, y + 9*s, x + 71*s, y + 27*s, detailPaint);
            detailPaint.setColor(Color.argb(190, 255, 255, 255));
            detailPaint.setStrokeWidth(1.0f * s);
            c.drawLine(x + 13*s, y + 20*s, x + 38*s, y + 17*s, detailPaint);
            c.drawLine(x + 132*s, y + 18*s, x + 158*s, y + 14*s, detailPaint);
            return;
        }

        if ("car_electric".equals(carId)) {
            detailPaint.setColor(Color.argb(210, 0, 170, 210));
            detailPaint.setStrokeWidth(1.2f * s);
            c.drawLine(x + 28*s, y + 30*s, x + 145*s, y + 27*s, detailPaint);
            c.drawLine(x + 143*s, y + 27*s, x + 158*s, y + 20*s, detailPaint);
            return;
        }

        boolean lowSportsCar = "car_super".equals(carId)
                || "car_hyper".equals(carId)
                || "car_modern_super".equals(carId);
        float doorTop = lowSportsCar ? 12*s : 16*s;
        float doorBottom = lowSportsCar ? 29*s : 34*s;
        float front = lowSportsCar ? 130*s : 125*s;

        // Doors and the shoulder crease make the side profile read as a real body panel.
        c.drawLine(x + 48*s, y + doorTop, x + 51*s, y + doorBottom, detailPaint);
        c.drawLine(x + 51*s, y + doorBottom, x + 105*s, y + doorBottom - 1*s, detailPaint);
        c.drawLine(x + 105*s, y + doorBottom - 1*s, x + 111*s, y + doorTop + 2*s, detailPaint);
        c.drawLine(x + 18*s, y + 24*s, x + front*s/1.0f, y + 22*s, detailPaint);

        detailPaint.setStyle(Paint.Style.FILL);
        detailPaint.setColor(Color.argb(190, 24, 24, 24));
        c.drawRoundRect(x + 70*s, y + doorTop + 2*s, x + 79*s, y + doorTop + 3.3f*s,
                1*s, 1*s, detailPaint);

        // Dark aerodynamic intake below the door, with a small painted edge.
        if (lowSportsCar) {
            c.drawRoundRect(x + 72*s, y + 28*s, x + 112*s, y + 32*s,
                    2*s, 2*s, detailPaint);
            detailPaint.setColor(Color.argb(130, 255, 255, 255));
            detailPaint.setStyle(Paint.Style.STROKE);
            detailPaint.setStrokeWidth(0.8f*s);
            c.drawLine(x + 76*s, y + 29*s, x + 108*s, y + 29*s, detailPaint);
        }

        detailPaint.setStyle(Paint.Style.STROKE);
        detailPaint.setStrokeWidth(0.9f*s);
        detailPaint.setColor(Color.argb(115, 255, 255, 255));
        c.drawLine(x + 34*s, y + 8*s, x + 91*s, y + 6*s, detailPaint);
    }

    private void drawDetailedWheel(Canvas c, float cx, float cy, float r, float angleDeg, int spokes, int rimAccent) {
        tirePaint.setColor(Color.parseColor("#111111"));
        c.drawCircle(cx, cy, r, tirePaint);
        tirePaint.setStyle(Paint.Style.STROKE); tirePaint.setStrokeWidth(r*0.12f);
        tirePaint.setColor(Color.parseColor("#1A1A1A"));
        c.drawCircle(cx, cy, r*0.88f, tirePaint);
        tirePaint.setStyle(Paint.Style.FILL);
        brakePaint.setColor(Color.parseColor("#505050"));
        c.drawCircle(cx, cy, r*0.72f, brakePaint);
        caliperPaint.setColor(Color.parseColor("#D32F2F"));
        c.drawCircle(cx+r*0.36f, cy, r*0.28f, caliperPaint);
        rimPaint.setColor(blendColor(Color.parseColor("#D8D8D8"), rimAccent, 0.18f));
        c.drawCircle(cx, cy, r*0.65f, rimPaint);
        rimPaint.setColor(Color.parseColor("#A8A8A8"));
        c.drawCircle(cx, cy, r*0.52f, rimPaint);
        c.save();
        c.rotate(angleDeg, cx, cy);
        spokePaint.setColor(Color.parseColor("#C8C8C8"));
        spokePaint.setStrokeWidth(r*(spokes<=5?0.14f:0.09f));
        for (int i=0;i<spokes;i++) {
            double rad = Math.toRadians(i*360.0/spokes);
            float ex = cx + r*0.60f*(float)Math.sin(rad);
            float ey = cy - r*0.60f*(float)Math.cos(rad);
            c.drawLine(cx, cy, ex, ey, spokePaint);
        }
        c.restore();
        rimPaint.setColor(Color.parseColor("#888888")); c.drawCircle(cx, cy, r*0.12f, rimPaint);
        rimPaint.setColor(Color.parseColor("#CCCCCC")); c.drawCircle(cx, cy, r*0.07f, rimPaint);
    }

    private void drawMirror(Canvas c, float x, float y, float s) {
        body2Paint.setColor(Color.parseColor("#222222"));
        c.drawRoundRect(x, y, x+9*s, y+6*s, 2*s, 2*s, body2Paint);
        glassPaint.setColor(Color.argb(180, 18, 32, 48));
        c.drawRoundRect(x+1*s, y+1*s, x+8*s, y+5*s, 1*s, 1*s, glassPaint);
        glassPaint.setColor(Color.argb(210, 18, 32, 48));
    }

    private void drawLEDHeadlight(Canvas c, float x, float y, float w, float h, float s) {
        lightPaint.setColor(Color.parseColor("#EEEEEE"));
        c.drawRoundRect(x, y, x+w, y+h, 2*s, 2*s, lightPaint);
        lightPaint.setColor(Color.parseColor("#B3E5FC"));
        c.drawRoundRect(x+1*s, y+1*s, x+w-1*s, y+h*0.4f, 1*s, 1*s, lightPaint);
        lightPaint.setColor(Color.parseColor("#FFFDE7"));
        c.drawRoundRect(x+1*s, y+h*0.5f, x+w-1*s, y+h-1*s, 1*s, 1*s, lightPaint);
    }

    private void drawLEDTaillight(Canvas c, float x, float y, float w, float h, float s) {
        lightPaint.setColor(Color.parseColor("#880000"));
        c.drawRoundRect(x, y, x+w, y+h, 2*s, 2*s, lightPaint);
        lightPaint.setColor(Color.parseColor("#FF1744"));
        c.drawRoundRect(x+0.5f*s, y+1*s, x+w-0.5f*s, y+h*0.4f, 1*s, 1*s, lightPaint);
        lightPaint.setColor(Color.parseColor("#FF6D00"));
        c.drawRoundRect(x+0.5f*s, y+h*0.55f, x+w-0.5f*s, y+h-1*s, 1*s, 1*s, lightPaint);
    }

    private void drawCircleHeadlight(Canvas c, float cx, float cy, float r) {
        lightPaint.setColor(Color.parseColor("#BBBBBB")); c.drawCircle(cx, cy, r, lightPaint);
        lightPaint.setColor(Color.parseColor("#FFFDE7")); c.drawCircle(cx, cy, r*0.7f, lightPaint);
        lightPaint.setColor(Color.parseColor("#B3E5FC")); c.drawCircle(cx, cy, r*0.35f, lightPaint);
    }

    private void drawGrilleBars(Canvas c, float x, float y, float w, float h, float s, int bars) {
        detailPaint.setColor(Color.parseColor("#1A1A1A"));
        detailPaint.setStrokeWidth(1.5f*s);
        float step = h/(bars+1);
        for (int i=1;i<=bars;i++) c.drawLine(x, y+i*step, x+w, y+i*step, detailPaint);
        detailPaint.setStyle(Paint.Style.STROKE);
        c.drawRoundRect(x, y, x+w, y+h, 2*s, 2*s, detailPaint);
        detailPaint.setStyle(Paint.Style.STROKE);
    }

    private void drawBMWGrille(Canvas c, float x, float y, float s) {
        detailPaint.setColor(Color.parseColor("#1A1A1A"));
        detailPaint.setStyle(Paint.Style.STROKE); detailPaint.setStrokeWidth(1.5f*s);
        c.drawRoundRect(x,      y, x+6*s,  y+14*s, 3*s, 5*s, detailPaint);
        c.drawRoundRect(x+8*s, y, x+14*s, y+14*s, 3*s, 5*s, detailPaint);
    }

    private void drawSpoiler(Canvas c, float x, float y, float w, float h, float s, int col) {
        body2Paint.setColor(darken(col, 0.55f));
        c.drawRoundRect(x, y, x+w, y+h, 2*s, 2*s, body2Paint);
        c.drawRect(x+w*0.3f, y+h, x+w*0.36f, y+h+6*s, body2Paint);
        c.drawRect(x+w*0.64f, y+h, x+w*0.70f, y+h+6*s, body2Paint);
    }

    private void drawGTRWing(Canvas c, float x, float y, float s, int col) {
        body2Paint.setColor(darken(col, 0.5f));
        float baseY = y + 47*s;
        c.drawRoundRect(x-8*s, baseY-44*s, x+30*s, baseY-38*s, 2*s, 2*s, body2Paint);
        c.drawRoundRect(x-10*s, baseY-44*s, x-6*s,  baseY-26*s, 2*s, 2*s, body2Paint);
        c.drawRoundRect(x+28*s, baseY-44*s, x+32*s, baseY-26*s, 2*s, 2*s, body2Paint);
        c.drawRect(x+4*s,  baseY-38*s, x+7*s,  baseY-28*s, body2Paint);
        c.drawRect(x+18*s, baseY-38*s, x+21*s, baseY-28*s, body2Paint);
    }

    private void drawLamboWing(Canvas c, float x, float y, float s, int col) {
        body2Paint.setColor(darken(col, 0.45f));
        float baseY = y + 38*s;
        c.drawRoundRect(x-12*s, baseY-38*s, x+28*s, baseY-32*s, 2*s, 2*s, body2Paint);
        c.drawRect(x+2*s,  baseY-32*s, x+5*s,  baseY-22*s, body2Paint);
        c.drawRect(x+16*s, baseY-32*s, x+19*s, baseY-22*s, body2Paint);
    }

    private void drawExhaust(Canvas c, float cx, float cy, float s) {
        exhaustPaint.setColor(Color.parseColor("#3A3A3A"));
        c.drawOval(cx, cy, cx+5*s, cy+4*s, exhaustPaint);
        exhaustPaint.setColor(Color.parseColor("#222222"));
        c.drawOval(cx+1*s, cy+1*s, cx+4*s, cy+3*s, exhaustPaint);
    }

    // =========================================================================
    // Utility
    // =========================================================================

    private float getShadowWidth(String carId, Car.BodyType bodyType, float s) {
        if (bodyType == Car.BodyType.DRAGSTER) return 245*s;
        if ("car_hyper".equals(carId)||"car_electric".equals(carId)) return 182*s;
        if ("car_super".equals(carId)||"car_modern_super".equals(carId)) return 176*s;
        if ("car_suv".equals(carId)) return 172*s;
        return 162*s;
    }

    private int darken(int color, float factor) {
        return Color.argb(Color.alpha(color),
                Math.min(255, Math.round(Color.red(color)   * factor)),
                Math.min(255, Math.round(Color.green(color) * factor)),
                Math.min(255, Math.round(Color.blue(color)  * factor)));
    }

    private int blendColor(int a, int b, float t) {
        return Color.argb(255,
                (int)(Color.red(a)   + (Color.red(b)   - Color.red(a))   * t),
                (int)(Color.green(a) + (Color.green(b) - Color.green(a)) * t),
                (int)(Color.blue(a)  + (Color.blue(b)  - Color.blue(a))  * t));
    }

    // =========================================================================
    // Particle system
    // =========================================================================

    private void spawnParticles(CarPhysics physics, float x, float y, float s) {
        float h = 50*s, rWx = x+30*s, wheelY = y+h;
        if (physics.isWheelSpinning() && Math.random()<0.7) {
            Particle p = new Particle();
            p.x = rWx; p.y = wheelY - 4*s;
            p.vx = -140.0f*(0.8f+(float)Math.random()*0.5f);
            p.vy = -35.0f-(float)Math.random()*40.0f;
            p.size = 9.0f*s; p.alpha = 1.0f;
            smokeParticles.add(p);
        }
        if (physics.isExhaustPop() && Math.random()<0.85) {
            Particle p = new Particle();
            p.x = x-2*s; p.y = y+h-8*s;
            p.vx = -220.0f-(float)Math.random()*120.0f;
            p.vy = (float)(Math.random()*24.0-12.0);
            p.size = 10.0f*s; p.alpha = 1.0f;
            p.color = physics.isNitroActive()
                    ? Color.parseColor("#00E5FF") : Color.parseColor("#FF9100");
            flameParticles.add(p);
        }
    }

    private void renderParticles(Canvas canvas, float dt) {
        Iterator<Particle> si = smokeParticles.iterator();
        while (si.hasNext()) {
            Particle p = si.next();
            p.x += p.vx*dt; p.y += p.vy*dt; p.size += 26.0f*dt; p.alpha -= 1.9f*dt;
            if (p.alpha<=0) { si.remove(); continue; }
            smokePaint.setColor(Color.argb((int)(p.alpha*190),220,220,220));
            canvas.drawCircle(p.x, p.y, p.size, smokePaint);
        }
        Iterator<Particle> fi = flameParticles.iterator();
        while (fi.hasNext()) {
            Particle p = fi.next();
            p.x += p.vx*dt; p.y += p.vy*dt;
            p.size = Math.max(1.0f, p.size-14.0f*dt); p.alpha -= 3.8f*dt;
            if (p.alpha<=0) { fi.remove(); continue; }
            flamePaint.setColor(p.color); flamePaint.setAlpha((int)(p.alpha*255));
            canvas.drawCircle(p.x, p.y, p.size, flamePaint);
        }
    }
}
