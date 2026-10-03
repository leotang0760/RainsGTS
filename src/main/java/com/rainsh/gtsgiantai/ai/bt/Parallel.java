package com.rainsh.gtsgiantai.ai.bt;

import com.rainsh.gtsgiantai.ai.GiantBrain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 并行节点：所有子节点每tick全部执行（如 巡逻 + 无聊值累加 + 环境扫描 同时进行）。
 * 策略：全部 SUCCESS → SUCCESS；任一 FAILURE → 整体 FAILURE（默认）；
 * 其余情况 RUNNING。
 */
public class Parallel extends Node {

    /** FAILURE策略：任一失败即整体失败 */
    public static final int POLICY_FAIL_ON_ANY = 0;
    /** FAILURE策略：全部失败才整体失败 */
    public static final int POLICY_FAIL_ON_ALL = 1;

    private final int failPolicy;
    protected final List<Node> children = new ArrayList<>();

    public Parallel(String name, int failPolicy, Node... children) {
        super(name);
        this.failPolicy = failPolicy;
        this.children.addAll(Arrays.asList(children));
    }

    public Parallel add(Node child) {
        this.children.add(child);
        return this;
    }

    @Override
    public Status tick(GiantBrain ctx) {
        boolean anyRunning = false;
        int success = 0;
        int failed = 0;
        for (Node child : children) {
            Status s = child.tick(ctx);
            if (s == Status.RUNNING) anyRunning = true;
            else if (s == Status.SUCCESS) success++;
            else failed++;
        }
        if (failPolicy == POLICY_FAIL_ON_ANY && failed > 0) {
            reset(ctx);
            return Status.FAILURE;
        }
        if (failPolicy == POLICY_FAIL_ON_ALL && failed == children.size()) {
            reset(ctx);
            return Status.FAILURE;
        }
        if (success == children.size()) {
            reset(ctx);
            return Status.SUCCESS;
        }
        return anyRunning ? Status.RUNNING : Status.SUCCESS;
    }

    @Override
    public void reset(GiantBrain ctx) {
        for (Node child : children) child.reset(ctx);
    }
}
