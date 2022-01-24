package tk.booky.offlineannouncer.util;
// Created by booky10 in OfflineAnnouncer (14:39 24.01.22)

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.ForwardingMap;
import reactor.util.annotation.NonNull;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ExpiringMap<K, V> extends ForwardingMap<K, V> {

    private final Map<K, V> mapView;

    public ExpiringMap(long minutes) {
        this(minutes, TimeUnit.MINUTES);
    }

    public ExpiringMap(long duration, TimeUnit unit) {
        mapView = Caffeine.newBuilder().expireAfterAccess(duration, unit).<K, V>build().asMap();
    }

    @NonNull
    @Override
    protected Map<K, V> delegate() {
        return mapView;
    }
}
