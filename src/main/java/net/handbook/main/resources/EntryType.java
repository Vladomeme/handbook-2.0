package net.handbook.main.resources;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import net.handbook.main.resources.entry.*;

public enum EntryType {
    normal(Entry.class),
    position(PositionEntry.class),
    area(AreaEntry.class),
    trader(TraderEntry.class),
    waypoint(WaypointEntry.class);

    public final Class<? extends Entry> typeClass;

    EntryType(Class<? extends Entry> typeClass) {
        this.typeClass = typeClass;
    }

    public static <E extends Entry> TypeToken<Category<E>> typeToken(Class<E> typeClass) {
        return new TypeToken<Category<E>>() {}.where(new TypeParameter<>() {}, typeClass);
    }
}
