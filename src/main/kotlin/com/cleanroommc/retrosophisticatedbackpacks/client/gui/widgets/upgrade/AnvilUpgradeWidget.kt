package com.cleanroommc.retrosophisticatedbackpacks.client.gui.widgets.upgrade

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.api.widget.Interactable
import com.cleanroommc.modularui.drawable.GuiDraw
import com.cleanroommc.modularui.screen.RichTooltip
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext
import com.cleanroommc.modularui.theme.WidgetThemeEntry
import com.cleanroommc.modularui.widgets.ButtonWidget
import com.cleanroommc.modularui.widgets.SlotGroupWidget
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget
import com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade.AnvilUpgradeWrapper
import com.cleanroommc.retrosophisticatedbackpacks.client.gui.RSBTextures
import com.cleanroommc.retrosophisticatedbackpacks.client.gui.widgets.slot.NoBackgroundItemSlot
import com.cleanroommc.retrosophisticatedbackpacks.sync.UpgradeSlotSH
import com.cleanroommc.retrosophisticatedbackpacks.util.Utils.asTranslationKey
import net.minecraft.item.ItemStack

class AnvilUpgradeWidget(
    slotIndex: Int,
    wrapper: AnvilUpgradeWrapper,
    stack: ItemStack
) : ExpandedUpgradeTabWidget<AnvilUpgradeWrapper>(slotIndex, wrapper, 5, stack, wrapper.settingsLangKey, width = 103) {
    init {
        size(103, 92)
        child(AnvilNameField().pos(5, 25).size(90, 15))
        val slots = SlotGroupWidget().name("anvil_$slotIndex").disableSortButtons()
        slots.size(67, 18).pos(4, 43)
        slots.child(NoBackgroundItemSlot().syncHandler("anvil_$slotIndex", 0).pos(0, 0))
        slots.child(NoBackgroundItemSlot().syncHandler("anvil_$slotIndex", 1).pos(49, 0))
        child(slots)
        child(resultButton().pos(80, 43))
        child(shiftClickButton().pos(5, 68))
    }

    override fun draw(context: ModularGuiContext, widgetTheme: WidgetThemeEntry<*>) {
        RSBTextures.SLOT_BACKGROUND.draw(context, 4, 43, 18, 18, widgetTheme.theme)
        RSBTextures.SLOT_BACKGROUND.draw(context, 53, 43, 18, 18, widgetTheme.theme)
        RSBTextures.SLOT_BACKGROUND.draw(context, 80, 43, 18, 18, widgetTheme.theme)
        RSBTextures.ADD_ICON.draw(context, 29, 45, 13, 13, widgetTheme.theme)
        RSBTextures.DOWN_ARROW_ICON.draw(context, 68, 45, 12, 12, widgetTheme.theme)
        GuiDraw.drawText("Cost: ${wrapper.maximumCost}", 5f, 62f, 0.75f, 0x404040, false)
        super.draw(context, widgetTheme)
    }

    private fun resultButton(): ButtonWidget<*> =
        ButtonWidget()
            .size(18)
            .onMousePressed {
                if (it != 0) false else {
                    slotSyncHandler?.syncToServer(UpgradeSlotSH.UPDATE_ANVIL_TAKE_RESULT) {}
                    true
                }
            }
            .tooltipDynamic {
                val player = context.mc.player
                val result = wrapper.updateRepairOutput(player, player.world)
                if (result.isEmpty) it.addLine(IKey.lang("gui.anvil_no_result".asTranslationKey()))
                else it.addLine(IKey.str(result.displayName))
                it.pos(RichTooltip.Pos.NEXT_TO_MOUSE)
            }

    private fun shiftClickButton(): ButtonWidget<*> =
        ButtonWidget()
            .size(20)
            .overlay(if (wrapper.shouldShiftClickIntoStorage) RSBTextures.CHECK_ICON else RSBTextures.CROSS_ICON)
            .onMousePressed {
                if (it != 0) false else {
                    wrapper.shouldShiftClickIntoStorage = !wrapper.shouldShiftClickIntoStorage
                    slotSyncHandler?.syncToServer(UpgradeSlotSH.UPDATE_ANVIL_SHIFT_CLICK) {}
                    true
                }
            }
            .tooltipStatic {
                it.addLine(IKey.lang("gui.anvil_shift_click_storage".asTranslationKey()))
                it.pos(RichTooltip.Pos.NEXT_TO_MOUSE)
            }

    private inner class AnvilNameField : TextFieldWidget() {
        init {
            setText(wrapper.itemName)
            setMaxLength(40)
        }

        override fun onRemoveFocus(context: ModularGuiContext) {
            super.onRemoveFocus(context)
            wrapper.itemName = text
            slotSyncHandler?.syncToServer(UpgradeSlotSH.UPDATE_ANVIL_ITEM_NAME) { it.writeString(wrapper.itemName) }
        }
    }
}
