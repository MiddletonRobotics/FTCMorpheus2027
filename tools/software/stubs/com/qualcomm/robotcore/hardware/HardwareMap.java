package com.qualcomm.robotcore.hardware;
import java.util.HashMap;
import java.util.Map;
/** Desktop test fixture; never packaged into the APK. */
public class HardwareMap {
    private final Map<String,Object> devices = new HashMap<>();
    public <T> T get(Class<? extends T> cls, String name) { return cls.cast(devices.get(name)); }
    public void put(String name, Object value) { devices.put(name, value); }
}
