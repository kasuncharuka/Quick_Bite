package lk.kcd.quickbite.helper;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class ShakeSensorManager implements SensorEventListener {

    private static final float SHAKE_THRESHOLD_GRAVITY = 2.7f;
    private static final int SHAKE_SLOP_TIME_MS        = 500;
    private static final int SHAKE_COUNT_RESET_TIME_MS = 3000;

    public interface OnShakeListener {
        void onShake(int count);
    }

    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    private OnShakeListener listener;

    private long shakeTimestamp;
    private int shakeCount;

    public ShakeSensorManager(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void setOnShakeListener(OnShakeListener listener) {
        this.listener = listener;
    }

    public void start() {
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_UI);
        }
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (listener == null) return;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float gForce = (float) Math.sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH;

        if (gForce < SHAKE_THRESHOLD_GRAVITY) return;

        long now = System.currentTimeMillis();


        if (shakeTimestamp + SHAKE_SLOP_TIME_MS > now) return;


        if (shakeTimestamp + SHAKE_COUNT_RESET_TIME_MS < now) {
            shakeCount = 0;
        }

        shakeTimestamp = now;
        shakeCount++;
        listener.onShake(shakeCount);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
