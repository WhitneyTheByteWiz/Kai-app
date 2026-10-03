package com.kai.assistant.core.tools

import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable

/**
 * Result of a tool execution
 */
@Serializable
sealed class ToolResult {
    data class Success(val data: Any?, val displayText: String? = null) : ToolResult()
    data class Failure(val error: ToolError, val displayText: String? = null) : ToolResult()
    data class Cancelled(val reason: String = "Tool execution cancelled") : ToolResult()
    data class Pending(val operationId: String, val progress: Float = 0f) : ToolResult()
}

/**
 * Tool execution errors
 */
sealed class ToolError(open val errorMessage: String) : Exception() {
    data class PermissionDenied(override val errorMessage: String, val permission: String) : ToolError(errorMessage)
    data class NotFound(override val errorMessage: String, val resource: String) : ToolError(errorMessage)
    data class ExecutionFailed(override val errorMessage: String) : ToolError(errorMessage)
    data class ValidationError(override val errorMessage: String, val field: String? = null) : ToolError(errorMessage)
    data class NotSupported(override val errorMessage: String) : ToolError(errorMessage)
    data class Cancelled(override val errorMessage: String) : ToolError(errorMessage)
    data class Timeout(override val errorMessage: String) : ToolError(errorMessage)
    data class Unknown(override val errorMessage: String) : ToolError(errorMessage)
}

/**
 * Tool confirmation request for sensitive operations
 */
@Serializable
data class ToolConfirmation(
    val title: String,
    val message: String,
    val confirmText: String = "Confirm",
    val cancelText: String = "Cancel",
    val isDestructive: Boolean = false,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Tool definition with all metadata
 */
interface Tool {
    val id: String
    val name: String
    val description: String
    val category: ToolCategory
    val inputSchema: Map<String, String> // JSON Schema as string map
    val requiredPermissions: List<String>
    val requiresConfirmation: Boolean
    val confirmation: ToolConfirmation?
    val isAvailable: Boolean
    val version: String

    /**
     * Execute the tool with given arguments
     */
    suspend fun execute(
        args: Map<String, Any>,
        scope: CoroutineScope,
        onProgress: (Float) -> Unit = {}
    ): ToolResult

    /**
     * Validate input arguments against schema
     */
    fun validate(args: Map<String, Any>): ValidationResult

    /**
     * Cancel ongoing execution
     */
    fun cancel(): Boolean
}

enum class ToolCategory {
    APPLICATIONS, COMMUNICATION, DEVICE_CONTROLS, BROWSER, INTERNET,
    CALENDAR, ALARMS, MEDIA, CONTACTS, MAPS, NOTIFICATIONS,
    FILES, SYSTEM_SETTINGS, PRODUCTIVITY, SEARCH, AI_TOOLS
}

sealed class ValidationResult {
    data class Valid(val sanitizedArgs: Map<String, Any>) : ValidationResult()
    data class Invalid(val errors: List<ValidationError>) : ValidationResult()
}

@Serializable
data class ValidationError(
    val field: String,
    val message: String,
    val code: String
)

/**
 * Tool registry for managing all available tools
 */
interface ToolRegistry {
    val tools: Map<String, Tool>
    val categories: Map<ToolCategory, List<Tool>>

    fun register(tool: Tool)
    fun unregister(toolId: String)
    fun getTool(toolId: String): Tool?
    fun getToolsByCategory(category: ToolCategory): List<Tool>
    fun getAvailableTools(): List<Tool>
    fun searchTools(query: String): List<Tool>
}

/**
 * Tool execution context
 */
data class ToolExecutionContext(
    val toolId: String,
    val args: Map<String, Any>,
    val conversationId: String,
    val messageId: String,
    val userId: String? = null,
    val metadata: Map<String, String> = emptyMap()
)