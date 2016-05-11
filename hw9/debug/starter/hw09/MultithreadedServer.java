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
    // TO DO: The sequential version of Task peeks at accounts
    // whenever it needs to get a value, and opens, updates, and closes
    // an account whenever it needs to set a value.  This won't work in
    // the parallel version.  Instead, you'll need to cache values
    // you've read and written, and then, after figuring out everything
    // you want to do, (1) open all accounts you need, for reading,
    // writing, or both, (2) verify all previously peeked-at values,
    // (3) perform all updates, and (4) close all opened accounts.

    public Task(Account[] allAccounts, String trans)
    {
        accounts = allAccounts;
        transaction = trans;
	//private Account[] acc_cache;//
	acc_cache = new Account [accounts.length];
	for(int i = 0; i < accounts.length ; i++)
	    {
		acc_cache[i] = new Account(accounts[i].peek());
	    }


    }
    
    // TO DO: parseAccount currently returns a reference to an account.
    // You probably want to change it to return a reference to an
    // account *cache* instead.
    //
    private Account parseAccount(String name)
    {
	//System.out.println("[parseAccount()]\n");
        int accountNum = (int) (name.charAt(0)) - (int) 'A';
        if (accountNum < A || accountNum > Z)
            throw new InvalidTransactionError();
        Account a = accounts[accountNum];
        for (int i = 1; i < name.length(); i++)
	    {
            if (name.charAt(i) != '*')
                throw new InvalidTransactionError();
            accountNum = (accounts[accountNum].peek() % numLetters);
            a = accounts[accountNum];
        }
	Account aa = new Account(a.getValue());
	//aa = a;
        return aa;
    }

    private int parseAccountOrNum(String name)
    {

	//System.out.println("[parseAccountOrNum()]\n");
        int rtn;
        if (name.charAt(0) >= '0' && name.charAt(0) <= '9')
	    {
		rtn = new Integer(name).intValue();
	    }
	else
	    {
		rtn = parseAccount(name).peek();
	    }
        return rtn;
    }

    public void run()
    {
	long thread_id = Thread.currentThread().getId();

	System.out.println("[THREAD " + thread_id + " ] status: cmd = '" + transaction + "'\n");
	
        // tokenize transaction
        String[] commands = transaction.split(";");

	System.out.println("[THREAD " + thread_id + " ] status: cmd _str = "+ Arrays.toString(commands) + "'\n");

        for (int i = 0; i < commands.length; i++) //process each segment of transaction
	    {
            String[] words = commands[i].trim().split("\\s");
	    System.out.println("[THREAD " + thread_id + " ] status: word_str = "+ Arrays.toString(words) + "'\n");
            if (words.length < 3)
                throw new InvalidTransactionError();
	    System.out.println("[THREAD " + thread_id + " ] status: lhs = "+ words[0] + "'\n");

            Account lhs = parseAccount(words[0]);

	    //check rest of expression
            if (!words[1].equals("=")) //if '=' is not present, throw error
                throw new InvalidTransactionError();
            int rhs = parseAccountOrNum(words[2]);
            for (int j = 3; j < words.length; j+=2)
		{
		    if (words[j].equals("+")) //compute sum
			rhs += parseAccountOrNum(words[j+1]);
		    else if (words[j].equals("-")) //compute difference
			rhs -= parseAccountOrNum(words[j+1]);
		    else
			throw new InvalidTransactionError();
            }
            try {
                lhs.open(true);
            } catch (TransactionAbortException e) {
                // won't happen in sequential version
            }
            lhs.update(rhs);
            lhs.close();
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
