//Pack.java
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
import java.io.*;
import java.util.*;
/**
   Modified again on 9/1/2003
   This class can now be used Pack rtrees at run time i.e an rtree object can now be packed and later used
   without recreating the rtree.
   <p>Original comments
   A utility class that packs a rtree.
   <br>Pack is a special utility class. This class can also be used to maintain
   a R-Tree after many insertions and deletions.(Just like defragmentation 
   in windows).
   <p><b> Never create more than one instance of this class.
   <br>Never run the methods of this class in any other thread except the main
   thread and make sure it is the only thread running.
   <br>Positively remember to reinitialise all the rtree objects after you
   call <code>packTree</code>.</b>
   @author Prachuryya Barua
*/
public class Pack
{
  public Pack(){};

  /**
     Added this new method that takes a list of <code>Element</code>s and builds a
  */
  public synchronized int packTree(List elmts, String newFile)
  {
    try{
      if(elmts.size() <= Node.MAX){
        RTree rtree = new RTree(newFile);
        for(int i=0; i<elmts.size(); i++)
          rtree.insert((LeafElement)elmts.get(i));
        rtree.flush();
        return 0;
      }
      return packTree((Element[])elmts.toArray(new Element[elmts.size()]), new RTree(newFile), newFile);
    }catch(Exception e){
      e.printStackTrace();
      return 2;
    }
  }
  /**
     Sort-Tile-Recursive(STR) packing algo. by  Leutenegger.
     <p><b>**FLUSH THE RTREE BEFORE CALLING**</b>
     <br>Prepocess the file and sort the rectangles
     <br>Load into file
     <br>Recursively pack above MBRs to nodes at the next level.
     <br>If you give the new file name same as the old one then the old would
     be overwritten. One word of caution, whichever new file name you give, it
     would be overwritten.
     @param rtree the rtree object to pack
     @param newFile the new rtree file after packing
     @return <b>0</b> if successfully created a new file,
     <br><b>1</b> if there is no need to pack the file, in this case a new 
     file is not created and the old file is left untouched,
     <br> Greater than zero if all fail.
  */
  public synchronized int packTree(RTree rtree,String newFile)
  {
    try{
      if(rtree == null)
        throw new IllegalArgumentException("PackTree.packTree: rtree null");
      List elmts = rtree.getAllElements();
      //RTree.chdNodes.removeAll();
      int ret = packTree((Element[])elmts.toArray(new Element[elmts.size()]), rtree, newFile);
      return ret;
    }catch(Exception e){
      e.printStackTrace();
      return 2;
    }
  }
  
  public final int BUFFER_SIZE = 8192;
  private int packTree(Element[] elmts, RTree rtree, String newFile)
  {
    try{
      //long t = System.currentTimeMillis();
      //rtree.flush();
      File tmpPckFile = File.createTempFile("pack",null);
      RandomAccessFile rFile = new RandomAccessFile(tmpPckFile.getAbsolutePath(),"rw");

      if(newFile.equalsIgnoreCase(rtree.getFileName())){//we need a write lock
        rtree.getFileHdr().lockWrite();
      }
      /*the following is required as we may pack an existing tree.. until we find a way to remove nodes of
        a particular rtree*/
      RTree.chdNodes.removeAll();
      //rtree.getFileHdr().getFile().getFD().sync();
      if(elmts.length <= Node.MAX)//change this for the first method
        return(1);
      System.out.println("Pack.packTree : Size of elmts: "+ elmts.length);

      packRec(rFile,tmpPckFile,elmts,elmts.length);

      //craete the new file
      File fo = new File(newFile); 
      //delete the new file if it exists !!
      if(fo.exists()){
        fo.delete();
        fo.createNewFile();
      }
      //overwrite the old rtree file with the temp file
      FileInputStream fis=new FileInputStream(tmpPckFile);
      FileOutputStream fos=new FileOutputStream(fo);
      byte b[]=new byte[BUFFER_SIZE];
      int i;
      while((i=fis.read(b))!=-1){
        fos.write(b, 0, i);
      }
      fos.close();
      fis.close();
      rFile.close();
      tmpPckFile.deleteOnExit();
      //System.out.println("Pack.packTree : packing took " + (System.currentTimeMillis() - t));
      return(0);
    }
    catch(Exception e){
      e.printStackTrace();
      System.out.println("rtree.RTree.pack: Could not pack rtree, the destination file may be corrupted.");
      return(2);
    }
    finally{//delete the source file header
      synchronized(rtree){
        //Here we have the old and the new file as same.. so we update the header
        try{
          rtree.updateHdr();
          if(newFile.equalsIgnoreCase(rtree.getFileName())){//we need a write lock
            rtree.getFileHdr().unlock();//relaese this lock as this header will be lost for ever
          }
        }catch(Exception e){
          System.out.println("Pack.packTree : The pack tree is made but some other error hs occured. "
                             +"It is recomended to restart the application");
          if(newFile.equalsIgnoreCase(rtree.getFileName()))//we need a write lock
            rtree.getFileHdr().unlock();//relaese this lock as this header will be lost for ever
        }
      }//synchronized
    }
  }
  private void packRec(RandomAccessFile rFile,File tmpPckFile, Element[] elmts,int length)
    throws Exception
  {
    //P the no. of leaf nodes - ceil(objects/max objects per node)
    Double temp = new Double(Node.MAX);//temp
    temp = new Double(Math.ceil(length/temp.doubleValue()));
    //int P = temp.intValue();//leaves
    //no. of vertical slices
    temp = new Double(Math.ceil(Math.sqrt(temp.doubleValue())));
    int S = temp.intValue();
    //System.out.println("total slices: "+S);
    Slice sls[] =  new Slice[S];

    //sort all the rectangles on X axis
    NonLeafElement.twoWayMerge(elmts,0,length-1,0);
        
    //divide into slices
    int start = 0;
    int end;
    for(int i=0; i<S; i++){
      if((start + (S*Node.MAX)) <= length)
        end = start+((S*Node.MAX)-1);
      else
        end = length - 1;
      sls[i] = new Slice(start,end);
      start = end+1;
    }
    //sort each slice on Y axis and write to file
    for(int i=0; i<S; i++)
      NonLeafElement.twoWayMerge(elmts,sls[i].start,sls[i].end,1);
    int newLength = writePckFile(rFile,tmpPckFile,elmts,sls);
    if(newLength == 1)//last insertion was a root
      return;
    packRec(rFile,tmpPckFile,elmts,newLength);
  }
  /**
     Method that handles the low level details of nodes, leaves, file headers etc.
     It writes the details to the rtree file.
     @return the no. of new leaf elements created. The element themselves are in
     the 'elmts' array.
  */
  private int writePckFile(RandomAccessFile rFile,File tmpPckFile,
                           Element[] elmts,Slice[] sls)
    throws Exception
  {
    FileHdr hdr = new FileHdr(Node.FREE_LIST_LIMIT,tmpPckFile.getAbsolutePath());
    hdr.setBufferPolicy(true);
    int length;
    Double totNodes;//total nodes for the slice
    int netNodes = 0;// !?!!
    int l=0;//the position on to which the old elmt. would be replaced
    for(int i=0; i<sls.length; i++){//for each slice
      //cal. the length of this slice
      length = sls[i].end - sls[i].start + 1;
      //calculate the no. of new nodes for the slice
      totNodes = new Double(length);
      totNodes= new Double(Math.ceil(totNodes.doubleValue()/Node.MAX));
      netNodes += totNodes.intValue();
      //make nodes of the elements in the slice 
      for(int j=0; j<totNodes.intValue(); j++){//loop for each new node
        Node node = new Node(rFile,tmpPckFile.getAbsolutePath(), Node.NOT_DEFINED,
                             elmts[sls[i].start].getElementType(), hdr);
        //add elements to the node
        ArrayList list = new ArrayList(Node.MAX);
        for(int k=0; (k<Node.MAX) && (sls[i].start <= sls[i].end); k++){
          //node.insertElement(elmts[sls[i].start++]);
          list.add(elmts[sls[i].start++]);
        }
        node.insertElement((Element[])list.toArray(new Element[list.size()]), true);
        
        //create the new nonleaf element - always nonleaf
        NonLeafElement nlf = new NonLeafElement(node.getNodeMBR(),node.getNodeIndex());
                
        elmts[l++] = (NonLeafElement)nlf.clone();
      }
    }
    return netNodes;
  }
  /**An inner class for the packing method*/
  class Slice
  {
    int start;
    int end;
    Slice(int start,int stop)
    {
      this.start = start;
      this.end = stop;
    }
  }
  /**A wrapper class for int*/
  class Int
  {
    int val;
    Int(int val)
    {
      this.val = val;
    }
  }
}


