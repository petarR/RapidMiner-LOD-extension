package lod.sparql;

public class HardQueries {

	private static final String OUTGOING_DEPTH1 = "CONSTRUCT {?s ?p ?o} where {values ?s {VALUES}. ?s ?p ?o}";
	// Construct {?s ?p ?o.?o ?x ?y} where { values ?s {
	// <http://data.linkedmdb.org/resource/film/2015>
	// <http://dbpedia.org/resource/Struga>}. ?s ?p ?o . ?o ?x ?y} LIMIT 10000
}
