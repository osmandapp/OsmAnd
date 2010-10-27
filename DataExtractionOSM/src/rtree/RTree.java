//RTree.java
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import rtree.join.IntersectPred;
import rtree.join.PairElmt;
import rtree.join.SweepLine;
/******************************************************************************************************
 * <p><b>1:</b>Call <code>flush</code> after single or multiple inserts. In affect before the application
 * shuts down, the flush method flushes all the rtrees.
 * <p><b>2:</b>Do not run more than one instance of this application. The package
 * cannot handle more than one instance of the application as all the locking
 * is done in the code itself(Monitors) and not on the hard-disk file.
 * <p><b>3:</b>The file name (of the rtree) is case insensitive.
 * <p><b>4:</b>The package is thread proof, i.e you can run as many threads as
 * you want, not worrying about concurrent updations. The package will handle the
 * situation of concurrent reads and writes. Again, it can take care of threads
 * of only one process(Application) and not for more than one process of the  program.
 * <p><b>5:</b>For immediate effects of tree writes, give the thread that needs
 * to write to the file a high priority. This will further speed up tree writes.
 * <p><b>6:</b>Give messages like "Updating, please wait..." to the user when
 * calling any of the methods that modifies the tree as the thread may have to
 * wait on a lock variable.
 * <p><b>7:</b>The package maintains a queue of all threads that need to wait on some lock.
 * The sequence of excecution of the threads is dependent on that queue. All the actions are fired in
 * the order in which they were received. <br><b>In short the tree is perfectly concurrent.</b>
 * <p><b>8:</b>For developers: Always obtain a lock from the <code>lockRead</code> or
 * <code>lockWrite</code> method before going into a <code>public</code> method of the
 * <code>RTree</code> class. Unlock by calling the <code>unlock</code> method.
 * See any existing method to understand the mechanism.
 * <p><b>9:</b>To adjust the cache buffer size, see the <code>Node</code> class documentation.
 * @author Prachuryya Barua
 ******************************************************************************************************/
public class RTree //the tree that would be made
{
  /**<b>Caution:</b> The file name (of the rtree) is case insensitive in spite
     of the fact that this package was developed on a Linux(RH7.0) platform.
  */
  protected String fileName;
  static Map fileList;//the no. of files open
  // static for the other way
  protected FileHdr fileHdr;
  public static CachedNodes chdNodes;
  /**Inner class for the fileList vector - A List of files*/
  class Header
  {
    FileHdr flHdr;
    String fileName;
    Header(FileHdr flH,String name)
    {
      fileName = name;
      flHdr = flH;
    }
  }
  public RTree(String fileName)
    throws  RTreeException
  {
    try{
      this.fileName = fileName;
      if(fileList == null)
        fileList = new HashMap();
      synchronized(fileList){//this may give problem
        if(fileList.get(fileName) != null){
          fileHdr = ((Header)fileList.get(fileName)).flHdr;
          return;
        }
        //a new file
        fileList.put(fileName, new Header(new FileHdr(Node.FREE_LIST_LIMIT, fileName),fileName));
        fileHdr = ((Header)fileList.get(fileName)).flHdr;
        //the cache of nodes - one cache for all the tree files.
        if(chdNodes == null)
          chdNodes = new CachedNodes();
      }
    }
    catch(Exception e){
      throw new  RTreeException("RTree.RTree: " +e.getMessage());
    }
  }
  /**
     This method is used to ask the fileHdr to update itself. This method is package parivate used by
     <code>Pack</code> class only.
  */
  void updateHdr()
    throws RTreeException, IOException, FileNotFoundException, NodeWriteException
  {
    Header tmp = (Header)fileList.get(fileName);
    if(tmp != null){
      //chdNodes.removeAll();//XXX check this out
      fileHdr.update(fileName);
    }
  }

  public Node getReadNode(long index) throws RTreeException
  {
    try{
      return chdNodes.getReadNode(fileHdr.getFile(), fileName, index, fileHdr);
    }catch(Exception e){
      throw new RTreeException ("RTree.getSortedNode : " + e.getMessage());
    }
  }

  public String getFileName()
  {
    return fileName;
  }
  /**
     Another package private method for getting the file header
  */
  public FileHdr getFileHdr()
  {
    return fileHdr;
  }
  public void flush()
    throws RTreeException
  {
    fileHdr.lockWrite();
    try{
      fileHdr.flush();
      chdNodes.flush();
    }catch(Exception e){
      throw new RTreeException(e.getMessage());
    }finally{
      fileHdr.unlock();
    }
  }
  /**
   * Adjust Tree from <b>Guttman the Great</b>.
   * @param Node[] The nodes that was has the new element and also the element
   * that resulted from the splt(if any).i.e N-node[0], NN-nodes[1]
   * Note:- This method is functionally very much coupled with Node.spliNode()
   * @param node The two new nodes caused by split.
   * @param slotIndex The index of the slot of this tree if any, else give NOT_DEFINED.
   * @return The new root (if it was created). If no new root was created then returns null.
   */
  protected Node adjustTree(Node[] nodes, long slotIndex)
    throws  RTreeException
  {
    try{
      //if(nodes[0].getParent() == Node.NOT_DEFINED){//original
      if(nodes[0].getParent() == slotIndex){
        if(nodes[1] != null){//if root is split
          Node newRoot;
          if(fileHdr.isWriteThr())
            newRoot = new Node(fileHdr.getFile(),fileName, slotIndex, Node.NONLEAF_NODE,
                               ((Header)fileList.get(fileName)).flHdr);
          else
            newRoot = chdNodes.getNode(fileHdr.getFile(),fileName, slotIndex, Node.NONLEAF_NODE,
                                       ((Header)fileList.get(fileName)).flHdr, nodes[0]);
          NonLeafElement branchA = new NonLeafElement(nodes[0].getNodeMBR(),nodes[0].getNodeIndex());
          NonLeafElement branchB = new NonLeafElement(nodes[1].getNodeMBR(),nodes[1].getNodeIndex());
          newRoot.insertElement(branchB);
          newRoot.insertElement(branchA);
          return newRoot;
        }
        return null;
      }else{
        //where the new element is inserted
        Node[] insertedNode = new Node[2];
        /*
          set the parent element's MBR equal to the nodeA's MBR.
        */
        //the parent of this node
        Node parentN;
        if(fileHdr.isWriteThr())
          parentN = new Node(fileHdr.getFile(),fileName,nodes[0].getParent(),fileHdr);
        else
          parentN = chdNodes.getNode(fileHdr.getFile(),fileName,nodes[0].getParent(),fileHdr);
        //get the parent element of nodes[0]
        //Integer intValue = new Integer(nodes[0].getNodeIndex());
        int parentElmtIndex = parentN.getElementIndex(nodes[0].getNodeIndex()/*intValue*/);
        //adjust the parent element's MBR
        parentN.modifyElement(parentElmtIndex,nodes[0].getNodeMBR());
        insertedNode[0] = parentN;
        insertedNode[1] = null;
        //if it is an split node add its entry in the parent
        if(nodes[1] != null){
          NonLeafElement elmtNN = new NonLeafElement(nodes[1].getNodeMBR(),nodes[1].getNodeIndex());
          try{//if another insert is possible
            parentN.insertElement(elmtNN);
            insertedNode[0] = parentN;
            insertedNode[1] = null;
          }catch(NodeFullException e){
            insertedNode = parentN.splitNode(elmtNN, Node.NOT_DEFINED);
          }
        }
        return adjustTree(insertedNode, slotIndex);
      }
    }
    catch(Exception e){
      e.printStackTrace();
      throw new  RTreeException("RTree.adjustTree: "+e.getMessage());
    }
  }
  /**
     Pass a <code>LeafElement</code>object.
  */
  public void insert(Element elmt)//Leaf
    throws  RTreeInsertException
  {
    fileHdr.lockWrite();
    Node node = null;
    try{
      //the node into which to insert
      node = chooseLeaf(elmt);
      //System.out.println("Returned:"+node.getNodeIndex());
      Node[] newNodes = new Node[2];
      try{
        node.insertElement(elmt);//if another insert is possible
        newNodes[0] = node;
        newNodes[1] = null;
      }
      //if another insert is not possible
      catch(NodeFullException e){
        newNodes = node.splitNode(elmt, Node.NOT_DEFINED);
      }
      adjustTree(newNodes, Node.NOT_DEFINED);
    }
    catch(Exception e){
      //e.printStackTrace();
      throw new RTreeInsertException("RTree.insert: " + e.getMessage() + " for " +
                                     Thread.currentThread());
    }
    finally{
      fileHdr.unlock();
    }
  }
  /**
     See <b>Guttman the Great</b>.
  */
  private Node chooseLeaf(Element elmt)
    throws  IllegalValueException, RTreeException
  {
    try{
      //get the root node
      long root = fileHdr.getRootIndex();
      Node node = null;
      if(fileHdr.isWriteThr())
        node = new Node(fileHdr.getFile(), fileName,root,fileHdr);
      else
        node = chdNodes.getNode(fileHdr.getFile(), fileName,root,fileHdr);
      switch (node.getElementType()){
      case Node.LEAF_NODE :
        break;
      case Node.NONLEAF_NODE :
        //repeat till you reach a leaf node
        while(true){
          //get the best fitting rect from the node
          Element nextElmt = node.getLeastEnlargement(elmt);
          if(nextElmt.getElementType() == Node.LEAF_NODE)
            break;
          if(fileHdr.isWriteThr())
            node = new Node(fileHdr.getFile(), fileName, nextElmt.getPtr(), fileHdr);
          else
            node = chdNodes.getNode(fileHdr.getFile(), fileName, nextElmt.getPtr(), fileHdr);
        }
        break;
      default :
        throw new IllegalValueException("RTree.chooseLeaf: Node corrupt, Illegal element type in "
                                        +"node");
      }
      return node;
    }
    catch( IllegalValueException e){
      throw new  IllegalValueException("RTree.chooseLeaf: "+e.getMessage());
    }
    catch(Exception e){
      e.printStackTrace();
      throw new  RTreeException("RTree.chooseLeaf: " + e.getMessage());
    }
  }
  /**
     given the leaf element, this method deletes the element from the tree
     compairing its Rect and pointer with the similar entries in the tree.
     @throws ElementNotException when the given element is not found.
  */
  public void delete(LeafElement elmt)
    throws  RTreeException,ElementNotFoundException
  {
    fileHdr.lockWrite();
    long root;
    if(elmt ==  null)
      throw new  RTreeException("RTree.delete: Rect is null");
    try{
      if(fileHdr.isWriteThr())
        chdNodes.removeAll();
      root = fileHdr.getRootIndex();//FileHdr.getRootIndex(fileName);
      //find the leaf that contains the element
      Node delNode;
      if(fileHdr.isWriteThr())
        delNode = findLeaf(new Node(fileHdr.getFile(),fileName,root,fileHdr),elmt);
      else
        delNode = findLeaf(chdNodes.getNode(fileHdr.getFile(),fileName,root,fileHdr),elmt);
      //if found...
      if(delNode != null){
        Element[] elmts = delNode.getAllElements();//all the elements
        int totElmts = delNode.getTotalElements();//total elements
        //index of the desired element
        int childIndex = Node.NOT_DEFINED;
        //find the index of the element that has to be deleted
        for(int i=0;i<totElmts;i++){
          if(elmts[i].getRect().encloses(elmt.getRect()) && (elmts[i].getPtr() == elmt.getPtr())){
            childIndex = i;
            break;
          }
        }
        //strange, but the element is not found....impossible!
        if(childIndex == Node.NOT_DEFINED)
          throw new ElementNotFoundException("RTree.delete: Element not in tree");
        delNode.deleteElement(childIndex, false);
        Stack stack = new Stack();
        condenseTree(delNode,stack);
        //find the root
        Node rootNode;
        if(fileHdr.isWriteThr())
          rootNode = new Node(fileHdr.getFile(),fileName,fileHdr.getRootIndex(),
                              fileHdr);
        else
          rootNode = chdNodes.getNode(fileHdr.getFile(),fileName,fileHdr.getRootIndex(),
                                      fileHdr);
        //if root has only one element then make the node pointed by the
        //element as the new root
        if((rootNode.getTotalElements() == 1) && (rootNode.getElementType() == Node.NONLEAF_NODE)){
          long childPtr= rootNode.getElement(0).getPtr();
          Node child;
          if(fileHdr.isWriteThr())
            child = new Node(fileHdr.getFile(),fileName, childPtr,fileHdr);
          else
            child = chdNodes.getNode(fileHdr.getFile(),fileName, childPtr,fileHdr);
          //set as the new root
          child.setParent(Node.NOT_DEFINED);
          //delete the original parent
          rootNode.deleteNode();
        }
      }
      else
        throw new ElementNotFoundException("RTree.delete: Element not in tree");
    }
    catch(Exception e){
      if(e instanceof ElementNotFoundException)
        throw (ElementNotFoundException)e;
      throw new  RTreeException("RTree.delete: "+e.getMessage());
    }
    finally{
      fileHdr.unlock();
    }
  }

  private void condenseTree(Node node,Stack stack)
    throws Exception
  {
    //is the parent node
    if(node.getParent() == Node.NOT_DEFINED){
      while(!stack.empty()){
        Node nd = (Node)stack.pop();
        List v = trvsRPost(nd,true);
        for(int i=0;i<v.size();i++)
          insert((LeafElement)v.get(i));
      }
      return;
    }
    //get the parent
    Node parentN;
    if(fileHdr.isWriteThr())
      parentN = new Node(fileHdr.getFile(),fileName,node.getParent(),fileHdr);
    else
      parentN = chdNodes.getNode(fileHdr.getFile(),fileName,node.getParent(),fileHdr);
    //get the parent element of 'node'.
    //Integer intValue = new Integer(node.getNodeIndex());
    int parentElmtIdx = parentN.getElementIndex(node.getNodeIndex()/*intValue*/);
    //delete parent element if node has less than 'm' elements
    if(node.getTotalElements() < Node.MIN){
      parentN.deleteElement(parentElmtIdx, false);
      stack.push(node);
    }
    else{//if ok
      parentN.modifyElement(parentElmtIdx,node.getNodeMBR());
    }
    condenseTree(parentN,stack);
  }

  /**Basically it returns the node that <b>may</b> contain the required element.
   */
  private Node findLeaf(Node node,LeafElement elmt)
    throws  IllegalValueException,FileNotFoundException,IOException, NodeReadException, NodeWriteException
  {
    if((node == null) || (elmt == null))
      throw new  IllegalValueException("RTree.findLeaf: Node is null");
    Node nd = null;
    Element[] allElmt = node.getAllElements();
    int totElements = node.getTotalElements();
    //Integer elmtPtr = (Integer)elmt.getPtr();
    for(int i=0; i<totElements; i++){//for every element
      //select elements that overlap
      if(!allElmt[i].getRect().disjoint(elmt.getRect())){//intersect
        //Integer nxtIndex = (Integer)allElmt[i].getPtr();
        if(allElmt[i].getElementType() == Node.NONLEAF_NODE){//non leaf
          if(fileHdr.isWriteThr())
            nd = findLeaf(new Node(fileHdr.getFile(),fileName, allElmt[i].getPtr(),fileHdr),elmt);
          else
            nd = findLeaf(chdNodes.getNode(fileHdr.getFile(),fileName, allElmt[i].getPtr(),fileHdr),elmt);
          if(nd != null)
            return nd;
        }
        else{
          //if any of the leaf element matches then check for exact
          //match with passed element, also check for the pointer
          //value so that elements are excactly match.
          if(allElmt[i].getRect().equals(elmt.getRect()) && (allElmt[i].getPtr() == elmt.getPtr()))
            return node;
        }
      }
    }
    return null;
  }
  //-----------------------------------------------------------------------
  /**
   * Find all index records whose MBR overlap a search MBR 'rect'. - <b>Guttman the Great</b>.
   * An vector is returned. The tree is traversed recusively in post order.
   * @return If the file is empty or search rect. is out of scope then the method returns a zero size vector.
   */
  public List overlaps(Rect rect)
    throws  RTreeException,FileNotFoundException
  {
    fileHdr.lockRead();
    //System.out.println("RTree.overlaps : in for thread " + Thread.currentThread().toString());
    long root;
    if(rect ==  null)
      throw new  RTreeException("RTree.overlaps: Rect is null");
    root = fileHdr.getRootIndex();
    try{
      Node node = chdNodes.getReadNode(fileHdr.getFile(),fileName,root, fileHdr);
      List ret = getRPostOvrlap(node, rect);
      return ret;
      //return(getRPostOvrlap(chdNodes.getNode(fileHdr.getFile(),fileName,root, fileHdr), rect));
    }
    catch(Exception e){
      throw new RTreeException("RTree.overlaps: "+e.getMessage());
    }
    finally{
      //System.out.println("RTree.overlaps : out for thread " + Thread.currentThread().toString());
      fileHdr.unlock();
    }
  }
  /**
     Given any node it traverses a tree in a recursive post order manner
     fetching all overlaping elements in the leaves.
  */
  private List getRPostOvrlap(Node node, Rect rect)
    throws  IllegalValueException,FileNotFoundException,NodeReadException,IOException, NodeWriteException
  {
    if((node == null) || (rect == null))
      throw new  IllegalValueException("RTree.getRPostOvrlap: Node is null");
    List list = new ArrayList();
    //System.out.println("Nodes visited:"+node.getNodeIndex());
    Element[] elmts = node.getAllElements();
    int totElements = node.getTotalElements();
    for(int i=0; i<totElements; i++){//for every element; we can use sweepline algorithm here
      if(elmts[i].getRect().overlaps(rect)){ //select elements that overlap
        if(elmts[i].getElementType() == Node.NONLEAF_NODE){//non leaf
          //Integer ndIndex = (Integer)elmts[i].getPtr();
          list.addAll(getRPostOvrlap(chdNodes.getReadNode(fileHdr.getFile(),fileName,elmts[i].getPtr(),
                                                          fileHdr),rect));
        }else{//if leaf element
          list.add(elmts[i]);
        }
      }
    }
    return list;
  }
  public List overlapsSweep(Rect rect)
    throws  RTreeException,FileNotFoundException
  {
    fileHdr.lockRead();
    long root;
    if(rect ==  null)
      throw new  RTreeException("RTree.overlaps: Rect is null");
    root = fileHdr.getRootIndex();
    try{
      return getRPostOvrlapSweep(chdNodes.getReadNode(fileHdr.getFile(),fileName,root, fileHdr), rect);
    }
    catch(Exception e){
      e.printStackTrace();
      throw new  RTreeException("RTree.overlaps: "+e.getMessage());
    }
    finally{
      fileHdr.unlock();
    }
  }
  private List getRPostOvrlapSweep(Node node, Rect rect)
    throws  IllegalValueException,FileNotFoundException,NodeReadException,IOException, NodeWriteException
  {
    if((node == null) || (rect == null))
      throw new  IllegalValueException("RTree.getRPostOvrlap: Node is null");
    List list = new ArrayList();
    //System.out.println("Nodes visited:"+node.getNodeIndex());
    Element[] elmts = node.getAllElements();
    SweepLine spLine = new SweepLine(new IntersectPred());
    //System.out.println("RTree.getRPostOvrlapSweep : calling sweep");
    List pairs = spLine.intersects(rect, elmts);
    //System.out.println("RTree.getRPostOvrlapSweep : called..and now for every pair");

    for(int i=0; i<pairs.size(); i++){//for every element; we can use sweepline algorithm here
      Element intElmt = ((PairElmt)pairs.get(i)).getRtElmt();
      if(intElmt.getElementType() == Node.NONLEAF_NODE){//non leaf
        list.addAll(getRPostOvrlapSweep(chdNodes.getReadNode(fileHdr.getFile(),fileName,intElmt.getPtr(),
                                                             fileHdr),rect));
        //System.out.println("RTree.getRPostOvrlapSweep : total objects NONLEAF " + list.size());
      }
      else{//if leaf element
        list.add(intElmt);
        //System.out.println("RTree.getRPostOvrlapSweep : total objects LEAF " + list.size());
      }
    }
    //System.out.println("RTree.getRPostOvrlapSweep : checked pairs as well");
    return list;
  }
  //-----------------------------------------------------------------------
  /**
     Find all index records whose MBR intsersect a search MBR 'rect'.
     The diff. betn. this method and 'overlap' method is that this method returns
     all rectangle that is in any way in touch with the test rectangle 'rect'.
     In method 'overlap' only the rects that have common area with 'rect'
     are returned but unlike this method it does not return rects that have
     common side with 'rect'.
     An vector is returned. The tree is traversed recusively in post order.
     @return If the file is empty or search rect. is out of scope then the method returns a zero size vector.
  */
  public List nonDisjoint(Rect rect)
    throws  RTreeException,FileNotFoundException
  {
    fileHdr.lockRead();
    long root;
    if(rect ==  null)
      throw new  RTreeException("RTree.nonDisjoint: Rect is null");
    root = fileHdr.getRootIndex();
    try{
      return(getRPostIntsect(chdNodes.getReadNode(fileHdr.getFile(),fileName,root,fileHdr), rect));
    }
    catch(Exception e){
      throw new  RTreeException("RTree.nonDisjoint: "+e.getMessage());
    }
    finally{
      fileHdr.unlock();
    }
  }
  /**
     Given any node it traverses a tree in a recursive post order manner
     fetching all intersecting elements in the leaves.
  */
  private List getRPostIntsect(Node node, Rect rect)
    throws  IllegalValueException,FileNotFoundException,NodeReadException,IOException, NodeWriteException
  {
    if((node == null) || (rect == null))
      throw new  IllegalValueException("RTree.getRPostIntsect: Node is null");
    List list = new ArrayList();
    //System.out.println("Nodes visited:"+node.getNodeIndex());
    Element[] elmts = node.getAllElements();
    int totElements = node.getTotalElements();
    for(int i=0; i<totElements; i++){//for every element
      if(!elmts[i].getRect().disjoint(rect)){//select elements that overlap
        if(elmts[i].getElementType() == Node.NONLEAF_NODE){//non leaf
          //Integer ndIndex = (Integer)elmts[i].getPtr();
          list.addAll(getRPostIntsect(chdNodes.getReadNode(fileHdr.getFile(),fileName,elmts[i].getPtr(),
                                                           fileHdr),rect));
        }
        else{//if leaf element
          list.add(elmts[i]);
        }
      }
    }
    return list;
  }

  //-----------------------------------------------------------------------
  /**Find all index records whose MBR are enclosed by the MBR 'rect'.
     An Vector is returned. The tree is traversed recusively in post order.
     @return If the file is empty or search rect. is out of scope then the
     method returns a zero size vector.
  */
  public List containedBy(Rect rect)
    throws  RTreeException,FileNotFoundException
  {
    fileHdr.lockRead();
    long root;
    if(rect ==  null)
      throw new  RTreeException("RTree.containedBy: Rect is null");
    root = fileHdr.getRootIndex();
    try{
      return(getRPostContBy(chdNodes.getReadNode(fileHdr.getFile(),fileName,root,fileHdr), rect));
    }
    catch(Exception e){
      throw new  RTreeException("RTree.containedBy: "+e.getMessage());
    }
    finally{
      fileHdr.unlock();
    }
  }
  /**
     Given any node it traverses a tree in a recursive post order manner
     fetching all the enclosed(inside 'rect') elements in the leaves.
  */
  private List getRPostContBy(Node node, Rect rect)
    throws  IllegalValueException,FileNotFoundException,NodeReadException,IOException, NodeWriteException
  {
    if((node == null) || (rect == null))
      throw new  IllegalValueException("RTree.getRPostContBy: Node is null");
    List list = new ArrayList();
    Element[] elmts = node.getAllElements();
    int totElements = node.getTotalElements();
    //System.out.println("Nodes visited:"+node.getNodeIndex());
    for(int i=0; i<totElements; i++){//for every element  //encloses
      if(rect.overlaps(elmts[i].getRect())){//select elements that overlap
        if(elmts[i].getElementType() == Node.NONLEAF_NODE){//non leaf
          //Integer ndIndex = (Integer)elmts[i].getPtr();
          list.addAll(getRPostContBy(chdNodes.getReadNode(fileHdr.getFile(),fileName,elmts[i].getPtr(),
                                                          fileHdr),rect));
        }
        else{//if leaf element
          if(elmts[i].getRect().containedBy(rect))
            list.add(elmts[i]);
        }
      }
    }
    return list;
  }
  //-----------------------------------------------------------------------
  /**Find all index records whose MBR are geometrically equal to MBR 'rect'
     An Vector is returned. The tree is traversed recusively in post order.
     @return If the file is empty or search rect. is out of scope then the
     method returns a zero size vector.
  */
  public List equal(Rect rect)
    throws  RTreeException,
    FileNotFoundException
  {
    fileHdr.lockRead();
    long root;
    if(rect ==  null)
      throw new  RTreeException("RTree.equal: Rect is null");
    root = fileHdr.getRootIndex();
    try{
      return(getRPostEqual(chdNodes.getReadNode(fileHdr.getFile(),fileName,root,fileHdr), rect));
    }
    catch(Exception e){
      throw new  RTreeException("RTree.equal: "+e.getMessage());
    }
    finally{
      fileHdr.unlock();
    }
  }
  /**
     Given any node it traverses a tree in a recursive post order manner
     fetching all the enclosed(inside 'rect') elements in the leaves.
  */
  private List getRPostEqual(Node node, Rect rect)
    throws  IllegalValueException,FileNotFoundException,NodeReadException,IOException, NodeWriteException
  {
    if((node == null) || (rect == null))
      throw new  IllegalValueException("RTree.getRPostEqual: Node is null");
    List list = new ArrayList();
    Element[] elmts = node.getAllElements();
    int totElements = node.getTotalElements();
    //System.out.println("Nodes visited:"+node.getNodeIndex());
    for(int i=0; i<totElements; i++){//for every element  //encloses
      if(rect.overlaps(elmts[i].getRect())){//select elements that overlap
        if(elmts[i].getElementType() == Node.NONLEAF_NODE){//non leaf
          //Integer ndIndex = (Integer)elmts[i].getPtr();
          list.addAll(getRPostEqual(chdNodes.getReadNode(fileHdr.getFile(),fileName,elmts[i].getPtr(),
                                                         fileHdr),rect));

        }
        else{//if leaf element
          if(elmts[i].getRect().equals(rect))
            list.add(elmts[i]);
        }
      }
    }
    return list;
  }
  //-----------------------------------------------------------------------
  /**Find all index records whose MBR meet on the sides of MBR 'rect'.
     An Vector is returned. The tree is traversed recusively in post order.
     @return If the file is empty or search rect. is out of scope then the
     method returns a zero size vector.
  */
  public List meet(Rect rect)
    throws  RTreeException,FileNotFoundException
  {
    fileHdr.lockRead();
    long root;
    if(rect ==  null)
      throw new  RTreeException("RTree.meet: Rect is null");
    root = fileHdr.getRootIndex();
    try{
      return(getRPostMeet(chdNodes.getReadNode(fileHdr.getFile(),fileName,root,fileHdr),  rect));
    }
    catch(Exception e){
      throw new  RTreeException("RTree.meet: "+e.getMessage());
    }
    finally{
      fileHdr.unlock();
    }
  }
  /**
     Given any node it traverses a tree in a recursive post order manner
     fetching all the enclosed(inside 'rect') elements in the leaves.
  */
  private List getRPostMeet(Node node,Rect rect)
    throws  IllegalValueException, FileNotFoundException,NodeReadException,IOException, NodeWriteException
  {
    if((node == null) || (rect == null))
      throw new  IllegalValueException("RTree.meet: Node is null");
    List list = new ArrayList();
    Element[] elmts = node.getAllElements();
    int totElements = node.getTotalElements();
    //System.out.println("Nodes visited:"+node.getNodeIndex());
    for(int i=0; i<totElements; i++){//for every element  //encloses
      if(!rect.disjoint(elmts[i].getRect())){//select elements that overlap
        if(elmts[i].getElementType() == Node.NONLEAF_NODE){//non leaf
          //Integer ndIndex = (Integer)elmts[i].getPtr();
          list.addAll(getRPostMeet(chdNodes.getReadNode(fileHdr.getFile(),fileName, elmts[i].getPtr() ,
                                                        fileHdr),rect));
        }
        else{//if leaf element
          if(elmts[i].getRect().meet(rect))
            list.add(elmts[i]);
        }
      }
    }
    return list;
  }
  //----------------------------------------------------------------------
  /**Find all index records whose MBR enclose/contain the MBR 'rect'.
     An Vector is returned. The tree is traversed recusively in post order.
     @return If the file is empty or search rect. is out of scope then the
     method returns a zero size vector.
  */
  public List contains(Rect rect)
    throws  RTreeException,FileNotFoundException
  {
    fileHdr.lockRead();
    long root;
    if(rect ==  null)
      throw new  RTreeException("RTree.contains: Rect is null");
    root = fileHdr.getRootIndex();
    try{
      return(getRPostContains(chdNodes.getReadNode(fileHdr.getFile(),fileName,root,fileHdr),rect));
    }
    catch(Exception e){
      throw new  RTreeException("RTree.contains: " +e.getMessage());
    }
    finally{
      fileHdr.unlock();
    }
  }
  /**
     Given any node it traverses a tree in a recursive post order manner
     fetching all the enclosed(inside 'rect') elements in the leaves.
  */
  private List getRPostContains(Node node, Rect rect)
    throws  IllegalValueException,FileNotFoundException,NodeReadException,IOException, NodeWriteException
  {
    if((node == null) || (rect == null))
      throw new  IllegalValueException("RTree.getRPostContains: Node is null");
    List list = new ArrayList();
    Element[] elmts = node.getAllElements();
    int totElements = node.getTotalElements();
    //System.out.println("Nodes visited:"+node.getNodeIndex());
    for(int i=0; i<totElements; i++){//for every element  //encloses
      if(rect.overlaps(elmts[i].getRect())){//select elements that overlap
        if(elmts[i].getElementType() == Node.NONLEAF_NODE){//non leaf
          //Integer ndIndex = (Integer)elmts[i].getPtr();
          list.addAll(getRPostContains(chdNodes.getReadNode(fileHdr.getFile(),fileName,elmts[i].getPtr(),
                                                            fileHdr),rect));
        }
        else{//if leaf element
          if(elmts[i].getRect().contains(rect))
            list.add(elmts[i]);
        }
      }
    }
    return list;
  }
  //-----------------------------------------------------------------------
  /**
     Returns all the elements traversing the tree recursively in <b>postorder</b>
  */
  public List getAllElements()
    throws  RTreeException, FileNotFoundException
  {
    //fileHdr.enter(Node.READ);
    fileHdr.lockRead();
    //System.out.println("RTree.getAllElements : in for thread " + Thread.currentThread().toString());
    long root;
    root = fileHdr.getRootIndex();
    try{
      return(trvsRPost(chdNodes.getReadNode(fileHdr.getFile(),fileName,root,fileHdr),false));
    }
    catch(Exception e){
      //e.printStackTrace();
      throw new RTreeException("RTree.getAllElements: " +e.getMessage());
    }
    finally{
      //fileHdr.leave();
      //System.out.println("RTree.getAllElements : out for thread " + Thread.currentThread().toString());
      fileHdr.unlock();
    }
  }
  /**
     Traverses the tree recursively in post order.
     The second parameter is for the delete method. As it goes through the nodes
     returning the leafelements it also deletes the nodes.
     This feature is not required for any other methods hence set <code>del</code> as
     <code>false</code> for all other cases.
  */
  private List trvsRPost(Node node,boolean del)
    throws  IllegalValueException,FileNotFoundException,NodeReadException,IOException, NodeWriteException
  {
    if((node == null))
      throw new  IllegalValueException("RTree.getRPostOvrlap: Node is null");
    List list = new ArrayList();
    Element[] elmts = node.getAllElements();
    int totElements = node.getTotalElements();
    //System.out.println("Nodes visited:"+node.getNodeIndex())
    for(int i=0; i<totElements; i++){//for every element
      //non leaf
      if(elmts[i].getElementType()==Node.NONLEAF_NODE){
        //Integer ndIndex = (Integer)elmts[i].getPtr();
        list.addAll(trvsRPost(chdNodes.getReadNode(fileHdr.getFile(),fileName,elmts[i].getPtr(), fileHdr),
                              del));
      }
      else{//if leaf element
        list.add(elmts[i]);
      }
    }
    if(del)
      node.deleteNode();
    return list;
  }
  //-------------------------------------------------------------------
  /**Prints the tree in recursively <b>preorder</b> manner.
   */
  public void printTree()
    throws  RTreeException,FileNotFoundException
  {
    fileHdr.lockRead();
    long root;
    root = fileHdr.getRootIndex();
    try{
      trvsRPrePrint(chdNodes.getReadNode(fileHdr.getFile(),fileName,root,fileHdr));
    }
    catch(Exception e){
      throw new  RTreeException("RTree.printTree: "+e.getMessage());
    }
    finally{
      fileHdr.unlock();
    }
  }
  /**
     This method will return MBR of the whole tree.
  */
  public Rect getTreeMBR()
  {
    fileHdr.lockRead();
    long root;
    try{
      root = fileHdr.getRootIndex();
      Node node = chdNodes.getReadNode(fileHdr.getFile(),fileName,root,fileHdr);
      return node.getNodeMBR();
    }
    catch(Exception e){
      return null;
    }
    finally{
      fileHdr.unlock();
    }
  }
  public void deleteAllElements()
    throws RTreeException
  {
    fileHdr.lockWrite();
    try{
      chdNodes.removeAll();
      fileHdr.resetHeader();
    }catch(Exception e){
      throw new RTreeException("RTree.deleteAllElements : " + e.getMessage());
    }
    finally{
      fileHdr.unlock();
    }
  }

  /**
     Traverses the tree recursively in post order.
  */
  private void trvsRPrePrint(Node node)
    throws  IllegalValueException,
    FileNotFoundException,NodeReadException,
    IOException,NodeWriteException
  {
    if((node == null))
      throw new  IllegalValueException("RTree.trvsRPrePrint: Node is null");
    Element[] elmts = node.getAllElements();
    int totElements = node.getTotalElements();
    System.out.println(node.toString());
    for(int i=0; i<totElements; i++){//for every element
      if(elmts[i].getElementType() == Node.NONLEAF_NODE){//non leaf
        //Integer ndIndex = (Integer)elmts[i].getPtr();
        trvsRPrePrint(chdNodes.getReadNode(fileHdr.getFile(),fileName,elmts[i].getPtr(),fileHdr));
      }
    }
    return;
  }
  //-----------------------------------------------------------------------
  /**
     <b>Read Me Well.</b><br>
     The Nearest Neighbour(NN) search from Roussopoulos and Cheung.
     <br>The returned leaf elements are sorted in ascending order of their
     differences from the query point. <br><b>If the no. of leaf elements found
     are less then <code>n</code>, then the rest of the values in the returend array
     are null.<br></b>Give the limit(distance) within which you want to search.
     The limit is in the same unit as the coordinates of the MBRs. <p>There may
     be some objects in the tree which come within the region but are so big
     that their size makes them extend much beyond the given region.
     Such objects would also be considered.
     If you do not want any limit then give <code>Long.MAX_VALUE</code> in <code>range</code>.
     You can also search for presence of any object at the given point by giving
     <code>range</code> as <code>0</code>.
     Also required are the no. of objects that need to be fetched(N-Nearest objects).
     <br><b>The value of <code>range</code> is actually square of the distance you want.
     Therefor if you want to search in an area of 10cm, give the <code>range</code> as 100.</b>
     @param pt the query point.
     @param range the region within which you want to search.
     @param n the number of objects required.
     @return the leaf elements found near the point.The length of the returned
     array would be equal to <code>n</code>.
  */
  public ABL[] nearestSearch(Point pt,long range,int n)
    throws  RTreeException, IllegalValueException
  {
    fileHdr.lockRead();
    if((pt == null) || (range < 0) || (n <= 0))
      throw new  IllegalValueException("RTree.nearestSearch: Illegal arguments");
    try{
      long root = fileHdr.getRootIndex();
      ABL[] elmts = new ABL[n];
      Nearest nrstDist = new Nearest();
      nrstDist.value = range;
      return INNSearch(chdNodes.getReadNode(fileHdr.getFile(),fileName,root,fileHdr),pt, elmts,nrstDist);
    }
    catch( IllegalValueException e){
      throw new IllegalValueException(e.getMessage());
    }
    catch(Exception e){
      throw new RTreeException("RTree.nearestSearch: " +e.getMessage());
    }
    finally{
      fileHdr.unlock();
    }
  }
  /**
     Improved Nearest Neighbour Search - Cheung, theory Roussopoulos
  */
  private ABL[] INNSearch(Node node, Point pt,ABL[] nrstElements,Nearest nrstDist)
    throws  IllegalValueException,FileNotFoundException,IOException,NodeReadException,
    RTreeException,NodeWriteException
  {
    if((node == null))
      throw new  IllegalValueException("RTree.INNSearch: Node is null");
    Element[] elmts = node.getAllElements();
    int totElements = node.getTotalElements();

    if(totElements == 0)//no elements
      return null;
    //System.out.println("In Node:"+node.getNodeIndex());
    //if leaf
    if(node.getElementType() == Node.LEAF_NODE){
      //at leaf level compute distance to actual objects
      for(int i=0; i<totElements; i++){
        long lfDist = Rect.minDist(pt,elmts[i].
                                   getRect());
        if(lfDist <= nrstDist.value){//assign the new nearest value
          //Integer index = (Integer)elmts[i].getPtr();
          ABL newElmt =  new ABL(new LeafElement(elmts[i].getRect(),elmts[i].getPtr()), lfDist);
          insertArray(nrstElements,newElmt,nrstDist);
        }
      }
      return nrstElements;
    }
    //if non-leaf
    else{
      //the Active Branch List
      ABL[] abl = new ABL[totElements];
      //calculate MINDIST and assign to the ABL array
      for(int i=0; i<abl.length; i++){
        //Integer ndIndex = (Integer)elmts[i].getPtr();
        abl[i] = new ABL(new NonLeafElement(elmts[i].getRect(),elmts[i].getPtr()),
                         Rect.minDist(pt,elmts[i].getRect()));
      }
      //sort the ABL
      abl[0].mergeSort(abl);
      //upward prun the branch
      for(int i=0; i<abl.length; i++){
        if(abl[i].minDist <= nrstDist.value){
          //Integer ndIndex = (Integer)abl[i].element.getPtr();
          nrstElements = INNSearch(chdNodes.getReadNode(fileHdr.getFile(),fileName,elmts[i].getPtr(),
                                                        fileHdr),pt,nrstElements,nrstDist);
        }
      }
      return nrstElements;
    }//else
  }//INNSearch
  /**
     A utility method for the search algorithm that inserts an element into the
     the correct position and adjusts the array accordingly.
     Assumes that the new 'element' is lesser than the last element of 'arr'.
  */
  protected void insertArray(ABL[] arr,ABL elmt,Nearest nrstDist)
    throws  RTreeException
  {
    try{
      ABL temp=null,temp1=null;
      //int nNearest = 0;
      int i=arr.length-1;
      //if the first or the only element to enter
      if((arr[0] == null) || (arr.length == 1))
        arr[0] = (ABL)elmt.clone();
      else{
        while((i < arr.length) && (i >= 0)){
          if(arr[--i] != null){
            if((arr[i].minDist <= elmt.minDist) ||
               ((i == 0)&&(elmt.minDist<arr[i].minDist))){//special case
              if((i==0)&&(elmt.minDist<arr[i].minDist))//special case
                i--;
              temp = elmt;//the element to insert
              //shift all elements one place right
              while(++i < (arr.length)){
                if(arr[i] != null){//if next is not null
                  temp1 = arr[i];//backup
                  arr[i] = (ABL)temp.clone();//replace
                  temp = temp1;//for the next loop
                }
                else{
                  arr[i] = (ABL)temp.clone();
                  break;
                }
              }
              break;
            }
          }
        }
      }

      //if last element is null(arr is not full) then that means that more
      //elements can be accepted in the given region. Thus do not change
      //the min. distance. Else the last element will be the min. - every
      //element that comes after must have a value less than that.
      if(arr[arr.length-1] != null)
        nrstDist.value = arr[arr.length-1].minDist;
    }
    catch(Exception e){
      throw new  RTreeException("RTree.insertArray: "+e.getMessage());
    }
  }
  //-------------------------------------------------------------------------
  /**Another version of the <code>nearestSearch</code> method(not overloaded). This
     method finds all the objects within the given range.<b> To understand
     this method please refer to the other <code>nearestSearch</code> method.</b>
     <br>When the no. of objects required is less then a few thousands, this
     method would be many <b>times</b> slower than the above method.
     If possible use the other method.
     @return vector of ABL objects within the given range.
  */
  public List nearestSearch(Point pt,long range)
    throws  RTreeException, IllegalValueException
  {
    fileHdr.lockRead();
    if((pt == null) || (range < 0))
      throw new  IllegalValueException("RTree.nearestSearch: "
                                       +"Point null or int less than one");
    try{
      long root = fileHdr.getRootIndex();
      List elmts = new ArrayList();
      elmts = INNSearch(chdNodes.getReadNode(fileHdr.getFile(),fileName,root,fileHdr),pt,elmts,range);
      return elmts;
    }
    catch( IllegalValueException e){
      throw new  IllegalValueException(e.getMessage());
    }
    catch(Exception e){
      throw new  RTreeException("RTree.nearestSearch: " + e.getMessage());
    }
    finally{
      fileHdr.unlock();
    }
  }
  private List INNSearch(Node node, Point pt, List  nrstElements,long nrstDist)
    throws  IllegalValueException, FileNotFoundException,IOException,NodeReadException, Exception
  {
    if((node == null))
      throw new  IllegalValueException("RTree.INNSearch: Node is null");
    Element[] elmts = node.getAllElements();
    int totElements = node.getTotalElements();

    if(totElements == 0)//no elements
      return null;
    //System.out.println("In Node:"+node.getNodeIndex());
    //if leaf
    if(node.getElementType() == Node.LEAF_NODE){
      //at leaf level compute distance to actual objects
      for(int i=0; i<totElements; i++){
        long lfDist = Rect.minDist(pt,elmts[i].
                                   getRect());
        if(lfDist <= nrstDist){//assign the new nearest value
          //Integer index = (Integer)elmts[i].getPtr();
          ABL newElmt =  new ABL(new LeafElement(elmts[i].getRect(),elmts[i].getPtr()),lfDist);
          //insert into vector
          int j=0;
          //the following line is doubtful
          for(j = 0;
              (j < nrstElements.size()) &&
                (((ABL)(nrstElements.get(j))).minDist<=newElmt.minDist);
              j++);
          nrstElements.add(j, newElmt);
        }
      }
      return nrstElements;
    }
    //if non-leaf
    else{
      //the Active Branch List
      ABL[] abl = new ABL[totElements];
      //calculate MINDIST and assign to the ABL array
      for(int i=0; i<abl.length; i++){
        //Integer ndIndex = (Integer)elmts[i].getPtr();
        abl[i] = new ABL(new NonLeafElement(elmts[i].getRect(),elmts[i].getPtr()),
                         Rect.minDist(pt,elmts[i].getRect()));
      }
      //sort the ABL
      abl[0].mergeSort(abl);
      //upward prun the branch
      for(int i=0; i<abl.length; i++){
        if(abl[i].minDist <= nrstDist){
          //Integer ndIndex = (Integer)abl[i].element.getPtr();
          nrstElements = INNSearch(chdNodes.getReadNode(fileHdr.getFile(),fileName,elmts[i].getPtr(),
                                                        fileHdr), pt,nrstElements,nrstDist);
        }
      }
      return nrstElements;
    }//else
  }//NNSearch

  /**
     this class is a wrapper class for a <code>long</code> value. There is no way to overwrite
     the value contained in the <code>java.lang.Long</code> wrapper class.
  */
  protected class Nearest//a wrapper class for long
  {
    public long value;
  }
  //--------------------------------get height------------------------------
  /**Upto 5 lakh objectes we can have a maximum height of 3
     TODO : Calculate other levels as well. One may need to know whether the tree is packed or unpacked for
     this.
     `*/
  public synchronized int getHeight()
  {
    int totNodes = fileHdr.getTotalNodes();
    if(totNodes <= 1)
      return 1;
    else if(totNodes <= 170)
      return 2;
    else //if((totNodes > (84*84)) && (totNodes < (169*169+1)))
      return 3;
    //    else
    //return 4;
  }
}
/* TODO:
   Immediate Improvements
   1)Take the common code from each query methods and put them in a single method.
   2)Make a way of retuning a Integer objects other than LeafElement Objects.
*/
