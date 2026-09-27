package com.jarves.mh.orchestrator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class HiveCoordinatorTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun assignmentWritesCanonicalRecordAndDedicatedInbox() {
        val hive = HiveCoordinator(temp.newFolder("project"))
        val task = hive.createTask(HiveTask(id = "task-auth", title = "Fix auth", description = "Add refresh locking"))

        val assigned = hive.assign(task.id, "dwight", "michael")

        assertTrue(assigned.isSuccess)
        assertEquals(HiveTaskStatus.IN_PROGRESS, hive.task(task.id)?.status)
        assertEquals("dwight", hive.task(task.id)?.assignedTo)
        assertEquals(listOf(task.id), hive.inbox("dwight").map { it.taskId })
        assertTrue(temp.root.walkTopDown().any { it.path.endsWith(".hive/tasks/task-auth.json") })
    }

    @Test fun anotherAgentCannotDoubleClaimTask() {
        val hive = HiveCoordinator(temp.newFolder("project"))
        hive.createTask(HiveTask(id = "task-one", title = "One", description = ""))
        assertTrue(hive.assign("task-one", "dwight").isSuccess)

        val claimedAgain = hive.assign("task-one", "jim")

        assertFalse(claimedAgain.isSuccess)
        assertEquals("dwight", hive.task("task-one")?.assignedTo)
    }

    @Test fun reviewResultCreatesReviewerMailboxAndPreservesOutboxReport() {
        val hive = HiveCoordinator(temp.newFolder("project"))
        hive.createTask(HiveTask(id = "task-one", title = "Fix auth", description = ""))
        hive.assign("task-one", "dwight")

        val submitted = hive.submitResult(HiveOutboxItem(
            taskId = "task-one", agentId = "dwight", status = HiveTaskStatus.DONE,
            summary = "Implemented mutex", filesChanged = listOf("src/Auth.kt"), reviewTargetAgentId = "jim",
            reviewFocus = "Check thread safety",
        ))

        assertTrue(submitted.isSuccess)
        assertEquals(HiveTaskStatus.REVIEW, hive.task("task-one")?.status)
        assertEquals(listOf("src/Auth.kt"), hive.task("task-one")?.artifacts)
        assertEquals(1, hive.outbox("dwight").size)
        assertEquals(1, hive.inbox("jim").size)
        assertTrue(hive.inbox("jim").single().title.startsWith("Review:"))
    }
}
