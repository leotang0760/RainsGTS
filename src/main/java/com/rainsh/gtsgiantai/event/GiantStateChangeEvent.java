package com.rainsh.gtsgiantai.event;

import com.rainsh.gtsgiantai.entity.GiantEntity;
import com.rainsh.gtsgiantai.entity.GiantState;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * 巨人状态切换事件（SLEEP/IDLE/ALERT/CHASE）
 */
public class GiantStateChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final GiantEntity giant;
    private final GiantState newState;

    public GiantStateChangeEvent(GiantEntity giant, GiantState newState) {
        this.giant = giant;
        this.newState = newState;
    }

    public GiantEntity getGiant() {
        return giant;
    }

    public GiantState getNewState() {
        return newState;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
