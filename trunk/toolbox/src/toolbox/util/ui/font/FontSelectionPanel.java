/* 
Copyright (C) 2000, 2001 Greg Merrill (greghmerrill@yahoo.com)

This file is part of Follow (http://follow.sf.net).

Follow is free software; you can redistribute it and/or modify
it under the terms of version 2 of the GNU General Public
License as published by the Free Software Foundation.

Follow is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Follow; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
-------------------------------------------------------------------------

Modified without permission

*/

package toolbox.util.ui.font;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Observable;
import java.util.Observer;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


/**
 * FontSelectionPanel
 */
public class FontSelectionPanel extends JPanel
{
    /** JList for font family */
    protected JList fontFamilyList_;
    
    /** FontStlyeList (subclass of JList) for font style */
    protected FontStyleList fontStyleList_;
    
    /** JTextField for font size */
    protected JTextField fontSize_;
    
    /** JList for font size */
    protected JList fontSizeList_;
    
    /** PhraseCanvas in which font samples are displayed */
    protected PhraseCanvas phraseCanvas_;

    /** Observable used for registering/notifying Observers */
    protected PublicChangeObservable observable_ = new PublicChangeObservable();

    /** Maximum number of characters permissibile in a valid font size */
    protected int maxNumCharsInFontSize_ = 3;

    /**
     * Like {@link #FontSelectionPanel(java.awt.Font)}, except an initialFont of 
     * <code>null</code> will be used.
     */
    public FontSelectionPanel()
    {
        this(null);
    }


    /**
     * Like {@link #FontSelectionPanel(java.awt.Font, String[], int[])}, except 
     * that a default list of styles{"Plain", "Bold", "Italic", "Bold Italic"}
     * and font sizes {8, 9, 10, 12, 14} will be used.
     * 
     * @param initialFont see 
     *        @link #FontSelectionPanel(java.awt.Font, String[], int[])}
     */
    public FontSelectionPanel(Font initialFont)
    {
        this(initialFont,
        
        // Don't change the following two values without changing the javadocs
        new String[] { "Plain", "Bold", "Italic", "Bold Italic" },
            new int[] { 8, 9, 10, 12, 14 });
    }


    /**
     * Construct a new FontSelectionPanel whose family, style & size widget
     * selections are set according to the supplied initial Font. Additionally,
     * the style & size values available will be dictated by the values in
     * styleDisplayNames and predefinedSizes, respectively.
     * 
     * @param initialFont 
     * 
     *      Newly constructed FontSelectionPanel's family, style, and size 
     *      widgets will be set according to this value. This value may be 
     *      null, in which case an initial font will be automatically created.
     *      This auto-created font will have a family, style, and size 
     *      corresponding to the first avaiable value in the widget form 
     *      family, style, and size respectively.
     * 
     * @param styleDisplayNames
     *  
     *      Must contain exactly four members. The members of this array 
     *      represent the following styles, in order: Font.PLAIN, Font.BOLD, 
     *      Font.ITALIC, and Font.BOLD+Font.ITALIC
     * 
     * @param predefinedSizes  
     * 
     *      Must contain one or more predefined font sizes which will be 
     *      available to the user as a convenience for populating the font 
     *      size text field; all values must be greater than 0.
     */    
    public FontSelectionPanel(Font initialFont, String[] styleDisplayNames,
        int[] predefinedSizes)
    {
        super(new GridBagLayout());
        this.setBorder(new EmptyBorder(12, 12, 11, 11));
        GridBagConstraints gbc = new GridBagConstraints();

        String[] availableFontFamilyNames = 
            GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();

        if (initialFont == null)
        {
            initialFont =  
                new Font(availableFontFamilyNames[0], Font.PLAIN,
                     predefinedSizes[0]);
        }

        // Font family
        fontFamilyList_ = new JList(availableFontFamilyNames);
        fontFamilyList_.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fontFamilyList_.setVisibleRowCount(8);
        
        ListSelectionListener phraseCanvasUpdater = new ListSelectionListener()
        {
            public void valueChanged(ListSelectionEvent e)
            {
                if (!e.getValueIsAdjusting())
                {
                    observable_.setChanged();
                    observable_.notifyObservers();
                }
            }
        };
        
        fontFamilyList_.addListSelectionListener(phraseCanvasUpdater);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridheight = 2;
        add(new JScrollPane(fontFamilyList_), gbc);

        // Font style
        fontStyleList_ = new FontStyleList(styleDisplayNames);
        fontStyleList_.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fontStyleList_.setVisibleRowCount(4);
        fontStyleList_.addListSelectionListener(phraseCanvasUpdater);
        gbc.gridx = 1;
        gbc.insets = new Insets(0, 10, 0, 0);
        
        // fontStyleList_ is put into a JScrollPane only because it puts a nice
        // border around it which is consistent with the border around 
        // fontFamilyList_
        add(new JScrollPane(fontStyleList_), gbc);

        // Font size
        fontSize_ = new JTextField();
        fontSize_.setHorizontalAlignment(JTextField.RIGHT);
        fontSize_.setColumns(4);
        gbc.gridx = 2;
        gbc.gridheight = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        this.add(fontSize_, gbc);
        
        fontSizeList_ =
            new JList(validateAndConvertPredefinedSizes(predefinedSizes));
            
        fontSizeList_.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Will be able to see more than 1 row because gbc.fill set to BOTH
        fontSizeList_.setVisibleRowCount(1);
        fontSizeList_.setCellRenderer(new ListCellRenderer());
        gbc.gridy = 1;
        gbc.insets = new Insets(10, 10, 0, 0);
        gbc.fill = GridBagConstraints.BOTH;
        this.add(new JScrollPane(fontSizeList_), gbc);

        // Phrase Canvas (displays current font selection)
        phraseCanvas_ =
            new PhraseCanvas(initialFont.getFamily(), initialFont, Color.black);
            
        addObserver(new Observer()
        {
            public void update(Observable o, Object arg)
            {
                try
                {
                    phraseCanvas_.setPhrase(
                        (String) fontFamilyList_.getSelectedValue());
                    phraseCanvas_.setFont(
                        FontSelectionPanel.this.getSelectedFont());
                }
                catch (FontSelectionException e)
                {
                    phraseCanvas_.setPhrase("");
                }
                phraseCanvas_.invalidate();
                phraseCanvas_.repaint();
            }
        });
        
        phraseCanvas_.setSize((int) this.getPreferredSize().getWidth(), 100);
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        gbc.insets = new Insets(10, 0, 0, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // put into JScrollPane for fmting purposes(no scrolling ever occurs)
        this.add(new JScrollPane(phraseCanvas_), gbc);

        // Use FontSizeSynchronizer to ensure consistency between text field &
        // list for font size
        FontSizeSynchronizer fontSizeSynchronizer =
            new FontSizeSynchronizer(fontSizeList_, fontSize_);
            
        fontSizeList_.addListSelectionListener(fontSizeSynchronizer);
        fontSize_.getDocument().addDocumentListener(fontSizeSynchronizer);

        // Set initial widget values here at the end of the constructor to 
        // ensure that all listeners have been added beforehand
        fontFamilyList_.setSelectedValue(initialFont.getFamily(), true);
        fontStyleList_.setSelectedStyle(initialFont.getStyle());
        fontSize_.setText(String.valueOf(initialFont.getSize()));
    }
    

    /**
     * @throws IllegalArgumentException thrown if 
     * 
     * <ul>
     *   <li>predefinedSizes does not contain one or more integer values
     *   <li>predefinedSizes contains any integers with a value of less than 1
     * </ul>
     */
    private Integer[] validateAndConvertPredefinedSizes(int[] predefinedSizes)
    {
        if (predefinedSizes == null)
            throw new IllegalArgumentException(
                "int[] predefinedSizes may not be null");

        if (predefinedSizes.length < 1)
            throw new IllegalArgumentException(
                "int[] predefinedSizes must contain one or more values");
        
        Integer[] predefinedSizeIntegers = new Integer[predefinedSizes.length];
        
        for (int i = 0; i < predefinedSizes.length; i++)
        {
            if (predefinedSizes[i] < 1)
                throw new IllegalArgumentException(
                    "int[] predefinedSizes may not contain integers" +
                    " with value less than 1");
     
            predefinedSizeIntegers[i] = new Integer(predefinedSizes[i]);
        }
        
        return predefinedSizeIntegers;
    }


    /**
     * Adds an Observer to this FontSelectionPanel; the supplied Observer will 
     * have its update() method called any time the Font currently specified
     * in the FontSelectionPanel changes. (The <tt>arg</tt> supplied to the
     * Observer will be <tt>null</tt>.)
     * 
     * @param   observer   Observer to be added
     * @see     java.util.Observer
     */
    public void addObserver(Observer observer)
    {
        observable_.addObserver(observer);
    }
    
    
    /**
     * Removes an Observer from this FontSelectionPanel.
     * 
     * @param   observer   Observer to be removed
     * @see     java.util.Observer
     */
    public void deleteObserver(Observer observer)
    {
        observable_.deleteObserver(observer);
    }
    

    /**
     * Returns the currently selected font family
     * 
     * @return  currently selected font family
     * @throws  NoFontFamilySelectedException thrown if no font family is
     *          currently selected
     */
    public String getSelectedFontFamily() throws FontSelectionException
    {
        String fontFamily = (String) fontFamilyList_.getSelectedValue();
        
        if (fontFamily == null)
            throw new FontSelectionException(
                "No font family is currently selected");

        return fontFamily;
    }

    /**
     * Returns the currently selected font style.
     * 
     * @return  Currently selected font style. This value will correspond to one
     *          of the font styles specified in {@link java.awt.Font}
     * @throws  FontSelectionException thrown if no font style is 
     *          currently selected
     */
    public int getSelectedFontStyle() throws FontSelectionException
    {
        return fontStyleList_.getSelectedStyle();
    }


    /**
     * Returns the currently selected font size.
     * 
     * @return currently selected font size.
     * @throws FontSelectionException thrown if no font size is
     *         currently specified
     */
    public int getSelectedFontSize() throws FontSelectionException, 
        FontSelectionException
    {
        String fontSize = fontSize_.getText();
        
        if ((fontSize == null) || (fontSize.equals("")))
            throw new FontSelectionException("No font size specified");
        
        if (fontSize.length() > maxNumCharsInFontSize_)
            throw new FontSelectionException(
                "Too many characters in font size");
            
        try
        {
            return Integer.parseInt(fontSize);
        }
        catch (NumberFormatException e)
        {
            throw new FontSelectionException(
                "The number specified in the font size text field (" + 
                fontSize_.getText() + ") is not a valid integer.");
        }
    }


    /**
     * Returns the currently selected font.
     * 
     * @return Currently selected font.
     * @throws FontSelectionException thrown if no valid font is currently 
     *         specified; the actual class of the exception thrown may be
     *         {@link FontSelectionPanel.FontSelectionException},
     */
    public Font getSelectedFont() throws FontSelectionException
    {
        return new Font(
            getSelectedFontFamily(), 
            getSelectedFontStyle(), 
            getSelectedFontSize());
    }


    /**
     * Changes the currently selected font by assigning all widget values to 
     * match the family/style/size values of the supplied font
     * 
     * @param   font    Font whose values should be used to set widgets
     * @throws  IllegalArgumentException thrown if the family or style of the
     *          font supplied are not available or invalid
     */
    public void setSelectedFont(Font font)
    {
        setSelectedFontFamily(font.getFamily());
        setSelectedFontStyle(font.getStyle());
        setSelectedFontSize(font.getSize());
    }


    /**
     * Sets the currently selected font family.
     * 
     * @param   family family to which selection should change
     * @throws  IllegalArgumentException thrown if the supplied font family is
     *          not among the list of available font families
     */
    public void setSelectedFontFamily(String family)
    {
        ListModel familyListModel = fontFamilyList_.getModel();
        
        for (int i = 0; i < familyListModel.getSize(); i++)
        {
            if (familyListModel.getElementAt(i).equals(family))
            {
                fontFamilyList_.setSelectedValue(family, true);
                return;
            }
        }
        
        throw new IllegalArgumentException(
            "The font family supplied, '" + family + 
            "', is not in the list of availalbe font families.");
    }


    /**
     * Sets the currently selected font style.
     * 
     * @param   style   Style to which selection should change
     * @throws  IllegalArgumentException thrown if the supplied font style is
     *          not one of Font.PLAIN, Font.BOLD, Font.ITALIC, or 
     *          Font.BOLD+Font.ITALIC
     */
    public void setSelectedFontStyle(int style)
    {
        fontStyleList_.setSelectedStyle(style);
    }


    /**
     * Sets the currently selected font size.
     * 
     * @param size size to which selection should change
     */
    public void setSelectedFontSize(int size)
    {
        fontSize_.setText(String.valueOf(size));
    }


    /**
     * This class synchronizes font size value between the list containing
     * available font sizes & the text field in which font size is ultimately
     * specified.
     */
    protected class FontSizeSynchronizer implements DocumentListener, 
        ListSelectionListener
    {
        
        protected JList list_;
        protected JTextField textField_;
        protected boolean updating_;
        
        
        /**
         * @param   list        List containing predefined font sizes 
         * @param   textField   Text field in which font size is specified
        */
        public FontSizeSynchronizer(JList list, JTextField textField)
        {
            list_ = list;
            textField_ = textField;
        }


        /** 
         * @see javax.swing.event.ListSelectionListener 
         */
        public void valueChanged(ListSelectionEvent e)
        {
            if (updating_)
                return;

            updating_ = true;
            
            if (!e.getValueIsAdjusting())
            {
                Object selectedValue =
                    ((JList) e.getSource()).getSelectedValue();
                    
                if (selectedValue != null)
                    textField_.setText(selectedValue.toString());

                observable_.setChanged();
                observable_.notifyObservers();
            }
            
            updating_ = false;
        }


        /** 
         * @see javax.swing.event.DocumentListener 
         */
        public void changedUpdate(DocumentEvent e)
        {
            handle(e);
        }

        
        /**
         *  @see javax.swing.event.DocumentListener 
         */
        public void insertUpdate(DocumentEvent e)
        {
            handle(e);
        }
        
        
        /** 
         * @see javax.swing.event.DocumentListener 
         */
        public void removeUpdate(DocumentEvent e)
        {
            handle(e);
        }
        
        
        /** 
         * Handles all DocumentEvents 
         */
        protected void handle(DocumentEvent e)
        {
            if (updating_)
                return;

            updating_ = true;
            
            try
            {
                Integer currentFontSizeInteger =
                    Integer.valueOf(textField_.getText());
                    
                boolean currentSizeWasInList = false;
                Object listMember;
                
                for (int i = 0; i < list_.getModel().getSize(); i++)
                {
                    listMember = list_.getModel().getElementAt(i);
                    
                    if (listMember.equals(currentFontSizeInteger))
                    {
                        list_.setSelectedValue(currentFontSizeInteger, true);
                        currentSizeWasInList = true;
                        break;
                    }
                }
                
                if (!currentSizeWasInList)
                    list_.clearSelection();
            }
            catch (NumberFormatException nfe)
            {
                list_.clearSelection();
            }
            
            observable_.setChanged();
            observable_.notifyObservers();
            updating_ = false;
        }
    }

    //
    // Static inner classes
    //


    /**
     * An implementation of {@link javax.swing.ListCellRenderer} which right
     * justifies all cells.
     */
    protected static class ListCellRenderer extends DefaultListCellRenderer
    {
        public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus)
        {
            JLabel label =
                (JLabel) super.getListCellRendererComponent(
                    list,
                    value,
                    index,
                    isSelected,
                    cellHasFocus);
            label.setHorizontalAlignment(JLabel.RIGHT);
            return label;
        }
    }


    /**
     * Subclass of {@link java.util.Observable} which allows <tt>public</tt> 
     * access to the setChanged() method.
     */
    protected static class PublicChangeObservable extends Observable
    {
        /** 
         * @see java.util.Observable#setChanged() 
         */
        public void setChanged()
        {
            super.setChanged();
        }
    }
}
