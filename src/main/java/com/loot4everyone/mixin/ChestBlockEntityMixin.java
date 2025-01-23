package com.loot4everyone.mixin;

import com.loot4everyone.PersistentLootTableAccessor;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(ChestBlockEntity.class)
public abstract class ChestBlockEntityMixin implements PersistentLootTableAccessor {


    @Override
    public String getPersistentLootTable(){
        ChestBlockEntity chest = (ChestBlockEntity) (Object) this;
        NbtCompound nbt = new NbtCompound();
        writeNbt(nbt, Objects.requireNonNull(chest.getWorld()).getRegistryManager());
        if (nbt.contains("PersistentLootTable")){
            return nbt.getString("PersistentLootTable");
        }
        return "";
    }

    @Shadow protected abstract void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries);

    @Inject(method = "writeNbt", at = @At("TAIL"))
    private void onWriteNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries, CallbackInfo ci) {
        if (!nbt.contains("PersistentLootTable") && nbt.contains("LootTable")){
            nbt.putString("PersistentLootTable", nbt.getString("LootTable"));
        }
    }
}
