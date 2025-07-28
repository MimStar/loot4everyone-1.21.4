package com.loot4everyone.mixin;
import com.loot4everyone.Loot4Everyone;
import com.loot4everyone.StateSaverAndLoader;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "canExplosionDestroyBlock", at = @At("HEAD"), cancellable = true)
    public void canExplosionDestroyBlock(Explosion explosion, BlockView world, BlockPos pos, BlockState state, float explosionPower, CallbackInfoReturnable<Boolean> cir){
        if (state.getBlock() instanceof ChestBlock || state.getBlock() instanceof BarrelBlock) {
            if (StateSaverAndLoader.getSettingsState(Loot4Everyone.server).getLootProtection() && StateSaverAndLoader.isBarrelStatePresent(Loot4Everyone.server,pos)){ //same as isChestStatePresent, but we only have info on the blockPos
                cir.setReturnValue(false);
            }
        }
    }
}