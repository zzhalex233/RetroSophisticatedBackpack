package com.cleanroommc.retrosophisticatedbackpacks.config

import com.cleanroommc.retrosophisticatedbackpacks.Tags
import net.minecraftforge.common.config.Config

@Config(modid = Tags.MOD_ID, name = "${Tags.MOD_ID}_general")
object Config {
    @JvmField
    @Config.Comment("Items that cannot be stored in backpack")
    @Config.RequiresMcRestart
    var blacklistedItems = arrayOf<String>()

    @JvmField
    @Config.Comment(
        "Entities that spawn with backpack equipped.",
        "Only entities that have chest armor slot will be able to spawn with a backpack equipped.",
    )
    var backpackEntitySpawnList = arrayOf(
        "minecraft:zombie",
        "minecraft:skeleton",
        "minecraft:husk",
        "minecraft:stray",
        "minecraft:wither_skeleton",
        "minecraft:zombie_pigman",
        "minecraft:wither_skeleton",
    )

    @JvmField
    var baseBackpackSpawnChance = 0.03f

    @JvmField
    val leatherBackpack = LeatherBackpackConfig()

    @JvmField
    val ironBackpack = IronBackpackConfig()

    @JvmField
    val goldBackpack = GoldBackpackConfig()

    @JvmField
    val diamondBackpack = DiamondBackpackConfig()

    @JvmField
    val obsidianBackpack = ObsidianBackpackConfig()

    @JvmField
    val stackUpgrade = StackUpgradeConfig()

    class LeatherBackpackConfig {
        @JvmField
        @Config.RequiresMcRestart
        var slots = 27

        @JvmField
        @Config.RequiresMcRestart
        var upgradeSlots = 1

        @JvmField
        var spawnChanceOnMob = 0.05f

        @JvmField
        val dropChance = 1f
    }

    class IronBackpackConfig {
        @JvmField
        @Config.RequiresMcRestart
        var slots = 54

        @JvmField
        @Config.RequiresMcRestart
        var upgradeSlots = 2

        @JvmField
        var spawnChanceOnMob = 0.04f

        @JvmField
        val dropChance = 1f
    }

    class GoldBackpackConfig {
        @JvmField
        @Config.RequiresMcRestart
        var slots = 81

        @JvmField
        @Config.RequiresMcRestart
        var upgradeSlots = 3

        @JvmField
        var spawnChanceOnMob = 0.03f

        @JvmField
        val dropChance = 1f
    }

    class DiamondBackpackConfig {
        @JvmField
        @Config.RequiresMcRestart
        var slots = 108

        @JvmField
        @Config.RequiresMcRestart
        var upgradeSlots = 5

        @JvmField
        var spawnChanceOnMob = 0.02f

        @JvmField
        val dropChance = 1f
    }

    class ObsidianBackpackConfig {
        @JvmField
        @Config.RequiresMcRestart
        var slots = 120

        @JvmField
        @Config.RequiresMcRestart
        var upgradeSlots = 7

        @JvmField
        var spawnChanceOnMob = 0.01f

        @JvmField
        val dropChance = 1f
    }

    class StackUpgradeConfig {
        @JvmField
        @Config.RequiresMcRestart
        var leatherMultiplier = 2

        @JvmField
        @Config.RequiresMcRestart
        var ironMultiplier = 4

        @JvmField
        @Config.RequiresMcRestart
        var goldMultiplier = 8

        @JvmField
        @Config.RequiresMcRestart
        var diamondMultiplier = 16

        @JvmField
        @Config.RequiresMcRestart
        var obsidianMultiplier = 32
    }
}
