package net.luneshine.connector.concurrency;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A pair of a lock and a condition.
 *
 * @param lock      the lock
 * @param condition the condition
 */
public record LockConditionPair(ReentrantLock lock, Condition condition) {
}