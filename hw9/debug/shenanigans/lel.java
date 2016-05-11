
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

//public class printStdIn{
public class lel{

	public static void main (String args[]) {
	    int i,len;
	    System.out.println("I (main) have been given: "+ args.length + " arguments\n");
	    len = args.length;
	    for(i = 0;i<len;i++)
		{
		    System.out.println("Arg "+i+" - " + args[i] + "\n");
		}

	     my_lel();
	}
public static void my_lel ()
 {
	
     System.out.println("In my_lel()\n");
	try{
		BufferedReader br = 
                      new BufferedReader(new InputStreamReader(System.in));
			
		String input;
			
		while((input=br.readLine())!=null){
			System.out.println(input);
		}
			
	}catch(IOException io){
		io.printStackTrace();
	}	
  }

}
