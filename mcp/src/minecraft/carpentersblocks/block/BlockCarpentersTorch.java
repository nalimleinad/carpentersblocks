package carpentersblocks.block;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Icon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import carpentersblocks.CarpentersBlocks;
import carpentersblocks.data.Torch;
import carpentersblocks.data.Torch.State;
import carpentersblocks.tileentity.TEBase;
import carpentersblocks.tileentity.TECarpentersTorch;
import carpentersblocks.util.BlockProperties;
import carpentersblocks.util.registry.BlockRegistry;
import carpentersblocks.util.registry.FeatureRegistry;
import carpentersblocks.util.registry.IconRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockCarpentersTorch extends BlockCoverable {

    public BlockCarpentersTorch(int blockID, Material material)
    {
        super(blockID, material);
    }

    @SideOnly(Side.CLIENT)
    @Override
    /**
     * When this method is called, your block should register all the icons it needs with the given IconRegister. This
     * is the only chance you get to register icons.
     */
    public void registerIcons(IconRegister iconRegister)
    {
        IconRegistry.icon_torch_lit             = iconRegister.registerIcon(CarpentersBlocks.MODID + ":" + "torch/torch_lit");
        IconRegistry.icon_torch_head_smoldering = iconRegister.registerIcon(CarpentersBlocks.MODID + ":" + "torch/torch_head_smoldering");
        IconRegistry.icon_torch_head_unlit      = iconRegister.registerIcon(CarpentersBlocks.MODID + ":" + "torch/torch_head_unlit");
    }

    @SideOnly(Side.CLIENT)
    @Override
    /**
     * Returns the icon on the side given the block metadata.
     */
    public Icon getIcon(int side, int metadata)
    {
        return IconRegistry.icon_torch_lit;
    }

    /**
     * Called when block is activated (right-click), before normal processing resumes.
     */
    @Override
    protected void preOnBlockActivated(TEBase TE, EntityPlayer entityPlayer, int side, float hitX, float hitY, float hitZ, List<Boolean> altered, List<Boolean> decInv)
    {
        ItemStack itemStack = entityPlayer.getHeldItem();

        if (itemStack != null && itemStack.getItem() instanceof ItemBlock) {
            if (!Torch.getState(TE).equals(State.LIT)) {
                Block block = BlockProperties.toBlock(itemStack);
                if (block.equals(BlockRegistry.blockCarpentersTorch) || block.equals(Block.torchWood)) {
                    Torch.setState(TE, State.LIT);
                    altered.add(true);
                }
            }
        }
    }

    @Override
    /**
     * Returns light value based on cover or side covers.
     */
    public int getLightValue(IBlockAccess world, int x, int y, int z)
    {
        TEBase TE = getTileEntity(world, x, y, z);

        if (TE != null) {

            int coverLight = super.getLightValue(world, x, y, z);
            int torchLight = 0;

            switch (Torch.getState(TE)) {
                case LIT:
                    torchLight = 15;
                    break;
                case SMOLDERING:
                    torchLight = 10;
                    break;
                default: {}
            }

            return coverLight > torchLight ? coverLight : torchLight;

        }

        return lightValue[blockID];
    }

    /**
     * Returns a bounding box from the pool of bounding boxes (this means this box can change after the pool has been
     * cleared to be reused)
     */
    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World par1World, int par2, int par3, int par4)
    {
        return null;
    }

    @Override
    /**
     * checks to see if you can place this block can be placed on that side of a block: BlockLever overrides
     */
    public boolean canPlaceBlockOnSide(World world, int x, int y, int z, int side)
    {
        if (side > 0) {

            ForgeDirection dir = ForgeDirection.getOrientation(side);
            Block blockYN = Block.blocksList[world.getBlockId(x, y - 1, z)];
            Block blockOffset = Block.blocksList[world.getBlockId(x - dir.offsetX, y - dir.offsetY, z - dir.offsetZ)];

            return blockOffset != null && blockOffset.isBlockSolidOnSide(world, x - dir.offsetX, y - dir.offsetY, z - dir.offsetZ, dir) || side == 1 && blockYN != null && blockYN.canPlaceTorchOnTop(world, x, y, z);

        }

        return false;
    }

    @Override
    /**
     * Called when a block is placed using its ItemBlock. Args: World, X, Y, Z, side, hitX, hitY, hitZ, block metadata
     */
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata)
    {
        return side;
    }

    @Override
    /**
     * Called when the block is placed in the world.
     */
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entityLiving, ItemStack itemStack)
    {
        TEBase TE = getTileEntity(world, x, y, z);

        if (TE != null) {

            int facing = world.getBlockMetadata(x, y, z);

            Torch.setFacing(TE, facing);
            Torch.setReady(TE);

        }

        super.onBlockPlacedBy(world, x, y, z, entityLiving, itemStack);
    }

    @Override
    /**
     * Lets the block know when one of its neighbor changes. Doesn't know which neighbor changed (coordinates passed are
     * their own) Args: x, y, z, neighbor blockID
     */
    public void onNeighborBlockChange(World world, int x, int y, int z, int blockID)
    {
        if (!world.isRemote) {

            TEBase TE = getTileEntity(world, x, y, z);

            if (TE != null && Torch.isReady(TE) && !canPlaceBlockOnSide(world, x, y, z, Torch.getFacing(TE).ordinal())) {
                dropBlockAsItem(world, x, y, z, 0, 0);
                world.setBlockToAir(x, y, z);
            }

        }

        super.onNeighborBlockChange(world, x, y, z, blockID);
    }

    /**
     * Ray traces through the blocks collision from start vector to end vector returning a ray trace hit. Args: world,
     * x, y, z, startVec, endVec
     */
    @Override
    public MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3 startVec, Vec3 endVec)
    {
        TEBase TE = getTileEntity(world, x, y, z);

        if (TE != null) {

            ForgeDirection facing = Torch.getFacing(TE);

            switch (facing) {
                case NORTH:
                    setBlockBounds(0.5F - 0.15F, 0.2F, 1.0F - 0.15F * 2.0F, 0.5F + 0.15F, 0.8F, 1.0F);
                    break;
                case SOUTH:
                    setBlockBounds(0.5F - 0.15F, 0.2F, 0.0F, 0.5F + 0.15F, 0.8F, 0.15F * 2.0F);
                    break;
                case WEST:
                    setBlockBounds(1.0F - 0.15F * 2.0F, 0.2F, 0.5F - 0.15F, 1.0F, 0.8F, 0.5F + 0.15F);
                    break;
                case EAST:
                    setBlockBounds(0.0F, 0.2F, 0.5F - 0.15F, 0.15F * 2.0F, 0.8F, 0.5F + 0.15F);
                    break;
                default:
                    setBlockBounds(0.5F - 0.1F, 0.0F, 0.5F - 0.1F, 0.5F + 0.1F, 0.6F, 0.5F + 0.1F);
                    break;
            }

        }

        return super.collisionRayTrace(world, x, y, z, startVec, endVec);
    }

    /**
     * Ticks the block if it's been scheduled
     */
    @Override
    public void updateTick(World world, int x, int y, int z, Random random)
    {
        if (!world.isRemote) {

            TEBase TE = getTileEntity(world, x, y, z);

            if (TE != null) {

                boolean isWet = world.isRaining() && world.canBlockSeeTheSky(x, y, z) && world.getBiomeGenForCoords(x, z).rainfall > 0.0F;
                boolean canDropState = FeatureRegistry.enableTorchWeatherEffects;

                /* Search spaces above for solid faces that could block weather effects. */

                if (isWet) {
                    for (int yInc = 1; yInc < world.getHeight(); ++yInc) {
                        if (world.isBlockSolidOnSide(x, y + yInc, z, ForgeDirection.UP) || world.isBlockSolidOnSide(x, y + yInc, z, ForgeDirection.DOWN)) {
                            isWet = false;
                            break;
                        }
                    }
                }

                switch (Torch.getState(TE)) {
                    case LIT:
                        if (canDropState && isWet) {
                            Torch.setState(TE, State.SMOLDERING);
                        }
                        break;
                    case SMOLDERING:
                        if (canDropState && isWet) {
                            Torch.setState(TE, State.UNLIT);
                        } else {
                            Torch.setState(TE, State.LIT);
                        }
                        break;
                    case UNLIT:
                        if (!canDropState || !isWet) {
                            Torch.setState(TE, State.SMOLDERING);
                        }
                        break;
                    default: {}
                }

            }

        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    /**
     * A randomly called display update to be able to add particles or other items for display
     */
    public void randomDisplayTick(World world, int x, int y, int z, Random random)
    {
        TEBase TE = getTileEntity(world, x, y, z);

        if (TE != null) {

            State state = Torch.getState(TE);

            if (!state.equals(State.UNLIT)) {

                double[] headCoords = Torch.getHeadCoordinates(TE);

                world.spawnParticle("smoke", headCoords[0], headCoords[1], headCoords[2], 0.0D, 0.0D, 0.0D);

                if (state.equals(State.LIT)) {
                    world.spawnParticle("flame", headCoords[0], headCoords[1], headCoords[2], 0.0D, 0.0D, 0.0D);
                }

            }

        }
    }

    @Override
    public TileEntity createNewTileEntity(World world)
    {
        return new TECarpentersTorch();
    }

    @Override
    /**
     * The type of render function that is called for this block
     */
    public int getRenderType()
    {
        return BlockRegistry.carpentersTorchRenderID;
    }

}
