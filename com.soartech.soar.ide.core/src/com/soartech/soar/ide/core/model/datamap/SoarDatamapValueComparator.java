package com.soartech.soar.ide.core.model.datamap;

import java.util.Comparator;

public class SoarDatamapValueComparator implements
		Comparator<ISoarDatamapValue> {

	@Override
	public int compare(ISoarDatamapValue o1, ISoarDatamapValue o2) {
		return o1.toString().compareTo(o2.toString());
	}

}
