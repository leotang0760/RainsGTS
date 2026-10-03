package com.rainsh.gtsgiantai.ai.bt;

import com.rainsh.gtsgiantai.ai.GiantBrain;

/**
 * 行为树节点抽象基类。
 * 所有分支/条件/动作均继承此类。
 */
public abstract class Node {

    protected final String name;

    protected Node(String name) {
        this.name = name;
    }

    /** 执行一次tick，返回状态 */
    public abstract Status tick(GiantBrain ctx);

    /** 节点结束（SUCCESS/FAILURE）时被调用，用于清理运行中状态 */
    public void reset(GiantBrain ctx) {
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
