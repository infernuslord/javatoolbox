package toolbox.util.ui.flippane;

import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;

import javax.swing.Icon;

/**
 * FlipIcon is the icon that holds the rotated text name of the flipper.
 */
public class FlipIcon implements Icon
{
    // TODO: Refactor to RotatedTextIcon and move to toolbox.util.ui
    
    //--------------------------------------------------------------------------
    // Constants 
    //--------------------------------------------------------------------------
    
    /** 
     * No rotation of text.
     */
    public static final int NONE = 0;
    
    /**
     * Clockwise rotation of text.
     */
    public static final int CW = 1;
    
    /**
     * Counter-clockwise rotation of text.
     */
    public static final int CCW = 2;
    
    //--------------------------------------------------------------------------
    // Fields 
    //--------------------------------------------------------------------------
    
    /**
     * Direction in which to rotate the text.
     */
    private int rotate_;
    
    /**
     * Font to use for rendering the text.
     */
    private Font font_;
    
    /**
     * Glyph vector.
     */
    private GlyphVector glyphs_;
    
    /**
     * Width of the text.
     */
    private float width_;
    
    /**
     * Height of the text.
     */
    private float height_;
    
    /**
     * Ascent of the text.
     */
    private float ascent_;
    
    /**
     * Rendering hints for antialiasing.
     */
    private RenderingHints renderHints_;

    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------
    
    /**
     * Creates a FlipIcon.
     * 
     * @param rotate [NONE|CW|CCW]
     * @param font Font to use for rendering.
     * @param text Text of icon.
     */
    public FlipIcon(int rotate, Font font, String text)
    {
        rotate_ = rotate;
        font_ = font;

        FontRenderContext fontRenderContext = 
            new FontRenderContext(null, true, true);
            
        glyphs_ = font.createGlyphVector(fontRenderContext, text);
        width_ = (int) glyphs_.getLogicalBounds().getWidth() + 4;
        //height = (int)glyphs.getLogicalBounds().getHeight();

        LineMetrics lineMetrics = font.getLineMetrics(text, fontRenderContext);

        ascent_ = lineMetrics.getAscent();
        height_ = (int) lineMetrics.getHeight();

        renderHints_ = new RenderingHints(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
            
        renderHints_.put(RenderingHints.KEY_FRACTIONALMETRICS,
            RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            
        renderHints_.put(RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY);
    } 

    //--------------------------------------------------------------------------
    // Icon Interface
    //--------------------------------------------------------------------------
    
    /**
     * Returns the icons width.
     * 
     * @return int
     */
    public int getIconWidth()
    {
        return (int) (rotate_ == CW || rotate_ == CCW ? height_ : width_);
    } 

    
    /**
     * Returns the icons height.
     * 
     * @return int
     */
    public int getIconHeight()
    {
        return (int) (rotate_ == CW || rotate_ == CCW ? width_ : height_);
    } 

    
    /**
     * Renders the icon on the graphics object.
     * 
     * @param c Component on which to paint the icon.
     * @param g Graphics context.
     * @param x X coord.
     * @param y y coord.
     */
    public void paintIcon(Component c, Graphics g, int x, int y)
    {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setFont(font_);
        AffineTransform oldTransform = g2d.getTransform();
        RenderingHints oldHints = g2d.getRenderingHints();

        g2d.setRenderingHints(renderHints_);
        g2d.setColor(c.getForeground());
        
        if (rotate_ == NONE)
        {
            // No rotation
            g2d.drawGlyphVector(glyphs_, x + 2, y + ascent_);
        } 
        else if (rotate_ == CW)
        {
            // Clockwise rotation
            AffineTransform trans = new AffineTransform();
            trans.concatenate(oldTransform);
            trans.translate(x, y + 2);
            trans.rotate(Math.PI / 2, height_ / 2, width_ / 2);
            
            g2d.setTransform(trans);
            
            g2d.drawGlyphVector(
                glyphs_,
                (height_ - width_) / 2,
                (width_ - height_) / 2 + ascent_);
        } 
        else if (rotate_ == CCW)
        {
            // Counterclockwise rotation
            AffineTransform trans = new AffineTransform();
            trans.concatenate(oldTransform);
            trans.translate(x, y - 2);
            trans.rotate(Math.PI * 3 / 2, height_ / 2, width_ / 2);
            
            g2d.setTransform(trans);
            
            g2d.drawGlyphVector(
                glyphs_,
                (height_ - width_) / 2,
                (width_ - height_) / 2 + ascent_);
        } 

        g2d.setTransform(oldTransform);
        g2d.setRenderingHints(oldHints);
    } 
}