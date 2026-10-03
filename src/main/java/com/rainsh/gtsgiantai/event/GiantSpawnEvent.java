package com.rainsh.gtsgiantai.event;

import com.rainsh.gtsgiantai.entity.GiantEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * 巨人生成事件（可取消）
 */
public class GiantSpawnEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final GiantEntity giant;
    private boolean cancelled = false;

    public GiantSpawnEvent(GiantEntity giant) {
        this.giant = giant;
    }

    public GiantEntity getGiant() {
        return giant;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
