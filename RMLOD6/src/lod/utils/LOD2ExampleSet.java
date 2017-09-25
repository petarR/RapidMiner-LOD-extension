package lod.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.MemoryExampleTable;

public class LOD2ExampleSet {

	public static MemoryExampleTable generateCleanMemoryTable(
			ExampleSet exampleSet) {
		List<Attribute> attrs = new ArrayList<Attribute>();
		Iterator<Attribute> iter = exampleSet.getAttributes().allAttributes();

		while (iter.hasNext()) {

			attrs.add(iter.next());
		}

		Attribute[] newAttrs = new Attribute[attrs.size()];
		for (int i = 0; i < attrs.size(); i++) {
			newAttrs[i] = attrs.get(i);
		}
		MemoryExampleTable table = new MemoryExampleTable(newAttrs);

		return table;
	}

	public static ExampleSet addDataToExampleSet(List<DataRow> newExamples,
			ExampleSet odlSet) {
		MemoryExampleTable table = generateCleanMemoryTable(odlSet);

		for (DataRow ex : newExamples) {
			// double values = new double [attrs.length];

			table.addDataRow(ex);
		}
		ExampleSet newSet = table.createExampleSet();

		return setSpecialAttributes(odlSet, newSet);

	}

	public static ExampleSet setSpecialAttributes(ExampleSet exampleSet,
			ExampleSet outSet) {

		outSet.getAttributes().setId(exampleSet.getAttributes().getId());
		outSet.getAttributes().setLabel(exampleSet.getAttributes().getLabel());
		outSet.getAttributes().setCluster(
				exampleSet.getAttributes().getCluster());
		outSet.getAttributes().setCost(exampleSet.getAttributes().getCost());
		outSet.getAttributes().setOutlier(
				exampleSet.getAttributes().getOutlier());
		outSet.getAttributes().setPredictedLabel(
				exampleSet.getAttributes().getPredictedLabel());
		outSet.getAttributes()
				.setWeight(exampleSet.getAttributes().getWeight());
		return outSet;
	}

	public static DoubleArrayDataRow getDataRowFromExampleWithSpecialAttrs(
			Example ex) {

		DoubleArrayDataRow row = new DoubleArrayDataRow(new double[ex
				.getAttributes().allSize()]);
		for (Attribute attr : ex.getAttributes()) {
			row.set(attr, ex.getValue(attr));
		}
		if (ex.getAttributes().getId() != null)
			row.set(ex.getAttributes().getId(), ex.getId());
		if (ex.getAttributes().getLabel() != null)
			row.set(ex.getAttributes().getLabel(), ex.getLabel());

		if (ex.getAttributes().getPredictedLabel() != null)
			row.set(ex.getAttributes().getPredictedLabel(),
					ex.getPredictedLabel());
		if (ex.getAttributes().getWeight() != null)
			row.set(ex.getAttributes().getWeight(), ex.getWeight());
		return row;
	}

	public static DataRow getDataRowFromExample(Example ex) {
		DataRowFactory rowFactory = new DataRowFactory(
				DataRowFactory.TYPE_SPARSE_MAP, '.');

		Iterator<Attribute> iter = ex.getAttributes().allAttributes();
		double values[] = new double[ex.getAttributes().allSize()];
		int i = 0;
		while (iter.hasNext()) {
			Attribute attr = iter.next();
			values[i] = ex.getValue(attr);
			i++;
		}
		DoubleArrayDataRow row = new DoubleArrayDataRow(values);

		return row;
	}
}
