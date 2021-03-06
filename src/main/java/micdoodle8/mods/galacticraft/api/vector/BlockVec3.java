package micdoodle8.mods.galacticraft.api.vector;

import net.minecraft.block.Block;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ReportedException;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.ForgeDirection;

/* BlockVec3 is similar to galacticraft.api.vector.Vector3?
 * 
 * But for speed it uses integer arithmetic not doubles, for block coordinates
 * This reduces unnecessary type conversion between integers and doubles and back again.
 * (Minecraft block coordinates are always integers, only entity coordinates are doubles.)
 * 
 * Note also when writing NBT data BlockVec3 writes and reads its coordinates as doubles,
 * for 100% save-file compatibility with prior code using Vector3.
 */
public class BlockVec3 implements Cloneable
{
	public int x;
	public int y;
	public int z;
	public boolean[] sideDone = {false,false,false,false,false,false};
	private static Chunk chunkCached;
	private static int chunkCacheX = 1876000; // outside the world edge
	private static int chunkCacheZ = 1876000; // outside the world edge
	

	public BlockVec3()
	{
		this(0, 0, 0);
	}

	public BlockVec3(int x, int y, int z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public BlockVec3(Entity par1)
	{
		this.x = (int) Math.floor(par1.posX);
		this.y = (int) Math.floor(par1.posY);
		this.z = (int) Math.floor(par1.posZ);
	}

	public BlockVec3(TileEntity par1)
	{
		this.x = par1.xCoord;
		this.y = par1.yCoord;
		this.z = par1.zCoord;
	}

	/**
	 * Makes a new copy of this Vector. Prevents variable referencing problems.
	 */
	@Override
	public BlockVec3 clone()
	{
		return new BlockVec3(this.x, this.y, this.z);
	}

	/**
	 * Get block ID at the BlockVec3 coordinates.
	 * 
	 * @param world
	 * @return the block ID, or -1 if the y-coordinate is less than 0 or greater than 256 or the x or z is outside the Minecraft worldmap. 
	 * Returns -2 if the coordinates being checked are in an unloaded chunk
	 */
	public Block getBlockID(World world)
	{
		if (this.y < 0 || this.y >= 256 || this.x < -30000000 || this.z < -30000000 || this.x >= 30000000 || this.z >= 30000000)
		{
			return null;
		}

		int chunkx = this.x >> 4;
		int chunkz = this.z >> 4;
		try
		{
			if (world.getChunkProvider().chunkExists(chunkx,chunkz))
			{
				// In a typical inner loop, 80% of the time consecutive calls to
				// this will be within the same chunk
				if (BlockVec3.chunkCacheX == chunkx && BlockVec3.chunkCacheZ == chunkz && BlockVec3.chunkCached.isChunkLoaded)
				{
					return BlockVec3.chunkCached.getBlock(this.x & 15, this.y, this.z & 15);
				}
				else
				{
					Chunk chunk = null;
					chunk = world.getChunkFromChunkCoords(chunkx, chunkz);
					BlockVec3.chunkCached = chunk;
					BlockVec3.chunkCacheX = chunkx;
					BlockVec3.chunkCacheZ = chunkz;
					return chunk.getBlock(this.x & 15, this.y, this.z & 15);
				}
			}
			//Chunk doesn't exist - meaning, it is not loaded
			return Blocks.bedrock;
		}
		catch (Throwable throwable)
		{
			CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Oxygen Sealer thread: Exception getting block type in world");
			CrashReportCategory crashreportcategory = crashreport.makeCategory("Requested block coordinates");
			crashreportcategory.addCrashSection("Location", CrashReportCategory.getLocationInfo(this.x, this.y, this.z));
			throw new ReportedException(crashreport);
		}
	}

	/**
	 * Get block ID at the BlockVec3 coordinates.
	 * Only call this 'safe' version if x and z coordinates are within the Minecraft world map (-30m to +30m)
	 * @param world
	 * @return the block ID, or -1 if the y-coordinate is less than 0 or greater than 256. 
	 * Returns -2 if the coordinates being checked are in an unloaded chunk
	 */
	public Block getBlockIDsafe(World world)
	{
		if (this.y < 0 || this.y >= 256)
		{
			return null;
		}

		int chunkx = this.x >> 4;
		int chunkz = this.z >> 4;
		try
		{
			if (world.getChunkProvider().chunkExists(chunkx,chunkz))
			{
				// In a typical inner loop, 80% of the time consecutive calls to
				// this will be within the same chunk
				if (BlockVec3.chunkCacheX == chunkx && BlockVec3.chunkCacheZ == chunkz && BlockVec3.chunkCached.isChunkLoaded)
				{
					return BlockVec3.chunkCached.getBlock(this.x & 15, this.y, this.z & 15);
				}
				else
				{
					Chunk chunk = null;
					chunk = world.getChunkFromChunkCoords(chunkx, chunkz);
					BlockVec3.chunkCached = chunk;
					BlockVec3.chunkCacheX = chunkx;
					BlockVec3.chunkCacheZ = chunkz;
					return chunk.getBlock(this.x & 15, this.y, this.z & 15);
				}
			}
			//Chunk doesn't exist - meaning, it is not loaded
			return Blocks.bedrock;
		}
		catch (Throwable throwable)
		{
			CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Oxygen Sealer thread: Exception getting block type in world");
			CrashReportCategory crashreportcategory = crashreport.makeCategory("Requested block coordinates");
			crashreportcategory.addCrashSection("Location", CrashReportCategory.getLocationInfo(this.x, this.y, this.z));
			throw new ReportedException(crashreport);
		}
	}

	public BlockVec3 add(BlockVec3 par1)
	{
		this.x += par1.x;
		this.y += par1.y;
		this.z += par1.z;
		return this;
	}

	public BlockVec3 translate(BlockVec3 par1)
	{
		this.x += par1.x;
		this.y += par1.y;
		this.z += par1.z;
		return this;
	}

	public BlockVec3 translate(int par1x, int par1y, int par1z)
	{
		this.x += par1x;
		this.y += par1y;
		this.z += par1z;
		return this;
	}

	public static BlockVec3 add(BlockVec3 par1, BlockVec3 a)
	{
		return new BlockVec3(par1.x + a.x, par1.y + a.y, par1.z + a.z);
	}

	public BlockVec3 subtract(BlockVec3 par1)
	{
		this.x = this.x -= par1.x;
		this.y = this.y -= par1.y;
		this.z = this.z -= par1.z;

		return this;
	}

	public BlockVec3 modifyPositionFromSide(ForgeDirection side, int amount)
	{
		switch (side.ordinal())
		{
		case 0:
			this.y -= amount;
			break;
		case 1:
			this.y += amount;
			break;
		case 2:
			this.z -= amount;
			break;
		case 3:
			this.z += amount;
			break;
		case 4:
			this.x -= amount;
			break;
		case 5:
			this.x += amount;
			break;
		}
		return this;
	}

	public BlockVec3 newVecSide(int side)
	{
		BlockVec3 vec = new BlockVec3(this.x, this.y, this.z);
		vec.sideDone[side ^ 1]=true;
		switch (side)
		{
		case 0:
			vec.y--;
			break;
		case 1:
			vec.y++;
			break;
		case 2:
			vec.z--;
			break;
		case 3:
			vec.z++;
			break;
		case 4:
			vec.x--;
			break;
		case 5:
			vec.x++;
			break;
		}
		return vec;
	}

	public BlockVec3 modifyPositionFromSide(ForgeDirection side)
	{
		return this.modifyPositionFromSide(side, 1);
	}

	@Override
	public int hashCode()
	{
		// Upgraded hashCode calculation from the one in VecDirPair to something
		// a bit stronger and faster
		return ((this.y * 379 + this.x) * 373 + this.z) * 7;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof BlockVec3)
		{
			BlockVec3 vector = (BlockVec3) o;
			return this.x == vector.x && this.y == vector.y && this.z == vector.z;
		}

		return false;
	}

	@Override
	public String toString()
	{
		return "BlockVec3 [" + this.x + "," + this.y + "," + this.z + "]";
	}

	public TileEntity getTileEntity(IBlockAccess world)
	{
		return world.getTileEntity(this.x, this.y, this.z);
	}

	public int getBlockMetadata(IBlockAccess world)
	{
		return world.getBlockMetadata(this.x, this.y, this.z);
	}

	public static BlockVec3 readFromNBT(NBTTagCompound nbtCompound)
	{
		BlockVec3 tempVector = new BlockVec3();
		tempVector.x = (int) Math.floor(nbtCompound.getInteger("x"));
		tempVector.y = (int) Math.floor(nbtCompound.getInteger("y"));
		tempVector.z = (int) Math.floor(nbtCompound.getInteger("z"));
		return tempVector;
	}

	public int distanceTo(BlockVec3 vector)
	{
		int var2 = vector.x - this.x;
		int var4 = vector.y - this.y;
		int var6 = vector.z - this.z;
		return (int) Math.floor(Math.sqrt(var2 * var2 + var4 * var4 + var6 * var6));
	}

	public NBTTagCompound writeToNBT(NBTTagCompound par1NBTTagCompound)
	{
		par1NBTTagCompound.setInteger("x", this.x);
		par1NBTTagCompound.setInteger("y", this.y);
		par1NBTTagCompound.setInteger("z", this.z);
		return par1NBTTagCompound;
	}

	public double getMagnitude()
	{
		return Math.sqrt(this.getMagnitudeSquared());
	}

	public int getMagnitudeSquared()
	{
		return this.x * this.x + this.y * this.y + this.z * this.z;
	}

	public void setBlock(World worldObj, Block block)
	{
		worldObj.setBlock(this.x, this.y, this.z, block, 0, 3);
	}

	public boolean blockExists(World world)
	{
		return world.blockExists(this.x, this.y, this.z);
	}
	
	public int intX()
	{
		return this.x;
	}

	public int intY()
	{
		return this.x;
	}

	public int intZ()
	{
		return this.x;
	}
	
	public void setSideDone(int side)
	{
		sideDone[side]=true;
	}
}
