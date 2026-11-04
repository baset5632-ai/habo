package com.example

import com.example.ai.UniversalAiService
import com.example.model.JarvisAgentResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun startGreeting_matchesExactKurdishRule() {
        val greeting = JarvisAgentResponse.createStartGreeting(
            currentName = "",
            requireWakeWord = false
        )
        assertEquals("سڵاو، چیت ئەوێت بۆت بکەم؟", greeting.voiceReply)
        assertTrue(greeting.shouldExecute)
        assertEquals("CHAT_ONLY", greeting.command.type)
    }

    @Test
    fun dynamicNameExtraction_extractsKurdishName() {
        val service = UniversalAiService()
        val name1 = service.extractAssignedNameFromText("تۆ ناوت کارزانـە")
        assertEquals("کارزان", name1)

        val name2 = service.extractAssignedNameFromText("ناوی تۆ زانایە")
        assertEquals("زانا", name2)
    }

    @Test
    fun wakeWordLockDetection_detectsKurdishLockAndUnlock() {
        val service = UniversalAiService()
        val locked = service.detectWakeWordToggle("تا ناوت نەهێنم هیچ کارێک مەکە")
        assertEquals(true, locked)

        val unlocked = service.detectWakeWordToggle("قوفڵی ناو لاببە و هەموو کات وەڵام بدەرەوە")
        assertEquals(false, unlocked)
    }

    @Test
    fun ignoreResponse_producesStrictIgnoreJson() {
        val ignored = JarvisAgentResponse.createIgnoreResponse(
            currentName = "کارزان",
            requireWakeWord = true
        )
        assertFalse(ignored.shouldExecute)
        assertEquals("IGNORE", ignored.command.type)
        assertEquals("کارزان", ignored.assignedName)
        assertTrue(ignored.requireWakeWord)
    }
}
