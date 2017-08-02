/**
 * 
 */


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;


/**
 * @author Li Chenliang [lich0020@ntu.edu.sg]
 *
 */
public class BTM {
	private int numTopic;
	private double alpha;
	private double beta;
	private int numIter;
	private int saveStep;
	private Random rg;
	
	private Map<Biterm, Integer> biterm2id;
    private Map<Integer, Biterm> id2biterm;
	private Map<String, Integer> word2id;
	private Map<Integer, String> id2word;
	private int vocSize;
	private int bitermSize;
	private List<List<Biterm>> docToBitermList;
	
	private List<int[]> assignmentList;
	public String initialFileName;
	
	private double[][] pdz; // p(z|d): [doc][topic]
	private double[][] phi; // p(w|z): [topic][word]
	private double[] pz;
	
	private int[][] N; // [topic][word] count
	
	// the number of terms in all the 
	// biterms assigned to a topic.
	private int[] topicCnts; 
	
	
	
	public BTM(ArrayList<Document> doc_list, int num_topic,
        int num_iter, int save_step, double beta, double alpha, int roundIndex){
        // init parameter
        numTopic = num_topic;
        this.alpha = alpha;
        this.beta = beta;
        this.numIter = num_iter;
        this.saveStep = save_step;
        
        //init map
        word2id = new HashMap<String, Integer>();
        id2word = new HashMap<Integer, String>();
        biterm2id = new HashMap<Biterm, Integer>();
        id2biterm = new HashMap<Integer, Biterm>();
        docToBitermList = new ArrayList<List<Biterm>>();
        assignmentList = new ArrayList<int[]>();
        for(int i=0;i<doc_list.size();i++){
            Document doc = doc_list.get(i);
            for(String word: doc.words){
              
            	if(word2id.containsKey(word)){
            	  continue;
              }else{
            	  word2id.put(word, word2id.size());
              }
            }
            //construct biterm list
            ArrayList<Biterm> bitermList = new ArrayList<Biterm>();
            for(int j=1;j<doc.words.length;j++){
            	for(int k=0;k<j;k++){
            		Biterm biterm = new Biterm(word2id.get(doc.words[j]), word2id.get(doc.words[k]));
            		
            		if(!biterm2id.containsKey(biterm)){
            			biterm2id.put(biterm, biterm2id.size());
            		}
            		bitermList.add(biterm);
            	}
            }
            docToBitermList.add(bitermList);
    	  }
    	  
    	  for(Map.Entry<String, Integer> entry:word2id.entrySet()){
      	    Integer wordID = entry.getValue();
      	    String word = entry.getKey();
      	    id2word.put(wordID, word);
          }
    	  for(Map.Entry<Biterm, Integer> entry: biterm2id.entrySet()){
    		  Integer bitermID = entry.getValue();
    		  Biterm biterm = entry.getKey();
    		  id2biterm.put(bitermID, biterm);
    	  }
        
        rg = new Random();
  }
	
	public boolean printDocUsefulWords() {
		try {
			PrintWriter out = new PrintWriter("d:/biterm_doc.txt", "UTF8");
			int index = 0;
			for (int d = 0; d < docToBitermList.size(); d++) {
				List<Biterm>  bitermList = docToBitermList.get(d);
				
				for (Biterm biterm : bitermList) {
					String content = index + "\t" + "test" + "|";
					String tmp = id2word.get(biterm.biterm()[0]) + " " + id2word.get(biterm.biterm()[1]);
					content += tmp;
					out.println(content);
					index ++;
				}
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

	
	public void initPara(){
		vocSize = word2id.size();
		bitermSize = biterm2id.size();
		phi = new double[numTopic][vocSize];
		pz = new double[numTopic];
		pdz = new double[docToBitermList.size()][numTopic];
		for(int i=0;i<docToBitermList.size();i++){
			for(int j=0;j<numTopic;j++){
				pdz[i][j] = 0.0;
			}
		}
		assignmentList = new ArrayList<int[]>(docToBitermList.size());
//		int index = 0;
//		for ( String word : wordSet){
//			word2id.put(word, index);
//			id2word.put(index, word);
//			index++;
//		}
		
//		int bitermIndex = 0;
//        for(Biterm biterm:bitermSet){
//          biterm2id.put(biterm, bitermIndex);
//          id2biterm.put(bitermIndex, biterm);
//          bitermIndex++;
//        }
		
		N = new int[numTopic][vocSize];
		topicCnts = new int[numTopic];
	}
	
	public void loadInitialStatus(String filename){
		try {
			FileInputStream fis = new FileInputStream(filename);
			InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
			BufferedReader reader = new BufferedReader(isr);
			String line;
			String[] items = new String[bitermSize];
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				items = line.split(" ");
				break;
			}
			int index=0;
			int z_k;
			for ( int d = 0; d < docToBitermList.size(); d++ ){
				List<Biterm> bitermList = docToBitermList.get(d);
				int[] assignments = new int[bitermList.size()];
				for (int b = 0; b < bitermList.size(); b++) {
					Biterm biterm = bitermList.get(b);
					Integer[] biterms = biterm.biterm();
					z_k = Integer.parseInt(items[index]);
					index++;
					
					assignments[b] = z_k;
					for (int i = 0; i < biterms.length; i++) {
						int termid = biterms[i];
						N[z_k][termid]++;
					}
					topicCnts[z_k]++;
					//bitermSize++;
				}
				
				assignmentList.add(assignments);
			}

			System.out.println("finish loading initial status");
		} catch (Exception e) {
			System.out.println("Error while reading other file:" + e.getMessage());
			e.printStackTrace();
		}
	}
	
	
	public void initModel(){
		int y = 0;
		int z_k = 0;
		for ( int d = 0; d < docToBitermList.size(); d++ ){
			List<Biterm> bitermList = docToBitermList.get(d);
			int[] assignments = new int[bitermList.size()];
			for (int b = 0; b < bitermList.size(); b++) {
				Biterm biterm = bitermList.get(b);
				Integer[] biterms = biterm.biterm();
				z_k = rg.nextInt(numTopic);
				assignments[b] = z_k;
				for (int i = 0; i < biterms.length; i++) {
					int termid = biterms[i];
					N[z_k][termid]++;
				}
				topicCnts[z_k]++;
		//		bitermSize++;
			}
			
			assignmentList.add(assignments);
		}
		System.out.println("finish initModel!");
	}
	
	private static long getCurrTime() {
		return System.currentTimeMillis();
	}
	
	public void run_iteration(){
		double[] prop = new double[numTopic];
		double sum = 0;
		for ( int iter = 1; iter <= numIter; iter++ ){
			System.out.println(iter + "th iteration begin");
			long _s = getCurrTime();
			
			for (int d = 0; d < docToBitermList.size(); d++) {
				List<Biterm> bitermList = docToBitermList.get(d);
				int[] d_assignments = 
					assignmentList.get(d);
				for (int b = 0; b < bitermList.size(); b++) {
					Biterm biterm = bitermList.get(b);
					Integer[] biterms = biterm.biterm();
					int oldTopic = d_assignments[b];
					int termid1 =biterms[0];
					int termid2 = biterms[1];
					
					// update the counter
					for (int i = 0; i < biterms.length; i++) {
						int termid = biterms[i];
						N[oldTopic][termid]--;
					}
					topicCnts[oldTopic]--;
					
					sum = 0.0;
					for (int k = 0; k < numTopic; k++) {
						prop[k] = 1.0 * (topicCnts[k] + alpha)
								* (N[k][termid1] + beta)
								/ (topicCnts[k]*2 + vocSize * beta + 1)
								* (N[k][termid2] + beta)
								/ (topicCnts[k]*2 + vocSize * beta + 1);
						sum += prop[k];
					}
					
					int newTopic = sampleZ(prop, sum);
					for (int i = 0; i < biterms.length; i++) {
						int termid = biterms[i];
						N[newTopic][termid]++;
					}
					topicCnts[newTopic]++;
					d_assignments[b] = newTopic;
				}
			}
			
			long _e = getCurrTime();
			System.out.println(iter
					+ "th iter finished and every iterration costs "
					+ (_e - _s) + "ms!  BTM QA " + numTopic);
		}
	}
	
	private int sampleZ(double[] dist, double sum){
		double u = rg.nextDouble()*sum;
		double temp = 0.0;
		int index = 0;
		for ( int i = 0; i < dist.length; i++ ){
			temp += dist[i];
			if ( Double.compare(temp, u) >= 0 ){
				index = i;
				break;
			}
		}
		
		return index;
	}
	
	public void run_BTM(String flag){
	  initPara();
      initModel();
  //    loadInitialStatus(initialFileName);
      System.out.println(vocSize+", numDOct:"+docToBitermList.size()+", biterm size: "+bitermSize);
      run_iteration();
      saveModel(flag);
	}
	
	public void saveModel(String flag){
      compute_pz();
      saveModelPz(flag+"_BTM_pz.txt");
      compute_phi();
      saveModelPhi(flag+"_BTM_phi.txt");
      compute_pdz();
      saveModelPdz(flag+"_BTM_pdz.txt");
      
      saveModelWords(flag+"_BTM_words.txt");
    }
	
	
	
	public void compute_phi() {
		for (int i = 0; i < numTopic; i++) {
			for (int j = 0; j < vocSize; j++) {
				phi[i][j] = (N[i][j] + beta) / (topicCnts[i]*2 + vocSize * beta);
			}
		}
	}
	
	public void compute_pz(){
	  double sum = 0.0;
	  for (int i=0;i<numTopic;i++){
	    sum += topicCnts[i];
	  }
		for ( int i = 0; i < numTopic; i++ ){
			pz[i] = 1.0*(topicCnts[i]+alpha)/(sum + numTopic*alpha);
		}
	}
	
	
	public void compute_pdz(){
		// using PBDMap, and Equation 3 in BTM paper.
	   // using PBDMap, and Equation 3 in BTM paper.
      double[][] pbz = new double[bitermSize][numTopic];
      for(Map.Entry<Biterm, Integer> entry:biterm2id.entrySet()){
        Integer bitermID = entry.getValue();
        Biterm biterm = entry.getKey();
        double row_sum = 0.0;
        for(int topicID=0;topicID<numTopic;topicID++){
          int word1_id = biterm.biterm()[0];
          int word2_id = biterm.biterm()[1];
          pbz[bitermID][topicID] = pz[topicID] * phi[topicID][word1_id] * phi[topicID][word2_id];
          row_sum += pbz[bitermID][topicID];
        }
        
        // normalize
        for(int i=0;i<numTopic;i++){
          pbz[bitermID][i] = pbz[bitermID][i] / row_sum;
        }
      }
      
      for(int docID=0;docID<docToBitermList.size();docID++){
        double row_sum = 0.0;
        List<Biterm> bitermList = docToBitermList.get(docID);
        for(int topicID=0;topicID<numTopic;topicID++){
            for(Biterm biterm: bitermList){
              pdz[docID][topicID] += pbz[biterm2id.get(biterm)][topicID];
            }
//            pdz[docID][topicID] *= pz[topicID];
            row_sum += pdz[docID][topicID];
        }
        
        for(int i=0;i<numTopic;i++){
          pdz[docID][i] = pdz[docID][i] / row_sum; 
        }
       
      }
	}
	
//	public void compute_prod_pzd(){
//		  double[][] pwz = new double[vocSize][numTopic];  //pwz[word][topic]
//		  for(int i=0;i<vocSize;i++){
//		    double row_sum = 0.0;
//		    for(int j=0;j<numTopic;j++){
//		      pwz[i][j] = pz[j]*phi[j][i];
//		      row_sum += pwz[i][j];
//		    }
//		    for(int j=0;j<numTopic;j++){
//		      pwz[i][j] = pwz[i][j] / row_sum;
//		    }
//		    
//		  }
//		  
//		  for(int i=0;i<docToBitermList.size();i++){
//		    int[] doc_word_id = docToWordIDList.get(i);
//		    double row_sum = 0.0;
//		    for(int j=0;j<numTopic;j++){
//		      
//		      for(int wordID:doc_word_id){
//		        pdz[i][j] += pwz[wordID][j];
//		      }
//		      row_sum += pdz[i][j];
//		      
//		    }
//		    for(int j=0;j<numTopic;j++){
//	          pdz[i][j] = pdz[i][j]/row_sum;
//	        }
//		  }
//		}
	
	
	// some saveModel.... functions
    // ......
       public boolean saveModelPz(String filename){
//        return false;
            try {
                PrintWriter out = new PrintWriter(filename);

                for (int i = 0; i < numTopic; i++) {
                        out.print(pz[i] + " ");
                }
                out.println();

                out.flush();
                out.close();
            } catch (Exception e) {
                System.out.println("Error while saving pz distribution:"
                        + e.getMessage());
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
                System.out.println("Error while saving word-topic distribution:"
                        + e.getMessage());
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
                System.out.println("Error while saveing words list: "
                        + e.getMessage());
                e.printStackTrace();
                return false;
            }
            return true;
        }
       
       
       public boolean saveModelPdz(String filename){
         try {
           PrintWriter out = new PrintWriter(filename);

           for (int i=0;i<docToBitermList.size();i++) {
               for (int j = 0; j < numTopic; j++) {
                   out.print(pdz[i][j] + " ");
               }
               out.println();
           }

           out.flush();
           out.close();
         } catch (Exception e) {
             System.out.println("Error while saving p(z|d) distribution:"
                     + e.getMessage());
             e.printStackTrace();
             return false;
         }
  
         return true;
       }
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
