package com.rainsh.gtsgiantai.ai.bt;

/**
 * 行为树节点执行状态
 */
public enum Status {
    /** 执行成功 */
    SUCCESS,
    /** 执行失败 */
    FAILURE,
    /** 执行中（多tick持续执行） */
    RUNNING
}
