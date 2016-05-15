package hw09;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// TO DO: Task is currently an ordinary class.
// You will need to modify it to make it a task,
// so it can be given to an Executor thread pool.
//
class Task implements Runnable{
    private static final int A = constants.A;
    private static final int Z = constants.Z;
    private static final int numLetters = constants.numLetters;

    private Account[] accounts; /**Shared Mutable Memory**/
    private Account[] cache = new Account[26]; //Acts as the local memory for threads to do computation on
    private Account[] veracity = new Account[26]; //Holds the original accounts state for verifying purposes
    //private String[] blockRequest = new String[26]; //Keeps track of blocks that will need to be requested
    private int[] readBlocks = new int[26]; //Keeps track of the read blocks
    private int[] writeBlocks = new int[26]; //Keeps track of the write blocks
    private String transaction;

    // TO DO: The sequential version of Task peeks at accounts
    // whenever it needs to get a value, and opens, updates, and closes
    // an account whenever it needs to set a value.  This won't work in
    // the parallel version.  Instead, you'll need to cache values
    // you've read and written, and then, after figuring out everything
    // you want to do, (1) open all accounts you need, for reading,
    // writing, or both, (2) verify all previously peeked-at values,
    // (3) perform all updates, and (4) close all opened accounts.

    public Task(Account[] allAccounts, String trans) {
        accounts = allAccounts;
        transaction = trans;
        for(int i = 0; i < accounts.length; i++){ //Copies the accounts into the threads local memory for computation and verification
        	cache[i] = new Account(accounts[i].getValue());
        	veracity[i] = new Account(accounts[i].getValue());
        }
        for(int i = 0; i < 26; i++){
        	readBlocks[i] = 0;
        	writeBlocks[i] = 0;
        }
    }
    
    // TO DO: parseAccount currently returns a reference to an account.
    // You probably want to change it to return a reference to an
    // account *cache* instead.
    //
    private Account parseAccount(String name) {
        int accountNum = (int) (name.charAt(0)) - (int) 'A';
        if (accountNum < A || accountNum > Z)
            throw new InvalidTransactionError();
        Account a = cache[accountNum];
        for (int i = 1; i < name.length(); i++) {
            if (name.charAt(i) != '*')
                throw new InvalidTransactionError();
            accountNum = (cache[accountNum].peek() % numLetters);
            a = cache[accountNum];
        }
        return a;
    }
    
    private Account parseGlobalAccount(String name) {
        int accountNum = (int) (name.charAt(0)) - (int) 'A';
        if (accountNum < A || accountNum > Z)
            throw new InvalidTransactionError();
        Account a = accounts[accountNum];
        for (int i = 1; i < name.length(); i++) {
            if (name.charAt(i) != '*')
                throw new InvalidTransactionError();
            accountNum = (accounts[accountNum].peek() % numLetters);
            a = accounts[accountNum];
        }
        return a;
    }
    
    private Account parseVerifyAccount(String name) {
        int accountNum = (int) (name.charAt(0)) - (int) 'A';
        if (accountNum < A || accountNum > Z)
            throw new InvalidTransactionError();
        Account a = veracity[accountNum];
        for (int i = 1; i < name.length(); i++) {
            if (name.charAt(i) != '*')
                throw new InvalidTransactionError();
            accountNum = (veracity[accountNum].peek() % numLetters);
            a = veracity[accountNum];
        }
        return a;
    }

    private int parseAccountOrNum(String name) {
        int rtn;
        if (name.charAt(0) >= '0' && name.charAt(0) <= '9') {
            rtn = new Integer(name).intValue();
        } else {
            rtn = parseAccount(name).peek();
        }
        return rtn;
    }

    public void run() {
        // tokenize transaction
        String[] commands = transaction.split(";");

        for (int i = 0; i < commands.length; i++) {
            String[] words = commands[i].trim().split("\\s");
            if (words.length < 3)
                throw new InvalidTransactionError();
            Account lhs = parseAccount(words[0]);
            
            int accNum = (int) (words[0].charAt(0)) - (int) 'A';//************************
            writeBlocks[accNum] = 1;
            
            if (!words[1].equals("="))
                throw new InvalidTransactionError();
            int rhs = parseAccountOrNum(words[2]);
            
            if(!(words[2].charAt(0) >= '0' && words[2].charAt(0) <= '9')){//************************
            	accNum = (int) (words[2].charAt(0)) - (int) 'A';
            	readBlocks[accNum] = 1;
            }
            
            
            for (int j = 3; j < words.length; j+=2) {
                if (words[j].equals("+")){
                    rhs += parseAccountOrNum(words[j+1]);
                    
                    if(!(words[j+1].charAt(0) >= '0' && words[j+1].charAt(0) <= '9')){//************************
                    	accNum = (int) (words[j+1].charAt(0)) - (int) 'A';
                    	readBlocks[accNum] = 1;
                    }
                }
                else if (words[j].equals("-")){
                    rhs -= parseAccountOrNum(words[j+1]);
                    
                    if(!(words[j+1].charAt(0) >= '0' && words[j+1].charAt(0) <= '9')){//************************
                    	accNum = (int) (words[j+1].charAt(0)) - (int) 'A';
                    	readBlocks[accNum] = 1;
                    }
                }
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
        
        String lockState = "";
        for(int i = 0; i < readBlocks.length; i++){//request read blocks
        	lockState = lockState + readBlocks[i] + " ";
        	if(readBlocks[i] == 1){
        		Account acc = parseGlobalAccount("" + (char)(i + 'A'));
        		try {
					acc.open(false);
				} catch (TransactionAbortException e) {
					for(int i1 = 0; i1 < readBlocks.length; i1++){//request block close
			        	lockState = lockState + (readBlocks[i1] + writeBlocks[i1]) + " ";
			        	if(readBlocks[i1] == 1 || writeBlocks[i1] == 1){
			        		Account acc1 = parseGlobalAccount("" + (char)(i1 + 'A'));
			        		acc1.close();
			        		readBlocks[i1] = 0;
			        		writeBlocks[i1] = 0;
			        	}
			        }
					// TODO Auto-generated catch block
					run();
				}
        	}
        }
        //System.out.print("\nRead " + lockState);
        
        
        lockState="";
        for(int i = 0; i < writeBlocks.length; i++){//request write blocks
        	lockState = lockState + writeBlocks[i] + " ";
        	if(writeBlocks[i] == 1){
        		Account acc = parseGlobalAccount("" + (char)(i + 'A'));
        		try {
					acc.open(true);
				} catch (TransactionAbortException e) {
					for(int i1 = 0; i1 < readBlocks.length; i1++){//request block close
			        	lockState = lockState + (readBlocks[i1] + writeBlocks[i1]) + " ";
			        	if(readBlocks[i1] == 1 || writeBlocks[i1] == 1){
			        		Account acc1 = parseGlobalAccount("" + (char)(i1 + 'A'));
			        		acc1.close();
			        		readBlocks[i1] = 0;
			        		writeBlocks[i1] = 0;
			        	}
			        }
					// TODO Auto-generated catch block
					run();
				}
        	}
        }
        //System.out.print("\nWrite " + lockState);
        
        
        lockState = "";
        for(int i = 0; i < readBlocks.length; i++){//verify main account
        	lockState = lockState + readBlocks[i] + " ";
        	if(readBlocks[i] == 1){
        		Account acc = parseGlobalAccount("" + (char)(i + 'A'));
        		Account ver = parseVerifyAccount("" + (char)(i + 'A'));
        		try {
					acc.verify(ver.getValue());
				} catch (TransactionAbortException e) {
					for(int i1 = 0; i1 < readBlocks.length; i1++){//request block close
			        	lockState = lockState + (readBlocks[i1] + writeBlocks[i1]) + " ";
			        	if(readBlocks[i1] == 1 || writeBlocks[i1] == 1){
			        		Account acc1 = parseGlobalAccount("" + (char)(i1 + 'A'));
			        		acc1.close();
			        		readBlocks[i1] = 0;
			        		writeBlocks[i1] = 0;
			        	}
			        }
					// TODO Auto-generated catch block
					run();
				}
        	}
        }
        //System.out.print("\nVerified " + lockState);
        
        
        lockState = "";
        for(int i = 0; i < writeBlocks.length; i++){//verify main account
        	lockState = lockState + writeBlocks[i] + " ";
        	if(writeBlocks[i] == 1){
        		Account acc = parseGlobalAccount("" + (char)(i + 'A'));
        		Account cac = parseAccount("" + (char)(i + 'A'));
        		
				acc.update(cac.getValue());
				
        	}
        }
        //System.out.print("\nUpdated " + lockState);
        
        
        lockState = "";
        for(int i = 0; i < readBlocks.length; i++){//request block close
        	lockState = lockState + (readBlocks[i] + writeBlocks[i]) + " ";
        	if(readBlocks[i] == 1 || writeBlocks[i] == 1){
        		Account acc = parseGlobalAccount("" + (char)(i + 'A'));
        		acc.close();
        		readBlocks[i] = 0;
        		writeBlocks[i] = 0;
        	}
        }
        //System.out.print("\nClose " + lockState);
        
    }
}

public class MultithreadedServer {

	// requires: accounts != null && accounts[i] != null (i.e., accounts are properly initialized)
	// modifies: accounts
	// effects: accounts change according to transactions in inputFile
    public static void runServer(String inputFile, Account accounts[])
        throws IOException {

        // read transactions from input file
        String line;
        BufferedReader input =
            new BufferedReader(new FileReader(inputFile));

        // TO DO: you will need to create an Executor and then modify the
        // following loop to feed tasks to the executor instead of running them
        // directly.  
        
        ExecutorService e = Executors.newCachedThreadPool();

        while ((line = input.readLine()) != null) {
            Task t = new Task(accounts, line);
            //t.run();
            e.execute(t);
        }
        
        input.close();
        try {
			e.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

    }
}
