package com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade

import com.cleanroommc.retrosophisticatedbackpacks.capability.BackpackWrapper
import com.cleanroommc.retrosophisticatedbackpacks.capability.Capabilities
import com.cleanroommc.retrosophisticatedbackpacks.item.RefillUpgradeItem
import com.cleanroommc.retrosophisticatedbackpacks.util.Utils.asTranslationKey
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.Constants
import net.minecraftforge.items.ItemHandlerHelper

open class RefillUpgradeWrapper(filterSlots: Int = 6) :
    BasicUpgradeWrapper<RefillUpgradeItem>(filterSlots), IRefillUpgrade {
    override val settingsLangKey = "gui.refill_settings".asTranslationKey()

    init {
        filterType = IBasicFilterable.FilterType.BLACKLIST
    }

    override fun refill(player: EntityPlayer, wrapper: BackpackWrapper) {
        if (!enabled) {
            return
        }

        for ((filterSlot, filter) in filterItems.inventory.withIndex()) {
            if (!filter.isEmpty) {
                refillFilter(player, wrapper, filter, getTargetSlot(filterSlot))
            }
        }
    }

    open fun getTargetSlot(filterSlot: Int): TargetSlot =
        TargetSlot.ANY

    protected fun refillFilter(player: EntityPlayer, wrapper: BackpackWrapper, filter: ItemStack, targetSlot: TargetSlot) {
        var missingCount = targetSlot.getMissingCount(player, filter)
        val carried = player.inventory.itemStack
        if (ItemHandlerHelper.canItemStacksStack(carried, filter)) {
            missingCount -= minOf(missingCount, carried.count)
        }
        if (missingCount <= 0) {
            return
        }

        val extracted = wrapper.extractMatching(filter, missingCount, true)
        if (extracted.isEmpty) {
            return
        }

        val remaining = targetSlot.insert(player, extracted)
        val moved = extracted.count - remaining.count

        if (moved > 0) {
            wrapper.extractMatching(filter, moved, false)
        }
    }

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean =
        capability == Capabilities.REFILL_UPGRADE_CAPABILITY ||
                super<IRefillUpgrade>.hasCapability(capability, facing) ||
                super<BasicUpgradeWrapper>.hasCapability(capability, facing)

    enum class TargetSlot {
        ANY {
            override fun getMissingCount(player: EntityPlayer, filter: ItemStack): Int {
                var count = 0
                for (slot in 0 until player.inventory.mainInventory.size) {
                    val stack = player.inventory.getStackInSlot(slot)
                    if (stack.isEmpty) {
                        count += filter.maxStackSize
                    } else if (ItemHandlerHelper.canItemStacksStack(stack, filter)) {
                        count += stack.maxStackSize - stack.count
                    }
                }
                return count
            }

            override fun insert(player: EntityPlayer, stack: ItemStack): ItemStack {
                val remaining = stack.copy()
                for (slot in 0 until player.inventory.mainInventory.size) {
                    val target = player.inventory.getStackInSlot(slot)
                    if (!ItemHandlerHelper.canItemStacksStack(target, remaining)) {
                        continue
                    }
                    val moved = minOf(remaining.count, target.maxStackSize - target.count)
                    if (moved > 0) {
                        target.grow(moved)
                        remaining.shrink(moved)
                    }
                    if (remaining.isEmpty) {
                        return ItemStack.EMPTY
                    }
                }
                for (slot in 0 until player.inventory.mainInventory.size) {
                    if (!player.inventory.getStackInSlot(slot).isEmpty) {
                        continue
                    }
                    player.inventory.setInventorySlotContents(slot, remaining.copy())
                    return ItemStack.EMPTY
                }
                return remaining
            }
        },
        MAIN_HAND {
            override fun getMissingCount(player: EntityPlayer, filter: ItemStack): Int =
                getSlotMissingCount(player.heldItemMainhand, filter)

            override fun insert(player: EntityPlayer, stack: ItemStack): ItemStack =
                refillSlot(player.heldItemMainhand, stack) { player.inventory.setInventorySlotContents(player.inventory.currentItem, it) }
        },
        OFF_HAND {
            override fun getMissingCount(player: EntityPlayer, filter: ItemStack): Int =
                getSlotMissingCount(player.heldItemOffhand, filter)

            override fun insert(player: EntityPlayer, stack: ItemStack): ItemStack =
                refillSlot(player.heldItemOffhand, stack) { player.setHeldItem(net.minecraft.util.EnumHand.OFF_HAND, it) }
        },
        HOTBAR_1, HOTBAR_2, HOTBAR_3, HOTBAR_4, HOTBAR_5, HOTBAR_6, HOTBAR_7, HOTBAR_8, HOTBAR_9;

        open fun getMissingCount(player: EntityPlayer, filter: ItemStack): Int {
            val slot = ordinal - HOTBAR_1.ordinal
            return getSlotMissingCount(player.inventory.getStackInSlot(slot), filter)
        }

        open fun insert(player: EntityPlayer, stack: ItemStack): ItemStack {
            val slot = ordinal - HOTBAR_1.ordinal
            return refillSlot(player.inventory.getStackInSlot(slot), stack) {
                player.inventory.setInventorySlotContents(slot, it)
            }
        }

        companion object {
            private fun getSlotMissingCount(stack: ItemStack, filter: ItemStack): Int =
                if (stack.isEmpty) filter.maxStackSize
                else if (ItemHandlerHelper.canItemStacksStack(stack, filter)) stack.maxStackSize - stack.count
                else 0

            private fun refillSlot(stack: ItemStack, toAdd: ItemStack, setter: (ItemStack) -> Unit): ItemStack {
                if (stack.isEmpty) {
                    setter(toAdd.copy())
                    return ItemStack.EMPTY
                }
                if (!ItemHandlerHelper.canItemStacksStack(stack, toAdd)) {
                    return toAdd
                }
                val moved = minOf(toAdd.count, stack.maxStackSize - stack.count)
                stack.grow(moved)
                return if (moved == toAdd.count) ItemStack.EMPTY else ItemHandlerHelper.copyStackWithSize(toAdd, toAdd.count - moved)
            }
        }
    }
}

class AdvancedRefillUpgradeWrapper : RefillUpgradeWrapper(12) {
    companion object {
        private const val TARGET_SLOTS_TAG = "TargetSlots"
        private const val SLOT_TAG = "Slot"
        private const val TARGET_TAG = "Target"
    }

    override val settingsLangKey = "gui.advanced_refill_settings".asTranslationKey()
    private val targetSlots = mutableMapOf<Int, TargetSlot>()

    override fun getTargetSlot(filterSlot: Int): TargetSlot =
        targetSlots[filterSlot] ?: TargetSlot.ANY

    fun setTargetSlot(filterSlot: Int, targetSlot: TargetSlot) {
        targetSlots[filterSlot] = targetSlot
    }

    fun pickBlock(player: EntityPlayer, wrapper: BackpackWrapper, pickedStack: ItemStack): Boolean {
        if (!enabled || pickedStack.isEmpty) {
            return false
        }

        val extracted = wrapper.extractMatching(pickedStack, pickedStack.maxStackSize, true)
        if (extracted.isEmpty || !moveHeldStackAway(player, wrapper)) {
            return false
        }

        player.setHeldItem(EnumHand.MAIN_HAND, wrapper.extractMatching(pickedStack, extracted.count, false))
        player.inventoryContainer.detectAndSendChanges()
        return true
    }

    private fun moveHeldStackAway(player: EntityPlayer, wrapper: BackpackWrapper): Boolean {
        val held = player.heldItemMainhand
        if (held.isEmpty) {
            return true
        }

        if (wrapper.insertStack(held.copy(), true).isEmpty) {
            wrapper.insertStack(held.copy(), false)
            player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY)
            return true
        }

        val remaining = held.copy()
        val currentSlot = player.inventory.currentItem
        for (slot in 0 until player.inventory.mainInventory.size) {
            if (slot == currentSlot) {
                continue
            }
            val target = player.inventory.getStackInSlot(slot)
            if (!ItemHandlerHelper.canItemStacksStack(target, remaining)) {
                continue
            }
            val moved = minOf(remaining.count, target.maxStackSize - target.count)
            if (moved > 0) {
                target.grow(moved)
                remaining.shrink(moved)
            }
            if (remaining.isEmpty) {
                break
            }
        }
        for (slot in 0 until player.inventory.mainInventory.size) {
            if (remaining.isEmpty) {
                break
            }
            if (slot != currentSlot && player.inventory.getStackInSlot(slot).isEmpty) {
                player.inventory.setInventorySlotContents(slot, remaining.copy())
                remaining.count = 0
            }
        }

        if (!remaining.isEmpty) {
            return false
        }

        player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY)
        return true
    }

    override fun serializeNBT(): NBTTagCompound {
        val nbt = super.serializeNBT()
        val list = NBTTagList()
        for ((slot, target) in targetSlots) {
            val entry = NBTTagCompound()
            entry.setInteger(SLOT_TAG, slot)
            entry.setByte(TARGET_TAG, target.ordinal.toByte())
            list.appendTag(entry)
        }
        nbt.setTag(TARGET_SLOTS_TAG, list)
        return nbt
    }

    override fun deserializeNBT(nbt: NBTTagCompound) {
        super.deserializeNBT(nbt)
        targetSlots.clear()
        val list = nbt.getTagList(TARGET_SLOTS_TAG, Constants.NBT.TAG_COMPOUND)
        for (entry in list) {
            val compound = entry as NBTTagCompound
            val targetOrdinal = compound.getByte(TARGET_TAG).toInt()
            targetSlots[compound.getInteger(SLOT_TAG)] = TargetSlot.entries.getOrElse(targetOrdinal) { TargetSlot.ANY }
        }
    }

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean =
        capability == Capabilities.ADVANCED_REFILL_UPGRADE_CAPABILITY ||
                super.hasCapability(capability, facing)
}
