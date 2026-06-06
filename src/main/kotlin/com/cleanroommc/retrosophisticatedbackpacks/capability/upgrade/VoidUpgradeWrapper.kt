package com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade

import com.cleanroommc.retrosophisticatedbackpacks.capability.Capabilities
import com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade.UpgradeFilterUtils.matchesAllowEmpty
import com.cleanroommc.retrosophisticatedbackpacks.item.VoidUpgradeItem
import com.cleanroommc.retrosophisticatedbackpacks.util.Utils.asTranslationKey
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.Capability

class VoidUpgradeWrapper : BasicUpgradeWrapper<VoidUpgradeItem>(), IVoidUpgrade {
    companion object {
        private const val VOID_TYPE_TAG = "VoidType"
        private const val SHOULD_WORK_IN_GUI_TAG = "ShouldWorkInGui"
    }

    override val settingsLangKey = "gui.void_settings".asTranslationKey()
    var voidType = VoidType.ALWAYS
    var shouldWorkInGui = false

    init {
        filterType = IBasicFilterable.FilterType.BLACKLIST
    }

    override fun shouldVoid(stack: ItemStack): Boolean =
        enabled && voidType == VoidType.ALWAYS && matchesAllowEmpty(stack)

    fun toggleWorkInGui() {
        shouldWorkInGui = !shouldWorkInGui
    }

    fun shouldVoidOverflow(stack: ItemStack, storageFull: Boolean, hasMatchingStack: Boolean): Boolean =
        enabled && matchesAllowEmpty(stack) && when (voidType) {
            VoidType.ALWAYS -> true
            VoidType.SLOT_OVERFLOW -> hasMatchingStack
            VoidType.STORAGE_OVERFLOW -> storageFull
        }

    override fun serializeNBT(): NBTTagCompound {
        val nbt = super.serializeNBT()
        nbt.setByte(VOID_TYPE_TAG, voidType.ordinal.toByte())
        nbt.setBoolean(SHOULD_WORK_IN_GUI_TAG, shouldWorkInGui)
        return nbt
    }

    override fun deserializeNBT(nbt: NBTTagCompound) {
        super.deserializeNBT(nbt)
        voidType = VoidType.entries.getOrElse(nbt.getByte(VOID_TYPE_TAG).toInt()) { VoidType.ALWAYS }
        shouldWorkInGui = nbt.getBoolean(SHOULD_WORK_IN_GUI_TAG)
    }

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean =
        capability == Capabilities.VOID_UPGRADE_CAPABILITY ||
                super<IVoidUpgrade>.hasCapability(capability, facing) ||
                super<BasicUpgradeWrapper>.hasCapability(capability, facing)
}

class AdvancedVoidUpgradeWrapper : AdvancedUpgradeWrapper<VoidUpgradeItem>(), IVoidUpgrade {
    companion object {
        private const val VOID_TYPE_TAG = "VoidType"
        private const val SHOULD_WORK_IN_GUI_TAG = "ShouldWorkInGui"
    }

    override val settingsLangKey = "gui.advanced_void_settings".asTranslationKey()
    var voidType = VoidType.ALWAYS
    var shouldWorkInGui = false

    init {
        filterType = IBasicFilterable.FilterType.BLACKLIST
    }

    override fun shouldVoid(stack: ItemStack): Boolean =
        enabled && voidType == VoidType.ALWAYS && matchesAllowEmpty(stack)

    fun toggleWorkInGui() {
        shouldWorkInGui = !shouldWorkInGui
    }

    fun shouldVoidOverflow(stack: ItemStack, storageFull: Boolean, hasMatchingStack: Boolean): Boolean =
        enabled && matchesAllowEmpty(stack) && when (voidType) {
            VoidType.ALWAYS -> true
            VoidType.SLOT_OVERFLOW -> hasMatchingStack
            VoidType.STORAGE_OVERFLOW -> storageFull
        }

    override fun serializeNBT(): NBTTagCompound {
        val nbt = super.serializeNBT()
        nbt.setByte(VOID_TYPE_TAG, voidType.ordinal.toByte())
        nbt.setBoolean(SHOULD_WORK_IN_GUI_TAG, shouldWorkInGui)
        return nbt
    }

    override fun deserializeNBT(nbt: NBTTagCompound) {
        super.deserializeNBT(nbt)
        voidType = VoidType.entries.getOrElse(nbt.getByte(VOID_TYPE_TAG).toInt()) { VoidType.ALWAYS }
        shouldWorkInGui = nbt.getBoolean(SHOULD_WORK_IN_GUI_TAG)
    }

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean =
        capability == Capabilities.ADVANCED_VOID_UPGRADE_CAPABILITY ||
                super<IVoidUpgrade>.hasCapability(capability, facing) ||
                super<AdvancedUpgradeWrapper>.hasCapability(capability, facing)
}

enum class VoidType {
    ALWAYS,
    SLOT_OVERFLOW,
    STORAGE_OVERFLOW;
}
