
package net.imagej.modelzoo.consumer.network;

import net.imglib2.type.numeric.RealType;

class TestNetworkSingle3D3D<T extends RealType<T>> extends
		TestNetwork<T>
{

	public TestNetworkSingle3D3D()
	{
		super();
		inputShape = new long[]{-1,-1,-1,-1,1};
		outputShape = new long[]{-1,-1,-1,-1,1};
	}

}
