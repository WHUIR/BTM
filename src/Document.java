import java.awt.print.Printable;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Document {
  
  public String [] words;
  public int id;
  
  public Document(int docid, String [] words){
    this.id = docid;
    this.words = words;

  }
  
  public static ArrayList<Document> LoadCorpus(String filename){
	    try{
	      FileInputStream fis = new FileInputStream(filename);
	      InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
	      BufferedReader reader = new BufferedReader(isr);
	      String line;
	      int id = 1;
	      ArrayList<Document> doc_list = new ArrayList();
	      while((line = reader.readLine()) != null){
	        line = line.trim();
	        String[] words = line.split(" ");
	        Document doc = new Document(id, words);
	        doc_list.add(doc);
	        id+=1;
	      }
	      return doc_list;
	    }
	    catch (Exception e){
	      System.out.println("Error while reading other file:" + e.getMessage());
	      e.printStackTrace();
//	      return false;
	  }
	    return null;
	    
	  }

}
