/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2015 Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.broad.igv.bbfile;

import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.seekablestream.SeekableStreamFactory;

import org.broad.igv.logging.*;

import java.util.ArrayList;


/**
 * Created by IntelliJ IDEA.
 * User: martind
 * Date: Dec 17, 2009
 * Time: 12:36:35 PM
 * To change this template use File | Settings | File Templates.
 */
/*
*   Container class which reads BBFile zoom information into ArrayLists.
*
*   1) Reads in Zoom Level Header Table D into Zoom Headers and
*       loads them into the ZoomLevelHeader array, one for each zoom level.
*
*   2) Reads in Table O overall zoom level format as referenced
*       by dataOffset in Table D.
*
*   Note: Zoom levels count from Table C is validated by BBZoomLevelHeader,
*   which will throw a RuntimeException if header is not found.
*
* */
public class BBZoomLevels {

    private static Logger log = LogManager.getLogger(BBZoomLevels.class);

    // defines the zoom headers access
    //private SeekableStream fis;       // BBFile handle
    private long zoomHeadersOffset;     // BBFile first zoom header offset
    private int zoomLevelsCount;        // BB File header Table C specified zoom levels

    // zoom level headers - Table D , one for each zoom level
    // Note: array size determines how many zoom levels actually read
    private ArrayList<BBZoomLevelHeader> zoomLevelHeaders;  // zoom level headers

    // zoom level data formats - BBFile Table O
    private ArrayList<BBZoomLevelFormat> zoomLevelFormatList;

    // zoom level R+ trees - one per level
    private ArrayList<RPTree> zoomLevelRPTree;

    /*
   *  constructor   - reads zoom level headers and data format from file I/O stream
   *      for all zoom levels and acts as a container for zoom data records.
   *
   *  Parameters:
   *      fis - file input stream handle
   *      fileOffset - file byte location for zoom level headers
   *      zoomLevels - count of zoom levels from BBFile Table C
   *      isLowToHigh - boolean flag indicates if values are arranged low to high bytes.
   *      uncompressBufSize - byte size of the buffer to use for decompression
   * */

    public BBZoomLevels(SeekableStream is, long fileOffset, int zoomLevels,
                        boolean isLowToHigh, int uncompressBufSize){
        int zoomLevel;
        int zoomHeadersRead;
        long zoomDataOffset;
        long zoomIndexOffset;

        // save the seekable file handle and zoom zoomLevel headers file offset
        //this.fis = fis;
        zoomHeadersOffset = fileOffset;
        zoomLevelsCount = zoomLevels;

        // We don't know the exact size of the header fields, so use a buffered stream
        SeekableStream fis = SeekableStreamFactory.getInstance().getBufferedStream(is, 512000);
        
        // Note: a bad zoom header will result in a 0 count returned
        zoomHeadersRead =  readZoomHeaders(fis, zoomHeadersOffset, zoomLevels, isLowToHigh);

        if(zoomHeadersRead > 0){

            // create zoom level data format containers
            zoomLevelFormatList = new ArrayList<BBZoomLevelFormat>();

            // for each zoom zoomLevel, get associated zoom data format
            for(int index = 0; index < zoomHeadersRead; ++index) {

                zoomLevel = index + 1;

                // Zoom dataOffset (from Table D) is file location for zoomCount (Table O)
                // Note: This dataOffset is zoomFormatLocation in BBZoomLevelFormat.
                zoomDataOffset = zoomLevelHeaders.get(index).getDataOffset();

                // R+ zoom index offset (Table D) marks end of zoom data in the
                // zoom level format (Table O)
                long dataSize = zoomLevelHeaders.get(index).getIndexOffset() - zoomDataOffset
                        - BBZoomLevelFormat.ZOOM_FORMAT_HEADER_SIZE;

                // get zoom zoomLevel data records  - zoomDataOffset references zoomCount in Table O
                // Note: zoom zoomLevel data records read their own data
                BBZoomLevelFormat zoomLevelData = new BBZoomLevelFormat(zoomLevel, fis, zoomDataOffset,
                        dataSize, isLowToHigh, uncompressBufSize);

                zoomLevelFormatList.add(zoomLevelData);
            }

            // create zoom level R+ tree containers
            zoomLevelRPTree = new ArrayList<RPTree>();

            // for each zoom zoomLevel, get associated R+ tree
            for(int index = 0; index < zoomHeadersRead; ++index) {

                // Zoom indexOffset (from Table D) is file location
                // for Table O zoomIndex for R+ tree zoom data
                zoomIndexOffset = zoomLevelHeaders.get(index).getIndexOffset();

                // get Zoom Data R+ Tree (Tables K, L, M, N): exists for zoom levels
                RPTree zoomRPTree = new RPTree(fis, zoomIndexOffset, isLowToHigh, uncompressBufSize, true);

                //if(zoomRPTree.getNodeCount() > 0)
                    zoomLevelRPTree.add(zoomRPTree);
            }
        }
    }

    /*
    *   Method returns the BBFile's first zoom header file offset.
    *
    *   Note zoom headers immediately follow the BBFile header (Table C)
    *
    *   Returns:
    *       first zoom header file offset
    * */
    public long getZoomHeadersOffset() {
        return zoomHeadersOffset;
    }

    /*
    *   Method returns the number of zoom level headers found.
    *
    *   Note Should match zoomLevels in the BBFile header (Table C)
    *
    *   Returns:
    *      number of zoom level headers found
    * */
    public int getZoomHeaderCount() {
        return zoomLevelHeaders.size();
    }

    /*
    *   Method returns the zoom level headers.
    *
    *   Returns:
    *      zoom level headers
    * */
    public ArrayList<BBZoomLevelHeader> getZoomLevelHeaders() {
        return zoomLevelHeaders;
    }

    /*
    *   Method returns the zoom level header for specified level.
    *
    *   Parameters:
    *       level - zoom level; level starts at 1
    *
    *   Returns:
    *      Zoom level header for specified level; or null for bad zoom level.
    * */
    public BBZoomLevelHeader getZoomLevelHeader(int level) {
        if(level < 1 || level > zoomLevelsCount)
        return null;

        return zoomLevelHeaders.get(level - 1);
    }

    /*
    *   Method returns the zoom level formats for zoom data.
    *
    *   Returns:
    *      zoom level formats for zoom data
    * */
    public ArrayList<BBZoomLevelFormat> getZoomLevelFormats(){
        return zoomLevelFormatList;
    }

    /*
    *   Method returns the R+ index tree for the specified zoom level.
    *
    *   Parameters:
    *       level - zoom level; level starts at 1
    *
    *   Returns:
    *      R+ index tree for the specified zoom level; or null for bad zoom level
    * */
    public RPTree getZoomLevelRPTree(int level) {
        if(level < 1 || level > zoomLevelsCount)
            return null;

        return zoomLevelRPTree.get(level - 1);
    }

    // prints out the zoom level header information
    public void printZoomHeaders() {

        // note if successfully read - should always be correct
        if(zoomLevelHeaders.size() == zoomLevelsCount)
            log.debug("Zoom level headers read for " + zoomLevelsCount + " levels:");

        else
            log.error("Zoom level headers not successfully read for "
                    + zoomLevelsCount + "levels.");

        for( int index = 0; index < zoomLevelHeaders.size(); ++index) {

            // zoom level headers print themselves
            zoomLevelHeaders.get(index).print();
        }

    }

    /*
    * Reads in all the Zoom Headers.
    *
    *   Parameters:
    *       fileOffset - File byte location for first zoom level header
    *       zoomLevels - count of zoom levels to read in
    *       isLowToHigh - indicate byte order is lwo to high, else is high to low
    *
    *   Returns:
    *       Count of zoom levels headers read, or 0 for failure to find the
    *       header information.
    * */
    private int readZoomHeaders(SeekableStream fis, long fileOffset, int zoomLevels, boolean isLowToHigh) {
        int level = 0;
        BBZoomLevelHeader zoomLevelHeader;

        if(zoomLevels < 1)
            return 0;

        // create zoom headers and data containers
        zoomLevelHeaders = new ArrayList<BBZoomLevelHeader>();

        // get zoom header information for each zoom levelsRead
        for(int index = 0; index < zoomLevels; ++index)  {
            level = index + 1;

            // read zoom level header - read error is returned as Runtime Exception
            zoomLevelHeader = new BBZoomLevelHeader(fis, fileOffset, level, isLowToHigh);

            zoomLevelHeaders.add(zoomLevelHeader);

            fileOffset += BBZoomLevelHeader.ZOOM_LEVEL_HEADER_SIZE;
        }

        return level;
    }

}
