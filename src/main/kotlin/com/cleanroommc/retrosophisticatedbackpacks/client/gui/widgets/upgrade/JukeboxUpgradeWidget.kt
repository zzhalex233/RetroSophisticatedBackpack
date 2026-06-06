package com.cleanroommc.retrosophisticatedbackpacks.client.gui.widgets.upgrade

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.screen.RichTooltip
import com.cleanroommc.modularui.widgets.ButtonWidget
import com.cleanroommc.modularui.widgets.SlotGroupWidget
import com.cleanroommc.modularui.widgets.slot.ItemSlot
import com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade.JukeboxUpgradeWrapper
import com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade.RepeatMode
import com.cleanroommc.retrosophisticatedbackpacks.client.gui.RSBTextures
import com.cleanroommc.retrosophisticatedbackpacks.client.gui.widgets.CyclicVariantButtonWidget
import com.cleanroommc.retrosophisticatedbackpacks.sync.UpgradeSlotSH
import com.cleanroommc.retrosophisticatedbackpacks.util.Utils.asTranslationKey
import net.minecraft.item.ItemStack

class JukeboxUpgradeWidget(slotIndex: Int, wrapper: JukeboxUpgradeWrapper, stack: ItemStack, private val slots: Int) :
    ExpandedUpgradeTabWidget<JukeboxUpgradeWrapper>(slotIndex, wrapper, if (slots > 4) 5 else 4, stack, wrapper.settingsLangKey, width = if (slots > 4) 112 else 80) {
    companion object {
        private const val SLOT_SIZE = 18
    }

    init {
        size(if (slots > 4) 112 else 80, if (slots > 4) 130 else 94)

        child(createCommandButton(UpgradeSlotSH.UPDATE_JUKEBOX_PLAY, "gui.jukebox_play".asTranslationKey(), RSBTextures.CHECK_ICON).pos(8, 28))
        child(createCommandButton(UpgradeSlotSH.UPDATE_JUKEBOX_STOP, "gui.jukebox_stop".asTranslationKey(), RSBTextures.CROSS_ICON).pos(30, 28))
        child(createCommandButton(UpgradeSlotSH.UPDATE_JUKEBOX_PREVIOUS, "gui.jukebox_previous".asTranslationKey(), RSBTextures.LEFT_ARROW_ICON).pos(52, 28))
        child(createCommandButton(UpgradeSlotSH.UPDATE_JUKEBOX_NEXT, "gui.jukebox_next".asTranslationKey(), RSBTextures.SOLID_UP_ARROW_ICON).pos(74, 28))
        child(createShuffleButton().pos(8, 50))
        child(createRepeatButton().pos(30, 50))

        val discs = SlotGroupWidget().name("jukebox_discs_$slotIndex").disableSortButtons()
        discs.flex().coverChildren().leftRel(0.5F).top(if (slots > 4) 74 else 72)
        repeat(slots) {
            discs.child(ItemSlot().syncHandler("jukebox_discs_$slotIndex", it).pos(it % 4 * SLOT_SIZE, it / 4 * SLOT_SIZE))
        }
        child(discs)
    }

    override fun onWrapperChange(after: JukeboxUpgradeWrapper) {
        super.onWrapperChange(after)
    }

    private fun createCommandButton(syncId: Int, langKey: String, icon: com.cleanroommc.modularui.api.drawable.IDrawable): ButtonWidget<*> =
        ButtonWidget()
            .size(20)
            .overlay(icon)
            .onMousePressed {
                if (it != 0) {
                    false
                } else {
                    slotSyncHandler?.syncToServer(syncId) {}
                    true
                }
            }
            .tooltipStatic {
                it.addLine(IKey.lang(langKey)).pos(RichTooltip.Pos.NEXT_TO_MOUSE)
            }

    private fun createShuffleButton(): CyclicVariantButtonWidget =
        CyclicVariantButtonWidget(SHUFFLE_VARIANTS, if (wrapper.shuffleEnabled) 1 else 0) {
            wrapper.toggleShuffle()
            slotSyncHandler?.syncToServer(UpgradeSlotSH.UPDATE_JUKEBOX_SHUFFLE) {}
        }

    private fun createRepeatButton(): CyclicVariantButtonWidget =
        CyclicVariantButtonWidget(REPEAT_VARIANTS, wrapper.repeatMode.ordinal) {
            wrapper.repeatMode = RepeatMode.entries[it]
            slotSyncHandler?.syncToServer(UpgradeSlotSH.UPDATE_JUKEBOX_REPEAT_MODE) {
                it.writeEnumValue(wrapper.repeatMode)
            }
        }
}

private val SHUFFLE_VARIANTS = listOf(
    CyclicVariantButtonWidget.Variant(IKey.lang("gui.jukebox_shuffle_disabled".asTranslationKey()), RSBTextures.CROSS_ICON),
    CyclicVariantButtonWidget.Variant(IKey.lang("gui.jukebox_shuffle_enabled".asTranslationKey()), RSBTextures.CHECK_ICON),
)

private val REPEAT_VARIANTS = listOf(
    CyclicVariantButtonWidget.Variant(IKey.lang("gui.jukebox_repeat_all".asTranslationKey()), RSBTextures.IN_OUT_ICON),
    CyclicVariantButtonWidget.Variant(IKey.lang("gui.jukebox_repeat_one".asTranslationKey()), RSBTextures.SMALL_1_ICON),
    CyclicVariantButtonWidget.Variant(IKey.lang("gui.jukebox_repeat_no".asTranslationKey()), RSBTextures.CROSS_ICON),
)
