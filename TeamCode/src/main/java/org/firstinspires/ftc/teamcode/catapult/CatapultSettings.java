package org.firstinspires.ftc.teamcode.catapult;

/** No measured limits were available during desktop development. Do not enable by guessing. */
public final class CatapultSettings {
    private CatapultSettings() { }
    public static CatapultCycle.Config measuredConfiguration() {
        CatapultCycle.Config config = new CatapultCycle.Config();
        config.enabled = false;
        config.safeAtZeroPowerVerified = false;
        // Fill travel, power, current and timing fields from guarded hardware characterization.
        // Home is encoder zero, established explicitly in INIT; both sides must share units/sign.
        return config;
    }
}
