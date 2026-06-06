package com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade

import com.cleanroommc.retrosophisticatedbackpacks.capability.Capabilities
import com.cleanroommc.retrosophisticatedbackpacks.inventory.ExposedItemStackHandler
import com.cleanroommc.retrosophisticatedbackpacks.item.JukeboxUpgradeItem
import com.cleanroommc.retrosophisticatedbackpacks.util.Utils.asTranslationKey
import net.minecraft.entity.Entity
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemRecord
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.items.IItemHandler
import java.util.Collections
import java.util.LinkedList

open class JukeboxUpgradeWrapper(private val slots: Int = 1) : UpgradeWrapper<JukeboxUpgradeItem>(), IJukeboxUpgrade {
    companion object {
        private const val INVENTORY_TAG = "Inventory"
        private const val IS_PLAYING_TAG = "IsPlaying"
        private const val ACTIVE_SLOT_TAG = "ActiveSlot"
        private const val SHUFFLE_TAG = "Shuffle"
        private const val REPEAT_MODE_TAG = "RepeatMode"
        private const val FINISH_TIME_TAG = "FinishTime"

        private const val RECORD_PLAY_EVENT = 1010
        private const val DEFAULT_DISC_LENGTH = 3600L

        private val VANILLA_DISC_LENGTHS = mapOf(
            Items.RECORD_13 to 1780L,
            Items.RECORD_CAT to 3700L,
            Items.RECORD_BLOCKS to 6900L,
            Items.RECORD_CHIRP to 3700L,
            Items.RECORD_FAR to 3480L,
            Items.RECORD_MALL to 3940L,
            Items.RECORD_MELLOHI to 1920L,
            Items.RECORD_STAL to 3000L,
            Items.RECORD_STRAD to 3760L,
            Items.RECORD_WARD to 5020L,
            Items.RECORD_11 to 1420L,
            Items.RECORD_WAIT to 4760L
        )
    }

    override val settingsLangKey = "gui.jukebox_settings".asTranslationKey()
    private var isPlaying = false
    private var activeSlot = -1
    var shuffleEnabled = false
    var repeatMode = RepeatMode.NO
    private var finishTime = 0L
    private var worldPlaying: World? = null
    private var posPlaying: BlockPos? = null
    private val playlist = LinkedList<Int>()
    private val history = LinkedList<Int>()

    override val discInventory: IItemHandler = object : ExposedItemStackHandler(slots) {
        override fun isItemValid(slot: Int, stack: ItemStack): Boolean =
            stack.isEmpty || stack.item is ItemRecord

        override fun onContentsChanged(slot: Int) {
            super.onContentsChanged(slot)
            initPlaylist(excludeActive = true)
        }
    }

    override fun play(world: World, pos: BlockPos) {
        if (world.isRemote) {
            return
        }
        worldPlaying = world
        posPlaying = pos
        if (!isPlaying) {
            playNext()
        }
    }

    override fun play(entity: Entity) {
        play(entity.world, BlockPos(entity))
    }

    override fun stop(world: World, pos: BlockPos) {
        if (world.isRemote) {
            return
        }
        world.playEvent(null, RECORD_PLAY_EVENT, pos, 0)
        setPlaying(false)
        playlist.clear()
        history.clear()
    }

    override fun next() {
        playNext()
    }

    override fun previous() {
        if (history.isEmpty()) {
            return
        }
        if (activeSlot >= 0) {
            playlist.addFirst(activeSlot)
        }
        activeSlot = history.pollLast()
        playDisc()
    }

    override fun tick(world: World, pos: BlockPos?) {
        if (world.isRemote || !isPlaying) {
            return
        }
        if (pos != null && (activeSlot == -1 || worldPlaying == null || posPlaying == null)) {
            worldPlaying = world
            posPlaying = pos
        }
        if (isPlaying && activeSlot == -1) {
            playNext()
            return
        }
        if (finishTime > 0L && world.totalWorldTime >= finishTime) {
            onDiscFinished()
        }
    }

    fun toggleShuffle() {
        shuffleEnabled = !shuffleEnabled
        initPlaylist(excludeActive = true)
    }

    fun cycleRepeatMode() {
        repeatMode = repeatMode.next()
    }

    fun isPlaying(): Boolean = isPlaying

    fun requestPlay() {
        setPlaying(true)
    }

    fun requestStop() {
        worldPlaying?.let { world ->
            posPlaying?.let { pos -> stop(world, pos) }
        } ?: setPlaying(false)
    }

    override fun onBeforeRemoved() {
        requestStop()
    }

    private fun playNext(startOverIfAtEnd: Boolean = true) {
        if (playlist.isEmpty() && startOverIfAtEnd) {
            initPlaylist(excludeActive = false)
        }
        val nextSlot = playlist.poll() ?: return
        if (activeSlot >= 0) {
            history.add(activeSlot)
            while (history.size > slots) {
                history.poll()
            }
        }
        activeSlot = nextSlot
        playDisc()
    }

    private fun onDiscFinished() {
        when (repeatMode) {
            RepeatMode.ONE -> playDisc()
            RepeatMode.ALL -> playNext()
            RepeatMode.NO -> playNext(startOverIfAtEnd = false)
        }
    }

    private fun playDisc() {
        val world = worldPlaying ?: return
        val pos = posPlaying ?: return
        val disc = getDisc()
        if (world.isRemote || disc.isEmpty || disc.item !is ItemRecord) {
            setPlaying(false)
            return
        }
        world.playEvent(null, RECORD_PLAY_EVENT, pos, 0)
        world.playEvent(null, RECORD_PLAY_EVENT, pos, Item.getIdFromItem(disc.item))
        finishTime = world.totalWorldTime + getDiscLength(disc)
        setPlaying(true)
    }

    private fun getDisc(): ItemStack =
        if (activeSlot in 0 until slots) discInventory.getStackInSlot(activeSlot) else ItemStack.EMPTY

    private fun setPlaying(playing: Boolean) {
        isPlaying = playing
        if (!playing) {
            activeSlot = -1
            finishTime = 0L
        }
    }

    private fun initPlaylist(excludeActive: Boolean) {
        playlist.clear()
        for (slot in 0 until slots) {
            if (!discInventory.getStackInSlot(slot).isEmpty && (!excludeActive || !isPlaying || slot != activeSlot)) {
                playlist.add(slot)
            }
        }
        if (shuffleEnabled) {
            Collections.shuffle(playlist)
        }
    }

    private fun getDiscLength(stack: ItemStack): Long =
        VANILLA_DISC_LENGTHS[stack.item] ?: DEFAULT_DISC_LENGTH

    override fun serializeNBT(): NBTTagCompound {
        val nbt = super.serializeNBT()
        nbt.setTag(INVENTORY_TAG, (discInventory as ExposedItemStackHandler).serializeNBT())
        nbt.setBoolean(IS_PLAYING_TAG, isPlaying)
        nbt.setInteger(ACTIVE_SLOT_TAG, activeSlot)
        nbt.setBoolean(SHUFFLE_TAG, shuffleEnabled)
        nbt.setString(REPEAT_MODE_TAG, repeatMode.name)
        nbt.setLong(FINISH_TIME_TAG, finishTime)
        return nbt
    }

    override fun deserializeNBT(nbt: NBTTagCompound) {
        super.deserializeNBT(nbt)
        (discInventory as ExposedItemStackHandler).deserializeNBT(nbt.getCompoundTag(INVENTORY_TAG))
        isPlaying = nbt.getBoolean(IS_PLAYING_TAG)
        activeSlot = nbt.getInteger(ACTIVE_SLOT_TAG)
        shuffleEnabled = nbt.getBoolean(SHUFFLE_TAG)
        repeatMode = runCatching { RepeatMode.valueOf(nbt.getString(REPEAT_MODE_TAG)) }.getOrDefault(RepeatMode.NO)
        finishTime = nbt.getLong(FINISH_TIME_TAG)
        initPlaylist(excludeActive = true)
    }

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean =
        capability == Capabilities.JUKEBOX_UPGRADE_CAPABILITY ||
                capability == Capabilities.IJUKEBOX_UPGRADE_CAPABILITY ||
                super<UpgradeWrapper>.hasCapability(capability, facing)
}

class AdvancedJukeboxUpgradeWrapper : JukeboxUpgradeWrapper(12) {
    override val settingsLangKey = "gui.advanced_jukebox_settings".asTranslationKey()

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean =
        capability == Capabilities.ADVANCED_JUKEBOX_UPGRADE_CAPABILITY ||
                super.hasCapability(capability, facing)
}

enum class RepeatMode {
    ALL,
    ONE,
    NO;

    fun next(): RepeatMode = entries[(ordinal + 1) % entries.size]
}
