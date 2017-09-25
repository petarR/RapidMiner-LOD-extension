package lod.sparql;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.jena.riot.RDFDataMgr;

import com.hp.hpl.jena.rdf.model.Model;

public class Test {

	public static void main(String[] args) {
		// final String url =
		// "https://www.googleapis.com/freebase/v1/rdf/m/038q89";
		// File file = new File("C:\\Users\\petar\\Desktop\\038q89.ttl");
		// final Model model = ModelFactory.createDefaultModel();//
		// RDFDataMgr.loadModel(file.toURI().toString());//
		// //
		// model.read(url, "TURTLE");
		// // model.read(url);
		// model.write(System.out);

		// readModelSync("C:\\Users\\petar\\workspace\\VectorRepresentationRM\\OutlierData\\auto-types.nt");
		readModelSync(args[0]);
	}

	public static Model readModelSync(String mFileUri) {
		FileInputStream FIS;
		try {
			FIS = new FileInputStream(mFileUri);
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("File " + mFileUri
					+ " does not exist.");
		}
		File file = new File(mFileUri);
		return RDFDataMgr.loadModel(file.toURI().toString());
	}

}
