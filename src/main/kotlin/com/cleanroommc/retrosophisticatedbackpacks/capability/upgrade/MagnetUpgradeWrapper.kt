package com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade

import com.cleanroommc.retrosophisticatedbackpacks.capability.Capabilities
import com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade.UpgradeFilterUtils.matchesAllowEmpty
import com.cleanroommc.retrosophisticatedbackpacks.config.Config
import com.cleanroommc.retrosophisticatedbackpacks.item.MagnetUpgradeItem
import com.cleanroommc.retrosophisticatedbackpacks.util.Utils.asTranslationKey
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.Capability

open class MagnetUpgradeWrapper(
    filterSlots: Int = Config.magnetUpgrade.filterSlots,
    slotsInRow: Int = Config.magnetUpgrade.slotsInRow,
    override val range: Double = Config.magnetUpgrade.magnetRange.toDouble()
) :
    BasicUpgradeWrapper<MagnetUpgradeItem>(filterSlots, slotsInRow), IMagnetUpgrade {
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

class AdvancedMagnetUpgradeWrapper :
    AdvancedUpgradeWrapper<MagnetUpgradeItem>(Config.advancedMagnetUpgrade.filterSlots, Config.advancedMagnetUpgrade.slotsInRow),
    IMagnetUpgrade {
    override val settingsLangKey = "gui.advanced_magnet_settings".asTranslationKey()
    override val range = Config.advancedMagnetUpgrade.magnetRange.toDouble()

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
