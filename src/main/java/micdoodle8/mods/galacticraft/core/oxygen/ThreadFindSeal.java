package micdoodle8.mods.galacticraft.core.oxygen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import micdoodle8.mods.galacticraft.api.block.IPartialSealableBlock;
import micdoodle8.mods.galacticraft.api.vector.BlockVec3;
import micdoodle8.mods.galacticraft.core.blocks.GCBlocks;
import micdoodle8.mods.galacticraft.core.blocks.BlockUnlitTorch;
import micdoodle8.mods.galacticraft.core.tick.TickHandlerServer;
import micdoodle8.mods.galacticraft.core.tile.TileEntityOxygenSealer;
import micdoodle8.mods.galacticraft.core.util.ConfigManagerCore;
import micdoodle8.mods.galacticraft.core.wrappers.ScheduledBlockChange;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEnchantmentTable;
import net.minecraft.block.BlockFarmland;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockGlass;
import net.minecraft.block.BlockGravel;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockLeavesBase;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockSponge;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.common.FMLLog;

/**
 * ThreadFindSeal.java
 * 
 * This file is part of the Galacticraft project
 * 
 * @author micdoodle8
 * @license Lesser GNU Public License v3 (http://www.gnu.org/licenses/lgpl.html)
 * 
 */
public class ThreadFindSeal //extends Thread
{
	public World world;
	public BlockVec3 head;
	public AtomicBoolean sealedFinal = new AtomicBoolean();
	private boolean sealed;
	public List<TileEntityOxygenSealer> sealers;
	public HashSet<BlockVec3> checked;
	public int checkCount;
	//public static AtomicBoolean anylooping = new AtomicBoolean();
	public AtomicBoolean looping = new AtomicBoolean();
	private HashMap<BlockVec3, TileEntityOxygenSealer> sealersAround = new HashMap<BlockVec3, TileEntityOxygenSealer>();
	static private Block breatheableAirID;
	static private Block oxygenSealerID;
	private List<BlockVec3> currentLayer = new LinkedList<BlockVec3>();
	private List<BlockVec3> nextLayer;
	private List<BlockVec3> airToReplace = new LinkedList<BlockVec3>();
	private List<BlockVec3> breatheableToReplace = new LinkedList<BlockVec3>();
	private List<TileEntityOxygenSealer> otherSealers = new LinkedList<TileEntityOxygenSealer>();
	private List<BlockVec3> torchesToUpdate = new LinkedList<BlockVec3>();

	static{
		ThreadFindSeal.breatheableAirID = GCBlocks.breatheableAir;
		ThreadFindSeal.oxygenSealerID = GCBlocks.oxygenSealer;
	}
	
	public ThreadFindSeal(TileEntityOxygenSealer sealer)
	{
		this(sealer.getWorldObj(), new BlockVec3(sealer).translate(0, 1, 0), sealer.getFindSealChecks(), new ArrayList<TileEntityOxygenSealer>(Arrays.asList(sealer)));
	}

	@SuppressWarnings("unchecked")
	public ThreadFindSeal(World world, BlockVec3 head, int checkCount, List<TileEntityOxygenSealer> sealers)
	{
		//super("GC Sealer Roomfinder Thread");
		//ThreadFindSeal.anylooping.set(true);
		this.world = world;
		this.head = head;
		this.checkCount = checkCount;
		this.sealers = sealers;
		//this.oxygenReliantBlocks = new ArrayList<BlockVec3>();
		this.checked = new HashSet<BlockVec3>();

		//If called by a sealer test the head block and include it in partiallySealableChecked if required
		if (!sealers.isEmpty() && checkCount>0)
		{	
			Block id = head.getBlockID(this.world);
			if (id!=Blocks.air && id!=ThreadFindSeal.breatheableAirID)
			{
				canBlockPassAirCheck(id, this.head, 1);
				//reset the checkCount as canBlockPassAirCheck might have changed it
				this.checkCount = checkCount;
			}
		}

		/*
		if (this.isAlive())
		{
			this.interrupt();
		}*/

		for (TileEntity tile : new ArrayList<TileEntity>(world.loadedTileEntityList))
		{
			if (tile instanceof TileEntityOxygenSealer && tile.getDistanceFrom(head.x, head.y, head.z) < 2048 * 2048)
			{
				this.sealersAround.put(new BlockVec3(tile.xCoord, tile.yCoord, tile.zCoord), (TileEntityOxygenSealer) tile);
			}
		}

		this.looping.set(true);
		/*
		this.start();
	}

	@Override
	public void run()
	{	*/
		long time1 = System.nanoTime();

		this.sealed = true;
		this.checked.add(this.head);
		this.currentLayer.clear();
		this.nextLayer = new LinkedList<BlockVec3>();
		this.airToReplace.clear();
		this.torchesToUpdate.clear();
		if (this.checkCount > 0)
		{
			this.currentLayer.add(this.head.clone());
			if (this.head.x < -29990000 || this.head.z < -29990000 || this.head.x >= 29990000 || this.head.z >= 29990000)
			{
				if (this.sealers.size()>0 && head.getBlockID(world)==Blocks.air) this.airToReplace.add(this.head);
				this.doLayerNearMapEdge();
			}
			else
			{
				if (this.sealers.size()>0 && head.getBlockIDsafe(world)==Blocks.air) this.airToReplace.add(this.head);
				this.doLayer();
			}
		}
		else
		{
			this.sealed = false;
		}

		long time2 = System.nanoTime();

		//Can only be properly sealed if there is at least one sealer here (on edge check)
		if (this.sealers.isEmpty()) this.sealed=false;
		
		if (this.sealed)
		{
			if (!this.airToReplace.isEmpty())
			{
				List<ScheduledBlockChange> changeList = new LinkedList<ScheduledBlockChange>();
				// Note: it is faster to use a LinkedList than an ArrayList
				// when adding a lot of small elements to a list
				for (BlockVec3 checkedVec : this.airToReplace)
				{
					//No block update for performance reasons; deal with unlit torches separately
					changeList.add(new ScheduledBlockChange(checkedVec.clone(), ThreadFindSeal.breatheableAirID, 0, 2));
				}
				TickHandlerServer.scheduleNewBlockChange(this.world.provider.dimensionId, changeList);

				if (!this.torchesToUpdate.isEmpty())
				{
					List<BlockVec3> torchUpdates = new LinkedList<BlockVec3>();
					torchUpdates.addAll(this.torchesToUpdate);
					TickHandlerServer.scheduleNewTorchUpdate(this.world.provider.dimensionId, torchUpdates);
				}
			}
		}
		else
		{
			this.checked.clear();
			this.breatheableToReplace.clear();
			this.otherSealers.clear();
			// loopThroughD will mark breatheableAir blocks for change as it
			// finds them, also searches for unchecked sealers
			this.currentLayer.clear();
			this.currentLayer.add(this.head.clone());
			this.nextLayer = new LinkedList<BlockVec3>();
			this.torchesToUpdate.clear();
			this.loopThroughD();

			if (!this.otherSealers.isEmpty())
			{
				// OtherSealers will have members if the space to be made
				// unbreathable actually still has an unchecked sealer in it
				List<TileEntityOxygenSealer> sealersDone = this.sealers;
				for (TileEntityOxygenSealer otherSealer : this.otherSealers)
				{
					// If it hasn't already been counted, need to check the
					// other sealer immediately in case it can keep the space
					// sealed
					if (!sealersDone.contains(otherSealer) && otherSealer.getFindSealChecks() > 0)
					{
						BlockVec3 newhead = new BlockVec3(otherSealer).translate(0, 1, 0);
						this.sealed = true;
						this.checkCount = otherSealer.getFindSealChecks();
						this.sealers = new LinkedList<TileEntityOxygenSealer>();
						this.sealers.add(otherSealer);
						this.checked.clear();
						this.checked.add(newhead);
						this.currentLayer.clear();
						this.nextLayer = new LinkedList<BlockVec3>();
						this.airToReplace.clear();
						this.currentLayer.add(newhead.clone());
						if (newhead.x < -29990000 || newhead.z < -29990000 || newhead.x >= 29990000 || newhead.z >= 29990000)
						{
							this.doLayerNearMapEdge();
						}
						else
						{
							this.doLayer();
						}

						// If found a sealer which can still seal the space, it
						// should take over as head
						if (this.sealed)
						{
							if (ConfigManagerCore.enableDebug)
							{
								FMLLog.info("Oxygen Sealer replacing head at x" + this.head.x + " y" + (this.head.y - 1) + " z" + this.head.z);
							}
							if (!sealersDone.isEmpty())
							{
								TileEntityOxygenSealer oldHead = sealersDone.get(0);
								if (!this.sealers.contains(oldHead))
								{
									this.sealers.add(oldHead);
								}
							}
							this.head = newhead.clone();
							otherSealer.threadSeal = this;
							otherSealer.stopSealThreadCooldown = 75+TileEntityOxygenSealer.countEntities;
							break;
						}
					}
					// Restore sealers to what it was, if this search did not
					// result in a seal
					this.sealers = sealersDone;
				}
			}

			if (this.sealed == false)
			{
				if (head.getBlockID(world)==ThreadFindSeal.breatheableAirID) this.breatheableToReplace.add(head);
				if(!this.breatheableToReplace.isEmpty())
				{
					List<ScheduledBlockChange> changeList = new LinkedList<ScheduledBlockChange>();
					// Note: a LinkedList is faster than an ArrayList when adding a
					// lot of small elements to a list
					for (BlockVec3 checkedVec : this.breatheableToReplace)
					{
						//No block update, otherwise it could be starting onNeighborBlockChange threads on adjacent breatheableAir which is still to clear
						changeList.add(new ScheduledBlockChange(checkedVec.clone(), Blocks.air, 0, 2));
					}
					TickHandlerServer.scheduleNewBlockChange(this.world.provider.dimensionId, changeList);

					if (!this.torchesToUpdate.isEmpty())
					{
						List<BlockVec3> torchUpdates = new LinkedList<BlockVec3>();
						torchUpdates.addAll(this.torchesToUpdate);
						TickHandlerServer.scheduleNewTorchUpdate(this.world.provider.dimensionId, torchUpdates);
					}
				}
			}
		}

		// Set any sealers found which are not the head sealer, not to run their
		// own seal checks for a while
		// (The player can control which is the head sealer in a space by
		// enabling just that one and disabling all the others)
		TileEntityOxygenSealer headSealer = this.sealersAround.get(this.head.translate(0, -1, 0));

		// If it is sealed, cooldown can be extended as frequent checks are not needed
		if (headSealer != null) headSealer.stopSealThreadCooldown += 75;
		
		for (TileEntityOxygenSealer sealer : this.sealers)
		{
			// Sealers which are not the head sealer: put them on cooldown so
			// the inactive ones don't start their own threads and so unseal
			// this volume
			// and update threadSeal reference of all sealers found (even the
			// inactive ones)
			if (sealer != headSealer && headSealer != null)
			{
				sealer.threadSeal = this;
				sealer.stopSealThreadCooldown = headSealer.stopSealThreadCooldown + 51;
			}
		}

		// Help the Java garbage collector by clearing big data structures which
		// are no longer needed
		this.airToReplace.clear();
		this.breatheableToReplace.clear();

		this.looping.set(false);

		if (ConfigManagerCore.enableDebug)
		{
			long time3 = System.nanoTime();
			FMLLog.info("Oxygen Sealer Check Completed at x" + this.head.x + " y" + this.head.y + " z" + this.head.z);
			FMLLog.info("   Sealed: " + this.sealed);
			FMLLog.info("   Loop Time taken: " + (time2 - time1) / 1000000.0D + "ms");
			FMLLog.info("   Place Time taken: " + (time3 - time2) / 1000000.0D + "ms");
			FMLLog.info("   Total Time taken: " + (time3 - time1) / 1000000.0D + "ms");
			FMLLog.info("   Found: " + this.sealers.size() + " sealers");
			FMLLog.info("   Looped through: " + this.checked.size() + " blocks");
		}
		this.checked.clear();
		this.sealedFinal.set(this.sealed);
		//ThreadFindSeal.anylooping.set(false);
	}

	private void loopThroughD()
	{
		while (this.currentLayer.size() > 0)
		{
			for (BlockVec3 vec : this.currentLayer)
			{
				for (int side = 0; side < 6; side++)
				{
					if (vec.sideDone[side]) continue;
					BlockVec3 sideVec = vec.newVecSide(side);

					if (!this.checked.contains(sideVec))
					{
						this.checked.add(sideVec);
						Block id = sideVec.getBlockID(this.world);
						if (id == ThreadFindSeal.breatheableAirID)
						{
							this.breatheableToReplace.add(sideVec);
							// Only follow paths with adjacent breatheableAir
							// blocks - this now can't jump through walls etc
							this.nextLayer.add(sideVec);
						}
						else if (id == ThreadFindSeal.oxygenSealerID)
						{
							TileEntityOxygenSealer sealer = this.sealersAround.get(sideVec);
							if (sealer != null)
							{
								if (!this.sealers.contains(sealer))
								{
									this.otherSealers.add(sealer);
								}
							}
						}
						else if (id!=null && id!=Blocks.air && this.canBlockPassAirCheck(id, sideVec, side))
						{
							//Look outbound through partially sealable blocks in case there is breatheableAir to clear beyond
							this.nextLayer.add(sideVec);
						}
					}
				}
			}
			
			// Set up the next layer as current layer for the while loop
			this.currentLayer = this.nextLayer;
			this.nextLayer = new LinkedList<BlockVec3>();
		}
	}

	private void doLayer()
	{
		LAYERLOOP:
		while (this.sealed && this.currentLayer.size() > 0)
		{
			for (BlockVec3 vec : this.currentLayer)
			{
					for (int side = 0; side < 6; side++)
					{
						//Skip the side which this was entered from
						//This is also used to skip looking on the solid sides of partially sealable blocks
						if (vec.sideDone[side]) continue;
						// The sides 0 to 5 correspond with the ForgeDirections
						// but saves a bit of time not to call ForgeDirection
						BlockVec3 sideVec = vec.newVecSide(side);

						if (!this.checked.contains(sideVec))
						{
							if (this.checkCount > 0)
							{
								this.checkCount--;
								this.checked.add(sideVec);

								Block id = sideVec.getBlockIDsafe(this.world);
								if (id == ThreadFindSeal.breatheableAirID)
								// The most likely case
								{
									this.nextLayer.add(sideVec);
								}	
								else if (id == Blocks.air)
								{
									this.nextLayer.add(sideVec);
									this.airToReplace.add(sideVec);								
								}
								else if (id == null)
								{
									// Broken through to the void or the
									// stratosphere (above y==255) - set
									// unsealed and abort
									this.checkCount = 0;
									this.sealed = false;
									break LAYERLOOP;
								}
								else if (this.canBlockPassAirCheck(id, sideVec, side))
								{
									this.nextLayer.add(sideVec);
								}
								else if (id == ThreadFindSeal.oxygenSealerID)
								{
									TileEntityOxygenSealer sealer = this.sealersAround.get(sideVec);

									if (sealer != null)
									{
										if (!this.sealers.contains(sealer))
										{
											this.sealers.add(sealer);
											this.checkCount += sealer.getFindSealChecks();
										}
									}
								}
								//If the chunk was unloaded, BlockVec3.getBlockID returns Blocks.bedrock
								//which is a solid block, so the loop will treat that as a sealed edge
								//and not iterate any further in that direction
							}
							else if (this.sealed)
							{
								Block id = sideVec.getBlockIDsafe(this.world);
								// id == null means the void or height y>255, both
								// of which are unsealed obviously
								if (id == null || id == Blocks.air || id == this.breatheableAirID || id == null || this.canBlockPassAirCheck(id, sideVec, side))
								{
									this.sealed = false;
									break LAYERLOOP;
								}
							}
						}
					}
				}

			// Is there a further layer of air/permeable blocks to test?
			this.currentLayer = this.nextLayer;
			this.nextLayer = new LinkedList<BlockVec3>();
		}
	}

	private void doLayerNearMapEdge()
	{
		LAYERLOOPNME:
		while (this.sealed && this.currentLayer.size() > 0)
		{
			for (BlockVec3 vec : this.currentLayer)
			{
				for (int side = 0; side < 6; side++)
				{
					// The sides 0 to 5 correspond with the ForgeDirections but
					// saves a bit of time not to call ForgeDirection
					if (vec.sideDone[side]) continue;
					BlockVec3 sideVec = vec.newVecSide(side);

					if (!this.checked.contains(sideVec))
					{
						if (this.checkCount > 0)
						{
							this.checkCount--;
							this.checked.add(sideVec);

							// This is a slower operation as it involves
							// map edge checks
							Block id = sideVec.getBlockID(this.world); 
							
							if (id == ThreadFindSeal.breatheableAirID)
							// The most likely case
							{
								this.nextLayer.add(sideVec);
							}	
							else if (id == Blocks.air)
							{
								this.nextLayer.add(sideVec);
								this.airToReplace.add(sideVec);								
							}
							else if (id == null)
							{
								// Broken through to the void or the
								// stratosphere (above y==255) - set
								// unsealed and abort
								this.checkCount = 0;
								this.sealed = false;
								break LAYERLOOPNME;
							}
							else if (this.canBlockPassAirCheck(id, sideVec, side))
							{
								this.nextLayer.add(sideVec);
							}
							else if (id == ThreadFindSeal.oxygenSealerID)
							{
								TileEntityOxygenSealer sealer = this.sealersAround.get(sideVec);

								if (sealer != null)
								{
									if (!this.sealers.contains(sealer))
									{
										this.sealers.add(sealer);
										this.checkCount += sealer.getFindSealChecks();
									}
								}
							}
							//If the chunk was unloaded, BlockVec3.getBlockID returns Blocks.bedrock
							//which is a solid block, so the loop will treat that as a sealed edge
							//and not iterate any further in that direction
						}
						else if (this.sealed)
						{
							Block id = sideVec.getBlockID(this.world);
							// id == null means the void or height y>255, both of
							// which are unsealed obviously
							if (id == null || id == Blocks.air || id == this.breatheableAirID || id == null || this.canBlockPassAirCheck(id, sideVec, side))
							{
								this.sealed = false;
								break LAYERLOOPNME;
							}
						}
					}
				}
			}
			// Is there a further layer of air/permeable blocks to test?
			this.currentLayer = this.nextLayer;
			this.nextLayer = new LinkedList<BlockVec3>();
		}
	}

	private boolean canBlockPassAirCheck(Block block, BlockVec3 vec, int side)
	{
		//Check leaves first, because their isOpaqueCube() test depends on graphics settings
		//(See net.minecraft.block.BlockLeaves.isOpaqueCube()!)
		if (block instanceof BlockLeavesBase)
		{
			return true;
		}
		
		if (block.isOpaqueCube()) 
		{
			//Gravel, wool and sponge are porous
			if (block instanceof BlockGravel || block.getMaterial() == Material.cloth || block instanceof BlockSponge)
			{
				return true;
			}
			
			return false;
		}
		
		if (block instanceof BlockGlass)
		{
			return false;
		}

		//Solid but non-opaque blocks, for example special glass
		if (OxygenPressureProtocol.nonPermeableBlocks.containsKey(block) && OxygenPressureProtocol.nonPermeableBlocks.get(block).contains(vec.getBlockMetadata(this.world)))
		{
			return false;
		}

		if (block instanceof IPartialSealableBlock)
		{
			IPartialSealableBlock blockPartial = (IPartialSealableBlock) block; 
			if (blockPartial.isSealed(this.world, vec.x, vec.y, vec.z, ForgeDirection.getOrientation(side)))
			{
				// If a partial block checks as solid, allow it to be tested
				// again from other directions
				// This won't cause an endless loop, because the block won't
				// be included in nextLayer if it checks as solid
				this.checked.remove(vec);
				this.checkCount--;
				return false;
			}

			//Find the solid sides so they don't get iterated into, when doing the next layer
			for (int i=0;i<6;i++)
			{
				if (i==side) continue;
				if (blockPartial.isSealed(this.world, vec.x, vec.y, vec.z, ForgeDirection.getOrientation(i)))
				{
					vec.setSideDone(i ^ 1);
				}
			}
			return true;
		}

		if (block instanceof BlockUnlitTorch)
		{
			torchesToUpdate.add(vec);
			return true;
		}

		//Half slab seals on the top side or the bottom side according to its metadata
		if (block instanceof BlockSlab)
        {
            boolean isTopSlab = (vec.getBlockMetadata(this.world) & 8) == 8;
			//Looking down onto a top slab or looking up onto a bottom slab
			if ((side == 0 && isTopSlab) || (side == 1 && !isTopSlab))
            {
            	//Sealed from that solid side but allow other sides still to be checked
    			this.checked.remove(vec);
    			this.checkCount--;
    			return false;		
            }
            //Not sealed
			vec.setSideDone(isTopSlab ? 1:0);
			return true;            	
        }
        
		//Farmland etc only seals on the solid underside
		if (block instanceof BlockFarmland || block instanceof BlockEnchantmentTable || block instanceof BlockLiquid)
        {
            if (side==1)
            {
    			//Sealed from the underside but allow other sides still to be checked
            	this.checked.remove(vec);
    			this.checkCount--;
    			return false;		
            }
            //Not sealed
			vec.setSideDone(0);
			return true;            		            	            
        }

		if (block instanceof BlockPistonBase)
		{
			BlockPistonBase piston = (BlockPistonBase)block;
			int meta = vec.getBlockMetadata(this.world);
			if (piston.isExtended(meta))
			{
				int facing = piston.getPistonOrientation(meta);
				if (side==facing)
				{
					this.checked.remove(vec);
					this.checkCount--;
					return false;		
				}
				vec.setSideDone(facing ^ 1);
				return true;
			}
			return false;
		}
		
		//General case - this should cover any block which correctly implements isBlockSolidOnSide
		//including most modded blocks - Forge microblocks in particular is covered by this.
		// ### Any exceptions in mods should implement the IPartialSealableBlock interface ###
		if (block.isSideSolid(this.world, vec.x, vec.y, vec.z, ForgeDirection.getOrientation(side ^ 1)))
		{
			//Solid on all sides
			if (block.isBlockNormalCube()) return false;
			//Sealed from this side but allow other sides still to be checked
			this.checked.remove(vec);
			this.checkCount--;
			return false;		
		}
		
		//Easy case: airblock, return without checking other sides
		if (block.getMaterial() == Material.air) return true;
		
		//Not solid on that side.
		//Look to see if there is any other side which is solid in which case a check will not be needed next time
		for (int i=0;i<6;i++)
		{
			if (i==(side ^ 1)) continue;
			if (block.isSideSolid(this.world, vec.x, vec.y, vec.z, ForgeDirection.getOrientation(i)))
			{
				vec.setSideDone(i);
			}
		}
			
		//Not solid from this side, so this is not sealed
		return true;
	}
}
