package toolbox.jedit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import junit.textui.TestRunner;

import nu.xom.Element;

import org.apache.log4j.Logger;

import org.jedit.syntax.TextAreaDefaults;
import org.jedit.syntax.XMLTokenMarker;

import toolbox.junit.testcase.UITestCase;
import toolbox.util.SwingUtil;
import toolbox.util.formatter.XMLFormatter;
import toolbox.util.ui.JSmartButton;
import toolbox.util.ui.JSmartComboBox;
import toolbox.util.ui.JSmartDialog;
import toolbox.util.ui.JSmartLabel;
import toolbox.util.ui.plaf.LookAndFeelUtil;

/**
 * Unit test for {@link toolbox.jedit.JEditTextArea}.
 */
public class JEditTextAreaTest extends UITestCase
{
    private static final Logger logger_ = Logger.getLogger(JEditTextAreaTest.class);
        
    //--------------------------------------------------------------------------
    // Main
    //--------------------------------------------------------------------------
    
    public static void main(String[] args) throws Exception
    {
        LookAndFeelUtil.setPreferredLAF();
        TestRunner.run(JEditTextAreaTest.class);
    }
    
    //--------------------------------------------------------------------------
    // Unit Tests
    //--------------------------------------------------------------------------
    
    /**
     * Instantiates the JEditTextArea so that it can be tested manually by
     * the user.
     */
    public void testJEditTextArea()
    {
        JEditTester test = new JEditTester();
        test.setVisible(true);            
    }
    
    //--------------------------------------------------------------------------
    // Helper Classes
    //--------------------------------------------------------------------------
    
    /**
     * Frame to encompass the JEditTextArea.
     */
    class JEditTester extends JSmartDialog implements ActionListener
    {
        private JEditTextArea jeta_;
        private JComboBox fgCombo_;
        private JComboBox bgCombo_;
        private Map clut_;
                        
        /**
         * Creates a JEditTester. 
         */
        public JEditTester()
        {
            super((JFrame) null, "testJEditTextArea", true);
            buildView();
            
            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            pack();
            SwingUtil.centerWindow(this);
        }
        
        
        /**
         * Constructs the user interface. 
         */
        protected void buildView()
        {
            Container c = getContentPane();

            TextAreaDefaults defaults = new JavaDefaults();
            defaults.popup = new JEditPopupMenu();

            jeta_ = new JEditTextArea(new XMLTokenMarker(), defaults);
            ((JEditPopupMenu) defaults.popup).setTextArea(jeta_);
            ((JEditPopupMenu) defaults.popup).buildView();

            c.add(BorderLayout.CENTER, new JScrollPane(jeta_));
            c.add(BorderLayout.SOUTH, buildControlView());
        }
        
        
        /**
         * Constructs the controls portion of the GUI.
         * 
         * @return JPanel
         */
        protected JPanel buildControlView() 
        {
            clut_ = new HashMap();
            clut_.put("red", Color.red);
            clut_.put("green", Color.green);
            clut_.put("blue", Color.blue);
            
            JPanel p = new JPanel(new FlowLayout());
            
            p.add(new JSmartLabel("Foreground"));
            
            p.add(fgCombo_ = new JSmartComboBox(
                new String [] {"red", "green", "blue"}));
            
            p.add(new JSmartLabel("Background"));
            
            p.add(bgCombo_ = new JSmartComboBox(
                new String [] {"red", "green", "blue"}));
            
            fgCombo_.addActionListener(this);
            bgCombo_.addActionListener(this);
            
            JSmartButton savePrefs = 
                new JSmartButton(
                    new AbstractAction("Save Prefs") 
                    {
                        public void actionPerformed(ActionEvent e)
                        {
                            Element root = new Element("root");
                            
                            try
                            {
                                jeta_.savePrefs(root);
                                jeta_.setText(
                                    new XMLFormatter().format(root.toXML()));
                            }
                            catch (Exception e1)
                            {
                                logger_.error(e1);
                            }
                        }
                    });
            
            p.add(savePrefs);
            
            return p;
        }
        
        
        /**
         * @see java.awt.event.ActionListener#actionPerformed(
         *      java.awt.event.ActionEvent)
         */
        public void actionPerformed(ActionEvent e)
        {
            Object obj = e.getSource();
            
            if (obj == fgCombo_)
            {
                jeta_.getPainter().setForeground((Color) 
                    clut_.get(fgCombo_.getSelectedItem()));
            }
            else if (obj == bgCombo_)
            {
                jeta_.getPainter().setBackground((Color) 
                    clut_.get(bgCombo_.getSelectedItem()));
            }
            else
                logger_.debug("Nothing to do!");
        }
    }
}