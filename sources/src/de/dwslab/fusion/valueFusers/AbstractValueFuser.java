package de.dwslab.fusion.valueFusers;

import java.util.List;

import de.dwslab.rmdi.fusion.fusers.AbstractFuser;
import de.dwslab.rmdi.fusion.fusers.AbstractFuser.FusionApproach;

public abstract class AbstractValueFuser {

	public String fuse(List<String> values) {
		return values.get(0);
	};

	protected FusionApproach selectedApproach;

	public void setSelectedApproach(FusionApproach selectedApproach) {
		this.selectedApproach = selectedApproach;
	}
}
