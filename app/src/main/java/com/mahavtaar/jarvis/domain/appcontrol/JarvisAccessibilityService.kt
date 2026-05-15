package com.mahavtaar.jarvis.domain.appcontrol

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class JarvisAccessibilityService : AccessibilityService() {

    companion object {
        var instance: JarvisAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { }

    override fun onInterrupt() { }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    fun typeText(text: String, targetPackage: String? = null): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val editTexts = findEditTexts(rootNode)

        val targetNode = editTexts.firstOrNull {
            targetPackage == null || it.packageName?.toString() == targetPackage
        } ?: editTexts.firstOrNull()

        return if (targetNode != null) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        } else {
            false
        }
    }

    fun clickByContentDescription(desc: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val node = findNodeByContentDescription(rootNode, desc)
        return node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }

    fun readScreenContent(): String {
        val rootNode = rootInActiveWindow ?: return "Screen content unavailable."
        val textBuilder = StringBuilder()
        extractText(rootNode, textBuilder)
        return textBuilder.toString().trim().takeIf { it.isNotBlank() } ?: "No readable text found on screen."
    }

    fun performGlobalBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun performGlobalHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun performGlobalRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)

    private fun findEditTexts(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val list = mutableListOf<AccessibilityNodeInfo>()
        if (node.isEditable) {
            list.add(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) list.addAll(findEditTexts(child))
        }
        return list
    }

    private fun findNodeByContentDescription(node: AccessibilityNodeInfo, desc: String): AccessibilityNodeInfo? {
        if (node.contentDescription?.toString()?.equals(desc, ignoreCase = true) == true) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val result = findNodeByContentDescription(child, desc)
                if (result != null) return result
            }
        }
        return null
    }

    private fun extractText(node: AccessibilityNodeInfo, builder: StringBuilder) {
        if (node.text != null) {
            builder.append(node.text).append(". ")
        } else if (node.contentDescription != null) {
            builder.append(node.contentDescription).append(". ")
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) extractText(child, builder)
        }
    }
}
