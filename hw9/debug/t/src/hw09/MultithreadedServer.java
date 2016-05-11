package hw09;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

//import java.io.BufferedReader;
//import java.io.IOException;
import java.io.InputStreamReader;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ExecutorService;
import java.util.Arrays;
//import java.util.concurrent.Executors;

// TO DO: Task is currently an ordinary class.
// You will need to modify it to make it a task,
// so it can be given to an Executor thread pool.
//

//class Task
class Task implements Runnable
{
    private static final int A = constants.A;
    private static final int Z = constants.Z;
    private static final int numLetters = constants.numLetters;

    private Account[] accounts;
    private String transaction;
    private Account[] acc_cache;
    private int[] acc_cache_int;
    private boolean[] verify_cache_status; //will hold true for any account that needs to be verified
    private int[] acc_update_cache; //will hold value of account(s) that need to be written to live mem
    private boolean[] cache_status; //will hold true for any account that needs to be written
    private boolean[] open_read_status;
    private boolean[] open_write_status;

    private int[] lhs_arr;
    private int[] temp_arr;

    // TO DO: The sequential version of Task peeks at accounts
    // whenever it needs to get a value, and opens, updates, and closes
    // an account whenever it needs to set a value.  This won't work in
    // the parallel version.  Instead, you'll need to cache values
    // you've read and written, and then, after figuring out everything
    // you want to do, (1) open all accounts you need, for reading,
    // writing, or both, (2) verify all previously peeked-at values,
    // (3) perform all updates, and (4) close all opened accounts.



    //Step 1: read rhs
    //Step 2: determine accounts needed from rhs
    //Step 3: peak at values in rhs
    //Step 4: store values from rhs in cache
    //Step 5: aquire locks
    //Step 6: compute resulte and store in variable (or update queue?)
    
    //check that cached values are the same as 'live memory'
    //if they are: place result in cache
    //if not

    public Task(Account[] allAccounts, String trans)
    {
        accounts = allAccounts;
        transaction = trans;
	//private Account[] acc_cache;//
	acc_cache = new Account [accounts.length];
	acc_cache_int = new int [accounts.length];
	cache_status = new boolean [accounts.length];
	verify_cache_status = new boolean [accounts.length];
	open_read_status = new boolean [accounts.length];
	open_write_status = new boolean [accounts.length];
	temp_arr = new int[1];
	lhs_arr = new int[1];

	for(int i = 0; i < accounts.length ; i++)
	    {
		//acc_cache[i] = new Account(accounts[i].peek());
		acc_cache_int[i] = accounts[i].peek();
		verify_cache_status[i] = false;
		cache_status[i] = false;
		
		//keep track of open files
		open_read_status[i] = false;
		open_write_status[i] = false;
	    }


    }
    
    // TO DO: parseAccount currently returns a reference to an account.
    // You probably want to change it to return a reference to an
    // account *cache* instead. (?)
    //
    private Account parseAccount(String name, int [] arr)
    {
	//System.out.println("[parseAccount()]\n");
        int accountNum = (int) (name.charAt(0)) - (int) 'A';
        if (accountNum < A || accountNum > Z)
            throw new InvalidTransactionError();
        Account a = accounts[accountNum];
        for (int i = 1; i < name.length(); i++) //if length > 1, this is a dereference?
	    {
            if (name.charAt(i) != '*')
                throw new InvalidTransactionError();
            accountNum = (accounts[accountNum].peek() % numLetters); //next dereference
            a = accounts[accountNum]; 
	    }
	//either open or put value in cache first
	acc_cache_int[accountNum] = a.peek(); //store value in cache
	acc_cache[accountNum] = new Account(acc_cache_int[accountNum]);
	//Account aa = new Account(a.getValue());
	//aa = a;
        //return aa;
	arr[0] = accountNum;
	return acc_cache[accountNum]; //return reference to account, peek will be called on this to get value
    }

    private int parseAccountOrNum(String name, int [] arr)
    {

	//System.out.println("[parseAccountOrNum()]\n");
        int rtn;
        if (name.charAt(0) >= '0' && name.charAt(0) <= '9')
	    {
		rtn = new Integer(name).intValue();
		arr[0] = -1;
	    }
	else
	    {
		rtn = parseAccount(name, arr).peek();
	    }
        return rtn; //return int constant or peeked value
    }

    public void run()
    {
	long thread_id = Thread.currentThread().getId();
	int i =0;
	System.out.println("[THREAD " + thread_id + " ] status: cmd = '" + transaction + "'\n");
	
        // tokenize transaction
        String[] commands = transaction.split(";");

	System.out.println("[THREAD " + thread_id + " ] status: cmd _str = "+ Arrays.toString(commands) + "'\n");
	
        for ( i = 0; i < commands.length; i++) //process each segment of transaction
	    {
            String[] words = commands[i].trim().split("\\s");
	    System.out.println("[THREAD " + thread_id + " ] status: word_str = "+ Arrays.toString(words) + "'\n");
            if (words.length < 3)
                throw new InvalidTransactionError();
	    System.out.println("[THREAD " + thread_id + " ] status: lhs = "+ words[0] + "'\n");

	    lhs_arr[0] = -1;
	    temp_arr[0] = -1;
            Account lhs = parseAccount(words[0], lhs_arr);   
	    
	    

	    //check rest of expression
            if (!words[1].equals("=")) //if '=' is not present, throw error
                throw new InvalidTransactionError();
            int rhs = parseAccountOrNum(words[2],temp_arr);
	    //verify_cache_status[temp_arr[0]] = true;
	    if(temp_arr[0] != -1)
		verify_cache_status[temp_arr[0]] = true;
	    //temp_arr holds number of
            for (int j = 3; j < words.length; j+=2)
		{
		    if (words[j].equals("+")) //compute sum
			{
			rhs += parseAccountOrNum(words[j+1],temp_arr);
			if(temp_arr[0] != -1)
			    verify_cache_status[temp_arr[0]] = true;
			}
		    else if (words[j].equals("-")) //compute difference
			{
			    rhs -= parseAccountOrNum(words[j+1],temp_arr);
			    if(temp_arr[0] != -1)
				verify_cache_status[temp_arr[0]] = true;
			}
		    else
			throw new InvalidTransactionError();
		}
            try
		{
		    //open all necessary indicies //read and write

		    //open files for reading
		    for(int k = 0;k<accounts.length;k++)
			if(verify_cache_status[k]) //if we need to open and veryfy, open here
			    {
			    accounts[k].open(false);
			    open_read_status[k] = true;
			    }

		    accounts[lhs_arr[0]].open(true); // open for reading
		    open_write_status[lhs_arr[0]] = true;
		    //open_read_status[i] = false;
		    //open_write_status[i] = false
		    //verify all indicies of verify_cache_status which are true
		    for(int l = 0;l<accounts.length;l++)
			if(verify_cache_status[i])
			    accounts[l].verify(acc_cache_int[l]); //verify against cache

		    //if all went well
		    //accounts[lhs_arr[0]] = rhs;//set the lhs equal to the rhs

		    System.out.println("[THREAD " + thread_id + " ] status: Trying to upddate = Account "+ ((char)(lhs_arr[0] + 65)) + " to have value " + rhs   + "'\n");
		    //lhs.update(rhs);
		    accounts[lhs_arr[0]].update(rhs);
		    //lhs.close();
		    for(int j = 0;j<accounts.length;j++)
			if(open_read_status[j] || open_write_status[j]) 
			    {
				accounts[j].close(); //removes both readers and writers with one call
				open_read_status[j] = false;
				open_write_status[j] = false;
			    }

		    //verify that nothing has changed
		    //lhs.open(true);
            }
	    catch (TransactionAbortException e) // won't happen in sequential version 
		{
		    /*
		      Thrown by
		      - verify()
		      - open()
		     */

		// free all locks?
		    		    //close all accounts
		    for(int j = 0;j<accounts.length;j++)
			if(open_read_status[j] || open_write_status[j]) 
			    {
				accounts[j].close(); //removes both readers and writers with one call
				open_read_status[j] = false;
				open_write_status[j] = false;
			    }


		}


	    //System.out.println("[THREAD " + thread_id + " ] status: Trying to upddate = Account "+ ((char)(lhs_arr[0] + 65)) + " to have value " + rhs   + "'\n");
	    //lhs.update(rhs);
            //lhs.close();
	    /*
	    //close all accounts
	    for(int j = 0;j<accounts.length;j++)
		if(open_read_status[j] || open_write_status[j]) 
		    {
			accounts[j].close(); //removes both readers and writers with one call
			open_read_status[j] = false;
			open_write_status[j] = false;
		    }
            //lhs.update(rhs);
            //lhs.close();
	    */

	    }
        System.out.println("commit: " + transaction);
    }
}

public class MultithreadedServer {
    
    // requires: accounts != null && accounts[i] != null (i.e., accounts are properly initialized)
    // modifies: accounts
    // effects: accounts change according to transactions in inputFile
    public static void runServer(String inputFile, Account accounts[])
        throws IOException
    {
	System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\n");

        // read transactions from input file
        String line;
        BufferedReader input =
            new BufferedReader(new FileReader(inputFile));

        // TO DO: you will need to create an Executor and then modify the
        // following loop to feed tasks to the executor instead of running them
        // directly.  

	//ThreadPoolExecutor executor = (ThreadPoolExecutor);
	//Executors.newCachedThreadPool();
	ExecutorService executor = Executors.newFixedThreadPool(5);
        while ((line = input.readLine()) != null)
	    {
		//line contains a given transaction
		//Task t = new Task(accounts, line); //initialize task object
		 Task t = new Task(accounts, line); //initialize task object
		 //t.run();
		 executor.execute(t);
	    }

	executor.shutdown();
        //executor.awaitTermination(180,TimeUnit.SECONDS);
        input.close();

    }

    public static void main(String[] args)
    {
	
	//System.out.println("number of arguments is: " + Array.getLength(args));
	int i,len;
	System.out.println("I (main) have been given: "+ args.length + " arguments\n");
	len = args.length;
	for(i = 0;i<len;i++)
	    {
		System.out.println("Arg "+i+" - " + args[i] + "\n");
	    }
	aaa();
	System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\n");

    }
    public static void aaa ()
    {
	
	//System.out.println("In my_lel()\n");
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
