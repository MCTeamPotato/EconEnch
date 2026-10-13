package me.kall.econench.mixin;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {
    @Unique private ItemStack econEnch$pendingRemainingBook = ItemStack.EMPTY;

    public AnvilMenuMixin(@Nullable MenuType<?> type, int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(type, containerId, playerInventory, access);
    }

    @Inject(method = "onTake", at = @At("HEAD"))
    private void recordOnTake(Player player, ItemStack output, CallbackInfo ci) {
        this.econEnch$pendingRemainingBook = ItemStack.EMPTY;

        ItemStack left = this.inputSlots.getItem(0);
        ItemStack right = this.inputSlots.getItem(1);

        if (left.isEmpty() || right.isEmpty() || output.isEmpty()) return;

        Map<Enchantment, Integer> rightEnch = EnchantmentHelper.getEnchantments(right);
        if (rightEnch.isEmpty()) return;

        Map<Enchantment, Integer> outputEnch = EnchantmentHelper.getEnchantments(output);

        Map<Enchantment, Integer> remaining = new Object2IntOpenHashMap<>();
        boolean hasRemaining = false;

        for (Map.Entry<Enchantment, Integer> entry : rightEnch.entrySet()) {
            Enchantment enchantment = entry.getKey();
            if (outputEnch.get(enchantment) <= 0) {
                remaining.put(enchantment, entry.getValue());
                hasRemaining = true;
            }
        }

        if (hasRemaining) {
            ItemStack remainingBook = right.copy();
            EnchantmentHelper.setEnchantments(remaining, remainingBook);
            this.econEnch$pendingRemainingBook = remainingBook;
        }
    }

    @Inject(method = "onTake", at = @At("TAIL"))
    private void keepOnTake(CallbackInfo ci) {
        if (this.econEnch$pendingRemainingBook.isEmpty()) return;

        this.inputSlots.setItem(1, this.econEnch$pendingRemainingBook.copy());
        this.econEnch$pendingRemainingBook = ItemStack.EMPTY;
        this.broadcastChanges();
    }
}
