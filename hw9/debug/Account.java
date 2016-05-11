package hw09;

import java.io.*;
import java.lang.Thread.*;
import java.util.HashSet;

class TransactionAbortException extends Exception {}
// this is intended to be caught
class TransactionUsageError extends Error {}
// this is intended to be fatal
class InvalidTransactionError extends Error {}
// bad input; will have to skip this transaction

// YOU ARE NOT PERMITTED TO MODIFY class Account!
//
public class Account {
    private int value = 0;
    private Thread writer = null;
    private HashSet<Thread> readers;

    public Account(int initialValue) {
        value = initialValue;
        readers = new HashSet<Thread>();
    }

    private void delay() {
        try {
            Thread.sleep(100);  // ms
        } catch(InterruptedException e) {}
            // Java requires you to catch that
    }

    public int peek() {
        delay();
        Thread self = Thread.currentThread();
        synchronized (this) 
	    {
            if (writer == self || readers.contains(self)) 
		{
                // should do all peeks before opening account
                // (but *can* peek while another thread has open)
		    System.out.println("[Account.peek()][value is: " + value + "[TransactionUsageError] Reason: attempted user of peek is either reader or writer (must use peek bfefore opening)\n"); //bmrk
                throw new TransactionUsageError();
            }
            return value;
        }
    }

    // TO DO: the sequential version does not call this method,
    // but the parallel version will need to.
    //
    public void verify(int expectedValue)
        throws TransactionAbortException 
    {
        delay();
        synchronized (this)
	    {
            if (!readers.contains(Thread.currentThread())) 
		{
		    
		    System.out.println("[Account.verify()][value is: " + value + "[TransactionUsageError] Reason: attempted user of verify is not reader \n"); //bmrk
                throw new TransactionUsageError();
            }
            if (value != expectedValue)
		{
                // somebody else modified value since we used it;
                // will have to retry
                throw new TransactionAbortException();
            }
        }
    }

    public void update(int newValue) {
        delay();
        synchronized (this) {
            if (writer != Thread.currentThread()) {
		System.out.println("[Account.update()][value is: " + value + "[TransactionUsageError] Reason: attempted user of update() is not writer() \n"); //bmrk
                throw new TransactionUsageError();
            }
            value = newValue;
        }
    }

    // TO DO: the sequential version does not open anything for reading
    // (verifying), but the parallel version will need to.
    //
    public void open(boolean forWriting)
        throws TransactionAbortException 
    {
        delay();
        Thread self = Thread.currentThread();
        synchronized (this)
	    {
            if (forWriting)
		{
                if (writer == self)
		    {
			System.out.println("[Account.open()][value is: " + value + "[TransactionUsageError] Reason: attempted user of open()  with purpose of writing is already a writer \n"); //bmrk
                    throw new TransactionUsageError();
		    }
                int numReaders = readers.size();
                if (writer != null || numReaders > 1
                        || (numReaders == 1 && !readers.contains(self)))
		    {
                    // encountered conflict with another transaction;
                    // will have to retry
                    throw new TransactionAbortException();
                }
                writer = self;
            } 
	    else //for reading
		{
                if (readers.contains(self) || (writer == self))
		    {
			if(readers.contains(self))
			    System.out.println("[Account.open()][value is: " + value + "[TransactionUsageError] Reason: attempted user of open()  with purpose of reading is already a reader \n"); //bmrk
			if(writer == self)
			    System.out.println("[Account.open()][value is: " + value + "[TransactionUsageError] Reason: attempted user of open()  with purpose of reading is a writer (forbidden?)\n");
			throw new TransactionUsageError();
		    }
                if (writer != null)
		    {
                    // encountered conflict with another transaction;
                    // will have to retry
			throw new TransactionAbortException();
		    }
                readers.add(Thread.currentThread());
            }
        }
    }

    public void close() {
        delay();
        Thread self = Thread.currentThread();
        synchronized (this) {
            if (writer != self && !readers.contains(self)) {
		System.out.println("[Account.close()][value is: " + value + "[TransactionUsageError] Reason: attempted user of close is not reader or writer \n"); //bmrk
                throw new TransactionUsageError();
            }
            if (writer == self) writer = null;
            if (readers.contains(self)) readers.remove(self);
        }
    }
    
    // The three methods below are unsynchronized 
    // to be used for testing purposes _after_ server has terminated
    
    // print value in wide output field
    public void print() {
        System.out.format("%11d", new Integer(value));
    }

    // print value % numLetters (indirection value) in 2 columns
    public void printMod()
    {
        int val = value % constants.numLetters;
        if (val < 10) System.out.print("0");
        System.out.print(val);
    }
    
    // return Account value
    public int getValue()
    {
        return value;
    }
}

