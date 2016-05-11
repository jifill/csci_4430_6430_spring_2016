package hw09.test;

import hw09.*;

import java.io.*;
import java.lang.Thread.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.*;
import java.util.Random;

import junit.framework.TestCase;

import org.junit.Test;

public class MultithreadedServerTests extends TestCase {
    private static final int A = constants.A;
    private static final int Z = constants.Z;
    private static final int numLetters = constants.numLetters;
    private static Account[] accounts;
            
    protected static void dumpAccounts() {
	    // output values:
	    for (int i = A; i <= Z; i++) {
	       System.out.print("    ");
	       if (i < 10) System.out.print("0");
	       System.out.print(i + " ");
	       System.out.print(new Character((char) (i + 'A')) + ": ");
	       accounts[i].print();
	       System.out.print(" (");
	       accounts[i].printMod();
	       System.out.print(")\n");
	    }
	 }    
     
        

     @Test
	 public void testIncrement() throws IOException {
	
		// initialize accounts 
		accounts = new Account[numLetters];
		for (int i = A; i <= Z; i++) {
			accounts[i] = new Account(Z-i);
		}			 
	

		System.out.println("Initial account status (begin)\n");
		dumpAccounts();//dump
		System.out.println("Initial account status (end)\n");
	
		MultithreadedServer.runServer("src/hw09/data/increment", accounts);
	
		System.out.println("Final account status (begin)\n");
		dumpAccounts();//dump
		System.out.println("Final account status (end)\n");

		// assert correct account values
		for (int i = A; i <= Z; i++) {
			Character c = new Character((char) (i+'A'));
			assertEquals("Account "+c+" differs",Z-i+1,accounts[i].getValue());
		}		

		
	 }


         @Test
	 public void other_tests() throws IOException
    {

	MultithreadedServer.runServer("hw09/data/test_1", accounts);
	MultithreadedServer.runServer("hw09/data/error_test_1", accounts);
	MultithreadedServer.runServer("hw09/data/error_test_2", accounts);
	MultithreadedServer.runServer("hw09/data/error_test_3", accounts);

    }
	 	  	 
	
}
