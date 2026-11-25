package com.meshi.fairy;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;

// brewing stand: fairy mash on a water bottle -> fairy flask
public class FairyFlaskBrewing implements IBrewingRecipe {
    @Override
    public boolean isInput(ItemStack input) {
        if (!input.is(Items.POTION)) return false;
        return input.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion()
                .flatMap(h -> Potions.WATER.unwrapKey().map(k -> h.is(k)))
                .orElse(false);
    }

    @Override
    public boolean isIngredient(ItemStack ingredient) {
        return ingredient.is(ModItems.FAIRY_MASH.get());
    }

    @Override
    public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
        if (!isInput(input) || !isIngredient(ingredient)) return ItemStack.EMPTY;
        return new ItemStack(ModItems.FAIRY_FLASK.get());
    }
}
