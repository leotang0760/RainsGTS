package com.rainsh.gtsgiantai.ai.bt;

import com.rainsh.gtsgiantai.ai.GiantBrain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 顺序节点：依次执行子节点，全部 SUCCESS 才返回 SUCCESS。
 * 任一子节点 FAILURE 立即整体 FAILURE；RUNNING 保持运行状态。
 */
public class Sequence extends Node {

    protected final List<Node> children = new ArrayList<>();
    private int runningIndex = -1;

    public Sequence(String name, Node... children) {
        super(name);
        this.children.addAll(Arrays.asList(children));
    }

    public Sequence add(Node child) {
        this.children.add(child);
        return this;
    }

    @Override
    public Status tick(GiantBrain ctx) {
        int start = runningIndex >= 0 ? runningIndex : 0;
        for (int i = start; i < children.size(); i++) {
            Status s = children.get(i).tick(ctx);
            if (s == Status.RUNNING) {
                runningIndex = i;
                return Status.RUNNING;
            }
            if (s == Status.FAILURE) {
                reset(ctx);
                return Status.FAILURE;
            }
            runningIndex = -1;
        }
        reset(ctx);
        return Status.SUCCESS;
    }

    @Override
    public void reset(GiantBrain ctx) {
        runningIndex = -1;
        for (Node child : children) child.reset(ctx);
    }
}
