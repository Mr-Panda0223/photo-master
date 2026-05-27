package com.photomaster.domain.manager

import com.photomaster.data.model.EditOperation

/**
 * 编辑历史管理器
 * 管理撤销/重做功能
 */
class EditHistoryManager {
    private val history = mutableListOf<EditOperation>()
    private var currentIndex = -1
    private val maxHistorySize = 30

    /**
     * 添加操作到历史
     */
    fun addOperation(operation: EditOperation) {
        // 删除当前位置之后的历史
        if (currentIndex < history.size - 1) {
            history.subList(currentIndex + 1, history.size).clear()
        }

        history.add(operation)
        currentIndex++

        // 限制历史记录大小
        if (history.size > maxHistorySize) {
            history.removeAt(0)
            currentIndex--
        }
    }

    /**
     * 撤销操作
     */
    fun undo(): EditOperation? {
        if (canUndo()) {
            currentIndex--
            return history.getOrNull(currentIndex)
        }
        return null
    }

    /**
     * 重做操作
     */
    fun redo(): EditOperation? {
        if (canRedo()) {
            currentIndex++
            return history[currentIndex]
        }
        return null
    }

    /**
     * 是否可以撤销
     */
    fun canUndo(): Boolean = currentIndex >= 0

    /**
     * 是否可以重做
     */
    fun canRedo(): Boolean = currentIndex < history.size - 1

    /**
     * 获取当前所有操作
     */
    fun getCurrentOperations(): List<EditOperation> {
        return history.take(currentIndex + 1)
    }

    /**
     * 获取当前历史记录（供ViewModel使用）
     */
    fun getCurrentHistory(): List<EditOperation> {
        return history.take(currentIndex + 1)
    }

    /**
     * 清空历史
     */
    fun clear() {
        history.clear()
        currentIndex = -1
    }

    /**
     * 获取历史大小
     */
    fun getHistorySize(): Int = history.size

    /**
     * 获取当前索引
     */
    fun getCurrentIndex(): Int = currentIndex
}
