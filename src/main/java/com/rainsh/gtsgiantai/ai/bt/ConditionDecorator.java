package com.rainsh.gtsgiantai.ai.bt;

import com.rainsh.gtsgiantai.ai.GiantBrain;

/**
 * 条件装饰器：子节点仅当 condition 为 true 时才执行。
 * 用于"追逐状态禁止休眠"、"潜行玩家不唤醒"等拦截逻辑。
 */
public class ConditionDecorator extends Node {

    private final Node child;
    private final java.util.function.Predicate<GiantBrain> condition;

    public ConditionDecorator(String name, java.util.function.Predicate<GiantBrain> condition, Node child) {
        super(name);
        this.condition = condition;
        this.child = child;
    }

    @Override
    public Status tick(GiantBrain ctx) {
        if (!condition.test(ctx)) {
            child.reset(ctx);
            return Status.FAILURE;
        }
        return child.tick(ctx);
    }

    @Override
    public void reset(GiantBrain ctx) {
        child.reset(ctx);
    }
}
