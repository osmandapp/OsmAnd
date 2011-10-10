//FileHdr.java
//  
//This library is free software; you can redistribute it and/or
//modify it under the terms of the GNU Lesser General Public
//License as published by the Free Software Foundation; either
//version 2.1 of the License, or (at your option) any later version.
//  
//This library is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//Lesser General Public License for more details.
package rtree;
//package rtree;
import java.io.*;
import java.util.Vector;
import java.util.Enumeration;
/**
   This class is the handler for the file.
   @author Prachuryya Barua
*/
public class FileHdr
{
  /**------------file header - will always take 1024 bytes------------*/
  /**total no. of nodes in the file*/
  int totalNodes;
  /**the index of the root*/
  long rootIndex;

  /**------------local variables--------------------------------------*/
  protected boolean writeThr = false;//whether write dirt or write through
  /**Overflow limit*/
  int stkLimit;
  /**The stack data. Although the indices are <code>long</code> but they can be easily represented as
     <code>int</code>*/
  private int[] S;
  /**Index of the top most element*/
  private int topIdx;
  private RandomAccessFile file;
  private String fileName;
  private boolean dirty = false;/*Tells whethet this is a dirty filehdr or not*/
  /**If any write thread is interested then increment this. This variable 
     results in the fact that writes will always have the preference. 
     Even when the JVM chooses a READ thread to run, it would have to wait 
     till <b>one</b> of all the waiting WRITE threads run. After one of the WRITE
     thread runs, the situation is open for all. Again, if any of the WRITE
     thread sets <tt>interested</tt>, then on next  <tt>notifyAll</tt> it is
     gauranteed that one of the WRITE threads will run before any other READ.
  */
  private boolean interested;
  /**The wait thread queue*/
  private Vector waiters;
  /**
     Although this 'stack' is part of the file header but it acts totally 
     independently of the rest of the file header. All of the file reads and
     writes are handled by the 'Node' class but the free node list('stack') is
     maintained by this class independently. Therefore all the 'Node' class 
     needs to do is call the 'push' and 'pop' method to read and write to the
     file regarding the free node list.<br>
     Note:- This class will work well with 150 element delets at one go but 
     beyond that it will not maintain the list of nodes that have been
     deleted. This is not fatal but the size of the file will increase and the
     deleted nodes that did not register with the stack will be lost forever.
     This condition can be rectified by calling <tt>Pack.packTree</tt>.
  */
  FileHdr(int stkLimit,String fileName)
    throws RTreeException
  {
    try{
      this.file = new RandomAccessFile(fileName,"rw");
      this.fileName = fileName;
      this.writeThr = false;
      this.stkLimit = stkLimit;
      waiters = new Vector();
      S = new int[this.stkLimit];
      topIdx = -1;
      int frNode;
      writeThr = false;
      //no nodes present
      //if(file.length() <= (Node.FILE_HDR_SIZE+1)){
      if(file.length() <= (Node.INTEGER_SIZE)){
        file.seek(0);
        file.writeInt(0);//total nodes
        file.writeLong(Node.NOT_DEFINED);//file.writeInt(Node.NOT_DEFINED);//original
        file.writeInt(Node.NOT_DEFINED);
        totalNodes = 0;
        rootIndex =  Node.NOT_DEFINED;
      }
      //read stack from the file if stack exists
      else{
        file.seek(0);
        byte[] data = new byte[ Node.FILE_HDR_SIZE ];
        file.read(data);
        DataInputStream ds = new DataInputStream(new ByteArrayInputStream(data));
        totalNodes = ds.readInt();
        rootIndex = ds.readLong();//rootIndex = ds.readInt();
        while((topIdx<stkLimit) &&
              ((frNode = ds.readInt()) != Node.NOT_DEFINED))
          S[++topIdx] = frNode;
        ds.close();
      }
    }
    catch(Exception e){
      throw new RTreeException("FileHdr.FileHdr: " +e.getMessage());
    }
  }
  /**
     This method at the moment is only for Pack. This one has potential!
  */
  public void setBufferPolicy(boolean writeThr)
    throws IOException
  {
    flush();
    this.writeThr = writeThr;
  }

  /**
     This method will be used by the Pack class to update this header from the file again. I assume that
     the object that called this method has write lock with it.
  */
  void update(String fileName)
    throws RTreeException
  {
    try{
      file.close();
      this.file = new RandomAccessFile(fileName,"rw");
      //file.getFD().sync();
      S = new int[this.stkLimit];
      topIdx = -1;
      int frNode;
      dirty = false;
      //no nodes present
      //if(file.length() <= (Node.FILE_HDR_SIZE+1)){
      if(file.length() <= (Node.INTEGER_SIZE)){
        file.seek(0);
        file.writeInt(0);
        file.writeLong(Node.NOT_DEFINED);//file.writeInt(Node.NOT_DEFINED);
        file.writeInt(Node.NOT_DEFINED);
        totalNodes = 0;
        rootIndex =  Node.NOT_DEFINED;
      }
      //read stack from the file if stack exists
      else{
        file.seek(0);
        byte[] data = new byte[Node.FILE_HDR_SIZE];
        file.read(data);
        DataInputStream ds = new DataInputStream(new ByteArrayInputStream(data));
        totalNodes = ds.readInt();
        rootIndex = ds.readLong();//rootIndex = ds.readInt();
        while((topIdx<stkLimit) 
              && ((frNode = ds.readInt()) != Node.NOT_DEFINED))
          S[++topIdx] = frNode;
        ds.close();
      }
    }
    catch(Exception e){
      e.printStackTrace();
      throw new RTreeException("FileHdr.FileHdr: " +e.getMessage());
    }
  }
  /**
     Will set the file header size to zero and remake the file header.
  */
  void resetHeader()
    throws Exception
  {
    S = new int[this.stkLimit];
    topIdx = -1;
    int frNode;
    file.setLength(1);
    file.seek(0);
    file.writeInt(0);
    file.writeLong(Node.NOT_DEFINED);
    file.writeInt(Node.NOT_DEFINED);
    totalNodes = 0;
    rootIndex = Node.NOT_DEFINED;
    dirty = false;
  }
  /**
     Pass the index of the node that needs to be pushed in the stack
  */
  synchronized void push(long lval)
    throws StackOverflowException,IOException
  {
    int val = (int)lval;
    if(topIdx >= (stkLimit-1))
      throw new StackOverflowException("FileHdr.push: Overflow but not fatal");
    //System.out.println("Push called, pushing at S["+(topIdx+1)+"]:"+val);
    S[++topIdx] = val;
    dirty = true;
    if(writeThr){
      file.seek(( Node.INTEGER_SIZE + Node.LONG_SIZE)+( Node.INTEGER_SIZE*(topIdx)));
      file.writeInt(val);
      //signal end of free list
      if(topIdx < ( Node.FREE_LIST_LIMIT-1))
        file.writeInt( Node.NOT_DEFINED);
    }
  }
  synchronized int pop()
    throws StackUnderflowException,IOException
  {
    if(topIdx < 0)
      throw new StackUnderflowException("FileHdr.pop: Underflow");
    //System.out.println("Pop called, returning S["+topIdx+"]:"+S[topIdx]);
    if(writeThr){
      file.seek(( Node.INTEGER_SIZE + Node.LONG_SIZE) + ( Node.INTEGER_SIZE*topIdx));
      file.writeInt( Node.NOT_DEFINED);
    }
    dirty = true;
    return S[topIdx--];
  }
  /**
     returns the size of the stck of free nodes.
  */
  int stackSize()
  {
    return topIdx + 1;
  }
  int peep(int index)
    throws IllegalValueException
  {
    if((index > topIdx) || (index < 0))
      throw new IllegalValueException("FileHdr.peep: Index out of bound");
    return S[index];
  }
  
  private synchronized void writeFileHeader()
    throws IOException
  {
    if(dirty){
      ByteArrayOutputStream bs = new ByteArrayOutputStream(Node.FILE_HDR_SIZE);
      DataOutputStream ds =  new DataOutputStream(bs);
      ds.writeInt(totalNodes);
      ds.writeLong(rootIndex);
      if(topIdx == -1)
        ds.writeInt(Node.NOT_DEFINED);
      else{//write the whole stack
        for(int i=0; i <= topIdx; i++)
          ds.writeInt(S[i]);
        ds.writeInt(Node.NOT_DEFINED);//indicate the end of list
      }//else
      bs.flush();
      ds.flush();
      file.seek(0);
      file.write(bs.toByteArray());
    }
    dirty = false;
  }     
  /**this function writes to file header as well as to  the local variables
     an atomic function.Does not concern itself with the stack info.
  */
  synchronized void writeFileHeader(int totNodes,long rootIdx)
    throws IOException
  {
    if(writeThr){
      ByteArrayOutputStream bs = new ByteArrayOutputStream(Node.INTEGER_SIZE + Node.LONG_SIZE);
      DataOutputStream ds =  new DataOutputStream(bs);
      ds.writeInt(totNodes);
      ds.writeLong(rootIdx);
      bs.flush();
      ds.flush();
      file.seek(0);
      file.write(bs.toByteArray());
      dirty = false;
    }
    dirty = true;
    //update local variables
    totalNodes = totNodes;
    rootIndex = rootIdx;
  }     
  /**
     This method does a file IO. Can we make another method which is not static.
     @return root index for any file.
     @deprecated Use the non static one.
  */
  public static long getRootIndex(String fileName) 
    throws FileNotFoundException
  {
    RandomAccessFile fl = new RandomAccessFile(fileName,"r");
    try{
      if (fl.length() == 0)//new file
        throw new FileNotFoundException("Node.getRootIndex : File not found");
      fl.seek( Node.INTEGER_SIZE );
      long rootIndx = fl.readLong();
      fl.close();
      return rootIndx;
    }
    catch(IOException e){
      System.out.println("Node.getRootIndex: Couldn't get root index"); 
      return  Node.NOT_DEFINED;
    }
  }
  /**
     Returns the <code>RandomAccessFile</code> object
  */
  public RandomAccessFile getFile()
  {
    return this.file;
  }
  /**
     Will return the total nodes in the tree. This does not include the nodes that are deleted and are 
     in the stack.
  */
  public int getTotalNodes()
  {
    if(topIdx < 0)
      return totalNodes;
    else
      return totalNodes - topIdx;
  }
  public long getRootIndex()
  {
    return rootIndex;
  }
  @Override
protected void finalize() throws Throwable 
  {
    try {
      flush();
      file.close();
    }catch (Exception e) {
      System.err.println(fileName);
      e.printStackTrace();
    }
  }
  /**
     Will flush the file header if it is dirty. It will <b>not</b> flush the individual nodes at it it not
     its responsiblity.
  */
  void flush()
    throws IOException
  {
    if(dirty && !writeThr){
      writeFileHeader();
      dirty = false;
    }
  }
  public boolean isWriteThr()
  {
    return writeThr;
  }
  void setDirty(boolean val)
  {
    this.dirty = val;
  }
  //-------------The following code is added by Ketan ...replacing my code!!!!------------------

  /**retuns the index of the first WRITE thread in the queue*/
  private int firstWriter()
  {
    Enumeration e=waiters.elements();
        
    for(int index=0;e.hasMoreElements();index++)
      {
        ThreadInfo threadinfo = (ThreadInfo) e.nextElement();
        if(threadinfo.lockType == Node.WRITE)
          return index;
      }
    return Integer.MAX_VALUE;
  }
  private int getIndex(Thread t)
  {
    Enumeration e=waiters.elements();
        
    /**  If thread is in the vector then 
     *          return it's index 
     *    else 
     *          return -1
     */
    for(int index=0;e.hasMoreElements();index++)
      {
        ThreadInfo threadinfo = (ThreadInfo) e.nextElement();
        /**  If Thread is already in the vector then 
         *   return it's Index
         */
        if(threadinfo.t == t)
          {
            return index;
          }
      }
    return -1;
  }
    
  public synchronized void lockRead()
  {
    ThreadInfo threadinfo;
    Thread me = Thread.currentThread();
    int index = getIndex(me);
    /** if index = -1 then
        the thread is not in the Vector, so create a new ThreadInfo
        and add it to the vector
        else
        thread is in the queue and get the index of the thread
    */
    if(index == -1)
      {
        threadinfo = new ThreadInfo(me,Node.READ);
        waiters.addElement(threadinfo);
      }
    else
      {
        threadinfo = (ThreadInfo) waiters.elementAt(index);
      }
        
    /**  If the currentThread has come after a Write Thread then
     *   make it wait() until WRITE thread is serviced
     */
    while(getIndex(me) >= firstWriter())
      {
        try
          {
            wait();
          }catch(Exception e){}
      }
    /**
     *  increase the no. of locks the threadinfo has acquired
     */
    threadinfo.nAcquired++;
    //System.out.println("FileHdr.lockRead : read locked for thread " + Thread.currentThread());
    //+" when "+this.toString());
  }
    
  public synchronized void lockWrite() throws IllegalArgumentException
  {
    ThreadInfo threadinfo;
    Thread me= Thread.currentThread();
    int index = getIndex(me);
    /** If the thread is not in the Vector then
        create a new ThreadInfo with WRITE status and add it to the Vector
        else
        get the Index for the thread from the Vector
    */
    if(index==-1)
      {
        threadinfo = new ThreadInfo(me,Node.WRITE);
        waiters.addElement(threadinfo);
      }
    else
      {
        //System.out.println("getIndex = " +getIndex(me));
        threadinfo = (ThreadInfo) waiters.elementAt(index);
        //if(threadinfo.lockType==Node.READ)
        //threadinfo.lockType = Node.WRITE;
      }
    while(getIndex(me)!=0)
      {
        try
          {
            wait();
          }catch(Exception e){}
      }
    threadinfo.nAcquired++;
    //System.out.println("FileHdr.lockWrite : write locked for thread " + Thread.currentThread());
  }
  public synchronized void unlock() throws IllegalArgumentException
  {
    ThreadInfo threadinfo;
    Thread me = Thread.currentThread();
    int index = getIndex(me);
        
    /** if the index is greater than first WRITE thread then
     *          lock  is not held by the thread so throw Exception
     else
         
    */
    if(index > firstWriter())
      throw new IllegalArgumentException("FileHdr.unlock: Lock not Held for the thread");
        
    threadinfo = (ThreadInfo) waiters.elementAt(index);
    threadinfo.nAcquired--;
        
    if(threadinfo.nAcquired==0)
      {
        waiters.removeElementAt(index);
        if(waiters.size()>0){
          //System.out.println("FileHdr.unlock : notifiying");
          notifyAll();
        }
      }
    //System.out.println("FileHdr.unlock : unlocking for thread " + Thread.currentThread());
  }
  /**
     This method will return only internal varaibles.
  */
  @Override
public String toString()
  {
    try{
      String str = new String();
      str += "\nTotal Nodes " +  totalNodes;
      str += "\nRoot Index " +  rootIndex;
      str += "\nFile length " + file.length();
      if(waiters != null){
        str += "\nWaiters : total " + waiters.size();
        for(int i=0; i<waiters.size();i++)
          str += "\n" + i + " : " + waiters.get(i).toString();
      }
      return str;
    }catch(Exception e){
      e.printStackTrace();
      return null;
    }
  }
  Vector getWaiters()
  {
    return waiters;
  }
  synchronized void setWaiters(Vector wtrs)
  {
    waiters = wtrs;
  }
}


/** 
 *   The class helps to store the details of a thread, like lockType
 */
class ThreadInfo
{
  int lockType;
  int nAcquired=0;
  Thread t;

  ThreadInfo(Thread t,int lockType)
  {
    this.t = t;
    this.lockType = lockType;
  }
  @Override
public String toString()
  {
    String str = new String("\nThreadInfo");
    str += "\n lockType : "+ lockType;
    str += "\n nAcquired : "+ nAcquired;    
    str += "\n Thread : "+ t;    
    return str;
  }
}



