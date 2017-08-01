import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

public class DMM {
	public Set<String> wordSet;
	public int numTopic;
	public double alpha, beta;
	public int numIter;
	public int saveStep;
	public ArrayList<Document> docList;
	public int roundIndex;
	private Random rg;
	public double threshold;
	public double weight;
	public int topWords;
	public int filterSize;
	public String word2idFileName;
	public String similarityFileName;

	public Map<String, Integer> word2id;
	public Map<Integer, String> id2word;
	public Map<Integer, Double> wordIDFMap;
	public Map<Integer, Map<Integer, Double>> docUsefulWords;
	public ArrayList<ArrayList<Integer>> topWordIDList;
	public int vocSize;
	public int numDoc;
	public ArrayList<int[]> docToWordIDList;
	public String initialFileName;  // we use the same initial for DMM-based model
	public double[][] phi;
	private double[] pz;
	private double[][] pdz;

	public ArrayList<ArrayList<Boolean>> wordGPUFlag; // wordGPUFlag.get(doc).get(wordIndex)
	public int[] assignmentList; // topic assignment for every document
	public ArrayList<ArrayList<Map<Integer, Double>>> wordGPUInfo;

	private int[] mz; // have no associatiom with word and similar word
	private double[] nz; // [topic]; nums of words in every topic
	private double[][] nzw; // V_{.k}

	public DMM(ArrayList<Document> doc_list, int num_topic, int num_iter, int save_step, double beta,
			double alpha) {
		docList = doc_list;
		numDoc = docList.size();
		numTopic = num_topic;
		this.alpha = alpha;
		numIter = num_iter;
		saveStep = save_step;
		this.beta = beta;

	}

	public boolean loadWordMap(String filename) {
		try {
			FileInputStream fis = new FileInputStream(filename);
			InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
			BufferedReader reader = new BufferedReader(isr);
			String line;
			
			//construct word2id map
			while ((line = reader.readLine()) != null) {
				String[] items = line.split(" ");
				word2id.put(items[0], Integer.parseInt(items[1]));
				id2word.put(Integer.parseInt(items[1]), items[0]);
			}
			reader.close();
			System.out.println("finish read wordmap and the num of word is " + word2id.size());
			return true;
		} catch (Exception e) {
			System.out.println("Error while reading other file:" + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	
	/**
	 * 
	 * @param filename for topic assignment for each document
	 */
	public void loadInitialStatus(String filename) {
		try {
			FileInputStream fis = new FileInputStream(filename);
			InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
			BufferedReader reader = new BufferedReader(isr);
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				String[] items = line.split(" ");
				assert(items.length == assignmentList.length);
				for (int i = 0; i < items.length; i++) {
					assignmentList[i] = Integer.parseInt(items[i]);
				}
				break;
			}
			reader.close();
			

			System.out.println("finish loading initial status");
		} catch (Exception e) {
			System.out.println("Error while reading other file:" + e.getMessage());
			e.printStackTrace();
		}
	}

	public void normalCount(Integer topic, int[] termIDArray, Integer flag) {
		mz[topic] += flag;
		for (int t = 0; t < termIDArray.length; t++) {
			int wordID = termIDArray[t];
			nzw[topic][wordID] += flag;
			nz[topic] += flag;
		}
	}
	
	
	public void initNewModel() {
		docToWordIDList = new ArrayList<int[]>();
		word2id = new HashMap<String, Integer>();
		id2word = new HashMap<Integer, String>();
		wordIDFMap = new HashMap<Integer, Double>();
		docUsefulWords = new HashMap<Integer, Map<Integer, Double>>();
		wordSet = new HashSet<String>();
		topWordIDList = new ArrayList<>();
		assignmentList = new int[numDoc];
		rg = new Random();
		// construct vocabulary
		loadWordMap(word2idFileName);

		vocSize = word2id.size();
		phi = new double[numTopic][vocSize];
		pz = new double[numTopic];
		pdz = new double[numDoc][numTopic];

		for (int i = 0; i < docList.size(); i++) {
			Document doc = docList.get(i);
			int[] termIDArray = new int[doc.words.length];
			for (int j = 0; j < doc.words.length; j++) {
				termIDArray[j] = word2id.get(doc.words[j]);

			}

			docToWordIDList.add(termIDArray);
		}

		// init the counter
		mz = new int[numTopic];
		nz = new double[numTopic];
		nzw = new double[numTopic][vocSize];
	}

	public void init_GSDMM() {
//		loadInitialStatus(initialFileName);

		for (int d = 0; d < docToWordIDList.size(); d++) {
			int[] termIDArray = docToWordIDList.get(d);
//			int topic = assignmentList[d];
			 int topic = rg.nextInt(numTopic);
			 assignmentList[d] = topic;
			normalCount(topic, termIDArray, +1);
		}
		System.out.println("finish init_MU!");
	}

	private static long getCurrTime() {
		return System.currentTimeMillis();
	}

	public void run_iteration(String flag) {

		for (int iteration = 1; iteration <= numIter; iteration++) {
			System.out.println(iteration + "th iteration begin");
			if (iteration % this.saveStep == 0){
				saveModel(flag+"_iter"+iteration);
			}
			long _s = getCurrTime();
			for (int s = 0; s < docToWordIDList.size(); s++) {

				int[] termIDArray = docToWordIDList.get(s);
				int preTopic = assignmentList[s];

				normalCount(preTopic, termIDArray, -1);

				double[] pzDist = new double[numTopic];
				for (int topic = 0; topic < numTopic; topic++) {
					double pz = 1.0 * (mz[topic] + alpha) / (numDoc - 1 + numTopic * alpha);
					double value = 1.0;
					double logSum = 0.0;
					for (int t = 0; t < termIDArray.length; t++) {
						int termID = termIDArray[t];
						value *= (nzw[topic][termID] + beta) / (nz[topic] + vocSize * beta + t);
						// we do not use log, it is a little slow
						// logSum += Math.log(1.0 * (nzw[topic][termID] + beta) / (nz[topic] + vocSize * beta + t));
					}
//					value = pz * Math.exp(logSum);
					value = pz * value;
					pzDist[topic] = value;
				}

				for (int i = 1; i < numTopic; i++) {
					pzDist[i] += pzDist[i - 1];
				}

				double u = rg.nextDouble() * pzDist[numTopic - 1];
				int newTopic = -1;
				for (int i = 0; i < numTopic; i++) {
					if (Double.compare(pzDist[i], u) >= 0) {
						newTopic = i;
						break;
					}
				}
				// update
				assignmentList[s] = newTopic;
				normalCount(newTopic, termIDArray, +1);

			}
			long _e = getCurrTime();
			System.out.println(iteration + "th iter finished and every iterration costs " + (_e - _s) + "ms! Snippet "
					+ numTopic + " topics round " + roundIndex);
		}
	}

	public void saveModel(String flag) {

		compute_phi();
		compute_pz();
		compute_pzd();
		saveModelPz(flag + "_theta.txt");
		saveModelPhi(flag + "_phi.txt");
		saveModelWords(flag + "_words.txt");
		saveModelAssign(flag + "_assign.txt");
		saveModelPdz(flag + "_pdz.txt");
	}

	public void compute_phi() {
		for (int i = 0; i < numTopic; i++) {
			double sum = 0.0;
			for (int j = 0; j < vocSize; j++) {
				sum += nzw[i][j];
			}
			for (int j = 0; j < vocSize; j++) {
				phi[i][j] = (nzw[i][j] + beta) / (sum + vocSize * beta);
			}
		}
	}

	public void compute_pz() {
		double sum = 0.0;
		for (int i = 0; i < numTopic; i++) {
			sum += nz[i];
		}
		for (int i = 0; i < numTopic; i++) {
			pz[i] = 1.0 * (nz[i] + alpha) / (sum + numTopic * alpha);
		}
	}

	public void compute_pzd() {
		double[][] pwz = new double[vocSize][numTopic]; // pwz[word][topic]
		for (int i = 0; i < vocSize; i++) {
			double row_sum = 0.0;
			for (int j = 0; j < numTopic; j++) {
				pwz[i][j] = pz[j] * phi[j][i];
				row_sum += pwz[i][j];
			}
			for (int j = 0; j < numTopic; j++) {
				pwz[i][j] = pwz[i][j] / row_sum;
			}

		}

		for (int i = 0; i < numDoc; i++) {
			int[] doc_word_id = docToWordIDList.get(i);
			double row_sum = 0.0;
			for (int j = 0; j < numTopic; j++) {

				for (int wordID : doc_word_id) {
					pdz[i][j] += pwz[wordID][j];
				}
				row_sum += pdz[i][j];

			}
			for (int j = 0; j < numTopic; j++) {
				pdz[i][j] = pdz[i][j] / row_sum;
			}
		}
	}
	
	public boolean saveModelAssign(String filename) {
		try {
			PrintWriter out = new PrintWriter(filename);

			for (int i = 0; i < numDoc; i++) {
				int topic = assignmentList[i];
				out.print(topic + " ");
				}
			out.println();
			out.flush();
			out.close();
		} catch (Exception e) {
			System.out.println("Error while saving assign list: " + e.getMessage());
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public boolean saveModelPdz(String filename) {
		try {
			PrintWriter out = new PrintWriter(filename);

			for (int i = 0; i < numDoc; i++) {
				for (int j = 0; j < numTopic; j++) {
					out.print(pdz[i][j] + " ");
				}
				out.println();
			}

			out.flush();
			out.close();
		} catch (Exception e) {
			System.out.println("Error while saving p(z|d) distribution:" + e.getMessage());
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public boolean saveModelPz(String filename) {
		// return false;
		try {
			PrintWriter out = new PrintWriter(filename);

			for (int i = 0; i < numTopic; i++) {
				out.print(pz[i] + " ");
			}
			out.println();

			out.flush();
			out.close();
		} catch (Exception e) {
			System.out.println("Error while saving pz distribution:" + e.getMessage());
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public boolean saveModelPhi(String filename) {
		try {
			PrintWriter out = new PrintWriter(filename);

			for (int i = 0; i < numTopic; i++) {
				for (int j = 0; j < vocSize; j++) {
					out.print(phi[i][j] + " ");
				}
				out.println();
			}
			out.flush();
			out.close();
		} catch (Exception e) {
			System.out.println("Error while saving word-topic distribution:" + e.getMessage());
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public boolean saveModelWords(String filename) {
		try {
			PrintWriter out = new PrintWriter(filename, "UTF8");
			for (String word : word2id.keySet()) {
				int id = word2id.get(word);
				out.println(word + "," + id);
			}
			out.flush();
			out.close();
		} catch (Exception e) {
			System.out.println("Error while saveing words list: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}


	public static void main(String[] args) {

		ArrayList<Document> doc_list = Document.LoadCorpus("data/dataset.txt");
		//here
		int numIter = 1000, save_step = 1000;
		double beta = 0.1;

		for (int round = 1; round <= 1; round += 1) {
			for (int num_topic = 50; num_topic <= 50; num_topic += 50) {

				double alpha = 1.0 * 50 / num_topic;
				DMM gsdmm = new DMM(doc_list, num_topic, numIter, save_step, beta, alpha);
				gsdmm.word2idFileName = "data/wordmap.txt";
				gsdmm.topWords = 100;
				
				//here
				gsdmm.roundIndex = round;
				gsdmm.initNewModel();
				gsdmm.init_GSDMM();
				String flag = round+"round_"+num_topic + "topic";
				flag = "DMM/" + flag;
				gsdmm.run_iteration(flag);
				gsdmm.saveModel(flag);
			}
		}
	}

}

/**
 * Comparator to rank the words according to their probabilities.
 */
class TopicalWordComparator implements Comparator<Integer> {
	private double[] distribution = null;

	public TopicalWordComparator(double[] distribution2) {
		distribution = distribution2;
	}

	@Override
	public int compare(Integer w1, Integer w2) {
		if (distribution[w1] < distribution[w2]) {
			return -1;
		} else if (distribution[w1] > distribution[w2]) {
			return 1;
		}
		return 0;
	}
}
