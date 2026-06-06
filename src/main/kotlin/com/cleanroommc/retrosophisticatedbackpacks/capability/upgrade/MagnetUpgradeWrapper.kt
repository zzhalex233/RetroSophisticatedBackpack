package com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade

import com.cleanroommc.retrosophisticatedbackpacks.capability.Capabilities
import com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade.UpgradeFilterUtils.matchesAllowEmpty
import com.cleanroommc.retrosophisticatedbackpacks.item.MagnetUpgradeItem
import com.cleanroommc.retrosophisticatedbackpacks.util.Utils.asTranslationKey
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.Capability

open class MagnetUpgradeWrapper(filterSlots: Int = 9, override val range: Double = 3.0) :
    BasicUpgradeWrapper<MagnetUpgradeItem>(filterSlots), IMagnetUpgrade {
    override val settingsLangKey = "gui.magnet_settings".asTranslationKey()

    init {
        filterType = IBasicFilterable.FilterType.BLACKLIST
    }

    override fun canPickup(stack: ItemStack): Boolean =
        enabled && matchesAllowEmpty(stack)

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean =
        capability == Capabilities.MAGNET_UPGRADE_CAPABILITY ||
                super<IMagnetUpgrade>.hasCapability(capability, facing) ||
                super<BasicUpgradeWrapper>.hasCapability(capability, facing)
}

class AdvancedMagnetUpgradeWrapper : AdvancedUpgradeWrapper<MagnetUpgradeItem>(), IMagnetUpgrade {
    override val settingsLangKey = "gui.advanced_magnet_settings".asTranslationKey()
    override val range = 5.0

    init {
        filterType = IBasicFilterable.FilterType.BLACKLIST
    }

    override fun canPickup(stack: ItemStack): Boolean =
        enabled && matchesAllowEmpty(stack)

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean =
        capability == Capabilities.ADVANCED_MAGNET_UPGRADE_CAPABILITY ||
                super<IMagnetUpgrade>.hasCapability(capability, facing) ||
                super<AdvancedUpgradeWrapper>.hasCapability(capability, facing)
}
