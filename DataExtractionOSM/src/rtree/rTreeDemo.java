//RTreeDemo.java
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

import java.io.RandomAccessFile;
import java.util.Random;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import rtree.join.*;
import rtree.seeded.*;
import java.util.List;
import java.util.ArrayList;

/**
	One can look at this class to understand the usage of RTree.
*/
public class rTreeDemo
{
  public static void main(String argv[])
  {
    //rTreeDemo rt = new rTreeDemo();
    new TreeThread();
  }
  public List tryJoin()
  {
    TreeThread tr = new TreeThread();    
    return tr.tryJoin();
  }
  public void setName(String name)
  {
    TreeThread.fileName = name;
  }
}

class TreeThread implements Runnable
{
  static String fileName = "c:\\temp\\temp.tree";
  static long seed = 1015395880;//1015395880431;
  static int h = 15000000;//2148460;//15000000;
  static int w = 6000000;//2757573;//6000000;
  Thread tree;
  //String tname;
  TreeThread()
  {
    for(int i=0; i<1; i++){
      tree = new Thread(this,Integer.toString(i));
      //  try{Thread.currentThread().sleep(3000);}
      //  catch(Exception e){System.out.println("shdb");}
      tree.start();
    }
  }
  public void run()
  {
    try{
      //RTree rt = new RTree(fileName);
      //rt.printTree();
      //  entry(fileName);
      //entryRand(fileName);
      //  rt.flush();
      
      //  HashSet set = new HashSet(list);
      //  System.out.println("rTreeDemo.run : size of set " + set.size());
      
      //rt.printTree();
      //overlapRR(fileName);

      //Pack pck = new Pack();
      //System.out.println("rTreeDemo : rt has before " + rt.getAllElements().size());
      //pck.packTree(rt, fileName);
      //System.out.println("rTreeDemo : rt has after " + rt.getAllElements().size());
      //rt.printTree();
      //pck.packTree(rt, fileName);
      
      //pck.packTree(new RTree(fileName+"2"), fileName+"2");
      //trySeed("/tmp/seed.dat", new RTree(fileName));
      //System.out.println("rTreeDemo : height " + rt.getHeight());
      
      RTreeRead rd = new RTreeRead(fileName+"1");
      rd.readSeq();
      rd = new RTreeRead(fileName+"2");
      rd.readSeq();
       
      //tryJoin();
    }
    catch(Exception e){
      try{
        e.printStackTrace();
      }catch(Exception ex){
        ex.printStackTrace();
      }
    }
    //tryOverlap();
    //tryCvr();
  }
  /**
     Enter records within l - 15000000 and w - 6000000
  */
  public  void entryRand(String fileName)
    throws Exception
  {
    List vct = new ArrayList(0);
    int ix,iy,xx,xy;//mIn,maX
    RTree rt = new RTree(fileName);
    Random rnd = new Random(seed);
    long start = System.currentTimeMillis();
    Rect rect = new Rect();

    //point data
    for(int i=0;i<2000; i++){
      iy = rnd.nextInt(h);//height
      ix = rnd.nextInt(w);//width
      LeafElement lf = new LeafElement(new Rect(ix,iy,ix,iy),218);
      rt.insert(lf);
      //rect.expandToInclude(lf.getRect());
      //vct.add((LeafElement)lf.clone());
    }
    
    //rectangles
    for(int i=0;i<30000; i++){
      iy = rnd.nextInt(h-2);//height
      ix = rnd.nextInt(w-2);//width
      xy = rnd.nextInt(h - iy);
      xx = rnd.nextInt(w - ix);
      LeafElement lf = new LeafElement(new Rect(ix,iy,ix+xx,iy+xy),218);
      rt.insert(lf);
      //rect.expandToInclude(lf.getRect());
      //vct.add((LeafElement)lf.clone());
    }
    
    for(int i=0;i<20000; i++){
      iy = rnd.nextInt(h);//height
      ix = rnd.nextInt(w);//width
      LeafElement lf = new LeafElement(new Rect(ix,iy,ix,iy),218);
      rt.insert(lf);
      //rect.expandToInclude(lf.getRect());
      //vct.add((LeafElement)lf.clone());
    }
    //rectangles
    for(int i=0;i<30000; i++){
      iy = rnd.nextInt(h-2);//height
      ix = rnd.nextInt(w-2);//width
      xy = rnd.nextInt(h - iy);
      xx = rnd.nextInt(w - ix);
      LeafElement lf = new LeafElement(new Rect(ix,iy,ix+xx,iy+xy),218);
      rt.insert(lf);
      rect.expandToInclude(lf.getRect());
      //vct.add((LeafElement)lf.clone());
    }
    //for(int i=0; i<vct.size(); i++){
    //System.out.println(i);
    //delete(fileName, (LeafElement)vct.get(i));
    //}
    //  RTreeRead rd = new RTreeRead(fileName);
    //  rd.readSeq();
    
    System.out.println("Entry over in ms : " +(System.currentTimeMillis()-start)
                       + " for thread " + Thread.currentThread());
  }
  //public
  public void entry(String fileName)
  {
    try{
      //Point data
      /*
        RTree rtree = new RTree(fileName);
        LeafElement lf1 = 
        new LeafElement(new Rect(4,3,4,3),218);
        LeafElement lf2 = 
        new LeafElement(new Rect(6,5,6,5),218);
        LeafElement lf3 = 
        new LeafElement(new Rect(5,6,5,6),218);
        LeafElement lf4 =
        new LeafElement(new Rect(7,2,7,2),218);
        rtree.insert(lf1);
        rtree.insert(lf2);
        rtree.insert(lf3);
        rtree.insert(lf4);
      */
      //BOXES
      long start = System.currentTimeMillis();      
      RTree rtree = new RTree(fileName);
      LeafElement lf1 = new LeafElement(new Rect(3,2,4,3),3243);//keep for join
      rtree.insert(lf1);
      rtree.delete(lf1);
      /*
      //getall(fileName);
      RTree rtree1 = new RTree(fileName);
      LeafElement lf2 = new LeafElement(new Rect(5,4,6,5),5465);
      //rtree1.insert(lf2);
            
      RTree rtree2 = new RTree(fileName);
      LeafElement lf3 = new LeafElement(new Rect(9,6,10,7),96107);
      //rtree2.insert(lf3);
            
      RTree rtree3 = new RTree(fileName);
      LeafElement lf4 = new LeafElement(new Rect(6,1,7,3),6173);//keep for join
      rtree3.insert(lf4);

      RTree rtree4 = new RTree(fileName);
      LeafElement lf5 = new LeafElement(new Rect(6,2,8,3),6283);
      //rtree4.insert(lf5);

      RTree rtree5 = new RTree(fileName);
      LeafElement lf6 = new LeafElement(new Rect(4,4,5,6),4456);
      //rtree5.insert(lf6);
      
      RTree rtree6 = new RTree(fileName);
      LeafElement lf7 = new LeafElement(new Rect(5,3,7,4),5374);
      //rtree6.insert(lf7);
                    
      RTree rtree7 = new RTree(fileName);
      LeafElement lf8 = new LeafElement(new Rect(9,5,10,6),95106);//keep for join
      rtree2.insert(lf8);

      LeafElement lf9 = new LeafElement(new Rect(1,2,2,3),1223);//keep for join
      rtree2.insert(lf9);
      */
      System.out.println("Time in ms:" +(System.currentTimeMillis()-start));
      System.out.println("Entry over");
    }
    catch(Exception e){
      e.printStackTrace();
      System.exit(1);
    }
  }
  /*
    Random rect queries
  */
  public  void overlapRR(String name)
    throws Exception
  {
    RTree rt = new RTree(name);
    int ix,iy,xx,xy;
    Random rnd = new Random(System.currentTimeMillis());
    for(int i=0;i<1;i++){
      iy = rnd.nextInt(h-2);//height
      ix = rnd.nextInt(w-2);//width
      xy = rnd.nextInt(h - iy);
      xx = rnd.nextInt(w - ix);
      long start4 = System.currentTimeMillis();
      List elmts = null;
      //  elmts = rt.overlaps(new Rect(ix,iy,ix+xx,iy+xy));
      //  System.out.println("Time in ms:" + (System.currentTimeMillis()-start4));
      //  System.out.println("Search result-Total elements:"+elmts.size());
      System.out.println("Sweep");
      start4 = System.currentTimeMillis();
      elmts = rt.overlapsSweep(new Rect(ix,iy,ix+xx,iy+xy));
      System.out.println("Time in ms:" + (System.currentTimeMillis()-start4));
      System.out.println("Search result-Total elements:"+elmts.size()); 
    }
  }
  /*
    Random point queries
  */
  public  void overlapRP(String name)
    throws Exception
  {
    RTree rt = new RTree(name);
    int ix,iy;
    Random rnd = new Random(System.currentTimeMillis());
    for(int i=0;i<10;i++){
      iy = rnd.nextInt(h);//height
      ix = rnd.nextInt(w);//width
      long start4 = System.currentTimeMillis();
      List elmts = rt.overlaps(new Rect(ix,iy,ix,iy));
      System.out.println("Time in ms:" +
                         (System.currentTimeMillis()-start4));
      System.out.println("Search result-Total elements:"+elmts.size());
      /*
        start4 = System.currentTimeMillis();
        elmts = rt.overlaps(new Rect(ix,iy,ix,iy));
        System.out.println("Time in ms:" +
        (System.currentTimeMillis()-start4));
        System.out.println("Search result-Total elements:"+elmts.size());
      */
    }
  }
  public  void getall(String name)
  {
    try{
      //get All elements
      RTree rt = new RTree(name);
            
      long start1 = System.currentTimeMillis();
      List elmts1 = rt.getAllElements();
      System.out.println("Time in ms:" +
                         (System.currentTimeMillis()-start1));
      System.out.println("Record fetched by "+
                         Thread.currentThread().getName()+
                         ": " + elmts1.size()); 
      /*
        start1 = System.currentTimeMillis();
        LinkedList elmts2 = rt.testgetAllElements();
        System.out.println("Time in ms:" +
        (System.currentTimeMillis()-start1));
        System.out.println("Record fetched: " + elmts2.size());
      */
      //for(int i=0; i<elmts2.length; i++)
      //System.out.println(elmts2[i].toString());

    }
    catch(Exception e){
      e.printStackTrace();
      System.exit(1);
    }
  }

  public  void overlap(String name,Rect rect)
  {
    try{
      //Test overlap
      RTree rt = new RTree(name);
      long start4 = System.currentTimeMillis();
      List elmts = rt.nonDisjoint(rect);
      System.out.println("Time in ms:" +
                         (System.currentTimeMillis()-start4));
      System.out.println("overlap Search -Total elements:"+elmts.size());
      //for(int i=0;i<elmts.size();i++)
      //System.out.println(((LeafElement)(elmts.elementAt(i))).toString());
    }
    catch(Exception e){
      e.printStackTrace();
      System.exit(1);
    }
  }
  public  void search(String name)
  {
    try{
      //Search Nearest
      RTree rt = new RTree(name);
      int ix,iy;
      Random rnd = new Random(System.currentTimeMillis());
      //for(int i=0;i<10;i++){
      iy = rnd.nextInt(h);//height
      ix = rnd.nextInt(w);//width
      Point pt = 
        new Point(ix,iy);
      //Point pt = 
      //new Point(2618917,1264511);
      //limited number
      long start5 = System.currentTimeMillis();
            
      ABL[] nrst = rt.nearestSearch(pt,50000000000L,10);
      System.out.println("Time in ms for NNSearch(Limited):"+ (System.currentTimeMillis()-start5));
      /*
        for(int i=0;i<nrst.length;i++)
        if(nrst[i] == null)
        System.out.println("Could not find anything!!");
        else
        System.out.println("Result" + nrst[i].element.toString()
        +"\tMINDIST:" + nrst[i].minDist);
      */
      //unlimited
            
      System.out.println("List");
      start5 = System.currentTimeMillis();
      List vec = rt.nearestSearch(pt,10000000000L);
      System.out.println("Time in ms for NNSearch(Unlimited):"+ (System.currentTimeMillis()-start5));
      System.out.println("Retrieved: "+vec.size());
            
      //for(int i=0;i<vec.size();i++)
      //        System.out.println("Result" + ((ABL)(vec.elementAt(i)))
      //                           .element.toString()
      //                   +"\tMINDIST:" + ((ABL)(vec.elementAt(i)))
      //                   .minDist);
            
    }
    catch(Exception e){
      e.printStackTrace();
      System.exit(1);
    }

  }
  public  void delete(String name,LeafElement element)
  {
    try{
      RTree rt = new RTree(fileName);
      rt.delete(element);
      //for(int i=0;i<150;i++)
      //rt.delete(element);
      
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }
  public  void tryOverlap()
  {
    try{
      Rect rect1 = new Rect();
      //Rect rect1 = new Rect(3,2,5,4);
      //Rect rect2 = new Rect(3,2,5,4);
      //Rect rect2 = new Rect(4,3,4,3);//true
      //Rect rect2 = new Rect(5,2,7,4);//false
      //Rect rect2 = new Rect(3,4,5,5);//false
      //Rect rect2 = new Rect(3,1,5,2);//false
      //Rect rect2 = new Rect(3,1,5,3);//false
      //Rect rect2 = new Rect(4,3,4,3);//true
      //Rect rect2 = new Rect(5,3,5,3);//true
      //Rect rect2 = new Rect(6,2,6,2);//false
      //Rect rect2 = new Rect(2,1,6,5);//false
      Rect rect2 = new Rect(4,2,5,3);
      //Rect rect1 = new Rect(9,6,10,7);//true
      //Rect rect2 = new Rect(8,5,11,8);//true
      System.out.println(rect1.toString()+" \nand\n"+rect2.toString()
                         +"\noverlap? \n\tAns- "+rect2.contains(rect1));
    }
    catch(Exception e){
      System.out.println("Exception "+e.getMessage());
    }
  }
  public void tryIntersection()
  {
    try{
      //Rect rect1 = new Rect(3,2,5,4);
      //Rect rect2 = new Rect(3,2,5,4);//3 2 5 4
      //Rect rect2 = new Rect(4,3,4,3);//4 3 4 3
      //Rect rect2 = new Rect(5,2,7,4);//5 2 5 4 - not null but a single line
      //Rect rect2 = new Rect(3,4,5,5);//3 4 5 4
      //Rect rect2 = new Rect(3,1,5,2);//3 2 5 2
      //Rect rect2 = new Rect(3,1,5,3);//3 2 5 3
      //Rect rect2 = new Rect(4,3,4,3);//4 3 4 3
      //Rect rect2 = new Rect(5,3,5,3);//5 3 5 3
      //Rect rect2 = new Rect(6,2,6,2);//null
      //Rect rect2 = new Rect(2,1,6,5);//3 2 5 4
      //Rect rect2 = new Rect(4,2,5,3);//4 2 5 3
      Rect rect1 = new Rect(9,6,10,7);//null
      Rect rect2 = new Rect(8,5,11,8);//null - comp the two 9 6 10 7
      System.out.println(rect1.toString()+" \nand\n"+rect2.toString()
                         +"\nIntersection \n\tAns- "+rect2.intersection(rect1));
    }
    catch(Exception e){
      System.out.println("Exception "+e.getMessage());
    }
  }
  public  void tryCvr()
  {
    try{
      Rect rect1 = new Rect(3,2,5,4);
      Rect rect2 = new Rect(3,2,4,4);//true 
      //Rect rect2 = new Rect(6,2,6,2);//false
      System.out.println(rect1.toString()+" \nand\n"+rect2.toString()
                         +"\nDoes first Eclose second? \n\tAns- "
                         +rect1.covers(rect2));
    }
    catch(Exception e){
      System.out.println("Exception "+e.getMessage());
    }
  }
  public  void tryIntsct()
  {
    try{
      Rect rect1 = new Rect(3,2,5,4);
      //Rect rect2 = new Rect(3,2,3,2);//true
      //Rect rect2 = new Rect(5,2,7,4);//false
      //Rect rect2 = new Rect(3,2,3,2);//true
      //Rect rect2 = new Rect(3,4,5,5);//false
      //Rect rect2 = new Rect(3,1,5,2);//false
      //Rect rect2 = new Rect(3,1,5,3);//true
      //Rect rect2 = new Rect(4,3,4,3);//true - check
      Rect rect2 = new Rect(3,2,3,2);//true
      System.out.println(rect1.toString()+" \nand\n"+rect2.toString()
                         +"\nDo Both Intersect? \n\tAns- "
                         +rect1.meet(rect2));
    }
    catch(Exception e){
      System.out.println("Exception "+e.getMessage());
    }
  }

  public void trySeed(String sdTree, RTree rtree)
  {
    try{
      //System.out.println("rTreeDemo.trySeed : height of rtree is " + rtree.getHeight());
      SdTree sdt = new SdTree(sdTree, rtree);
      //now grow
      int ix,iy,xx,xy;//mIn,maX
      Random rnd = new Random(seed);
      long start = System.currentTimeMillis();      
      //  LeafElement llf = new LeafElement(new Rect(1752, 2179, 5999888, 14999646),218);
      //  sdt.growLeaf(llf);
      
      for(int i=0;i<2000000; i++){
        iy = rnd.nextInt(h-2);//height 
        ix = rnd.nextInt(w-2);//width
        xy = rnd.nextInt(h - iy);
        xx = rnd.nextInt(w - ix);
        LeafElement lf = new LeafElement(new Rect(ix,iy,ix+xx,iy+xy),218);
        sdt.growLeaf(lf);
      } 
      sdt.cleanUp();
      (new RTree(sdTree)).flush();
      //  RTreeRead rd = new RTreeRead(sdTree);
      //  rd.readSeq();
    }catch(Exception e){
      e.printStackTrace();
    }
  }

  public List tryJoin()
  {
    try{
      //String lt = new String("c:\\temp\temptree.dat");
      //String lt = new String("/mnt/projects/data/MUM4_78.idx");
      //String rt = new String("/mnt/projects/data/MUM4_118.idx");
      RTree ltTree = new RTree(/*lt*/fileName+"1");
      RTree rtTree = new RTree(/*rt*/fileName+"2");
      //System.out.println("rTreeDemo.tryJoin : lt size " + ltTree.getAllElements().size());

      Join join = new Join(ltTree, rtTree, new Pair(), new IntersectPred());

      System.out.println("rTreeDemo : left tree size " + ltTree.getAllElements().size()
                         +"\nright tree size " + rtTree.getAllElements().size()
                         +"\n join size " + join.relate().size());
      
      long t = System.currentTimeMillis();
      List list = join.relate();
      System.out.println("Join returned " + list.size() + " pointers in " 
                         + (System.currentTimeMillis() - t) + " ms" );
      return list;
    }catch(Exception e){
      e.printStackTrace();
      return null;
    }
  }
}

class RTreeRead
{
  static int nleaf;
  static int leaf;
  static int minX  = Integer.MAX_VALUE, minY = Integer.MAX_VALUE,maxX = Integer.MIN_VALUE,
    maxY = Integer.MIN_VALUE;
  RandomAccessFile file;
  RTreeRead(String fileName)
  {
    nleaf = leaf = 0;
    try{
      file = new RandomAccessFile(fileName,"r");
    }
    catch(Exception e){
      System.out.println("RTreeRead "+e.getMessage());
    }
  }
  public void readSeq()
  {
    try{
      long length = file.length();
      if(length == 0)
        return;
      Integer ln = new Integer((new Long(file.length())).intValue());
      int kbytes = (new Double(Math.floor(ln.doubleValue()/4096))).intValue();
      file.seek(0);
      for(int i=0;i<kbytes+1;i++)
        {
          byte[] data = new byte[Node.NODE_SIZE];
          file.read(data);              
          if(i==0)
            printFlHdr(data);
          else
            printNode(i-1,data);
        }
      System.out.println("Total Leaf:"+leaf+"\tNonLeaf:"+nleaf);
      System.out.println("MinX:"+minX+"\tMinY:"+minY+"\tMaxX:"+maxX+"\tMaxY:"+maxY);
    }
    catch(Exception e){
      System.out.println("RTreeRead"+e.getMessage());
    }
  }
  public void printFlHdr(byte[] data)
  {
    try{
      int frNode = 123;
      DataInputStream ds = 
        new DataInputStream(new ByteArrayInputStream(data));
      System.out.println("\t***The File Header***");
      System.out.println("TotalNodes:(includes unused ones):"+ds.readInt());
      System.out.println("RootIndex: " + ds.readLong());
      System.out.println("The free nodes Stack");
      for(int topIdx=0;(topIdx<Node.FREE_LIST_LIMIT)&&
            ((frNode = ds.readInt()) !=
             Node.NOT_DEFINED); topIdx++){
        System.out.println("At " + topIdx + ": "+ frNode);
      }
      //System.out.println(frNode);
    }
    catch(Exception e){
      System.out.println("Error at printFlHdr");
      System.exit(1);
    }
  }
  public void printNode(int index,byte[] data)
  {
    int mx,my,xx,xy;
    try{
      DataInputStream ds = 
        new DataInputStream(new ByteArrayInputStream(data));
      System.out.println("\t***Node at Index: "+index+"***");
      System.out.println("Node Header");
      int totElmt = ds.readInt();
      System.out.println("TotalElements:"+totElmt);
      System.out.println("Parent:"+ds.readLong());
      System.out.println("Element Size:"+ds.readInt());
      int elmtType = ds.readInt();
      System.out.println("Element Types:"+elmtType);
      if(elmtType == Node.NONLEAF_NODE)
        nleaf++;
      else
        leaf++;
      for(int i=0;i<totElmt;i++){
        System.out.println("Elements...");

        mx = ds.readInt();
        if(mx < minX)
          minX = mx;
        System.out.println("MinX: "+mx);

        my = ds.readInt();
        if(my < minY)
          minY = my;
        System.out.println("MinY: "+my);            

        xx = ds.readInt();
        if(xx > maxX)
          maxX = xx;
        System.out.println("MaxX: "+xx);

        xy = ds.readInt();
        if(xy > maxY)
          maxY = xy;
        System.out.println("MaxY: "+xy);
        System.out.println("Pointer: "+ds.readLong());
      }
    }
    catch(Exception e){
      System.out.println("Error");
      System.exit(1);
    }

  }
}
