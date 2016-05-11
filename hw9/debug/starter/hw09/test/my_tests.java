package hw09.test;

import hw09.*;

import java.io.*;
import java.lang.Thread.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.*;
import java.util.Random;

//import junit.framework.TestCase;

//import org.junit.Test;

public class my_tests
{
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
     
        

	 public static void testIncrement() throws IOException
 {
	
		// initialize accounts 
		accounts = new Account[numLetters];
		for (int i = A; i <= Z; i++) {
			accounts[i] = new Account(Z-i);

		}	
		 
		char ch = 'A';
		int f = (int)ch;
		System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\n");
		System.out.println("f =" + f + " \n");
		//MultithreadedServer.aaa();
		//MultithreadedServer.runServer("src/hw09/data/increment", accounts);
		//MultithreadedServer.runServer("hw09/data/increment", accounts);
		MultithreadedServer.runServer("hw09/data/i", accounts);
		System.out.println("bbbbbbbbbbbbbbbbbbbbbbbb\n");
		// assert correct account values
		/*
		for (int i = A; i <= Z; i++) {
			Character c = new Character((char) (i+'A'));
			assertEquals("Account "+c+" differs",Z-i+1,accounts[i].getValue());
		}
		*/

	 }

    public static void main(String[] args)
    {

	System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\n");
	try
	    {
	testIncrement();

	    }
	catch(IOException e)
	    {
	    e.printStackTrace();
	}
    }
	 	  	 
	
}
