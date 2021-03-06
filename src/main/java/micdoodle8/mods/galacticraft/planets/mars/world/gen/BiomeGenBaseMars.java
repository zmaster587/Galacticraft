package micdoodle8.mods.galacticraft.planets.mars.world.gen;

import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.world.biome.BiomeGenBase;

/**
 * GCMarsBiomeGenBase.java
 * 
 * This file is part of the Galacticraft project
 * 
 * @author micdoodle8
 * @license Lesser GNU Public License v3 (http://www.gnu.org/licenses/lgpl.html)
 * 
 */
public class BiomeGenBaseMars extends BiomeGenBase
{
	public static final BiomeGenBase marsFlat = new BiomeGenFlagMars(103).setBiomeName("marsFlat");

	@SuppressWarnings("unchecked")
	public BiomeGenBaseMars(int var1)
	{
		super(var1);
		this.spawnableMonsterList.clear();
		this.spawnableWaterCreatureList.clear();
		this.spawnableCreatureList.clear();
		this.spawnableMonsterList.add(new SpawnListEntry(EntityZombie.class, 10, 4, 4));
		this.spawnableMonsterList.add(new SpawnListEntry(EntitySpider.class, 10, 4, 4));
		this.rainfall = 0F;
	}

	@Override
	public BiomeGenBaseMars setColor(int var1)
	{
		return (BiomeGenBaseMars) super.setColor(var1);
	}

	@Override
	public float getSpawningChance()
	{
		return 0.01F;
	}
}
