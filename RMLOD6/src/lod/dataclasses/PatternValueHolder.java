package lod.dataclasses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import lod.linking.SimpleLinker;

/**
 * Repersents a wrapper class used in {@link SimpleLinker}.
 * @author Evgeny Mitichkin
 *
 */
public class PatternValueHolder {
	
	public static final byte IS_CHUNK = 0;
	public static final byte IS_ATTRIBUTE = 1;
	
	private List<String> dataPart;
	private List<Byte> dataIndex;
	
	public PatternValueHolder()
	{
		this.dataPart = new ArrayList<String>();
		this.dataIndex = new ArrayList<Byte>();
	}

	public List<String> getDataPart() {
		return dataPart;
	}

	public void setDataPart(List<String> dataPart) {
		this.dataPart = dataPart;
	}

	public List<Byte> getDataIndex() {
		return dataIndex;
	}

	public void setDataIndex(List<Byte> dataIndex) {
		this.dataIndex = dataIndex;
	}
	
	public void addData(String data, byte dataType)
	{
		dataPart.add(data);
		dataIndex.add(dataType);
	}
	
	public HashMap<Byte, String> getPatternPart(int index)
	{
		HashMap<Byte, String> pair = new HashMap<Byte, String>();
		pair.put(dataIndex.get(index), dataPart.get(index));
		return pair;
	}
	
	public int getPatternLength()
	{
		return dataIndex.size();
	}
	
	public int getPatternAttributeArraySize()
	{
		int size = 0;
		for (int i=0;i<dataIndex.size();i++)
		{
			if (dataIndex.get(i) == PatternValueHolder.IS_ATTRIBUTE)
			{
				size++;
			}
		}
		return size;
	}
	
	public boolean isDataPartExists(String part)
	{
		boolean result = false;
		for (int i=0;i<dataIndex.size();i++)
		{
			if (dataIndex.get(i) == PatternValueHolder.IS_ATTRIBUTE)
			{
				if (dataPart.get(i).equals(part))
				{
					result = true;
					break;
				}
			}
		}
		return result;
	}
}
