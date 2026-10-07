package com.dragracing.game.data;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlayerData {
    private static final String PREF_NAME = "DragRacingPrefs";
    private static final String KEY_CASH = "player_cash";
    private static final String KEY_CURRENT_CAR = "current_car_id";
    private static final String KEY_OWNED_CARS = "owned_cars";
    private static final String KEY_CAREER_STAGE = "career_stage";
    private static final String KEY_CAREER_STEP = "career_step";
    private static final String KEY_BEST_ET_1_4 = "best_et"; // Existing key for 1/4 mile
    private static final String KEY_BEST_ET_1_2 = "best_et_1_2";
    private static final String KEY_BEST_ET_1 = "best_et_1";
    private static final String KEY_BEST_ET_2 = "best_et_2";

    private static PlayerData instance;
    private final SharedPreferences prefs;

    private int cash;
    private String currentCarId;
    private final Set<String> ownedCarIds;
    private int careerStage;
    private int careerStep; // Progress within the stage
    private final List<Car> allCars;

    private PlayerData(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        allCars = CarDatabase.createDefaultCars();

        // Initial default values
        cash = prefs.getInt(KEY_CASH, 2000); // Give $2,000 starting cash
        currentCarId = prefs.getString(KEY_CURRENT_CAR, "car_toyota_86gt");
        careerStage = prefs.getInt(KEY_CAREER_STAGE, 1);
        careerStep = prefs.getInt(KEY_CAREER_STEP, 0);

        ownedCarIds = new HashSet<>(prefs.getStringSet(KEY_OWNED_CARS, new HashSet<>()));
        migrateLegacyCarData();

        loadCarUpgradesAndColors();
    }

    public static synchronized PlayerData getInstance(Context context) {
        if (instance == null) {
            instance = new PlayerData(context);
        }
        return instance;
    }

    private void migrateLegacyCarData() {
        if ("car_starter".equals(currentCarId) || getCarByIdOrNull(currentCarId) == null) {
            currentCarId = "car_toyota_86gt";
        }

        ownedCarIds.remove("car_starter");
        ownedCarIds.removeIf(carId -> getCarByIdOrNull(carId) == null);
        ownedCarIds.add("car_toyota_86gt");
    }

    private Car getCarByIdOrNull(String id) {
        for (Car car : allCars) {
            if (car.getId().equals(id)) return car;
        }
        return null;
    }

    private String getCarPreferencePrefix(Car car) {
        String carId = car.getId();
        if (carId.startsWith("car_")) {
            carId = carId.substring(4);
        }
        return "car_" + carId + "_";
    }

    private String getLegacyCarPreferencePrefix(Car car) {
        return "car_" + car.getId() + "_";
    }

    private int getCarPreferenceInt(Car car, String key, int defaultValue) {
        String prefix = getCarPreferencePrefix(car);
        String legacyPrefix = getLegacyCarPreferencePrefix(car);
        if (prefs.contains(prefix + key)) {
            return prefs.getInt(prefix + key, defaultValue);
        }
        if (prefs.contains(legacyPrefix + key)) {
            return prefs.getInt(legacyPrefix + key, defaultValue);
        }
        return defaultValue;
    }

    private void loadCarUpgradesAndColors() {
        for (Car car : allCars) {
            String prefix = getCarPreferencePrefix(car);
            String legacyPrefix = getLegacyCarPreferencePrefix(car);
            car.setEngineLevel(getCarPreferenceInt(car, "engine", 0));
            car.setTurboLevel(getCarPreferenceInt(car, "turbo", 0));
            car.setNitroLevel(getCarPreferenceInt(car, "nitro", 0));
            car.setTiresLevel(getCarPreferenceInt(car, "tires", 0));
            car.setGearboxLevel(getCarPreferenceInt(car, "gearbox", 0));
            car.setWeightLevel(getCarPreferenceInt(car, "weight", 0));
            int savedColor = -1;
            if (prefs.contains(prefix + "color")) {
                savedColor = prefs.getInt(prefix + "color", -1);
            } else if (prefs.contains(legacyPrefix + "color")) {
                savedColor = prefs.getInt(legacyPrefix + "color", -1);
            }
            if (savedColor != -1) {
                car.setColor(savedColor);
            }
        }
    }

    public void saveCar(Car car) {
        String prefix = getCarPreferencePrefix(car);
        prefs.edit()
                .putInt(prefix + "engine", car.getEngineLevel())
                .putInt(prefix + "turbo", car.getTurboLevel())
                .putInt(prefix + "nitro", car.getNitroLevel())
                .putInt(prefix + "tires", car.getTiresLevel())
                .putInt(prefix + "gearbox", car.getGearboxLevel())
                .putInt(prefix + "weight", car.getWeightLevel())
                .putInt(prefix + "color", car.getColor())
                .apply();
    }

    public void save() {
        prefs.edit()
                .putInt(KEY_CASH, cash)
                .putString(KEY_CURRENT_CAR, currentCarId)
                .putStringSet(KEY_OWNED_CARS, ownedCarIds)
                .putInt(KEY_CAREER_STAGE, careerStage)
                .putInt(KEY_CAREER_STEP, careerStep)
                .apply();
    }

    public int getCash() {
        return cash;
    }

    public void addCash(int amount) {
        this.cash += amount;
        save();
    }

    public boolean spendCash(int amount) {
        if (this.cash >= amount) {
            this.cash -= amount;
            save();
            return true;
        }
        return false;
    }

    public List<Car> getAllCars() {
        return allCars;
    }

    public Car getCarById(String id) {
        for (Car car : allCars) {
            if (car.getId().equals(id)) return car;
        }
        return allCars.get(0);
    }

    public Car getCurrentCar() {
        return getCarById(currentCarId);
    }

    public void setCurrentCarId(String carId) {
        this.currentCarId = carId;
        save();
    }

    public boolean isCarOwned(String carId) {
        return ownedCarIds.contains(carId);
    }

    public boolean buyCar(Car car) {
        if (spendCash(car.getPrice())) {
            ownedCarIds.add(car.getId());
            setCurrentCarId(car.getId());
            save();
            return true;
        }
        return false;
    }

    public int getCareerStage() {
        return careerStage;
    }

    public int getCareerStep() {
        return careerStep;
    }

    public void advanceCareer() {
        this.careerStep++;
        if (this.careerStep >= 5) { // 5 races per stage: 4 regular + 1 boss
            this.careerStep = 0;
            this.careerStage++;
        }
        save();
    }

    public void advanceCareerStage() {
        this.careerStage++;
        this.careerStep = 0;
        save();
    }

    private String getDistanceKey(double distance) {
        if (Math.abs(distance - 402.336) < 1.0) return KEY_BEST_ET_1_4;
        if (Math.abs(distance - 804.672) < 1.0) return KEY_BEST_ET_1_2;
        if (Math.abs(distance - 1609.34) < 1.0) return KEY_BEST_ET_1;
        if (Math.abs(distance - 3218.68) < 1.0) return KEY_BEST_ET_2;
        return KEY_BEST_ET_1_4;
    }

    public float getBestET(double distance) {
        return prefs.getFloat(getDistanceKey(distance), 0.0f);
    }

    // Keep compatibility
    public float getBestET() {
        return getBestET(402.336);
    }

    public void updateBestET(double distance, float et) {
        String key = getDistanceKey(distance);
        float currentBest = prefs.getFloat(key, 0.0f);
        if (currentBest <= 0.001f || et < currentBest) {
            prefs.edit().putFloat(key, et).apply();
        }
    }
}
