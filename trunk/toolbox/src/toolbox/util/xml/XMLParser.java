package toolbox.util.xml;

import java.io.Reader;
import java.io.IOException;
import java.util.Stack;

/**
 * A tiny parser of xml text. It is intended to deal with simple 
 * config files.
 */
public class XMLParser
{
    // various states in which the parser may be.
    static final private int VACUUM = 1;
    static final private int IN_TAG = 2;
    static final private int IN_TAG_NAME = 3;
    static final private int CLOSING_TAG = 9;
    static final private int SLASH = 4;
    static final private int ATTR_NAME = 5;
    static final private int END_ATTR_NAME = 6;
    static final private int ATTR_VALUE = 7;
    static final private int END_ATTR_VALUE = 8;
    static final private int START_COMMENT = 10;
    static final private int IN_COMMENT = 11;
    
    // special state to handle the DOCTYPE 'tag'
    static final private int IN_DOCTYPE = 12;
    static final private int IN_DTD = 13;

    public XMLParser()
    {
    }

    /**
     * Parse an XML text read in from a Reader.
     * Returns the root node of the xml text.
     */
    public XMLNode parseXML(Reader reader) throws IOException
    {
        int state = VACUUM;
        int x = 1; // current character number 
        int y = 1; // current line number
        int rch = 0;
        char ch;
        boolean pi = false;

        StringBuffer tag_name = new StringBuffer();
        StringBuffer attr_name = new StringBuffer();
        StringBuffer attr_value = new StringBuffer();
        StringBuffer value = new StringBuffer();

        XMLNode root = null;
        XMLNode node = null;

        // the current parent.
        XMLNode parent = null;

        // Stack is used to remember the hierarchy of parents.
        Stack stack = new Stack();

        while ((rch = reader.read()) != -1)
        {
            ch = (char) rch;
            // QUERY: Should newlines only be allowed in a VACUUM ?
            // error messaging
            x++;
            if (ch == '\n')
            {
                y++;
                x = 1;
                continue;
            }

            switch (state)
            {
                case VACUUM :
                    {
                        if (ch == '<')
                        {
                            pi = false;
                            state = IN_TAG_NAME;
                            if (value.length() != 0)
                            {
                                node = new XMLNode(tag_name.toString());
                                node.setPlaintext(value.toString().trim());
                                parent.addNode(node);
                                value.setLength(0);
                            }
                        }
                        else if ((ch == ' ') || (ch == '\t'))
                        {
                            if (value.length() != 0)
                            {
                                value.append(ch);
                            }
                            continue;
                        }
                        else if (ch == '\r')
                        { // part of a newline.
                            if (value.length() != 0)
                            {
                                value.append(ch);
                            }
                            continue;
                        }
                        else
                        {
                            value.append(ch);
                        }
                    }
                    break;

                case START_COMMENT :
                    {
                        if (ch == '-')
                        {
                            // feels bad to do this here.
                            ch = (char) reader.read();
                            if (ch == '-')
                            {
                                state = IN_COMMENT;
                            }
                            else
                            {
                                value.append("!-");
                                value.append(ch);
                            }
                        }
                        else
                        {
                            tag_name.append('!');
                            tag_name.append(ch);
                            state = IN_TAG_NAME;
                        }
                    }
                    break;

                case IN_COMMENT :
                    {
                        if (ch == '-')
                        {
                            // feels bad to do this here.
                            ch = (char) reader.read();
                            if (ch == '-')
                            {
                                ch = (char) reader.read();
                                if (ch == '>')
                                {
                                    node = new XMLNode();
                                    node.setPlaintext(value.toString().trim());
                                    node.setComment(true);
                                    if (parent == null)
                                    {
                                        parent = new XMLNode();
                                        parent.setInvisible(true);
                                        root = parent;
                                    }
                                    parent.addNode(node);
                                    value.setLength(0);
                                    state = VACUUM;
                                }
                                else
                                {
                                    value.append("--");
                                    value.append(ch);
                                }
                            }
                            else
                            {
                                value.append("-");
                                value.append(ch);
                            }
                        }
                        else
                        {
                            value.append(ch);
                        }
                    }
                    break;

                case IN_DOCTYPE :
                    {
                        if (ch == '[')
                        {
                            value.append(ch);
                            state = IN_DTD;
                        }
                        else if (ch == '>')
                        {
                            node = new XMLNode("!DOCTYPE");
                            node.setPlaintext(value.toString());
                            node.setDocType(true);
                            if (parent == null)
                            {
                                parent = new XMLNode();
                                parent.setInvisible(true);
                                root = parent;
                            }
                            parent.addNode(node);
                            value.setLength(0);
                            state = VACUUM;
                        }
                        else
                        {
                            value.append(ch);
                        }
                    }
                    break;

                case IN_DTD :
                    {
                        if (ch == ']')
                        {
                            value.append(ch);
                            state = IN_DOCTYPE;
                        }
                        else
                        {
                            value.append(ch);
                        }
                    }

                case IN_TAG_NAME :
                    {
                        if ((ch == '!') && (tag_name.length() == 0))
                        {
                            state = START_COMMENT;
                        }
                        else if (
                            !pi && (ch == '?') && (tag_name.length() == 0))
                        {
                            pi = true;
                        }
                        else if ((ch == '/') && (tag_name.length() == 0))
                        {
                            // closing tag
                            parent = (XMLNode) stack.pop();
                            state = CLOSING_TAG;
                        }
                        else if (
                            ((ch == ' ')
                                || (ch == '\t')
                                || (ch == '>')
                                || (ch == '/'))
                                || (ch == '?'))
                        {
                            if ("!DOCTYPE".equals(tag_name.toString()))
                            {
                                state = IN_DOCTYPE;
                                tag_name.setLength(0);
                                continue;
                            }
                            if (pi)
                            {
                                if (parent == null)
                                {
                                    parent = new XMLNode();
                                    parent.setInvisible(true);
                                    root = parent;
                                }
                            }
                            node = new XMLNode(tag_name.toString());
                            node.setPI(pi);
                            pi = false;
                            if (root == null)
                            {
                                root = node;
                            }
                            if (parent != null)
                            {
                                parent.addNode(node);
                            }
                            tag_name.setLength(0);
                            if ((ch == '/') || (ch == '?'))
                            {
                                state = SLASH;
                            }
                            else if (ch == '>')
                            {
                                state = VACUUM;
                                stack.push(parent);
                                parent = node;
                            }
                            else
                            {
                                state = IN_TAG;
                            }
                        }
                        else
                        {
                            tag_name.append(ch);
                        }
                    }
                    break;

                case IN_TAG :
                    {
                        if ((ch == ' ') || (ch == '\t'))
                        {
                            continue;
                        }
                        else if (ch == '>')
                        {
                            state = VACUUM;
                            stack.push(parent);
                            parent = node;
                        }
                        else if ((ch == '/') || (ch == '?'))
                        {
                            // empty tag
                            state = SLASH;
                        }
                        else
                        {
                            state = ATTR_NAME;
                            attr_name.setLength(1);
                            attr_name.setCharAt(0, ch);
                        }
                    }
                    break;

                case SLASH :
                    {
                        if (ch == '>')
                        {
                            state = VACUUM;
                        }
                        else
                        {
                            state = IN_TAG;
                        }
                    }
                    break;

                case CLOSING_TAG :
                    {
                        if (ch == '>')
                        {
                            state = VACUUM;
                        }
                        else
                        {
                            continue;
                        }
                    }
                    break;

                case ATTR_NAME :
                    {
                        if ((ch == ' ') || (ch == '\t'))
                        {
                            node.addAttr(
                                attr_name.toString(),
                                attr_name.toString());
                            state = IN_TAG;
                        }
                        else if (ch == '=')
                        {
                            state = END_ATTR_NAME;
                        }
                        else
                        {
                            attr_name.append(ch);
                        }
                    }
                    break;

                case END_ATTR_NAME :
                    {
                        if (ch == '"')
                        {
                            state = ATTR_VALUE;
                            attr_value.setLength(0);
                        }
                        else
                        {
                            state = ATTR_NAME;
                        }
                    }
                    break;

                case ATTR_VALUE :
                    {
                        if (ch == '"')
                        {
                            node.addAttr(
                                attr_name.toString(),
                                attr_value.toString());
                            state = IN_TAG;
                        }
                        else
                        {
                            attr_value.append(ch);
                        }
                    }
                    break;
            }
        }
        return root;
    }
}
