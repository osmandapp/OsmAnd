//Node.java
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

/**
   This class will contain one node at a time in memory.
   This class along with FileHdr are the only two classes that handle the 
   rtree file. No other class handles the rtree file. Therefore be very careful
   when modifying this class, you may corrupt the file in the process.
   </p><b>CHANGELOG</b> See the projects/ChangeLog file
   
   @author Prachuryya Barua
   @version Node.NOT_DEFINED
*/

public class Node implements Cloneable //can be made abstract if leaf and non leaf required
{
  /**max no. of entries in a node*/
//  public final static int MAX = 169;//84;//101; //50;//testing 3
  public final static int MAX = 40;//84;//101; //50;//testing 3
  /**min. no. of entries in a node*/
//  public final static int MIN= 84;//51; //25;//testing 2
  public final static int MIN= 20;//51; //25;//testing 2
  /**The size of the cache.<br>
     Minimum cache size is 50% of total no. of elements (1lakh records has 597 nodes).
     <br>Maximum cache size should be 70%, beyound that there may not be major improvements but the
     overheads will increase.
     <br>Eg: 1 lakh packed records - Total nodes in tree - 597: Height 3, 1 root,4 nonleaf, 129 leaf
     and should have a cache size of 250(sufficient for normal casses) to 400(good for queries). 
     <br>Multiply the cache size by 4kbytes and you get the cache size in MBytes.
     <br><b>These observations are for a packed tree only. An unpacked tree
     needs a 100% buffer size - thererore don't use unpacked tree.</b>
     <br>For unpacked tree
     <br>1 lakh records - Total nodes in tree - 991( Height 3: 1 root, 10 NonLeaf, 981 Leaf

     These observations shold hold good for query building as well. But it is no harm to give large caches
     for query building.
  */
  public final static int CACHE_SIZE = 250;
  /**bytes*/
  final static int NODE_HDR_SIZE = 20;//16;
  /**2 kBytes - will include the file header and the stack*/
  final static int FILE_HDR_SIZE = 4096;//2048;//1024;
  /** 2 kByte*/
  final static int NODE_SIZE = 4096;//2048;//1024;
  /**2048-16=2032 then 2048-20=2028 NOW 4096-20=4076*/
  final static int NODE_BODY_SIZE = 4076;//2028;//2032;//1008;
  /**can be increased by increasing file header size*/
  final static int FREE_LIST_LIMIT = 1020;//509;//5102k;//2541k;
  /**So that I don't forget*/
  final static int INTEGER_SIZE = 4;
  /**So that I don't forget*/
  final static int LONG_SIZE = 8;
  /**node elements type - LEAF*/
  public final static int LEAF_NODE = 1;
  /**node elements type - NON LEAF*/
  public final static int NONLEAF_NODE = 2;
  /**node elements type - NONE*/
  public final static int NOT_DEFINED = -999;
  public final static long NOT_DEFINED_LONG = -999;
  /**for thread which reads the tree*/
  final static int READ = 0;
  /**for threads which writes the reads*/
  final static int WRITE = 1;
  /**No threads reading or writing*/
  final static int NONE = 2;
    
  /**-------------local variables-----------------------*/
  protected RandomAccessFile file;
  protected String fileName;
  protected boolean dirty = false;/*This flags keeps track of the changes made*/
  /**will contain the index of the node in the file*/
  protected long nodeIndex;
  /**a flag to see whether a node is empty*/
  //protected boolean isNodeEmpty;
  protected boolean sorted;
  /**will contain all the elements.*/
  protected Element[] elements;
  /**the file header and the free nodes stack*/
  protected FileHdr fileHdr;
  /**The cached Node MBR... no loops over the node Elements*/
  protected Rect nodeMBR;//remove
  /**-------------------Node header and body-----------------------*/
  /**total no. of elements*/
  protected int totalElements;
  /**parent index - root's parent is not defined*/
  protected long parent;
  /**size of the element*/
  protected int elementSize;
  /**type of the elements in the node*/
  protected int elementType;
  /**---------------Element structure------------*/
  /**
   *minX
   *minY
   *maxX
   *maxY
   *pointer
   */
  
  /**This is only for subclasses*/
  protected Node(){}

  /**for a new node
     Remember if the file is new this constructor will overwrite the 
     <code>parent</code> parameter with 'NOT_DEFINED' as it will be the root node.
     If the <code>prnt</code> parameter is given as NOT_DEFINED then it is understood
     that a new root is required.The file will then have a new root.
  */
  protected Node(RandomAccessFile file,String fileName, long prnt,int elmtType, FileHdr flHdr)
    throws IOException, NodeWriteException
  {
    this.file = file;
    fileHdr = flHdr;
    //initialise local variables
    this.fileName = fileName;
    elements = new Element[MAX];
    nodeMBR = new Rect();//remove
    int size;//size of the element type
    /* Though there is not much difference between LeafElement and NonLeafElement but we still use
       the if condition as there may sometime in the future come a case where they differ.
       But again then again to incorporate such change we must make MAX dynamic with the type of the element
       we have.
    */
    if(elmtType == NONLEAF_NODE) 
      size = NonLeafElement.sizeInBytes();
    else
      size = LeafElement.sizeInBytes();
    try{
      /*a new file with an empty root node*/
      //if((file.length() <= (FILE_HDR_SIZE+2))){//no nodes written
      if(fileHdr.getRootIndex() == NOT_DEFINED){//no nodes written
        //writing the file header
        fileHdr.writeFileHeader(1,0);
        //local variables
        nodeIndex = 0;//this is root - though empty
        //isNodeEmpty = true;
        //write node header
        writeNodeHeader(fileHdr.rootIndex,0,NOT_DEFINED,size,elmtType);
      }
      /*for an existing file with a new node*/
      else{
        //see if any free node exists
        try{
          nodeIndex = fileHdr.pop();
        }
        catch(StackUnderflowException e){//else
          nodeIndex = fileHdr.totalNodes++;//new node index
        }
        //local variables
        //isNodeEmpty = true;
        if(prnt == NOT_DEFINED)/*new Node is the root node.*/
          fileHdr.writeFileHeader(fileHdr.totalNodes,nodeIndex);
        else/*new node is any other node*/
          fileHdr.writeFileHeader(fileHdr.totalNodes,fileHdr.rootIndex);
        //write the node
        writeNodeHeader(nodeIndex,0,prnt,size,elmtType);
      }
            
      return;
    } 
    catch(IOException e){
      throw new IOException("Node.Node(new) : " + e.getMessage());
    }
  }
  /**
     Reading existing nodes. But if this a new file then it will create the 
     file and make a new root node.
  */
  protected Node(RandomAccessFile file,String fileName,long ndIndex,FileHdr flHdr)
    throws FileNotFoundException,IOException,NodeReadException, NodeWriteException
  {
    //check whether file is new or old
    //see whether the user must be specified or not if it is a new file
    fileHdr = flHdr;
    this.file = file;
    this.fileName = fileName;
    elements = new Element[MAX];
    nodeMBR = new Rect();//remove
    //a new file with an empty root node
    //if(file.length() <= (FILE_HDR_SIZE+2)){//new file with no nodes
    if(fileHdr.getRootIndex() == NOT_DEFINED){//no nodes written
      try{
        //write file header
        fileHdr.writeFileHeader(1,0);
        //local variables
        nodeIndex = 0;//this is root - though empty
        //isNodeEmpty = true;
        //write node header
        writeNodeHeader(fileHdr.rootIndex, 0, NOT_DEFINED, LeafElement.sizeInBytes(), LEAF_NODE);
      }
      catch(IOException e){
        throw new IOException("Node.constructor : Can't write to fileHeader and/or node " + e.getMessage());
      }
    }
    //if an old file with an existing node
    else{
      //if out of bond index
      if((FILE_HDR_SIZE+(NODE_SIZE*ndIndex)) > file.length())
        throw new NodeReadException("Node.Node.: nodeIndex is out of bound");
      //update the local variable
      this.nodeIndex = ndIndex;
      //read all the values into local variables from the node
      refreshNode();
    }
  }
    
  /**
     A stupid internal constructor for the clone method.
  */
  protected Node(RandomAccessFile file,String fileName,long nodeIndex, boolean sorted,
                 Element[] elmts, FileHdr fileHdr, int totalElements,long parent,int elmtSize,
                 int elmtType, boolean dirty, Rect nodeMBR)//remove
  {
    try{
      //Integer intVal;
      this.file = file;
      this.dirty = dirty;
      this.fileName = new String(fileName.toCharArray());
      this.nodeIndex = nodeIndex;
      //this.isNodeEmpty = isNodeEmpty;
      this.sorted = sorted;
      this.nodeMBR = new Rect(nodeMBR);//remove
      this.elements = new Element[elmts.length];
      for(int i=0; i<elmts.length; i++){
        if(elmts[i] != null){
          if(elmtType == LEAF_NODE)
            this.elements[i] = new LeafElement(new Rect(elmts[i].getRect()), elmts[i].getPtr());
          else
            this.elements[i] = new NonLeafElement(new Rect(elmts[i].getRect()), elmts[i].getPtr());
        }//if
      }//for
      this.fileHdr = fileHdr;
      this.totalElements = totalElements;
      this.parent = parent;
      this.elementSize = elmtSize;
      this.elementType = elmtType;
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }
  public Object clone()
  {
    return new Node(file,fileName,nodeIndex, sorted, elements, fileHdr,totalElements,
                    parent, elementSize, elementType, dirty, nodeMBR);//remove
  }

  /**
     read an element from the hard disk. Not used any more
     @deprecated In-fact it has never been used except in early development days.
  */
  private Element readElement(long index) throws NodeReadException
  {
    if((index < 0)||(index > (MAX-1)) || (totalElements > index+1 ))
      throw new NodeReadException("Node.readElement: Index value not correct");
    try
      {
        int minX,minY,maxX,maxY;
        //create a buffer
        byte[] data = new byte[NODE_SIZE];
        seekCurrNode();
        file.read(data);
        DataInputStream ds = new DataInputStream(new ByteArrayInputStream(data));
        //skip the header - check for error value
        int skipValue = NODE_HDR_SIZE + (elementSize * (int)index);
        if(ds.skipBytes(skipValue) != skipValue)
          throw new NodeReadException("Can't read buffer: Header or index wrong");
        //read the points
        minX = ds.readInt();
        minY = ds.readInt();
        maxX = ds.readInt();
        maxY = ds.readInt();
        Rect Rectangle = new Rect(minX,minY,maxX,maxY);
        if(elementType == LEAF_NODE){
          long ptr = ds.readLong();
          return(new LeafElement(Rectangle, ptr));
        }
        //if non leaf type then...
        long nodePtr = ds.readLong();
        return(new NonLeafElement(Rectangle, nodePtr));
      }
    catch(Exception e){
      throw new NodeReadException("Node.readElement: " +e.getMessage());
    }
  }
  /**
     This method delets the element with the given index from the node.
     It rewrites the node.
     This method now also being used to write the whole node to the file.
     @param index The element to delete. Give -1 if the whole node is to be flushed.
     @param force Whether to force IO. As this method is also used to write the whole node, this was 
     required.
     @return thengaa!
     XXX : This is till not correct.
  */
  public void deleteElement(int index, boolean force)
    throws IllegalValueException, NodeWriteException
  {
    if((index > (totalElements-1)))
      throw new IllegalValueException("Node.deleteElement: index out of bound");
    if(fileHdr.isWriteThr())
      RTree.chdNodes.remove(fileName,nodeIndex);
    int j = -1;
    try{
      nodeMBR = new Rect();//remove
      ByteArrayOutputStream bs = null;
      DataOutputStream ds = null;
      if(fileHdr.isWriteThr() || force){
        bs = new ByteArrayOutputStream(NODE_SIZE);
        ds =  new DataOutputStream(bs);
        if(index < 0)
          ds.writeInt(totalElements);
        else
          ds.writeInt(totalElements - 1);
        ds.writeLong(parent);
        ds.writeInt(elementSize);
        ds.writeInt(elementType);
      }
      for(int i=0; i<totalElements; i++){
        if(i != index){
          nodeMBR.expandToInclude(elements[i].getRect());//update the local variable as well - remove
          if(fileHdr.isWriteThr() || force){//same condition of buffer policy again, we wanted the loop
            ds.writeInt(elements[i].getRect().getMinX());
            ds.writeInt(elements[i].getRect().getMinY());
            ds.writeInt(elements[i].getRect().getMaxX());
            ds.writeInt(elements[i].getRect().getMaxY());
            ds.writeLong(elements[i].getPtr());//see [2]
          }//if
        }else
          j = i;
      }//for
      if(fileHdr.isWriteThr() || force){
        bs.flush();
        ds.flush();
        seekCurrNode();
        file.write(bs.toByteArray());
        setDirty(false);
      }else{//if we do not write through
        setDirty(true);
        for(int i=0; i<totalElements; i++)
          if(i != index)
            nodeMBR.expandToInclude(elements[i].getRect());//update the local variable as well - remove
          else
            j = i;
      }//else
    }catch(Exception e){
      e.printStackTrace();
      throw new NodeWriteException("Node.deleteElement Can't delete element. Rtree may be corrupted.");
    }
    //update local variable
    try{
      if(j != -1){//do these things only if it an element was deleted
        totalElements--;
        if(totalElements>0)
          System.arraycopy(elements,j+1,elements,j,(totalElements-j));
        //  else
        //    isNodeEmpty = true;
      }
    }catch(Exception e){
      System.out.println("Node.deleteElement : Error while updating "
                         +"local variable...reading back from file..");
      try{
        refreshNode();
        System.out.println("...successful");
      }catch(IOException ex){
        setDirty(true);
        System.out.println("..node corrupted, rebuild tree  ...quitting");
        throw new NodeWriteException("Node.deleteElement : Can't delete element");
      }
    }
  }
  /**to add an element at the end
     As elements are allocated to this node, each allocated 
     element's children node's parent are reset. This is simply because the
     new node index(for <code>elmt</code>) would be different from the old
     ones(if any). 
     <br>Note:-This again is for non leaf node only.
  */
  public void insertElement(Element elmt) 
    throws NodeWriteException, NodeFullException
  {
    //check for space
    if((totalElements == MAX))
      throw new NodeFullException("Node.insertElement: Node full");
    //check if it is an empty node or not
    //if(!isNodeEmpty){//if not empty
    if(totalElements > 0){//if not empty
      if(elmt.getElementType() != elementType)
        throw new NodeWriteException("Node.insertElement: Wrong element type");
      if(((totalElements+1)*elementSize) > NODE_BODY_SIZE)//no space left
        throw new NodeWriteException("Node.insertElement: Node size is becoming more than allowed");
      if(fileHdr.isWriteThr())
        RTree.chdNodes.remove(fileName,nodeIndex);
      writeLastElement(elmt);
    }
    else{//else set the header values depending upon the new object header
      writeLastElement(elmt);
    }
    //update the children's parents
    if( elmt.getElementType() == Node.NONLEAF_NODE ){
      try{
        Node child = null;
        if(fileHdr.isWriteThr()){
          child = new Node(file, fileName, elmt.getPtr(), fileHdr);
          RTree.chdNodes.remove(fileName, child.getNodeIndex());
        }
        else{
          child = RTree.chdNodes.getNode(file, fileName, elmt.getPtr(), fileHdr);
        }
        child.setParent(nodeIndex);
      }
      catch(Exception e){
        throw new NodeWriteException("Node.insertElement: " + e.getMessage());
      }
    }
  }
  /**
     This func. writes the element to the last position.
     It also takes care of updating the node header.
     Also updates the local variables.
     Also takes care whether it is the first element in the node or not.
     If it is the first element then it sets the node header accordingly.
      
     Least error checking - use it with care.
     If the node is old and the new element type is of different type then it
     will change the node header to the new type - so be very very careful!
     In short don't call this method to insert an element different from the
     already present element type.
  */
  private void writeLastElement(Element elmt)
    throws NodeWriteException
  {
    //taking backup in case of rollback
    int oldElementSize = elementSize;
    int oldElementType = elementType;
    int oldTotalElements = totalElements;
    //boolean oldIsNodeEmpty = isNodeEmpty;
    if(fileHdr.isWriteThr())
      RTree.chdNodes.remove(fileName,nodeIndex);
    try{
      //setting local variables first
      if(elmt instanceof LeafElement){
        //size of the element
        elementSize = LeafElement.sizeInBytes();
        elementType = LEAF_NODE;
      }
      else{
        //size of the element
        elementSize = NonLeafElement.sizeInBytes();
        elementType = NONLEAF_NODE;
      }
      if(fileHdr.isWriteThr()){
        //byte[] data = new byte[elementSize];
        ByteArrayOutputStream bs = new ByteArrayOutputStream(elementSize);
        DataOutputStream ds =  new DataOutputStream(bs);
        ds.writeInt(elmt.getRect().getMinX());
        ds.writeInt(elmt.getRect().getMinY());
        ds.writeInt(elmt.getRect().getMaxX());
        ds.writeInt(elmt.getRect().getMaxY());
        ds.writeLong(elmt.getPtr());//see [2] - replace elements with elmt
        
        bs.flush();
        ds.flush();
        //write to the file
        seekLastElement();//uses var.
        file.write(bs.toByteArray());
        setDirty(false);
      }else
        setDirty(true);
      //write the node header
      writeNodeHeader(nodeIndex,totalElements+1,parent,elementSize,elementType);
      //local variables
      //isNodeEmpty = false;
      elements[totalElements-1] = elmt;
      nodeMBR.expandToInclude(elmt.getRect());//remove
    }
    catch(Exception e){
      //e.printStackTrace();
      //if anything goes wrong then set 'totalElements' will not
      //be set hence it remains '0'
      elementSize = oldElementSize;
      elementType = oldElementType;
      totalElements = oldTotalElements;
      //isNodeEmpty = oldIsNodeEmpty;
      throw new 
        NodeWriteException("Node.writeLastElement: Can't write element to file"); 
    }
  }

  /**to add more than onr element at the end.
     As elements are allocated to this node, each allocated element's children node's parent are reset.
     This is simply because the new node index(for <code>elmts</code>) would be different from the old
     ones(if any). none of the element should be null.
     <br><b>Note:-</b> Giving <code>updateChldrn</code> <code>true</code> will not update the parent from
     cache, but would actually update the parent on disk.
     @param elmts The elements that are to be entered. None of them should be null.
     @param updateChldrn Whether to update the children. When we are moving from root to leaves,
     this should be false.
  */
  public void insertElement(Element[] elmts, boolean updateChldrn)
    throws NodeWriteException, NodeFullException
  {
    //check for space
    if(totalElements == MAX)
      throw new NodeFullException("Node.insertElement: Node full or not adequate space");
    //check if it is an empty node or not
    //if(!isNodeEmpty){//if not empty
    if(totalElements > 0){//if not empty
      if(elmts[0].getElementType() != elementType)
        throw new NodeWriteException("Node.insertElement: Wrong element type");
      if(((totalElements+elmts.length)*elementSize) > NODE_BODY_SIZE)//no space left
        throw new NodeWriteException("Node.insertElement: Node size is becoming more than allowed");
      if(fileHdr.isWriteThr())
        RTree.chdNodes.remove(fileName,nodeIndex);
      writeLastElements(elmts);
    }
    else{//else set the header values depending upon the new object header
      writeLastElements(elmts);
    }
    if(!updateChldrn)
      return;
    //update the children's parent from the disk and not the cache.
    if( elmts[0].getElementType() == Node.NONLEAF_NODE ){
      try{
        for(int i=0; i<elmts.length; i++){
          if(elmts[i].getPtr() == Node.NOT_DEFINED)
            continue;
          Node child = null;
          if(fileHdr.isWriteThr()){
            child = new Node(file, fileName, elmts[i].getPtr(), fileHdr);
            RTree.chdNodes.remove(fileName, child.getNodeIndex());
          }else
            child = RTree.chdNodes.getNode(file, fileName, elmts[i].getPtr(), fileHdr);
          //child = new Node(file, fileName, elmts[i].getPtr(), fileHdr);
          child.setParent(nodeIndex);
        }
      }catch(Exception e){
        e.printStackTrace();
        
        throw new NodeWriteException("Node.insertElement: " + e.getMessage());
      }
    }
  }
  /**
     This method helps in bulk loading.
     This func. writes the elements to the last position.
     It also takes care of updating the node header.
     Also updates the local variables.
     Also takes care whether these are the first elements in the node or not.
     If these <i>are</i> the first element then it sets the node header accordingly.

     Least error checking - use it with care.
     If the node is old and the new element type is of different type then it
     will change the node header to the new type - so be very very careful!
     In short don't call this method to insert an element different from the
     already present element type.
  */
  private void writeLastElements(Element[] elmts)
    throws NodeWriteException
  {
    //taking backup in case of rollback
    int oldElementSize = elementSize;
    int oldElementType = elementType;
    int oldTotalElements = totalElements;
    //boolean oldIsNodeEmpty = isNodeEmpty;
    if(fileHdr.isWriteThr())
      RTree.chdNodes.remove(fileName,nodeIndex);
    try{
      //setting local variables first
      if(elmts[0] instanceof LeafElement){
        //size of the element
        elementSize = LeafElement.sizeInBytes();
        elementType = LEAF_NODE;
      }
      else{
        //size of the element
        elementSize = NonLeafElement.sizeInBytes();
        elementType = NONLEAF_NODE;
      }
      ByteArrayOutputStream bs = null;
      DataOutputStream ds =  null;

      //write node header

      if(fileHdr.isWriteThr()){
        setDirty(false);
        bs = new ByteArrayOutputStream(Node.NODE_SIZE);
        ds =  new DataOutputStream(bs);
        writeNodeHeader(nodeIndex, totalElements+elmts.length, parent, elementSize, elementType, ds);
        //write the existing elements
        for(int i=0; i<oldTotalElements; i++){
          ds.writeInt(elements[i].getRect().getMinX());
          ds.writeInt(elements[i].getRect().getMinY());
          ds.writeInt(elements[i].getRect().getMaxX());
          ds.writeInt(elements[i].getRect().getMaxY());
          ds.writeLong(elements[i].getPtr());
        }
      }else{
        writeNodeHeader(nodeIndex, totalElements+elmts.length, parent, elementSize, elementType, ds);
        setDirty(true);
      }
      //write the new elements
      for(int i=0; i<elmts.length; i++){
        nodeMBR.expandToInclude(elmts[i].getRect());//remove
        elements[oldTotalElements+i] = elmts[i];
        if(fileHdr.isWriteThr()){
          ds.writeInt(elmts[i].getRect().getMinX());
          ds.writeInt(elmts[i].getRect().getMinY());
          ds.writeInt(elmts[i].getRect().getMaxX());
          ds.writeInt(elmts[i].getRect().getMaxY());
          ds.writeLong(elmts[i].getPtr());
        }
      }
      if(fileHdr.isWriteThr()){
        bs.flush();
        ds.flush();
        //write to the file
        seekNode(nodeIndex);
        file.write(bs.toByteArray());
      }
      //local variables
      //isNodeEmpty = false;
    }
    catch(Exception e){
      e.printStackTrace();
      //if anything goes wrong then set 'totalElements' will not
      //be set hence it remains '0'
      elementSize = oldElementSize;
      elementType = oldElementType;
      totalElements = oldTotalElements;
      //isNodeEmpty = oldIsNodeEmpty;
      try{
        writeNodeHeader(nodeIndex, totalElements+elmts.length, parent, elementSize, elementType);
      }catch(Exception ex){throw new NodeWriteException(ex.getMessage());}
      throw new NodeWriteException("Node.writeLastElement: Can't write element to file"); 
    }
  }

  /**this function simply places the file pointer, it does not check for any
     condition like placing the pointer beyond the end of file
  */
  private void seekCurrNode() throws IOException
  {
    file.seek(FILE_HDR_SIZE + (nodeIndex * NODE_SIZE));
  }
  /**seek the specified node*/
  private void seekNode(long nodeIdx) throws IOException
  {
    file.seek(FILE_HDR_SIZE + (nodeIdx * NODE_SIZE));
  }

  /**Seek the last element of the node. No checking*/
  private void seekLastElement() throws IOException
  {
    file.seek(FILE_HDR_SIZE + (nodeIndex * NODE_SIZE) 
              + (NODE_HDR_SIZE) +(elementSize * totalElements));
  }
  /**seek the specified element.No checking*/
  private void seekElement(int elmtIndex) throws IOException
  {
    file.seek(FILE_HDR_SIZE + (nodeIndex * NODE_SIZE) 
              + (NODE_HDR_SIZE) + (elementSize * elmtIndex));
  }
  /**seek the specified element's pointer.No checking*/
  private void seekElementPtr(int elmtIndex) throws IOException
  {
    file.seek(FILE_HDR_SIZE + (nodeIndex * NODE_SIZE) 
              + (NODE_HDR_SIZE) + (elementSize * elmtIndex)
              + Rect.sizeInBytes());
  }
  public int getElementType()
  {
    return elementType;
  }
  /**this func. writes the passed info to the current node's header.
     Also updates the local variables.
  */
  private void writeNodeHeader(long nodeIdx,int totElmt,long prnt,int elmtSz,int elmtTp)
    throws IOException, NodeWriteException
  {
    if(fileHdr.isWriteThr())
      RTree.chdNodes.remove(fileName,nodeIndex);
    if(fileHdr.isWriteThr()){
      ByteArrayOutputStream bs = new ByteArrayOutputStream(FILE_HDR_SIZE);
      DataOutputStream ds =  new DataOutputStream(bs);
      ds.writeInt(totElmt);//total elements
      ds.writeLong(prnt);//parent
      ds.writeInt(elmtSz);//element size 
      ds.writeInt(elmtTp);//element type 
      
      bs.flush();
      ds.flush();
      //write to the file
      seekNode(nodeIdx);
      file.write(bs.toByteArray());
      setDirty(false);
    }else
      setDirty(true);
    //update local variables
    totalElements = totElmt;
    parent = prnt;
    elementSize = elmtSz;
    elementType = elmtTp;
  }
  /**
     This method will write the node header into the <code>ds</code>. Will also update the local variables.
     It is assumed that the write pointer is correctly set in the o/p stream.
  */
  private void writeNodeHeader(long nodeIdx, int totElmt,long prnt,int elmtSz,int elmtTp, 
                               DataOutputStream ds)
    throws IOException, NodeWriteException
  {
    if(fileHdr.isWriteThr())
      RTree.chdNodes.remove(fileName,nodeIndex);
    if(fileHdr.isWriteThr()){
      ds.writeInt(totElmt);//total elements
      ds.writeLong(prnt);//parent
      ds.writeInt(elmtSz);//element size 
      ds.writeInt(elmtTp);//element type 
      ds.flush();
      setDirty(true);
    }else
      setDirty(true);
    //update local variables
    totalElements = totElmt;
    parent = prnt;
    elementSize = elmtSz;
    elementType = elmtTp;
  }
  /**
     will return the index of the node.
  */
  public long getNodeIndex()//for new nodes
  {
    return(nodeIndex);
  }
  private void refreshNode()//see wherever it is called from for writethr
    throws IOException
  {
    try{
      byte[] data = new byte[NODE_SIZE];
      seekCurrNode();
      //read the whole node
      file.read(data);
      //get the header details into the variables
      DataInputStream ds = new DataInputStream(new ByteArrayInputStream(data));
      totalElements = ds.readInt();
      parent = ds.readLong();//ds.readInt();
      elementSize = ds.readInt();
      elementType = ds.readInt();
      if(totalElements <= 0)//set local variable
        return;
      //  else
      //    isNodeEmpty = false;
      //get the elements if present
      if(totalElements < 1)
        return;
      nodeMBR = new Rect();//remove
      int minX,minY,maxX,maxY;
      for(int i=0; i< totalElements; i++){
        //read the points
        minX = ds.readInt();
        minY = ds.readInt();
        maxX = ds.readInt();
        maxY = ds.readInt();
        Rect rectangle = new Rect(minX,minY,maxX,maxY);
        nodeMBR.expandToInclude(rectangle);//remove
        if(elementType == LEAF_NODE){// modified see [1]
          long ptr = ds.readLong();
          elements[i] = new LeafElement(rectangle,ptr);
        }else if(elementType == NONLEAF_NODE){//if non leaf type then...
          long nodePtr = ds.readLong();
          elements[i] = new NonLeafElement(rectangle,nodePtr);
        }
      }
      ds.close();
    }
    catch(Exception e){
      throw new IOException("Node.refreshNode : Can't read from node header " + e.getMessage());
    }
        
  }
  Rect[] getAllRectangles()
    throws  IllegalValueException
  {
    if(totalElements == 0)
      throw new  IllegalValueException("Node.getAllRectangles: No elements in the node");
    Rect[] rects = new Rect[totalElements];
    for(int i=0; i<totalElements; i++){
      rects[i] = elements[i].getRect();
    }
    return rects;
  }
  /**
     Returns the element(of the current node) whose rectangle needs the least 
     enlargment to include <code>elmt</code>.
     The logic assumes that the elements are not sorted.
     See the documentation for least enlargement logic.
  */
  public Element getLeastEnlargement(Element elmt)
    throws NodeEmptyException, IllegalValueException, NodeWriteException
  {
    if(elmt == null)
      throw new  IllegalValueException("Node.getBestFitElement : Element is null");
    //if(isNodeEmpty){//if there are no elements in the node
    if(totalElements <= 0){//if there are no elements in the node
      throw new NodeEmptyException("Node.getBestFitElement : Node does not have any elements");
    }
    if(fileHdr.isWriteThr())
      RTree.chdNodes.remove(fileName,nodeIndex);
    Element retElmt;//initialize with first element         
    int area;
        
    //get area of first MBR and call it the least area
    int leastArea=(elements[0].getRect().getResultingMBR(elmt.getRect())).getArea();
    leastArea -= elements[0].getRect().getArea(); 
    if(elementType == Node.LEAF_NODE)
      //Integer intVal = (Integer)elements[0].getPtr();//the org. code
      retElmt = new LeafElement(elements[0].getRect(), elements[0].getPtr() );
    else
      retElmt = new NonLeafElement(elements[0].getRect(), elements[0].getPtr());
        
    //now check each of the elements
    for(int i=1; i < totalElements; ++i){
      //get area of the MBR of the two rects.
      area=(elements[i].getRect().getResultingMBR(elmt.getRect())).getArea();
      //remove the area of the encapsulating rect.
      area -= elements[i].getRect().getArea();
      //check if it is the smallest rect
      if(leastArea > area){
        leastArea = area;
        if(elementType == Node.LEAF_NODE)
          retElmt = new LeafElement(elements[i].getRect(), elements[i].getPtr());
        else
          retElmt = new NonLeafElement(elements[i].getRect(), elements[i].getPtr());
      }
      else if(leastArea == area){//Resovle ties by choosing the entry with the rectangle of smallest area."
        if(retElmt.getRect().getArea() >= elements[i].getRect().getArea()){
          leastArea = area;
          if(elementType == Node.LEAF_NODE)
            retElmt = new LeafElement(elements[i].getRect(), elements[i].getPtr());
          else
            retElmt = new NonLeafElement(elements[i].getRect(),elements[i].getPtr());
        }
      }
    }
    return(retElmt);
  }
  /**
     @return a boolean describing whether there is space for another element
  */
  boolean isInsertPossible()
  {
    if(totalElements >= MAX)
      return false;
    else
      return true;
  }

  /**
     See ./Logic.txt
     Linear split Algo.
     February 1003 - Now quad split algo.
     @return First node would be the original node.Second would be the new one.
     @param Element The new element to be inserted
     @param slotIndex The index of the slot of this tree if any, else give NOT_DEFINED.
  */
  public Node[] splitNode(Element elmtM1, long slotIndex)
    throws RTreeException, NodeWriteException
  {
    if((totalElements < MAX) || (elmtM1.getElementType() != elementType))
      throw new RTreeException("Node.splitNode: Node is not full or new element is of wrong type");
    if(fileHdr.isWriteThr())
      RTree.chdNodes.remove(fileName,nodeIndex);
    try{        
      int rem = totalElements+1;//no. of elements remaining + the new element
      Element[] elmtPlusOne = new Element[rem];
      for(int i=0;i<rem-1;i++)
        elmtPlusOne[i] = elements[i];
      elmtPlusOne[totalElements] = elmtM1; 
      //the elements which have been allocated: 1 - present, 0 - absent
      int[] elmtsGone = new int[rem];
      for(int i=0; i<elmtsGone.length; ++i)
        elmtsGone[i] = 1;//all elements present
      int elmtsInA = 0;//total elements in A
      int elmtsInB = 0;//total elements in B
      Rect mbrA;
      Rect mbrB;
      //int[] seeds = lnrPickSeeds(elmtPlusOne);//the two seed
      int[] seeds = quadPickSeeds(elmtPlusOne);//the two seed
      int elmtType = elmtPlusOne[0].getElementType();
      //never do any file read or write on the old node now as the new nodes
      //could very well be the old node from the free list.
      //Although they would be freed explicitly later.
      Node nodeA, nodeB;
      if(fileHdr.isWriteThr()){
        nodeA = new Node(file,fileName,parent,elmtType,fileHdr);
        nodeB = new Node(file,fileName,parent,elmtType,fileHdr);
      }else{
        nodeA = RTree.chdNodes.getNode(file,fileName,parent,elmtType,fileHdr);
        nodeB = RTree.chdNodes.getNode(file,fileName,parent,elmtType,fileHdr);
      }
      nodeA.insertElement(elmtPlusOne[seeds[0]]);
      nodeB.insertElement(elmtPlusOne[seeds[1]]);
      //update the MBRs
      mbrA = elmtPlusOne[seeds[0]].getRect();
      mbrB = elmtPlusOne[seeds[1]].getRect();
      //update the variables
      elmtsInA++;//Inc. 'A' count by 1
      elmtsGone[seeds[0]] = 0;//mark the element assigned
      elmtsInB++;//Inc. 'B' count by 1
      elmtsGone[seeds[1]] = 0;//mark the element assigned
      rem -= 2;//less the no. of elements still left
      //a loop till the elements last
      while(rem > 0){
        //if A needs all to equate m then give it all the elements - see Guttman
        if((Node.MIN - elmtsInA) == rem){
          for(int i=0; i < elmtsGone.length; i++){
            //check if the elmt. is already assigned
            if(elmtsGone[i] == 1){
              nodeA.insertElement(elmtPlusOne[i]);//read  variable
              elmtsGone[i] = 0;//element now is absent
              elmtsInA++;
              rem--;
              //find the new MBR fro A
              /*Commented because not required
                mbrA = Rect.getResultingMBR(mbrA,elmtPlusOne[i].getRect());
              */
              //the end of the loop and method
            }
          }
        }
        //if B needs all to equate m then give it all the elements-see Guttman
        else if((Node.MIN - elmtsInB) == rem){
          for(int i=0; i < elmtsGone.length; i++){
            //check if the elmt. is already assigned
            if(elmtsGone[i] == 1){
              nodeB.insertElement(elmtPlusOne[i]);//read  variable
              elmtsGone[i] = 0;//element now is absent
              elmtsInB++;
              rem--;
              //find the new MBR fro B
              /*Commented because not required
                mbrB = Rect.getResultingMBR(mbrB,elmtPlusOne[i].getRect());
              */
              //the end of the loop and method
            }
          }
        }
        //if both are ok
        else{
          int i=-1;
          //loop till an unassigned element is found
          try{
            while((++i<elmtsGone.length)&&(elmtsGone[i] == 0));
          }catch(Exception e){
            System.out.println("Node.splitNode: trouble in paradise");
            //System.exit(1);
          }
          /*Above statement can be troublesome, in that case use the 
            below logic
            for(int i=0; i < elmtsGone.length; i++)
            if(elmtsGone[i] == 0)
            break;
          */
          //find MBR for both the groups
          Rect newMBRA = elmtPlusOne[i].getRect().getResultingMBR(mbrA);
          Rect newMBRB = elmtPlusOne[i].getRect().getResultingMBR(mbrB);
          //remove the original area
          int newAreaA = newMBRA.getArea() - mbrA.getArea();
          int newAreaB = newMBRB.getArea() - mbrB.getArea();
          //find which group to enter - see - 'Guttman(QS3)
          //A needs least enlargement
          if (newAreaA < newAreaB){
            nodeA.insertElement(elmtPlusOne[i]);//read local variable
            elmtsGone[i] = 0;//element now is absent
            elmtsInA++;
            rem--;
            mbrA = newMBRA;
          }
          //B needs least enlargement
          else if(newAreaA > newAreaB){
            nodeB.insertElement(elmtPlusOne[i]);//read local variable
            elmtsGone[i] = 0;//element now is absent
            elmtsInB++;
            rem--;
            mbrB = newMBRB;
          }
          //if equal but A is smaller
          else if(mbrA.getArea() < mbrB.getArea()){
            nodeA.insertElement(elmtPlusOne[i]);//read local variable
            elmtsGone[i] = 0;//element now is absent
            elmtsInA++;
            rem--;
            mbrA = newMBRA;
          }
          //if equal but B is smaller
          else if(mbrA.getArea() > mbrB.getArea()){
            nodeB.insertElement(elmtPlusOne[i]);//read local variable
            elmtsGone[i] = 0;//element now is absent
            elmtsInB++;
            rem--;
            mbrB = newMBRB;
          }
          //also equal in area elmts. in A are less 
          else if(elmtsInA < elmtsInB){
            nodeA.insertElement(elmtPlusOne[i]);//read local variable
            elmtsGone[i] = 0;//element now is absent
            elmtsInA++;
            rem--;
            mbrA = newMBRA;
          }
          //also equal in area elemts. in B are less
          else if(elmtsInA > elmtsInB){
            nodeB.insertElement(elmtPlusOne[i]);//read local variable
            elmtsGone[i] = 0;//element now is absent
            elmtsInB++;
            rem--;
            mbrB = newMBRB;
          }
          //choose any one
          else{
            nodeA.insertElement(elmtPlusOne[i]);//read local variable
            elmtsGone[i] = 0;//element now is absent
            elmtsInA++;
            rem--;
            mbrA = newMBRA;
          }
        }
      }
      /*
        Adjust the parent element so that it points to the first nodeA.
      */
      if(parent != slotIndex){//NOT_DEFINED){
        Node parentN = null;
        if(fileHdr.isWriteThr())
          parentN = new Node(file,fileName,parent,fileHdr);
        else
          parentN = RTree.chdNodes.getNode(file,fileName,parent,fileHdr);
        if(fileHdr.isWriteThr())
          RTree.chdNodes.remove(fileName,parent);
        //get the parent element of nodes[0]
        int parentElmtIndex = parentN.getElementIndex(nodeIndex);
        parentN.modifyElement(parentElmtIndex, nodeA.getNodeIndex());
      }
      Node[] ret = new Node[2];
      ret[0] = nodeA;
      ret[1] = nodeB;
      deleteNode();
      return(ret);
    }catch(Exception e){
      e.printStackTrace();
      throw new RTreeException("Node.nodeSplit : " + e.getMessage());
    }
  }
  /**
   * The linear Pick Seed method from Guttman.
   * Assuming that we have only 2 dimensions.
   * <br>this method is no longer used (certainly not deprecated), instead <code>quadPickSeeds</code>
   * is being used
   * @return the two picked indexes from the node
   */
  private int[] lnrPickSeeds(Element[] elmts)
    throws  IllegalValueException
  {
    if(elmts.length <= 1)
      throw new  IllegalValueException("Node.lnrPickSeed : PickSeed not possible as there are no elements");
    //find the MBR of the set.
    Rect mbr = elmts[0].getRect();
    for(int i=1; i<elmts.length; ++i)
      mbr = elmts[i].getRect().getResultingMBR(mbr);
    //"Find Extreme Rectangles along all dimensions" - Guttman
    //find the highest low side and lowest high side.
    int hlsX = Integer.MIN_VALUE;//highest low side for X
    int hlsY = Integer.MIN_VALUE;//  ..
    int lhsX = Integer.MAX_VALUE;//  ..
    int lhsY = Integer.MAX_VALUE;//  ..
    int hlsIdxX = Integer.MAX_VALUE;//highest low side index for X
    int hlsIdxY = Integer.MAX_VALUE;//  ..
    int lhsIdxX = Integer.MAX_VALUE;//  ..
    int lhsIdxY = Integer.MAX_VALUE;//  ..

    for(int i=0; i<elmts.length; ++i){
      //highest low side for X
      if(elmts[i].getRect().getMinX() >= hlsX){
        hlsX = elmts[i].getRect().getMinX();
        hlsIdxX = i;
      }
      //highest low side for Y
      if(elmts[i].getRect().getMinY() >= hlsY){
        hlsY = elmts[i].getRect().getMinY();
        hlsIdxY = i;
      }
      //lowest high side for X
      if(elmts[i].getRect().getMaxX() <= lhsX){
        lhsX = elmts[i].getRect().getMaxX();
        lhsIdxX = i;
      }
      //lowest high side for Y
      if(elmts[i].getRect().getMaxY() <= lhsY){
        lhsY = elmts[i].getRect().getMaxY();
        lhsIdxY = i;
      }
    }
    //"Normalize the separations" - Guttman
    //divide the separations along each dimensions by the width of the MBR
    int[] retIdx = new int[2];
    int Xpair = (Math.abs(lhsX - hlsX))/mbr.getWidth(); 
    int Ypair = (Math.abs(lhsY - hlsY))/mbr.getHeight();
    if(Xpair > Ypair){
      //if both are the same rect then choose randomly
      if(hlsIdxX == lhsIdxX){
        if(hlsIdxX != 0) 
          hlsIdxX = 0;
        else
          hlsIdxX = 1;
      }
      retIdx[0] = hlsIdxX;
      retIdx[1] = lhsIdxX;   
    }
    else if(Xpair < Ypair){
      /*if both are the same rect then choose randomly - an accidental 
        discovery*/
      if(hlsIdxY == lhsIdxY){
        if(hlsIdxY != 0) 
          hlsIdxY = 0;
        else
          hlsIdxY = 1;
      }
      retIdx[0] = hlsIdxY;
      retIdx[1] = lhsIdxY;          
    }
    else{//if normalized values are equal
      //choose the pair with the least separation(not normalized sap.)
      if((Math.abs(lhsX - hlsX)) >= (Math.abs(lhsY - hlsY))){
        if(hlsIdxX == lhsIdxX){
          if(hlsIdxX != 0) 
            hlsIdxX = 0;
          else
            hlsIdxX = 1;
        }
        retIdx[0] = hlsIdxX;
        retIdx[1] = lhsIdxX;        
      }
      else{
        if(hlsIdxY == lhsIdxY){
          if(hlsIdxY != 0) 
            hlsIdxY = 0;
          else
            hlsIdxY = 1;
        }
        retIdx[0] = hlsIdxY;
        retIdx[1] = lhsIdxY;        
      }
    }
    return(retIdx);
  }
  /**
     The quadratic Pick Seed method from Guttman.
     Assuming that we have only 2 dimensions. This method is slightly slower than the linear method but
     it results in better splits, besides it is much easier to implement.
     @return the two picked indexes from the node
  */
  private int[] quadPickSeeds(Element[] elmts)
    throws  IllegalValueException
  {
    if(elmts.length <= 1)
      throw new  IllegalValueException("Node.quadPickSeed : PickSeed not possible as there are no elements");
    int retIdx[] = new int[2];
    int mostIneff = Integer.MIN_VALUE;//area of the most inefficient pair
    for(int i=0;i<elmts.length;i++){
      for(int j=i+1;j<elmts.length;j++){
        Rect seedRect = Rect.getResultingMBR(elmts[i].getRect(), elmts[j].getRect());
        int arr = seedRect.getArea() - elmts[i].getRect().getArea() - elmts[j].getRect().getArea();
        //if this is the most inefficient pair
        if(arr > mostIneff){
          retIdx[0] = i;
          retIdx[1] = j;
          mostIneff = arr;
        }
      }
    }
    return retIdx;
  }
  public long getParent()
  {
    return(parent);
  }
  /**
   * returns index of the element with the pointer passed in the parameter
   * @param Object Depends upon the Element type.
   * @return returns NOT_DEFINED if ptr is not found
   */
  public int getElementIndex(long ptr/*Object ptr*/)
  {
    if(totalElements < 1)
      return NOT_DEFINED;
    for(int i=0; i<totalElements; i++){
      if(elements[i].getPtr() == ptr)
        return i;
    }
    System.out.println("Node.getElementIndex: Element not found, returning NOT_DEFINED");
    //Even if object types do not match it will return NOT_DEFINED
    return NOT_DEFINED;//if nothing found
  }
  /**
     Used to overwrite the old Element with the new one.
     It modifies the element in the disk as well as in the local variables.
  */
  public void modifyElement(int index,Element elmt)
    throws  IllegalValueException,IOException, NodeWriteException
  {
    if((index > totalElements) || (index < 0) || (elmt == null))
      throw new  IllegalValueException("Node.modifyElmtMBR : index out of bound or MBR is null");
        
    if(elmt.getElementType() != elementType)
      throw new  IllegalValueException("Node.modifyElmtMBR : Element of wrong type");
    if(fileHdr.isWriteThr())
      RTree.chdNodes.remove(fileName,nodeIndex);
    if(fileHdr.isWriteThr()){
      ByteArrayOutputStream bs = new ByteArrayOutputStream(elementSize);
      DataOutputStream ds =  new DataOutputStream(bs);
      ds.writeInt(elmt.getRect().getMinX());
      ds.writeInt(elmt.getRect().getMinY());
      ds.writeInt(elmt.getRect().getMaxX());
      ds.writeInt(elmt.getRect().getMaxY());
      ds.writeLong(elmt.getPtr());//see [2]
      
      bs.flush();
      ds.flush();
      //write to the file
      seekElement(index);
      file.write(bs.toByteArray());
      setDirty(false);
    }else     
      setDirty(true);
    //adjsut in the local variable
    elements[index].setRect(elmt.getRect());
    elements[index].setPtr(elmt.getPtr());
    //we do not recalculate the whole mbr if we do not need to
    if(elmt.getRect().contains(elements[index].getRect()))//if previous MBR was smaller...
      nodeMBR.expandToInclude(elmt.getRect());
    else//i guess we need to calculate
      refreshNodeMBR();//remove
  }
  /**
     Overloaded
  */
  public void modifyElement(int index,long pointer)
    throws  IllegalValueException,IOException, NodeWriteException
  {
    if((index > totalElements) || (index < 0)){
      try{
        throw new  IllegalValueException("Node.modifyElmtMBR : index out of bound for node "+nodeIndex);
      }catch(Exception e){
        e.printStackTrace();
      }
    }
    if(fileHdr.isWriteThr())
      RTree.chdNodes.remove(fileName,nodeIndex);
    if(fileHdr.isWriteThr()){
      ByteArrayOutputStream bs = new ByteArrayOutputStream(LONG_SIZE);
      DataOutputStream ds =  new DataOutputStream(bs);
      ds.writeLong(pointer);//ds.writeInt(pointer);
      bs.flush();
      ds.flush();
      //write to the file
      seekElementPtr(index);
      file.write(bs.toByteArray());
      setDirty(false);
    }
    else
      setDirty(true);
    //adjsut in the local variable
    elements[index].setPtr(pointer);
  }
  /**
     Overloaded
  */
  public void modifyElement(int index,Rect rect)
    throws  IllegalValueException,IOException, NodeWriteException
  {
    if((index > totalElements) || (index < 0))
      throw new IllegalValueException("Node.modifyElmtMBR : index out of bound or MBR is null");
    if(fileHdr.isWriteThr())
      RTree.chdNodes.remove(fileName,nodeIndex);
    if(fileHdr.isWriteThr()){
      ByteArrayOutputStream bs = new ByteArrayOutputStream(Rect.sizeInBytes());
      DataOutputStream ds =  new DataOutputStream(bs);
      ds.writeInt(rect.getMinX());
      ds.writeInt(rect.getMinY());
      ds.writeInt(rect.getMaxX());
      ds.writeInt(rect.getMaxY());
      bs.flush();
      ds.flush();
      //write to the file
      seekElement(index);
      file.write(bs.toByteArray());
      setDirty(false);
    }else
      setDirty(true);
    //adjsut in the local variable
    elements[index].setRect(rect);
    //remove
    //we do not recalculate the whole mbr if we do not need to
    if(rect.contains(elements[index].getRect()))
      nodeMBR.expandToInclude(rect);
    else//i guess we need to calculate
      refreshNodeMBR();//remove
  }
  /**
     This function runs a loop on the elements to calculate the total MBR.
     Therefore in case if you already have loop that runs through each of the entries,
     then it is better to calculate MBR in that loop without calling this method.
     @throws IllegalValueException When there are no elements in the node.
  */
  public Rect getNodeMBR()
    throws  IllegalValueException
  {
    //remove
    if(totalElements < 1)
      throw new  IllegalValueException("Node.getNodeMBR: Node empty");
    return nodeMBR;
    //In case of trouble remove the above line and commission the below written lines
    /*
      Rect ret = elements[0].getRect();
      for(int i=1; i<totalElements; i++)
      ret = Rect.getResultingMBR(ret, elements[i].getRect());
      return ret;
    */
  }
  /**
     Will refresh the <code>nodeMBR</code> for the <code>elements</code>
  */
  private void refreshNodeMBR()
  {
    nodeMBR = new Rect();
    for(int i=0; i<totalElements; i++)
      nodeMBR.expandToInclude(elements[i].getRect());
  }

  /**
     No error echecking at all.
  */
  public void setParent(long /*int*/ prnt)
    throws IOException, NodeWriteException
  {
    if(prnt == NOT_DEFINED)//if this is the new root then update the file hdr
      fileHdr.writeFileHeader(fileHdr.totalNodes,nodeIndex);
    if(fileHdr.isWriteThr())
      RTree.chdNodes.remove(fileName,nodeIndex);
    writeNodeHeader(nodeIndex,totalElements,prnt,elementSize,elementType);
  }
  /**
     Obvious, isn't it?
  */
  public String toString()
  {
    String ret = "\n\t***Node at Index: "+Long.toString(nodeIndex)+"***";
    ret += "\nLocal Variables-";
    //ret += "\n\tFile: " + fileName;
    ret += "\n\tnodeIndex: "+ Long.toString(nodeIndex);
    if (totalElements <= 0){
      //ret += "\n\tisNodeEmpty: true";
      ret += "\n\tnodeMBR: isNull";
    }
    //  else{
    //    ret += "\n\tisNodeEmpty: false";
    //    //ret += "\n\tnodeMBR: " + nodeMBR.toString();
    //  }
    ret += "\nFile Header-";
    ret += "\n\ttotalNodes: " + Integer.toString(fileHdr.totalNodes);
    ret += "\n\trootIndex: " + Long.toString(fileHdr.rootIndex);
    ret += "\nNode Header-";
    ret += "\n\ttotalElements: " + Integer.toString(totalElements);
    ret += "\n\tparent: " + Long.toString(parent);
    ret += "\n\telementSize:" + Integer.toString(elementSize);
    ret += "\n\telementType:" + Integer.toString(elementType);
    for(int i=0;i<totalElements;i++)
      ret += elements[i].toString();
    return ret;
  }
  public int getTotalElements()
  {
    return totalElements;
  }
  /**
     Although it returns all the elements but the total elements will not be equal to the length of
     the returned array.Therefore <br><b>Never Use <code>.length</code> field With the Returned Array
     </b>. Instead use <code>getTotalElements()</code>.
     @return An element Array.
  */
  public Element[] getAllElements()
  {
    return elements;
  }
  Element getElement(int index)
    throws  IllegalValueException
  {
    if((index < 0) || (index > totalElements-1))
      throw new  IllegalValueException("Node.getElement Index out of bound");
    return elements[index];
  }
  /**
     Adds the node to the free stack.
     Be very careful with this method because once called, this node may be
     given to any new node even when you have not destroyed its object.
     If the node is the only node then it updates the file header as well.
     </br><i><b>Once called, there is no turning back!</b></i>.
  */
  public void deleteNode()
    throws NodeWriteException
  {
    setDirty(false);//this is intentional
    RTree.chdNodes.remove(fileName,nodeIndex);//we do not check for writeThr here
    try{
      fileHdr.push(nodeIndex);
    }catch(StackOverflowException e){
    }catch(IOException ex){
      throw new NodeWriteException(ex.getMessage());
    }
  }
  /**
   * This method is added to sort the elements in this node to help sweepline algorithm.
   */
  void sweepSort()//check out for null elements
  {
    if(elements != null && elements.length > 1 && sorted == false){
      Arrays.sort(elements, 0, totalElements, new rtree.join.CompElmtX());
      sorted = true;
    }//if
  }//sweepSort
  /**
     This is a new methos that will help the phylosophy where one should write to tbe cache only when
     required.
     @return true if needed write and written or false (not dirty).
  */
  public boolean flush()
    throws NodeWriteException
  {
    try{
      if(dirty && !fileHdr.isWriteThr()){
        deleteElement(-1, true);
        setDirty(false);
        return true;
      }else
        return false;
    }catch(Exception e){
      e.printStackTrace();
      throw new NodeWriteException(e.getMessage());
    }
  }
  void setDirty(boolean val)
  {
    if(val)//dirty
      sorted = false;
    dirty = val;
  }
  public boolean isDirty()
  {
    return dirty;
  }
  
}
