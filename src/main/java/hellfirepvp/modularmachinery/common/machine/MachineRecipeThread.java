package hellfirepvp.modularmachinery.common.machine;

import crafttweaker.annotations.ZenRegister;
import github.kasuminova.mmce.common.concurrent.RecipeSearchTask;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.RecipeRegistry;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftingStatus;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import io.netty.util.internal.ThrowableUtil;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import stanhebben.zenscript.annotations.ZenClass;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

@ZenRegister
@ZenClass("mods.modularmachinery.MachineRecipeThread")
public class MachineRecipeThread extends RecipeThread {
    private final TileMachineController controller;
    private RecipeSearchTask searchTask = null;

    public MachineRecipeThread(TileMachineController ctrl) {
        super(ctrl);
        this.controller = ctrl;
    }

    @Override
    public void fireFinishedEvent() {
        controller.onFinished();
    }

    @Override
    public void tryRestartRecipe() {
        activeRecipe.reset();
        activeRecipe.setMaxParallelism(ctrl.getMaxParallelism());
        context = createContext(activeRecipe);

        RecipeCraftingContext.CraftingCheckResult result = ctrl.onCheck(context);
        if (result.isSuccess()) {
            controller.onStart();
        } else {
            activeRecipe = null;
            context = null;
            status = CraftingStatus.IDLE;
            createRecipeSearchTask();
        }
    }

    @Override
    public RecipeCraftingContext createContext(ActiveMachineRecipe activeRecipe) {
        return controller.createContext(activeRecipe);
    }

    @Override
    public void searchAndStartRecipe() {
        if (searchTask != null) {
            if (!searchTask.isDone()) {
                return;
            }

            RecipeCraftingContext context = null;
            try {
                context = searchTask.get();
                status = searchTask.getStatus();
            } catch (Exception e) {
                ModularMachinery.log.warn(ThrowableUtil.stackTraceToString(e));
            }
            searchTask = null;

            if (context == null) {
                return;
            }

            if (context.canStartCrafting().isSuccess()) {
                this.context = context;
                this.activeRecipe = context.getActiveRecipe();
                this.status = CraftingStatus.SUCCESS;
                controller.onStart();
            }
        } else {
            if (ctrl.getTicksExisted() % controller.currentRecipeSearchDelay() == 0) {
                createRecipeSearchTask();
            }
        }
    }

    protected void createRecipeSearchTask() {
        TileMachineController controller = this.controller;
        assert controller.getFoundMachine() != null;

        searchTask = new RecipeSearchTask(
                controller,
                controller.getFoundMachine(),
                controller.getMaxParallelism(),
                RecipeRegistry.getRecipesFor(controller.getFoundMachine()));
        ForkJoinPool.commonPool().submit(searchTask);
    }

    public NBTTagCompound serialize(NBTTagCompound tag) {
        tag.setTag("statusTag", status.serialize());
        if (activeRecipe != null && activeRecipe.getRecipe() != null) {
            tag.setTag("activeRecipe", activeRecipe.serialize());
        }
        if (!permanentModifiers.isEmpty()) {
            NBTTagList tagList = new NBTTagList();
            permanentModifiers.forEach((key, modifier) -> {
                if (key != null && modifier != null) {
                    NBTTagCompound modifierTag = new NBTTagCompound();
                    modifierTag.setString("key", key);
                    modifierTag.setTag("modifier", modifier.serialize());
                    tagList.appendTag(modifierTag);
                }
            });
            tag.setTag("permanentModifiers", tagList);
        }
        if (!semiPermanentModifiers.isEmpty()) {
            NBTTagList tagList = new NBTTagList();
            semiPermanentModifiers.forEach((key, modifier) -> {
                if (key != null && modifier != null) {
                    NBTTagCompound modifierTag = new NBTTagCompound();
                    modifierTag.setString("key", key);
                    modifierTag.setTag("modifier", modifier.serialize());
                    tagList.appendTag(modifierTag);
                }
            });
            tag.setTag("semiPermanentModifiers", tagList);
        }
        return tag;
    }

    public static MachineRecipeThread deserialize(NBTTagCompound tag, TileMachineController ctrl) {
        if (!tag.hasKey("statusTag")) {
            return null;
        }

        Map<String, RecipeModifier> permanentModifiers = new HashMap<>();
        if (tag.hasKey("permanentModifiers", Constants.NBT.TAG_LIST)) {
            NBTTagList tagList = tag.getTagList("permanentModifiers", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < tagList.tagCount(); i++) {
                NBTTagCompound modifierTag = tagList.getCompoundTagAt(i);
                permanentModifiers.put(modifierTag.getString("key"), RecipeModifier.deserialize(modifierTag.getCompoundTag("modifier")));
            }
        }

        Map<String, RecipeModifier> semiPermanentModifiers = new HashMap<>();
        if (tag.hasKey("semiPermanentModifiers", Constants.NBT.TAG_LIST)) {
            NBTTagList tagList = tag.getTagList("semiPermanentModifiers", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < tagList.tagCount(); i++) {
                NBTTagCompound modifierTag = tagList.getCompoundTagAt(i);
                semiPermanentModifiers.put(modifierTag.getString("key"), RecipeModifier.deserialize(modifierTag.getCompoundTag("modifier")));
            }
        }

        ActiveMachineRecipe activeRecipe = deserializeActiveRecipe(tag, ctrl);
        MachineRecipeThread thread = (MachineRecipeThread) new MachineRecipeThread(ctrl)
                .setActiveRecipe(activeRecipe)
                .setStatus(CraftingStatus.deserialize(tag.getCompoundTag("statusTag")));
        thread.permanentModifiers.putAll(permanentModifiers);
        thread.semiPermanentModifiers.putAll(semiPermanentModifiers);

        // Simple Thread
        return thread;
    }
}
