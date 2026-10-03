package com.rainsh.gtsgiantai.ai.bt;

import com.rainsh.gtsgiantai.ai.GiantBrain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 选择器：按优先级从上到下执行子节点。
 * 第一个返回 SUCCESS 或 RUNNING 即停止；全部 FAILURE 才返回 FAILURE。
 * 用于行为树根节点：追逐 > 警戒 > 空闲 > 休眠。
 */
public class Selector extends Node {

    protected final List<Node> children = new ArrayList<>();

    public Selector(String name, Node... children) {
        super(name);
        this.children.addAll(Arrays.asList(children));
    }

    public Selector add(Node child) {
        this.children.add(child);
        return this;
    }

    @Override
    public Status tick(GiantBrain ctx) {
        for (Node child : children) {
            Status s = child.tick(ctx);
            if (s != Status.FAILURE) {
                if (s == Status.SUCCESS) {
                    for (Node other : children) {
                        if (other != child) other.reset(ctx);
                    }
                }
                return s;
            }
        }
        return Status.FAILURE;
    }

    @Override
    public void reset(GiantBrain ctx) {
        for (Node child : children) child.reset(ctx);
    }
}
