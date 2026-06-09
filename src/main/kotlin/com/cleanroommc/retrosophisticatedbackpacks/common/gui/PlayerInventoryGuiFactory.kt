package com.cleanroommc.retrosophisticatedbackpacks.common.gui

import com.cleanroommc.modularui.api.IGuiHolder
import com.cleanroommc.modularui.factory.AbstractUIFactory
import com.cleanroommc.modularui.factory.GuiManager
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.network.PacketBuffer
import net.minecraft.util.EnumHand

object PlayerInventoryGuiFactory : AbstractUIFactory<PlayerInventoryGuiData>("rsb:player_inv") {
    fun open(player: EntityPlayer, handIn: EnumHand) {
        val slotId = when (handIn) {
            EnumHand.MAIN_HAND -> player.inventory.currentItem
            EnumHand.OFF_HAND -> 40
        }

        open(player, player, PlayerInventoryGuiData.InventoryType.PLAYER_INVENTORY, slotId)
    }

    fun open(
        targetEntity: EntityLivingBase,
        fromPlayer: EntityPlayer,
        inventoryType: PlayerInventoryGuiData.InventoryType,
        slotIndex: Int
    ) {
        if (fromPlayer !is EntityPlayerMP)
            throw IllegalStateException("Synced GUIs must be opened from server side")

        val guiData = PlayerInventoryGuiData(targetEntity, fromPlayer, inventoryType, slotIndex)
        GuiManager.open(this, guiData, fromPlayer)
    }

    override fun getGuiHolder(data: PlayerInventoryGuiData): IGuiHolder<PlayerInventoryGuiData> =
        castGuiHolder(data.usedItemStack.item) ?: throw IllegalArgumentException("Item was not a gui holder!")

    override fun writeGuiData(
        guiData: PlayerInventoryGuiData,
        buffer: PacketBuffer
    ) {
        buffer.writeInt(guiData.targetEntity.entityId)
        buffer.writeInt(guiData.inventoryType.ordinal)
        buffer.writeInt(guiData.slotIndex)
    }

    override fun readGuiData(
        player: EntityPlayer,
        buffer: PacketBuffer
    ): PlayerInventoryGuiData {
        val targetEntityId = buffer.readInt()
        val targetEntity = player.world.getEntityByID(targetEntityId) as? EntityLivingBase
            ?: throw IllegalStateException("Unable to find the target entity to open the backpack from")
        val inventoryType = PlayerInventoryGuiData.InventoryType.entries[buffer.readInt()]
        val slotIndex = buffer.readInt()

        return PlayerInventoryGuiData(targetEntity, player, inventoryType, slotIndex)
    }
}
