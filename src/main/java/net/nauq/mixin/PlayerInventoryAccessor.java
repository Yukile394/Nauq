package net.nauq.mixin;

import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// selectedSlot 1.21.x boyunca ayni isimle var (yeni surumlerde private), accessor hepsinde calisir
@Mixin(PlayerInventory.class)
public interface PlayerInventoryAccessor {
    @Accessor("selectedSlot") int nauq$getSelectedSlot();
    @Accessor("selectedSlot") void nauq$setSelectedSlot(int slot);
}
