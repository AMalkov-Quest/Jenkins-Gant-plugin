package hudson.plugins.gant;

import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;

/**
 * Gant installation.
 * 
 * @author Kohsuke Kawaguchi
*/
public final class GantInstallation {
    private final String name;
    private final String groovyHome;
	private final String javaHome;
	private final String antHome;
    private final String scriptsHome;
    private final String execName;

    @DataBoundConstructor
    public GantInstallation(String name, String home, String java, String ant, String scripts, String execname) {
        this.name = name;
        this.groovyHome = home;
		this.javaHome = java;
		this.antHome = ant;
        this.scriptsHome = scripts;
        this.execName = execname;
    }

    /**
     * install directory.
     */
    public String getGroovyHome() {
        return groovyHome;
    }
	
	public String getJavaHome() {
        return javaHome;
    }
	
	public String getAntHome() {
        return antHome;
    }
    
    /**
     * scripts location directory.
     */
    public String getScriptsHome() {
        return scriptsHome;
    }
    
    /**
     * gant executable name.
     */
    public String getExecName() {
        return execName;
    }

    /**
     * Human readable display name.
     */
    public String getName() {
        return name;
    }

    public File getExecutable() {
    	String executable = execName;
    	
    	if(execName == null)
        {
	        if( File.separatorChar == '\\' )
	        {
	        	executable = "gant.exe";
	        }
	        else
	        {
	        	executable = "gant";
	        }
        }
    	
    	return new File( getGroovyHome(), "bin/" + executable);
    }

    /**
     * Returns true if the executable exists.
     */
    public boolean getExists() {
        return getExecutable().exists();
    }
}
