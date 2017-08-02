/**
 * 
 */


/**
 * @author Li Chenliang [lich0020@ntu.edu.sg]
 *
 */
public class Biterm {

	private Integer[] biterm;
	
	public Biterm(Integer term1, Integer term2){
		biterm = new Integer[2];
		biterm[0] = term1;
		biterm[1] = term2;
	}
	
	public Integer[] biterm(){
		return biterm;
	}
	
	@Override
	public int hashCode(){
		int tmp = 0;
		if ( biterm[0] != null ){
			tmp += biterm[0].hashCode();
		}	
		if ( biterm[1] != null ){
			tmp += biterm[1].hashCode();
		}
		
		return tmp;
	}
	
	@Override
	public boolean equals(Object obj){
		if ( obj == this )
			return true;
		
		if ( obj instanceof Biterm ){
			final Biterm other = (Biterm)obj;
			return ((biterm[0].equals(other.biterm[0]) && 
					biterm[1].equals(other.biterm[1]))
					||(biterm[0].equals(other.biterm[1]) && 
							biterm[1].equals(other.biterm[0])));
			
		} else {
			return false;
		}
	}
	
	@Override
	public String toString(){
		return biterm[0] + " " + biterm[1];
	}
}
