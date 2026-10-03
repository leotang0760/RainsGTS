package com.rainsh.gtsgiantai.event;

import com.rainsh.gtsgiantai.entity.GiantEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * 巨人与玩家交互事件（捧起/戏弄/轻拨等）
 */
public class GiantInteractEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final GiantEntity giant;
    private final Player player;
    private final Action action;

    public enum Action {
        GRAB,       // 捧起
        TEASE,      // 戏弄/轻拨
        STEP_OVER,  // 跨过
        BLOCK_PATH, // 挡路
        CRUSH_WARNING // 碾压警告
    }

    public GiantInteractEvent(GiantEntity giant, Player player, Action action) {
        this.giant = giant;
        this.player = player;
        this.action = action;
    }

    public GiantEntity getGiant() {
        return giant;
    }

    public Player getPlayer() {
        return player;
    }

    public Action getAction() {
        return action;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
