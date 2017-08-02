
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Vector;



public class Main {
  
  public static void main(String[] args) {
    // TODO 自动生成的方法存根
    ArrayList<Document> doc_list = Document.LoadCorpus("data/comments.txt");
//    int num_longdoc = 500;
    int num_iter = 1000, save_step = 1000;
    double beta=0.01;
    for(int round=1;round<=1;round++){
    	for(int num_topic=200;num_topic<=200;num_topic+= 50){
 
    			double alpha = 50/num_topic;
    
        		BTM btm = new BTM(doc_list, num_topic, num_iter, 
        	    		save_step, beta, alpha, round);
            	String flag = round+"round_topic"+num_topic;
            	flag = "/BTM/"+flag;
      //      	btm.initialFileName = InitialFileName;
            	btm.run_BTM(flag);
          //  	btm.printDocUsefulWords();
    			
    		
    	}
    	
    }
    
  }

}
