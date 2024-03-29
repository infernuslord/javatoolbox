package toolbox.tivo;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;

import org.apache.log4j.Logger;

import toolbox.util.collections.CircularCharQueue;

/**
 * Consumes the output generated by FFMpeg while transcoding a file and
 * determines the amount of progress by filtering the stream for the number of
 * seconds of the movie that has been transcoded. FFMpeg writes special ascii
 * codes to output to back up and overwrite data on the same line so a 
 * line number reader/stream was not used.
 * <p>
 * In retrospect, could have just used regular expressions...
 */
public class FFMpegProgressOutputStream extends FilterOutputStream {
    
    private static final Logger logger_ = Logger.getLogger(FFMpegProgressOutputStream.class);
    
    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------
    
    /**
     * String after which the time is embedded in the stream output.
     */
    private static final String TOKEN_TIME =  " time=";
    
    /**
     * String after which the current frame is embedded in the stream output.
     */
    private static final String TOKEN_FRAME = "frame=";
    
    /**
     * State in when searching for the time token.
     */
    private static final int STATE_SCAN_TIME_TAG = 0;
    
    /**
     * State in when parsing the time value.
     */
    private static final int STATE_SCAN_TIME_VALUE = 1;

    /**
     * State when searching for the frame token.
     */
    private static final int STATE_SCAN_FRAME_TAG = 2;
    
    /**
     * State when searching for the frame value.
     */
    private static final int STATE_SCAN_FRAME_VALUE = 3;
    
    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------
    
    /**
     * Fixed length window into the stream used to search for string values.
     * Basically a circular FIFO queue. 
     */
    private CircularCharQueue buffer_;
    
    /**
     * Current state of the scanner state machine.
     */
    private int state_;
    
    /**
     * Buffer used to parse the time (number of seconds) of movie that has been
     * transcoded.
     */
    private StringBuffer secs_;
    
    /** 
     * Buffer used to parse the number of frames of movie that has been 
     * transcoded.
     */
    private StringBuffer frames_;
    
    /** 
     * Passed into the construtor so debug/info output can so progress relative
     * to the total time length of the movie.
     */
    private int totalSeconds_;
    
    /**
     * Last successfully parsed time for the transcoding.
     */
    private int progressSecs_;
    
    /**
     * Last successfully parsed frame for the transcoding.
     */
    private int progressFrames_;
    
    private NumberFormat nf = NumberFormat.getInstance();
    
    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------
    
    public FFMpegProgressOutputStream(OutputStream out) {
        super(out);
        buffer_ = new CircularCharQueue(TOKEN_TIME.length());
        state_ = STATE_SCAN_FRAME_TAG;
        secs_ = new StringBuffer();
        frames_ = new StringBuffer();
        progressSecs_ = -1;
        progressFrames_ = 0;
        nf.setMaximumFractionDigits(1);
        nf.setMinimumFractionDigits(1);
    }

    
    public FFMpegProgressOutputStream(int totalSeconds, OutputStream out) {
        super(out);
        buffer_ = new CircularCharQueue(TOKEN_TIME.length());
        state_ = STATE_SCAN_FRAME_TAG;
        secs_ = new StringBuffer();
        frames_ = new StringBuffer();
        totalSeconds_ = totalSeconds;
        progressSecs_ = -1;
        progressFrames_ = 0;
        nf.setMaximumFractionDigits(1);
        nf.setMinimumFractionDigits(1);
    }
    
    // -------------------------------------------------------------------------
    // Overrides FilterOutputStream
    // -------------------------------------------------------------------------
    
    public void write(int b) throws IOException {
        
        // always pass data to delegate non-destructively
        out.write(b);

        // Basic state machine for extracting time and frame values for the
        // output stream:
        //
        // 1. scan for frame tag
        // 2. parse frame value
        // 3. scan for time tag
        // 4. parse time value
        // 5. goto 1
        
        char c = (char) b;
        
        switch (state_) {

            case STATE_SCAN_TIME_TAG:
                buffer_.add(c);
                
                // Search for the " time=" token
                
                String s = buffer_.toString();
                if (s.equals(TOKEN_TIME)) {
                    state_ = STATE_SCAN_TIME_VALUE;
                    secs_ = new StringBuffer();
                }
                break;
                
            case STATE_SCAN_TIME_VALUE:
                
                // Once " time=" is found, scan until the first period is found 
                // and that represents the number of seconds processed.
                
                if (c != '.') {
                    secs_.append(c);
                }
                else {
                    state_ = STATE_SCAN_FRAME_TAG; //STATE_SCAN_TOKEN;
                    buffer_.clear();
                    int secs = Integer.parseInt(secs_.toString());
                    
                    if ((secs < 10      || 
                         secs % 30 == 0 || 
                         secs + 10 > totalSeconds_) && 
                             secs != progressSecs_) {
                        
                        float percent = secs/ (float) totalSeconds_;
                        logger_.info("Progress: " + secs + "/" + totalSeconds_ + " (" + nf.format(percent * 100) + "%)");
                        progressSecs_ = secs;
                    }
                }
                break;
                
                
            case STATE_SCAN_FRAME_TAG:
                buffer_.add(c);
                
                // Search for the "frame= " token
                
                String fs = buffer_.toString();
                if (fs.equals(TOKEN_FRAME)) {
                    state_ = STATE_SCAN_FRAME_VALUE;
                    frames_ = new StringBuffer();
                }
                break;

                
            case STATE_SCAN_FRAME_VALUE:

            	/*
            	Comparison
            	old: frame= 2521 q=0.0 size=    6072kB time=84.1 bitrate= 591.6kbits/s
            	new: frame= 2511 fps= 34 q=31.0 size=    6268kB time=83.8 bitrate= 613.1kbits/s
            	*/
            	
                // Once "frame=" is found, 
                // scan until the first int is found
                // scan until next space is found
                // values inbetween is number of frames
                
                if (c != 'f') {  // read until the fps= 34 is hit
                    frames_.append(c);
                }
                else {
                    state_ = STATE_SCAN_TIME_TAG;
                    buffer_.clear();
                    
                    int frames = 0; // = progressFrames_; // init w/ prev value just in case new value has problems being parsed
                    
                    try {
                    	frames = Integer.parseInt(frames_.toString().trim());
                    }
                    catch (NumberFormatException nfe) {
                    	logger_.warn("Could not parse as int: '" + frames_.toString() + "'. Defaulting to previous value of " + frames);
                    }
                    //logger_.info("Progress: frame " + frames);
                    progressFrames_ = frames;
                }
                break;
        }
    }

    
    /**
     * Returns the progressFrames.
     * 
     * @return int
     */
    public int getProgressFrames() {
        return progressFrames_;
    }

    
    /**
     * Returns the progressSecs.
     * 
     * @return int
     */
    public int getProgressSecs() {
        return progressSecs_;
    }
}

// -------------------------------------------------------------------------
// Sample output generated by ffmpeg
// -------------------------------------------------------------------------

/*
ffmpeg version 0.4.9-pre1, build 4743, Copyright (c) 2000-2004 Fabrice Bellard
configuration:  --enable-mp3lame --enable-vorbis --enable-faad --enable-faac --enable-xvid --enable-mingw32 --enable-a52 --enable-dts --enable-pp --enable-gpl --enable-memalign-hack 
built on Feb 22 2005 04:58:29, gcc: 3.4.2 (mingw-special)
Input #0, avi, from 'c:\tivo\incoming\h264.avi':
Duration: 00:01:37.1, start: 0.000000, bitrate: 203 kb/s
Stream #0.0: Video: h264, 416x304, 25.00 fps
Stream #0.1: Audio: mp3, 22050 Hz, mono, 31 kb/s
Output #0, dvd, to 'c:\tivo\working\h264.mpg':
Stream #0.0: Video: mpeg2video (hq), 720x480, 29.97 fps, q=2-31, 203 kb/s
Stream #0.1: Audio: mp2, 48000 Hz, stereo, 224 kb/s
Stream mapping:
Stream #0.0 -> #0.0
Stream #0.1 -> #0.1
frame=   10 q=0.0 size=      60kB time=0.3 bitrate=1636.8kbits/s    
frame=   24 q=0.0 size=     102kB time=0.8 bitrate=1088.8kbits/s    
frame=   40 q=0.0 size=     150kB time=1.3 bitrate= 944.3kbits/s    
frame=   54 q=0.0 size=     174kB time=1.8 bitrate= 806.0kbits/s    
frame=   68 q=0.0 size=     220kB time=2.2 bitrate= 806.2kbits/s    
frame=   82 q=0.0 size=     282kB time=2.7 bitrate= 854.8kbits/s    
frame=   96 q=0.0 size=     324kB time=3.2 bitrate= 837.3kbits/s    
frame=  110 q=0.0 size=     346kB time=3.6 bitrate= 779.3kbits/s    
frame=  123 q=0.0 size=     396kB time=4.1 bitrate= 796.9kbits/s    
frame=  138 q=0.0 size=     414kB time=4.6 bitrate= 741.9kbits/s    
frame=  153 q=0.0 size=     448kB time=5.1 bitrate= 723.6kbits/s    
frame=  165 q=0.0 size=     486kB time=5.5 bitrate= 727.6kbits/s    
frame=  175 q=0.0 size=     518kB time=5.8 bitrate= 730.9kbits/s    
frame=  191 q=0.0 size=     560kB time=6.3 bitrate= 723.6kbits/s    
frame=  206 q=0.0 size=     602kB time=6.8 bitrate= 721.0kbits/s    
frame=  221 q=0.0 size=     618kB time=7.3 bitrate= 689.7kbits/s    
frame=  233 q=0.0 size=     656kB time=7.7 bitrate= 694.2kbits/s    
frame=  245 q=0.0 size=     690kB time=8.1 bitrate= 694.3kbits/s    
frame=  257 q=0.0 size=     714kB time=8.5 bitrate= 684.8kbits/s    
frame=  271 q=0.0 size=     752kB time=9.0 bitrate= 683.8kbits/s    
frame=  284 q=0.0 size=     786kB time=9.4 bitrate= 681.9kbits/s    
frame=  299 q=0.0 size=     820kB time=9.9 bitrate= 675.6kbits/s    
frame=  313 q=0.0 size=     858kB time=10.4 bitrate= 675.2kbits/s    
frame=  327 q=0.0 size=     878kB time=10.9 bitrate= 661.2kbits/s    
frame=  342 q=0.0 size=     912kB time=11.4 bitrate= 656.6kbits/s    
frame=  356 q=0.0 size=     952kB time=11.8 bitrate= 658.4kbits/s    
frame=  370 q=0.0 size=     986kB time=12.3 bitrate= 656.0kbits/s    
frame=  384 q=0.0 size=    1022kB time=12.8 bitrate= 655.1kbits/s    
frame=  399 q=0.0 size=    1062kB time=13.3 bitrate= 655.1kbits/s    
frame=  414 q=0.0 size=    1078kB time=13.8 bitrate= 640.8kbits/s    
frame=  429 q=0.0 size=    1114kB time=14.3 bitrate= 639.0kbits/s    
frame=  445 q=0.0 size=    1148kB time=14.8 bitrate= 634.8kbits/s    
frame=  458 q=0.0 size=    1200kB time=15.2 bitrate= 644.7kbits/s    
frame=  472 q=0.0 size=    1216kB time=15.7 bitrate= 633.9kbits/s    
frame=  487 q=0.0 size=    1250kB time=16.2 bitrate= 631.5kbits/s    
frame=  500 q=0.0 size=    1286kB time=16.6 bitrate= 632.7kbits/s    
frame=  514 q=0.0 size=    1322kB time=17.1 bitrate= 632.7kbits/s    
frame=  527 q=0.0 size=    1358kB time=17.6 bitrate= 633.9kbits/s    
frame=  542 q=0.0 size=    1396kB time=18.1 bitrate= 633.5kbits/s    
frame=  555 q=0.0 size=    1406kB time=18.5 bitrate= 623.1kbits/s    
frame=  571 q=0.0 size=    1446kB time=19.0 bitrate= 622.8kbits/s    
frame=  585 q=0.0 size=    1484kB time=19.5 bitrate= 623.9kbits/s    
frame=  599 q=0.0 size=    1524kB time=20.0 bitrate= 625.7kbits/s    
frame=  614 q=0.0 size=    1560kB time=20.5 bitrate= 624.8kbits/s    
frame=  628 q=0.0 size=    1576kB time=20.9 bitrate= 617.1kbits/s    
frame=  643 q=0.0 size=    1608kB time=21.4 bitrate= 614.9kbits/s    
frame=  657 q=0.0 size=    1640kB time=21.9 bitrate= 613.8kbits/s    
frame=  670 q=0.0 size=    1676kB time=22.3 bitrate= 615.1kbits/s    
frame=  685 q=0.0 size=    1716kB time=22.8 bitrate= 615.9kbits/s    
frame=  699 q=0.0 size=    1748kB time=23.3 bitrate= 614.8kbits/s    
frame=  713 q=0.0 size=    1784kB time=23.8 bitrate= 615.2kbits/s    
frame=  728 q=0.0 size=    1830kB time=24.3 bitrate= 618.0kbits/s    
frame=  742 q=0.0 size=    1870kB time=24.7 bitrate= 619.6kbits/s    
frame=  756 q=0.0 size=    1898kB time=25.2 bitrate= 617.2kbits/s    
frame=  770 q=0.0 size=    1940kB time=25.7 bitrate= 619.4kbits/s    
frame=  784 q=0.0 size=    1980kB time=26.1 bitrate= 620.8kbits/s    
frame=  798 q=0.0 size=    2022kB time=26.6 bitrate= 622.9kbits/s    
frame=  812 q=0.0 size=    2060kB time=27.1 bitrate= 623.6kbits/s    
frame=  827 q=0.0 size=    2100kB time=27.6 bitrate= 624.2kbits/s    
frame=  842 q=0.0 size=    2108kB time=28.1 bitrate= 615.4kbits/s    
frame=  857 q=0.0 size=    2146kB time=28.6 bitrate= 615.5kbits/s    
frame=  872 q=0.0 size=    2200kB time=29.1 bitrate= 620.1kbits/s    
frame=  885 q=0.0 size=    2238kB time=29.5 bitrate= 621.6kbits/s    
frame=  900 q=0.0 size=    2280kB time=30.0 bitrate= 622.7kbits/s    
frame=  915 q=0.0 size=    2312kB time=30.5 bitrate= 621.0kbits/s    
frame=  929 q=0.0 size=    2350kB time=31.0 bitrate= 621.7kbits/s    
frame=  943 q=0.0 size=    2364kB time=31.4 bitrate= 616.1kbits/s    
frame=  958 q=0.0 size=    2396kB time=31.9 bitrate= 614.7kbits/s    
frame=  972 q=0.0 size=    2438kB time=32.4 bitrate= 616.4kbits/s    
frame=  987 q=0.0 size=    2476kB time=32.9 bitrate= 616.5kbits/s    
frame=  999 q=0.0 size=    2514kB time=33.3 bitrate= 618.5kbits/s    
frame= 1013 q=0.0 size=    2552kB time=33.8 bitrate= 619.1kbits/s    
frame= 1026 q=0.0 size=    2574kB time=34.2 bitrate= 616.5kbits/s    
frame= 1041 q=0.0 size=    2610kB time=34.7 bitrate= 616.1kbits/s    
frame= 1055 q=0.0 size=    2648kB time=35.2 bitrate= 616.8kbits/s    
frame= 1069 q=0.0 size=    2686kB time=35.6 bitrate= 617.5kbits/s    
frame= 1083 q=0.0 size=    2724kB time=36.1 bitrate= 618.1kbits/s    
frame= 1096 q=0.0 size=    2758kB time=36.5 bitrate= 618.4kbits/s    
frame= 1110 q=0.0 size=    2794kB time=37.0 bitrate= 618.5kbits/s    
frame= 1124 q=0.0 size=    2808kB time=37.5 bitrate= 613.9kbits/s    
frame= 1139 q=0.0 size=    2838kB time=38.0 bitrate= 612.3kbits/s    
frame= 1152 q=0.0 size=    2896kB time=38.4 bitrate= 617.7kbits/s    
frame= 1165 q=0.0 size=    2938kB time=38.8 bitrate= 619.7kbits/s    
frame= 1180 q=0.0 size=    2966kB time=39.3 bitrate= 617.6kbits/s    
frame= 1193 q=0.0 size=    3002kB time=39.8 bitrate= 618.3kbits/s    
frame= 1206 q=0.0 size=    3048kB time=40.2 bitrate= 621.0kbits/s    
frame= 1220 q=0.0 size=    3090kB time=40.7 bitrate= 622.3kbits/s    
frame= 1234 q=0.0 size=    3114kB time=41.1 bitrate= 620.1kbits/s    
frame= 1248 q=0.0 size=    3152kB time=41.6 bitrate= 620.6kbits/s    
frame= 1262 q=0.0 size=    3188kB time=42.1 bitrate= 620.7kbits/s    
frame= 1277 q=0.0 size=    3220kB time=42.6 bitrate= 619.6kbits/s    
frame= 1292 q=0.0 size=    3258kB time=43.1 bitrate= 619.6kbits/s    
frame= 1307 q=0.0 size=    3296kB time=43.6 bitrate= 619.6kbits/s    
frame= 1319 q=0.0 size=    3304kB time=44.0 bitrate= 615.5kbits/s    
frame= 1333 q=0.0 size=    3336kB time=44.4 bitrate= 614.9kbits/s    
frame= 1349 q=0.0 size=    3376kB time=45.0 bitrate= 614.9kbits/s    
frame= 1364 q=0.0 size=    3412kB time=45.5 bitrate= 614.6kbits/s    
frame= 1379 q=0.0 size=    3442kB time=46.0 bitrate= 613.3kbits/s    
frame= 1394 q=0.0 size=    3478kB time=46.5 bitrate= 613.0kbits/s    
frame= 1409 q=0.0 size=    3518kB time=47.0 bitrate= 613.4kbits/s    
frame= 1423 q=0.0 size=    3534kB time=47.4 bitrate= 610.2kbits/s    
frame= 1436 q=0.0 size=    3562kB time=47.9 bitrate= 609.4kbits/s    
frame= 1451 q=0.0 size=    3600kB time=48.4 bitrate= 609.6kbits/s    
frame= 1465 q=0.0 size=    3638kB time=48.8 bitrate= 610.1kbits/s    
frame= 1479 q=0.0 size=    3652kB time=49.3 bitrate= 606.6kbits/s    
frame= 1493 q=0.0 size=    3702kB time=49.8 bitrate= 609.2kbits/s    
frame= 1507 q=0.0 size=    3734kB time=50.3 bitrate= 608.7kbits/s    
frame= 1522 q=0.0 size=    3772kB time=50.8 bitrate= 608.9kbits/s    
frame= 1537 q=0.0 size=    3814kB time=51.3 bitrate= 609.6kbits/s    
frame= 1551 q=0.0 size=    3828kB time=51.7 bitrate= 606.3kbits/s    
frame= 1566 q=0.0 size=    3870kB time=52.2 bitrate= 607.1kbits/s    
frame= 1580 q=0.0 size=    3908kB time=52.7 bitrate= 607.6kbits/s    
frame= 1594 q=0.0 size=    3938kB time=53.2 bitrate= 606.9kbits/s    
frame= 1609 q=0.0 size=    3970kB time=53.7 bitrate= 606.2kbits/s    
frame= 1624 q=0.0 size=    4008kB time=54.2 bitrate= 606.3kbits/s    
frame= 1639 q=0.0 size=    4044kB time=54.7 bitrate= 606.1kbits/s    
frame= 1653 q=0.0 size=    4060kB time=55.1 bitrate= 603.4kbits/s    
frame= 1668 q=0.0 size=    4110kB time=55.6 bitrate= 605.3kbits/s    
frame= 1683 q=0.0 size=    4148kB time=56.1 bitrate= 605.5kbits/s    
frame= 1698 q=0.0 size=    4158kB time=56.6 bitrate= 601.6kbits/s    
frame= 1713 q=0.0 size=    4198kB time=57.1 bitrate= 602.0kbits/s    
frame= 1729 q=0.0 size=    4228kB time=57.7 bitrate= 600.7kbits/s    
frame= 1744 q=0.0 size=    4266kB time=58.2 bitrate= 600.9kbits/s    
frame= 1759 q=0.0 size=    4300kB time=58.7 bitrate= 600.5kbits/s    
frame= 1773 q=0.0 size=    4338kB time=59.1 bitrate= 601.0kbits/s    
frame= 1789 q=0.0 size=    4376kB time=59.7 bitrate= 600.9kbits/s    
frame= 1803 q=0.0 size=    4408kB time=60.1 bitrate= 600.6kbits/s    
frame= 1816 q=0.0 size=    4434kB time=60.6 bitrate= 599.8kbits/s    
frame= 1831 q=0.0 size=    4466kB time=61.1 bitrate= 599.2kbits/s    
frame= 1845 q=0.0 size=    4502kB time=61.5 bitrate= 599.4kbits/s    
frame= 1859 q=0.0 size=    4536kB time=62.0 bitrate= 599.4kbits/s    
frame= 1874 q=0.0 size=    4566kB time=62.5 bitrate= 598.5kbits/s    
frame= 1887 q=0.0 size=    4600kB time=62.9 bitrate= 598.8kbits/s    
frame= 1901 q=0.0 size=    4636kB time=63.4 bitrate= 599.1kbits/s    
frame= 1916 q=0.0 size=    4654kB time=63.9 bitrate= 596.7kbits/s    
frame= 1930 q=0.0 size=    4686kB time=64.4 bitrate= 596.4kbits/s    
frame= 1944 q=0.0 size=    4728kB time=64.8 bitrate= 597.4kbits/s    
frame= 1958 q=0.0 size=    4764kB time=65.3 bitrate= 597.7kbits/s    
frame= 1971 q=0.0 size=    4798kB time=65.7 bitrate= 598.0kbits/s    
frame= 1984 q=0.0 size=    4818kB time=66.2 bitrate= 596.5kbits/s    
frame= 1998 q=0.0 size=    4856kB time=66.6 bitrate= 597.0kbits/s    
frame= 2012 q=0.0 size=    4890kB time=67.1 bitrate= 597.0kbits/s    
frame= 2026 q=0.0 size=    4926kB time=67.6 bitrate= 597.2kbits/s    
frame= 2039 q=0.0 size=    4946kB time=68.0 bitrate= 595.8kbits/s    
frame= 2052 q=0.0 size=    4980kB time=68.4 bitrate= 596.1kbits/s    
frame= 2066 q=0.0 size=    5018kB time=68.9 bitrate= 596.6kbits/s    
frame= 2080 q=0.0 size=    5056kB time=69.4 bitrate= 597.1kbits/s    
frame= 2092 q=0.0 size=    5076kB time=69.8 bitrate= 596.0kbits/s    
frame= 2105 q=0.0 size=    5114kB time=70.2 bitrate= 596.7kbits/s    
frame= 2118 q=0.0 size=    5148kB time=70.6 bitrate= 597.0kbits/s    
frame= 2131 q=0.0 size=    5190kB time=71.1 bitrate= 598.2kbits/s    
frame= 2145 q=0.0 size=    5218kB time=71.5 bitrate= 597.5kbits/s    
frame= 2158 q=0.0 size=    5240kB time=72.0 bitrate= 596.4kbits/s    
frame= 2172 q=0.0 size=    5270kB time=72.4 bitrate= 596.0kbits/s    
frame= 2187 q=0.0 size=    5304kB time=72.9 bitrate= 595.7kbits/s    
frame= 2200 q=0.0 size=    5340kB time=73.4 bitrate= 596.2kbits/s    
frame= 2215 q=0.0 size=    5378kB time=73.9 bitrate= 596.4kbits/s    
frame= 2229 q=0.0 size=    5394kB time=74.3 bitrate= 594.4kbits/s    
frame= 2243 q=0.0 size=    5432kB time=74.8 bitrate= 594.8kbits/s    
frame= 2257 q=0.0 size=    5466kB time=75.3 bitrate= 594.9kbits/s    
frame= 2271 q=0.0 size=    5512kB time=75.7 bitrate= 596.2kbits/s    
frame= 2286 q=0.0 size=    5524kB time=76.2 bitrate= 593.5kbits/s    
frame= 2302 q=0.0 size=    5554kB time=76.8 bitrate= 592.6kbits/s    
frame= 2315 q=0.0 size=    5590kB time=77.2 bitrate= 593.1kbits/s    
frame= 2330 q=0.0 size=    5622kB time=77.7 bitrate= 592.7kbits/s    
frame= 2345 q=0.0 size=    5658kB time=78.2 bitrate= 592.6kbits/s    
frame= 2360 q=0.0 size=    5694kB time=78.7 bitrate= 592.6kbits/s    
frame= 2374 q=0.0 size=    5722kB time=79.2 bitrate= 592.0kbits/s    
frame= 2387 q=0.0 size=    5756kB time=79.6 bitrate= 592.3kbits/s    
frame= 2400 q=0.0 size=    5778kB time=80.0 bitrate= 591.3kbits/s    
frame= 2413 q=0.0 size=    5812kB time=80.5 bitrate= 591.6kbits/s    
frame= 2426 q=0.0 size=    5846kB time=80.9 bitrate= 591.9kbits/s    
frame= 2440 q=0.0 size=    5878kB time=81.4 bitrate= 591.7kbits/s    
frame= 2453 q=0.0 size=    5900kB time=81.8 bitrate= 590.8kbits/s    
frame= 2467 q=0.0 size=    5934kB time=82.3 bitrate= 590.8kbits/s    
frame= 2482 q=0.0 size=    5972kB time=82.8 bitrate= 591.0kbits/s    
frame= 2495 q=0.0 size=    6012kB time=83.2 bitrate= 591.8kbits/s    
frame= 2508 q=0.0 size=    6048kB time=83.7 bitrate= 592.3kbits/s    
frame= 2521 q=0.0 size=    6072kB time=84.1 bitrate= 591.6kbits/s    
frame= 2534 q=0.0 size=    6114kB time=84.5 bitrate= 592.6kbits/s    
frame= 2547 q=0.0 size=    6156kB time=85.0 bitrate= 593.6kbits/s    
frame= 2561 q=0.0 size=    6186kB time=85.4 bitrate= 593.3kbits/s    
frame= 2576 q=0.0 size=    6224kB time=85.9 bitrate= 593.4kbits/s    
frame= 2591 q=0.0 size=    6276kB time=86.4 bitrate= 594.9kbits/s    
frame= 2606 q=0.0 size=    6286kB time=86.9 bitrate= 592.4kbits/s    
frame= 2621 q=0.0 size=    6318kB time=87.4 bitrate= 592.0kbits/s    
frame= 2636 q=0.0 size=    6356kB time=87.9 bitrate= 592.2kbits/s    
frame= 2651 q=0.0 size=    6404kB time=88.4 bitrate= 593.3kbits/s    
frame= 2666 q=0.0 size=    6444kB time=88.9 bitrate= 593.7kbits/s    
frame= 2681 q=0.0 size=    6462kB time=89.4 bitrate= 592.0kbits/s    
frame= 2696 q=0.0 size=    6496kB time=89.9 bitrate= 591.8kbits/s    
frame= 2712 q=0.0 size=    6530kB time=90.5 bitrate= 591.4kbits/s    
frame= 2726 q=0.0 size=    6568kB time=90.9 bitrate= 591.8kbits/s    
frame= 2740 q=0.0 size=    6602kB time=91.4 bitrate= 591.8kbits/s    
frame= 2754 q=0.0 size=    6632kB time=91.9 bitrate= 591.4kbits/s    
frame= 2768 q=0.0 size=    6672kB time=92.3 bitrate= 592.0kbits/s    
frame= 2781 q=0.0 size=    6694kB time=92.8 bitrate= 591.2kbits/s    
frame= 2796 q=0.0 size=    6728kB time=93.3 bitrate= 591.0kbits/s    
frame= 2810 q=0.0 size=    6764kB time=93.7 bitrate= 591.2kbits/s    
frame= 2824 q=0.0 size=    6800kB time=94.2 bitrate= 591.4kbits/s    
frame= 2838 q=0.0 size=    6836kB time=94.7 bitrate= 591.6kbits/s    
frame= 2852 q=0.0 size=    6878kB time=95.1 bitrate= 592.3kbits/s    
frame= 2866 q=0.0 size=    6896kB time=95.6 bitrate= 590.9kbits/s    
frame= 2880 q=0.0 size=    6936kB time=96.1 bitrate= 591.5kbits/s    
frame= 2894 q=0.0 size=    6974kB time=96.5 bitrate= 591.8kbits/s    
frame= 2908 q=0.0 Lsize=    7038kB time=97.0 bitrate= 594.4kbits/s    

video:3089kB audio:2654kB global headers:0kB muxing overhead 22.557943%
*/

/*
Comparison
old: frame= 2521 q=0.0 size=    6072kB time=84.1 bitrate= 591.6kbits/s
new: frame= 2511 fps= 34 q=31.0 size=    6268kB time=83.8 bitrate= 613.1kbits/s
*/

/*
Latest ffmpeg - SVN trunk - 3/2007

FFMpeg command: 
[ffmpeg, -i, /home/analogue/workspaces/workspace-toolbox/toolbox/test/toolbox/tivo/h264.avi, -target, ntsc-dvd, -b, 331000, -aspect, 4:3, -s, 656x480, -padleft, 32, -padright, 32, -acodec, mp2, -ab, 128000, -ac, 2, -mbd, 2, -qmin, 2, -async, 1, -y, /tmp/tivoH264.mpg]

FFmpeg version SVN-r8487, Copyright (c) 2000-2007 Fabrice Bellard, et al.
  configuration: --enable-libmp3lame --enable-gpl --enable-pp --enable-pthreads --enable-libvorbis --enable-libogg --enable-liba52 --enable-libdts --enable-libgsm --enable-dc1394 --enable-debug --enable-libfaac --enable-libfaad --enable-libtheora --enable-libfaadbin
  libavutil version: 49.4.0
  libavcodec version: 51.40.0
  libavformat version: 51.11.0
  built on Mar 23 2007 15:55:40, gcc: 4.1.2 20060928 (prerelease) (Ubuntu 4.1.1-13ubuntu5)
Input #0, avi, from '/home/analogue/workspaces/workspace-toolbox/toolbox/test/toolbox/tivo/h264.avi':
  Duration: 00:01:37.1, start: 0.000000, bitrate: 203 kb/s
  Stream #0.0: Video: h264, yuv420p, 416x304, 25.00 fps(r)
  Stream #0.1: Audio: mp3, 22050 Hz, mono, 32 kb/s
Output #0, dvd, to '/tmp/tivoH264.mpg':
  Stream #0.0: Video: mpeg2video (hq), yuv420p, 720x480, q=2-31, 331 kb/s, 29.97 fps(c)
  Stream #0.1: Audio: mp2, 48000 Hz, stereo, 128 kb/s
Stream mapping:
  Stream #0.0 -> #0.0
  Stream #0.1 -> #0.1
Press [q] to stop encoding
frame=   18 fps=  0 q=4.4 size=     118kB time=0.6 bitrate=1704.2kbits/s    
frame=   36 fps= 35 q=4.7 size=     176kB time=1.2 bitrate=1234.6kbits/s    
frame=   54 fps= 35 q=8.0 size=     258kB time=1.8 bitrate=1195.1kbits/s    
frame=   73 fps= 35 q=7.7 size=     332kB time=2.4 bitrate=1132.1kbits/s    
frame=   90 fps= 35 q=15.0 size=     398kB time=3.0 bitrate=1097.9kbits/s    
frame=  109 fps= 35 q=13.4 size=     468kB time=3.6 bitrate=1063.9kbits/s    
frame=  128 fps= 35 q=13.2 size=     506kB time=4.2 bitrate= 978.2kbits/s    
frame=  149 fps= 36 q=12.0 size=     568kB time=4.9 bitrate= 942.2kbits/s    
frame=  165 fps= 35 q=12.4 size=     602kB time=5.5 bitrate= 901.2kbits/s    
frame=  183 fps= 35 q=24.5 size=     688kB time=6.1 bitrate= 928.1kbits/s    
frame=  203 fps= 35 q=23.7 size=     726kB time=6.7 bitrate= 882.4kbits/s    
frame=  221 fps= 35 q=23.8 size=     788kB time=7.3 bitrate= 879.4kbits/s    
frame=  239 fps= 35 q=31.0 size=     840kB time=7.9 bitrate= 866.5kbits/s    
frame=  254 fps= 35 q=31.0 size=     874kB time=8.4 bitrate= 848.1kbits/s    
frame=  273 fps= 35 q=31.0 size=     926kB time=9.1 bitrate= 835.8kbits/s    
frame=  290 fps= 35 q=31.0 size=     958kB time=9.6 bitrate= 813.9kbits/s    
frame=  309 fps= 35 q=31.0 size=    1006kB time=10.3 bitrate= 801.9kbits/s    
frame=  325 fps= 35 q=31.0 size=    1036kB time=10.8 bitrate= 785.0kbits/s    
frame=  344 fps= 35 q=31.0 size=    1090kB time=11.4 bitrate= 780.2kbits/s    
frame=  362 fps= 35 q=31.0 size=    1136kB time=12.0 bitrate= 772.6kbits/s    
frame=  382 fps= 35 q=31.0 size=    1188kB time=12.7 bitrate= 765.5kbits/s    
frame=  392 fps= 34 q=31.0 size=    1218kB time=13.0 bitrate= 764.8kbits/s    
frame=  411 fps= 34 q=31.0 size=    1248kB time=13.7 bitrate= 747.3kbits/s    
frame=  429 fps= 34 q=31.0 size=    1302kB time=14.3 bitrate= 746.9kbits/s    
frame=  447 fps= 34 q=31.0 size=    1334kB time=14.9 bitrate= 734.3kbits/s    
frame=  464 fps= 34 q=31.0 size=    1378kB time=15.4 bitrate= 730.7kbits/s    
frame=  484 fps= 35 q=31.0 size=    1430kB time=16.1 bitrate= 726.9kbits/s    
frame=  501 fps= 35 q=31.0 size=    1458kB time=16.7 bitrate= 715.9kbits/s    
frame=  521 fps= 35 q=31.0 size=    1512kB time=17.4 bitrate= 713.9kbits/s    
frame=  541 fps= 35 q=31.0 size=    1558kB time=18.0 bitrate= 708.4kbits/s    
frame=  561 fps= 35 q=31.0 size=    1610kB time=18.7 bitrate= 705.9kbits/s    
frame=  577 fps= 35 q=31.0 size=    1646kB time=19.2 bitrate= 701.6kbits/s    
frame=  595 fps= 35 q=31.0 size=    1696kB time=19.8 bitrate= 701.0kbits/s    
frame=  611 fps= 35 q=31.0 size=    1726kB time=20.4 bitrate= 694.7kbits/s    
frame=  631 fps= 35 q=31.0 size=    1774kB time=21.0 bitrate= 691.3kbits/s    
frame=  645 fps= 35 q=31.0 size=    1800kB time=21.5 bitrate= 686.2kbits/s    
frame=  661 fps= 34 q=31.0 size=    1850kB time=22.0 bitrate= 688.2kbits/s    
frame=  675 fps= 34 q=31.0 size=    1876kB time=22.5 bitrate= 683.4kbits/s    
frame=  688 fps= 34 q=31.0 size=    1916kB time=22.9 bitrate= 684.7kbits/s    
frame=  705 fps= 34 q=31.0 size=    1952kB time=23.5 bitrate= 680.7kbits/s    
frame=  722 fps= 33 q=31.0 size=    2006kB time=24.1 bitrate= 683.1kbits/s    
frame=  741 fps= 33 q=31.0 size=    2046kB time=24.7 bitrate= 678.8kbits/s    
frame=  758 fps= 33 q=31.0 size=    2104kB time=25.3 bitrate= 682.4kbits/s    
frame=  777 fps= 33 q=31.0 size=    2144kB time=25.9 bitrate= 678.3kbits/s    
frame=  796 fps= 33 q=31.0 size=    2202kB time=26.5 bitrate= 680.0kbits/s    
frame=  812 fps= 33 q=31.0 size=    2248kB time=27.1 bitrate= 680.5kbits/s    
frame=  831 fps= 33 q=31.0 size=    2282kB time=27.7 bitrate= 675.0kbits/s    
frame=  850 fps= 33 q=31.0 size=    2330kB time=28.3 bitrate= 673.8kbits/s    
frame=  869 fps= 34 q=31.0 size=    2376kB time=29.0 bitrate= 672.1kbits/s    
frame=  886 fps= 34 q=31.0 size=    2414kB time=29.5 bitrate= 669.7kbits/s    
frame=  905 fps= 34 q=31.0 size=    2466kB time=30.2 bitrate= 669.7kbits/s    
frame=  924 fps= 34 q=31.0 size=    2516kB time=30.8 bitrate= 669.2kbits/s    
frame=  943 fps= 34 q=31.0 size=    2544kB time=31.4 bitrate= 663.0kbits/s    
frame=  963 fps= 34 q=31.0 size=    2598kB time=32.1 bitrate= 663.0kbits/s    
frame=  977 fps= 34 q=31.0 size=    2626kB time=32.6 bitrate= 660.6kbits/s    
frame=  991 fps= 34 q=31.0 size=    2658kB time=33.0 bitrate= 659.2kbits/s    
frame= 1011 fps= 34 q=31.0 size=    2710kB time=33.7 bitrate= 658.8kbits/s    
frame= 1027 fps= 34 q=31.0 size=    2744kB time=34.2 bitrate= 656.6kbits/s    
frame= 1045 fps= 34 q=31.0 size=    2810kB time=34.8 bitrate= 660.8kbits/s    
frame= 1062 fps= 34 q=31.0 size=    2848kB time=35.4 bitrate= 659.0kbits/s    
frame= 1081 fps= 34 q=31.0 size=    2904kB time=36.0 bitrate= 660.2kbits/s    
frame= 1099 fps= 34 q=31.0 size=    2938kB time=36.6 bitrate= 656.9kbits/s    
frame= 1118 fps= 34 q=31.0 size=    2982kB time=37.3 bitrate= 655.4kbits/s    
frame= 1138 fps= 34 q=31.0 size=    3014kB time=37.9 bitrate= 650.8kbits/s    
frame= 1157 fps= 34 q=31.0 size=    3070kB time=38.6 bitrate= 652.0kbits/s    
frame= 1171 fps= 34 q=24.8 size=    3124kB time=39.0 bitrate= 655.5kbits/s    
frame= 1189 fps= 34 q=31.0 size=    3166kB time=39.6 bitrate= 654.3kbits/s    
frame= 1206 fps= 34 q=31.0 size=    3208kB time=40.2 bitrate= 653.6kbits/s    
frame= 1225 fps= 34 q=31.0 size=    3268kB time=40.8 bitrate= 655.5kbits/s    
frame= 1242 fps= 34 q=31.0 size=    3304kB time=41.4 bitrate= 653.7kbits/s    
frame= 1261 fps= 34 q=31.0 size=    3356kB time=42.0 bitrate= 653.9kbits/s    
frame= 1280 fps= 34 q=31.0 size=    3402kB time=42.7 bitrate= 653.0kbits/s    
frame= 1300 fps= 34 q=31.0 size=    3434kB time=43.3 bitrate= 649.0kbits/s    
frame= 1320 fps= 34 q=31.0 size=    3484kB time=44.0 bitrate= 648.5kbits/s    
frame= 1339 fps= 34 q=31.0 size=    3534kB time=44.6 bitrate= 648.5kbits/s    
frame= 1359 fps= 34 q=31.0 size=    3566kB time=45.3 bitrate= 644.7kbits/s    
frame= 1379 fps= 34 q=31.0 size=    3622kB time=46.0 bitrate= 645.3kbits/s    
frame= 1398 fps= 34 q=31.0 size=    3674kB time=46.6 bitrate= 645.7kbits/s    
frame= 1412 fps= 34 q=31.0 size=    3702kB time=47.1 bitrate= 644.1kbits/s    
frame= 1431 fps= 34 q=31.0 size=    3736kB time=47.7 bitrate= 641.4kbits/s    
frame= 1451 fps= 34 q=31.0 size=    3784kB time=48.4 bitrate= 640.7kbits/s    
frame= 1470 fps= 34 q=31.0 size=    3832kB time=49.0 bitrate= 640.4kbits/s    
frame= 1489 fps= 34 q=31.0 size=    3874kB time=49.6 bitrate= 639.2kbits/s    
frame= 1508 fps= 34 q=31.0 size=    3928kB time=50.3 bitrate= 639.9kbits/s    
frame= 1526 fps= 34 q=31.0 size=    3958kB time=50.9 bitrate= 637.2kbits/s    
frame= 1546 fps= 34 q=31.0 size=    4012kB time=51.6 bitrate= 637.5kbits/s    
frame= 1564 fps= 34 q=31.0 size=    4048kB time=52.2 bitrate= 635.9kbits/s    
frame= 1566 fps= 34 q=31.0 size=    4066kB time=52.2 bitrate= 637.9kbits/s    
frame= 1580 fps= 34 q=31.0 size=    4100kB time=52.7 bitrate= 637.5kbits/s    
frame= 1599 fps= 34 q=31.0 size=    4144kB time=53.3 bitrate= 636.7kbits/s    
frame= 1617 fps= 34 q=31.0 size=    4172kB time=53.9 bitrate= 633.8kbits/s    
frame= 1636 fps= 34 q=31.0 size=    4224kB time=54.6 bitrate= 634.3kbits/s    
frame= 1656 fps= 34 q=31.0 size=    4274kB time=55.2 bitrate= 634.0kbits/s    
frame= 1675 fps= 34 q=31.0 size=    4316kB time=55.9 bitrate= 633.0kbits/s    
frame= 1692 fps= 34 q=31.0 size=    4364kB time=56.4 bitrate= 633.6kbits/s    
frame= 1712 fps= 34 q=31.0 size=    4394kB time=57.1 bitrate= 630.5kbits/s    
frame= 1730 fps= 34 q=31.0 size=    4444kB time=57.7 bitrate= 631.0kbits/s    
frame= 1750 fps= 34 q=31.0 size=    4488kB time=58.4 bitrate= 630.0kbits/s    
frame= 1767 fps= 34 q=31.0 size=    4518kB time=58.9 bitrate= 628.1kbits/s    
frame= 1778 fps= 34 q=31.0 size=    4540kB time=59.3 bitrate= 627.3kbits/s    
frame= 1796 fps= 34 q=31.0 size=    4592kB time=59.9 bitrate= 628.1kbits/s    
frame= 1815 fps= 34 q=31.0 size=    4640kB time=60.5 bitrate= 628.0kbits/s    
frame= 1833 fps= 34 q=31.0 size=    4674kB time=61.1 bitrate= 626.4kbits/s    
frame= 1852 fps= 34 q=31.0 size=    4722kB time=61.8 bitrate= 626.3kbits/s    
frame= 1870 fps= 34 q=31.0 size=    4760kB time=62.4 bitrate= 625.3kbits/s    
frame= 1891 fps= 34 q=31.0 size=    4810kB time=63.1 bitrate= 624.8kbits/s    
frame= 1910 fps= 34 q=31.0 size=    4844kB time=63.7 bitrate= 623.0kbits/s    
frame= 1929 fps= 34 q=31.0 size=    4890kB time=64.3 bitrate= 622.7kbits/s    
frame= 1948 fps= 34 q=31.0 size=    4944kB time=65.0 bitrate= 623.4kbits/s    
frame= 1967 fps= 34 q=31.0 size=    4984kB time=65.6 bitrate= 622.4kbits/s    
frame= 1985 fps= 34 q=31.0 size=    5042kB time=66.2 bitrate= 623.9kbits/s    
frame= 2004 fps= 34 q=31.0 size=    5078kB time=66.8 bitrate= 622.4kbits/s    
frame= 2024 fps= 34 q=31.0 size=    5122kB time=67.5 bitrate= 621.6kbits/s    
frame= 2043 fps= 34 q=24.8 size=    5174kB time=68.1 bitrate= 622.1kbits/s    
frame= 2061 fps= 34 q=31.0 size=    5206kB time=68.7 bitrate= 620.5kbits/s    
frame= 2076 fps= 34 q=31.0 size=    5244kB time=69.2 bitrate= 620.5kbits/s    
frame= 2094 fps= 34 q=31.0 size=    5294kB time=69.8 bitrate= 621.0kbits/s    
frame= 2113 fps= 34 q=31.0 size=    5332kB time=70.5 bitrate= 619.8kbits/s    
frame= 2131 fps= 34 q=31.0 size=    5384kB time=71.1 bitrate= 620.6kbits/s    
frame= 2147 fps= 34 q=31.0 size=    5428kB time=71.6 bitrate= 621.0kbits/s    
frame= 2163 fps= 34 q=31.0 size=    5458kB time=72.1 bitrate= 619.8kbits/s    
frame= 2179 fps= 34 q=31.0 size=    5504kB time=72.7 bitrate= 620.4kbits/s    
frame= 2196 fps= 34 q=31.0 size=    5534kB time=73.2 bitrate= 619.0kbits/s    
frame= 2203 fps= 34 q=31.0 size=    5556kB time=73.5 bitrate= 619.5kbits/s    
frame= 2215 fps= 34 q=31.0 size=    5582kB time=73.9 bitrate= 619.0kbits/s    
frame= 2230 fps= 34 q=24.8 size=    5622kB time=74.4 bitrate= 619.2kbits/s    
frame= 2243 fps= 34 q=31.0 size=    5648kB time=74.8 bitrate= 618.5kbits/s    
frame= 2257 fps= 34 q=31.0 size=    5676kB time=75.3 bitrate= 617.7kbits/s    
frame= 2274 fps= 34 q=31.0 size=    5714kB time=75.8 bitrate= 617.2kbits/s    
frame= 2292 fps= 34 q=31.0 size=    5760kB time=76.4 bitrate= 617.3kbits/s    
frame= 2308 fps= 34 q=31.0 size=    5786kB time=77.0 bitrate= 615.8kbits/s    
frame= 2326 fps= 34 q=24.8 size=    5834kB time=77.6 bitrate= 616.1kbits/s    
frame= 2346 fps= 34 q=31.0 size=    5860kB time=78.2 bitrate= 613.5kbits/s    
frame= 2365 fps= 34 q=31.0 size=    5918kB time=78.9 bitrate= 614.6kbits/s    
frame= 2384 fps= 34 q=31.0 size=    5952kB time=79.5 bitrate= 613.2kbits/s    
frame= 2399 fps= 34 q=31.0 size=    6000kB time=80.0 bitrate= 614.3kbits/s    
frame= 2418 fps= 34 q=31.0 size=    6034kB time=80.6 bitrate= 612.9kbits/s    
frame= 2437 fps= 34 q=31.0 size=    6078kB time=81.3 bitrate= 612.6kbits/s    
frame= 2455 fps= 34 q=31.0 size=    6114kB time=81.9 bitrate= 611.7kbits/s    
frame= 2474 fps= 34 q=31.0 size=    6170kB time=82.5 bitrate= 612.5kbits/s    
frame= 2492 fps= 34 q=31.0 size=    6212kB time=83.1 bitrate= 612.3kbits/s    
frame= 2511 fps= 34 q=31.0 size=    6268kB time=83.8 bitrate= 613.1kbits/s    
frame= 2529 fps= 34 q=31.0 size=    6308kB time=84.4 bitrate= 612.6kbits/s    
frame= 2547 fps= 34 q=31.0 size=    6366kB time=85.0 bitrate= 613.9kbits/s    
frame= 2567 fps= 34 q=31.0 size=    6430kB time=85.6 bitrate= 615.2kbits/s    
frame= 2586 fps= 34 q=31.0 size=    6466kB time=86.3 bitrate= 614.1kbits/s    
frame= 2599 fps= 34 q=31.0 size=    6486kB time=86.7 bitrate= 612.9kbits/s    
frame= 2618 fps= 34 q=31.0 size=    6536kB time=87.3 bitrate= 613.2kbits/s    
frame= 2635 fps= 34 q=31.0 size=    6560kB time=87.9 bitrate= 611.5kbits/s    
frame= 2654 fps= 34 q=31.0 size=    6606kB time=88.5 bitrate= 611.3kbits/s    
frame= 2673 fps= 34 q=31.0 size=    6662kB time=89.2 bitrate= 612.1kbits/s    
frame= 2694 fps= 34 q=31.0 size=    6692kB time=89.9 bitrate= 610.1kbits/s    
frame= 2710 fps= 34 q=31.0 size=    6744kB time=90.4 bitrate= 611.2kbits/s    
frame= 2728 fps= 34 q=31.0 size=    6772kB time=91.0 bitrate= 609.7kbits/s    
frame= 2745 fps= 34 q=31.0 size=    6816kB time=91.6 bitrate= 609.8kbits/s    
frame= 2764 fps= 34 q=31.0 size=    6852kB time=92.2 bitrate= 608.9kbits/s    
frame= 2781 fps= 34 q=31.0 size=    6898kB time=92.8 bitrate= 609.2kbits/s    
frame= 2788 fps= 34 q=31.0 size=    6908kB time=93.0 bitrate= 608.5kbits/s    
frame= 2806 fps= 34 q=31.0 size=    6954kB time=93.6 bitrate= 608.7kbits/s    
frame= 2824 fps= 34 q=31.0 size=    6988kB time=94.2 bitrate= 607.7kbits/s    
frame= 2842 fps= 34 q=31.0 size=    7048kB time=94.8 bitrate= 609.1kbits/s    
frame= 2859 fps= 34 q=31.0 size=    7082kB time=95.4 bitrate= 608.4kbits/s    
frame= 2878 fps= 34 q=31.0 size=    7138kB time=96.0 bitrate= 609.1kbits/s    
frame= 2896 fps= 34 q=31.0 size=    7178kB time=96.6 bitrate= 608.7kbits/s    
frame= 2909 fps= 34 q=31.0 Lsize=    7222kB time=97.0 bitrate= 609.7kbits/s    
video:4060kB audio:1516kB global headers:0kB muxing overhead 29.510681%

Transcoded at 1.13x speed
*/