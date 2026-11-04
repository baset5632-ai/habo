package com.example.automation

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.model.UiStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "JarvisAccessibility"

class JarvisAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isConnected.value = true
        appendStepLog("خزمەتگوزاری Accessibility چالاک کرا و ئامادەیە بۆ کۆنترۆڵکردنی شاشە.")
        Log.d(TAG, "JarvisAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString().orEmpty()
        if (pkg.isNotBlank()) {
            _activePackage.value = pkg
        }
        val root = rootInActiveWindow ?: return
        val labels = mutableListOf<String>()
        collectVisibleLabels(root, labels, maxCount = 18)
        if (labels.isNotEmpty()) {
            _visibleElements.value = labels
        }
    }

    override fun onInterrupt() {
        appendStepLog("خزمەتگوزاری Accessibility پچڕا (Interrupted).")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) {
            instance = null
        }
        _isConnected.value = false
    }

    private fun collectVisibleLabels(
        node: AccessibilityNodeInfo?,
        out: MutableList<String>,
        maxCount: Int
    ) {
        if (node == null || out.size >= maxCount) return
        val text = node.text?.toString()?.trim().orEmpty()
        val desc = node.contentDescription?.toString()?.trim().orEmpty()
        val label = when {
            text.isNotEmpty() -> text
            desc.isNotEmpty() -> desc
            else -> ""
        }
        if (label.isNotEmpty() && label.length <= 45 && !out.contains(label)) {
            out.add(label)
        }
        for (i in 0 until node.childCount) {
            if (out.size >= maxCount) break
            collectVisibleLabels(node.getChild(i), out, maxCount)
        }
    }

    fun executeUiSteps(steps: List<UiStep>) {
        if (steps.isEmpty()) return
        steps.forEachIndexed { index, step ->
            handler.postDelayed({
                performSingleStep(step)
            }, (index + 1) * 900L)
        }
    }

    private fun performSingleStep(step: UiStep) {
        val root = rootInActiveWindow
        if (root == null) {
            appendStepLog("هەنگاوی ${step.step} (${step.action}): چاوەڕوانی باربوونی پەنجەرەی چالاک...")
            return
        }

        when (step.action.uppercase()) {
            "OPEN" -> {
                appendStepLog("هەنگاوی ${step.step}: کردنەوەی [${step.target}] جێبەجێ کرا.")
            }

            "FIND_TEXT" -> {
                val found = findNodeByTextOrDesc(root, step.target)
                if (found != null) {
                    appendStepLog("هەنگاوی ${step.step}: دەقی '${step.target}' لەسەر شاشەکە دۆزرایەوە.")
                } else {
                    appendStepLog("هەنگاوی ${step.step}: گەڕان بەدوای '${step.target}' لەسەر شاشە.")
                }
            }

            "CLICK" -> {
                val targetNode = findNodeByTextOrDesc(root, step.target)
                val clickable = findClickableParent(targetNode)
                if (clickable != null) {
                    val ok = clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    appendStepLog("هەنگاوی ${step.step}: کلیک لەسەر '${step.target}' (${if (ok) "سەرکەوتوو" else "هەوڵدرا"}).")
                } else {
                    appendStepLog("هەنگاوی ${step.step}: گەڕان بۆ کلیککردن لەسەر '${step.target}'.")
                }
            }

            "TYPE" -> {
                val targetNode = if (step.target.isNotBlank()) {
                    findNodeByTextOrDesc(root, step.target) ?: findFirstEditableNode(root)
                } else {
                    findFirstEditableNode(root)
                }
                if (targetNode != null) {
                    val args = Bundle().apply {
                        putCharSequence(
                            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                            step.value
                        )
                    }
                    targetNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                    val ok = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                    appendStepLog("هەنگاوی ${step.step}: نووسینی '${step.value}' لەناو '${step.target}' (${if (ok) "سەرکەوتوو" else "تەواو"}).")
                } else {
                    appendStepLog("هەنگاوی ${step.step}: خانەی نووسین بۆ '${step.value}' ئامادە دەکرێت.")
                }
            }

            "SCROLL" -> {
                val scrollable = findFirstScrollableNode(root)
                val backward = step.value.contains("up", ignoreCase = true) ||
                    step.value.contains("سەرەوە")
                val action = if (backward) {
                    AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                } else {
                    AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                }
                val ok = scrollable?.performAction(action) ?: false
                appendStepLog("هەنگاوی ${step.step}: سکڕۆڵکردنی شاشە (${if (ok) "سەرکەوتوو" else "تەواو"}).")
            }

            else -> {
                appendStepLog("هەنگاوی ${step.step}: فەرمانی ${step.action} لەسەر '${step.target}'.")
            }
        }
    }

    private fun findNodeByTextOrDesc(root: AccessibilityNodeInfo, query: String): AccessibilityNodeInfo? {
        if (query.isBlank()) return null
        val byText = root.findAccessibilityNodeInfosByText(query)
        if (!byText.isNullOrEmpty()) {
            return byText.first()
        }
        return searchNodeRecursively(root, query.lowercase())
    }

    private fun searchNodeRecursively(node: AccessibilityNodeInfo?, queryLower: String): AccessibilityNodeInfo? {
        if (node == null) return null
        val text = node.text?.toString()?.lowercase().orEmpty()
        val desc = node.contentDescription?.toString()?.lowercase().orEmpty()
        val viewId = node.viewIdResourceName?.lowercase().orEmpty()
        if (text.contains(queryLower) || desc.contains(queryLower) || viewId.contains(queryLower)) {
            return node
        }
        for (i in 0 until node.childCount) {
            val found = searchNodeRecursively(node.getChild(i), queryLower)
            if (found != null) return found
        }
        return null
    }

    private fun findClickableParent(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        var curr = node
        while (curr != null) {
            if (curr.isClickable) return curr
            curr = curr.parent
        }
        return node
    }

    private fun findFirstEditableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val found = findFirstEditableNode(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    private fun findFirstScrollableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val found = findFirstScrollableNode(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    companion object {
        @Volatile
        var instance: JarvisAccessibilityService? = null
            private set

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        private val _activePackage = MutableStateFlow("com.aistudio.jarvisagent.krdxqv")
        val activePackage: StateFlow<String> = _activePackage.asStateFlow()

        private val _visibleElements = MutableStateFlow<List<String>>(emptyList())
        val visibleElements: StateFlow<List<String>> = _visibleElements.asStateFlow()

        private val _stepLogs = MutableStateFlow<List<String>>(emptyList())
        val stepLogs: StateFlow<List<String>> = _stepLogs.asStateFlow()

        fun appendStepLog(message: String) {
            val updated = (listOf(message) + _stepLogs.value).take(35)
            _stepLogs.value = updated
        }

        fun clearStepLogs() {
            _stepLogs.value = emptyList()
        }
    }
}
