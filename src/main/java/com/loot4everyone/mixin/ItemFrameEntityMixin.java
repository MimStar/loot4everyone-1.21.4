package com.loot4everyone.mixin;

import com.loot4everyone.ItemFrameData;
import com.loot4everyone.Loot4Everyone;
import com.loot4everyone.StateSaverAndLoader;
import net.minecraft.entity.decoration.ItemFrameEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(ItemFrameEntity.class)
public abstract class ItemFrameEntityMixin {
    @Inject(method = "onPlace", at = @At("HEAD"))
    private void onPlace(CallbackInfo ci){
        ItemFrameEntity itemFrame = (ItemFrameEntity) (Object) this;
        ItemFrameData itemFrameData = StateSaverAndLoader.getItemFrameState(Loot4Everyone.server, itemFrame.getBlockPos());
        itemFrameData.setPlayerPlaced(true);
        itemFrameData.getPlayersUsed().clear();
        StateSaverAndLoader.saveState(Objects.requireNonNull(itemFrame.getServer()));
    }
}
