package micdoodle8.mods.galacticraft.core.tile;

import micdoodle8.mods.galacticraft.api.power.EnergySource;
import micdoodle8.mods.galacticraft.api.power.EnergySource.EnergySourceWireless;
import micdoodle8.mods.galacticraft.api.power.ILaserNode;
import micdoodle8.mods.galacticraft.api.vector.Vector3;

public class TileEntityBeamReflector extends TileEntityBeamOutput implements ILaserNode
{
	public Vector3 color = new Vector3(0, 1, 0);
	private EnergyStorage storage = new EnergyStorage(10, 1);

	@Override
	public void updateEntity()
	{
		super.updateEntity();
	}
	
	@Override
	public Vector3 getInputPoint() 
	{
		float distance = 0.15F;
		Vector3 deviation = new Vector3(Math.sin(Math.toRadians(this.yaw - 180)) * distance, 0, Math.cos(Math.toRadians(this.yaw - 180)) * distance);
		Vector3 headVec = new Vector3(this.xCoord + 0.5, this.yCoord + 1.13228 / 2.0, this.zCoord + 0.5);
		headVec.translate(deviation.clone().invert());
		return headVec;
	}

	@Override
	public Vector3 getOutputPoint(boolean offset) 
	{
		float distance = 0.15F;
		Vector3 deviation = new Vector3(Math.sin(Math.toRadians(this.yaw)) * distance, 0, Math.cos(Math.toRadians(this.yaw)) * distance);
		Vector3 headVec = new Vector3(this.xCoord + 0.5, this.yCoord + 1.13228 / 2.0, this.zCoord + 0.5);
		if (offset)
		{
			headVec.translate(deviation.clone().invert());
		}
		return headVec;
	}

	@Override
	public double getPacketRange() 
	{
		return 24.0D;
	}

	@Override
	public int getPacketCooldown() 
	{
		return 3;
	}

	@Override
	public boolean isNetworkedTile() 
	{
		return true;
	}

	@Override
	public Vector3 getColor() 
	{
		return this.color;
	}

	@Override
	public boolean canConnectTo(ILaserNode laserNode) 
	{
		return this.color.equals(laserNode.getColor());
	}

	@Override
	public int receiveEnergyGC(EnergySource from, int amount, boolean simulate) 
	{
		if (this.target != null)
		{
			if (from instanceof EnergySourceWireless)
			{
				if (((EnergySourceWireless) from).nodes.contains(this.target))
				{
					return 0;
				}
				
				((EnergySourceWireless) from).nodes.add(this);
			}
			
			return this.target.receiveEnergyGC(from, amount, simulate);
		}
		
		return this.storage.receiveEnergyGC(amount, simulate);
	}

	@Override
	public int extractEnergyGC(EnergySource from, int amount, boolean simulate) 
	{
		return 0;
	}

	@Override
	public boolean nodeAvailable(EnergySource from) 
	{
		return from instanceof EnergySourceWireless;
	}

	@Override
	public int getEnergyStoredGC(EnergySource from) 
	{
		return this.storage.getEnergyStoredGC();
	}

	@Override
	public int getMaxEnergyStoredGC(EnergySource from) 
	{
		return this.storage.getCapacityGC();
	}
}
