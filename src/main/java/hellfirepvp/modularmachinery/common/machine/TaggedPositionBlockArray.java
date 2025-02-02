/*******************************************************************************
 * HellFirePvP / Modular Machinery 2018
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.machine;

import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentSelectorTag;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import net.minecraft.util.math.BlockPos;
import stanhebben.zenscript.annotations.ZenClass;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: TaggedPositionBlockArray
 * Created by HellFirePvP
 * Date: 04.03.2019 / 21:33
 */
@ZenRegister
@ZenClass("mods.modularmachinery.TaggedPositionBlockArray")
public class TaggedPositionBlockArray extends BlockArray {
    public TaggedPositionBlockArray() {
    }
    public TaggedPositionBlockArray(long traitNum) {
        super(traitNum);
    }

    private final Map<BlockPos, ComponentSelectorTag> taggedPositions = new HashMap<>();

    public void setTag(BlockPos pos, ComponentSelectorTag tag) {
        this.taggedPositions.put(pos, tag);
    }

    @Nullable
    public ComponentSelectorTag getTag(BlockPos pos) {
        return this.taggedPositions.get(pos);
    }

    @Override
    public TaggedPositionBlockArray rotateYCCW() {
        TaggedPositionBlockArray out = new TaggedPositionBlockArray(traitNum);

        Map<BlockPos, BlockInformation> outPattern = out.pattern;
        for (BlockPos pos : pattern.keySet()) {
            outPattern.put(MiscUtils.rotateYCCW(pos), pattern.get(pos).copyRotateYCCW());
        }
        for (BlockPos pos : taggedPositions.keySet()) {
            out.taggedPositions.put(MiscUtils.rotateYCCW(pos), taggedPositions.get(pos));
        }

        return out;
    }
}
