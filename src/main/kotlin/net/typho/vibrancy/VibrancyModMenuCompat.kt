package net.typho.vibrancy

//? fabric {
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi

object VibrancyModMenuCompat : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory { parent -> VibrancyConfig.createScreen(parent) }
    }
}
//? } neoforge {
/*import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.client.gui.IConfigScreenFactory

@Mod(value = Vibrancy.MOD_ID, dist = [Dist.CLIENT])
class VibrancyModMenuCompat(eventBus: IEventBus, modContainer: ModContainer) {
    init {
        modContainer.registerExtensionPoint(IConfigScreenFactory::class.java, IConfigScreenFactory { container, modListScreen -> VibrancyConfig.createScreen(modListScreen) })
    }
}
*///? }