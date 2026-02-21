package com.nony545.pocketpastures.shippingbin.tier;

public enum ShippingTier {

    WOOD(1, false, Schedule.DAILY_SUNRISE),
    IRON(2, false, Schedule.DAILY_SUNRISE),
    GOLD(4, false, Schedule.DAILY_SUNRISE),
    DIAMOND(4, true, Schedule.DAILY_SUNRISE),
    NETHERITE(4, true, Schedule.TWICE_DAILY);

    public final int rows;
    public final boolean automatable;
    public final Schedule schedule;

    ShippingTier(int rows, boolean automatable, Schedule schedule) {
        this.rows = rows;
        this.automatable = automatable;
        this.schedule = schedule;
    }

    public int slots() {
        return rows * 9;
    }

    public enum Schedule {
        DAILY_SUNRISE,
        TWICE_DAILY
    }
}