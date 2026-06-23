package net.alkeari.geotectonic.mixin;

import net.alkeari.geotectonic.GeoTectonic;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

// Tectonic injects its worldgen as a built-in datapack at Pack.Position.TOP, which outranks
// normal mod resources and reverts GeoTectonic's cave-suppression overrides. Force GeoTectonic's
// override pack to the end of the selected list (highest priority) so it wins regardless of how
// any other pack positions itself.
@Mixin(PackRepository.class)
public class PackRepositoryMixin {
    @Inject(method = "openAllSelected", at = @At("RETURN"), cancellable = true)
    private void geotectonic$prioritizeOverrides(CallbackInfoReturnable<List<PackResources>> cir) {
        List<PackResources> packs = cir.getReturnValue();
        PackResources override = null;
        for (PackResources pack : packs) {
            if (pack.packId().contains(GeoTectonic.OVERRIDE_PACK_ID)) {
                override = pack;
                break;
            }
        }
        if (override == null || packs.get(packs.size() - 1) == override) {
            return;
        }
        List<PackResources> reordered = new ArrayList<>(packs);
        reordered.remove(override);
        reordered.add(override);
        cir.setReturnValue(reordered);
    }
}
