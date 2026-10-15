package me.kall.econench.mixin;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {
    @Unique private ItemStack econEnch$pendingRemaining = ItemStack.EMPTY;

    public AnvilMenuMixin(@Nullable MenuType<?> type, int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(type, containerId, playerInventory, access);
    }

    @Inject(method = "onTake", at = @At("HEAD"))
    private void recordOnTake(Player player, ItemStack output, CallbackInfo ci) {
        this.econEnch$pendingRemaining = ItemStack.EMPTY;

        ItemStack left = this.inputSlots.getItem(0);
        ItemStack right = this.inputSlots.getItem(1);

        if (left.isEmpty() || right.isEmpty() || output.isEmpty()) return;

        ItemEnchantments rightEnch = EnchantmentHelper.getEnchantmentsForCrafting(right);
        if (rightEnch.isEmpty()) return;

        ItemEnchantments outputEnch = EnchantmentHelper.getEnchantmentsForCrafting(output);

        ItemEnchantments.Mutable remainingEnch = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        boolean hasRemaining = false;

        for (Object2IntMap.Entry<Holder<Enchantment>> entry : rightEnch.entrySet()) {
            Holder<Enchantment> enchantment = entry.getKey();
            if (outputEnch.getLevel(enchantment) <= 0) {
                remainingEnch.set(enchantment, entry.getIntValue());
                hasRemaining = true;
            }
        }

        if (hasRemaining) {
            ItemStack remaining = right.copy();
            EnchantmentHelper.setEnchantments(remaining, remainingEnch.toImmutable());
            this.econEnch$pendingRemaining = remaining;
        }
    }

    @Inject(method = "onTake", at = @At("TAIL"))
    private void keepOnTake(CallbackInfo ci) {
        if (this.econEnch$pendingRemaining.isEmpty()) return;

        this.inputSlots.setItem(1, this.econEnch$pendingRemaining.copy());
        this.econEnch$pendingRemaining = ItemStack.EMPTY;
        this.broadcastChanges();
    }
}
