package hw09;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Executor;
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

    private Account[] accounts; /**shared mutable state **/
    private Account[] cache = new Account[26]; //Acts as the local memory for threads to do computation on
    private Account[] veracity = new Account[26]; //Holds the original accounts state for verifying purposes
    private String[] blockRequest = new String[26]; //Keeps track of blocks that will need to be requested
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
        Account a = accounts[accountNum];
        for (int i = 1; i < name.length(); i++) {
            if (name.charAt(i) != '*')
                throw new InvalidTransactionError();
            accountNum = (accounts[accountNum].peek() % numLetters);
            a = accounts[accountNum];
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
        int blocks = 0; //Keeps track of current block slot being kept track of

        for (int i = 0; i < commands.length; i++) {
            String[] words = commands[i].trim().split("\\s");
            if (words.length < 3)
                throw new InvalidTransactionError();
            Account lhs = parseAccount(words[0]);
            blockRequest[blocks] = words[0]+"W"; //Collects the write block for later
            blocks++; //Keeps track of how many blocks being used.
            if (!words[1].equals("="))
                throw new InvalidTransactionError();
            int rhs = parseAccountOrNum(words[2]);
            if(!(words[2].charAt(0) >= '0' && words[2].charAt(0) <= '9')){ //Collects the read block for later
            	blockRequest[blocks] = words[2]+"R";
            	blocks++;
        	}
            for (int j = 3; j < words.length; j+=2) {
                if (words[j].equals("+")){
                    rhs += parseAccountOrNum(words[j+1]);
                    if(!(words[j+1].charAt(0) >= '0' && words[j+1].charAt(0) <= '9')){ //Collects the read block for later
                    	blockRequest[blocks] = words[j+1]+"R";
                    	blocks++;
                	}
            	}
                else if (words[j].equals("-")){
                    rhs -= parseAccountOrNum(words[j+1]);
                    if(!(words[j+1].charAt(0) >= '0' && words[j+1].charAt(0) <= '9')){//Collects the read block for later
                    	blockRequest[blocks] = words[j+1]+"R";
                    	blocks++;
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
        
        
        
        for(int i = 0; i < blockRequest.length; i++){ //Delete duplicate block request
        	for(int j = i+1; j < blockRequest.length; j++){
        		boolean once = true;
        		if(blockRequest[i] != null && blockRequest[j] != null && (blockRequest[i].charAt(0) == blockRequest[j].charAt(0)) && blockRequest[i].charAt(1) == 'W' && blockRequest[j].charAt(1) == 'R' && once){
        			String temp = blockRequest[j];
        			blockRequest[j] = blockRequest[i];
        			blockRequest[i] = temp;
        			once = false;
        		}
        		if(blockRequest[i] != null && blockRequest[j] != null && (blockRequest[i].charAt(0) == blockRequest[j].charAt(0)) && blockRequest[j].charAt(1) == 'R'){
        			blockRequest[j] = null;
        		}
        		if(blockRequest[i] != null && blockRequest[j] != null && blockRequest[i].compareTo(blockRequest[j]) == 0 && blockRequest[i].charAt(1) == 'W'){
        			blockRequest[j] = null;
        		}
        	}
        }
        
        for(int i = 0; i < blockRequest.length; i++){ //Request all blocks
        	if(blockRequest[i] != null){

	        	//Arrays.sort(blockRequest);
	        	Account acc = parseGlobalAccount(blockRequest[i].substring(0, 1));
	        	if(blockRequest[i].charAt(1) == 'W'){
	        		try {
	                    acc.open(true);
	                    System.out.println(blockRequest[i] + " is open");
	                } catch (TransactionAbortException e) {
	                	for(int i1 = 0; i1 < accounts.length; i1++){ //Copies the accounts into the threads local memory for computation and verification
	                    	cache[i1] = new Account(accounts[i1].getValue());
	                    	veracity[i1] = new Account(accounts[i1].getValue());
	                    }
	                	for(int i1 = 0; i1 < blockRequest.length; i1++){ //Closes global accounts and frees blocks
	                    	for(int j = i1+1; j < blockRequest.length; j++){
	                    		if(blockRequest[i1] != null && blockRequest[j] != null){
	                    			
	                    			if(blockRequest[i1].charAt(0) == blockRequest[j].charAt(0)){
	                    				
	                    				blockRequest[j] = null;
	                    				
	                    			}
	                    		
	                    		}
	                    		
	                    	}
	                    	if(blockRequest[i1] != null){
	            	        	Account acc1 = parseGlobalAccount(blockRequest[i1].substring(0, 1));
	            	        	System.out.println(blockRequest[i1] + " is closed");
	            				acc1.close();
	            				for(int k = 0; k < blockRequest.length; k++){
	            	        		if(blockRequest[k] != null && blockRequest[i1] != null && blockRequest[k].charAt(0) == blockRequest[i1].charAt(0)){
	            	        			blockRequest[k] = null;
	            	        		}
	            	        	}
	            	        	blockRequest[i1] = null;
	                    	}
	                    	
	                    }
	                    run();
	                }
	        	}else{
	        		try {
	                    acc.open(false);
	                    System.out.println(blockRequest[i] + " is open");
	                } catch (TransactionAbortException e) {
	                	for(int i1 = 0; i1 < accounts.length; i1++){ //Copies the accounts into the threads local memory for computation and verification
	                    	cache[i1] = new Account(accounts[i1].getValue());
	                    	veracity[i1] = new Account(accounts[i1].getValue());
	                    }
	                	for(int i1 = 0; i1 < blockRequest.length; i1++){ //Closes global accounts and frees blocks
	                    	for(int j = i1+1; j < blockRequest.length; j++){
	                    		if(blockRequest[i1] != null && blockRequest[j] != null){
	                    			
	                    			if(blockRequest[i1].charAt(0) == blockRequest[j].charAt(0)){
	                    				
	                    				blockRequest[j] = null;
	                    				
	                    			}
	                    		
	                    		}
	                    		
	                    	}
	                    	if(blockRequest[i1] != null){
	            	        	Account acc1 = parseGlobalAccount(blockRequest[i1].substring(0, 1));
	            	        	System.out.println(blockRequest[i1] + " is closed");
	            				acc1.close();
            					for(int k = 0; k < blockRequest.length; k++){
	            	        		if(blockRequest[k] != null && blockRequest[i1] != null && blockRequest[k].charAt(0) == blockRequest[i1].charAt(0)){
	            	        			blockRequest[k] = null;
	            	        		}
	            	        	}
	            	        	blockRequest[i1] = null;
	            	        	
	                    	}
	                    	
	                    }
	                    run();
	                }
	        	}
        	
        	}
        	
        }
        
        for(int i = 0; i < blockRequest.length; i++){ //Verify that accounts have not been changed
        	if(blockRequest[i] != null && blockRequest[i].charAt(1) != 'W'){
	        	
	        	Account acc = parseGlobalAccount(blockRequest[i].substring(0, 1));
	        	
	        	Account veri = parseVerifyAccount(blockRequest[i].substring(0, 1));
	        	try {
	        		System.out.println(blockRequest[i] + " is verified");
					acc.verify(veri.getValue());
				} catch (TransactionAbortException e) {
					// TODO Auto-generated catch block
					for(int i1 = 0; i1 < accounts.length; i1++){ //Copies the accounts into the threads local memory for computation and verification
			        	cache[i1] = new Account(accounts[i1].getValue());
			        	veracity[i1] = new Account(accounts[i1].getValue());
			        }
					for(int i1 = 0; i1 < blockRequest.length; i1++){ //Closes global accounts and frees blocks
			        	for(int j = i1+1; j < blockRequest.length; j++){
			        		if(blockRequest[i1] != null && blockRequest[j] != null){
			        			
			        			if(blockRequest[i1].charAt(0) == blockRequest[j].charAt(0)){
			        				
			        				blockRequest[j] = null;
			        				
			        			}
			        		
			        		}
			        		
			        	}
			        	if(blockRequest[i1] != null){
				        	Account acc1 = parseGlobalAccount(blockRequest[i1].substring(0, 1));
							acc1.close();
							System.out.println(blockRequest[i1] + " is closed");
							for(int k = i1; k < blockRequest.length; k++){
            	        		if(blockRequest[k].charAt(0) == blockRequest[i1].charAt(0)){
            	        			blockRequest[k] = null;
            	        		}
            	        	}
				        	blockRequest[i1] = null;
			        	}
			        	
			        }
					run();
				}
        	}
        	
        }
        
        for(int i = 0; i < blockRequest.length; i++){ //Push changes to global Accounts
        	if(blockRequest[i] != null){
        		
        		if(blockRequest[i].charAt(1) == 'W'){
        			
        			Account acc = parseGlobalAccount(blockRequest[i].substring(0, 1));
        			Account cacheAcc = parseAccount(blockRequest[i].substring(0, 1));;
        			acc.update(cacheAcc.getValue());
        			System.out.println(blockRequest[i] + " was updated");
        			
        		}
        		
        	}
        	
        }
        
        for(int i = 0; i < blockRequest.length; i++){ //Closes global accounts and frees blocks
        	for(int j = i+1; j < blockRequest.length; j++){
        		if(blockRequest[i] != null && blockRequest[j] != null){
        			
        			if(blockRequest[i].charAt(0) == blockRequest[j].charAt(0)){
        				
        				blockRequest[j] = null;
        				
        			}
        		
        		}
        		
        	}
        	if(blockRequest[i] != null){
	        	Account acc = parseGlobalAccount(blockRequest[i].substring(0, 1));
				acc.close();
				System.out.println(blockRequest[i] + " is closed");
	        	blockRequest[i] = null;
        	}
        	
        }
        
    }
    
}

public class MultithreadedServerOLD {

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
