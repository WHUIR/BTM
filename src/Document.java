import java.awt.print.Printable;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Document {
  
  public String [] words;
  public int id;
  public String room_id;
  public String room_user_id;
  public String time;
  public String comment_user_id;
  
  
  public Document(int docid, String [] words, String r_id, String r_u_id, String t, String c_u_id){
	  
    this.id = docid;
    this.words = words;
    this.room_id = r_id;
    this.room_user_id = r_u_id;
    this.time = t;
    this.comment_user_id = c_u_id;
    
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
	        String[] items = line.split("\t");
	        String[] words = items[0].trim().split(" ");
	        String[] others = items[1].trim().split(" ");
	        String r_id = others[0];
	        String r_u_id = "-1";
	        try{
	        	r_u_id = others[1];
	        }
	        catch(Exception t){
	        	System.out.println("fuck");
	        }
	        String t = others[2] + others[3];
	        String c_u_id = others[4];
	        Document doc = new Document(id, words,r_id,r_u_id,t,c_u_id);
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
