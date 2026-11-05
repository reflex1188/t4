package dev.jax.jaxclient.mixin;

import dev.jax.jaxclient.JaxClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void jaxclient$forceGlow(CallbackInfoReturnable<Boolean> cir) {
        if (JaxClient.wantsGlow((Entity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
