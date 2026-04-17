package ashjack.simukraftreloaded.blocks.gases;

public class BlockRadiationGas extends BlockGas
{
	public BlockRadiationGas() 
	{
		super();
		setRiseRate(3);
		setTickRandomly(true);
	    disableStats();
	    setHardness(0.0F);
	    //setCreativeTab(CreativeTabs.tabMisc);
	}
}
