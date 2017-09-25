package de.dwslab.rmdi.schemamatching.matchers.lod.utils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javatools.administrative.D;
import javatools.datatypes.Pair;
import lod.rdf.model.RdfHolder;
import lod.rdf.model.RdfTriple;
import paris.Config;
import paris.EqualityStore;
import paris.EqualityStoreMultiple;
import paris.HashArrayNeighborhood;
import paris.JoinRelation;
import paris.MapperOutput;
import paris.Neighborhood;
import paris.RelationNormalizer;
import paris.Result;
import paris.Setting;
import paris.SubThingStore.SubPair;
import paris.storage.FactStore;
import paris.storage.FactStore.PredicateAndObject;
import bak.pcj.IntIterator;
import bak.pcj.set.IntOpenHashSet;
import bak.pcj.set.IntSet;
import de.dwslab.rmdi.schemamatching.models.ColumnInfo;
import de.dwslab.rmdi.schemamatching.models.MatchedColumnsInfo;

public class ParisHelper {

	public static FactStore loadFactStoreFromRdfHolder(RdfHolder rdfHolder,
			FactStore fs) {
		for (RdfTriple triple : rdfHolder.getTriples()) {
			fs.add(triple.getSubject(), triple.getPredicate(),
					triple.getObject());
		}
		fs.prepare();
		return fs;
	}

	public static void computeClassesOneWay(FactStore fs1, FactStore fs2,
			Result computed, List<MatchedColumnsInfo> matchedColumnsInfo,
			double acceptanceThredhold) {
		int counter = fs2.numClasses();
		// Announce.progressStart("Computing subclasses one direction",
		// counter);
		for (int cls = 0; cls < fs2.numEntities(); cls++) {
			if (!fs2.isClass(cls))
				continue;
			findSuperClassesOf(cls, fs2, fs1, computed, matchedColumnsInfo,
					acceptanceThredhold);

		}
	}

	/** Finds the super classes of a class */
	public static void findSuperClassesOf(Integer subclass, FactStore subStore,
			FactStore superStore, Result computed,
			List<MatchedColumnsInfo> matchedColumnsInfo,
			double acceptanceThredhold) {
		// We ignore classes that contain practically all entities
		if (subStore.entity(subclass).startsWith("owl:")
				|| subStore.entity(subclass).equals(
						"http://www.opengis.net/gml/_Feature"))
			return;

		// maps each superclass d to
		// SUM x such that type(x,c): 1 - PROD y such that type(y,d): 1-P(x=y)
		Map<Integer, Double> superclassDegree = new TreeMap<Integer, Double>();
		// contains the value
		// # x such that type(x,c) // no longer: and exists y: y=x and
		// type(y,some
		// class)
		double normalizer = 0;
		int counter = 0;
		// Don't compute that for classes that are too far up in the hierarchy
		IntSet subInstances = subStore.instancesOf(subclass);
		if (subInstances == null)
			return;
		IntIterator it = subInstances.iterator();
		while (it.hasNext()) {
			int subclassInstance = it.next();
			// For each instance x of c...
			boolean foundeqv = false;
			Map<Integer, Double> membershipProduct = new TreeMap<Integer, Double>();
			// maps each superclass d to
			// PROD y such that type(y,d): 1-P(x=y)
			for (Pair<Object, Double> superclassInstancePair : computed
					.equalToScored(subStore, subclassInstance)) {
				if (!(superclassInstancePair.first() instanceof Integer))
					continue;
				Integer superclassInstance = (Integer) superclassInstancePair
						.first();
				double equality = superclassInstancePair.second();

				if (equality < Config.THETA)
					continue;
				IntSet classes = superStore.classesOf(superclassInstance);
				IntIterator it2 = classes.iterator();
				while (it2.hasNext()) {
					int superClass = it2.next();
					assert (superClass > 0);
					double prod = membershipProduct.containsKey(superClass) ? membershipProduct
							.get(superClass) : 1;

					prod *= 1 - equality;
					membershipProduct.put(superClass, prod);
					foundeqv = true;
				}
			}
			if (foundeqv) {
				for (Integer superclass : membershipProduct.keySet()) {
					D.addKeyValueDbl(superclassDegree, superclass,
							1 - membershipProduct.get(superclass));
				}
			}
			normalizer++;
		}
		// We do not do the domain/range deduction
		// Collect all classes about which we know something in superclassDegree
		// Say that if we have no instances, the superclassDegree is 0
		// for (Integer superclass : domainSuperclassDegree.keySet()) {
		// if (!superclassDegree.containsKey(superclass))
		// superclassDegree.put(superclass, 0.0);
		// }

		// If the normalizer is 0, superclassDegree(x)=0 for all x.
		// So instanceScore will be 0 anyway. Hence, set the normalizer to 1
		// to avoid NAN values when we compute superclassDegree(x)/normalizer.
		if (normalizer == 0)
			normalizer = 1;

		// Set the final values
		for (Integer superclass : superclassDegree.keySet()) {
			double instanceScore = superclassDegree.get(superclass)
					/ normalizer;
			double domainScore = 1.0; // domainSuperclassDegree.containsKey(superclass)
										// ?
										// domainSuperclassDegree.get(superclass)
										// :
										// 1.0;
			if (1 - (1 - instanceScore) * domainScore < Config.THETA)
				continue;

			// computed.setSubclass(subStore, subclass, superclass, 1
			// - (1 - instanceScore) * domainScore);
			double val = 1 - (1 - instanceScore) * domainScore;
			if (val < Config.THETA)
				continue;
			// computed.superClassesForFactStore(subStore).setValue(subclass,
			// superclass, 1 - (1 - instanceScore) * domainScore);

			if (val < acceptanceThredhold)
				continue;

			ColumnInfo leftColumnInfo = new ColumnInfo();
			leftColumnInfo.setUri(subStore.entity(subclass));
			ColumnInfo rightColumnInfo = new ColumnInfo();
			rightColumnInfo.setUri(superStore.entity(subclass));

			MatchedColumnsInfo info = new MatchedColumnsInfo();
			info.setMatchingProbability(val);
			info.setLeftColumn(leftColumnInfo);
			info.setRightColumn(rightColumnInfo);
			matchedColumnsInfo.add(info);

		}
	}

	/**
	 * Runs one whole iteration
	 * 
	 * @param equalitiesFromTable
	 * @param acceptanceThredhold
	 * 
	 * @throws InterruptedException
	 */
	public static void oneIteration(int run, FactStore factStore1,
			FactStore factStore2, Result computed, Setting setting,
			List<MatchedColumnsInfo> matchedColumnsInfo,
			Map<String, String> equalitiesFromTable, double acceptanceThredhold)
			throws IOException, InterruptedException {
		EqualityStore equalities1 = new EqualityStore(factStore1, factStore2);
		// EqualityStore equalities2 = new EqualityStore(factStore2,
		// factStore1);
		EqualityStoreMultiple equalitiesMultiple = null;
		if (setting.cleverMatching) {

			equalitiesMultiple = new EqualityStoreMultiple(factStore1,
					factStore2);
		}

		MapperOutput mapperOutput1 = null;
		MapperOutput mapperOutput2 = null;

		/** We do the computation on the ontologies */
		mapperOutput1 = oneIterationOneWay(run, factStore1, equalities1,
				equalitiesMultiple, setting, computed);
		if (setting.cleverMatching)
			equalities1 = equalitiesMultiple.takeMaxMaxClever();
		// equalities1.dump(new File(setting.tsvFolder, run + "_eqv_full.tsv"));
		equalities1.takeMaxMaxBothWays();
		// equalities1.dump(new File(setting.tsvFolder, run + "_eqv.tsv"));
		if (setting.bothWays) {
			mapperOutput2 = oneIterationOneWay(run, factStore2, null, null,
					setting, computed);
		}

		computed.mapperOutput1 = mapperOutput1;
		computed.mapperOutput2 = mapperOutput2;

		computed.equalityStore = equalities1;
		// Announce.message("done equalities at", NumberFormatter.ISOtime());

		/**
		 * Now, we aggregate the relation alignments to use them in the next
		 * iteration
		 */
		// Announce.message("loading neighborhoods in one direction");
		computed.superRelationsOf1.loadMapperOutput(mapperOutput1);
		// Announce.message("loading neighborhoods in other direction");
		if (setting.bothWays) {
			computed.superRelationsOf2.loadMapperOutput(mapperOutput2);
		}

		/** Write the alignments */
		// if (setting.bothWays) {
		// // equalities2.dump(new File(setting.tsvFolder, run + "_eqv2.tsv"));
		// computed.superRelationsOf2.dump(new File(setting.tsvFolder, run
		// + "_superrelations2.tsv"));
		// }
		// computed.superRelationsOf1.dump(new File(setting.tsvFolder, run
		// + "_superrelations1.tsv"));

		for (SubPair<JoinRelation> pair : computed.superRelationsOf1.all()) {
			if (pair.val > acceptanceThredhold) {
				// details.add(computed.superRelationsOf1.toTsv(pair));
				// create the columnInfos, and populate them later
				ColumnInfo leftColumnInfo = new ColumnInfo();
				String uriTmp = pair.sub.toString();
				if (uriTmp.endsWith("-"))
					uriTmp = uriTmp.substring(0, uriTmp.length() - 1);
				leftColumnInfo.setUri(uriTmp);

				ColumnInfo rightColumnInfo = new ColumnInfo();
				uriTmp = pair.supr.toString();
				if (uriTmp.endsWith("-"))
					uriTmp = uriTmp.substring(0, uriTmp.length() - 1);
				rightColumnInfo.setUri(uriTmp);

				MatchedColumnsInfo info = new MatchedColumnsInfo();
				info.setMatchingProbability(pair.val);
				info.setLeftColumn(leftColumnInfo);
				info.setRightColumn(rightColumnInfo);
				matchedColumnsInfo.add(info);
			}
		}

		if (setting.printNeighborhoodsSampling)
			computed.printNeighborhoods();
		// Announce.progressDone();
		// Announce.message("done properties at", NumberFormatter.ISOtime());
	}

	/**
	 * Perfom the alignment of one factStore against the other equality
	 * (initialized by caller) is where entity alignments are stored the
	 * relation alignment is returned as a MapperOutput
	 */
	public static MapperOutput oneIterationOneWay(int run, FactStore factStore,
			EqualityStore equalities, EqualityStoreMultiple equalitiesMultiple,
			Setting setting, Result computed) throws InterruptedException {

		MapperOutput mapperOutput = null;

		// Announce.message("starting equalities at",
		// NumberFormatter.ISOtime());
		List<Integer> entities = factStore.properEntities();
		// initialize a queue with all entities to manage
		ConcurrentLinkedQueue<Integer> inputs = new ConcurrentLinkedQueue<Integer>();
		int nAdded = 0;
		if (setting.shuffleEntities) {
			Collections.shuffle(entities);
		}
		for (int i = 0; i < entities.size(); i++) {
			int e1 = entities.get(i);
			if ((factStore.isClass(e1) && Config.ignoreClasses)
			/* || fs1.isRelation(e1) */)
				continue;
			inputs.add(e1);
			nAdded++;
		}
		// Announce.message("run", run, nAdded, "added to queue");

		int limit = run >= 2 && setting.sampleEntities > 0 ? setting.sampleEntities
				: 0;
		int tempNThreads = 0;
		if (setting.nThreads == 1) {
			// perform the computation directly
			Mapper mapper = new Mapper(run, -1, factStore, equalities,
					equalitiesMultiple, new MapperOutput(factStore), null,
					null, inputs, limit, computed, setting);
			mapperOutput = mapper.findEqualsOfQueue();
		} else {
			// spawn threads to perform the computation
			// Announce.message("Will manage", nAdded, "entities");
			mapperOutput = aggregateThreads(run, factStore, equalities,
					equalitiesMultiple, null, null, inputs, limit, setting,
					computed);
		}

		if (limit > 0) {
			// Announce.message("Will end sampling");
			Neighborhood relationGuide = endSampling(run, mapperOutput,
					setting, computed);
			// Announce.message("Will manage the rest now that sampling is done");

			if (setting.nThreads == 1) {
				Mapper mapper2 = new Mapper(run, -1, factStore, equalities,
						equalitiesMultiple, mapperOutput, relationGuide, null,
						inputs, 0, computed, setting);
				mapperOutput = mapper2.findEqualsOfQueue();
			} else {
				mapperOutput = aggregateThreads(run, factStore, equalities,
						equalitiesMultiple, mapperOutput, relationGuide,
						inputs, 0, setting, computed);
			}

			// Announce.done();
		}

		// Announce.done();

		return mapperOutput;
	}

	/**
	 * limit the mapperOutput to interesting alignments and return the relation
	 * guide
	 */
	public static Neighborhood endSampling(int run, MapperOutput mapperOutput,
			Setting setting, Result computed) {
		// the current relation normalizer and neighborhoods are the results of
		// exploring without constraints
		// Announce.message("End of the sampling phase!");
		if (setting.printNeighborhoodsSampling) {
			// Announce.message("BEFORE:");
			mapperOutput.print(computed.other(mapperOutput.fs));
		}
		Neighborhood relationGuide = new HashArrayNeighborhood(mapperOutput.fs,
				-1, true, mapperOutput.fs.getJoinLengthLimit());
		for (int i = 0; i < mapperOutput.fs.maxJoinRelationCode(); i++) {
			if (mapperOutput.neighborhoods[i] == null)
				continue;
			boolean result;
			JoinRelation jr = mapperOutput.fs.joinRelationByCode(i);
			if (mapperOutput.relationNormalizer.getNormalizer(jr) > setting.joinThreshold) {
				result = mapperOutput.neighborhoods[i].thresholdByNormalizer(
						mapperOutput.relationNormalizer.getNormalizer(jr),
						setting.joinThreshold, jr.length() == 1);
			} else {
				mapperOutput.neighborhoods[i] = null;
				result = false;
			}
			if (result) {
				// write in relationGuide that the join relation i in the first
				// ontology should be explored
				Neighborhood cn = relationGuide;
				for (int j = 0; j < jr.length(); j++) {
					assert (jr.get(j) <= mapperOutput.fs.maxRelationId());
					cn = cn.getChild(run, jr.get(j));
				}
				// cn is now the neighborhood representing the join relation i
				// we don't care about the value that it carries, just that it
				// exists
			}
		}
		// relationGuide is now the tree of join relations in onto 1 which align
		// to something in onto 2 (like the statistics module or something)
		// mapperOutput.neighborhoods[i] is now the tree of join relations in
		// onto 2 which align to join relation i in onto 1
		if (setting.printNeighborhoodsSampling) {
			// Announce.message("AFTER:");
			mapperOutput.print(computed.other(mapperOutput.fs));
			// Announce.message("GUIDE:");
			((HashArrayNeighborhood) relationGuide).print(new JoinRelation(
					mapperOutput.fs));
		}
		return relationGuide;
	}

	public static MapperOutput aggregateThreads(int run, FactStore factStore,
			EqualityStore equalities, EqualityStoreMultiple equalitiesMultiple,
			MapperOutput mapperOutput, Neighborhood relationGuide,
			ConcurrentLinkedQueue<Integer> inputs, int limit, Setting setting,
			Result computed) throws InterruptedException {
		// Announce.message("Spawning", setting.nThreads, "threads");
		LinkedList<Thread> threads = new LinkedList<Thread>();
		BlockingQueue<MapperOutput> results = new LinkedBlockingQueue<MapperOutput>();
		for (int i = 0; i < setting.nThreads; i++) {
			MapperOutput myMapperOutput;
			if (mapperOutput == null) {
				myMapperOutput = new MapperOutput(factStore);
			} else {
				// If we want to resume from a mapperOutput, we have to create
				// nThreads copies of it and scale them down by this factor
				myMapperOutput = new MapperOutput(mapperOutput);
				myMapperOutput.scaleDown(setting.nThreads);
			}

			Mapper mapper = new Mapper(run, i, factStore, equalities,
					equalitiesMultiple, myMapperOutput, relationGuide, results,
					inputs, limit, computed, setting);
			Thread thread = new Thread(mapper);
			threads.add(thread);
			thread.start();
		}

		// wait for termination
		for (Thread thread : threads) {
			thread.join();

		}
		// aggregate results in a blank factstore
		mapperOutput = new MapperOutput(factStore);
		for (MapperOutput p : results) {
			mapperOutput.reduceWith(p);
		}
		return mapperOutput;
	}

	private static class Mapper implements Runnable {
		int run;
		EqualityStore equalities;
		EqualityStoreMultiple equalitiesMultiple;
		BlockingQueue<MapperOutput> target;
		ConcurrentLinkedQueue<Integer> inputs;
		FactStore fs1;
		FactStore fs2;
		int id;
		Map<Integer, Double> equalityProduct;
		Map<Pair<Integer, Pair<Integer, Integer>>, Pair<Pair<Double, Double>, Double>> fullEqualityProduct;
		boolean localDebug;
		int localJoinLengthLimit1;
		int localJoinLengthLimit2;
		// guide to explore only the interesting relations and joins in the
		// first ontology
		Neighborhood relationGuide;
		MapperOutput mapperOutput;
		IntSet visited1;
		IntSet visited2;
		int limit;

		Result computed;
		Setting setting;

		public Mapper(int run, int id, FactStore factStore,
				EqualityStore equalities,
				EqualityStoreMultiple equalitiesMultiple,
				MapperOutput mapperOutput, Neighborhood relationGuide,
				BlockingQueue<MapperOutput> target,
				ConcurrentLinkedQueue<Integer> inputs, int limit,
				Result computed, Setting setting) {
			this.computed = computed;
			this.setting = setting;
			this.run = run;
			this.equalities = equalities;
			this.equalitiesMultiple = equalitiesMultiple;
			this.target = target;
			this.fs1 = factStore;
			this.fs2 = computed.other(fs1);
			this.inputs = inputs;
			this.id = id;
			// this is to be able to reduce the join length limit during the
			// process
			this.localJoinLengthLimit1 = fs1.getJoinLengthLimit();
			this.localJoinLengthLimit2 = fs2.getJoinLengthLimit();
			visited1 = new IntOpenHashSet();
			visited2 = new IntOpenHashSet();
			this.limit = limit;
			this.relationGuide = relationGuide;

			if (setting.sampleEntities > 0) {
				// don't do any joins during the few first runs
				if (run < 2) {
					localJoinLengthLimit1 = 1;
					localJoinLengthLimit2 = 1;
				}
			}
			this.mapperOutput = mapperOutput;
		}

		/**
		 * Explore the second ontology.
		 * 
		 * @param visited
		 *            -- hashset of visited relation/object pairs for the fact
		 *            in the first ontology
		 * @param newNeighborhood
		 *            -- the neighborhood at the current iteration, that we
		 *            write
		 * @param x1
		 * @param r1
		 * @param y1
		 *            such that x1 -r1-> y1
		 * @param x2
		 * @param r2
		 * @param y2
		 *            such that x2 -r2-> y2
		 * @param xeqv
		 *            the score between x1 and x2 at the previous iteration
		 * @param oldNeighborhood
		 *            -- the neighborhood at the previous iteration, actually
		 *            unused
		 */
		public void exploreSecondOntology(Neighborhood newNeighborhood, int x1,
				JoinRelation r1, int y1, int x2, JoinRelation r2, int y2,
				double xeqv, Neighborhood oldNeighborhood) {
			if (r2.isTrivial())
				return;
			if (y2 < fs2.numEntities() && Config.ignoreClasses
					&& fs2.isClass(y2))
				return;
			double yeqv = computed.equality(fs1, y1, y2);

			// we assume that there are no duplicate facts
			// hence, for a join relation length of 1, there is no need to check
			// visited
			// so we save time for the specific case where no joins are made
			// Pair<Integer, Integer> p = new Pair<Integer, Integer>(r2.code(),
			// y2);
			int p = r2.code() * fs2.numEntities() + y2;
			if (r2.length() == 1 || !visited2.contains(p)) {

				if (r2.length() > 1) {
					visited2.add(p);
					// if (localDebug) {
					// Announce.debug("New contents of visited:");
					// for (Pair<JoinRelation, Integer> pp : visited) {
					// Announce.debug(pp.first.toString(),
					// fs2.entity(pp.second));
					// }
					// }
				}
				newNeighborhood.registerOccurrence(xeqv);
				newNeighborhood.registerScore(xeqv * yeqv);
				if (equalities != null)
					registerEquality(x1, r1, y1, xeqv, x2, r2, y2);
			} else {
				if (localDebug) {
					// Announce.debug("ignore duplicate", r2.toString(),
					// fs2.entity(y2));
				}
			}

			if (r2.length() >= localJoinLengthLimit2)
				return;
			if (!setting.allowLoops && x2 == y2)
				return;
			if (relationGuide != null && newNeighborhood.isEmpty())
				return;

			List<PredicateAndObject> facts = fs2.factsAbout(y2);
			for (int i = 0; i < facts.size(); i++) {
				int r2bis = facts.get(i).predicate;
				int ny2 = facts.get(i).object;
				// Neighborhood n2 = oldNeighborhood == null ? null :
				// oldNeighborhood.getChildRO(r2bis);
				Neighborhood n2 = oldNeighborhood;
				// if (!extendNeighborhoods && oldNeighborhood == null) {
				// continue;
				// }
				// JoinRelation nr2 = new JoinRelation(r2);
				Neighborhood nn2 = null;
				if (relationGuide == null) {
					nn2 = newNeighborhood.getChild(run, r2bis);
				} else {
					nn2 = newNeighborhood.getChildRO(r2bis);
				}
				if (nn2 == null) {
					continue;
				}
				if (setting.interestingnessThreshold && run > 0) {
					if (!nn2.worthTrying()) {
						continue;
					}
				}
				r2.push(r2bis);
				exploreSecondOntology(nn2, x1, r1, y1, x2, r2, ny2, xeqv, n2);
				r2.pop();
			}
		}

		/**
		 * register evidence for the equality of y1 and y2 from x1 -r1-> y1 and
		 * x2 -r2-> y2
		 */
		public void registerEquality(int x1, JoinRelation r1, int y1,
				double xeqv, int x2, JoinRelation r2, int y2) {

			// when using the one pass method, we must use the small initial
			// weights for
			// the two first iterations
			// otherwise nothing can align
			boolean isFirstRun = (run <= 1);

			// if (!Config.treatIdAsRelation && fs2.getIdRel() != null
			// && r2.isSimpleRelation(-fs2.getIdRel().id))
			// return;
			assert (!Config.treatIdAsRelation);

			double subprop = computed.subRelation(fs2, r2, r1);
			if (subprop >= 0)
				subprop /= Config.epsilon;
			double superprop = computed.subRelation(fs1, r1, r2);
			if (superprop >= 0)
				superprop /= Config.epsilon;

			if (subprop < Config.THETA && superprop < Config.THETA) {
				if (isFirstRun) {
					double val = Config.IOTA
							/ (1 + Config.iotaDependenceOnLength
									* ((r1.length() - 1) + (r2.length() - 1)));
					subprop = val;
					superprop = val;
				} else
					return;
			}

			double fun1 = fs1.functionality(r1) / Config.epsilon;
			double fun1r = fs1.inverseFunctionality(r1) / Config.epsilon;

			double fun2 = fs2.functionality(r2) / Config.epsilon;
			double fun2r = fs2.inverseFunctionality(r2) / Config.epsilon;

			double factor = 1;
			double factor1 = 1 - xeqv * subprop * fun1
					* (Config.bothWayFunctionalities ? fun1r : 1.0);
			double factor2 = 1 - xeqv * superprop * fun2
					* (Config.bothWayFunctionalities ? fun2r : 1.0);
			if (subprop >= 0 && fun1 >= 0)
				factor *= factor1;
			if (Config.subAndSuper && superprop >= 0 && fun2 >= 0)
				factor *= factor2;

			// with the new method, don't do this for literals
			// also don't do it for very small things
			if (!fs2.isLiteral(y2) && 1 - factor > 0.01) {
				if (!setting.useNewEqualityProduct) {
					// classical equality propagation formula from the PARIS
					// paper
					double val = equalityProduct.containsKey(y2) ? equalityProduct
							.get(y2) : 1.0;
					double oldval = val;
					val *= factor;
					assert (val >= 0 && val <= 1);
					equalityProduct.put((Integer) y2, val);
					if (localDebug) {
						// Announce.debug("  Align", fs1.entity(y1), "with",
						// fs2.entity(y2), "for:");
						// Announce.debug("    ", fs1.entity(x1), r1.toString(),
						// fs1.entity(y1));
						// Announce.debug("    ", fs2.entity(x2), r2.toString(),
						// fs2.entity(y2));
						// Announce.debug("     xeqv=", xeqv, "fun1=", fun1,
						// "fun1r=", fun1r, "fun2=", fun2, "fun2r", fun2r,
						// "r1<r2=", subprop, "r2<r1=", superprop);
						// Announce.debug("val=", 1 - val, "oval=", 1 - oldval);
					}
				} else {
					// revised formula from my report
					Pair<Integer, Pair<Integer, Integer>> k = new Pair<Integer, Pair<Integer, Integer>>(
							(Integer) y2, new Pair<Integer, Integer>(x1, x2));

					if (!fullEqualityProduct.containsKey(k)) {
						fullEqualityProduct.put(k,
								new Pair<Pair<Double, Double>, Double>(
										new Pair<Double, Double>(1.0, 1.0),
										xeqv));
					}
					Pair<Pair<Double, Double>, Double> pval = fullEqualityProduct
							.get(k);
					if (subprop >= 0 && fun1 >= 0)
						pval.first.first *= 1 - subprop * fun1
								* (Config.bothWayFunctionalities ? fun1r : 1.0);
					if (Config.subAndSuper && superprop >= 0 && fun2 >= 0)
						pval.first.second *= 1 - superprop * fun2
								* (Config.bothWayFunctionalities ? fun2r : 1.0);
				}
			}
		}

		/**
		 * register evidence for the equality of y1 and y2 from x1 -r1-> y1 and
		 * x2 -r2-> y2 optimized for non-joins
		 */
		public void registerEquality(int x1, int r1, int y1, double xeqv,
				int x2, int r2, int y2) {

			// when using the one pass method, we must use the small initial
			// weights for
			// the two first iterations
			// otherwise nothing can align
			boolean isFirstRun = (run <= 1);

			// if (!Config.treatIdAsRelation && fs2.getIdRel() != null
			// && r2.isSimpleRelation(-fs2.getIdRel().id))
			// return;
			assert (!Config.treatIdAsRelation);

			double subprop = computed.subRelation(fs2, r2, r1);
			if (subprop >= 0)
				subprop /= Config.epsilon;
			double superprop = computed.subRelation(fs1, r1, r2);
			if (superprop >= 0)
				superprop /= Config.epsilon;

			if (subprop < Config.THETA && superprop < Config.THETA) {
				if (isFirstRun) {
					double val = Config.IOTA;
					subprop = val;
					superprop = val;
				} else
					return;
			}

			double fun1 = fs1.functionality(r1) / Config.epsilon;
			double fun2 = fs2.functionality(r2) / Config.epsilon;

			double fun1r = -42;
			double fun2r = -42;

			if (Config.bothWayFunctionalities) {
				fun1r = fs1.inverseFunctionality(r1) / Config.epsilon;
				fun2r = fs2.inverseFunctionality(r2) / Config.epsilon;
			}

			double factor = 1;
			double factor1 = 1 - xeqv * subprop * fun1
					* (Config.bothWayFunctionalities ? fun1r : 1.0);
			double factor2 = 1 - xeqv * superprop * fun2
					* (Config.bothWayFunctionalities ? fun2r : 1.0);
			if (subprop >= 0 && fun1 >= 0)
				factor *= factor1;
			if (Config.subAndSuper && superprop >= 0 && fun2 >= 0)
				factor *= factor2;

			// with the new method, don't do this for literals
			// also don't do it for very small things
			if (!fs2.isLiteral(y2) && 1 - factor > 0.01) {
				if (!setting.useNewEqualityProduct) {
					// classical equality propagation formula from the PARIS
					// paper
					double val = equalityProduct.containsKey(y2) ? equalityProduct
							.get(y2) : 1.0;
					double oldval = val;
					val *= factor;
					assert (val >= 0 && val <= 1);
					equalityProduct.put((Integer) y2, val);
					if (localDebug) {
						// Announce.debug("  Align", fs1.entity(y1), "with",
						// fs2.entity(y2), "for:");
						// Announce.debug("    ", fs1.entity(x1),
						// fs1.relation(r1), fs1.entity(y1));
						// Announce.debug("    ", fs2.entity(x2),
						// fs2.relation(r2), fs2.entity(y2));
						// Announce.debug("     xeqv=", xeqv, "fun1=", fun1,
						// "fun1r=", fun1r, "fun2=", fun2, "fun2r", fun2r,
						// "r1<r2=", subprop, "r2<r1=", superprop);
						// Announce.debug("val=", 1 - val, "oval=", 1 - oldval);
					}
				} else {
					// revised formula from my report
					Pair<Integer, Pair<Integer, Integer>> k = new Pair<Integer, Pair<Integer, Integer>>(
							(Integer) y2, new Pair<Integer, Integer>(x1, x2));

					if (!fullEqualityProduct.containsKey(k)) {
						fullEqualityProduct.put(k,
								new Pair<Pair<Double, Double>, Double>(
										new Pair<Double, Double>(1.0, 1.0),
										xeqv));
					}
					Pair<Pair<Double, Double>, Double> pval = fullEqualityProduct
							.get(k);
					if (subprop >= 0 && fun1 >= 0)
						pval.first.first *= 1 - subprop * fun1
								* (Config.bothWayFunctionalities ? fun1r : 1.0);
					if (Config.subAndSuper && superprop >= 0 && fun2 >= 0)
						pval.first.second *= 1 - superprop * fun2
								* (Config.bothWayFunctionalities ? fun2r : 1.0);
				}
			}
		}

		/**
		 * findEqualsOf for a fixed fact x1 -r1-> y1, other arguments are the
		 * normalizer of relations in fs2, the neighborhood for r1, the equality
		 * products
		 */
		public void findEqualsOfFact(RelationNormalizer normalizer,
				Neighborhood neighborhood, int x1, JoinRelation r1, int y1) {

			if (localDebug) {
				// Announce.debug("run", run, "findEqualsOfFact:",
				// fs1.entity(x1),
				// r1.toString(), fs1.entity(y1));
			}

			// will only be initialized if there is something to do
			Neighborhood oldNeighborhood = null;

			if (!fs1.isLiteral(x1) && Config.ignoreClasses && fs1.isClass(x1))
				return;

			// we don't do that anymore because we need to align relations
			// if (fun1 < Config.THETA)
			// return;

			for (Pair<Object, Double> x2pair : computed
					.equalToScoredId(fs1, x1)) {
				int x2 = (Integer) x2pair.first();
				double xeqv = x2pair.second();
				assert (xeqv >= 0 && xeqv <= 1);

				if (xeqv < Config.THETA)
					continue;

				// for all matching x2, y2's, we need to add weight for the
				// normalizer
				for (Pair<Object, Double> y2pair : computed.equalToScored(fs1,
						y1)) {
					Double yeqv = y2pair.second();
					if (localDebug) {
						Object y2pf = y2pair.first();
						// Announce.debug(
						// "Increment normalizer of",
						// r1.toString(),
						// "by",
						// xeqv * yeqv,
						// "for",
						// fs1.entity(x1),
						// r1.toString(),
						// fs1.entity(y1),
						// fs2.entity(x2),
						// y2pf instanceof String ? fs2
						// .entity((String) y2pf) : fs2
						// .entity((int) y2pf));
					}
					normalizer.incrementSimpleNormalizer(r1, xeqv * yeqv);
					normalizer.incrementCurrentRealNormalizer(xeqv * yeqv);
				}
				normalizer.addNormalizer(r1);

				visited2.clear();

				List<PredicateAndObject> facts = fs2.factsAbout(x2);
				for (int i = 0; i < facts.size(); i++) {
					// if (oldNeighborhood == null)
					// oldNeighborhood = computed.getNeighborhood(fs1, r1);
					int r2bis = facts.get(i).predicate;
					int ny2 = facts.get(i).object;
					Neighborhood n2 = oldNeighborhood == null ? null
							: oldNeighborhood.getChildRO(r2bis);
					JoinRelation nr2 = new JoinRelation(fs2, r2bis);
					Neighborhood nn2 = neighborhood.getChild(run, r2bis);
					if (setting.interestingnessThreshold && run > 0) {
						if (!nn2.worthTrying()) {
							continue;
						}
					}
					// Announce.message("@exploreSecondOntology",
					// fs1.toString(x1), r1.toString(), fs1.toString(y1),
					// "and", fs2.toString(x2), nr2.toString(),
					// fs2.toString(ny2));

					exploreSecondOntology(nn2, x1, r1, y1, x2, nr2, ny2, xeqv,
							n2);
				}
			}

			neighborhood.propagateScores();
		}

		// beware, we accumulate in r1 in the REVERSE order
		/**
		 * find possible r1's and y1's for a given x1 by exploring recursively
		 * around x1, and call findEqualsOfFact for x1 -r1-> y1
		 * 
		 * @param output
		 * @param equalityProduct
		 * @param fullEqualityProduct
		 * @param x1
		 * @param r1
		 * @param y1
		 * @param rg
		 *            -- the current relation guide
		 * 
		 *            Caution: r1 is built in the reverse order and reversed at
		 *            the end when calling findEqualsOfFact
		 */
		public void exploreFirstOntology(int x1, JoinRelation r1, int y1,
				Neighborhood rg) {

			// we assume that there are no duplicate facts
			// hence, for a join relation length of 1, there is no need to check
			// visited
			// Pair<Integer, Integer> pvisited = new Pair<Integer,
			// Integer>(r1.code(), y1);
			int pvisited = r1.code() * fs1.numEntities() + y1;

			if (r1.length() == 1 || !visited1.contains(pvisited)) {
				if (r1.length() > 1)
					visited1.add(pvisited);
				if (localDebug) {
					// don't compute the toString's unless running in debug
					// mode, to save time
					// Announce.debug("Mark as visited", r1.toString(),
					// fs1.entity(y1));
				}

				// TODO2 reverse r1 and reverse it back
				JoinRelation nr1 = new JoinRelation(r1);
				nr1.reverseDirection();
				if (mapperOutput.neighborhoods[nr1.code()] == null) {
					mapperOutput.neighborhoods[nr1.code()] = new HashArrayNeighborhood(
							fs2, run, true, Math.min(fs2.getJoinLengthLimit(),
									setting.sumJoinLengthLimit - nr1.length()));
				}
				findEqualsOfFact(mapperOutput.relationNormalizer,
						mapperOutput.neighborhoods[nr1.code()], x1, nr1, y1);
			} else {
				if (localDebug) {
					// Announce.debug("ignore duplicate", r1.toString(),
					// fs1.entity(y1));
				}
			}

			if (r1.length() >= localJoinLengthLimit1)
				return;
			if (!setting.allowLoops && x1 == y1)
				return;
			if (relationGuide != null && (rg == null || rg.isEmpty()))
				return;
			// we don't consider joins on the first ontology before the second
			// run
			if (run == 0)
				return;

			List<PredicateAndObject> facts = fs1.factsAbout(x1);
			for (int i = 0; i < facts.size(); i++) {
				PredicateAndObject f = facts.get(i);
				if (f.predicate == r1.getLast())
					continue; // relation will be trivial
				int r1bis = FactStore.inverse(f.predicate);
				Neighborhood nrg = null;
				if (relationGuide != null) {
					nrg = rg.getChildRO(f.predicate);
					if (nrg == null)
						continue;
				}
				r1.push(r1bis);
				exploreFirstOntology(f.object, r1, y1, nrg);
				r1.pop();
			}
		}

		/** Find equality candidates for an entity y1 */
		public void findEqualsOf(int y1) {
			// Announce.message("@CALL findEqualsOf", y1, fs1.toString(y1), "");
			// equalityProduct -- maps candidate y2's to their alignment score
			// with y1
			// fullEqualityProduct -- maps candidate y2's and (x1, x2) to their
			// first direction and second direction scores, and to the equiv of
			// x1 and x2
			equalityProduct.clear();
			fullEqualityProduct.clear();

			// Announce.debug("run", run, "findEqualsOf:", fs1.entity(y1), "");
			// HashSet<Pair<Integer, Integer>> visited = new
			// HashSet<Pair<Integer, Integer>>();
			visited1.clear();
			// call exploreFirstOntology for all fact about y1
			// (the first recursive call is unrolled to make things run faster)

			List<PredicateAndObject> facts = fs1.factsAbout(y1);
			for (int i = 0; i < facts.size(); i++) {
				PredicateAndObject f = facts.get(i);
				int nx1 = f.object;
				int r1bis = FactStore.inverse(f.predicate);
				JoinRelation nr1 = new JoinRelation(fs1, r1bis);
				Neighborhood rg = null;
				if (relationGuide != null) {
					rg = relationGuide.getChildRO(f.predicate);
					if (rg == null && !Config.allLengthOneAfterSample)
						continue;
				}
				exploreFirstOntology(nx1, nr1, y1, rg);
			}

			assert (!equalityProduct.keySet().contains(null));

			if (equalities != null)
				setEqualities(y1);
		}

		void setEqualities(int y1) {
			assert (setting.takeMaxMax);

			// double max = 0;
			// Set<Integer> vmax = new TreeSet<Integer>();
			Map<Integer, Double> usefulEqualityProduct;
			if (setting.useNewEqualityProduct) {
				// compute the newEqualityProduct (the one with the revised
				// entity propagation formula) from the fullEqualityProduct
				Map<Integer, Double> newEqualityProduct = new HashMap<Integer, Double>();
				for (Map.Entry<Pair<Integer, Pair<Integer, Integer>>, Pair<Pair<Double, Double>, Double>> e : fullEqualityProduct
						.entrySet()) {
					int y2 = e.getKey().first;
					double val = newEqualityProduct.containsKey(y2) ? newEqualityProduct
							.get(y2) : 1.0;
					double xeqv = e.getValue().second;

					val *= (1 - xeqv * (1 - e.getValue().first.first))
							* (1 - xeqv * (1 - e.getValue().first.second));
					newEqualityProduct.put(y2, val);
				}
				usefulEqualityProduct = newEqualityProduct;
			} else {
				usefulEqualityProduct = equalityProduct;
			}
			if (setting.cleverMatching) {
				equalitiesMultiple.set(y1, usefulEqualityProduct);
				return;
			}
			reduceToMinMin(usefulEqualityProduct);
			for (Integer y2 : usefulEqualityProduct.keySet()) {
				double val = 1 - usefulEqualityProduct.get(y2);
				// if (val < Config.THETA)
				// continue;
				// foundEquality = true;
				equalities.setValue(y1, y2, val);
				// if (val > max) {
				// vmax.clear();
				// vmax.add(y2);
				// max = val;
				// }
				// if (val == max)
				// if (!setting.takeMaxMax || vmax.isEmpty())
				// vmax.add(y2);

			}

		}

		/** Find equality candidates for an entity y1 */
		public void findEqualsOf1(int y1) {
			equalityProduct.clear();
			fullEqualityProduct.clear();

			List<PredicateAndObject> facts = fs1.factsAbout(y1);
			for (int i = 0; i < facts.size(); i++) {
				int x1 = facts.get(i).object;
				int r1bis = FactStore.inverse(facts.get(i).predicate);

				if (!fs1.isLiteral(x1) && Config.ignoreClasses
						&& fs1.isClass(x1))
					continue;

				if (mapperOutput.neighborhoods[r1bis] == null) {
					mapperOutput.neighborhoods[r1bis] = new HashArrayNeighborhood(
							fs2, run, true, Math.min(fs2.getJoinLengthLimit(),
									setting.sumJoinLengthLimit - 1));
				}
				Neighborhood currentNeighborhood = mapperOutput.neighborhoods[r1bis];

				for (Pair<Object, Double> x2pair : computed.equalToScoredId(
						fs1, x1)) {
					int x2 = (Integer) x2pair.first();
					double xeqv = x2pair.second();
					assert (xeqv >= 0 && xeqv <= 1);

					if (xeqv < Config.THETA)
						continue;

					// for all matching x2, y2's, we need to add weight for the
					// normalizer
					for (Pair<Object, Double> y2pair : computed.equalToScored(
							fs1, y1)) {
						Double yeqv = y2pair.second();
						mapperOutput.relationNormalizer
								.incrementSimpleNormalizer(r1bis, xeqv * yeqv);
						mapperOutput.relationNormalizer
								.incrementCurrentRealNormalizer(xeqv * yeqv);
						// if (nr1.toString().startsWith("dbp:infl") ||
						// nr1.toString().startsWith("influences"))
						// Announce.message("@@@normalizer", nr1.toString(),
						// fs1.entity(x1), fs1.entity(y1), fs2.entity(x2),
						// fs2.entity((Integer) y2pair.first()), xeqv, yeqv);
					}
					mapperOutput.relationNormalizer.addNormalizer(r1bis);

					List<PredicateAndObject> facts2 = fs2.factsAbout(x2);
					for (int j = 0; j < facts2.size(); j++) {
						int ny2 = facts2.get(j).object;
						double yeqv = computed.equality(fs1, y1, ny2);
						int r2bis = facts2.get(j).predicate;
						Neighborhood nn2 = currentNeighborhood.getChild(run,
								r2bis);
						nn2.registerOccurrence(xeqv);
						nn2.registerScore(xeqv * yeqv);
						// if (fs2.relation(r2bis).startsWith("dbp:infl") ||
						// fs2.relation(r2bis).toString().startsWith("influences"))
						// Announce.message("@@@score", nr1.toString(),
						// fs2.relation(r2bis), fs1.entity(x1), fs1.entity(y1),
						// fs2.entity(x2), fs2.entity(ny2), xeqv, yeqv);
						if (equalities != null)
							registerEquality(x1, r1bis, y1, xeqv, x2, r2bis,
									ny2);
					}
				}

				currentNeighborhood.propagateScores();
			}

			if (equalities != null)
				setEqualities(y1);
		}

		/** Run findEqualsOf on entities fetched from inputs */
		public MapperOutput findEqualsOfQueue() {

			equalityProduct = new HashMap<Integer, Double>();
			fullEqualityProduct = new HashMap<Pair<Integer, Pair<Integer, Integer>>, Pair<Pair<Double, Double>, Double>>();

			int done = 0;
			long start = System.currentTimeMillis();
			long last = start;
			int nManaged = 0;
			while (true) {
				Integer e1;
				try {
					e1 = inputs.remove();
				} catch (java.util.NoSuchElementException e) {
					// someone took the last item from the queue before we did
					break;
				}
				++done;
				if (done % setting.reportInterval == 0) {
					// Announce.message("Entities done:", done,
					// "Time per entity:", timeSum
					// / ((float) reportInterval), "ms     Facts per entity:",
					// factSum /
					// ((float) reportInterval),
					// NumberFormatter.formatMS((long) ((double)
					// (System.currentTimeMillis()
					// - timeStart) / (done - startAt) * (total - done))));
					long t = System.currentTimeMillis();
					double perEntity = (t - start) / ((float) done);
					// Announce.message("(" + id + ") Entities done:", done,
					// "Time per entity:", perEntity, "ms");
					// Announce.message("(" + id + ") Last time:", t - last);
					// Announce.message("(" + id + ") Last entity:",
					// fs1.entity(e1));
					last = t;
				}
				nManaged++;

				if (nManaged == limit) {
					break;
				}

				if (setting.debugEntity != null) {
					if (fs1.entity(e1).contains(setting.debugEntity)) {
						// Announce.message("DEBUGENTITY");
						// Announce.setLevel(Level.DEBUG);
						localDebug = true;
					}
				}

				if (localJoinLengthLimit1 == 1 && localJoinLengthLimit2 == 1
						&& setting.sampleEntities == 0
						&& setting.optimizeNoJoins)
					findEqualsOf1(e1);
				else
					findEqualsOf(e1);

				if (setting.debugEntity != null) {
					if (fs1.entity(e1).contains(setting.debugEntity)) {
						// Announce.setLevel(Level.MESSAGES);
						localDebug = false;
					}
				}
			}
			// Announce.message("run", run, nManaged, "actually managed");
			return mapperOutput;
		}

		public void run() {
			target.add(findEqualsOfQueue());
		}
	}

	/** Reduces a map to one value with the minimum */
	public static <T> void reduceToMinMin(Map<T, Double> map) {
		if (map.isEmpty())
			return;
		double min = 2;
		T key = null;
		for (T v : map.keySet()) {
			if (map.get(v) < min) {
				key = v;
				min = map.get(v);
			}
		}
		map.clear();
		map.put(key, min);
	}

}
