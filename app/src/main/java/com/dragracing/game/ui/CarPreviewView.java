package com.dragracing.game.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import com.dragracing.game.data.Car;
import com.dragracing.game.engine.CarPhysics;
import com.dragracing.game.render.CarRenderer;

public class CarPreviewView extends View {
    private final CarRenderer renderer;
    private final Paint groundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean grounded;
    private CarPhysics physics;

    public CarPreviewView(Context context, Car car) {
        super(context);
        renderer = new CarRenderer(context);
        groundPaint.setColor(Color.argb(150, 0, 0, 0));
        setCar(car);
    }

    public void setCar(Car car) {
        if (car != null) {
            this.physics = new CarPhysics(car);
        }
        invalidate();
    }

    public void setGrounded(boolean grounded) {
        this.grounded = grounded;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (physics == null) return;

        float width = getWidth();
        float height = getHeight();

        float scale = Math.min(width / 220.0f, height / 70.0f);
        float carWidth = 160.0f * scale;
        float carHeight = 45.0f * scale;
        float carX = (width - carWidth) / 2.0f;
        float carY = grounded ? height - 61.0f * scale : (height - carHeight) / 2.0f - 4.0f;

        canvas.drawOval(carX - 4.0f * scale, carY + 55.0f * scale,
            carX + 185.0f * scale, carY + 61.0f * scale, groundPaint);
        renderer.renderPreview(canvas, physics.getCar(), carX, carY, scale);
    }
}
