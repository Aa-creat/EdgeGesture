package com.omarea.gesture

/**
 * 现代版无障碍服务入口，继承自原有服务核心。
 * 类名定为 ModernGestureService，严格避免以 "AccessibilityServiceGesture" 结尾，
 * 彻底解决旧版应用通过 endsWith("AccessibilityServiceGesture") 产生的误判混淆。
 */
class ModernGestureService : AccessibilityServiceGesture()
