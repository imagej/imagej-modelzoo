
package net.imagej.modelzoo.consumer.network;

import net.imglib2.type.numeric.RealType;

class TestNetworkSingle2D2D<T extends RealType<T>> extends
		TestNetwork<T>
{

	public TestNetworkSingle2D2D()
	{
		super();
		inputShape = new long[]{-1,-1,-1,1};
		outputShape = new long[]{-1,-1,-1,1};
	}

}
