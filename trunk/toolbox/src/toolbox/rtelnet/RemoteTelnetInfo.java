package toolbox.rtelnet;

/**
 * RemoteTelnetInfo is a data object that stores information needed to
 * identify, authenticate, and drive a telnet session.
 * 
 * @see toolbox.rtelnet.RemoteTelnet
 * @see toolbox.rtelnet.RemoteTelnetClient
 */
public class RemoteTelnetInfo
{
    //--------------------------------------------------------------------------
    // Defaults
    //--------------------------------------------------------------------------
    
    /**
     * Default telnet port as defined in the RFC is 23.
     */
    public static final int DEFAULT_TELNET_PORT = 23;
    
    //--------------------------------------------------------------------------
    // Fields
    //--------------------------------------------------------------------------
    
    /**
     * Hostname of the machine we're telnetting to.
     */
    private String hostname_;
    
    /**
     * Telnet port on the hostname.
     */
    private int port_;
    
    /**
     * Username for authentication.
     */
    private String username_;
    
    /**
     * Password for username.
     */
    private String password_;
    
    /**
     * Command to execute in the remote shell.
     */
    private String command_;

    //--------------------------------------------------------------------------
    // Constructors 
    //--------------------------------------------------------------------------
    
    /**
     * Creates a RemoteTelnetInfo using the default telnet port.
     */    
    public RemoteTelnetInfo()
    {
        setPort(DEFAULT_TELNET_PORT);
    }
    
    
    /**
     * Creates a RemoteTelnetInfo with the given parameters.
     * 
     * @param hostname Host to telnet to.
     * @param port Port to connect to on telnet host.
     * @param username Username.
     * @param password Password.
     * @param command Command to execute.
     */
    public RemoteTelnetInfo(
        String hostname, 
        int port, 
        String username, 
        String password, 
        String command)
    {
        setHostname(hostname);
        setPort(port);
        setUsername(username);
        setPassword(password);
        setCommand(command);    
    }
    
    //--------------------------------------------------------------------------
    // Public 
    //--------------------------------------------------------------------------
    
    /**
     * Returns the command.
     * 
     * @return String
     */
    public String getCommand()
    {
        return command_;
    }

    
    /**
     * Returns the hostname.
     * 
     * @return String
     */
    public String getHostname()
    {
        return hostname_;
    }

    
    /**
     * Returns the password.
     * 
     * @return String
     */
    public String getPassword()
    {
        return password_;
    }

    
    /**
     * Returns the telnet port.
     * 
     * @return int
     */
    public int getPort()
    {
        return port_;
    }

    
    /**
     * Returns the username.
     * 
     * @return String
     */
    public String getUsername()
    {
        return username_;
    }

    
    /**
     * Sets the command.
     * 
     * @param command The command to set.
     */
    public void setCommand(String command)
    {
        command_ = command;
    }

    
    /**
     * Sets the hostname.
     * 
     * @param hostname The hostname to set.
     */
    public void setHostname(String hostname)
    {
        hostname_ = hostname;
    }

    
    /**
     * Sets the password.
     * 
     * @param password The password to set.
     */
    public void setPassword(String password)
    {
        password_ = password;
    }

    
    /**
     * Sets the port.
     * 
     * @param port The port to set.
     */
    public void setPort(int port)
    {
        port_ = port;
    }

    
    /**
     * Sets the username.
     * 
     * @param username The username to set.
     */
    public void setUsername(String username)
    {
        username_ = username;
    }
    
    //--------------------------------------------------------------------------
    // Overrides java.lang.Object  
    //--------------------------------------------------------------------------
    
    /**
     * Returns TelnetInfo as string.
     * 
     * @return String
     */
    public String toString()
    {
        return 
            "TelnetInfo\n" + 
            "==========\n" +
            "Hostname: " + getHostname() + "\n" +
            "Port    : " + getPort() + "\n" +
            "Username: " + getUsername() + "\n" +
            "Password: " + getPassword() + "\n" +
            "Command : " + getCommand() + "\n";
    }
}