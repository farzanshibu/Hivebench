package com.jarves.mh.orchestrator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentLifecycleTest {
    @Test fun promptIsReadyOnlyAfterOutputQuiesces() {
        val detector = PromptQuiescenceDetector(quiescenceMillis = 500)
        detector.onOutput("\u001B[32mworker@repo$ \u001B[0m", 1_000)

        assertFalse(detector.isReady(1_499))
        assertTrue(detector.isReady(1_500))
    }

    @Test fun nudgeIsWithheldWhileWorkerIsBusy() {
        val worker = WorkerLifecycle("dwight", PromptQuiescenceDetector(500))
        val writes = mutableListOf<String>()
        worker.wake(); worker.ptySpawned()
        worker.onTerminalOutput("$ ", 1_000)

        assertFalse(worker.deliverIfReady(true, writes::add))
        assertTrue(worker.tick(1_500))
        assertTrue(worker.deliverIfReady(true, writes::add))
        assertEquals(WorkerState.BUSY, worker.state)
        assertEquals(1, writes.size)
        assertTrue(writes.single().contains(".hive/inbox/dwight.json"))
    }
}
