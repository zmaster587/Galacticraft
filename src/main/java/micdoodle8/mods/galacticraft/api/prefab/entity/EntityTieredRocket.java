package micdoodle8.mods.galacticraft.api.prefab.entity;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import micdoodle8.mods.galacticraft.api.entity.IDockable;
import micdoodle8.mods.galacticraft.api.entity.IRocketType;
import micdoodle8.mods.galacticraft.api.entity.IWorldTransferCallback;
import micdoodle8.mods.galacticraft.api.tile.ILandingPadAttachable;
import micdoodle8.mods.galacticraft.api.world.IOrbitDimension;
import micdoodle8.mods.galacticraft.core.GalacticraftCore;
import micdoodle8.mods.galacticraft.core.blocks.BlockLandingPadFull;
import micdoodle8.mods.galacticraft.core.entities.player.GCEntityPlayerMP;
import micdoodle8.mods.galacticraft.core.event.EventLandingPadRemoval;
import micdoodle8.mods.galacticraft.core.network.PacketSimple;
import micdoodle8.mods.galacticraft.core.network.PacketSimple.EnumSimplePacket;
import micdoodle8.mods.galacticraft.core.tile.TileEntityFuelLoader;
import micdoodle8.mods.galacticraft.core.util.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;

/**
 * Do not include this prefab class in your released mod download.
 */
public abstract class EntityTieredRocket extends EntityAutoRocket implements IRocketType, IDockable, IInventory, IWorldTransferCallback
{
	public EnumRocketType rocketType;
	public float rumble;
	public int launchCooldown;

	public EntityTieredRocket(World par1World)
	{
		super(par1World);
		this.setSize(0.98F, 4F);
		this.yOffset = this.height / 2.0F;
	}

	public EntityTieredRocket(World world, double posX, double posY, double posZ)
	{
		super(world, posX, posY, posZ);
	}

	@Override
	protected void entityInit()
	{
		super.entityInit();

		if (Loader.isModLoaded("ICBM|Explosion"))
		{
			try
			{
				Class.forName("calclavia.api.icbm.RadarRegistry").getMethod("register", Entity.class).invoke(null, this);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	public void setDead()
	{
		super.setDead();

		if (Loader.isModLoaded("ICBM|Explosion"))
		{
			try
			{
				Class.forName("calclavia.api.icbm.RadarRegistry").getMethod("unregister", Entity.class).invoke(null, this);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public void igniteCheckingCooldown()
	{
		if (!this.worldObj.isRemote && this.launchCooldown <= 0)
		{
			this.ignite();
		}
	}

	@Override
	public void onUpdate()
	{
		if (this.getWaitForPlayer())
		{
			if (this.riddenByEntity != null)
			{
				if (this.ticks >= 40)
				{
					if (!this.worldObj.isRemote)
					{
						Entity e = this.riddenByEntity;
						this.riddenByEntity.ridingEntity = null;
						this.riddenByEntity = null;
						e.mountEntity(this);
					}

					this.setWaitForPlayer(false);
					this.motionY = -0.5D;
				}
				else
				{
					this.motionX = this.motionY = this.motionZ = 0.0D;
					this.riddenByEntity.motionX = this.riddenByEntity.motionY = this.riddenByEntity.motionZ = 0;
				}
			}
			else
			{
				this.motionX = this.motionY = this.motionZ = 0.0D;
			}
		}

		super.onUpdate();

		if (this.landing)
		{
			this.rotationPitch = this.rotationYaw = 0;
		}

		if (!this.worldObj.isRemote)
		{
			if (this.launchCooldown > 0)
			{
				this.launchCooldown--;
			}
		}

		if (!this.worldObj.isRemote && this.getLandingPad() != null && this.getLandingPad().getConnectedTiles() != null)
		{
			for (ILandingPadAttachable tile : this.getLandingPad().getConnectedTiles())
			{
				if (this.worldObj.getTileEntity(((TileEntity) tile).xCoord, ((TileEntity) tile).yCoord, ((TileEntity) tile).zCoord) != null && this.worldObj.getTileEntity(((TileEntity) tile).xCoord, ((TileEntity) tile).yCoord, ((TileEntity) tile).zCoord) instanceof TileEntityFuelLoader)
				{
					if (tile instanceof TileEntityFuelLoader && ((TileEntityFuelLoader) tile).getEnergyStoredGC() > 0)
					{
						if (this.launchPhase == EnumLaunchPhase.LAUNCHED.getPhase())
						{
							this.setPad(null);
						}
					}
				}
			}
		}

		if (this.rumble > 0)
		{
			this.rumble--;
		}

		if (this.rumble < 0)
		{
			this.rumble++;
		}

		if (this.riddenByEntity != null)
		{
			this.riddenByEntity.posX += this.rumble / 30F;
			this.riddenByEntity.posZ += this.rumble / 30F;
		}

		if (this.launchPhase == EnumLaunchPhase.IGNITED.getPhase() || this.launchPhase == EnumLaunchPhase.LAUNCHED.getPhase())
		{
			this.performHurtAnimation();

			this.rumble = (float) this.rand.nextInt(3) - 3;
		}

		if (!this.worldObj.isRemote)
		{
			this.lastLastMotionY = this.lastMotionY;
			this.lastMotionY = this.motionY;
		}
	}

	@Override
	public void decodePacketdata(ByteBuf buffer)
	{
		this.rocketType = EnumRocketType.values()[buffer.readInt()];
		super.decodePacketdata(buffer);

		if (buffer.readBoolean())
		{
			this.posX = buffer.readDouble() / 8000.0D;
			this.posY = buffer.readDouble() / 8000.0D;
			this.posZ = buffer.readDouble() / 8000.0D;
		}
	}

	@Override
	public void getNetworkedData(ArrayList<Object> list)
	{
		list.add(this.rocketType != null ? this.rocketType.getIndex() : 0);
		super.getNetworkedData(list);

		boolean sendPosUpdates = this.ticks < 25 || this.launchPhase != EnumLaunchPhase.LAUNCHED.ordinal();
		list.add(sendPosUpdates);

		if (sendPosUpdates)
		{
			list.add(this.posX * 8000.0D);
			list.add(this.posY * 8000.0D);
			list.add(this.posZ * 8000.0D);
		}
	}

	@Override
	public void handlePacketData(Side side, EntityPlayer player)
	{
	}

	@Override
	public void onReachAtmoshpere()
	{
		if (this.destinationFrequency != -1)
		{
			if (this.worldObj.isRemote)
			{
				return;
			}

			this.setTarget(true, this.destinationFrequency);

			if (this.targetVec != null)
			{
				if (this.targetDimension != this.worldObj.provider.dimensionId)
				{
					WorldServer worldServer = FMLCommonHandler.instance().getMinecraftServerInstance().worldServerForDimension(this.targetDimension);

					if (!this.worldObj.isRemote && worldServer != null)
					{
						if (this.riddenByEntity != null)
						{
							WorldUtil.transferEntityToDimension(this.riddenByEntity, this.targetDimension, worldServer, false, this);
						}
					}
				}
				else
				{
					this.setPosition(this.targetVec.x + 0.5F, this.targetVec.y + 800, this.targetVec.z + 0.5F);
					this.landing = true;
				}
			}
			else
			{
				this.setDead();
			}
		}
		else
		{
			if (this.riddenByEntity != null)
			{
				if (this.riddenByEntity instanceof GCEntityPlayerMP)
				{
					GCEntityPlayerMP player = (GCEntityPlayerMP) this.riddenByEntity;

					HashMap<String, Integer> map = WorldUtil.getArrayOfPossibleDimensions(WorldUtil.getPossibleDimensionsForSpaceshipTier(this.getRocketTier()), player);

					String temp = "";
					int count = 0;

					for (Entry<String, Integer> entry : map.entrySet())
					{
						temp = temp.concat(entry.getKey() + (count < map.entrySet().size() - 1 ? "." : ""));
						count++;
					}

					GalacticraftCore.packetPipeline.sendTo(new PacketSimple(EnumSimplePacket.C_UPDATE_DIMENSION_LIST, new Object[] { player.getGameProfile().getName(), temp }), player);
					player.setSpaceshipTier(this.getRocketTier());
					player.setUsingPlanetGui();

					this.onTeleport(player);
					player.mountEntity(this);

					if (!this.isDead)
					{
						this.setDead();
					}
				}
			}
		}
	}

	@Override
	protected boolean shouldCancelExplosion()
	{
		return this.hasValidFuel() && Math.abs(this.lastLastMotionY) < 4;
	}

	public void onTeleport(EntityPlayerMP player)
	{
		;
	}

	@Override
	protected void onRocketLand(int x, int y, int z)
	{
		super.onRocketLand(x, y, z);
		this.launchCooldown = 40;
		this.setPositionAndRotation(x + 0.5, y + 1.8D, z + 0.5, this.rotationYaw, 0.0F);
	}

	@Override
	public void onLaunch()
	{
		super.onLaunch();

		if (!this.worldObj.isRemote)
		{
			if (!(this.worldObj.provider instanceof IOrbitDimension) && this.riddenByEntity != null && this.riddenByEntity instanceof GCEntityPlayerMP)
			{
				((GCEntityPlayerMP) this.riddenByEntity).setCoordsTeleportedFromX(this.riddenByEntity.posX);
				((GCEntityPlayerMP) this.riddenByEntity).setCoordsTeleportedFromZ(this.riddenByEntity.posZ);
			}

			int amountRemoved = 0;

			for (int x = MathHelper.floor_double(this.posX) - 1; x <= MathHelper.floor_double(this.posX) + 1; x++)
			{
				for (int y = MathHelper.floor_double(this.posY) - 3; y <= MathHelper.floor_double(this.posY) + 1; y++)
				{
					for (int z = MathHelper.floor_double(this.posZ) - 1; z <= MathHelper.floor_double(this.posZ) + 1; z++)
					{
						final Block block = this.worldObj.getBlock(x, y, z);

						if (block instanceof BlockLandingPadFull)
						{
							if (amountRemoved < 9)
							{
								EventLandingPadRemoval event = new EventLandingPadRemoval(this.worldObj, x, y, z);
								MinecraftForge.EVENT_BUS.post(event);

								if (event.allow)
								{
									this.worldObj.setBlockToAir(x, y, z);
									amountRemoved = 9;
								}
							}
						}
					}
				}
			}

			this.playSound("random.pop", 0.2F, ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.7F + 1.0F) * 2.0F);
		}
	}

	@Override
	protected boolean shouldMoveClientSide()
	{
		return true;
	}

	@Override
	public boolean interactFirst(EntityPlayer par1EntityPlayer)
	{
		if (this.launchPhase == EnumLaunchPhase.LAUNCHED.getPhase())
		{
			return false;
		}

		if (this.riddenByEntity != null && this.riddenByEntity instanceof GCEntityPlayerMP)
		{
			if (!this.worldObj.isRemote)
			{
				GalacticraftCore.packetPipeline.sendTo(new PacketSimple(EnumSimplePacket.C_RESET_THIRD_PERSON, new Object[] { ((EntityPlayerMP) this.riddenByEntity).getGameProfile().getName() }), (EntityPlayerMP) par1EntityPlayer);
				GalacticraftCore.packetPipeline.sendTo(new PacketSimple(EnumSimplePacket.C_ZOOM_CAMERA, new Object[] { 0 }), (EntityPlayerMP) par1EntityPlayer);
				((GCEntityPlayerMP) par1EntityPlayer).setChatCooldown(0);
				par1EntityPlayer.mountEntity(null);
			}

			return true;
		}
		else if (par1EntityPlayer instanceof GCEntityPlayerMP)
		{
			if (!this.worldObj.isRemote)
			{
				GalacticraftCore.packetPipeline.sendTo(new PacketSimple(EnumSimplePacket.C_MOUNT_ROCKET, new Object[] { par1EntityPlayer.getGameProfile().getName() }), (EntityPlayerMP) par1EntityPlayer);
				GalacticraftCore.packetPipeline.sendTo(new PacketSimple(EnumSimplePacket.C_ZOOM_CAMERA, new Object[] { 1 }), (EntityPlayerMP) par1EntityPlayer);
				((GCEntityPlayerMP) par1EntityPlayer).setChatCooldown(0);
				par1EntityPlayer.mountEntity(this);
			}

			return true;
		}

		return false;
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbt)
	{
		nbt.setInteger("Type", this.rocketType.getIndex());
		super.writeEntityToNBT(nbt);
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbt)
	{
		this.rocketType = EnumRocketType.values()[nbt.getInteger("Type")];
		super.readEntityFromNBT(nbt);
	}

	@Override
	public EnumRocketType getType()
	{
		return this.rocketType;
	}

	@Override
	public int getSizeInventory()
	{
		return this.rocketType.getInventorySpace();
	}

	@Override
	public void onWorldTransferred(World world)
	{
		if (this.targetVec != null)
		{
			this.setPosition(this.targetVec.x + 0.5F, this.targetVec.y + 800, this.targetVec.z + 0.5F);
			this.landing = true;
			this.setWaitForPlayer(true);
			this.motionX = this.motionY = this.motionZ = 0.0D;
		}
		else
		{
			this.setDead();
		}
	}

	@Override
	public void updateRiderPosition()
	{
		if (this.riddenByEntity != null)
		{
			this.riddenByEntity.setPosition(this.posX, this.posY + this.getMountedYOffset() + this.riddenByEntity.getYOffset(), this.posZ);
		}
	}

	@Override
	public boolean isPlayerRocket()
	{
		return true;
	}
}
