package de.dwslab.rmdi.schemamatching.models;

import java.io.Serializable;

public class MatchedColumnsInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3786871305300016297L;

	ColumnInfo leftColumn;

	ColumnInfo rightColumn;

	double matchingProbability;

	public ColumnInfo getLeftColumn() {
		return leftColumn;
	}

	public void setLeftColumn(ColumnInfo leftColumn) {
		this.leftColumn = leftColumn;
	}

	public ColumnInfo getRightColumn() {
		return rightColumn;
	}

	public void setRightColumn(ColumnInfo rightColumn) {
		this.rightColumn = rightColumn;
	}

	public double getMatchingProbability() {
		return matchingProbability;
	}

	public void setMatchingProbability(double matchingProbability) {
		this.matchingProbability = matchingProbability;
	}

}
